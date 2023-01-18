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
import java.util.Collections;
import java.util.List;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.FormalParameter;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Reference;
import org.netbeans.modules.php.editor.parser.astnodes.SingleFieldDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.Variadic;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class InitializeFieldSuggestion extends SuggestionRule {
    private static final String SUGGESTION_ID = "Initialize.Field.Suggestion"; //NOI18N

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() != null) {
            FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
            if (fileObject != null) {
                int caretOffset = getCaretOffset();
                final BaseDocument doc = context.doc;
                OffsetRange lineBounds = VerificationUtils.createLineBounds(caretOffset, doc);
                if (lineBounds.containsInclusive(caretOffset)) {
                    ConstructorVisitor constructorVisitor = new ConstructorVisitor(fileObject, doc);
                    phpParseResult.getProgram().accept(constructorVisitor);
                    hints.addAll(constructorVisitor.getHints());
                }
            }
        }
    }

    private final class ConstructorVisitor extends DefaultVisitor {
        private final FileObject fileObject;
        private final BaseDocument baseDocument;
        private final ArrayList<Hint> hints;
        private List<FormalParameter> formalParameters;
        private List<String> declaredFields;
        private List<String> usedVariables;
        private boolean isInConstructor;
        private int typeBodyStartOffset;
        private int constructorBodyEndOffset;

        private ConstructorVisitor(FileObject fileObject, BaseDocument baseDocument) {
            this.fileObject = fileObject;
            this.baseDocument = baseDocument;
            this.hints = new ArrayList<>();
        }

        public List<Hint> getHints() {
            return hints;
        }

        @Override
        public void visit(ClassDeclaration node) {
            typeBodyStartOffset = node.getBody().getStartOffset() + 1;
            declaredFields = new ArrayList<>();
            usedVariables = new ArrayList<>();
            super.visit(node);
            typeBodyStartOffset = 0;
        }

        @Override
        public void visit(TraitDeclaration node) {
            typeBodyStartOffset = node.getBody().getStartOffset() + 1;
            declaredFields = new ArrayList<>();
            usedVariables = new ArrayList<>();
            super.visit(node);
            typeBodyStartOffset = 0;
        }

        @Override
        public void visit(InterfaceDeclaration node) {
            // do not process ifaces
        }

        @Override
        public void visit(SingleFieldDeclaration node) {
            String fieldName = CodeUtils.extractVariableName(node.getName());
            if (fieldName != null) {
                declaredFields.add(fieldName);
            }
        }

        @Override
        public void visit(MethodDeclaration node) {
            FunctionDeclaration function = node.getFunction();
            if (CodeUtils.isConstructor(node) && function.getBody() != null) {
                formalParameters = new ArrayList<>(function.getFormalParameters());
                isInConstructor = true;
                constructorBodyEndOffset = function.getBody().getEndOffset() - 1;
                scan(function.getBody());
                isInConstructor = false;
                createHints();
            }
        }

        private void createHints() {
            for (ParameterToInit parameterToInit : createParametersToInit()) {
                hints.add(parameterToInit.createHint(fileObject, baseDocument));
            }
        }

        private List<ParameterToInit> createParametersToInit() {
            List<ParameterToInit> result = new ArrayList<>();
            for (FormalParameter formalParameter : formalParameters) {
                String parameterName = extractParameterName(formalParameter.getParameterName());
                if (parameterName != null && !parameterName.isEmpty()) {
                    List<Initializer> initializers = new ArrayList<>();
                    if (!usedVariables.contains(parameterName)) {
                        initializers.add(new FieldAssignmentInitializer(constructorBodyEndOffset, parameterName));
                    }
                    if (!declaredFields.contains(parameterName)) {
                        initializers.add(new FieldDeclarationInitializer(typeBodyStartOffset, formalParameter));
                    }
                    if (!initializers.isEmpty()) {
                        result.add(new ParameterToInit(formalParameter, initializers));
                    }
                }
            }
            return result;
        }

        @Override
        public void visit(Variable node) {
            if (isInConstructor) {
                String variableName = CodeUtils.extractVariableName(node);
                if (variableName != null) {
                    usedVariables.add(variableName);
                }
            }
        }

    }

    private final class ParameterToInit {
        private final FormalParameter formalParameter;
        private final List<Initializer> initializers;

        public ParameterToInit(FormalParameter formalParameter, List<Initializer> initializers) {
            this.formalParameter = formalParameter;
            this.initializers = initializers;
        }

        public String getName() {
            return extractParameterName(formalParameter.getParameterName());
        }

        @NbBundle.Messages({
            "# {0} - Field name",
            "InitializeFieldSuggestionText=Initialize Field: {0}"
        })
        public Hint createHint(FileObject fileObject, BaseDocument baseDocument) {
            OffsetRange offsetRange = new OffsetRange(formalParameter.getStartOffset(), formalParameter.getEndOffset());
            return new Hint(
                    InitializeFieldSuggestion.this,
                    Bundle.InitializeFieldSuggestionText(getName()),
                    fileObject,
                    offsetRange,
                    Collections.<HintFix>singletonList(new Fix(this, baseDocument)),
                    500);
        }

        public void initialize(EditList editList) {
            for (Initializer initializer : initializers) {
                initializer.initialize(editList);
            }
        }

    }

    private interface Initializer {

        void initialize(EditList editList);

    }

    private abstract static class InitializerImpl implements Initializer {
        private final int offset;

        public InitializerImpl(int offset) {
            this.offset = offset;
        }

        @Override
        public void initialize(EditList editList) {
            editList.replace(offset, 0, getInitString(), true, 0);
        }

        public abstract String getInitString();

    }

    private static class FieldDeclarationInitializer extends InitializerImpl {
        private final String initString;

        public FieldDeclarationInitializer(int offset, FormalParameter node) {
            super(offset);
            Expression parameterType = node.getParameterType();
            String typeName = parameterType == null ? null : CodeUtils.extractQualifiedName(parameterType);
            String typePart = ""; //NOI18N
            if (typeName != null) {
                typePart = "/**\n * @var " + typeName + "\n */\n"; //NOI18N
            }
            String parameterName = extractParameterName(node.getParameterName());
            initString = "\n" + typePart + "private " + parameterName + ";\n"; //NOI18N
        }

        @Override
        public String getInitString() {
            return initString;
        }

    }

    private static class FieldAssignmentInitializer extends InitializerImpl {
        private final String initString;

        public FieldAssignmentInitializer(int offset, String parameterName) {
            super(offset);
            initString = "$this->" + parameterName.substring(1) + " = " + parameterName + ";\n"; //NOI18N
        }

        @Override
        public String getInitString() {
            return initString;
        }

    }

    private static final class Fix implements HintFix {
        private final ParameterToInit parameterToInit;
        private final BaseDocument baseDocument;

        private Fix(ParameterToInit parameterToInit, BaseDocument baseDocument) {
            this.parameterToInit = parameterToInit;
            this.baseDocument = baseDocument;
        }

        @Override
        @NbBundle.Messages({
            "# {0} - Field name",
            "InitializeFieldSuggestionFix=Initialize Field: {0}"
        })
        public String getDescription() {
            return Bundle.InitializeFieldSuggestionFix(parameterToInit.getName());
        }

        @Override
        public void implement() throws Exception {
            EditList editList = new EditList(baseDocument);
            parameterToInit.initialize(editList);
            editList.apply();
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

    }

    @CheckForNull
    private static String extractParameterName(Expression parameterNameExpression) {
        String result = null;
        if (parameterNameExpression instanceof Variable) {
            result = CodeUtils.extractVariableName((Variable) parameterNameExpression);
        } else if (parameterNameExpression instanceof Reference) {
            Reference reference = (Reference) parameterNameExpression;
            Expression expression = reference.getExpression();
            if (expression instanceof Variable) {
                result = CodeUtils.extractVariableName((Variable) expression);
            }
        } else if(parameterNameExpression instanceof Variadic) { // #249306
            Variadic variadic = (Variadic) parameterNameExpression;
            Expression expression = variadic.getExpression();
            if (expression instanceof Variable) {
                result = CodeUtils.extractVariableName((Variable) expression);
            }
        }
        return result;
    }

    @Override
    public String getId() {
        return SUGGESTION_ID;
    }

    @Override
    @NbBundle.Messages("InitializeFieldSuggestionDesc=Initializes field with a parameter passed to constructor.")
    public String getDescription() {
        return Bundle.InitializeFieldSuggestionDesc();
    }

    @Override
    @NbBundle.Messages("InitializeFieldSuggestionDisp=Initialize Field in Constructor")
    public String getDisplayName() {
        return Bundle.InitializeFieldSuggestionDisp();
    }

}
