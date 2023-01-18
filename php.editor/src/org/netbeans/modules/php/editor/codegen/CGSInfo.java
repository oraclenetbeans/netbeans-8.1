/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.api.elements.ElementFilter;
import org.netbeans.modules.php.editor.api.elements.ElementTransformation;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.TreeElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.api.elements.TypeNameResolver;
import org.netbeans.modules.php.editor.codegen.CGSGenerator.GenWay;
import org.netbeans.modules.php.editor.elements.TypeNameResolverImpl;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.impl.Type;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.BodyDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Comment;
import org.netbeans.modules.php.editor.parser.astnodes.FieldsDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocBlock;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTypeNode;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTypeTag;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.SingleFieldDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TypeDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Petr Pisl
 */
public final class CGSInfo {

    private String className;
    // cotain the class consructor?
    private boolean hasConstructor;
    private final List<Property> properties;
    private final List<Property> instanceProperties;
    private final List<Property> possibleGetters;
    private final List<Property> possibleSetters;
    private final List<Property> possibleGettersSetters;
    private final List<MethodProperty> possibleMethods;
    private final JTextComponent textComp;
    /**
     * how to generate  getters and setters method name
     */
    private CGSGenerator.GenWay howToGenerate;
    private boolean generateDoc;
    private boolean fluentSetter;
    private boolean isPublicModifier;

    private CGSInfo(JTextComponent textComp) {
        properties = new ArrayList<>();
        instanceProperties = new ArrayList<>();
        possibleGetters = new ArrayList<>();
        possibleSetters = new ArrayList<>();
        possibleGettersSetters = new ArrayList<>();
        possibleMethods = new ArrayList<>();
        className = null;
        this.textComp = textComp;
        hasConstructor = false;
        this.generateDoc = true;
        fluentSetter = false;
        isPublicModifier = false;
        this.howToGenerate = CGSGenerator.GenWay.AS_JAVA;
    }

    public static CGSInfo getCGSInfo(JTextComponent textComp) {
        CGSInfo info = new CGSInfo(textComp);
        info.findPropertyInScope();
        return info;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Property> getInstanceProperties() {
        return instanceProperties;
    }

    public List<MethodProperty> getPossibleMethods() {
        return possibleMethods;
    }

    public List<Property> getPossibleGetters() {
        return possibleGetters;
    }

    public List<Property> getPossibleGettersSetters() {
        return possibleGettersSetters;
    }

    public List<Property> getPossibleSetters() {
        return possibleSetters;
    }

    public String getClassName() {
        return className;
    }

    public boolean hasConstructor() {
        return hasConstructor;
    }

    public GenWay getHowToGenerate() {
        return howToGenerate;
    }

    public void setHowToGenerate(GenWay howGenerate) {
        this.howToGenerate = howGenerate;
    }

    public boolean isGenerateDoc() {
        return generateDoc;
    }

    public void setGenerateDoc(boolean generateDoc) {
        this.generateDoc = generateDoc;
    }

    public boolean isFluentSetter() {
        return fluentSetter;
    }

    public void setFluentSetter(final boolean fluentSetter) {
        this.fluentSetter = fluentSetter;
    }

    public boolean isPublicModifier() {
        return isPublicModifier;
    }

    public void setPublicModifier(boolean isPublicModifier) {
        this.isPublicModifier = isPublicModifier;
    }

    public JTextComponent getComponent() {
        return textComp;
    }

    public TypeNameResolver createTypeNameResolver(MethodElement method) {
        TypeNameResolver result;
        if (method.getParameters().isEmpty()) {
            result = TypeNameResolverImpl.forNull();
        } else {
            Model model = ModelUtils.getModel(Source.create(getComponent().getDocument()), 300);
            if (model == null) {
                result = TypeNameResolverImpl.forNull();
            } else {
                result = CodegenUtils.createSmarterTypeNameResolver(method, model, getComponent().getCaretPosition());
            }
        }
        return result;
    }

    /**
     * Extract attributes and methods from caret enclosing class and initialize list of properties.
     */
    private void findPropertyInScope() {
        FileObject file = NavUtils.getFile(textComp.getDocument());
        if (file == null) {
            return;
        }
        try {
            ParserManager.parse(Collections.singleton(Source.create(textComp.getDocument())), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    initProperties(resultIterator);
                }
            });
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void initProperties(ResultIterator resultIterator) throws ParseException {
        PHPParseResult info = (PHPParseResult) resultIterator.getParserResult();
        if (info != null) {
            int caretOffset = textComp.getCaretPosition();
            TypeDeclaration typeDecl = findEnclosingClassOrTrait(info, caretOffset);
            if (typeDecl != null) {
                className = typeDecl.getName().getName();
                if (className != null) {
                    FileObject fileObject = info.getSnapshot().getSource().getFileObject();
                    Index index = ElementQueryFactory.getIndexQuery(info);
                    final ElementFilter forFilesFilter = ElementFilter.forFiles(fileObject);
                    QualifiedName fullyQualifiedName = VariousUtils.getFullyQualifiedName(
                            QualifiedName.create(className),
                            caretOffset,
                            info.getModel().getVariableScope(caretOffset));
                    Set<ClassElement> classes = forFilesFilter.filter(index.getClasses(NameKind.exact(fullyQualifiedName)));
                    for (ClassElement classElement : classes) {
                        ElementFilter forNotDeclared = ElementFilter.forExcludedElements(index.getDeclaredMethods(classElement));
                        final Set<MethodElement> accessibleMethods = new HashSet<>();
                        accessibleMethods.addAll(forNotDeclared.filter(index.getAccessibleMethods(classElement, classElement)));
                        accessibleMethods.addAll(
                                ElementFilter.forExcludedElements(accessibleMethods).filter(forNotDeclared.filter(index.getConstructors(classElement))));
                        accessibleMethods.addAll(
                                ElementFilter.forExcludedElements(accessibleMethods).filter(forNotDeclared.filter(index.getAccessibleMagicMethods(classElement))));
                        final Set<TypeElement> preferedTypes = forFilesFilter.prefer(ElementTransformation.toMemberTypes().transform(accessibleMethods));
                        final TreeElement<TypeElement> enclosingType = index.getInheritedTypesAsTree(classElement, preferedTypes);
                        final List<MethodProperty> methodProperties = new ArrayList<>();
                        final Set<MethodElement> methods = ElementFilter.forMembersOfTypes(preferedTypes).filter(accessibleMethods);
                        for (final MethodElement methodElement : methods) {
                            if (!methodElement.isFinal()) {
                                methodProperties.add(new MethodProperty(methodElement, enclosingType));
                            }
                        }
                        Collections.<MethodProperty>sort(methodProperties, MethodProperty.getComparator());
                        getPossibleMethods().addAll(methodProperties);
                    }
                }

                List<String> existingGetters = new ArrayList<>();
                List<String> existingSetters = new ArrayList<>();

                PropertiesVisitor visitor = new PropertiesVisitor(existingGetters, existingSetters, Utils.getRoot(info));
                visitor.scan(typeDecl);
                String propertyName;
                boolean existGetter, existSetter;
                for (Property property : getProperties()) {
                    propertyName = property.getName().toLowerCase();
                    existGetter = existingGetters.contains(propertyName);
                    existSetter = existingSetters.contains(propertyName);
                    if (!existGetter && !existSetter) {
                        getPossibleGettersSetters().add(property);
                        getPossibleGetters().add(property);
                        getPossibleSetters().add(property);
                    } else if (!existGetter) {
                        getPossibleGetters().add(property);
                    } else if (!existSetter) {
                        getPossibleSetters().add(property);
                    }
                }
            }
        }
    }

    /**
     * Find out class enclosing caret
     * @param info
     * @param offset caret offset
     * @return class declaration or null
     */
    private TypeDeclaration findEnclosingClassOrTrait(ParserResult info, int offset) {
        List<ASTNode> nodes = NavUtils.underCaret(info, offset);
        int count = nodes.size();
        if (count > 2) {  // the cursor has to be in class block see issue #142417
            ASTNode declaration = nodes.get(count - 2);
            ASTNode block = nodes.get(count - 1);
            if (block instanceof Block &&  (declaration instanceof ClassDeclaration || declaration instanceof TraitDeclaration)) {
                return (TypeDeclaration) declaration;
            }
        }
        return null;
    }

    private class PropertiesVisitor extends DefaultVisitor {

        private final List<String> existingGetters;
        private final List<String> existingSetters;
        private final Program program;

        public PropertiesVisitor(List<String> existingGetters, List<String> existingSetters, Program program) {
            this.existingGetters = existingGetters;
            this.existingSetters = existingSetters;
            this.program = program;
        }

        @Override
        public void visit(FieldsDeclaration node) {
            List<SingleFieldDeclaration> fields = node.getFields();
            for (SingleFieldDeclaration singleFieldDeclaration : fields) {
                Variable variable = singleFieldDeclaration.getName();
                if (variable != null && variable.getName() instanceof Identifier) {
                    String name = ((Identifier) variable.getName()).getName();
                    Property property = new Property(name, node.getModifier(), getPropertyType(singleFieldDeclaration));
                    if (!BodyDeclaration.Modifier.isStatic(node.getModifier())) {
                        getInstanceProperties().add(property);
                    }
                    getProperties().add(property);
                }
            }
        }

        private String getPropertyType(final ASTNode node) {
            String result = ""; //NOI18N
            Comment comment = Utils.getCommentForNode(program, node);
            if (comment instanceof PHPDocBlock) {
                result = getFirstTypeFromBlock((PHPDocBlock) comment);
            }
            return result;
        }

        private String getFirstTypeFromBlock(final PHPDocBlock phpDoc) {
            String result = ""; //NOI18N
            for (PHPDocTag pHPDocTag : phpDoc.getTags()) {
                if (pHPDocTag instanceof PHPDocTypeTag && pHPDocTag.getKind().equals(PHPDocTag.Type.VAR)) {
                    result = getFirstTypeFromTag((PHPDocTypeTag) pHPDocTag);
                    if (!result.isEmpty()) {
                        break;
                    }
                }
            }
            return result;
        }

        private String getFirstTypeFromTag(final PHPDocTypeTag typeTag) {
            String result = ""; //NOI18N
            for (PHPDocTypeNode typeNode : typeTag.getTypes()) {
                String type = typeNode.getValue();
                if (!Type.isPrimitive(type) && !VariousUtils.isSpecialClassName(type)) {
                    result = typeNode.isArray() ? Type.ARRAY : type;
                    break;
                }
            }
            return result;
        }

        @Override
        public void visit(MethodDeclaration node) {
            String name = node.getFunction().getFunctionName().getName();
            String possibleProperty;
            if (name != null) {
                if (name.startsWith(CGSGenerator.START_OF_GETTER)) {
                    possibleProperty = name.substring(CGSGenerator.START_OF_GETTER.length());
                    existingGetters.addAll(getAllPossibleProperties(possibleProperty));
                } else if (name.startsWith(CGSGenerator.START_OF_SETTER)) {
                    possibleProperty = name.substring(CGSGenerator.START_OF_GETTER.length());
                    existingSetters.addAll(getAllPossibleProperties(possibleProperty));
                } else if (className != null && (className.equals(name) || "__construct".equals(name))) { //NOI18N
                    hasConstructor = true;
                }
            }
        }

        /**
         * Returns all possible properties which are based on the passed property derived from method name.
         *
         * @param possibleProperty Name of the property which was derived from method name (setField() -> field).
         * @return field => (field, _field) OR _field => (_field, field)
         */
        private List<String> getAllPossibleProperties(String possibleProperty) {
            List<String> allPossibleProperties = new LinkedList<>();
            possibleProperty = possibleProperty.toLowerCase();
            allPossibleProperties.add(possibleProperty);
            if (possibleProperty.startsWith("_")) { // NOI18N
                allPossibleProperties.add(possibleProperty.substring(1));
            } else {
                allPossibleProperties.add("_" + possibleProperty); // NOI18N
            }
            return allPossibleProperties;
        }
    }
}
