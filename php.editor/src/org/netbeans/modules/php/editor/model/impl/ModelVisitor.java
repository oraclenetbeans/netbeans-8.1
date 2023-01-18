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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.api.editor.PhpBaseElement;
import org.netbeans.modules.php.api.editor.PhpClass;
import org.netbeans.modules.php.api.editor.PhpVariable;
import org.netbeans.modules.php.editor.Cache;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ParameterElement;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.api.elements.TypeResolver;
import org.netbeans.modules.php.editor.elements.TypeResolverImpl;
import org.netbeans.modules.php.editor.elements.VariableElementImpl;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.CodeMarker;
import org.netbeans.modules.php.editor.model.FileScope;
import org.netbeans.modules.php.editor.model.FunctionScope;
import org.netbeans.modules.php.editor.model.IndexScope;
import org.netbeans.modules.php.editor.model.MethodScope;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.Occurence;
import org.netbeans.modules.php.editor.model.Scope;
import org.netbeans.modules.php.editor.model.TraitScope;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.model.VariableName;
import org.netbeans.modules.php.editor.model.VariableScope;
import org.netbeans.modules.php.editor.model.VariableScopeFinder;
import org.netbeans.modules.php.editor.model.nodes.ASTNodeInfo;
import org.netbeans.modules.php.editor.model.nodes.ASTNodeInfo.Kind;
import org.netbeans.modules.php.editor.model.nodes.ConstantDeclarationInfo;
import org.netbeans.modules.php.editor.model.nodes.PhpDocTypeTagInfo;
import org.netbeans.modules.php.editor.model.nodes.UseStatementPartInfo;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayAccess;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayCreation;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayElement;
import org.netbeans.modules.php.editor.parser.astnodes.Assignment;
import org.netbeans.modules.php.editor.parser.astnodes.CatchClause;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ClassInstanceCreation;
import org.netbeans.modules.php.editor.parser.astnodes.ClassName;
import org.netbeans.modules.php.editor.parser.astnodes.Comment;
import org.netbeans.modules.php.editor.parser.astnodes.ConstantDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.DoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.FieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.FieldsDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ForEachStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FormalParameter;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionName;
import org.netbeans.modules.php.editor.parser.astnodes.GlobalStatement;
import org.netbeans.modules.php.editor.parser.astnodes.GotoLabel;
import org.netbeans.modules.php.editor.parser.astnodes.GotoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Include;
import org.netbeans.modules.php.editor.parser.astnodes.InstanceOfExpression;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.LambdaFunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceName;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocBlock;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocMethodTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTypeTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocVarTypeTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPVarComment;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.Reference;
import org.netbeans.modules.php.editor.parser.astnodes.ReflectionVariable;
import org.netbeans.modules.php.editor.parser.astnodes.ReturnStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar.Type;
import org.netbeans.modules.php.editor.parser.astnodes.SingleFieldDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.StaticConstantAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticFieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.SwitchStatement;
import org.netbeans.modules.php.editor.parser.astnodes.TraitConflictResolutionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitMethodAliasDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TryStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatementPart;
import org.netbeans.modules.php.editor.parser.astnodes.UseTraitStatementPart;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.VariableBase;
import org.netbeans.modules.php.editor.parser.astnodes.WhileStatement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultTreePathVisitor;
import org.netbeans.modules.php.project.api.PhpEditorExtender;
import org.netbeans.modules.php.spi.annotation.AnnotationParsedLine;
import org.netbeans.modules.php.spi.editor.EditorExtender;
import org.openide.filesystems.FileObject;
import org.openide.util.Parameters;

/**
 *
 * @author Radek Matous
 */
public final class ModelVisitor extends DefaultTreePathVisitor {

    private final FileScopeImpl fileScope;
    //@GuardedBy("this")
    private IndexScope indexScope;
    private Map<Scope, Map<String, VariableNameImpl>> vars;
    private final Map<String, List<PhpDocTypeTagInfo>> varTypeComments;
    private volatile OccurenceBuilder occurencesBuilder;
    private volatile CodeMarkerBuilder markerBuilder;
    private final ModelBuilder modelBuilder;
    private final PHPParseResult info;
    //@GuardedBy("lock")
    private boolean  askForEditorExtensions = true;
    private final Object lock = new Object();
    private final List<PhpBaseElement> baseElements;
    private final Cache<Scope, Map<String, AssignmentImpl>> assignmentMapCache = new Cache<>();

    private boolean lazyScan = true;
    private volatile ScopeImpl previousScope;
    private volatile List<String> currentLexicalVariables = new LinkedList<>();

    public ModelVisitor(final PHPParseResult info) {
        this.fileScope = new FileScopeImpl(info);
        varTypeComments = new HashMap<>();
        occurencesBuilder = new OccurenceBuilder();
        markerBuilder = new CodeMarkerBuilder();
        this.modelBuilder = new ModelBuilder(this.fileScope);
        this.info = info;
        this.baseElements = new ArrayList<>();
    }

    public ParserResult getCompilationInfo() {
        return this.info;
    }

    @Override
    public void scan(ASTNode node) {
        super.scan(node);
    }

    public List<PhpBaseElement> extendedElements() {
        synchronized (lock) {
            if (!askForEditorExtensions) {
                return new ArrayList<>(baseElements);
            }
            askForEditorExtensions = false;
        }
        baseElements.clear();
        final FileObject fileObject = fileScope.getFileObject();
        EditorExtender editorExtender = PhpEditorExtender.forFileObject(fileObject);
        final List<PhpBaseElement> elements = editorExtender.getElementsForCodeCompletion(fileObject);
        baseElements.addAll(elements);
        if (elements.size() > 0) {
            for (PhpBaseElement element : elements) {
                if (element instanceof PhpVariable) {
                    PhpVariable phpVariable = (PhpVariable) element;
                    Collection<? extends NamespaceScope> declaredNamespaces = fileScope.getDeclaredNamespaces();
                    for (NamespaceScope namespace : declaredNamespaces) {
                        NamespaceScopeImpl namespaceScope = (NamespaceScopeImpl) namespace;
                        if (namespaceScope != null) {
                            final String varName = phpVariable.getName();
                            VariableNameImpl variable = findVariable(namespace, varName);
                            final PhpClass type = phpVariable.getType();
                            if (variable != null) {
                                variable.indexedElement = VariableElementImpl.create(
                                        varName,
                                        phpVariable.getOffset(),
                                        phpVariable.getFile(),
                                        null,
                                        type != null ? TypeResolverImpl.parseTypes(type.getFullyQualifiedName()) : Collections.<TypeResolver>emptySet(),
                                        false);
                            } else {
                                int offset = namespaceScope.getOffset();
                                VariableElementImpl var = VariableElementImpl.create(
                                        varName,
                                        offset,
                                        phpVariable.getFile(),
                                        null,
                                        type != null ? TypeResolverImpl.parseTypes(type.getFullyQualifiedName()) : Collections.<TypeResolver>emptySet(),
                                        false);
                                namespaceScope.createElement(var);
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(baseElements);
    }

    @Override
    public void visit(PHPDocMethodTag node) {
        AnnotationParsedLine kind = node.getKind();
        Scope currentScope = modelBuilder.getCurrentScope();
        boolean scopeHasBeenModified = false;
        // Someone uses @method tag in method scope :/ So we have to simulate that it's defined in class scope...
        if (currentScope instanceof MethodScope) {
            MethodScope methodScope = (MethodScope) currentScope;
            currentScope = methodScope.getInScope();
            modelBuilder.setCurrentScope((ScopeImpl) currentScope);
            scopeHasBeenModified = true;
        }
        if (currentScope instanceof TypeScope && kind.equals(PHPDocTag.Type.METHOD)) {
            modelBuilder.buildMagicMethod(node, occurencesBuilder);
            occurencesBuilder.prepare(node, currentScope);
        }
        // ...and then reset it to avoid possible problems.
        if (scopeHasBeenModified) {
            modelBuilder.reset();
        }
        if (currentScope instanceof TypeScope) {
            MethodScopeImpl methodScope = MethodScopeImpl.createElement(currentScope, node);
            modelBuilder.setCurrentScope(methodScope);
        } else {
            modelBuilder.setCurrentScope((ScopeImpl) currentScope);
        }
        super.visit(node);
        modelBuilder.reset();
    }

    @Override
    public void visit(ReturnStatement node) {
        super.visit(node);
        final ScopeImpl currentScope = modelBuilder.getCurrentScope();
        markerBuilder.prepare(node, currentScope);
        String typeName = null;
        if (currentScope instanceof FunctionScope) {
            FunctionScopeImpl functionScope = (FunctionScopeImpl) currentScope;
            Expression expression = node.getExpression();
            if (expression instanceof ClassInstanceCreation) {
                ClassInstanceCreation instanceCreation = (ClassInstanceCreation) expression;
                ASTNodeInfo<ClassInstanceCreation> inf = ASTNodeInfo.create(instanceCreation);
                String pureTypeName = inf.getQualifiedName().toString();
                typeName = VariousUtils.qualifyTypeNames(pureTypeName, node.getStartOffset(), currentScope);
            } else if (expression instanceof VariableBase) {
                typeName = VariousUtils.extractTypeFroVariableBase((VariableBase) expression);
                if (typeName != null) {
                    Collection<? extends VariableName> allVariables = VariousUtils.getAllVariables(functionScope, typeName);
                    Map<String, String> var2Type = new HashMap<>();
                    for (VariableName variable : allVariables) {
                        String name = variable.getName();
                        String type = resolveVariableType(name, functionScope, node);
                        String qualifiedType = VariousUtils.qualifyTypeNames(type, node.getStartOffset(), currentScope);
                        var2Type.put(name, qualifiedType);
                    }
                    if (!var2Type.isEmpty()) {
                        typeName = VariousUtils.replaceVarNames(typeName, var2Type);
                    }
                }
            } else if (expression instanceof Scalar) {
                typeName = VariousUtils.extractVariableTypeFromExpression(expression, null);
            }
            if (typeName != null) {
                functionScope.addReturnType(QualifiedName.create(typeName).toString());
            }
        }
    }

    private static final Set<String> recursionDetection = new HashSet<>(); //#168868

    private String resolveVariableType(String varName, FunctionScopeImpl varScope, ReturnStatement node) {
        try {
            if (varName != null && recursionDetection.add(varName)) {
                if (varName.equalsIgnoreCase("$this") && varScope instanceof MethodScope) { //NOI18N
                    return varScope.getInScope().getName();
                }
                VariableNameImpl var = (VariableNameImpl) ModelUtils.getFirst(varScope.getDeclaredVariables(), varName);
                if (var != null) {
                    AssignmentImpl assignment = var.findVarAssignment(node.getStartOffset());
                    if (assignment != null) {
                        String typeName = assignment.typeNameFromUnion();
                        if (typeName != null) {
                            if (!VariousUtils.isSemiType(typeName)) {
                                return typeName;
                            } else {
                                String variableName = getName(typeName, VariousUtils.Kind.VAR, true);
                                if (variableName != null && !variableName.equalsIgnoreCase(varName)) {
                                    return resolveVariableType(variableName, varScope, node);
                                }
                                return typeName;
                            }
                        }
                    }
                }
            }
        } finally {
            if (varName != null) {
                recursionDetection.remove(varName);
            }
        }
        return null;
    }

    public static String getName(String semiType, VariousUtils.Kind kind, boolean strict) {
        if (semiType != null) {
            String prefix = VariousUtils.PRE_OPERATION_TYPE_DELIMITER + kind.toString(); // NOI18N
            if (semiType.startsWith(prefix)) {
                String[] split = semiType.split(prefix, 2);
                if (split.length > 1) {

                    if (VariousUtils.isSemiType(split[1])) {
                        if (strict) {
                            return null;
                        } else {
                            split = split[1].split(VariousUtils.PRE_OPERATION_TYPE_DELIMITER);
                            if (split.length < 1) {
                                return null;
                            }
                            return split[0];
                        }
                    } else {
                        return split[1];
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void visit(GotoLabel label) {
        super.visit(label);
        occurencesBuilder.prepare(label, modelBuilder.getCurrentScope());
    }
    @Override
    public void visit(GotoStatement statement) {
        super.visit(statement);
        occurencesBuilder.prepare(statement, modelBuilder.getCurrentScope());
    }

    @Override
    public void visit(Program program) {
        lazyScan = true;
        modelBuilder.setProgram(program);
        fileScope.setBlockRange(program);
        this.vars = new HashMap<>();
        prepareVarComments(program);
        super.visit(program);
        handleVarComments();
    }

    @Override
    public void visit(Include node) {
        modelBuilder.build(node, occurencesBuilder);
        super.visit(node);
    }

    @Override
    public void visit(NamespaceDeclaration node) {
        modelBuilder.build(node, occurencesBuilder);
        try {
            super.visit(node);
        } finally {
            modelBuilder.reset();
        }
    }

    @Override
    public void visit(NamespaceName namespaceName) {
        super.visit(namespaceName);
        ASTNode parent = getPath().get(0);
        if (parent instanceof FunctionName) {
            occurencesBuilder.prepare(Kind.FUNCTION, namespaceName, fileScope);
        } else if (!(parent instanceof ClassDeclaration) && !(parent instanceof InterfaceDeclaration)
                && !(parent instanceof FormalParameter) && !(parent instanceof InstanceOfExpression)
                && !(parent instanceof UseTraitStatementPart) && !(parent instanceof TraitConflictResolutionDeclaration)
                && !(parent instanceof TraitMethodAliasDeclaration)) {
            occurencesBuilder.prepare(Kind.CONSTANT, namespaceName, fileScope);
        }
        occurencesBuilder.prepare(namespaceName, modelBuilder.getCurrentScope());
    }

    @Override
    public void visit(UseStatementPart statementPart) {
        UseStatement.Type type = ((UseStatement) getPath().get(0)).getType();
        UseStatementPartInfo useStatementPartInfo = UseStatementPartInfo.create(statementPart, type);
        modelBuilder.getCurrentNameSpace().createUseStatementPart(useStatementPartInfo);
        ScopeImpl currentScope = modelBuilder.getCurrentScope();
        switch (type) {
            case CONST:
                occurencesBuilder.prepare(Kind.CONSTANT, statementPart.getName(), currentScope);
                break;
            case FUNCTION:
                occurencesBuilder.prepare(Kind.FUNCTION, statementPart.getName(), currentScope);
                break;
            case TYPE:
                occurencesBuilder.prepare(Kind.CLASS, statementPart.getName(), currentScope);
                break;
        }
        if (statementPart.getAlias() != null) {
            occurencesBuilder.prepare(Kind.USE_ALIAS, statementPart.getAlias(), currentScope);
        }
    }

    @Override
    public void visit(UseTraitStatementPart node) {
        occurencesBuilder.prepare(Kind.TRAIT, node.getName(), modelBuilder.getCurrentScope());
        super.visit(node);
    }

    @Override
    public void visit(TraitMethodAliasDeclaration node) {
        Expression traitName = node.getTraitName();
        if (traitName instanceof NamespaceName) {
            occurencesBuilder.prepare(Kind.TRAIT, traitName, modelBuilder.getCurrentScope());
        }
        super.visit(node);
    }

    @Override
    public void visit(TraitConflictResolutionDeclaration node) {
        ScopeImpl currentScope = modelBuilder.getCurrentScope();
        Expression preferredTraitName = node.getPreferredTraitName();
        if (preferredTraitName instanceof NamespaceName) {
            occurencesBuilder.prepare(Kind.TRAIT, preferredTraitName, currentScope);
        }
        for (Expression suppressedTraitName : node.getSuppressedTraitNames()) {
            if (suppressedTraitName instanceof NamespaceName) {
                occurencesBuilder.prepare(Kind.TRAIT, suppressedTraitName, currentScope);
            }
        }
        super.visit(node);
    }

    @Override
    public void visit(ClassDeclaration node) {
        modelBuilder.build(node, occurencesBuilder);
        checkComments(node);
        try {
            super.visit(node);
        } finally {
            modelBuilder.reset();
        }
    }

    @Override
    public void visit(TraitDeclaration node) {
        modelBuilder.build(node, occurencesBuilder);
        checkComments(node);
        try {
            super.visit(node);
        } finally {
            modelBuilder.reset();
        }
    }

    @Override
    public void visit(InterfaceDeclaration node) {
        modelBuilder.build(node, occurencesBuilder);
        try {
            super.visit(node);
        } finally {
            modelBuilder.reset();
        }
    }

    @Override
    public void visit(MethodDeclaration node) {
        if (lazyScan) {
            modelBuilder.build(node, occurencesBuilder, this);
            markerBuilder.prepare(node, modelBuilder.getCurrentScope());
            checkComments(node);
        }
        try {
            if (!lazyScan) {
                lazyScan = true; // scan only one exact method...no nested methods (even though that they shouldn't exist)
                scan(node.getFunction().getFormalParameters());
                scan(node.getFunction().getBody());
            }
        } finally {
            if (lazyScan) {
                modelBuilder.reset();
            }
        }
    }

    @Override
    public void visit(FieldsDeclaration node) {
        modelBuilder.build(node, occurencesBuilder);
        checkComments(node);
        super.visit(node);
    }

    @Override
    public void visit(ClassInstanceCreation node) {
        Expression className = node.getClassName().getName();
        if (className instanceof Variable) {
            scan(className);
        } else {
            ScopeImpl currentScope = modelBuilder.getCurrentScope();
            occurencesBuilder.prepare(node, currentScope);
            if (className instanceof NamespaceName) {
                occurencesBuilder.prepare((NamespaceName) className, currentScope);
            }
        }
        scan(node.ctorParams());
    }

    @Override
    public void visit(InstanceOfExpression node) {
        ClassName className = node.getClassName();
        Expression expression = node.getExpression();
        if (className.getName() instanceof Variable) {
            occurencesBuilder.prepare((Variable) className.getName(), modelBuilder.getCurrentScope());
            if (expression instanceof Variable) {
                occurencesBuilder.prepare((Variable) expression, modelBuilder.getCurrentScope());
            }
        } else {
            if (className.getName() instanceof NamespaceName) {
                occurencesBuilder.prepare((NamespaceName) className.getName(), modelBuilder.getCurrentScope());
            }
            String clsName = CodeUtils.extractClassName(node.getClassName());
            if (clsName != null) {
                if (expression instanceof Variable) {
                    Variable var = (Variable) expression;
                    Scope currentScope = modelBuilder.getCurrentScope();
                    VariableNameImpl varN = findVariable(currentScope, var);
                    if (varN != null) {
                        VarAssignmentImpl varAssignment = varN.createAssignment(currentScope, true, getBlockRange(currentScope), ASTNodeInfo.create(var).getRange(), clsName);
                        varN.addElement(varAssignment);
                    }
                }

            }
        }
        super.visit(node);
    }

    @Override
    public void visit(MethodInvocation node) {
        FunctionInvocation method = node.getMethod();
        if (method != null) {
            if (hasCommonFunctionName(method)) {
                occurencesBuilder.prepare(node, modelBuilder.getCurrentScope());
            } else {
                scan(method);
            }
        }
        scan(node.getDispatcher());
        scan(node.getMethod().getParameters());
    }

    @Override
    public void visit(Scalar scalar) {
        String stringValue = scalar.getStringValue();
        if (stringValue != null && stringValue.trim().length() > 0
                && scalar.getScalarType() == Type.STRING && !NavUtils.isQuoted(stringValue)) {
            occurencesBuilder.prepare(Kind.CONSTANT, scalar, fileScope);
        }
        super.visit(scalar);
    }

    @Override
    public void visit(StaticMethodInvocation node) {
        Scope scope = modelBuilder.getCurrentScope();
        FunctionInvocation method = node.getMethod();
        if (method != null) {
            if (hasCommonFunctionName(method)) {
                occurencesBuilder.prepare(node, modelBuilder.getCurrentScope());
            } else {
                scan(method);
            }
        }
        Expression className = node.getClassName();
        if (className instanceof Variable) {
            scan(className);
        } else if (className instanceof NamespaceName) {
            occurencesBuilder.prepare((NamespaceName) className, scope);
        }
        scan(node.getMethod().getParameters());
    }

    private boolean hasCommonFunctionName(final FunctionInvocation functionInvocation) {
        boolean result = false;
        FunctionName functionName = functionInvocation.getFunctionName();
        if (functionName != null) {
            Expression name = functionName.getName();
            if (name instanceof Variable) {
                Variable variable = (Variable) name;
                result = !variable.isDollared() && !(name instanceof ReflectionVariable);
            } else {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void visit(ClassName node) {
        if (!(node.getName() instanceof Variable) && !(node.getName() instanceof FieldAccess)) {
            Scope scope = modelBuilder.getCurrentScope();
            occurencesBuilder.prepare(node, scope);
        }
        scan(node.getName());
    }

    @Override
    public void visit(StaticConstantAccess node) {
        Scope scope = modelBuilder.getCurrentScope();
        occurencesBuilder.prepare(node, scope);
        Expression className = node.getClassName();
        if (className instanceof Variable) {
            scan(className);
        } else if (className instanceof NamespaceName) {
            Kind[] kinds = {Kind.CLASS, Kind.IFACE};
            occurencesBuilder.prepare(kinds, (NamespaceName) className, scope);
        }
    }

    @Override
    public void visit(ConstantDeclaration node) {
        Scope scope = modelBuilder.getCurrentScope();
        if (scope instanceof NamespaceScope) {
            List<? extends ConstantDeclarationInfo> constantDeclarationInfos = ConstantDeclarationInfo.create(node);
            for (ConstantDeclarationInfo nodeInfo : constantDeclarationInfos) {
                ConstantElementImpl createElement = modelBuilder.getCurrentNameSpace().createElement(nodeInfo);
                occurencesBuilder.prepare(nodeInfo, createElement);
            }
        } else {
            modelBuilder.build(node, occurencesBuilder);
        }
        super.visit(node);
    }

    @Override
    public void visit(SingleFieldDeclaration node) {
        scan(node.getValue());
    }

    @Override
    public void visit(Variable node) {
        String varName = CodeUtils.extractVariableName(node);
        if (varName == null) {
            return;
        }
        Scope scope = modelBuilder.getCurrentScope();
        if (previousScope != null && isLexicalVariable(node)) {
            occurencesBuilder.prepare(node, previousScope);
        } else {
            occurencesBuilder.prepare(node, scope);
        }
        if (scope instanceof VariableNameFactory) {
            ASTNodeInfo<Variable> varInfo = ASTNodeInfo.create(node);
            if (scope instanceof MethodScope && "$this".equals(varInfo.getName())) { //NOI18N
                scope = scope.getInScope();
            }
            if (scope instanceof VariableNameFactory) {
                createVariable((VariableNameFactory) scope, node);
            }
        } else {
            assert scope instanceof TypeScope : scope;
        }
        super.visit(node);
    }

    private boolean isLexicalVariable(final Variable variable) {
        return currentLexicalVariables.contains(CodeUtils.extractVariableName(variable));
    }

    @Override
    public void visit(GlobalStatement node) {
        super.visit(node);
        List<Variable> variables = node.getVariables();
        for (Variable var : variables) {
            String varName = CodeUtils.extractVariableName(var);
            if (varName == null) {
                continue;
            }
            Scope scope = modelBuilder.getCurrentScope();
            if (scope instanceof VariableNameFactory) {
                VariableNameFactory vc = (VariableNameFactory) scope;
                Collection<? extends VariableName> variablesImpl = ModelUtils.filter(vc.getDeclaredVariables(), varName);
                VariableNameImpl varElem = (VariableNameImpl) ModelUtils.getFirst(variablesImpl);
                if (varElem != null) {
                    varElem.setGloballyVisible(true);
                } else {
                    vc = (VariableNameFactory) modelBuilder.getCurrentNameSpace();
                    variablesImpl = ModelUtils.filter(vc.getDeclaredVariables(), varName);
                    varElem = (VariableNameImpl) ModelUtils.getFirst(variablesImpl);
                    if (varElem != null) {
                        varElem.setGloballyVisible(true);
                    }
                }
            }
        }
    }

    @Override
    public void visit(FieldAccess node) {
        Variable field = node.getField();
        if (field.isDollared() || field instanceof ReflectionVariable) {
            scan(field);
        } else {
            occurencesBuilder.prepare(node, modelBuilder.getCurrentScope());
        }
        if (field instanceof ArrayAccess) {
            ArrayAccess access = (ArrayAccess) field;
            scan(access.getDimension());
            VariableBase name = access.getName();
            while (name instanceof ArrayAccess) {
                ArrayAccess access1 = (ArrayAccess) name;
                scan(access1.getDimension());
                name = access1.getName();
            }
        }
        scan(node.getDispatcher());
    }

    private Map<String, AssignmentImpl> getAssignmentMap(Scope scope, final VariableBase leftHandSide) {
        Map<String, AssignmentImpl> allAssignments = new HashMap<>();
        Map<String, AssignmentImpl> cachedMap = assignmentMapCache.get(scope);
        if (cachedMap == null || cachedMap.isEmpty()) {
            if (scope instanceof VariableScope) {
                VariableScope variableScope = (VariableScope) scope;
                Collection<? extends VariableName> declaredVariables = variableScope.getDeclaredVariables();
                for (VariableName variableName : declaredVariables) {
                    if (variableName instanceof VariableNameImpl) {
                        VariableNameImpl vni = (VariableNameImpl) variableName;
                        AssignmentImpl ai = vni.findVarAssignment(leftHandSide.getStartOffset());
                        if (ai != null) {
                            allAssignments.put(vni.getName(), ai);
                        }
                    }
                }
            }
            assignmentMapCache.save(scope, allAssignments);
        }
        return allAssignments;
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void visit(Assignment node) {
        Scope scope = modelBuilder.getCurrentScope();
        final VariableBase leftHandSide = node.getLeftHandSide();
        Expression rightHandSide = node.getRightHandSide();
        super.scan(leftHandSide);
        if (leftHandSide instanceof Variable) {
            VariableNameImpl varN = findVariable(scope, leftHandSide);
            if (varN != null) {
                Map<String, AssignmentImpl> allAssignments = getAssignmentMap(scope, leftHandSide);
                Variable var = ((Variable) leftHandSide);
                if (rightHandSide instanceof ArrayCreation) {
                    ArrayCreation arrayCreation = (ArrayCreation) rightHandSide;
                    List<ArrayElement> elements = arrayCreation.getElements();
                    if (!elements.isEmpty()) {
                        for (ArrayElement arrayElement : elements) {
                            Expression value = arrayElement.getValue();
                            String typeName = VariousUtils.extractVariableTypeFromExpression(value, allAssignments);
                            ASTNode conditionalNode = findConditionalStatement(getPath());
                            VarAssignmentImpl vAssignment = varN.createAssignment(
                                    scope,
                                    conditionalNode != null,
                                    getBlockRange(conditionalNode, scope),
                                    new OffsetRange(var.getStartOffset(), var.getEndOffset()),
                                    typeName);
                            varN.addElement(vAssignment);
                            vAssignment.setAsArrayAccess(true);
                        }
                    } else {
                        String typeName = VariousUtils.extractVariableTypeFromExpression(rightHandSide, allAssignments);
                        ASTNode conditionalNode = findConditionalStatement(getPath());
                        VarAssignmentImpl varAssignment = varN.createAssignment(
                                scope,
                                conditionalNode != null,
                                getBlockRange(conditionalNode, scope),
                                new OffsetRange(var.getStartOffset(), var.getEndOffset()),
                                typeName);
                        varN.addElement(varAssignment);
                    }
                } else {
                    ASTNode conditionalNode = findConditionalStatement(getPath());
                    VarAssignmentImpl varAssignment = varN.createAssignment(
                            scope,
                            conditionalNode != null,
                            getBlockRange(conditionalNode, scope),
                            new OffsetRange(var.getStartOffset(), var.getEndOffset()),
                            node,
                            allAssignments);
                    varN.addElement(varAssignment);
                }
                occurencesBuilder.prepare((Variable) leftHandSide, scope);
            }
        } else if (leftHandSide instanceof FieldAccess) {
            FieldAccess fieldAccess = (FieldAccess) leftHandSide;
            VariableNameImpl varN = findVariable(modelBuilder.getCurrentScope(), fieldAccess.getDispatcher());
            if (varN != null) {
                varN.createLazyFieldAssignment(fieldAccess, node, scope);
            }
        } else if (leftHandSide instanceof StaticFieldAccess) {
            StaticFieldAccess staticFieldAccess = (StaticFieldAccess) leftHandSide;
            Expression className = staticFieldAccess.getClassName();
            String unqualifiedClassName = CodeUtils.extractUnqualifiedName(className);
            if (VariousUtils.isStaticClassName(unqualifiedClassName)) {
                VariableNameImpl varN = findVariable(modelBuilder.getCurrentScope(), "$this"); //NOI18N
                if (varN != null) {
                    varN.createLazyStaticFieldAssignment(staticFieldAccess, node, scope);
                }
            }
        }
        super.scan(rightHandSide);
    }

    @Override
    public void visit(ForEachStatement node) {
        Scope scope = modelBuilder.getCurrentScope();
        super.visit(node);
        Expression expression = node.getExpression();
        Expression value = node.getValue();
        if (value instanceof Variable) {
            VariableNameImpl varValue = findVariable(scope, (Variable) value);
            if (varValue != null) {
                varValue.setTypeResolutionKind(VariableNameImpl.TypeResolutionKind.MERGE_ASSIGNMENTS);
                if (expression instanceof Variable) {
                    VariableNameImpl varArray = findVariable(scope, (Variable) expression);
                    if (varArray != null) {
                        processVarComment(varArray.getName(), scope);
                        Collection<? extends String> typeNames = varArray.getArrayAccessTypeNames(node.getStartOffset());
                        for (String tpName : typeNames) {
                            VarAssignmentImpl varAssignment = varValue.createAssignment(
                                    scope,
                                    true,
                                    getBlockRange(scope),
                                    new OffsetRange(value.getStartOffset(),
                                    value.getEndOffset()),
                                    tpName);
                            varValue.addElement(varAssignment);
                        }
                    }
                } else {
                    String varType = VariousUtils.extractVariableTypeFromExpression(expression, getAssignmentMap(scope, (Variable) value));
                    if (varType != null) {
                        VarAssignmentImpl varAssignment = varValue.createAssignment(
                                scope,
                                true,
                                getBlockRange(scope),
                                new OffsetRange(value.getStartOffset(),
                                value.getEndOffset()),
                                varType);
                        varValue.addElement(varAssignment);
                    }
                }
            }
        }
    }


    @Override
    public void visit(FormalParameter node) {
        Expression parameterName = node.getParameterName();
        Expression parameterType = node.getParameterType();
        Scope scp = modelBuilder.getCurrentScope();
        if (scp instanceof FunctionScopeImpl) {
            FunctionScopeImpl fncScope = (FunctionScopeImpl) scp;
            while (parameterName instanceof Reference) {
                Reference ref = (Reference) parameterName;
                Expression expression = ref.getExpression();
                if (expression instanceof Variable || expression instanceof Reference) {
                    parameterName = expression;
                }
            }
            List<? extends ParameterElement> parameters = fncScope.getParameters();
            if (parameterName instanceof Variable) {
                for (ParameterElement parameter : parameters) {
                    Set<TypeResolver> types = parameter.getTypes();
                    String typeName = null;
                    for (TypeResolver typeResolver : types) {
                        if (typeResolver.isResolved()) {
                            QualifiedName typeQualifiedName = typeResolver.getTypeName(false);
                            if (typeQualifiedName != null) {
                                typeName = typeQualifiedName.toString();
                            }
                        }
                    }
                    VariableNameImpl var = createParameter(fncScope, parameter);
                    if (!types.isEmpty() && var != null) {
                        VarAssignmentImpl varAssignment = var.createAssignment(fncScope, false, fncScope.getBlockRange(), parameter.getOffsetRange(), typeName);
                        var.addElement(varAssignment);
                    }
                }
            }
            if (parameterName instanceof Variable) {
                if (parameterType instanceof NamespaceName) {
                    Kind[] kinds = {Kind.CLASS, Kind.IFACE};
                    occurencesBuilder.prepare(kinds, (NamespaceName) parameterType, fncScope);
                }
                occurencesBuilder.prepare((Variable) parameterName, fncScope);
            }
            super.visit(node);
        }
    }

    @Override
    public void visit(CatchClause node) {
        Variable variable = node.getVariable();
        Scope scope = modelBuilder.getCurrentScope();
        if (scope instanceof VariableNameFactory) {
            VariableNameImpl varNameImpl = createVariable((VariableNameFactory) scope, variable);
            if (varNameImpl != null) {
                VarAssignmentImpl varAssignment = varNameImpl.createAssignment(
                        scope,
                        true,
                        new OffsetRange(node.getStartOffset(), node.getEndOffset()),
                        VariableNameImpl.toOffsetRange(variable),
                        CodeUtils.extractQualifiedName(node.getClassName()));
                varNameImpl.addElement(varAssignment);
            }
        }
        Expression className = node.getClassName();
        if (className instanceof NamespaceName) {
            occurencesBuilder.prepare((NamespaceName) className, scope);
        } else {
            occurencesBuilder.prepare(Kind.CLASS, className, scope);
        }
        occurencesBuilder.prepare(variable, scope);
        scan(node.getBody());
    }

    @Override
    public void visit(LambdaFunctionDeclaration node) {
        ScopeImpl scope = modelBuilder.getCurrentScope();
        FunctionScopeImpl fncScope = FunctionScopeImpl.createElement(scope, node);
        List<Expression> lexicalVariables = node.getLexicalVariables();
        for (Expression expression : lexicalVariables) {
            if (expression instanceof Variable) {
                Variable variable = (Variable) expression;
                currentLexicalVariables.add(CodeUtils.extractVariableName(variable));
                VariableNameImpl varNameImpl = createVariable((VariableNameFactory) fncScope, variable);
                varNameImpl.setGloballyVisible(true);
            }
        }
        scan(lexicalVariables);
        modelBuilder.setCurrentScope(fncScope);
        scan(node.getFormalParameters());
        previousScope = scope;
        scan(node.getBody());
        previousScope = null;
        currentLexicalVariables.clear();
        modelBuilder.reset();
    }

    @Override
    public void visit(FunctionDeclaration node) {
        Scope scope = modelBuilder.getCurrentScope();
        assert (scope instanceof FunctionScope) || (scope instanceof NamespaceScopeImpl);
        while (!(scope instanceof NamespaceScope)) {
            scope = scope.getInScope();
        }
        FunctionScopeImpl fncScope = ((NamespaceScopeImpl) scope).createElement(modelBuilder.getProgram(), node);
        modelBuilder.setCurrentScope(fncScope);
        occurencesBuilder.prepare(node, fncScope);
        markerBuilder.prepare(node, fncScope);
        checkComments(node);
        scan(node.getFormalParameters());
        scan(node.getBody());
        modelBuilder.reset();
    }

    @Override
    public void visit(FunctionInvocation node) {
        Scope scope = modelBuilder.getCurrentScope();
        Expression functionName = node.getFunctionName().getName();
        if (functionName instanceof Variable) {
            Variable variable = (Variable) functionName;
            scan(variable);
        } else {
            occurencesBuilder.prepare(node, scope);
            if (functionName instanceof NamespaceName) {
                occurencesBuilder.prepare((NamespaceName) functionName, scope);
            }
        }
        ASTNodeInfo<FunctionInvocation> nodeInfo = ASTNodeInfo.create(node);
        String name = nodeInfo.getName();
        if ("define".equals(name) && (node.getParameters().size() == 2 || node.getParameters().size() == 3)) { //NOI18N
            Expression d = node.getParameters().get(0);
            if (d instanceof Scalar && ((Scalar) d).getScalarType() == Type.STRING) {
                Scalar scalar = (Scalar) d;
                String value = scalar.getStringValue();
                if (NavUtils.isQuoted(value)) {
                    ASTNodeInfo<Scalar> scalarInfo = ASTNodeInfo.create(Kind.CONSTANT, scalar);
                    Expression parameterExpression = node.getParameters().get(1);
                    String parameterValue = (parameterExpression instanceof Scalar)
                            ? ((Scalar) parameterExpression).getStringValue() : null;
                    ScalarConstantElementImpl constantImpl = modelBuilder.getCurrentNameSpace().
                            createConstantElement(scalarInfo, parameterValue);
                    occurencesBuilder.prepare(scalarInfo, constantImpl);
                }
            }
        } else if ("constant".equals(name) && node.getParameters().size() == 1) { //NOI18N
            Expression d = node.getParameters().get(0);
            if (d instanceof Scalar) {
                Scalar scalar = (Scalar) d;
                if (scalar.getScalarType() == Type.STRING && NavUtils.isQuoted(scalar.getStringValue())) {
                    occurencesBuilder.prepare(Kind.CONSTANT, scalar, fileScope);
                }

            }

        }

        super.visit(node);
    }

    @Override
    public void visit(StaticFieldAccess node) {
        Scope scope = modelBuilder.getCurrentScope();
        occurencesBuilder.prepare(node, scope);
        Expression className = node.getClassName();
        if (className instanceof Variable) {
            scan(className);
        } else if (className instanceof NamespaceName) {
            occurencesBuilder.prepare((NamespaceName) className, scope);
        }
        Variable field = node.getField();
        if (field instanceof ArrayAccess) {
            ArrayAccess access = (ArrayAccess) field;
            scan(access.getDimension());
            VariableBase name = access.getName();
            while (name instanceof ArrayAccess) {
                ArrayAccess access1 = (ArrayAccess) name;
                scan(access1.getDimension());
                name = access1.getName();
            }
        }
    }

    @Override
    public void visit(PHPDocTypeTag node) {
        occurencesBuilder.prepare(node, modelBuilder.getCurrentScope());
        super.visit(node);
    }

    @Override
    public void visit(PHPDocVarTypeTag node) {
        Scope currentScope = modelBuilder.getCurrentScope();
        StringBuilder sb = new StringBuilder();
        List<? extends PhpDocTypeTagInfo> tagInfos = PhpDocTypeTagInfo.create(node, currentScope);
        for (Iterator<? extends PhpDocTypeTagInfo> it = tagInfos.iterator(); it.hasNext();) {
            PhpDocTypeTagInfo phpDocTypeTagInfo = it.next();
            if (phpDocTypeTagInfo.getKind().equals(Kind.FIELD) && !phpDocTypeTagInfo.getName().isEmpty()) {
                String typeName = phpDocTypeTagInfo.getTypeName();
                StringBuilder fqNames = new StringBuilder();
                if (typeName != null) {
                    if (sb.length() > 0) {
                        sb.append("|"); //NOI18N
                    }
                    if (fqNames.length() > 0) {
                        fqNames.append("|"); //NOI18N
                    }
                    String qualifiedTypeNames = VariousUtils.qualifyTypeNames(typeName, node.getStartOffset(), currentScope);
                    fqNames.append(qualifiedTypeNames);
                    sb.append(typeName);
                }
                if ((currentScope instanceof ClassScope || currentScope instanceof TraitScope) && !it.hasNext()) {
                    new FieldElementImpl(currentScope, sb.length() > 0 ? sb.toString() : null, fqNames.length() > 0 ? fqNames.toString() : null, phpDocTypeTagInfo);
                }
            } else if (node.getKind().equals(PHPDocTag.Type.GLOBAL) && phpDocTypeTagInfo.getKind().equals(Kind.VARIABLE)) {
                final String typeName = phpDocTypeTagInfo.getTypeName();
                final String varName = phpDocTypeTagInfo.getName();
                VariableScope variableScope = getVariableScope(node.getStartOffset());
                if (variableScope != null) {
                    VariableNameImpl varN = findVariable(variableScope, varName);
                    if (varN == null && variableScope instanceof VariableNameFactory) {
                        VariableNameFactory factory = (VariableNameFactory) variableScope;
                        final OffsetRange nameRange = new OffsetRange(node.getStartOffset(), node.getEndOffset());
                        varN = new VariableNameImpl(factory, varName, variableScope.getFile(), nameRange, true);
                    }
                    if (varN != null) {
                        VarAssignmentImpl varAssignment = varN.createAssignment(variableScope, false, variableScope.getBlockRange(), varN.getNameRange(), typeName);
                        varN.addElement(varAssignment);
                    }
                }
            }
        }

        occurencesBuilder.prepare(node, currentScope);
        super.visit(node);
    }

    public FileScope getFileScope() {
        return fileScope;
    }

    public synchronized IndexScope getIndexScope() {
        if (indexScope == null) {
            indexScope = new IndexScopeImpl(info);
        }
        return indexScope;
    }

    @CheckForNull
    public CodeMarker getCodeMarker(int offset) {
        buildCodeMarks(offset);
        return findStrictCodeMarker((FileScopeImpl) getFileScope(), offset, null);
    }

    private void checkComments(ASTNode node) {
        Comment comment = node instanceof Comment ? (Comment) node : Utils.getCommentForNode(modelBuilder.getProgram(), node);
        if (comment instanceof PHPDocBlock) {
            PHPDocBlock phpDoc = (PHPDocBlock) comment;
            for (PHPDocTag tag : phpDoc.getTags()) {
                scan(tag);
            }
        } else if (comment instanceof PHPVarComment) {
            PHPDocVarTypeTag typeTag = ((PHPVarComment) comment).getVariable();
            List<? extends PhpDocTypeTagInfo> tagInfos = PhpDocTypeTagInfo.create(typeTag, fileScope);
            for (PhpDocTypeTagInfo tagInfo : tagInfos) {
                if (tagInfo.getKind().equals(ASTNodeInfo.Kind.VARIABLE)) {
                    String name = tagInfo.getName();
                    List<PhpDocTypeTagInfo> infos = varTypeComments.get(name);
                    if (infos == null) {
                        infos = new ArrayList<>();
                        varTypeComments.put(name, infos);
                    }
                    infos.add(tagInfo);
                }
            }
        }
    }

    private VariableNameImpl findVariable(Scope scope, String varName) {
        VariableNameImpl retval = null;
        Scope scopeToInspect = scope;
        if (varName != null) {
            Map<String, VariableNameImpl> varnames = vars.get(scopeToInspect);
            while (scopeToInspect != null) {
                if (varnames != null) {
                    retval = varnames.get(varName);
                    if (retval != null) {
                        break;
                    }
                }
                scopeToInspect = scopeToInspect.getInScope();
                varnames = vars.get(scopeToInspect);
            }
        }
        return retval;
    }

    private VariableNameImpl findVariable(Scope scope, final VariableBase leftHandSide) {
        String varName = null;
        if (leftHandSide instanceof Variable) {
            varName = VariableNameImpl.toName((Variable) leftHandSide);
        }
        return varName != null ? findVariable(scope, varName) : null;
    }

    private VariableNameImpl createParameter(FunctionScopeImpl fncScope, ParameterElement parameter) {
        VariableNameFactory varContainer = (VariableNameFactory) fncScope;
        Map<String, VariableNameImpl> map = vars.get(varContainer);
        if (map == null) {
            map = new HashMap<>();
            vars.put(varContainer, map);
        }
        String name = parameter.getName();
        VariableNameImpl varInstance = map.get(name);
        if (varInstance == null) {
            if (ModelUtils.filter(varContainer.getDeclaredVariables(), name).isEmpty()) {
                varInstance = new VariableNameImpl(fncScope, name, fncScope.getFile(), parameter.getOffsetRange(), false);
                fncScope.addElement(varInstance);
                map.put(name, varInstance);
            }
        }
        return varInstance;
    }

    private VariableNameImpl createVariable(VariableNameFactory varContainer, Variable node) {
        Map<String, VariableNameImpl> map = vars.get(varContainer);
        if (map == null) {
            map = new HashMap<>();
            vars.put(varContainer, map);
        }
        String name = VariableNameImpl.toName(node);
        VariableNameImpl retval = map.get(name);
        if (retval == null) {
            if (ModelUtils.filter(varContainer.getDeclaredVariables(), name).isEmpty()) {
                retval = varContainer.createElement(node);
                map.put(name, retval);
            }
        }
        return retval;
    }

    @CheckForNull
    private ASTNode findConditionalStatement(List<ASTNode> path) {
        for (ASTNode aSTNode : path) {
            if (aSTNode instanceof IfStatement) {
                return aSTNode;
            } else if (aSTNode instanceof WhileStatement) {
                return aSTNode;
            } else if (aSTNode instanceof DoStatement) {
                return aSTNode;
            } else if (aSTNode instanceof ForEachStatement) {
                return aSTNode;
            } else if (aSTNode instanceof ForStatement) {
                return aSTNode;
            } else if (aSTNode instanceof CatchClause) {
                return aSTNode;
            } else if (aSTNode instanceof SwitchStatement) {
                return aSTNode;
            } else if (aSTNode instanceof TryStatement) {
                return aSTNode;
            } else if (aSTNode instanceof InstanceOfExpression) {
                return aSTNode;
            }
        }
        return null;
    }

    private CodeMarker findStrictCodeMarker(FileScopeImpl scope, int offset, CodeMarker atOffset) {
        CodeMarker result = atOffset;
        List<? extends CodeMarker> markers = scope.getMarkers();
        for (CodeMarker codeMarker : markers) {
            assert codeMarker != null;
            if (codeMarker.containsInclusive(offset)) {
                result = codeMarker;
            }
        }
        return result;
    }

    @CheckForNull
    public Occurence getOccurence(int offset) {
        if (occurencesBuilder != null) {
            return occurencesBuilder.build(fileScope, offset);
        }
        return null;
    }

    @CheckForNull
    public List<Occurence> getOccurence(ModelElement element) {
        if (occurencesBuilder != null) {
            return occurencesBuilder.build(fileScope, element);
        }
        return Collections.emptyList();
    }

    public ModelElement findDeclaration(PhpElement element) {
        final int offset = element.getOffset();
        final List<? extends ModelElement> elements = ModelUtils.getElements(getFileScope(), true);
        ModelElement possibleElement = null;
        final OffsetRange nameOffsetRange = new OffsetRange(offset, offset + element.getName().length());
        for (ModelElement modelElement : elements) {
            if (modelElement.getNameRange().overlaps(nameOffsetRange)) {
                if (possibleElement == null || contains(possibleElement.getNameRange(), modelElement.getNameRange())) {
                    possibleElement = modelElement;
                }
            }
        }
        return possibleElement;
    }

    private static boolean contains(final OffsetRange outer, final OffsetRange inner) {
        return inner.getStart() >= outer.getStart() && inner.getEnd() <= outer.getEnd();
    }

    public VariableScope getNearestVariableScope(int offset) {
        return VariableScopeFinder.create().findNearestVarScope((FileScopeImpl) getFileScope(), offset, null);
    }

    public VariableScope getVariableScope(int offset) {
        return getVariableScope(offset, VariableScopeFinder.ScopeRangeAcceptor.BLOCK);
    }

    public VariableScope getVariableScope(int offset, VariableScopeFinder.ScopeRangeAcceptor scopeRangeAcceptor) {
        return VariableScopeFinder.create().find(getFileScope(), offset, scopeRangeAcceptor);
    }

    private void buildCodeMarks(final int offset) {
        if (markerBuilder != null) {
            fileScope.clearMarkers();
            markerBuilder.build(fileScope, offset);
        }
    }

    private OffsetRange getBlockRange(Scope currentScope) {
        ASTNode conditionalNode = findConditionalStatement(getPath());
        return getBlockRange(conditionalNode, currentScope);
    }
    private OffsetRange getBlockRange(ASTNode conditionalNode, Scope currentScope) {
        OffsetRange scopeRange = (conditionalNode != null) ? new OffsetRange(conditionalNode.getStartOffset(), conditionalNode.getEndOffset()) : currentScope.getBlockRange();
        return scopeRange;
    }
    private void handleVarComments() {
        Set<String> varCommentNames = varTypeComments.keySet();
        for (String name : new HashSet<>(varCommentNames)) {
            handleVarComment(name);
        }
    }

    private void handleVarComment(final String name) {
        Parameters.notNull("name", name); //NOI18N
        List<PhpDocTypeTagInfo> varComments = varTypeComments.get(name); //varComments.size() varTypeComments.size()
        if (varComments != null) {
            for (PhpDocTypeTagInfo phpDocTypeTagInfo : new ArrayList<>(varComments)) {
                VariableScope varScope = getVariableScope(phpDocTypeTagInfo.getRange().getStart());
                if (varScope != null) {
                    handleVarAssignment(name, varScope, phpDocTypeTagInfo);
                }
            }
        }
    }

    private void handleVarAssignment(final String name, final VariableScope varScope, final PhpDocTypeTagInfo phpDocTypeTagInfo) {
        VariableNameImpl varInstance = (VariableNameImpl) ModelUtils.getFirst(ModelUtils.filter(varScope.getDeclaredVariables(), name));
        if (varInstance == null) {
            varInstance = new VariableNameImpl(varScope, name, varScope.getFile(), phpDocTypeTagInfo.getRange(), varScope instanceof NamespaceScopeImpl);
        } else {
            varInstance.setTypeResolutionKind(VariableNameImpl.TypeResolutionKind.MERGE_ASSIGNMENTS);
        }
        ASTNode conditionalNode = findConditionalStatement(getPath());
        VarAssignmentImpl varAssignment = varInstance.createAssignment(
                (Scope) varScope,
                conditionalNode != null,
                getBlockRange(varScope),
                phpDocTypeTagInfo.getRange(),
                phpDocTypeTagInfo.getTypeName());
        varInstance.addElement(varAssignment);
        occurencesBuilder.prepare(phpDocTypeTagInfo.getTypeTag(), varScope);
    }

    private void processVarComment(final String variableName, final Scope variableScope) {
        Parameters.notNull("variableName", variableName); //NOI18N
        Parameters.notNull("variableScope", variableScope); //NOI18N
        List<PhpDocTypeTagInfo> varComments = varTypeComments.get(variableName);
        if (varComments != null) {
            for (PhpDocTypeTagInfo phpDocTypeTagInfo : new ArrayList<>(varComments)) {
                VariableScope varScope = getVariableScope(phpDocTypeTagInfo.getRange().getStart());
                if (variableScope.equals(varScope)) {
                    handleVarAssignment(variableName, varScope, phpDocTypeTagInfo);
                }
            }
        }
    }

    private void prepareVarComments(Program program) {
        List<Comment> comments = program.getComments();
        for (Comment comment : comments) {
            Comment.Type type = comment.getCommentType();
            if (type.equals(Comment.Type.TYPE_VARTYPE)) {
                checkComments(comment);
            }
        }
    }

    void scanNoLazy(ASTNode node, Scope inScope) {
        // Remember the old scope. It can happen that will be needed scanned constructor
        // in non lazy mode as well.
        Scope originalScope = modelBuilder.getCurrentScope();
        modelBuilder.prepareForScope(inScope);
        lazyScan = false;
        scan(node);
        // set the original scope back.
        modelBuilder.prepareForScope(originalScope);
    }

}
