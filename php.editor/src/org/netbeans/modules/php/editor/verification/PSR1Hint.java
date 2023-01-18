/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ConstantDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.EmptyStatement;
import org.netbeans.modules.php.editor.parser.astnodes.FieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.SingleFieldDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Statement;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TypeDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.UseStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public abstract class PSR1Hint extends HintRule {

    @Override
    public void invoke(PHPRuleContext context, List<Hint> result) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() != null) {
            FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
            if (fileObject != null) {
                CheckVisitor checkVisitor = createVisitor(fileObject, context.doc);
                phpParseResult.getProgram().accept(checkVisitor);
                result.addAll(checkVisitor.getHints());
            }
        }
    }

    abstract CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument);

    public static class ConstantDeclarationHint extends PSR1Hint {
        private static final String HINT_ID = "PSR1.Hint.Constant"; //NOI18N
        private static final Pattern CONSTANT_PATTERN = Pattern.compile("[A-Z0-9]+[A-Z0-9_]*[A-Z0-9]+"); //NOI18N

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new ConstantsVisitor(this, fileObject, baseDocument);
        }

        private static final class ConstantsVisitor extends CheckVisitor {

            public ConstantsVisitor(PSR1Hint psr1hint, FileObject fileObject, BaseDocument baseDocument) {
                super(psr1hint, fileObject, baseDocument);
            }

            @Override
            @NbBundle.Messages("PSR1ConstantDeclarationHintText=Class constants MUST be declared in all upper case with underscore separators.")
            public void visit(ConstantDeclaration node) {
                for (Identifier constantNameNode : node.getNames()) {
                    String constantName = constantNameNode.getName();
                    if (constantName != null && !CONSTANT_PATTERN.matcher(constantName).matches()) {
                        createHint(constantNameNode, Bundle.PSR1ConstantDeclarationHintText());
                    }
                }
            }

        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("PSR1ConstantHintDesc=Class constants MUST be declared in all upper case with underscore separators.")
        public String getDescription() {
            return Bundle.PSR1ConstantHintDesc();
        }

        @Override
        @NbBundle.Messages("PSR1ConstantHintDisp=Class Constant Declaration")
        public String getDisplayName() {
            return Bundle.PSR1ConstantHintDisp();
        }

    }

    public static class MethodDeclarationHint extends PSR1Hint {
        private static final String HINT_ID = "PSR1.Hint.Method"; //NOI18N
        private static final String MAGIC_METHODS = "__(construct|destruct|call|callStatic|get|set|isset|unset|sleep|wakeup|toString|invoke|set_state|clone)"; //NOI18N
        private static final Pattern METHOD_PATTERN = Pattern.compile("([a-z]|" + MAGIC_METHODS + ")[a-zA-Z0-9]*"); //NOI18N

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new MethodDeclarationVisitor(this, fileObject, baseDocument);
        }

        private static final class MethodDeclarationVisitor extends CheckVisitor {

            public MethodDeclarationVisitor(PSR1Hint psr1hint, FileObject fileObject, BaseDocument baseDocument) {
                super(psr1hint, fileObject, baseDocument);
            }

            @Override
            @NbBundle.Messages("PSR1MethodDeclarationHintText=Method names MUST be declared in camelCase().")
            public void visit(MethodDeclaration node) {
                Identifier functionNameNode = node.getFunction().getFunctionName();
                String methodName = functionNameNode.getName();
                if (methodName != null && !METHOD_PATTERN.matcher(methodName).matches()) {
                    createHint(functionNameNode, Bundle.PSR1MethodDeclarationHintText());
                }
            }

        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("PSR1MethodDeclarationHintDesc=Method names MUST be declared in camelCase().")
        public String getDescription() {
            return Bundle.PSR1MethodDeclarationHintDesc();
        }

        @Override
        @NbBundle.Messages("PSR1MethodDeclarationHintDisp=Method Declaration")
        public String getDisplayName() {
            return Bundle.PSR1MethodDeclarationHintDisp();
        }

    }

    public static class TypeDeclarationHint extends PSR1Hint {
        private static final String HINT_ID = "PSR1.Hint.Type"; //NOI18N
        private FileObject fileObject;

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            assert fileObject != null;
            this.fileObject = fileObject;
            return new TypeDeclarationVisitor(this, fileObject, baseDocument);
        }

        protected boolean isPhp52() {
            return CodeUtils.isPhp52(fileObject);
        }

        private static final class TypeDeclarationVisitor extends CheckVisitor {
            private static final Pattern PHP52_TYPE_NAME_PATTERN = Pattern.compile("([A-Z][a-zA-Z0-9]*_)+[A-Z][a-zA-Z0-9]+"); //NOI18N
            private static final Pattern PHP53_TYPE_NAME_PATTERN = Pattern.compile("[A-Z][a-zA-Z0-9]+"); //NOI18N
            private final boolean isPhp52;
            private boolean isInNamedNamespaceDeclaration = false;
            private boolean isDeclaredType = false;

            public TypeDeclarationVisitor(TypeDeclarationHint typeDeclarationHint, FileObject fileObject, BaseDocument baseDocument) {
                super(typeDeclarationHint, fileObject, baseDocument);
                isPhp52 = typeDeclarationHint.isPhp52();
            }

            @Override
            public void visit(NamespaceDeclaration node) {
                isInNamedNamespaceDeclaration = node.getName() != null;
                super.visit(node);
            }

            @Override
            public void visit(ClassDeclaration node) {
                processTypeDeclaration(node);
            }

            @Override
            public void visit(InterfaceDeclaration node) {
                processTypeDeclaration(node);
            }

            @Override
            public void visit(TraitDeclaration node) {
                processTypeDeclaration(node);
            }

            @NbBundle.Messages("PSR1TypeDeclarationMoreTypesHintText=Each type MUST be in a file by itself.")
            private void processTypeDeclaration(TypeDeclaration node) {
                Identifier typeNameNode = node.getName();
                if (isDeclaredType) {
                    createHint(typeNameNode, Bundle.PSR1TypeDeclarationMoreTypesHintText());
                } else {
                    isDeclaredType = true;
                    processFirstDeclaration(typeNameNode);
                }
            }

            private void processFirstDeclaration(Identifier typeNameNode) {
                if (isPhp52) {
                    checkPhp52Violations(typeNameNode);
                } else {
                    checkPhp53Violations(typeNameNode);
                }
            }

            @NbBundle.Messages("PSR1TypeDeclaration52HintText=Type names SHOULD use the pseudo-namespacing convention of Vendor_ prefixes on type names.")
            private void checkPhp52Violations(Identifier typeNameNode) {
                String typeName = typeNameNode.getName();
                if (typeName != null && !PHP52_TYPE_NAME_PATTERN.matcher(typeName).matches()) {
                    createHint(typeNameNode, Bundle.PSR1TypeDeclaration52HintText());
                }
            }

            @NbBundle.Messages({
                "PSR1TypeDeclaration53HintText=Type names MUST be declared in StudlyCaps.",
                "PSR1TypeDeclaration53NoNsHintText=Each type MUST be in a namespace of at least one level: a top-level vendor name."
            })
            private void checkPhp53Violations(Identifier typeNameNode) {
                String typeName = typeNameNode.getName();
                if (!isInNamedNamespaceDeclaration) {
                    createHint(typeNameNode, Bundle.PSR1TypeDeclaration53NoNsHintText());
                } else {
                    if (typeName != null && !PHP53_TYPE_NAME_PATTERN.matcher(typeName).matches()) {
                        createHint(typeNameNode, Bundle.PSR1TypeDeclaration53HintText());
                    }
                }
            }

        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("PSR1TypeDeclarationHintDesc=Type names MUST be declared in StudlyCaps (Code written for 5.2.x and before SHOULD use the pseudo-namespacing convention of Vendor_ prefixes on type names). Each type is in a file by itself, and is in a namespace of at least one level: a top-level vendor name.")
        public String getDescription() {
            return Bundle.PSR1TypeDeclarationHintDesc();
        }

        @Override
        @NbBundle.Messages("PSR1TypeDeclarationHintDisp=Type Declaration")
        public String getDisplayName() {
            return Bundle.PSR1TypeDeclarationHintDisp();
        }
    }

    public static final class PropertyNameHint extends PSR1Hint {
        private static final String HINT_ID = "PSR1.Hint.Property"; //NOI18N

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new PropertyNameVisitor(this, fileObject, baseDocument);
        }

        private static final class PropertyNameVisitor extends CheckVisitor {
            private static final Pattern STUDLY_CAPS_PATTERN = Pattern.compile("[A-Z][a-zA-Z0-9]*"); //NOI18N
            private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("[a-z]+([A-Z][a-z0-9]*)*"); //NOI18N
            private static final Pattern UNDER_SCORE_PATTERN = Pattern.compile("[a-z]+(_[a-z0-9]*)*"); //NOI18N
            private final List<Pattern> possiblePatterns = new ArrayList<>();

            public PropertyNameVisitor(PSR1Hint psr1hint, FileObject fileObject, BaseDocument baseDocument) {
                super(psr1hint, fileObject, baseDocument);
            }

            @Override
            public void visit(FieldAccess node) {
                checkProperty(node.getField());
                super.visit(node);
            }

            @Override
            public void visit(SingleFieldDeclaration node) {
                checkProperty(node.getName());
                super.visit(node);
            }

            @NbBundle.Messages(
                "PSR1PropertyNameHintText=Property names SHOULD be declared in $StudlyCaps, $camelCase, or $under_score format (consistently in a scope).\n"
                    + "Previous property usage was in a different format, or this property name is absolutely wrong."
            )
            private void checkProperty(Variable node) {
                String propertyName = CodeUtils.extractVariableName(node);
                if (propertyName != null) {
                    String normalizedPropertyName = propertyName.startsWith("$") ? propertyName.substring(1) : propertyName;
                    if (possiblePatterns.isEmpty()) {
                        fetchPossiblePatterns(normalizedPropertyName);
                    }
                    if (!isValidPropertyName(normalizedPropertyName)) {
                        createHint(node, Bundle.PSR1PropertyNameHintText());
                    }
                }
            }

            private boolean isValidPropertyName(String propertyName) {
                boolean result = true;
                if (possiblePatterns.isEmpty()) {
                    result = false;
                } else {
                    if (possiblePatterns.size() == 1) { // all property names must match
                        Pattern pattern = possiblePatterns.get(0);
                        Matcher matcher = pattern.matcher(propertyName);
                        if (!matcher.matches()) {
                            result = false;
                        }
                    } else {
                        // more patterns, probably all previous properties were of unspecific name, i.e. "foobarbaz" (it matches camel and under)
                        List<Pattern> matchingPatterns = new ArrayList<>();
                        for (Pattern pattern : possiblePatterns) {
                            Matcher matcher = pattern.matcher(propertyName);
                            if (matcher.matches()) {
                                matchingPatterns.add(pattern);
                            }
                        }
                        if (matchingPatterns.isEmpty()) {
                            result = false;
                        } else {
                            possiblePatterns.clear();
                            possiblePatterns.addAll(matchingPatterns);
                        }
                    }
                }
                return result;
            }

            private void fetchPossiblePatterns(String propertyName) {
                assert possiblePatterns.isEmpty();
                Matcher studlyCapsMatcher = STUDLY_CAPS_PATTERN.matcher(propertyName);
                if (studlyCapsMatcher.matches()) {
                    possiblePatterns.add(STUDLY_CAPS_PATTERN);
                } else {
                    Matcher camelCaseMatcher = CAMEL_CASE_PATTERN.matcher(propertyName);
                    if (camelCaseMatcher.matches()) {
                        possiblePatterns.add(CAMEL_CASE_PATTERN);
                    }
                    Matcher underScoreMatcher = UNDER_SCORE_PATTERN.matcher(propertyName);
                    if (underScoreMatcher.matches()) {
                        possiblePatterns.add(UNDER_SCORE_PATTERN);
                    }
                }
            }

        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("PSR1PropertyNameHintDesc=Property names SHOULD be declared in $StudlyCaps, $camelCase, or $under_score format (consistently in a scope).")
        public String getDescription() {
            return Bundle.PSR1PropertyNameHintDesc();
        }

        @Override
        @NbBundle.Messages("PSR1PropertyNameHintDisp=Property Name")
        public String getDisplayName() {
            return Bundle.PSR1PropertyNameHintDisp();
        }

    }

    public static final class SideEffectHint extends PSR1Hint {
        private static final String HINT_ID = "PSR1.Hint.Side.Effect"; //NOI18N

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new SideEffectVisitor(this, fileObject, baseDocument);
        }

        private static final class SideEffectVisitor extends CheckVisitor {
            private boolean containsDeclaration = false;
            private ASTNode firstSideEffectNode;

            public SideEffectVisitor(PSR1Hint psr1hint, FileObject fileObject, BaseDocument baseDocument) {
                super(psr1hint, fileObject, baseDocument);
            }

            @Override
            public void visit(Program node) {
                checkStatements(node.getStatements());
                checkSideEffects();
            }

            private void checkStatements(List<Statement> statements) {
                for (Statement statement : statements) {
                    checkStatement(statement);
                }
            }

            private void checkStatement(ASTNode node) {
                if (isNamespaceDeclaration(node)) {
                    NamespaceDeclaration namespaceDeclaration = (NamespaceDeclaration) node;
                    checkStatements(namespaceDeclaration.getBody().getStatements());
                } else if (isDeclaration(node)) {
                    containsDeclaration = true;
                } else if (isCondition(node)) {
                    IfStatement ifStatement = (IfStatement) node;
                    checkCondition(ifStatement.getTrueStatement());
                    checkCondition(ifStatement.getFalseStatement());
                } else {
                    if (!isAllowedEverywhere(node)) {
                        initSideEffect(node);
                    }
                }
            }

            private void checkCondition(Statement node) {
                if (node instanceof Block) {
                    Block body = (Block) node;
                    checkStatements(body.getStatements());
                } else {
                    checkStatement(node);
                }
            }

            private void initSideEffect(ASTNode node) {
                if (firstSideEffectNode == null) {
                    firstSideEffectNode = node;
                }
            }

            @NbBundle.Messages(
                "PSR1SideEffectHintText=A file SHOULD declare new symbols and cause no other side effects, or it SHOULD execute logic with side effects, "
                    + "but SHOULD NOT do both."
            )
            private void checkSideEffects() {
                if (isSideEffect()) {
                    createHint(firstSideEffectNode, Bundle.PSR1SideEffectHintText());
                }
            }

            private boolean isSideEffect() {
                return firstSideEffectNode != null && containsDeclaration;
            }

            private static boolean isNamespaceDeclaration(ASTNode node) {
                return node instanceof NamespaceDeclaration;
            }

            private static boolean isCondition(ASTNode node) {
                return node instanceof IfStatement;
            }

            private static boolean isDeclaration(ASTNode node) {
                return node instanceof TypeDeclaration || node instanceof FunctionDeclaration || node instanceof ConstantDeclaration;
            }

            private static boolean isAllowedEverywhere(ASTNode node) {
                return node instanceof UseStatement || node instanceof NamespaceDeclaration || node instanceof EmptyStatement;
            }

        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages(
            "PSR1SideEffectHintDesc=A file SHOULD declare new symbols and cause no other side effects, or it SHOULD execute logic with side effects, "
                + "but SHOULD NOT do both."
        )
        public String getDescription() {
            return Bundle.PSR1SideEffectHintDesc();
        }

        @Override
        @NbBundle.Messages("PSR1SideEffectHintDisp=Side Effects")
        public String getDisplayName() {
            return Bundle.PSR1SideEffectHintDisp();
        }

    }

    private abstract static class CheckVisitor extends DefaultVisitor {
        private final PSR1Hint psr1hint;
        private final FileObject fileObject;
        private final BaseDocument baseDocument;
        private final List<Hint> hints;

        public CheckVisitor(PSR1Hint psr1hint, FileObject fileObject, BaseDocument baseDocument) {
            this.psr1hint = psr1hint;
            this.fileObject = fileObject;
            this.baseDocument = baseDocument;
            this.hints = new ArrayList<>();
        }

        public List<Hint> getHints() {
            return hints;
        }

        @NbBundle.Messages({
            "# {0} - Text which describes the violation",
            "PSR1ViolationHintText=PSR-1 Violation:\n{0}"
        })
        protected void createHint(ASTNode node, String message) {
            OffsetRange offsetRange = new OffsetRange(node.getStartOffset(), node.getEndOffset());
            if (psr1hint.showHint(offsetRange, baseDocument)) {
                hints.add(new Hint(
                        psr1hint,
                        Bundle.PSR1ViolationHintText(message),
                        fileObject,
                        offsetRange,
                        null,
                        500));
            }
        }

    }

    @Override
    public boolean getDefaultEnabled() {
        return false;
    }

}
