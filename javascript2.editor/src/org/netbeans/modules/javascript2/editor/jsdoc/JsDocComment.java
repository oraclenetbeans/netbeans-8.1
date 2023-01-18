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
package org.netbeans.modules.javascript2.editor.jsdoc;

import java.util.*;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.doc.spi.DocParameter;
import org.netbeans.modules.javascript2.editor.doc.spi.JsComment;
import org.netbeans.modules.javascript2.editor.doc.spi.JsModifier;
import org.netbeans.modules.javascript2.editor.jsdoc.model.DeclarationElement;
import org.netbeans.modules.javascript2.editor.jsdoc.model.DescriptionElement;
import org.netbeans.modules.javascript2.editor.jsdoc.model.JsDocElement;
import org.netbeans.modules.javascript2.editor.jsdoc.model.JsDocElementType;
import org.netbeans.modules.javascript2.editor.jsdoc.model.JsDocElementUtils;
import org.netbeans.modules.javascript2.editor.jsdoc.model.NamedParameterElement;
import org.netbeans.modules.javascript2.editor.jsdoc.model.UnnamedParameterElement;
import org.netbeans.modules.javascript2.editor.model.Type;

/**
 * Represents block of jsDoc comment which contains particular {@link JsDocTag}s.
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public class JsDocComment extends JsComment {

    private final Map<JsDocElementType, List<JsDocElement>> tags = new EnumMap<JsDocElementType, List<JsDocElement>>(JsDocElementType.class);
    private final JsDocCommentType type;

    /**
     * Creates new {@code JsComment} with given parameters.
     *
     * @param offsetRange offset range of the comment
     * @param type comment {@code JsDocCommentType}
     * @param elements list of tags contained in this block, never {@code null}
     */
    public JsDocComment(OffsetRange offsetRange, JsDocCommentType type, List<JsDocElement> elements) {
        super(offsetRange);
        this.type = type;
        initTags(elements);
    }

    /**
     * Gets type of the jsDoc block comment.
     * @return type of the jsDoc block comment
     */
    public JsDocCommentType getType() {
        return type;
    }

    @Override
    public List<String> getSummary() {
        List<String> summaries = new LinkedList<String>();
        for (JsDocElement jsDocElement : getTagsForTypes(
                new JsDocElementType[]{JsDocElementType.DESCRIPTION, JsDocElementType.CONTEXT_SENSITIVE})) {
            summaries.add(((DescriptionElement) jsDocElement).getDescription());
        }

        // issue #224759 - class description should appear in the documentation window
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.CLASS)) {
            summaries.add(((DescriptionElement) jsDocElement).getDescription());
        }
        return summaries;
    }

    @Override
    public List<String> getSyntax() {
        List<String> syntaxes = new LinkedList<String>();
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.SYNTAX)) {
            syntaxes.add(((DescriptionElement) jsDocElement).getDescription());
        }
        return syntaxes;
    }

    @Override
    public DocParameter getReturnType() {
        for (JsDocElement jsDocElement : getTagsForTypes(
                new JsDocElementType[]{JsDocElementType.RETURN, JsDocElementType.RETURNS})) {
            return  (DocParameter) jsDocElement;
        }
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.TYPE)) {
            return UnnamedParameterElement.create(
                    jsDocElement.getType(),
                    JsDocElementUtils.parseTypes(((DeclarationElement) jsDocElement).getDeclaredType().getType(), ((DeclarationElement) jsDocElement).getDeclaredType().getOffset()),
                    "");
        }
        return null;
    }

    @Override
    public List<DocParameter> getParameters() {
        List<DocParameter> params = new LinkedList<DocParameter>();
        for (JsDocElement jsDocElement : getTagsForTypes(
                new JsDocElementType[]{JsDocElementType.PARAM, JsDocElementType.ARGUMENT})) {
            params.add((NamedParameterElement) jsDocElement);
        }
        return params;
    }

    @Override
    public String getDeprecated() {
        List<? extends JsDocElement> deprecatedTags = getTagsForType(JsDocElementType.DEPRECATED);
        if (deprecatedTags.isEmpty()) {
            return null;
        } else {
            return ((DescriptionElement) deprecatedTags.get(0)).getDescription();
        }
    }

    @Override
    public Set<JsModifier> getModifiers() {
        Set<JsModifier> modifiers = EnumSet.noneOf(JsModifier.class);
        for (JsDocElement jsDocElement : getTagsForTypes(new JsDocElementType[]{
                JsDocElementType.PRIVATE, JsDocElementType.PUBLIC, JsDocElementType.STATIC})) {
            modifiers.add(JsModifier.fromString(jsDocElement.getType().toString().substring(1)));
        }
        return modifiers;
    }

    @Override
    public List<DocParameter> getThrows() {
        List<DocParameter> throwsEntries = new LinkedList<DocParameter>();
        for (JsDocElement exceptionTag : getTagsForType(JsDocElementType.THROWS)) {
            throwsEntries.add((UnnamedParameterElement) exceptionTag);
        }
        return throwsEntries;
    }

    @Override
    public List<Type> getExtends() {
        List<Type> extendsEntries = new LinkedList<Type>();
        for (JsDocElement extend : getTagsForTypes(
                new JsDocElementType[]{JsDocElementType.EXTENDS, JsDocElementType.AUGMENTS})) {
            DeclarationElement ident = (DeclarationElement) extend;
            extendsEntries.add(ident.getDeclaredType());
        }
        return extendsEntries;
    }

    @Override
    public List<String> getSee() {
        List<String> sees = new LinkedList<String>();
        for (JsDocElement extend : getTagsForType(JsDocElementType.SEE)) {
            DescriptionElement element = (DescriptionElement) extend;
            sees.add(element.getDescription());
        }
        return sees;
    }

    @Override
    public String getSince() {
        List<? extends JsDocElement> since = getTagsForType(JsDocElementType.SINCE);
        if (since.isEmpty()) {
            return null;
        } else {
            return ((DescriptionElement) since.get(0)).getDescription();
        }
    }

    @Override
    public boolean isClass() {
        return !getTagsForTypes(new JsDocElementType[]{JsDocElementType.CLASS, JsDocElementType.CONSTRUCTOR,
            JsDocElementType.CONSTRUCTS}).isEmpty();
    }

//    @Override
//    public List<String> getAuthor() {
//        List<String> authors = new LinkedList<String>();
//        for (JsDocElement extend : getTagsForType(JsDocElementType.AUTHOR)) {
//            DescriptionElement element = (DescriptionElement) extend;
//            authors.add(element.getDescription());
//        }
//        return authors;
//    }
//
//    @Override
//    public String getVersion() {
//        List<? extends JsDocElement> version = getTagsForType(JsDocElementType.VERSION);
//        if (version.isEmpty()) {
//            return null;
//        } else {
//            return ((DescriptionElement) version.get(0)).getDescription();
//        }
//    }

    @Override
    public List<String> getExamples() {
        List<String> examples = new LinkedList<String>();
        for (JsDocElement extend : getTagsForType(JsDocElementType.EXAMPLE)) {
            DescriptionElement element = (DescriptionElement) extend;
            examples.add(element.getDescription());
        }
        return examples;
    }

    private void initTags(List<JsDocElement> elements) {
        for (JsDocElement jsDocElement : elements) {
            List<JsDocElement> list = tags.get(jsDocElement.getType());
            if (list == null) {
                list = new LinkedList<JsDocElement>();
                tags.put(jsDocElement.getType(), list);
            }
            tags.get(jsDocElement.getType()).add(jsDocElement);
        }
    }

    /**
     * Gets list of all {@code JsDocTag}s.
     * <p>
     * Should be used just in testing use cases.
     * @return list of {@code JsDocTag}s
     */
    protected List<? extends JsDocElement> getTags() {
        List<JsDocElement> allTags = new LinkedList<JsDocElement>();
        for (List<JsDocElement> list : tags.values()) {
            allTags.addAll(list);
        }
        return allTags;
    }

    /**
     * Gets list of {@code JsDocTag}s of given type.
     * @return list of {@code JsDocTag}s
     */
    public List<? extends JsDocElement> getTagsForType(JsDocElementType type) {
        List<JsDocElement> tagsForType = tags.get(type);
        return tagsForType == null ? Collections.<JsDocElement>emptyList() : tagsForType;
    }

    /**
     * Gets list of {@code JsDocTag}s of given types.
     * @return list of {@code JsDocTag}s
     */
    public List<? extends JsDocElement> getTagsForTypes(JsDocElementType[] types) {
        List<JsDocElement> list = new LinkedList<JsDocElement>();
        for (JsDocElementType type : types) {
            list.addAll(getTagsForType(type));
        }
        return list;
    }

    @Override
    public List<DocParameter> getProperties() {
        List<DocParameter> properties = new LinkedList<DocParameter>();
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.PROPERTY)) {
            properties.add((NamedParameterElement) jsDocElement);
        }
        return properties;
    }

    @Override
    public DocParameter getDefinedType() {
        List<DocParameter> definedTypes = new LinkedList<DocParameter>();
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.TYPEDEF)) {
            definedTypes.add((NamedParameterElement) jsDocElement);
        }
        if (definedTypes.isEmpty()) {
            return null;
        }
        return definedTypes.get(0);
    }

    @Override
    public List<Type> getTypes() {
        List<Type> properties = new LinkedList<Type>();
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.TYPE)) {
            properties.add(((DeclarationElement) jsDocElement).getDeclaredType());
        }
        return properties;
    }

    @Override
    public Type getCallBack() {
        List<Type> callbacks = new LinkedList<Type>();
        for (JsDocElement jsDocElement : getTagsForType(JsDocElementType.CALLBACK)) {
            callbacks.add(((DeclarationElement) jsDocElement).getDeclaredType());
        }
        return callbacks.isEmpty() ? null : callbacks.get(0);
    }
    
    
}