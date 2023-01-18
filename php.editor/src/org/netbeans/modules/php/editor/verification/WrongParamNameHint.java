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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.api.Utils;
import org.netbeans.modules.php.editor.parser.astnodes.Comment;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.FormalParameter;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocNode;
import org.netbeans.modules.php.editor.parser.astnodes.PHPDocVarTypeTag;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class WrongParamNameHint extends HintRule {
    private static final Logger LOGGER = Logger.getLogger(WrongParamNameHint.class.getName());
    private static final String HINT_ID = "wrong.param.name.hint"; //NOI18N

    @Override
    public void invoke(PHPRuleContext context, List<Hint> result) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() != null) {
            FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
            if (fileObject != null) {
                CheckVisitor checkVisitor = new CheckVisitor(fileObject, context.doc);
                phpParseResult.getProgram().accept(checkVisitor);
                result.addAll(checkVisitor.getHints());
            }
        }
    }

    private final class CheckVisitor extends DefaultVisitor {
        private final FileObject fileObject;
        private final BaseDocument doc;
        private final List<Hint> hints;
        private Program program;

        private CheckVisitor(FileObject fileObject, BaseDocument doc) {
            this.fileObject = fileObject;
            this.doc = doc;
            hints = new ArrayList<>();
        }

        private Collection<? extends Hint> getHints() {
            return hints;
        }

        @Override
        public void visit(Program program) {
            this.program = program;
            super.visit(program);
        }

        @Override
        public void visit(FunctionDeclaration node) {
            Comment comment = Utils.getCommentForNode(program, node);
            if (comment != null) {
                checkNodeWithComment(node, comment);
            }
        }

        private void checkNodeWithComment(FunctionDeclaration node, Comment comment) {
            CommentVisitor commentVisitor = new CommentVisitor();
            comment.accept(commentVisitor);
            List<PHPDocNode> paramVariables = commentVisitor.getParamVariables();
            List<FormalParameter> formalParameters = node.getFormalParameters();
            if (formalParameters.size() == paramVariables.size()) {
                for (int i = 0; i < paramVariables.size(); i++) {
                    checkParametersEquality(paramVariables, formalParameters, i);
                }
            }
        }

        private void checkParametersEquality(List<PHPDocNode> paramVariables, List<FormalParameter> formalParameters, int i) {
            PHPDocNode paramVariable = paramVariables.get(i);
            String paramVariableName = paramVariable.getValue();
            FormalParameter formalParameter = formalParameters.get(i);
            Expression parameterNameExpression = formalParameter.getParameterName();
            if (parameterNameExpression instanceof Variable) {
                Variable parameterVariable = (Variable) parameterNameExpression;
                String parameterName = CodeUtils.extractVariableName(parameterVariable);
                if (StringUtils.hasText(paramVariableName) && !paramVariableName.equals(parameterName)) {
                    createHint(paramVariable, parameterName);
                }
            }
        }

        @NbBundle.Messages("WrongParamNameHintText=Wrong Param Name")
        private void createHint(PHPDocNode paramVariable, String parameterName) {
            OffsetRange checkOffsetRange = new OffsetRange(paramVariable.getStartOffset(), getLineEnd(paramVariable));
            if (showHint(checkOffsetRange, doc)) {
                OffsetRange variableOffsetRange = new OffsetRange(paramVariable.getStartOffset(), paramVariable.getEndOffset());
                hints.add(new Hint(
                        WrongParamNameHint.this,
                        Bundle.WrongParamNameHintText(),
                        fileObject,
                        variableOffsetRange,
                        Collections.<HintFix>singletonList(new Fix(doc, variableOffsetRange, parameterName)),
                        500));
            }
        }

        private int getLineEnd(PHPDocNode paramVariable) {
            int result = paramVariable.getEndOffset();
            try {
                result = Utilities.getRowEnd(doc, paramVariable.getStartOffset());
            } catch (BadLocationException ex) {
                LOGGER.log(Level.FINE, null, ex);
            }
            return result;
        }

    }

    private static final class CommentVisitor extends DefaultVisitor {
        private final List<PHPDocNode> paramVariables = new ArrayList<>();

        @Override
        public void visit(PHPDocVarTypeTag node) {
            paramVariables.add(node.getVariable());
        }

        public List<PHPDocNode> getParamVariables() {
            return paramVariables;
        }

    }

    private static final class Fix implements HintFix {
        private final BaseDocument doc;
        private final OffsetRange offsetRange;
        private final String parameterName;

        public Fix(BaseDocument doc, OffsetRange offsetRange, String parameterName) {
            this.doc = doc;
            this.offsetRange = offsetRange;
            this.parameterName = parameterName;
        }

        @Override
        @NbBundle.Messages("WrongParamNameHintFix=Rename Param")
        public String getDescription() {
            return Bundle.WrongParamNameHintFix();
        }

        @Override
        public void implement() throws Exception {
            EditList editList = new EditList(doc);
            editList.replace(offsetRange.getStart(), offsetRange.getLength(), parameterName, true, 0);
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

    @Override
    public String getId() {
        return HINT_ID;
    }

    @Override
    @NbBundle.Messages("WrongParamNameHintDesc=Parameter names in @param annotations should correspond with parameter names in commented functions.")
    public String getDescription() {
        return Bundle.WrongParamNameHintDesc();
    }

    @Override
    @NbBundle.Messages("WrongParamNameHintName=Wrong Param Name")
    public String getDisplayName() {
        return Bundle.WrongParamNameHintName();
    }

}
