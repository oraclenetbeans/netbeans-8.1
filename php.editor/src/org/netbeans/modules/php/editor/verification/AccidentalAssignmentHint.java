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

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.Assignment;
import org.netbeans.modules.php.editor.parser.astnodes.DoStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.ForStatement;
import org.netbeans.modules.php.editor.parser.astnodes.IfStatement;
import org.netbeans.modules.php.editor.parser.astnodes.WhileStatement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class AccidentalAssignmentHint extends HintRule implements CustomisableRule {

    private static final String HINT_ID = "Accidental.Assignment.Hint"; //NOI18N
    private static final String CHECK_ASSIGNMENTS_IN_SUB_STATEMENTS = "php.verification.check.assignments.in.sub.statements"; //NOI18N
    private static final boolean CHECK_ASSIGNMENTS_IN_SUB_STATEMENTS_DEFAULT = false;
    private static final String CHECK_ASSIGNMENTS_IN_WHILE_STATEMENTS = "php.verification.check.assignments.in.while.statements"; //NOI18N
    private static final boolean CHECK_ASSIGNMENTS_IN_WHILE_STATEMENTS_DEFAULT = false;
    private static final Logger LOGGER = Logger.getLogger(AccidentalAssignmentHint.class.getName());
    private Preferences preferences;

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() == null) {
            return;
        }
        FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
        if (fileObject == null) {
            return;
        }
        CheckVisitor checkVisitor = new CheckVisitor(fileObject, context.doc);
        phpParseResult.getProgram().accept(checkVisitor);
        hints.addAll(checkVisitor.getHints());
    }

    private class CheckVisitor extends DefaultVisitor {

        private final FileObject fileObject;
        private final List<Hint> hints = new ArrayList<>();
        private final List<Assignment> accidentalAssignments = new ArrayList<>();
        private final BaseDocument doc;

        public CheckVisitor(FileObject fileObject, BaseDocument doc) {
            this.fileObject = fileObject;
            this.doc = doc;
        }

        public List<Hint> getHints() {
            for (Assignment assignment : accidentalAssignments) {
                createHint(assignment);
            }
            return hints;
        }

        @Messages({
            "# {0} - Condition text",
            "AccidentalAssignmentHintCustom=Accidental assignment in a condition {0}"
        })
        private void createHint(Assignment assignment) {
            OffsetRange offsetRange = new OffsetRange(assignment.getStartOffset(), assignment.getEndOffset());
            if (showHint(offsetRange, doc)) {
                hints.add(new Hint(
                        AccidentalAssignmentHint.this,
                        Bundle.AccidentalAssignmentHintCustom(asText(assignment)),
                        fileObject,
                        offsetRange,
                        createFixes(assignment), 500));
            }
        }

        private String asText(Assignment assignment) {
            String retval = ""; //NOI18N
            try {
                int start = assignment.getStartOffset();
                int end = assignment.getEndOffset();
                retval = doc.getText(start, end - start);
            } catch (BadLocationException ex) {
                LOGGER.log(Level.FINE, "Can't obtain assignment text.", ex);
            }
            return retval;
        }

        private List<HintFix> createFixes(Assignment assignment) {
            List<HintFix> fixes = new ArrayList<>();
            fixes.add(new IdenticalComparisonHintFix(assignment, doc));
            fixes.add(new EqualComparisonHintFix(assignment, doc));
            return fixes;
        }

        private void processCondition(Expression node) {
            if (checkAssignmentsInSubStatements(preferences)) {
                processSubAssignments(node);
            } else if (node instanceof Assignment) {
                accidentalAssignments.add((Assignment) node);
            }
        }

        private void processSubAssignments(Expression node) {
            AssignmentVisitor assignmentVisitor = new AssignmentVisitor();
            node.accept(assignmentVisitor);
            accidentalAssignments.addAll(assignmentVisitor.getAccidentalAssignments());
        }

        @Override
        public void visit(DoStatement node) {
            if (checkAssignmentsInWhileStatements(preferences)) {
                processCondition(node.getCondition());
            }
            scan(node.getBody());
        }

        @Override
        public void visit(IfStatement node) {
            processCondition(node.getCondition());
            scan(node.getTrueStatement());
            scan(node.getFalseStatement());
        }

        @Override
        public void visit(ForStatement node) {
            for (Expression condition : node.getConditions()) {
                processCondition(condition);
            }
            scan(node.getInitializers());
            scan(node.getUpdaters());
            scan(node.getBody());
        }

        @Override
        public void visit(WhileStatement node) {
            if (checkAssignmentsInWhileStatements(preferences)) {
                processCondition(node.getCondition());
            }
            scan(node.getBody());
        }

    }

    private static class AssignmentVisitor extends DefaultVisitor {

        private final List<Assignment> accidentalAssignments = new ArrayList<>();

        @Override
        public void visit(Assignment node) {
            accidentalAssignments.add(node);
            scan(node.getRightHandSide());
        }

        public List<Assignment> getAccidentalAssignments() {
            return Collections.unmodifiableList(accidentalAssignments);
        }

    }

    private abstract class ComparisonHintFix implements HintFix {

        private final Assignment assignment;
        private final BaseDocument doc;

        public ComparisonHintFix(Assignment assignment, BaseDocument doc) {
            this.assignment = assignment;
            this.doc = doc;
        }

        @Override
        @Messages({
            "# {0} - Comparison text",
            "ChangeAssignmentDisp=Change assignment to comparison: {0}"
        })
        public String getDescription() {
            return Bundle.ChangeAssignmentDisp(getCorrectedAssignmentText());
        }

        @org.netbeans.api.annotations.common.SuppressWarnings({"DLS_DEAD_LOCAL_STORE"})
        private String getCorrectedAssignmentText() {
            StringBuilder sb = new StringBuilder();
            try {
                sb.append(getExpressionText(assignment.getLeftHandSide()));
                sb.append(" ").append(getOperatorText()).append(" "); //NOI18N
                sb.append(getExpressionText(assignment.getRightHandSide()));
                return sb.toString();
            } catch (BadLocationException ex) {
                LOGGER.log(Level.FINE, "Can't obtain corrected assignment text.", ex);
            }
            return sb.toString();
        }

        private String getExpressionText(Expression expression) throws BadLocationException {
            int start = expression.getStartOffset();
            int end = expression.getEndOffset();
            return doc.getText(start, end - start);
        }

        @Override
        public void implement() throws Exception {
            EditList edits = new EditList(doc);
            OffsetRange offsetRange = getOffsetRange();
            edits.replace(offsetRange.getStart(), offsetRange.getLength(), getCorrectedAssignmentText(), true, 0);
            edits.apply();
        }

        private OffsetRange getOffsetRange() {
            int start = assignment.getStartOffset();
            int end = assignment.getEndOffset();
            return new OffsetRange(start, end);
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        protected abstract String getOperatorText();

    }

    private class EqualComparisonHintFix extends ComparisonHintFix {

        public EqualComparisonHintFix(Assignment assignment, BaseDocument doc) {
            super(assignment, doc);
        }

        @Override
        protected String getOperatorText() {
            return "=="; //NOI18N
        }

    }

    private class IdenticalComparisonHintFix extends ComparisonHintFix {

        public IdenticalComparisonHintFix(Assignment assignment, BaseDocument doc) {
            super(assignment, doc);
        }

        @Override
        protected String getOperatorText() {
            return "==="; //NOI18N
        }

    }

    @Override
    public String getId() {
        return HINT_ID;
    }

    @Override
    @Messages("AccidentalAssignmentHintDesc=Using an assignment operator (=) instead of comparison operator (===) is a frequent cause of bugs. Therefore assignments in conditional clauses should be avoided.")
    public String getDescription() {
        return Bundle.AccidentalAssignmentHintDesc();
    }

    @Override
    @Messages("AccidentalAssignmentHintDispName=Accidental Assignments")
    public String getDisplayName() {
        return Bundle.AccidentalAssignmentHintDispName();
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
        JComponent customizer = new AccidentalAssignmentCustomizer(preferences, this);
        setCheckAssignmentsInSubStatements(preferences, checkAssignmentsInSubStatements(preferences));
        setCheckAssignmentsInWhileStatements(preferences, checkAssignmentsInWhileStatements(preferences));
        return customizer;
    }

    public void setCheckAssignmentsInSubStatements(Preferences preferences, boolean isEnabled) {
        preferences.putBoolean(CHECK_ASSIGNMENTS_IN_SUB_STATEMENTS, isEnabled);
    }

    public boolean checkAssignmentsInSubStatements(Preferences preferences) {
        return preferences.getBoolean(CHECK_ASSIGNMENTS_IN_SUB_STATEMENTS, CHECK_ASSIGNMENTS_IN_SUB_STATEMENTS_DEFAULT);
    }

    public void setCheckAssignmentsInWhileStatements(Preferences preferences, boolean isEnabled) {
        preferences.putBoolean(CHECK_ASSIGNMENTS_IN_WHILE_STATEMENTS, isEnabled);
    }

    public boolean checkAssignmentsInWhileStatements(Preferences preferences) {
        return preferences.getBoolean(CHECK_ASSIGNMENTS_IN_WHILE_STATEMENTS, CHECK_ASSIGNMENTS_IN_WHILE_STATEMENTS_DEFAULT);
    }

}
