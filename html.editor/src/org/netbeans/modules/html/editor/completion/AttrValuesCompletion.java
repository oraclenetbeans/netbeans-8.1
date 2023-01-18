/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.html.editor.completion;

import java.awt.Color;
import org.netbeans.modules.html.editor.api.completion.HtmlCompletionItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.ImageIcon;
import org.netbeans.modules.web.common.api.FileReferenceCompletion;
import org.netbeans.modules.web.common.api.ValueCompletion;
import org.openide.filesystems.FileObject;

/**
 *
 * @author marekfukala
 */
public abstract class AttrValuesCompletion {

    private static final Map<String, Map<String, ValueCompletion<HtmlCompletionItem>>> SUPPORTS =
            new HashMap<>();
    private static final Map<String, ValueCompletion<HtmlCompletionItem>> ALL_TAG_SUPPORTS =
            new HashMap<>();
    public static final ValueCompletion<HtmlCompletionItem> FILE_NAME_SUPPORT = new FilenameSupport();
    private static final ValueCompletion<HtmlCompletionItem> CONTENT_TYPE_SUPPORT =
            new ValuesSetSupport(new String[]{"text/css", "text/javascript"});
    private static final ValueCompletion<HtmlCompletionItem> TRUE_FALSE_SUPPORT =
            new ValuesSetSupport(new String[]{"true", "false"});
    private static final ValueCompletion<HtmlCompletionItem> SCOPE_SUPPORT =
            new ValuesSetSupport(new String[]{"row", "col", "rowgroup", "colgroup"});
    private static final ValueCompletion<HtmlCompletionItem> SHAPE_SUPPORT =
            new ValuesSetSupport(new String[]{"circle", "default", "poly", "rect"});
    private static final ValueCompletion<HtmlCompletionItem> ON_OFF_SUPPORT =
            new ValuesSetSupport(new String[]{"on", "off"});
    private static final ValueCompletion<HtmlCompletionItem> FORMENCTYPE_SUPPORT =
            new ValuesSetSupport(new String[]{"application/x-www-form-urlencoded", "multipart/form-data", "text/plain"});
    private static final ValueCompletion<HtmlCompletionItem> FORMMETHOD_SUPPORT =
            new ValuesSetSupport(new String[]{"GET", "POST", "PUT", "DELETE"});
    private static final ValueCompletion<HtmlCompletionItem> PRELOAD_SUPPORT =
            new ValuesSetSupport(new String[]{"none", "metadata", "auto"});
    private static final ValueCompletion<HtmlCompletionItem> BUTTON_TYPE_SUPPORT =
            new ValuesSetSupport(new String[]{"submit", "reset", "button"});
    private static final ValueCompletion<HtmlCompletionItem> COMMAND_TYPE_SUPPORT =
            new ValuesSetSupport(new String[]{"command", "checkbox", "radio"});
    private static final ValueCompletion<HtmlCompletionItem> MENU_TYPE_SUPPORT =
            new ValuesSetSupport(new String[]{"context", "toolbar"});
    private static final ValueCompletion<HtmlCompletionItem> WRAP_SUPPORT =
            new ValuesSetSupport(new String[]{"soft", "hard"});
    private static final ValueCompletion<HtmlCompletionItem> INPUT_TYPE_SUPPORT =
            new ValuesSetSupport(new String[]{"hidden",
                "text","search","tel","url","email","password","datetime","date",
                "month","week","time","datetime-local","number","range","color",
                "checkbox","radio","file","submit","image","reset","button"});

    private static final ValueCompletion<HtmlCompletionItem> LINK_TYPES_SUPPORT =
            new ValuesSetSupport(new String[]{
                "alternate", "stylesheet", "start", "next", "prev", "contents",
                "index", "glossary", "copyright", "chapter", "section", "subsection",
                "appendix", "help","bookmark"});
    
    private static final ValueCompletion<HtmlCompletionItem> DIR_SUPPORT =
            new ValuesSetSupport(new String[]{"ltr", "rtl"});
    
    static {
        //TODO uff, such long list ... redo it so it resolves according to the DTD attribute automatically
        //mixed with html5 content...
        putSupport("a", "href", FILE_NAME_SUPPORT); //NOI18N
        putSupport("area", "href", FILE_NAME_SUPPORT); //NOI18N
        putSupport("a", "ping", FILE_NAME_SUPPORT); //NOI18N
        putSupport("area", "ping", FILE_NAME_SUPPORT); //NOI18N
        putSupport("link", "href", FILE_NAME_SUPPORT); //NOI18N
        putSupport("base", "href", FILE_NAME_SUPPORT); //NOI18N
        putSupport("script", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("audio", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("embed", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("iframe", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("img", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("input", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("source", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("track", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("video", "src", FILE_NAME_SUPPORT); //NOI18N        
        putSupport("img", "longdesc", FILE_NAME_SUPPORT); //NOI18N
        putSupport("img", "usemap", FILE_NAME_SUPPORT); //NOI18N
        putSupport("input", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("frame", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("iframe", "src", FILE_NAME_SUPPORT); //NOI18N
        putSupport("body", "background", FILE_NAME_SUPPORT); //NOI18N
        putSupport("input", "usemap", FILE_NAME_SUPPORT); //NOI18N
        putSupport("object", "classid", FILE_NAME_SUPPORT); //NOI18N
        putSupport("object", "codebase", FILE_NAME_SUPPORT); //NOI18N
        putSupport("object", "data", FILE_NAME_SUPPORT); //NOI18N
        putSupport("object", "usemap", FILE_NAME_SUPPORT); //NOI18N
        putSupport("applet", "codebase", FILE_NAME_SUPPORT); //NOI18N
        putSupport("q", "cite", FILE_NAME_SUPPORT); //NOI18N
        putSupport("blackquote", "cite", FILE_NAME_SUPPORT); //NOI18N
        putSupport("ins", "cite", FILE_NAME_SUPPORT); //NOI18N
        putSupport("del", "cite", FILE_NAME_SUPPORT); //NOI18N
        putSupport("form", "action", FILE_NAME_SUPPORT); //NOI18N
        putSupport("button", "formaction", FILE_NAME_SUPPORT); //NOI18N
        putSupport("input", "formaction", FILE_NAME_SUPPORT); //NOI18N
        putSupport("command", "icon", FILE_NAME_SUPPORT); //NOI18N
        putSupport(null, "itemprop", FILE_NAME_SUPPORT); //NOI18N
        putSupport(null, "itemtype", FILE_NAME_SUPPORT); //NOI18N
        putSupport("html", "manifest", FILE_NAME_SUPPORT); //NOI18N
        putSupport("video", "poster", FILE_NAME_SUPPORT); //NOI18N

        putSupport("script", "type", CONTENT_TYPE_SUPPORT); //NOI18N
        putSupport("style", "type", CONTENT_TYPE_SUPPORT); //NOI18N
        putSupport("link", "type", CONTENT_TYPE_SUPPORT); //NOI18N

        putSupport("form", "autocomplete", ON_OFF_SUPPORT); //NOI18N
        putSupport("input", "autocomplete", ON_OFF_SUPPORT); //NOI18N

        putSupport(null, "contenteditable", TRUE_FALSE_SUPPORT); //NOI18N

        putSupport("button", "formenctype", FORMENCTYPE_SUPPORT); //NOI18N
        putSupport("input", "formenctype", FORMENCTYPE_SUPPORT); //NOI18N

        putSupport("button", "formmethod", FORMMETHOD_SUPPORT); //NOI18N
        putSupport("input", "formmethod", FORMMETHOD_SUPPORT); //NOI18N

        putSupport("audio", "preload", PRELOAD_SUPPORT); //NOI18N
        putSupport("video", "preload", PRELOAD_SUPPORT); //NOI18N

        putSupport(null, "spellcheck", TRUE_FALSE_SUPPORT); //NOI18N

        putSupport("th", "scope", SCOPE_SUPPORT); //NOI18N

        putSupport("area", "shape", SHAPE_SUPPORT); //NOI18N

        putSupport("button", "type", BUTTON_TYPE_SUPPORT); //NOI18N
        putSupport("command", "type", COMMAND_TYPE_SUPPORT); //NOI18N
        putSupport("menu", "type", MENU_TYPE_SUPPORT); //NOI18N

        putSupport("textarea", "wrap", WRAP_SUPPORT); //NOI18N

        putSupport("input", "type", INPUT_TYPE_SUPPORT); //NOI18N
        
        putSupport(null, "rel", LINK_TYPES_SUPPORT);
        
        putSupport(null, "dir", DIR_SUPPORT);
    }

    private static void putSupport(String tag, String attr, ValueCompletion<HtmlCompletionItem> support) {
        if (tag == null) {
            ALL_TAG_SUPPORTS.put(attr, support);
        } else {
            Map<String, ValueCompletion<HtmlCompletionItem>> map = SUPPORTS.get(tag);
            if (map == null) {
                map = new HashMap<>();
                SUPPORTS.put(tag, map);
            }
            map.put(attr, support);
        }
    }

    public static Map<String, ValueCompletion<HtmlCompletionItem>> getSupportsForTag(String tag) {
        return SUPPORTS.get(tag.toLowerCase(Locale.ENGLISH));
    }

    public static ValueCompletion<HtmlCompletionItem> getSupport(String tag, String attr) {
        Map<String, ValueCompletion<HtmlCompletionItem>> map = getSupportsForTag(tag);
        if (map == null) {
            return ALL_TAG_SUPPORTS.get(attr);
        } else {
            ValueCompletion completion = map.get(attr.toLowerCase(Locale.ENGLISH));
            return completion == null ? ALL_TAG_SUPPORTS.get(attr) : completion;
        }
    }

    public static class ValuesSetSupport implements ValueCompletion {

        private String[] values;

        public ValuesSetSupport(String[] values) {
            this.values = values;
        }

        public String[] getTags() {
            return values;
        }
        
        @Override
        public List<HtmlCompletionItem> getItems(FileObject file, int offset, String valuePart) {
            //linear search, too little items, no problem
            List<HtmlCompletionItem> items = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                if (values[i].startsWith(valuePart)) {
                    items.add(HtmlCompletionItem.createAttributeValue(values[i], offset));
                }
            }
            return items;
        }
    }

    public static class FilenameSupport extends FileReferenceCompletion<HtmlCompletionItem> {

        @Override
        public HtmlCompletionItem createFileItem(FileObject file, int anchor) {
            return HtmlCompletionItem.createFileCompletionItem(file, anchor);
        }

        @Override
        public HtmlCompletionItem createGoUpItem(int anchor, Color color, ImageIcon icon) {
            return HtmlCompletionItem.createGoUpFileCompletionItem(anchor, color, icon); // NOI18N
        }
    }
}
