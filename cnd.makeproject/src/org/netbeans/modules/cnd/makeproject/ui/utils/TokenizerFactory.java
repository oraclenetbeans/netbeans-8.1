/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.makeproject.ui.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cnd.makeproject.api.configurations.Folder;
import org.netbeans.modules.cnd.makeproject.api.configurations.Item;
import org.netbeans.modules.cnd.utils.CndPathUtilities;

/**
 * list <--> converter
 *
 * @author Alexander Simon
 */
public final class TokenizerFactory {

    public interface Converter {

        String convertToString(List<String> list);

        List<String> convertToList(String text);
    }

    public static final Converter MACRO_CONVERTER = new Converter() {
            private final PathResolver resolver = new TrivialPathResolver();
            @Override
            public String convertToString(List<String> list) {
                return TokenizerFactory.convertToString(list,  ' ');
            }

            @Override
            public List<String> convertToList(String text) {
                return TokenizerFactory.tokenize(text, new String[]{"-D"}, " ", resolver); // NOI18N
            }
        };

    public static final Converter UNDEF_CONVERTER = new Converter() {
            private final PathResolver resolver = new TrivialPathResolver();
            @Override
            public String convertToString(List<String> list) {
                return TokenizerFactory.convertToString(list,  ' ');
            }

            @Override
            public List<String> convertToList(String text) {
                return TokenizerFactory.tokenize(text, new String[]{"-U"}, " ", resolver); // NOI18N
            }
        };

    public static final Converter DEFAULT_CONVERTER = new Converter() {
            @Override
            public String convertToString(List<String> list) {
                return TokenizerFactory.convertToString(list,  ';');
            }

            @Override
            public List<String> convertToList(String text) {
                List<String> newList = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(text, ";"); // NOI18N
                while (st.hasMoreTokens()) {
                    newList.add(st.nextToken());
                }
                return newList;
            }
        };

    public static Converter getPathConverter(final Project project, final Folder folder, final Item item, final String flag) {
        return new Converter() {
            private final PathResolver resolver = new PathResolver(){

                @Override
                public String resolve(String from) {
                    if (CndPathUtilities.isPathAbsolute(from)) {
                        return from;
                    }
                    if (project != null) {
                        return from;
                    } else if (folder != null) {
                        if (folder.isDiskFolder()) {
                            String folderPath = folder.getRootPath();
                            folder.isDiskFolder();
                            if (folderPath != null) {
                                from = folderPath+"/"+from; // NOI18N
                                if (CndPathUtilities.isPathAbsolute(from)) {
                                    return CndPathUtilities.toAbsoluteOrRelativePath(folder.getConfigurationDescriptor().getBaseDir(), from);
                                } else {
                                    return from;
                                }
                            }
                        } else {
                            // TODO: can we suggest more useful?
                            return from;
                        }
                    } else if (item != null) {
                        // TODO: should use compile dir?
                        from = CndPathUtilities.getDirName(item.getAbsolutePath())+"/"+from; // NOI18N
                        return CndPathUtilities.toAbsoluteOrRelativePath(item.getFolder().getConfigurationDescriptor().getBaseDir(), from);
                    }
                    return from;
                }
                
            };
            @Override
            public String convertToString(List<String> list) {
                return TokenizerFactory.convertToString(list,  ';');
            }

            @Override
            public List<String> convertToList(String text) {
                return TokenizerFactory.tokenize(text, new String[]{flag}, "; ", resolver); // NOI18N
            }
        };
    }

    private interface PathResolver {
        String resolve(String from);
    }
    
    private static final class TrivialPathResolver implements PathResolver {

        @Override
        public String resolve(String from) {
            return from;
        }
    }
    
    // This is naive implementation of tokenizer for strings like this (without ordinal quotes):
    // ' 111   222  333=444       555'
    // '111 "222 333" "44 4=555" "666=777 888" 999=000 "a"'
    // '111 "222 333"   "44 4=555"   "666=777 888"   999=000 "a" b'
    // Should work in most real-word case, but you can easily broke it if you want.
    // If token is started with -D, then -D is removed.
    private static List<String> tokenize(String text, String[] keys, String SEPARATOR, PathResolver resolver) {
        final char QUOTE = '\"'; // NOI18N
        List<String> result = new ArrayList<>();
        boolean inQuote = false;
        boolean innerQuote = false;
        int start = 0;
        int i = 0;
        char prev = 0;
        while (i < text.length()) {
            String str = text.substring(start, i).trim();
            if (isSeparator(SEPARATOR, text.charAt(i)) && !inQuote) {
                if (str.length() > 0) {
                    addItem(result, keys, str, resolver);
                    start = i + 1;
                }
            } else if (text.charAt(i) == QUOTE && inQuote) {
                if (str.length() > 0) {
                    addItem(result, keys, str + (innerQuote ? QUOTE : ""), resolver); // NOI18N
                    start = i + 1;
                    inQuote = false;
                    innerQuote = false;
                }
            } else if (text.charAt(i) == QUOTE) {
                inQuote = true;
                if (isSeparator(SEPARATOR, prev)) {
                    start = i + 1;
                } else {
                    innerQuote = true;
                }
            }
            prev = text.charAt(i);
            i++;
        }
        if (start != i) {
            addItem(result, keys, text.substring(start).trim(), resolver);
        }
        return result;
    }

    private static boolean isSeparator(String separators, char c) {
        return separators.indexOf(c) >= 0;
    }
    
    private static void addItem(List<String> result, String[] keys, String value, PathResolver resolver) {
        for(String key : keys) {
            if (value.startsWith(key)) {
                String s = removePrefix(key, value, resolver);
                if (!s.isEmpty()) {
                    result.add(s);
                }
                return;
            }
        }
        if (value.startsWith("-")) { // NOI18N
            //ugnore other keys
        } else {
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
    }
    
    private static String removePrefix(String prefix, String str, PathResolver resolver) {
        return str.startsWith(prefix) ? resolver.resolve(str.substring(prefix.length())) : str;
    }

    private static String convertToString(List<String> list, char separator) {
        boolean addSep = false;
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (addSep) {
                ret.append(separator);
            }
            ret.append(CndPathUtilities.quoteIfNecessary(list.get(i)));
            addSep = true;
        }
        return ret.toString();
    }
}
