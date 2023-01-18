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
package org.netbeans.modules.php.editor.api;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.netbeans.modules.php.editor.api.ElementQuery.QueryScope;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.api.elements.ConstantElement;
import org.netbeans.modules.php.editor.api.elements.FieldElement;
import org.netbeans.modules.php.editor.api.elements.FunctionElement;
import org.netbeans.modules.php.editor.api.elements.InterfaceElement;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.NamespaceElement;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.api.elements.TraitElement;
import org.netbeans.modules.php.editor.api.elements.TypeConstantElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.api.elements.TypeResolver;
import org.netbeans.modules.php.editor.api.elements.VariableElement;
import org.netbeans.modules.php.editor.elements.ClassElementImpl;
import org.netbeans.modules.php.editor.elements.ConstantElementImpl;
import org.netbeans.modules.php.editor.elements.FieldElementImpl;
import org.netbeans.modules.php.editor.elements.FunctionElementImpl;
import org.netbeans.modules.php.editor.elements.InterfaceElementImpl;
import org.netbeans.modules.php.editor.elements.MethodElementImpl;
import org.netbeans.modules.php.editor.elements.NamespaceElementImpl;
import org.netbeans.modules.php.editor.elements.TraitElementImpl;
import org.netbeans.modules.php.editor.elements.TypeConstantElementImpl;
import org.netbeans.modules.php.editor.elements.VariableElementImpl;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.model.VariableScope;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.model.nodes.ASTNodeInfo;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ConstantDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.FieldsDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TypeDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.VariableBase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Parameters;

public final class FileElementQuery extends AbstractElementQuery implements ElementQuery.File {

    private final FileObject fileObject;
    private final PHPParseResult result;
    private final URL url;
    private final Map<PhpElement, Map<String, VariableElement>> varMap = new HashMap<>();
    private final Set<FieldElement> fields = new HashSet<>();

    private FileElementQuery(final PHPParseResult result) {
        super(QueryScope.FILE_SCOPE);
        this.result = result;
        this.fileObject = result.getSnapshot().getSource().getFileObject();
        this.url = fileObject != null ? URLMapper.findURL(fileObject, URLMapper.INTERNAL) : null;
    }

    public static FileElementQuery getInstance(final PHPParseResult result) {
        return new FileElementQuery(result);
    }

    public NamespaceElement create(final NamespaceDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        final NamespaceElement retval = NamespaceElementImpl.fromNode(node, this);
        addElement(retval);
        return retval;
    }

    public TypeElement create(final NamespaceElement namespace, final TypeDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        TypeElement retval = null;
        if (node instanceof ClassDeclaration) {
            retval = create(namespace, (ClassDeclaration) node);
        } else if (node instanceof InterfaceDeclaration) {
            retval = create(namespace, (InterfaceDeclaration) node);
        } else if (node instanceof TraitDeclaration) {
            retval = create(namespace, (TraitDeclaration) node);
        }
        addElement(retval);
        return retval;
    }

    public ClassElement create(final NamespaceElement namespace, final ClassDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        final ClassElement retval = ClassElementImpl.fromNode(namespace, node, this);
        addElement(retval);
        return retval;
    }

    public InterfaceElement create(final NamespaceElement namespace, final InterfaceDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        final InterfaceElement retval = InterfaceElementImpl.fromNode(namespace, node, this);
        addElement(retval);
        return retval;
    }

    public TraitElement create(final NamespaceElement namespace, final TraitDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        final TraitElement retval = TraitElementImpl.fromNode(namespace, node, this);
        addElement(retval);
        return retval;
    }

    public FunctionElement create(final NamespaceElement namespace, final FunctionDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        final FunctionElement retval = FunctionElementImpl.fromNode(namespace, node, this);
        addElement(retval);
        return retval;
    }

    public MethodElement create(final TypeElement type, final MethodDeclaration node) {
        Parameters.notNull("type", type); //NOI18N
        Parameters.notNull("node", node); //NOI18N
        MethodElement retval = MethodElementImpl.fromNode(type, node, this);
        addElement(retval);
        return retval;
    }

    public Set<FieldElement> create(final TypeElement type, final FieldsDeclaration node) {
        Parameters.notNull("type", type); //NOI18N
        Parameters.notNull("node", node); //NOI18N
        final Set<FieldElement> retval = FieldElementImpl.fromNode(type, node, this);
        for (FieldElement fieldElement : retval) {
            if (!fields.contains(fieldElement)) {
                fields.add(fieldElement);
                addElement(fieldElement);
            }
        }
        return retval;
    }

    /**
     * @return instance of PropertyElement or null
     */
    public FieldElement create(final TypeElement type, final FieldAccess node) {
        Parameters.notNull("type", type); //NOI18N
        Parameters.notNull("node", node); //NOI18N
        final Set<TypeResolver> resolvers = new HashSet<>();
        resolvers.add(new VariableTypeResolver(node));
        VariableBase dispatcher = node.getDispatcher();
        final MethodElement method = getLast(MethodElement.class);
        if (method != null && dispatcher instanceof Variable) {
            VariableElement varDispatcher = createMethodVariable(method, (Variable) dispatcher);
            if (varDispatcher.getName(false).equalsIgnoreCase("this")) { //NOI18N
                final FieldElement retval = FieldElementImpl.fromNode(type, node, resolvers, this);
                if (!fields.contains(retval)) {
                    fields.add(retval);
                    addElement(retval);
                }
                return retval;
            }
        }
        return null;
    }

    public VariableElement createTopLevelVariable(final Variable node) {
        Parameters.notNull("node", node); //NOI18N
        final Set<TypeResolver> resolvers = new HashSet<>();
        resolvers.add(new VariableTypeResolver(node));
        return addTopLevelVariable(VariableElementImpl.fromNode(node, resolvers, this));
    }

    public VariableElement createMethodVariable(final MethodElement method, final Variable node) {
        Parameters.notNull("method", method); //NOI18N
        Parameters.notNull("node", node); //NOI18N
        final Set<TypeResolver> resolvers = new HashSet<>();
        resolvers.add(new VariableTypeResolver(node));
        return addMethodVariable(method, VariableElementImpl.fromNode(node, resolvers, this));
    }

    public VariableElement createFunctionVariable(final FunctionElement function, final Variable node) {
        Parameters.notNull("method", function); //NOI18N
        Parameters.notNull("node", node); //NOI18N
        final Set<TypeResolver> resolvers = new HashSet<>();
        resolvers.add(new VariableTypeResolver(node));
        return addFunctionVariable(function, VariableElementImpl.fromNode(node, resolvers, this));
    }

    public Set<ConstantElement> createConstant(final NamespaceElement namespace, final ConstantDeclaration node) {
        Parameters.notNull("node", node); //NOI18N
        final Set<ConstantElement> retval = ConstantElementImpl.fromNode(namespace, node, this);
        addElements(retval);
        return retval;
    }

    public Set<TypeConstantElement> createTypeConstant(final TypeElement type, final ConstantDeclaration node) {
        Parameters.notNull("type", type); //NOI18N
        Parameters.notNull("node", node); //NOI18N
        final Set<TypeConstantElement> retval = TypeConstantElementImpl.fromNode(type, node, this);
        addElements(retval);
        return retval;
    }

    @Override
    public FileObject getFileObject() {
        return fileObject;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public PHPParseResult getResult() {
        return result;
    }

    @Override
    public Set<MethodElement> getDeclaredMethods(TypeElement typeElement) {
        return getMethods(NameKind.exact(typeElement.getFullyQualifiedName()), NameKind.empty());
    }

    @Override
    public Set<FieldElement> getDeclaredFields(TypeElement typeElement) {
        return getFields(NameKind.exact(typeElement.getFullyQualifiedName()), NameKind.empty());
    }

    @Override
    public Set<TypeConstantElement> getDeclaredTypeConstants(TypeElement typeElement) {
        return getTypeConstants(NameKind.exact(typeElement.getFullyQualifiedName()), NameKind.empty());
    }

    private synchronized VariableElement addVariable(final PhpElement scope, final VariableElement variable) {
        Map<String, VariableElement> map = varMap.get(scope);
        if (map == null) {
            map = new HashMap<>();
        }
        VariableElement old = map.put(variable.getName(), variable);
        if (old != null) {
            map.put(old.getName(), old);
        }
        varMap.put(scope, map);
        return old != null ? old : variable;
    }

    private synchronized VariableElement addFunctionVariable(final FunctionElement scope, final VariableElement variable) {
        return addVariable(scope, variable);
    }

    private synchronized VariableElement addMethodVariable(final MethodElement scope, final VariableElement variable) {
        return addVariable(scope, variable);
    }

    private synchronized VariableElement addTopLevelVariable(final VariableElement variable) {
        VariableElement retval = addVariable(null, variable);
        if (retval == variable) {
            super.addElement(variable);
        }
        return retval;
    }

    @Override
    public Set<VariableElement> getTopLevelVariables() {
        Map<String, VariableElement> map = varMap.get(null);
        return map != null ? new HashSet(map.values()) : Collections.emptySet();
    }

    @Override
    public Set<VariableElement> getMethodVariables(MethodElement method) {
        Map<String, VariableElement> map = varMap.get(method);
        return map != null ? new HashSet(map.values()) : Collections.emptySet();
    }

    @Override
    public Set<VariableElement> getFunctionVariables(FunctionElement function) {
        Map<String, VariableElement> map = varMap.get(function);
        return map != null ? new HashSet(map.values()) : Collections.emptySet();
    }

    private final class VariableTypeResolver implements TypeResolver {

        private final String rawName;
        private QualifiedName name;
        private boolean resolved;
        private final int offset;

        private VariableTypeResolver(final Variable var) {
            ASTNodeInfo<Variable> info = ASTNodeInfo.create(var);
            this.rawName = VariousUtils.extractTypeFroVariableBase(info.getOriginalNode());
            this.offset = info.getRange().getStart();
        }
        private VariableTypeResolver(final FieldAccess field) {
            ASTNodeInfo<FieldAccess> info = ASTNodeInfo.create(field);
            this.rawName = VariousUtils.extractTypeFroVariableBase(info.getOriginalNode());
            this.offset = info.getRange().getStart();
        }

        @Override
        public synchronized boolean isResolved() {
            return resolved;
        }

        @Override
        public boolean canBeResolved() {
            return true;
        }

        @Override
        public String getRawTypeName() {
            return rawName;
        }

        @Override
        public synchronized QualifiedName getTypeName(boolean resolve) {
            if (!isResolved() && resolve) {
                resolved = true;
                Model model = getResult().getModel();
                VariableScope scope = model.getVariableScope(offset);
                if (scope != null) {
                    Collection<? extends TypeScope> types = VariousUtils.getType(scope, rawName, offset, false);
                    for (TypeScope typeScope : types) {
                        name = typeScope.getFullyQualifiedName();
                        if (name != null) {
                            break;
                        }
                    }

                }
            }
            return name;
        }
    }
}
