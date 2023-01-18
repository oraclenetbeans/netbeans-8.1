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
package org.netbeans.modules.php.editor.csl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.netbeans.modules.csl.api.ColoringAttributes;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.SemanticAnalyzer;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ElementFilter;
import org.netbeans.modules.php.editor.api.elements.FieldElement;
import org.netbeans.modules.php.editor.api.elements.FunctionElement;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.TypeConstantElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.VariableScope;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayAccess;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.BodyDeclaration.Modifier;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ConstantDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.FieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.FieldsDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionName;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceName;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTypeNode;
import org.netbeans.modules.php.editor.parser.astnodes.PHPVarComment;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.StaticConstantAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticFieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitMethodAliasDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TypeDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatementPart;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultTreePathVisitor;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.netbeans.modules.php.project.api.PhpAnnotations;

/**
 *
 * @author Petr Pisl
 */
public class SemanticAnalysis extends SemanticAnalyzer {

    public static final EnumSet<ColoringAttributes> UNUSED_FIELD_SET = EnumSet.of(ColoringAttributes.UNUSED, ColoringAttributes.FIELD);
    public static final EnumSet<ColoringAttributes> DEPRECATED_UNUSED_FIELD_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.UNUSED, ColoringAttributes.FIELD);
    public static final EnumSet<ColoringAttributes> DEPRECATED_FIELD_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.FIELD);
    public static final EnumSet<ColoringAttributes> UNUSED_STATIC_FIELD_SET = EnumSet.of(ColoringAttributes.UNUSED, ColoringAttributes.FIELD, ColoringAttributes.STATIC);
    public static final EnumSet<ColoringAttributes> DEPRECATED_UNUSED_STATIC_FIELD_SET = EnumSet.of(
            ColoringAttributes.DEPRECATED,
            ColoringAttributes.UNUSED,
            ColoringAttributes.FIELD,
            ColoringAttributes.STATIC);
    public static final EnumSet<ColoringAttributes> DEPRECATED_STATIC_FIELD_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.FIELD, ColoringAttributes.STATIC);
    public static final EnumSet<ColoringAttributes> UNUSED_METHOD_SET = EnumSet.of(ColoringAttributes.UNUSED, ColoringAttributes.METHOD);
    public static final EnumSet<ColoringAttributes> DEPRECATED_UNUSED_METHOD_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.UNUSED, ColoringAttributes.METHOD);
    public static final EnumSet<ColoringAttributes> STATIC_METHOD_SET = EnumSet.of(ColoringAttributes.STATIC, ColoringAttributes.METHOD);
    public static final EnumSet<ColoringAttributes> DEPRECATED_STATIC_METHOD_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.STATIC, ColoringAttributes.METHOD);
    public static final EnumSet<ColoringAttributes> DEPRECATED_METHOD_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.METHOD);
    public static final EnumSet<ColoringAttributes> UNUSED_STATIC_METHOD_SET = EnumSet.of(ColoringAttributes.STATIC, ColoringAttributes.METHOD, ColoringAttributes.UNUSED);
    public static final EnumSet<ColoringAttributes> DEPRECATED_UNUSED_STATIC_METHOD_SET = EnumSet.of(
            ColoringAttributes.DEPRECATED,
            ColoringAttributes.STATIC,
            ColoringAttributes.METHOD,
            ColoringAttributes.UNUSED);
    public static final EnumSet<ColoringAttributes> DEPRECATED_CLASS_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.CLASS);
    public static final EnumSet<ColoringAttributes> DEPRECATED_SET = EnumSet.of(ColoringAttributes.DEPRECATED);
    public static final EnumSet<ColoringAttributes> DEPRECATED_STATIC_SET = EnumSet.of(ColoringAttributes.DEPRECATED, ColoringAttributes.STATIC);
    public static final EnumSet<ColoringAttributes> ANNOTATION_TYPE_SET = EnumSet.of(ColoringAttributes.ANNOTATION_TYPE);
    public static final EnumSet<ColoringAttributes> METHOD_INVOCATION_SET = EnumSet.of(ColoringAttributes.CUSTOM1);
    public static final EnumSet<ColoringAttributes> STATIC_METHOD_INVOCATION_SET = EnumSet.of(ColoringAttributes.STATIC, ColoringAttributes.CUSTOM1);
    private static final Logger LOGGER = Logger.getLogger(SemanticAnalysis.class.getName());
    private static boolean isLogged = false;
    private volatile boolean cancelled;
    private boolean checkIfResolveDeprecatedElements = true;
    private boolean isResolveDeprecatedElements = false;
    private Map<OffsetRange, Set<ColoringAttributes>> semanticHighlights;

    public SemanticAnalysis() {
        semanticHighlights = null;
    }

    private static void setIsLogged(boolean isLogged) {
        SemanticAnalysis.isLogged = isLogged;
    }

    private static boolean isLogged() {
        return isLogged;
    }

    @Override
    public Map<OffsetRange, Set<ColoringAttributes>> getHighlights() {
        return semanticHighlights;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public void run(Result r, SchedulerEvent event) {
        checkIfResolveDeprecatedElements = true;
        if (isResolveDeprecatedElements()) {
            if (!isLogged()) {
                LOGGER.info("Resolving of deprecated elements in Semantic analysis - IDE will be possibly slow!");
                setIsLogged(true);
            }
        }
        resume();

        if (isCancelled()) {
            return;
        }
        process(r);
    }

    void process(Result r) {
        PHPParseResult result = (PHPParseResult) r;
        Map<OffsetRange, Set<ColoringAttributes>> highlights = new HashMap<>(100);
        if (result.getProgram() != null) {
            SemanticHighlightVisitor semanticHighlightVisitor = new SemanticHighlightVisitor(highlights, result.getSnapshot(), result.getModel());
            result.getProgram().accept(semanticHighlightVisitor);
            if (highlights.size() > 0) {
                semanticHighlights = highlights;
            } else {
                semanticHighlights = null;
            }
        }
    }

    protected final boolean isCancelled() {
        return cancelled;
    }

    protected final void resume() {
        cancelled = false;
    }

    protected boolean isResolveDeprecatedElements() {
        if (checkIfResolveDeprecatedElements) {
            isResolveDeprecatedElements = PhpAnnotations.getDefault().isResolveDeprecatedElements();
            checkIfResolveDeprecatedElements = false;
        }
        return isResolveDeprecatedElements;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    private class SemanticHighlightVisitor extends DefaultTreePathVisitor {

        private class ASTNodeColoring {
            public ASTNode identifier;
            public Set<ColoringAttributes> coloring;

            public ASTNodeColoring(ASTNode identifier, Set<ColoringAttributes> coloring) {
                this.identifier = identifier;
                this.coloring = coloring;
            }
        }

        Map<OffsetRange, Set<ColoringAttributes>> highlights;
        // for unused private fields: name, varible
        // if isused, then it's deleted from the list and marked as the field
        private final Map<UnusedIdentifier, ASTNodeColoring> privateFieldsUnused;
        // for unsed private method: name, identifier
        private final Map<UnusedIdentifier, ASTNodeColoring> privateUnusedMethods;
        // this is holder of blocks, which has to be scanned for usages in the class.
        private List<Block> needToScan = new ArrayList<>();

        private final Snapshot snapshot;

        private final Model model;

        private Set<TypeElement> deprecatedTypes;

        private Set<MethodElement> deprecatedMethods;

        private Set<FieldElement> deprecatedFields;

        private Set<TypeConstantElement> deprecatedConstants;

        private Set<FunctionElement> deprecatedFunctions;

        // last visited type declaration
        private TypeDeclaration typeDeclaration;

        public SemanticHighlightVisitor(Map<OffsetRange, Set<ColoringAttributes>> highlights, Snapshot snapshot, Model model) {
            this.highlights = highlights;
            privateFieldsUnused = new HashMap<>();
            privateUnusedMethods = new HashMap<>();
            this.snapshot = snapshot;
            this.model = model;
        }

        private Set<TypeElement> getDeprecatedTypes() {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            if (deprecatedTypes == null) {
                deprecatedTypes = ElementFilter.forDeprecated(true).filter(model.getIndexScope().getIndex().getTypes(NameKind.empty()));
            }
            return deprecatedTypes;
        }

        private Set<MethodElement> getDeprecatedMethods() {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            if (deprecatedMethods == null) {
                deprecatedMethods = ElementFilter.forDeprecated(true).filter(model.getIndexScope().getIndex().getMethods(NameKind.empty()));
            }
            return deprecatedMethods;
        }

        private Set<FunctionElement> getDeprecatedFunctions() {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            if (deprecatedFunctions == null) {
                deprecatedFunctions = ElementFilter.forDeprecated(true).filter(model.getIndexScope().getIndex().getFunctions(NameKind.empty()));
            }
            return deprecatedFunctions;
        }

        private Set<FieldElement> getDeprecatedFields() {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            if (deprecatedFields == null) {
                deprecatedFields = ElementFilter.forDeprecated(true).filter(model.getIndexScope().getIndex().getFields(NameKind.empty()));
            }
            return deprecatedFields;
        }

        private Set<TypeConstantElement> getDeprecatedConstants() {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            if (deprecatedConstants == null) {
                deprecatedConstants = ElementFilter.forDeprecated(true).filter(model.getIndexScope().getIndex().getTypeConstants(NameKind.empty()));
            }
            return deprecatedConstants;
        }

        private void addColoringForNode(ASTNode node, Set<ColoringAttributes> coloring) {
            int start = snapshot.getOriginalOffset(node.getStartOffset());
            if (start > -1) {
                int end = start + node.getEndOffset() - node.getStartOffset();
                assert coloring != null : snapshot.getText().toString();
                highlights.put(new OffsetRange(start, end), coloring);
            }
        }

        @Override
        public void scan(ASTNode node) {
            if (!isCancelled()) {
                super.scan(node);
            }
        }

        @Override
        public void visit(Program program) {
            if (isCancelled()) {
                return;
            }
            scan(program.getStatements());
            scan(program.getComments());
            // are there unused private methods?
            for (ASTNodeColoring item : privateUnusedMethods.values()) {
                if (item.coloring.contains(ColoringAttributes.STATIC)) {
                    if (item.coloring.contains(ColoringAttributes.DEPRECATED)) {
                        addColoringForNode(item.identifier, DEPRECATED_UNUSED_STATIC_METHOD_SET);
                    } else {
                        addColoringForNode(item.identifier, UNUSED_STATIC_METHOD_SET);
                    }
                } else {
                    if (item.coloring.contains(ColoringAttributes.DEPRECATED)) {
                        addColoringForNode(item.identifier, DEPRECATED_UNUSED_METHOD_SET);
                    } else {
                        addColoringForNode(item.identifier, UNUSED_METHOD_SET);
                    }
                }
            }
        }

        @Override
        public void visit(ClassDeclaration cldec) {
            if (isCancelled()) {
                return;
            }
            addToPath(cldec);
            this.typeDeclaration = cldec;
            scan(cldec.getSuperClass());
            scan(cldec.getInterfaes());
            Identifier name = cldec.getName();
            addColoringForNode(name, createTypeNameColoring(name));
            needToScan = new ArrayList<>();
            if (cldec.getBody() != null) {
                cldec.getBody().accept(this);

                // find all usages in the method bodies
                for (Block block : needToScan) {
                    block.accept(this);
                }
                // are there unused private fields?
                for (ASTNodeColoring item : privateFieldsUnused.values()) {
                    if (item.coloring.contains(ColoringAttributes.STATIC)) {
                        if (item.coloring.contains(ColoringAttributes.DEPRECATED)) {
                            addColoringForNode(item.identifier, DEPRECATED_UNUSED_STATIC_FIELD_SET);
                        } else {
                            addColoringForNode(item.identifier, UNUSED_STATIC_FIELD_SET);
                        }
                    } else {
                        if (item.coloring.contains(ColoringAttributes.DEPRECATED)) {
                            addColoringForNode(item.identifier, DEPRECATED_UNUSED_FIELD_SET);
                        } else {
                            addColoringForNode(item.identifier, UNUSED_FIELD_SET);
                        }
                    }
                }
            }
        }

        private Set<ColoringAttributes> createTypeNameColoring(Identifier typeName) {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            Set<ColoringAttributes> result;
            if (isDeprecatedTypeDeclaration(typeName)) {
                result = DEPRECATED_CLASS_SET;
            } else {
                result = ColoringAttributes.CLASS_SET;
            }
            return result;
        }

        private boolean isDeprecatedTypeDeclaration(Identifier typeName) {
            boolean isDeprecated = false;
            if (!isCancelled() && isResolveDeprecatedElements()) {
                VariableScope variableScope = model.getVariableScope(typeName.getStartOffset());
                QualifiedName fullyQualifiedName = VariousUtils.getFullyQualifiedName(QualifiedName.create(typeName), typeName.getStartOffset(), variableScope);
                for (TypeElement typeElement : getDeprecatedTypes()) {
                    if (typeElement.getFullyQualifiedName().equals(fullyQualifiedName)) {
                        isDeprecated = true;
                        break;
                    }
                }
            }
            return isDeprecated;
        }

        @Override
        public void visit(FunctionDeclaration node) {
            if (isCancelled()) {
                return;
            }
            Identifier functionName = node.getFunctionName();
            if (isDeprecatedFunctionDeclaration(functionName)) {
                addColoringForNode(functionName, DEPRECATED_SET);
            }
            super.visit(node);
        }

        private boolean isDeprecatedFunctionDeclaration(Identifier functionName) {
            boolean isDeprecated = false;
            if (!isCancelled() && isResolveDeprecatedElements()) {
                for (FunctionElement functionElement : getDeprecatedFunctions()) {
                    if (functionElement.getName().equals(functionName.getName())) {
                        isDeprecated = true;
                        break;
                    }
                }
            }
            return isDeprecated;
        }

        @Override
        public void visit(MethodDeclaration md) {
            if (isCancelled()) {
                return;
            }
            scan(md.getFunction().getFormalParameters());
            boolean isPrivate = Modifier.isPrivate(md.getModifier());
            Identifier identifier = md.getFunction().getFunctionName();
            String name = identifier.getName().toLowerCase();
            Set<ColoringAttributes> coloring = createMethodDeclarationColoring(md);
            // don't color private magic private method. methods which start __
            if (isPrivate && name != null && !name.startsWith("__")) {
                privateUnusedMethods.put(new UnusedIdentifier(name, typeDeclaration), new ASTNodeColoring(identifier, coloring));
            } else {
                // color now only non private method
                addColoringForNode(identifier, coloring);
            }
            if (!Modifier.isAbstract(md.getModifier())) {
                // don't scan the body now. It should be scanned after all declarations
                // are known
                Block body = md.getFunction().getBody();
                if (body != null) {
                    needToScan.add(body);
                }
            }
        }

        private Set<ColoringAttributes> createMethodDeclarationColoring(MethodDeclaration methodDeclaration) {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            boolean isDeprecated = isDeprecatedMethodDeclaration(methodDeclaration.getFunction().getFunctionName());
            Set<ColoringAttributes> coloring = isDeprecated ? DEPRECATED_METHOD_SET : ColoringAttributes.METHOD_SET;
            if (Modifier.isStatic(methodDeclaration.getModifier())) {
                coloring = isDeprecated ? DEPRECATED_STATIC_METHOD_SET : STATIC_METHOD_SET;
            }
            return coloring;
        }

        private boolean isDeprecatedMethodDeclaration(Identifier methodName) {
            boolean isDeprecated = false;
            if (!isCancelled() && isResolveDeprecatedElements()) {
                VariableScope variableScope = model.getVariableScope(methodName.getStartOffset());
                QualifiedName typeFullyQualifiedName = VariousUtils.getFullyQualifiedName(
                        QualifiedName.create(typeDeclaration.getName()),
                        methodName.getStartOffset(),
                        variableScope);
                for (MethodElement methodElement : getDeprecatedMethods()) {
                    if (methodElement.getName().equals(methodName.getName()) && methodElement.getType().getFullyQualifiedName().equals(typeFullyQualifiedName)) {
                        isDeprecated = true;
                        break;
                    }
                }
            }
            return isDeprecated;
        }

        @Override
        public void visit(TraitMethodAliasDeclaration node) {
            if (isCancelled()) {
                return;
            }
            if (node.getNewMethodName() != null) {
                addColoringForNode(node.getNewMethodName(), ColoringAttributes.METHOD_SET);
            }
        }

        @Override
        public void visit(MethodInvocation node) {
            if (isCancelled()) {
                return;
            }
            Identifier identifier = null;
            if (node.getMethod().getFunctionName().getName() instanceof Variable) {
                Variable variable = (Variable) node.getMethod().getFunctionName().getName();
                if (variable.getName() instanceof Identifier) {
                    identifier = (Identifier) variable.getName();
                }
            } else if (node.getMethod().getFunctionName().getName() instanceof Identifier) {
                identifier = (Identifier) node.getMethod().getFunctionName().getName();
            }
            if (identifier != null) {
                ASTNodeColoring item = privateUnusedMethods.remove(new UnusedIdentifier(identifier.getName().toLowerCase(), typeDeclaration));
                if (item != null) {
                    addColoringForNode(item.identifier, item.coloring);
                }
                addColoringForNode(identifier, METHOD_INVOCATION_SET);
            }
            super.visit(node);
        }

        @Override
        public void visit(InterfaceDeclaration node) {
            if (isCancelled()) {
                return;
            }
            typeDeclaration = node;
            Identifier name = node.getName();
            addColoringForNode(name, createTypeNameColoring(name));
            super.visit(node);
        }

        @Override
        public void visit(TraitDeclaration node) {
            if (isCancelled()) {
                return;
            }
            typeDeclaration = node;
            Identifier name = node.getName();
            addColoringForNode(name, createTypeNameColoring(name));
            needToScan = new ArrayList<>();
            if (node.getBody() != null) {
                node.getBody().accept(this);
                for (Block block : needToScan) {
                    block.accept(this);
                }
            }
            super.visit(node);
        }

        @Override
        public void visit(FieldsDeclaration node) {
            if (isCancelled()) {
                return;
            }
            boolean isPrivate = Modifier.isPrivate(node.getModifier());
            boolean isStatic = Modifier.isStatic(node.getModifier());
            Variable[] variables = node.getVariableNames();
            for (int i = 0; i < variables.length; i++) {
                Variable variable = variables[i];
                Set<ColoringAttributes> coloring = createFieldDeclarationColoring(variable, isStatic);
                if (!isPrivate) {
                    addColoringForNode(variable.getName(), coloring);
                } else {
                    if (variable.getName() instanceof Identifier) {
                        Identifier identifier =  (Identifier) variable.getName();
                        privateFieldsUnused.put(new UnusedIdentifier(identifier.getName(), typeDeclaration), new ASTNodeColoring(identifier, coloring));
                    }
                }
            }
            super.visit(node);
        }

        private Set<ColoringAttributes> createFieldDeclarationColoring(Variable variable, boolean isStatic) {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            boolean isDeprecated = isDeprecatedFieldDeclaration(variable);
            Set<ColoringAttributes> coloring = isDeprecated ? DEPRECATED_FIELD_SET : ColoringAttributes.FIELD_SET;
            if (isStatic) {
                coloring = isDeprecated ? DEPRECATED_STATIC_FIELD_SET : ColoringAttributes.STATIC_FIELD_SET;
            }
            return coloring;
        }

        private boolean isDeprecatedFieldDeclaration(Variable variable) {
            boolean isDeprecated = false;
            if (!isCancelled() && isResolveDeprecatedElements()) {
                String variableName = CodeUtils.extractVariableName(variable);
                VariableScope variableScope = model.getVariableScope(variable.getStartOffset());
                QualifiedName typeFullyQualifiedName = VariousUtils.getFullyQualifiedName(
                        QualifiedName.create(typeDeclaration.getName()),
                        variable.getStartOffset(),
                        variableScope);
                for (FieldElement fieldElement : getDeprecatedFields()) {
                    if (fieldElement.getName().equals(variableName) && fieldElement.getType().getFullyQualifiedName().equals(typeFullyQualifiedName)) {
                        isDeprecated = true;
                        break;
                    }
                }
            }
            return isDeprecated;
        }

        @Override
        public void visit(FieldAccess node) {
            if (isCancelled()) {
                return;
            }
            if (!node.getField().isDollared()) {
                new FieldAccessVisitor(ColoringAttributes.FIELD_SET).scan(node.getField().getName());
            }
            scan(node.getField());
            super.scan(node.getDispatcher());
        }

        @Override
        public void visit(StaticMethodInvocation node) {
            if (isCancelled()) {
                return;
            }
            FunctionName fnName = node.getMethod().getFunctionName();
            if (fnName.getName() instanceof Identifier) {
                Identifier identifier = (Identifier) fnName.getName();
                String name = identifier.getName().toLowerCase();
                ASTNodeColoring item = privateUnusedMethods.remove(new UnusedIdentifier(name, typeDeclaration));
                if (item != null) {
                    addColoringForNode(item.identifier, item.coloring);
                }
            }
            addColoringForNode(fnName, STATIC_METHOD_INVOCATION_SET);
            super.visit(node);
        }

        @Override
        public void visit(PHPVarComment node) {
            if (isCancelled()) {
                return;
            }
            int start = node.getVariable().getStartOffset();
            int end = start + 4;
            int startTranslated = snapshot.getOriginalOffset(start);
            if (startTranslated > -1) {
                int endTranslated = startTranslated + end - start;
                highlights.put(new OffsetRange(startTranslated, endTranslated), ANNOTATION_TYPE_SET);
            }
        }

        @Override
        public void visit(StaticFieldAccess node) {
            if (isCancelled()) {
                return;
            }
            Expression expr = node.getField().getName();
            if (expr instanceof ArrayAccess) {
                ArrayAccess arrayAccess = (ArrayAccess) expr;
                expr = arrayAccess.getName();
            }
            new FieldAccessVisitor(ColoringAttributes.STATIC_FIELD_SET).scan(expr);
            super.visit(node);

        }

        @Override
        public void visit(ConstantDeclaration node) {
            if (isCancelled()) {
                return;
            }
            ASTNode parentNode = null;
            List<ASTNode> path = getPath();
            if (path != null && path.size() > 1) {
                parentNode = path.get(1);
            }
            if (parentNode instanceof ClassDeclaration || parentNode instanceof InterfaceDeclaration
                    || parentNode instanceof TraitDeclaration) {
                List<Identifier> names = node.getNames();
                if (!names.isEmpty()) {
                    for (Identifier identifier : names) {
                        addColoringForNode(identifier, createConstantDeclarationColoring(identifier));
                    }
                }
            }
            super.visit(node);
        }

        private Set<ColoringAttributes> createConstantDeclarationColoring(Identifier constantName) {
            if (isCancelled()) {
                return Collections.EMPTY_SET;
            }
            return isDeprecatedConstantDeclaration(constantName) ? DEPRECATED_STATIC_FIELD_SET : ColoringAttributes.STATIC_FIELD_SET;
        }

        private boolean isDeprecatedConstantDeclaration(Identifier constantName) {
            boolean isDeprecated = false;
            if (!isCancelled() && isResolveDeprecatedElements()) {
                VariableScope variableScope = model.getVariableScope(constantName.getStartOffset());
                QualifiedName typeFullyQualifiedName = VariousUtils.getFullyQualifiedName(
                        QualifiedName.create(typeDeclaration.getName()),
                        constantName.getStartOffset(),
                        variableScope);
                for (TypeConstantElement constantElement : getDeprecatedConstants()) {
                    if (constantElement.getName().equals(constantName.getName()) && constantElement.getType().getFullyQualifiedName().equals(typeFullyQualifiedName)) {
                        isDeprecated = true;
                        break;
                    }
                }
            }
            return isDeprecated;
        }

        @Override
        public void visit(StaticConstantAccess node) {
            if (isCancelled()) {
                return;
            }
            Identifier constant = node.getConstant();
            if (constant != null) {
                addColoringForNode(constant, ColoringAttributes.STATIC_FIELD_SET);
            }
            super.visit(node);
        }

        @Override
        public void visit(PHPDocTypeNode node) {
            if (isCancelled()) {
                return;
            }
            if (isDeprecatedTypeNode(node)) {
                addColoringForNode(node, DEPRECATED_SET);
            }
        }

        private boolean isDeprecatedTypeNode(PHPDocTypeNode node) {
            return isDeprecatedType(QualifiedName.create(node.getValue()), node.getStartOffset());
        }

        @Override
        public void visit(NamespaceName node) {
            if (isCancelled()) {
                return;
            }
            if (isDeprecatedNamespaceName(node)) {
                addColoringForNode(node, DEPRECATED_SET);
            }
        }

        private boolean isDeprecatedNamespaceName(NamespaceName node) {
            return isDeprecatedType(QualifiedName.create(node), node.getStartOffset());
        }

        private boolean isDeprecatedType(QualifiedName qualifiedName, int offset) {
            boolean isDeprecated = false;
            if (!isCancelled() && isResolveDeprecatedElements()) {
                VariableScope variableScope = model.getVariableScope(offset);
                QualifiedName fullyQualifiedName = VariousUtils.getFullyQualifiedName(qualifiedName, offset, variableScope);
                for (TypeElement typeElement : getDeprecatedTypes()) {
                    if (typeElement.getFullyQualifiedName().equals(fullyQualifiedName)) {
                        isDeprecated = true;
                        break;
                    }
                }
            }
            return isDeprecated;
        }

        @Override
        public void visit(UseStatement node) {
            if (isCancelled()) {
                return;
            }
            List<UseStatementPart> parts = node.getParts();
            for (int i = 0; i < parts.size(); i++) {
                UseStatementPart useStatementPart = parts.get(i);
                boolean isDeprecated = isDeprecatedNamespaceName(useStatementPart.getName());
                if (isDeprecated) {
                    addColoringForNode(useStatementPart.getName(), DEPRECATED_SET);
                }
            }
        }

        private class FieldAccessVisitor extends DefaultVisitor {
            private final Set<ColoringAttributes> coloring;

            public FieldAccessVisitor(Set<ColoringAttributes> coloring) {
                this.coloring = coloring;
            }

            @Override
            public void visit(ArrayAccess node) {
                scan(node.getName());
                // don't scan(scan(node.getDimension()); issue #194535
            }

            @Override
            public void visit(Identifier identifier) {
                //remove the field, because is used
                ASTNodeColoring removed = privateFieldsUnused.remove(new UnusedIdentifier(identifier.getName(), typeDeclaration));
                if (removed != null) {
                    // if it was removed, marked as normal field
                    addColoringForNode(removed.identifier, removed.coloring);
                }
                addColoringForNode(identifier, coloring);
            }
        }

        private class UnusedIdentifier {
            private final String name;
            private final TypeDeclaration typeDeclaration;

            public UnusedIdentifier(final String name, final TypeDeclaration classDeclaration) {
                this.name = name;
                this.typeDeclaration = classDeclaration;
            }

            @Override
            public int hashCode() {
                int hash = 5;
                hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
                hash = 29 * hash + (this.typeDeclaration != null ? this.typeDeclaration.hashCode() : 0);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final UnusedIdentifier other = (UnusedIdentifier) obj;
                if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                    return false;
                }
                if (this.typeDeclaration != other.typeDeclaration && (this.typeDeclaration == null || !this.typeDeclaration.equals(other.typeDeclaration))) {
                    return false;
                }
                return true;
            }

        }
    }
}
