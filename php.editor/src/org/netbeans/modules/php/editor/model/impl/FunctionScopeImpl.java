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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.editor.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.parsing.spi.indexing.support.IndexDocument;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.PhpModifiers;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.BaseFunctionElement;
import org.netbeans.modules.php.editor.api.elements.BaseFunctionElement.PrintAs;
import org.netbeans.modules.php.editor.api.elements.FunctionElement;
import org.netbeans.modules.php.editor.api.elements.ParameterElement;
import org.netbeans.modules.php.editor.elements.ParameterElementImpl;
import org.netbeans.modules.php.editor.index.PHPIndexer;
import org.netbeans.modules.php.editor.index.Signature;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.FunctionScope;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.Scope;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.model.VariableName;
import org.netbeans.modules.php.editor.model.nodes.FunctionDeclarationInfo;
import org.netbeans.modules.php.editor.model.nodes.LambdaFunctionDeclarationInfo;
import org.netbeans.modules.php.editor.model.nodes.MagicMethodDeclarationInfo;
import org.netbeans.modules.php.editor.model.nodes.MethodDeclarationInfo;
import org.netbeans.modules.php.editor.parser.astnodes.LambdaFunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;

/**
 *
 * @author Radek Matous
 */
class FunctionScopeImpl extends ScopeImpl implements FunctionScope, VariableNameFactory {
    private static final String TYPE_SEPARATOR = "|"; //NOI18N
    private static final String TYPE_SEPARATOR_REGEXP = "\\|"; //NOI18N
    private List<? extends ParameterElement> paremeters;
    //@GuardedBy("this")
    private String returnType;

    //new contructors
    FunctionScopeImpl(Scope inScope, FunctionDeclarationInfo info, String returnType, boolean isDeprecated) {
        super(inScope, info, PhpModifiers.fromBitMask(PhpModifiers.PUBLIC), info.getOriginalNode().getBody(), isDeprecated);
        this.paremeters = info.getParameters();
        this.returnType = returnType;
    }

    FunctionScopeImpl(Scope inScope, LambdaFunctionDeclarationInfo info) {
        super(inScope, info, PhpModifiers.fromBitMask(PhpModifiers.PUBLIC), info.getOriginalNode().getBody(), inScope.isDeprecated());
        this.paremeters = info.getParameters();
    }

    protected FunctionScopeImpl(Scope inScope, MethodDeclarationInfo info, String returnType, boolean isDeprecated) {
        super(inScope, info, info.getAccessModifiers(), info.getOriginalNode().getFunction().getBody(), isDeprecated);
        this.paremeters = info.getParameters();
        this.returnType = returnType;
    }

    protected FunctionScopeImpl(Scope inScope, MagicMethodDeclarationInfo info, String returnType, boolean isDeprecated) {
        super(inScope, info, info.getAccessModifiers(), null, isDeprecated);
        this.paremeters = info.getParameters();
        this.returnType = returnType;
    }

    FunctionScopeImpl(Scope inScope, BaseFunctionElement indexedFunction) {
        this(inScope, indexedFunction, PhpElementKind.FUNCTION);
    }

    protected FunctionScopeImpl(Scope inScope, final BaseFunctionElement element, PhpElementKind kind) {
        super(inScope, element, kind);
        this.paremeters = element.getParameters();
        this.returnType =  element.asString(PrintAs.ReturnSemiTypes);
    }

    public static FunctionScopeImpl createElement(Scope scope, LambdaFunctionDeclaration node) {
        return new FunctionScopeImpl(scope, LambdaFunctionDeclarationInfo.create(node)) {
            @Override
            public boolean isAnonymous() {
                return true;
            }
        };
    }

    //old contructors

    public synchronized void addReturnType(String type) {
        if (!StringUtils.hasText(returnType)) {
            returnType = type;
        } else {
            Set<String> distinctTypes = new HashSet<>();
            distinctTypes.addAll(Arrays.asList(returnType.split(TYPE_SEPARATOR_REGEXP)));
            distinctTypes.add(type);
            returnType = StringUtils.implode(distinctTypes, TYPE_SEPARATOR);
        }
    }

    protected synchronized String getReturnType() {
        return returnType;
    }

    @Override
    public Collection<? extends TypeScope> getReturnTypes() {
        return getReturnTypesDescriptor(getReturnType(), false).getModifiedResult(Collections.<TypeScope>emptyList());
    }

    @Override
    public synchronized Collection<? extends String> getReturnTypeNames() {
        Collection<String> retval = Collections.<String>emptyList();
        String type = getReturnType();
        if (type != null && type.length() > 0) {
            retval = new ArrayList<>();
            for (String typeName : type.split(TYPE_SEPARATOR_REGEXP)) {
                if (!VariousUtils.isSemiType(typeName)) {
                    retval.add(typeName);
                }
            }
        }
        return retval;
    }

    @Override
    public Collection<? extends TypeScope> getReturnTypes(boolean resolveSemiTypes, Collection<? extends TypeScope> callerTypes) {
        assert callerTypes != null;
        String types = getReturnType();
        Collection<? extends TypeScope> result = getReturnTypesDescriptor(types, resolveSemiTypes).getModifiedResult(callerTypes);
        updateReturnTypes(types, result);
        return result;
    }

    private static Set<String> recursionDetection = new HashSet<>(); //#168868

    private ReturnTypesDescriptor getReturnTypesDescriptor(String types, boolean resolveSemiTypes) {
        ReturnTypesDescriptor result = ReturnTypesDescriptor.NONE;
        if (StringUtils.hasText(types)) {
            String[] typeNames = types.split(TYPE_SEPARATOR_REGEXP);
            if (containsCallerDependentType(typeNames)) {
                result = new CallerDependentTypesDescriptor();
            } else if (getInScope() instanceof ClassScope && containsSelfDependentType(typeNames)) {
                result = new CommonTypesDescriptor(Collections.singleton((TypeScope) getInScope()));
            } else {
                Collection<TypeScope> retval = new HashSet<>();
                for (String typeName : typeNames) {
                    if (typeName.trim().length() > 0) {
                        boolean added = false;
                        try {
                            added = recursionDetection.add(typeName);
                            if (added && recursionDetection.size() < 15) {
                                if (resolveSemiTypes && VariousUtils.isSemiType(typeName)) {
                                    retval.addAll(VariousUtils.getType(this, typeName, getLastValidMethodOffset(), false));
                                } else {
                                    String modifiedTypeName = typeName;
                                    if (typeName.indexOf("[") != -1) { //NOI18N
                                        modifiedTypeName = typeName.replaceAll("\\[.*\\]", ""); //NOI18N
                                    }
                                    retval.addAll(IndexScopeImpl.getTypes(QualifiedName.create(modifiedTypeName), this));
                                }
                            }
                        } finally {
                            if (added) {
                                recursionDetection.remove(typeName);
                            }
                        }
                    }
                }
                result = new CommonTypesDescriptor(retval);
            }
        }
        return result;
    }

    private int getLastValidMethodOffset() {
        int result = getOffset();
        List<? extends ModelElement> elements = ModelUtils.getElements(this, true);
        if (elements != null && !elements.isEmpty()) {
            Collections.sort(elements, new ModelElementsPositionComparator());
            result = elements.get(0).getNameRange().getEnd();
        }
        return result;
    }

    @org.netbeans.api.annotations.common.SuppressWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
    private static final class ModelElementsPositionComparator implements Comparator<ModelElement> {

        @Override
        public int compare(ModelElement o1, ModelElement o2) {
            int o1End = o1.getNameRange().getEnd();
            int o2End = o2.getNameRange().getEnd();
            // furthest first
            if (o1End < o2End) {
                return 1;
            } else if (o1End > o2End) {
                return -1;
            }
            return 0;
        }

    }

    private static boolean containsCallerDependentType(String[] typeNames) {
        return (Arrays.binarySearch(typeNames, "\\this") >= 0) || (Arrays.binarySearch(typeNames, "\\static") >= 0); //NOI18N
    }

    private static boolean containsSelfDependentType(String[] typeNames) {
        return (Arrays.binarySearch(typeNames, "\\self") >= 0) || (Arrays.binarySearch(typeNames, Type.OBJECT) >= 0); //NOI18N
    }

    private void updateReturnTypes(String oldTypes, Collection<? extends TypeScope> resolvedReturnTypes) {
        if (VariousUtils.isSemiType(oldTypes)) {
            updateSemiReturnTypes(oldTypes, resolvedReturnTypes);
        }
    }

    private void updateSemiReturnTypes(String oldTypes, Collection<? extends TypeScope> resolvedReturnTypes) {
        StringBuilder sb = new StringBuilder();
        for (TypeScope typeScope : resolvedReturnTypes) {
            if (sb.length() != 0) {
                sb.append(TYPE_SEPARATOR);
            }
            sb.append(typeScope.getNamespaceName().append(typeScope.getName()).toString());
        }
        updateReturnTypesIfNotChanged(oldTypes, sb.toString());
    }

    private synchronized void updateReturnTypesIfNotChanged(String oldTypes, String newTypes) {
        if (oldTypes.equals(getReturnType()) && StringUtils.hasText(newTypes)) {
            returnType = newTypes;
        }
    }

    @NonNull
    @Override
    public List<? extends String> getParameterNames() {
        assert paremeters != null;
        List<String> parameterNames = new ArrayList<>();
        for (ParameterElement parameter : paremeters) {
            parameterNames.add(parameter.getName());
        }
        return parameterNames;
    }

    @NonNull
    @Override
    public List<? extends ParameterElement> getParameters() {
        return paremeters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Collection<? extends TypeScope> returnTypes = getReturnTypes();
        sb.append('[');
        for (TypeScope typeScope : returnTypes) {
            if (sb.length() == 1) {
                sb.append(TYPE_SEPARATOR); //NOI18N
            }
            sb.append(typeScope.getName());
        }
        sb.append("] "); //NOI18N
        sb.append(super.toString()).append("("); //NOI18N
        List<? extends String> parameters = getParameterNames();
        for (int i = 0; i < parameters.size(); i++) {
            String param = parameters.get(i);
            if (i > 0) {
                sb.append(","); //NOI18N
            }
            sb.append(param);
        }
        sb.append(")"); //NOI18N

        return sb.toString();
    }

    @Override
    public Collection<? extends VariableName> getDeclaredVariables() {
        return filter(getElements(), new ElementFilter() {
            @Override
            public boolean isAccepted(ModelElement element) {
                return element.getPhpElementKind().equals(PhpElementKind.VARIABLE);
            }
        });
    }

    @Override
    public VariableNameImpl createElement(Variable node) {
        VariableNameImpl retval = new VariableNameImpl(this, node, false);
        addElement(retval);
        return retval;
    }

    @Override
    public void addSelfToIndex(IndexDocument indexDocument) {
        indexDocument.addPair(PHPIndexer.FIELD_BASE, getIndexSignature(), true, true);
        indexDocument.addPair(PHPIndexer.FIELD_TOP_LEVEL, getName().toLowerCase(), true, true);
    }

    private String getIndexSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName().toLowerCase()).append(Signature.ITEM_DELIMITER);
        sb.append(getName()).append(Signature.ITEM_DELIMITER);
        sb.append(getOffset()).append(Signature.ITEM_DELIMITER);
        List<? extends ParameterElement> parameters = getParameters();
        for (int idx = 0; idx < parameters.size(); idx++) {
            ParameterElementImpl parameter = (ParameterElementImpl) parameters.get(idx);
            if (idx > 0) {
                sb.append(','); //NOI18N
            }
            sb.append(parameter.getSignature());

        }
        sb.append(Signature.ITEM_DELIMITER);
        String type = getReturnType();
        if (type != null && !Type.MIXED.equalsIgnoreCase(type)) {
            sb.append(type);
        }
        sb.append(Signature.ITEM_DELIMITER);
        NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(this);
        assert namespaceScope != null;
        QualifiedName qualifiedName = namespaceScope.getQualifiedName();
        sb.append(qualifiedName.toString()).append(Signature.ITEM_DELIMITER);
        sb.append(isDeprecated() ? 1 : 0).append(Signature.ITEM_DELIMITER);
        sb.append(getFilenameUrl()).append(Signature.ITEM_DELIMITER);
        return sb.toString();
    }

    @Override
    public QualifiedName getNamespaceName() {
        if (indexedElement instanceof FunctionElement) {
            FunctionElement indexedFunction = (FunctionElement) indexedElement;
            return indexedFunction.getNamespaceName();
        }
        return super.getNamespaceName();
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }


    private interface ReturnTypesDescriptor {
        ReturnTypesDescriptor NONE = new ReturnTypesDescriptor() {

            @Override
            public Collection<? extends TypeScope> getModifiedResult(Collection<? extends TypeScope> callerTypes) {
                return Collections.emptyList();
            }
        };

        Collection<? extends TypeScope> getModifiedResult(Collection<? extends TypeScope> callerTypes);

    }

    private static final class CommonTypesDescriptor implements ReturnTypesDescriptor {
        private final Collection<? extends TypeScope> rawTypes;

        public CommonTypesDescriptor(Collection<? extends TypeScope> rawTypes) {
            assert rawTypes != null;
            this.rawTypes = rawTypes;
        }

        @Override
        public Collection<? extends TypeScope> getModifiedResult(Collection<? extends TypeScope> callerTypes) {
            assert callerTypes != null;
            return rawTypes;
        }

    }

    private static final class CallerDependentTypesDescriptor implements ReturnTypesDescriptor {

        @Override
        public Collection<? extends TypeScope> getModifiedResult(Collection<? extends TypeScope> callerTypes) {
            assert callerTypes != null;
            return callerTypes;
        }

    }
}
