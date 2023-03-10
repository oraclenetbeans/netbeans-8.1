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
package org.netbeans.modules.javascript2.editor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.*;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.csl.spi.support.CancelSupport;
import org.netbeans.modules.html.editor.lib.api.model.HtmlTagAttribute;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.index.IndexedElement;
import org.netbeans.modules.javascript2.editor.model.JsElement;
import org.netbeans.modules.javascript2.editor.model.JsFunction;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.Type;
import org.netbeans.modules.javascript2.editor.model.TypeUsage;
import org.netbeans.modules.javascript2.editor.model.impl.ModelUtils;
import org.netbeans.modules.javascript2.editor.model.impl.TypeUsageImpl;
import org.netbeans.modules.javascript2.editor.options.OptionsUtils;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.javascript2.editor.spi.CompletionContext;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Petr Pisl
 */
public class JsCompletionItem implements CompletionProposal {
    
    protected final CompletionRequest request;
    protected final ElementHandle element;
    
    JsCompletionItem(ElementHandle element, CompletionRequest request) {
        this.element = element;
        this.request = request;
    }
    
    @Override
    public int getAnchorOffset() {
        return LexUtilities.getLexerOffset((JsParserResult)request.info, request.anchor);
    }

    @Override
    public ElementHandle getElement() {
        return element;
    }

    @Override
    public String getName() {
        return element.getName();
    }

    @Override
    public String getInsertPrefix() {
        return element.getName();
    }

    @Override
    public String getSortText() {
        StringBuilder sb = new StringBuilder();
        if (element != null) {
            FileObject sourceFo = request.result.getSnapshot().getSource().getFileObject();
            FileObject elementFo = element.getFileObject();
            if (elementFo != null && sourceFo != null && sourceFo.equals(elementFo)) {
                sb.append("1");     //NOI18N
            } else {
                if (OffsetRange.NONE.equals(element.getOffsetRange(request.result))) {
                    sb.append("8");
                } else {
                    sb.append("9");     //NOI18N
                }
            }
        }
        sb.append(getName());    
        return sb.toString();
    }
    
    protected boolean isDeprecated() {
        return element.getModifiers().contains(Modifier.DEPRECATED);
    }
    
    @Override
    public String getLhsHtml(HtmlFormatter formatter) {
        formatName(formatter);
        return formatter.getText();
    }

    protected void formatName(HtmlFormatter formatter) {
        if (isDeprecated()) {
            formatter.deprecated(true);
            formatter.appendText(getName());
            formatter.deprecated(false);
        } else {
            formatter.appendText(getName());
        }
    }
    
    @Messages("JsCompletionItem.lbl.js.platform=JS Platform")
    @Override
    public String getRhsHtml(HtmlFormatter formatter) {
        String location = null;
        if (element instanceof JsElement) {
            JsElement jsElement = (JsElement) element;
            if (jsElement.isPlatform()) {
                location = Bundle.JsCompletionItem_lbl_js_platform();
            } else if (jsElement.getSourceLabel() != null) {
                location = jsElement.getSourceLabel();
            }
        }
        if (location == null) {
            location = getFileNameURL();
        }
        if (location == null) {
            return null;
        }

        formatter.reset();
        boolean isgues = OffsetRange.NONE.equals(element.getOffsetRange(request.result));
        if (isgues) {
            formatter.appendHtml("<font color=#999999>");
        }
        formatter.appendText(location);
        if (isgues) {
            formatter.appendHtml("</font>");
        }
        return formatter.getText();
    }

    @Override
    public ElementKind getKind() {
        return element.getKind();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public Set<Modifier> getModifiers() {
        Set<Modifier> modifiers = (getElement() == null || getElement().getModifiers().isEmpty() ? Collections.EMPTY_SET : EnumSet.copyOf(getElement().getModifiers()));
        if (modifiers.contains(Modifier.PRIVATE) && (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED))) {
            modifiers.remove(Modifier.PUBLIC);
            modifiers.remove(Modifier.PROTECTED);
        }
        return modifiers;
    }

    @Override
    public boolean isSmart() {
        // TODO implemented properly
        return false;
    }

    @Override
    public int getSortPrioOverride() {
        int order = 100;
        if (element != null && element instanceof JsElement) {
            if (((JsElement)element).isPlatform()) {
                if (ModelUtils.PROTOTYPE.equals(element.getName())) { //NOI18N
                    order = 1;
                } else {
                    order = 0;
                }
            }
            if (OffsetRange.NONE.equals(element.getOffsetRange(request.result))) {
                order = 120;
            }
        }
        return order;
    }

    @Override
    public String getCustomInsertTemplate() {
        return null;
    }

    @CheckForNull
    public final String getFileNameURL() {
        ElementHandle elem = getElement();
        if (elem == null) {
            return null;
        }
        FileObject fo = elem.getFileObject();
        if (fo != null) {
            return fo.getNameExt();
        }
        return getName();
     }
    
    public static class CompletionRequest {
        public int anchor;
        public JsParserResult result;
        public ParserResult info;
        public String prefix;
        public CompletionContext completionContext;
        public boolean addHtmlTagAttributes;
        public CancelSupport cancelSupport;
    }
    
    private static  ImageIcon priviligedIcon = null;
    
    public static class JsFunctionCompletionItem extends JsCompletionItem {
        
        private final Set<String> returnTypes;
        private final Map<String, Set<String>> parametersTypes;
        JsFunctionCompletionItem(ElementHandle element, CompletionRequest request, Set<String> resolvedReturnTypes, Map<String, Set<String>> parametersTypes) {
            super(element, request);
            this.returnTypes = resolvedReturnTypes != null ? resolvedReturnTypes : Collections.EMPTY_SET;
            this.parametersTypes = parametersTypes != null ? parametersTypes : Collections.EMPTY_MAP;
        }

        @Override
        public String getLhsHtml(HtmlFormatter formatter) {
            formatter.setMaxLength(OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).getCodeCompletionItemSignatureWidth());
            formatter.emphasis(true);
            formatName(formatter);
            formatter.emphasis(false);
            if (!asObject()) {
                formatter.appendText("(");  //NOI18N
                appendParamsStr(formatter);
                formatter.appendText(")");  //NOI18N
                appendReturnTypes(formatter);
            }
            return formatter.getText();
        }

        private void appendParamsStr(HtmlFormatter formatter){
            for (Iterator<Map.Entry<String, Set<String>>> it = parametersTypes.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Set<String>> entry = it.next();
                formatter.parameters(true);
                formatter.appendText(entry.getKey());
                formatter.parameters(false);
                Collection<String> types = entry.getValue();
                if (!types.isEmpty()) {
                    formatter.type(true);
                    formatter.appendText(": ");  //NOI18N
                    for (Iterator<String> itTypes = types.iterator(); itTypes.hasNext();) {
                        formatter.appendText(itTypes.next());
                        if (itTypes.hasNext()) {
                            formatter.appendText("|");   //NOI18N
                        }
                    }
                    formatter.type(false);
                }
                if (it.hasNext()) {
                    formatter.appendText(", ");  //NOI18N
                }    
            }
        }

        private void appendReturnTypes(HtmlFormatter formatter) {
            if (!returnTypes.isEmpty()) {
                formatter.appendText(": "); //NOI18N
                formatter.type(true);
                for (Iterator<String> it = returnTypes.iterator(); it.hasNext();) {
                    formatter.appendText(it.next());
                    if (it.hasNext()) {
                        formatter.appendText("|"); //NOI18N
                    }
                }
                formatter.type(false);
            }
        }

        @Override
        public String getCustomInsertTemplate() {
            StringBuilder template = new StringBuilder();
            template.append(getName());
            if (!asObject()) {
                if (parametersTypes.isEmpty()) {
                    template.append("()${cursor}");     //NOI18N
                } else {
                    template.append("(${cursor})");     //NOI18N
                }
            } else {
                template.append("${cursor}");       //NOI18N
            }
            return template.toString();
        }

        @Override
        public ImageIcon getIcon() {
            if (getModifiers().contains(Modifier.PROTECTED)) {
                if(priviligedIcon == null) {
                    priviligedIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/javascript2/editor/resources/methodPriviliged.png")); //NOI18N
                }
                return priviligedIcon;
            }
            return super.getIcon(); //To change body of generated methods, choose Tools | Templates.
        }
        
        private boolean isAfterNewKeyword() {
            boolean isAfterNew = false;
            Snapshot snapshot = request.result.getSnapshot();
            int offset = request.anchor;
            TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(snapshot, snapshot.getOriginalOffset(offset));
            if (ts != null) {
                ts.move(offset);
                if (ts.moveNext()) {
                    Token<? extends JsTokenId> token = LexUtilities.findPrevious(ts, Arrays.asList(JsTokenId.IDENTIFIER, JsTokenId.OPERATOR_DOT, JsTokenId.BLOCK_COMMENT, JsTokenId.WHITESPACE, JsTokenId.LINE_COMMENT, JsTokenId.EOL));
                    if (token.id() == JsTokenId.KEYWORD_NEW) {
                        isAfterNew = true;
                    }
                }
            }
            return isAfterNew;
        }
        
        /**
         * 
         * @return true if the element should be treated as an object or function in the context
         */
        private boolean asObject() {
            boolean result = false;
            char firstChar = getName().charAt(0);
            JsElement.Kind jsKind = null;
            if (element instanceof JsElement) {
                jsKind = ((JsElement)element).getJSKind();
            }
            if ((jsKind != null && jsKind == JsElement.Kind.CONSTRUCTOR) || Character.isUpperCase(firstChar)) {
                boolean isAfterNew = isAfterNewKeyword();
                if (!isAfterNew) {
                    // check return types, whether it can be really constructor
                    for (String type : returnTypes) {
                        if (type.endsWith(element.getName())) {
                            return true;
                        }
                    }
                    if (returnTypes.isEmpty()) {
                        result = true;
                    } else if (returnTypes.size() == 1) {
                        String type = returnTypes.iterator().next(); 
                        firstChar = type.charAt(0);
                        if (Character.isUpperCase(firstChar) && !(Type.NUMBER.equals(type) || Type.BOOLEAN.equals(type) 
                                || Type.STRING.equals(type) || Type.ARRAY.equals(type))) {
                            result = true;
                        }
                    }
                }
            }
            return result;
        }
    }

    public static class JsCallbackCompletionItem extends JsCompletionItem {
        private static ImageIcon callbackIcon = null;
        private IndexedElement.FunctionIndexedElement function;
                
        public JsCallbackCompletionItem(IndexedElement.FunctionIndexedElement element, CompletionRequest request) {
            super(element, request);
            function = element;
        }
        
        @Override
        public ImageIcon getIcon() {
            if (callbackIcon == null) {
                callbackIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/javascript2/editor/resources/methodCallback.png")); //NOI18N
            }
            return callbackIcon;
        }
        
        @Override
        public String getLhsHtml(HtmlFormatter formatter) {
            formatter.setMaxLength(OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).getCodeCompletionItemSignatureWidth());
            formatter.name(ElementKind.KEYWORD, true);
            formatter.appendText("function");   //NOI18N
            formatter.name(ElementKind.KEYWORD, false);
            formatter.appendText(" (");  //NOI18N
            appendParamsStr(formatter);
            formatter.appendText(")");  //NOI18N
            return formatter.getText();
        }

        @Override
        public int getSortPrioOverride() {
            return 90;      // display as first items?
        }
    
        
        private void appendParamsStr(HtmlFormatter formatter){
            for (Iterator<Map.Entry<String, Collection<String>>> it = function.getParameters().entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Collection<String>> entry = it.next();
                formatter.parameters(true);
                formatter.appendText(entry.getKey());
                formatter.parameters(false);
                Collection<String> types = entry.getValue();
                if (!types.isEmpty()) {
                    formatter.type(true);
                    formatter.appendText(": ");  //NOI18N
                    for (Iterator<String> itTypes = types.iterator(); itTypes.hasNext();) {
                        formatter.appendText(itTypes.next());
                        if (itTypes.hasNext()) {
                            formatter.appendText("|");   //NOI18N
                        }
                    }
                    formatter.type(false);
                }
                if (it.hasNext()) {
                    formatter.appendText(", ");  //NOI18N
                }    
            }
        }
        
        @Override
        public String getCustomInsertTemplate() {
            StringBuilder template = new StringBuilder();
            template.append(" \n /** ");    //NOI18N
            for (Iterator<Map.Entry<String, Collection<String>>> it = function.getParameters().entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Collection<String>> entry = it.next();
                Collection<String> types = entry.getValue();
                template.append("\n * @param {");//NOI18N
                if (!types.isEmpty()) {
                    for (Iterator<String> itTypes = types.iterator(); itTypes.hasNext();) {
                        template.append(itTypes.next());
                        if (itTypes.hasNext()) {
                            template.append("|");   //NOI18N
                        }
                    }
                } else {
                    template.append("Object");//NOI18N
                }
                template.append("} ");//NOI18N
                template.append(entry.getKey());
            }
            template.append("\n */");//NOI18N
            template.append("\nfunction (");//NOI18N
            for (Iterator<Map.Entry<String, Collection<String>>> it = function.getParameters().entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Collection<String>> entry = it.next();
                template.append(entry.getKey());
                if (it.hasNext()) {
                    template.append(", ");  //NOI18N
                }
            }
            template.append(") {\n ${cursor}\n}");//NOI18N
            return template.toString();
        }

        @Override
        public String getName() {
            return "function";
        }

    }
    
    static class KeywordItem extends JsCompletionItem {
        private static  ImageIcon keywordIcon = null;
        private String keyword = null;

        public KeywordItem(String keyword, CompletionRequest request) {
            super(null, request);
            this.keyword = keyword;
        }

        @Override
        public String getName() {
            return keyword;
        }

        @Override
        public String getLhsHtml(HtmlFormatter formatter) {
            formatter.name(getKind(), true);
            formatter.appendText(getName());
            formatter.name(getKind(), false);
            return formatter.getText();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.KEYWORD;
        }

        @Override
        public String getRhsHtml(HtmlFormatter formatter) {
            return null;
        }

        @Override
        public ImageIcon getIcon() {
            if (keywordIcon == null) {
                keywordIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/javascript2/editor/resources/javascript.png")); //NOI18N
            }
            return keywordIcon;
        }
        
        @Override
        public String getInsertPrefix() {
            return getName();
        }
        
        @Override
        public String getCustomInsertTemplate() {
            StringBuilder builder = new StringBuilder();
            
            JsKeyWords.CompletionType type = JsKeyWords.KEYWORDS.get(getName());
            if (type == null) {
                return getName();
            }
            
            switch(type) {
                case SIMPLE:
                    builder.append(getName());
                    break;
                case ENDS_WITH_SPACE:
                    builder.append(getName());
                    builder.append(" ${cursor}"); //NOI18N
                    break;
                case CURSOR_INSIDE_BRACKETS:
                    builder.append(getName());
                    builder.append("(${cursor})"); //NOI18N
                    break;
                case ENDS_WITH_CURLY_BRACKETS:
                    builder.append(getName());
                    builder.append(" {${cursor}}"); //NOI18N
                    break;
                case ENDS_WITH_SEMICOLON:
                    builder.append(getName());
                    CharSequence text = request.info.getSnapshot().getText();
                    int index = request.anchor + request.prefix.length();
                    if (index == text.length() || ';' != text.charAt(index)) { //NOI18N
                        builder.append(";"); //NOI18N
                    }
                    break;
                case ENDS_WITH_COLON:
                    builder.append(getName());
                    builder.append(" ${cursor}:"); //NOI18N
                    break;
                case ENDS_WITH_DOT:
                    builder.append(getName());
                    builder.append(".${cursor}"); //NOI18N
                    break;
                default:
                    assert false : type.toString();
                    break;
            }
            return builder.toString();
        }

        @Override
        public int getSortPrioOverride() {
            return 130;
        }
    }

    static class CssCompletionItem extends JsCompletionItem {
        
        private static ImageIcon cssIcon = null;
        
        private final String name;
        
        public CssCompletionItem(String name, CompletionRequest request) {
            super(null, request);
            this.name = name;
        }
        
        @Override
        public ImageIcon getIcon() {
            if (cssIcon == null) {
                cssIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/javascript2/jquery/resources/style_sheet_16.png")); //NOI18N
            }
            return cssIcon;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getInsertPrefix() {
            return getName();
        }
        
        @Override
        public String getLhsHtml(HtmlFormatter formatter) {
            formatter.reset();
            formatter.appendText(getName());
            return formatter.getText();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.RULE;
        }

        @Override
        public String getRhsHtml(HtmlFormatter formatter) {
            return null;
        }
        
    }
    
    public static class JsHtmlAttributeItem extends JsCompletionItem {
        
        private final HtmlTagAttribute attr;
        
        public JsHtmlAttributeItem(HtmlTagAttribute attr, CompletionRequest request) {
            super(new HtmlAttrElement(attr), request);
            this.attr = attr;
        }
        
        @Override
        public String getName() {
            return attr.getName();
        }
        
        @Override
        public String getInsertPrefix() {
            return getName();
        }
        
        @Override
        public String getLhsHtml(HtmlFormatter formatter) {
            formatter.reset();
            formatter.appendText(getName());
            return formatter.getText();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.ATTRIBUTE;
        }

        @Messages("JsCompletionItem.lbl.html.attribute=HTML Attribute")
        @Override
        public String getRhsHtml(HtmlFormatter formatter) {
            formatter.reset();
            formatter.appendHtml("<font color=#999999>");
            formatter.appendText(Bundle.JsCompletionItem_lbl_html_attribute());
            formatter.appendHtml("</font>");
            return formatter.getText();
        }
        
        private static class HtmlAttrElement extends SimpleDocElement {
            private final HtmlTagAttribute attribute;
            
            public HtmlAttrElement(HtmlTagAttribute attribute) {
                super(attribute.getName(), ElementKind.ATTRIBUTE);
                this.attribute = attribute;
            }

            @Override
            public String getDocumentation() {
                String content = attribute.getHelp().getHelpContent();
                if (content == null) {
                    if (attribute.getHelp().getHelpResolver() != null && attribute.getHelp().getHelpURL() != null) {
                        content = attribute.getHelp().getHelpResolver().getHelpContent(attribute.getHelp().getHelpURL());
                    }
                }
                return content;
            }
        }
    }
    
    public static class JsPropertyCompletionItem extends JsCompletionItem {

        private final Set<String> resolvedTypes;
        
        JsPropertyCompletionItem(ElementHandle element, CompletionRequest request, Set<String> resolvedTypes) {
            super(element, request);
            this.resolvedTypes = resolvedTypes != null ? resolvedTypes : Collections.EMPTY_SET;
        }

        @Override
        public String getLhsHtml(HtmlFormatter formatter) {
            formatName(formatter);
            if (!resolvedTypes.isEmpty()) {
                formatter.type(true);
                formatter.appendText(": ");  //NOI18N
                for (Iterator<String> it = resolvedTypes.iterator(); it.hasNext();) {
                    formatter.appendText(it.next());
                    if (it.hasNext()) {
                        formatter.appendText("|");   //NOI18N
                    }
                }
                formatter.type(false);
            }
            return formatter.getText();
        }

        @Override
        public String getCustomInsertTemplate() {
            if (request.completionContext == CompletionContext.OBJECT_PROPERTY_NAME) {
                return getName() + ": ${cursor}"; // NOI18N
            }
            return super.getCustomInsertTemplate(); //To change body of generated methods, choose Tools | Templates.
        }
        
    }

    public static class Factory {
        
        public static void create( Map<String, List<JsElement>> items, CompletionRequest request, List<CompletionProposal> result) {
            // This maps unresolved types to the display name of the resolved type. 
            // It should save time to not resolve one type more times
            HashMap<String, Set<String>> resolvedTypes = new HashMap<String, Set<String>>();

            for (Map.Entry<String, List<JsElement>> entry: items.entrySet()) {

                // this helps to eleminate items that will look as the same items in the cc
                HashMap<String, JsCompletionItem> signatures = new HashMap<String, JsCompletionItem>();
                for (JsElement element : entry.getValue()) {
                    switch (element.getJSKind()) {
                        case CONSTRUCTOR:
                        case FUNCTION:
                        case METHOD:
                            Set<String> returnTypes = new HashSet<String>();
                            HashMap<String, Set<String>> allParameters = new LinkedHashMap<String, Set<String>>();
                            if (element instanceof JsFunction) {
                                // count return types
                                Collection<TypeUsage> resolveTypes = ModelUtils.resolveTypes(((JsFunction) element).getReturnTypes(), (JsParserResult)request.info,
                                        OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).autoCompletionTypeResolution(), false);
                                returnTypes.addAll(Utils.getDisplayNames(resolveTypes));
                                // count parameters type
                                for (JsObject jsObject : ((JsFunction) element).getParameters()) {
                                    Set<String> paramTypes = new HashSet<String>();
                                    for (TypeUsage type : jsObject.getAssignmentForOffset(jsObject.getOffset() + 1)) {
                                        Set<String> resolvedType = resolvedTypes.get(type.getType());
                                        if (resolvedType == null) {
                                            resolvedType = new HashSet(1);
                                            String displayName = Utils.getDisplayName(type);
                                            if (!displayName.isEmpty()) {
                                                resolvedType.add(displayName);
                                            }
                                            resolvedTypes.put(type.getType(), resolvedType);
                                        }
                                        paramTypes.addAll(resolvedType);
                                    }
                                    allParameters.put(jsObject.getName(), paramTypes);
                                }
                            } else if (element instanceof IndexedElement.FunctionIndexedElement) {
                                // count return types
                                HashSet<TypeUsage> returnTypeUsages = new HashSet<TypeUsage>();
                                for (String type : ((IndexedElement.FunctionIndexedElement) element).getReturnTypes()) {
                                    returnTypeUsages.add(new TypeUsageImpl(type, -1, false));
                                }
                                Collection<TypeUsage> resolveTypes = ModelUtils.resolveTypes(returnTypeUsages, (JsParserResult)request.info,
                                        OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).autoCompletionTypeResolution(), false);
                                returnTypes.addAll(Utils.getDisplayNames(resolveTypes));
                                // count parameters type
                                LinkedHashMap<String, Collection<String>> parameters = ((IndexedElement.FunctionIndexedElement) element).getParameters();
                                for (Map.Entry<String, Collection<String>> paramEntry : parameters.entrySet()) {
                                    Set<String> paramTypes = new HashSet<String>();
                                    for (String type : paramEntry.getValue()) {
                                        Set<String> resolvedType = resolvedTypes.get(type);
                                        if (resolvedType == null) {
                                            resolvedType = new HashSet(1);
                                            String displayName = ModelUtils.getDisplayName(type);
                                            if (!displayName.isEmpty()) {
                                                resolvedType.add(displayName);
                                            }
                                            resolvedTypes.put(type, resolvedType);
                                        }
                                        paramTypes.addAll(resolvedType);
                                    }
                                    allParameters.put(paramEntry.getKey(), paramTypes);
                                }
                            }
                            // create signature
                            String signature = createFnSignature(entry.getKey(), allParameters, returnTypes);
                            if (!signatures.containsKey(signature)) {
                                JsCompletionItem item = new JsFunctionCompletionItem(element, request, returnTypes, allParameters);
                                signatures.put(signature, item);
                            }
                            break;
                        case PARAMETER:
                        case PROPERTY:
                        case PROPERTY_GETTER:
                        case PROPERTY_SETTER:
                        case FIELD:
                        case VARIABLE:
                            Set<String> typesToDisplay = new HashSet<String>();
                            Collection<? extends TypeUsage> assignment = null;
                            if (element instanceof JsObject) {
                                JsObject jsObject = (JsObject) element;
                                assignment = jsObject.getAssignments();
                            } else if (element instanceof IndexedElement) {
                                IndexedElement iElement = (IndexedElement) element;
                                assignment = iElement.getAssignments();
                            }
                            if (assignment != null && !assignment.isEmpty()) {
                                HashSet<TypeUsage> toResolve = new HashSet<TypeUsage>();
                                for (TypeUsage type : assignment) {
                                    if (type.isResolved()) {
                                        if (!Type.UNDEFINED.equals(type.getType())) {
                                            typesToDisplay.add(Utils.getDisplayName(type));
                                        }
                                    } else {
                                        Set<String> resolvedType = resolvedTypes.get(type.getType());
                                        if (resolvedType == null) {
                                            toResolve.clear();
                                            toResolve.add(type);
                                            resolvedType = new HashSet(1);
                                            Collection<TypeUsage> resolved = ModelUtils.resolveTypes(toResolve, request.result,
                                                    OptionsUtils.forLanguage(JsTokenId.javascriptLanguage()).autoCompletionTypeResolution(), false);
                                            for (TypeUsage rType : resolved) {
                                                String displayName = Utils.getDisplayName(rType);
                                                if (!displayName.isEmpty()) {
                                                    resolvedType.add(displayName);
                                                }
                                            }
                                            resolvedTypes.put(type.getType(), resolvedType);
                                        }
                                        typesToDisplay.addAll(resolvedType);
                                    }
                                }
                            }
                            // signatures
                            signature = element.getName() + ":" + createTypeSignature(typesToDisplay);
                            if (!signatures.containsKey(signature)) {
                                // add the item to the cc only if doesn't exist any similar
                                JsCompletionItem item = new JsPropertyCompletionItem(element, request, typesToDisplay);
                                signatures.put(signature, item);
                            }
                            break;
                        default:
                            signature = element.getName();
                            if (!signatures.containsKey(signature)) {
                                JsCompletionItem item = new JsCompletionItem(element, request);
                                signatures.put(signature, item);
                            }
                    }
                }
                for (JsCompletionItem item: signatures.values()) {
                    result.add(item);
                }
            }
        }
        
        private static String createFnSignature(String name, HashMap<String, Set<String>> params, Set<String> returnTypes) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append('(');
            for (Map.Entry<String, Set<String>> entry : params.entrySet()) {
                sb.append(entry.getKey()).append(':');
                sb.append(createTypeSignature(entry.getValue()));
                sb.append(',');
            }
            sb.append(')');
            sb.append(createTypeSignature(returnTypes));
            return sb.toString();
        }
        
        private static String createTypeSignature(Set<String> types) {
            StringBuilder sb = new StringBuilder();
            for(String name: types){
                sb.append(name).append('|');
            }
            return sb.toString();
        }
    }
    
    public abstract static class SimpleDocElement implements ElementHandle {

        private final String name;
        private final ElementKind kind;

        public SimpleDocElement(String name, ElementKind kind) {
            this.name = name;
            this.kind = kind;
        }
        
        
        @Override
        public FileObject getFileObject() {
            return null;
        }

        @Override
        public String getMimeType() {
            return "";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getIn() {
            return "";
        }

        @Override
        public ElementKind getKind() {
            return kind;
        }

        @Override
        public Set<Modifier> getModifiers() {
            return Collections.<Modifier>emptySet();
        }

        @Override
        public boolean signatureEquals(ElementHandle handle) {
            return false;
        }

        @Override
        public OffsetRange getOffsetRange(ParserResult result) {
            return OffsetRange.NONE;
        }
        
        abstract public String getDocumentation();
    }
}
