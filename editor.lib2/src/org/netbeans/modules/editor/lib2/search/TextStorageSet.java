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
package org.netbeans.modules.editor.lib2.search;

import java.util.HashMap;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.lib.editor.util.CharSubSequence;

/**
 * Set of character sequences allowing to compare a part of character sequence.
 * <br/>
 * Note: Character sequences stored must be comparable by their equals() method.
 *
 * @author Miloslav Metelka
 */

public final class TextStorageSet {
    
    private final CharSequencesMap textMap = new CharSequencesMap();

    public CharSequence add(CharSequence text) {
        return textMap.put(text, text);
    }

    public CharSequence get(CharSequence text) {
        return get(text, 0, text.length());
    }

    public CharSequence get(CharSequence text, int startIndex, int endIndex) {
        return textMap.get(text, startIndex, endIndex);
    }

    public CharSequence remove(CharSequence text) {
        return remove(text, 0, text.length());
    }

    public CharSequence remove(CharSequence text, int startIndex, int endIndex) {
        return textMap.remove(text, startIndex, endIndex);
    }
    
    public int size() {
        return textMap.size();
    }

    public void clear() {
        textMap.clear();
    }

    private static final class CharSequencesMap extends HashMap<CharSequence,CharSequence> implements CharSequence {

        CharSequence compareText;

        int compareIndex;

        int compareLength;

        static final long serialVersionUID = 0L;

        public CharSequence get(CharSequence text, int startIndex, int endIndex) {
            compareText = text;
            compareIndex = startIndex;
            compareLength = endIndex - startIndex;
            CharSequence ret = get(this);
            compareText = null; // enable possible GC
            return ret;
        }

        public boolean containsKey(CharSequence text, int startIndex, int endIndex) {
            compareText = text;
            compareIndex = startIndex;
            compareLength = endIndex - startIndex;
            boolean ret = containsKey(this);
            compareText = null; // enable possible GC
            return ret;
        }

        public CharSequence remove(CharSequence text, int startIndex, int endIndex) {
            compareText = text;
            compareIndex = startIndex;
            compareLength = endIndex - startIndex;
            CharSequence ret = remove(this);
            compareText = null;
            return ret;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof CharSequence) {
                CharSequence text = (CharSequence) o;
                if (compareLength == text.length()) {
                    for (int index = compareLength - 1; index >= 0; index--) {
                        if (compareText.charAt(compareIndex + index) != text.charAt(index)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            return false;
        }

        @Override
        public int hashCode() {
            int h = 0;
            CharSequence text = compareText;
            int endIndex = compareIndex + compareLength;

            for (int i = compareIndex; i < endIndex; i++) {
                h = 31 * h + text.charAt(i);
            }

            return h;
        }

        @Override
        public int length() {
            return compareLength;
        }

        @Override
        public char charAt(int index) {
            CharSequenceUtilities.checkIndexValid(index, length());
            return compareText.charAt(compareIndex + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new CharSubSequence.StringLike(this, start, end);
        }
        
    }

}
