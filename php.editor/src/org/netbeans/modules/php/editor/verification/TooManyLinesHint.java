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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.Block;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Comment;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.InterfaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.LambdaFunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.Program;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public abstract class TooManyLinesHint extends HintRule implements CustomisableRule {

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

    public static class FunctionLinesHint extends TooManyLinesHint {
        private static final String HINT_ID = "Function.Lines.Hint"; //NOI18N
        private static final String MAX_ALLOWED_FUNCTION_LINES = "php.verification.max.allowed.function.lines"; //NOI18N
        private static final int DEFAULT_MAX_ALLOWED_FUNCTION_LINES = 20;
        private Preferences preferences;

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new FunctionVisitor(this, fileObject, baseDocument, getMaxAllowedLines(preferences));
        }

        @Override
        public void setPreferences(Preferences preferences) {
            assert preferences != null;
            this.preferences = preferences;
        }

        public void setMaxAllowedLines(Preferences preferences, Integer value) {
            assert preferences != null;
            assert value != null;
            preferences.putInt(MAX_ALLOWED_FUNCTION_LINES, value);
        }

        private static final class FunctionVisitor extends CheckVisitor {
            private final int maxAllowedLines;

            public FunctionVisitor(TooManyLinesHint linesHint, FileObject fileObject, BaseDocument baseDocument, int maxAllowedLines) {
                super(linesHint, fileObject, baseDocument);
                this.maxAllowedLines = maxAllowedLines;
            }

            @Override
            public void visit(FunctionDeclaration node) {
                super.visit(node);
                checkBlock(node.getBody(), new OffsetRange(node.getFunctionName().getStartOffset(), node.getFunctionName().getEndOffset()));
            }

            @Override
            public void visit(LambdaFunctionDeclaration node) {
                super.visit(node);
                checkBlock(node.getBody(), new OffsetRange(node.getStartOffset(), node.getBody().getStartOffset()));
            }

            @Override
            public void visit(InterfaceDeclaration node) {
                //don't check interface methods
            }

            @NbBundle.Messages({
                "# {0} - function length in lines",
                "# {1} - allowed lines per function declaration",
                "FunctionLinesHintText=Method Length is {0} Lines ({1} allowed)"
            })
            private void checkBlock(Block block, OffsetRange warningRange) {
                int countLines = block == null ? 0 : countLines(block);
                if (countLines > maxAllowedLines) {
                    addHint(Bundle.FunctionLinesHintText(countLines, maxAllowedLines), warningRange);
                }
            }

        }

        public int getMaxAllowedLines(Preferences preferences) {
            assert preferences != null;
            return preferences.getInt(MAX_ALLOWED_FUNCTION_LINES, DEFAULT_MAX_ALLOWED_FUNCTION_LINES);
        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("FunctionLinesHintDesc=Maximum allowed lines per function/method declaration.")
        public String getDescription() {
            return Bundle.FunctionLinesHintDesc();
        }

        @Override
        @NbBundle.Messages("FunctionLinesHintDisp=Function (Method) Declaration")
        public String getDisplayName() {
            return Bundle.FunctionLinesHintDisp();
        }

        @Override
        public JComponent getCustomizer(Preferences preferences) {
            JComponent customizer = new FunctionLinesCustomizer(preferences, this);
            setMaxAllowedLines(preferences, getMaxAllowedLines(preferences));
            return customizer;
        }

    }

    public static class ClassLinesHint extends TooManyLinesHint {
        private static final String HINT_ID = "Class.Lines.Hint"; //NOI18N
        private static final String MAX_ALLOWED_CLASS_LINES = "php.verification.max.allowed.class.lines"; //NOI18N
        private static final int DEFAULT_MAX_ALLOWED_CLASS_LINES = 200;
        private Preferences preferences;

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new ClassVisitor(this, fileObject, baseDocument, getMaxAllowedLines(preferences));
        }

        @Override
        public void setPreferences(Preferences preferences) {
            this.preferences = preferences;
        }

        public void setMaxAllowedLines(Preferences preferences, Integer value) {
            assert preferences != null;
            assert value != null;
            preferences.putInt(MAX_ALLOWED_CLASS_LINES, value);
        }

        private static final class ClassVisitor extends CheckVisitor {
            private final int maxAllowedLines;

            public ClassVisitor(TooManyLinesHint linesHint, FileObject fileObject, BaseDocument baseDocument, int maxAllowedLines) {
                super(linesHint, fileObject, baseDocument);
                this.maxAllowedLines = maxAllowedLines;
            }

            @Override
            public void visit(ClassDeclaration node) {
                super.visit(node);
                checkBlock(node.getBody(), new OffsetRange(node.getName().getStartOffset(), node.getName().getEndOffset()));
            }

            @NbBundle.Messages({
                "# {0} - class length in lines",
                "# {1} - allowed lines per class declaration",
                "ClassLinesHintText=Class Length is {0} Lines ({1} allowed)"
            })
            private void checkBlock(Block block, OffsetRange warningRange) {
                int countLines = countLines(block);
                if (countLines > maxAllowedLines) {
                    addHint(Bundle.ClassLinesHintText(countLines, maxAllowedLines), warningRange);
                }
            }

        }

        public int getMaxAllowedLines(Preferences preferences) {
            assert preferences != null;
            return preferences.getInt(MAX_ALLOWED_CLASS_LINES, DEFAULT_MAX_ALLOWED_CLASS_LINES);
        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("ClassLinesHintDesc=Maximum allowed lines per class declaration.")
        public String getDescription() {
            return Bundle.ClassLinesHintDesc();
        }

        @Override
        @NbBundle.Messages("ClassLinesHintDisp=Class Declaration")
        public String getDisplayName() {
            return Bundle.ClassLinesHintDisp();
        }

        @Override
        public JComponent getCustomizer(Preferences preferences) {
            JComponent customizer = new ClassLinesCustomizer(preferences, this);
            setMaxAllowedLines(preferences, getMaxAllowedLines(preferences));
            return customizer;
        }

    }

    public static class InterfaceLinesHint extends TooManyLinesHint {
        private static final String HINT_ID = "Interface.Lines.Hint"; //NOI18N
        private static final String MAX_ALLOWED_INTERFACE_LINES = "php.verification.max.allowed.interface.lines"; //NOI18N
        private static final int DEFAULT_MAX_ALLOWED_INTERFACE_LINES = 100;
        private Preferences preferences;

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new InterfaceVisitor(this, fileObject, baseDocument, getMaxAllowedLines(preferences));
        }

        @Override
        public void setPreferences(Preferences preferences) {
            this.preferences = preferences;
        }

        public void setMaxAllowedLines(Preferences preferences, Integer value) {
            assert preferences != null;
            assert value != null;
            preferences.putInt(MAX_ALLOWED_INTERFACE_LINES, value);
        }

        private static final class InterfaceVisitor extends CheckVisitor {
            private final int maxAllowedLines;

            public InterfaceVisitor(TooManyLinesHint linesHint, FileObject fileObject, BaseDocument baseDocument, int maxAllowedLines) {
                super(linesHint, fileObject, baseDocument);
                this.maxAllowedLines = maxAllowedLines;
            }

            @Override
            public void visit(InterfaceDeclaration node) {
                super.visit(node);
                checkBlock(node.getBody(), new OffsetRange(node.getName().getStartOffset(), node.getName().getEndOffset()));
            }

            @NbBundle.Messages({
                "# {0} - interface length in lines",
                "# {1} - allowed lines per interface declaration",
                "InterfaceLinesHintText=Interface Length is {0} Lines ({1} allowed)"
            })
            private void checkBlock(Block block, OffsetRange warningRange) {
                int countLines = countLines(block);
                if (countLines > maxAllowedLines) {
                    addHint(Bundle.InterfaceLinesHintText(countLines, maxAllowedLines), warningRange);
                }
            }

        }

        public int getMaxAllowedLines(Preferences preferences) {
            assert preferences != null;
            return preferences.getInt(MAX_ALLOWED_INTERFACE_LINES, DEFAULT_MAX_ALLOWED_INTERFACE_LINES);
        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("InterfaceLinesHintDesc=Maximum allowed lines per interface declaration.")
        public String getDescription() {
            return Bundle.InterfaceLinesHintDesc();
        }

        @Override
        @NbBundle.Messages("InterfaceLinesHintDisp=Interface Declaration")
        public String getDisplayName() {
            return Bundle.InterfaceLinesHintDisp();
        }

        @Override
        public JComponent getCustomizer(Preferences preferences) {
            JComponent customizer = new InterfaceLinesCustomizer(preferences, this);
            setMaxAllowedLines(preferences, getMaxAllowedLines(preferences));
            return customizer;
        }

    }

    public static class TraitLinesHint extends TooManyLinesHint {
        private static final String HINT_ID = "Trait.Lines.Hint"; //NOI18N
        private static final String MAX_ALLOWED_TRAIT_LINES = "php.verification.max.allowed.trait.lines"; //NOI18N
        private static final int DEFAULT_MAX_ALLOWED_TRAIT_LINES = 200;
        private Preferences preferences;

        @Override
        CheckVisitor createVisitor(FileObject fileObject, BaseDocument baseDocument) {
            return new TraitVisitor(this, fileObject, baseDocument, getMaxAllowedLines(preferences));
        }

        @Override
        public void setPreferences(Preferences preferences) {
            this.preferences = preferences;
        }

        public void setMaxAllowedLines(Preferences preferences, Integer value) {
            assert preferences != null;
            assert value != null;
            preferences.putInt(MAX_ALLOWED_TRAIT_LINES, value);
        }

        private static final class TraitVisitor extends CheckVisitor {
            private final int maxAllowedLines;

            public TraitVisitor(TooManyLinesHint linesHint, FileObject fileObject, BaseDocument baseDocument, int maxAllowedLines) {
                super(linesHint, fileObject, baseDocument);
                this.maxAllowedLines = maxAllowedLines;
            }

            @Override
            public void visit(TraitDeclaration node) {
                super.visit(node);
                checkBlock(node.getBody(), new OffsetRange(node.getName().getStartOffset(), node.getName().getEndOffset()));
            }

            @NbBundle.Messages({
                "# {0} - trait length in lines",
                "# {1} - allowed lines per trait declaration",
                "TraitLinesHintText=Trait Length is {0} Lines ({1} allowed)"
            })
            private void checkBlock(Block block, OffsetRange warningRange) {
                int countLines = countLines(block);
                if (countLines > maxAllowedLines) {
                    addHint(Bundle.TraitLinesHintText(countLines, maxAllowedLines), warningRange);
                }
            }

        }

        public int getMaxAllowedLines(Preferences preferences) {
            assert preferences != null;
            return preferences.getInt(MAX_ALLOWED_TRAIT_LINES, DEFAULT_MAX_ALLOWED_TRAIT_LINES);
        }

        @Override
        public String getId() {
            return HINT_ID;
        }

        @Override
        @NbBundle.Messages("TraitLinesHintDesc=Maximum allowed lines per trait declaration.")
        public String getDescription() {
            return Bundle.TraitLinesHintDesc();
        }

        @Override
        @NbBundle.Messages("TraitLinesHintDisp=Trait Declaration")
        public String getDisplayName() {
            return Bundle.TraitLinesHintDisp();
        }

        @Override
        public JComponent getCustomizer(Preferences preferences) {
            JComponent customizer = new TraitLinesCustomizer(preferences, this);
            setMaxAllowedLines(preferences, getMaxAllowedLines(preferences));
            return customizer;
        }

    }

    private abstract static class CheckVisitor extends DefaultVisitor {
        private static final Logger LOGGER = Logger.getLogger(CheckVisitor.class.getName());
        private final List<Hint> hints;
        private final BaseDocument baseDocument;
        private final FileObject fileObject;
        private final TooManyLinesHint linesHint;
        private final List<OffsetRange> commentRanges;

        private CheckVisitor(TooManyLinesHint linesHint, FileObject fileObject, BaseDocument baseDocument) {
            this.linesHint = linesHint;
            this.fileObject = fileObject;
            this.baseDocument = baseDocument;
            hints = new ArrayList<>();
            commentRanges = new ArrayList<>();
        }

        @Override
        public void visit(Program node) {
            for (Comment comment : node.getComments()) {
                commentRanges.add(new OffsetRange(comment.getStartOffset(), comment.getEndOffset()));
            }
            super.visit(node);
        }

        protected int countLines(final Block block) {
            final AtomicInteger result = new AtomicInteger(0);
            baseDocument.render(new Runnable() {

                @Override
                public void run() {
                    result.set(countLinesUnderReadLock(block));
                }
            });
            return result.get();
        }

        private int countLinesUnderReadLock(Block block) {
            int result = 0;
            try {
                result = tryCountLines(block);
            } catch (BadLocationException ex) {
                // see issue 227687 and #172881
                LOGGER.log(Level.FINE, null, ex);
            }
            return result;
        }

        private int tryCountLines(Block block) throws BadLocationException {
            int searchOffset = block.getStartOffset() + 1;
            int firstNonWhiteFwd = Utilities.getFirstNonWhiteFwd(baseDocument, searchOffset);
            int startLineOffset = Utilities.getLineOffset(baseDocument, firstNonWhiteFwd == -1 ? searchOffset : firstNonWhiteFwd);
            int endLineOffset = Utilities.getLineOffset(baseDocument, Utilities.getFirstNonWhiteBwd(baseDocument, block.getEndOffset()));
            return countLinesBetweenLineOffsets(startLineOffset, endLineOffset);
        }

        private int countLinesBetweenLineOffsets(int startLineOffset, int endLineOffset) throws BadLocationException {
            int result = 0;
            for (int lineOffset = startLineOffset; lineOffset < endLineOffset; lineOffset++) {
                int rowStartFromLineOffset = Utilities.getRowStartFromLineOffset(baseDocument, lineOffset);
                if (!Utilities.isRowWhite(baseDocument, rowStartFromLineOffset) && !isJustCommentOnLine(rowStartFromLineOffset)) {
                    result++;
                }
            }
            return result;
        }

        private boolean isJustCommentOnLine(int rowStartOffset) throws BadLocationException {
            boolean result = false;
            int rowFirstNonWhite = Utilities.getRowFirstNonWhite(baseDocument, rowStartOffset);
            int rowLastNonWhite = Utilities.getRowLastNonWhite(baseDocument, rowStartOffset);
            for (OffsetRange commentRange : commentRanges) {
                if (commentRange.containsInclusive(rowFirstNonWhite) && commentRange.containsInclusive(rowLastNonWhite)) {
                    result = true;
                    break;
                }
            }
            return result;
        }

        protected void addHint(String description, OffsetRange warningRange) {
            if (linesHint.showHint(warningRange, baseDocument)) {
                hints.add(new Hint(linesHint, description, fileObject, warningRange, null, 500));
            }
        }

        public List<Hint> getHints() {
            return hints;
        }

    }

}
