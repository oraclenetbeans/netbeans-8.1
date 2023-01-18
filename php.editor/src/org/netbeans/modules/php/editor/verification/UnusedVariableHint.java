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
package org.netbeans.modules.php.editor.verification;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Assignment;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.CastExpression;
import org.netbeans.modules.php.editor.parser.astnodes.CatchClause;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ClassInstanceCreation;
import org.netbeans.modules.php.editor.parser.astnodes.CloneExpression;
import org.netbeans.modules.php.editor.parser.astnodes.ConditionalExpression;
import org.netbeans.modules.php.editor.parser.astnodes.ConstantDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ContinueStatement;
import org.netbeans.modules.php.editor.parser.astnodes.DeclareStatement;
import org.netbeans.modules.php.editor.parser.astnodes.DoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.EchoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.ExpressionStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FieldsDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ForEachStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ForStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FormalParameter;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.GotoLabel;
import org.netbeans.modules.php.editor.parser.astnodes.GotoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Include;
import org.netbeans.modules.php.editor.parser.astnodes.InstanceOfExpression;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.LambdaFunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocBlock;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocMethodTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocStaticAccessType;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocTypeTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocVarTypeTag;
import org.netbeans.modules.php.editor.parser.astnodes.PHPVarComment;
import org.netbeans.modules.php.editor.parser.astnodes.PostfixExpression;
import org.netbeans.modules.php.editor.parser.astnodes.PrefixExpression;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.ReflectionVariable;
import org.netbeans.modules.php.editor.parser.astnodes.ReturnStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.SingleFieldDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.StaticFieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.SwitchStatement;
import org.netbeans.modules.php.editor.parser.astnodes.ThrowStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UnaryOperation;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatement;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatementPart;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.WhileStatement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class UnusedVariableHint extends HintRule implements CustomisableRule {

    private static final String HINT_ID = "Unused.Variable.Hint"; //NOI18N
    private static final String CHECK_UNUSED_FORMAL_PARAMETERS = "php.verification.check.unused.formal.parameters"; //NOI18N
    private static final List<String> UNCHECKED_VARIABLES = new ArrayList<>();
    private Preferences preferences;

    static {
        UNCHECKED_VARIABLES.add("this"); //NOI18N
        UNCHECKED_VARIABLES.add("GLOBALS"); //NOI18N
        UNCHECKED_VARIABLES.add("_SERVER"); //NOI18N
        UNCHECKED_VARIABLES.add("_GET"); //NOI18N
        UNCHECKED_VARIABLES.add("_POST"); //NOI18N
        UNCHECKED_VARIABLES.add("_FILES"); //NOI18N
        UNCHECKED_VARIABLES.add("_COOKIE"); //NOI18N
        UNCHECKED_VARIABLES.add("_SESSION"); //NOI18N
        UNCHECKED_VARIABLES.add("_REQUEST"); //NOI18N
        UNCHECKED_VARIABLES.add("_ENV"); //NOI18N
    }

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() == null) {
            return;
        }
        FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
        if (fileObject != null) {
            CheckVisitor checkVisitor = new CheckVisitor(fileObject, context.doc);
            phpParseResult.getProgram().accept(checkVisitor);
            hints.addAll(checkVisitor.getHints());
        }
    }

    private class CheckVisitor extends DefaultVisitor {

        private final Stack<ASTNode> parentNodes = new Stack<>();
        private final Map<ASTNode, List<HintVariable>> unusedVariables = new HashMap<>();
        private final Map<ASTNode, List<HintVariable>> usedVariables = new HashMap<>();
        private final FileObject fileObject;
        private final BaseDocument baseDocument;
        private final List<Hint> hints;
        private boolean forceVariableAsUsed;
        private boolean forceVariableAsUnused;

        public CheckVisitor(FileObject fileObject, BaseDocument baseDocument) {
            this.fileObject = fileObject;
            this.baseDocument = baseDocument;
            hints = new ArrayList<>();
        }

        public List<Hint> getHints() {
            for (ASTNode scopeNode : unusedVariables.keySet()) {
                List<HintVariable> scopeVariables = unusedVariables.get(scopeNode);
                for (HintVariable variable : scopeVariables) {
                    createHint(variable);
                }
            }
            return hints;
        }

        @Messages({
            "# {0} - Name of the variable",
            "UnusedVariableHintCustom=Variable ${0} seems to be unused in its scope"
        })
        private void createHint(HintVariable variable) {
            int start = variable.getStartOffset();
            int end = variable.getEndOffset();
            OffsetRange offsetRange = new OffsetRange(start, end);
            if (showHint(offsetRange, baseDocument)) {
                hints.add(new Hint(UnusedVariableHint.this, Bundle.UnusedVariableHintCustom(variable.getName()), fileObject, offsetRange, null, 500));
            }
        }

        @CheckForNull
        private Identifier getIdentifier(Variable variable) {
            Identifier retval = null;
            if (variable != null && variable.isDollared()) {
                if (variable.getName() instanceof Identifier) {
                    retval = (Identifier) variable.getName();
                }
            }
            return retval;
        }

        private List<HintVariable> getUsedScopeVariables(ASTNode parentNode) {
            List<HintVariable> usedScopeVariables = usedVariables.get(parentNode);
            if (usedScopeVariables == null) {
                usedScopeVariables = new ArrayList<>();
                usedVariables.put(parentNode, usedScopeVariables);
            }
            return usedScopeVariables;
        }

        private List<HintVariable> getUnusedScopeVariables(ASTNode parentNode) {
            List<HintVariable> unusedScopeVariables = unusedVariables.get(parentNode);
            if (unusedScopeVariables == null) {
                unusedScopeVariables = new ArrayList<>();
                unusedVariables.put(parentNode, unusedScopeVariables);
            }
            return unusedScopeVariables;
        }

        @CheckForNull
        private HintVariable getUnusedVariable(String currentVarName, List<HintVariable> unusedScopeVariables) {
            HintVariable retval = null;
            for (HintVariable variable : unusedScopeVariables) {
                String varName = variable.getName();
                if (currentVarName.equals(varName)) {
                    retval = variable;
                    break;
                }
            }
            return retval;
        }

        private boolean isVariableUsed(String currentVarName, List<HintVariable> usedScopeVariables) {
            boolean retval = false;
            for (HintVariable variable : usedScopeVariables) {
                String varName = variable.getName();
                if (currentVarName.equals(varName)) {
                    retval = true;
                    break;
                }
            }
            return retval;
        }

        private void forceVariableAsUnused(HintVariable node, List<HintVariable> unusedScopeVariables) {
            HintVariable unusedVariable = getUnusedVariable(node.getName(), unusedScopeVariables);
            if (unusedVariable != null) {
                unusedScopeVariables.remove(unusedVariable);
            }
            unusedScopeVariables.add(node);
        }

        private void forceVariableAsUsed(HintVariable hintVariable, List<HintVariable> usedScopeVariables, List<HintVariable> unusedScopeVariables) {
            String currentVarName = hintVariable.getName();
            if (isVariableUsed(currentVarName, usedScopeVariables)) {
                return;
            }
            usedScopeVariables.add(hintVariable);
            HintVariable unusedVariable = getUnusedVariable(currentVarName, unusedScopeVariables);
            if (unusedVariable != null) {
                unusedScopeVariables.remove(unusedVariable);
            }
        }

        @Override
        public void visit(Variable node) {
            Identifier identifier = getIdentifier(node);
            if (identifier != null && !isInGlobalContext()) {
                process(HintVariable.create(node, identifier.getName()));
            }
        }

        private boolean isInGlobalContext() {
            return (parentNodes.peek() instanceof Program) || (parentNodes.peek() instanceof NamespaceDeclaration);
        }

        private void process(HintVariable hintVariable) {
            if (hintVariable != null && !UNCHECKED_VARIABLES.contains(hintVariable.getName())) {
                ASTNode parentNode = parentNodes.peek();
                String currentVarName = hintVariable.getName();
                List<HintVariable> usedScopeVariables = getUsedScopeVariables(parentNode);
                List<HintVariable> unusedScopeVariables = getUnusedScopeVariables(parentNode);
                if (forceVariableAsUnused) {
                    forceVariableAsUnused(hintVariable, unusedScopeVariables);
                    return;
                }
                if (forceVariableAsUsed) {
                    forceVariableAsUsed(hintVariable, usedScopeVariables, unusedScopeVariables);
                    return;
                }
                if (isVariableUsed(currentVarName, usedScopeVariables)) {
                    return;
                }
                HintVariable unusedVariable = getUnusedVariable(currentVarName, unusedScopeVariables);
                if (unusedVariable != null) {
                    unusedScopeVariables.remove(unusedVariable);
                    usedScopeVariables.add(hintVariable);
                    return;
                }
                unusedScopeVariables.add(hintVariable);
            }
        }

        @Override
        public void visit(Program node) {
            parentNodes.push(node);
            super.visit(node);
            parentNodes.pop();
        }

        @Override
        public void visit(NamespaceDeclaration node) {
            parentNodes.push(node);
            super.visit(node);
            parentNodes.pop();
        }

        @Override
        public void visit(FunctionDeclaration node) {
            if (node.getBody() != null) {
                parentNodes.push(node);
                super.visit(node);
                parentNodes.pop();
            }
        }

        @Override
        public void visit(EchoStatement node) {
            forceVariableAsUsed = true;
            scan(node.getExpressions());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(ExpressionStatement node) {
            if (node.getExpression() instanceof Variable) { // just variable without anything: {  $var; }
                forceVariableAsUnused = true;
                scan(node.getExpression());
                forceVariableAsUnused = false;
            } else {
                scan(node.getExpression());
            }
        }

        @Override
        public void visit(Include node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(FunctionInvocation node) {
            forceVariableAsUsed = true;
            String functionName = CodeUtils.extractFunctionName(node);
            if ("compact".equals(functionName)) { //NOI18N
                handleCompactFunction(node);
            }
            super.visit(node);
            forceVariableAsUsed = false;
        }

        private void handleCompactFunction(final FunctionInvocation node) {
            List<Expression> parameters = node.getParameters();
            for (Expression parameter : parameters) {
                handleFunctionParameter(parameter);
            }
        }

        private void handleFunctionParameter(final Expression parameter) {
            if (parameter instanceof Scalar) {
                handleScalar((Scalar) parameter);
            }
        }

        private void handleScalar(final Scalar scalar) {
            if (scalar.getScalarType().equals(Scalar.Type.STRING)) {
                process(HintVariable.create(scalar, extractVariableName(scalar.getStringValue())));
            }
        }

        private String extractVariableName(final String quotedValue) {
            String result = quotedValue;
            if ((quotedValue.startsWith("'") && quotedValue.endsWith("'")) || (quotedValue.startsWith("\"") && quotedValue.endsWith("\""))) { //NOI18N
                result = quotedValue.substring(1, quotedValue.length() - 1);
            }
            return result;
        }

        @Override
        public void visit(MethodInvocation node) {
            forceVariableAsUsed = true;
            scan(node.getDispatcher());
            forceVariableAsUsed = false;
            scan(node.getMethod());
        }

        @Override
        public void visit(IfStatement node) {
            forceVariableAsUsed = true;
            scan(node.getCondition());
            forceVariableAsUsed = false;
            scan(node.getTrueStatement());
            scan(node.getFalseStatement());
        }

        @Override
        public void visit(InstanceOfExpression node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            scan(node.getClassName());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(PostfixExpression node) {
            forceVariableAsUsed = true;
            scan(node.getVariable());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(PrefixExpression node) {
            forceVariableAsUsed = true;
            scan(node.getVariable());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(ReflectionVariable node) {
            forceVariableAsUsed = true;
            Expression name = node.getName();
            if (name instanceof Scalar) {
                handleScalar((Scalar) name);
            } else {
                scan(name);
            }
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(CloneExpression node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(CastExpression node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(Assignment node) {
            scan(node.getLeftHandSide());
            forceVariableAsUsed = true;
            scan(node.getRightHandSide());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(ConditionalExpression node) {
            forceVariableAsUsed = true;
            scan(node.getCondition());
            scan(node.getIfTrue());
            scan(node.getIfFalse());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(ReturnStatement node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(SwitchStatement node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
            scan(node.getBody());
        }

        @Override
        public void visit(ThrowStatement node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(UnaryOperation node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(ClassDeclaration node) {
            scan(node.getBody());
        }

        @Override
        public void visit(ClassInstanceCreation node) {
            forceVariableAsUsed = true;
            scan(node.getClassName());
            scan(node.ctorParams());
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(DoStatement node) {
            forceVariableAsUsed = true;
            scan(node.getCondition());
            forceVariableAsUsed = false;
            scan(node.getBody());
        }

        @Override
        public void visit(DeclareStatement node) {
            scan(node.getBody());
        }

        @Override
        public void visit(CatchClause node) {
            Block body = node.getBody();
            if (!body.getStatements().isEmpty()) {
                scan(node.getVariable());
            }
            scan(body);
        }

        @Override
        public void visit(FormalParameter node) {
            if (checkUnusedFormalParameters(preferences)) {
                scan(node.getParameterName());
            } else {
                forceVariableAsUsed = true;
                scan(node.getParameterName());
                forceVariableAsUsed = false;
            }
        }

        @Override
        public void visit(ForEachStatement node) {
            forceVariableAsUsed = true;
            scan(node.getExpression());
            forceVariableAsUsed = false;
            scan(node.getKey());
            scan(node.getValue());
            scan(node.getStatement());
        }

        @Override
        public void visit(ForStatement node) {
            forceVariableAsUsed = true;
            scan(node.getInitializers());
            scan(node.getConditions());
            scan(node.getUpdaters());
            forceVariableAsUsed = false;
            scan(node.getBody());
        }

        @Override
        public void visit(StaticMethodInvocation node) {
            forceVariableAsUsed = true;
            scan(node.getClassName());
            forceVariableAsUsed = false;
            scan(node.getMethod());
        }

        @Override
        public void visit(WhileStatement node) {
            forceVariableAsUsed = true;
            scan(node.getCondition());
            forceVariableAsUsed = false;
            scan(node.getBody());
        }

        @Override
        public void visit(LambdaFunctionDeclaration node) {
            forceVariableAsUsed = true;
            scan(node.getLexicalVariables());
            forceVariableAsUsed = false;
            parentNodes.push(node);
            scan(node.getLexicalVariables());
            scan(node.getFormalParameters());
            scan(node.getBody());
            parentNodes.pop();
        }

        @Override
        public void visit(StaticFieldAccess node) {
            forceVariableAsUsed = true;
            super.visit(node);
            forceVariableAsUsed = false;
        }

        @Override
        public void visit(InterfaceDeclaration node) {
            // intentionally
        }

        @Override
        public void visit(FieldsDeclaration node) {
            // intentionally
        }

        @Override
        public void visit(PHPDocBlock node) {
            // intentionally
        }

        @Override
        public void visit(PHPDocTypeTag node) {
            // intentionally
        }

        @Override
        public void visit(PHPDocMethodTag node) {
            // intentionally
        }

        @Override
        public void visit(PHPDocVarTypeTag node) {
            // intentionally
        }

        @Override
        public void visit(PHPDocStaticAccessType node) {
            // intentionally
        }

        @Override
        public void visit(PHPVarComment node) {
            // intentionally
        }

        @Override
        public void visit(ConstantDeclaration node) {
            // intentionally
        }

        @Override
        public void visit(ContinueStatement node) {
            // intentionally
        }

        @Override
        public void visit(SingleFieldDeclaration node) {
            // intentionally
        }

        @Override
        public void visit(UseStatement node) {
            // intentionally
        }

        @Override
        public void visit(UseStatementPart node) {
            // intentionally
        }

        @Override
        public void visit(GotoLabel node) {
            // intentionally
        }

        @Override
        public void visit(GotoStatement node) {
            // intentionally
        }

    }

    private static final class HintVariable {
        private final ASTNode node;
        private final String name;

        static HintVariable create(final ASTNode node, final String name) {
            return new HintVariable(node, name);
        }

        private HintVariable(final ASTNode node, final String name) {
            this.node = node;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getStartOffset() {
            return node.getStartOffset() + 1;
        }

        public int getEndOffset() {
            return node.getEndOffset();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final HintVariable other = (HintVariable) obj;
            return Objects.equals(this.name, other.name);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.name);
            return hash;
        }

    }

    @Override
    public String getId() {
        return HINT_ID;
    }

    @Override
    @Messages("UnusedVariableHintDesc=Detects variables which are declared, but not used in their scope.")
    public String getDescription() {
        return Bundle.UnusedVariableHintDesc();
    }

    @Override
    @Messages("UnusedVariableHintDispName=Unused Variables")
    public String getDisplayName() {
        return Bundle.UnusedVariableHintDispName();
    }

    @Override
    public HintSeverity getDefaultSeverity() {
        return HintSeverity.WARNING;
    }

    @Override
    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public JComponent getCustomizer(Preferences preferences) {
        JComponent customizer = new UnusedVariableCustomizer(preferences, this);
        setCheckUnusedFormalParameters(preferences, checkUnusedFormalParameters(preferences));
        return customizer;
    }

    public void setCheckUnusedFormalParameters(Preferences preferences, boolean isEnabled) {
        preferences.putBoolean(CHECK_UNUSED_FORMAL_PARAMETERS, isEnabled);
    }

    public boolean checkUnusedFormalParameters(Preferences preferences) {
        return preferences.getBoolean(CHECK_UNUSED_FORMAL_PARAMETERS, true);
    }

}
