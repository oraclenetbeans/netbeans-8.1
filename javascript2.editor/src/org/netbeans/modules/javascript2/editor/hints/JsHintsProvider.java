/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.editor.hints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.Rule;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.css.lib.api.FilterableError;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.web.common.api.Lines;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Pisl
 */
public class JsHintsProvider implements HintsProvider {

    private static final Logger LOGGER = Logger.getLogger(JsHintsProvider.class.getName());

    private volatile boolean cancel = false;

    @org.netbeans.api.annotations.common.SuppressWarnings("BC_UNCONFIRMED_CAST")
    @Override
    public void computeHints(HintsManager manager, RuleContext context, List<Hint> hints) {
        resume();

        Map<?, List<? extends Rule.AstRule>> allHints = manager.getHints(false, context);
        // find out whether there is a convention hint enabled
        List<? extends Rule.AstRule> conventionHints = allHints.get(JsConventionHint.JSCONVENTION_OPTION_HINTS);
        boolean countConventionHints = false;
        if (conventionHints != null) {
            for (Rule.AstRule astRule : conventionHints) {
                if (manager.isEnabled(astRule)) {
                    countConventionHints = true;
                }
            }
        }
        if (countConventionHints && !cancel) {
            JsConventionRule rule = new JsConventionRule();
            invokeHint(rule, manager, context, hints, -1);
        }

        // find out whether there is a documentation hint enabled
        List<? extends Rule.AstRule> documentationRules = allHints.get(JsFunctionDocumentationRule.JSDOCUMENTATION_OPTION_HINTS);
        boolean documentationHints = false;
        if (documentationRules != null) {
            for (Rule.AstRule astRule : documentationRules) {
                if (manager.isEnabled(astRule)) {
                    documentationHints = true;
                }
            }
        }
        if (documentationHints && !cancel) {
            JsFunctionDocumentationRule rule = new JsFunctionDocumentationRule();
            invokeHint(rule, manager, context, hints, -1);
        }

        List<? extends Rule.AstRule> otherHints = allHints.get(WeirdAssignment.JS_OTHER_HINTS);
        if (otherHints != null && !cancel) {
            for (Rule.AstRule astRule : otherHints) {
                if (manager.isEnabled(astRule)) {
                    JsAstRule rule = (JsAstRule)astRule;
                    invokeHint(rule, manager, context, hints, -1);
                }
            }
        }
    }

    @Override
    public void computeSuggestions(HintsManager manager, RuleContext context, List<Hint> suggestions, int caretOffset) {
        resume();

        Map<?, List<? extends Rule.AstRule>> allSuggestions = manager.getHints(true, context);
        List<? extends Rule.AstRule> otherHints = allSuggestions.get(WeirdAssignment.JS_OTHER_HINTS);
        if (otherHints != null && !cancel) {
            for (Rule.AstRule astRule : otherHints) {
                if (manager.isEnabled(astRule)) {
                    JsAstRule rule = (JsAstRule)astRule;
                    invokeHint(rule, manager, context, suggestions, caretOffset);
                }
            }
        }
    }

    @Override
    public void computeSelectionHints(HintsManager manager, RuleContext context, List<Hint> suggestions, int start, int end) {

    }

    @NbBundle.Messages({
        "MSG_HINT_ENABLE_ERROR_CHECKS_FILE_DESCR=JavaScript error checking for this file is disabled, you can enable it with this hint"
    })
    @Override
    public void computeErrors(HintsManager manager, RuleContext context, List<Hint> hints, List<Error> unhandled) {
        resume();

        JsParserResult parserResult = (JsParserResult) context.parserResult;
        List<? extends org.netbeans.modules.csl.api.Error> errors = parserResult.getDiagnostics();
        // if in embedded
        if (parserResult.isEmbedded()) {
            String mimeType = ErrorCheckingSupport.getMimeType(parserResult);
            List<HintFix> defaultFixes = new ArrayList<HintFix>(2);
            if (!ErrorCheckingSupport.isErrorCheckingEnabledForFile(parserResult)) {
                defaultFixes.add(ErrorCheckingSupport.createErrorFixForFile(parserResult.getSnapshot(), true));
            }
            if (!ErrorCheckingSupport.isErrorCheckingEnabledForMimetype(mimeType)) {
                defaultFixes.add(ErrorCheckingSupport.createErrorFixForMimeType(
                        parserResult.getSnapshot(), mimeType, true));
            }

            if (!errors.isEmpty()) {
                if (ErrorCheckingSupport.isErrorCheckingEnabled(parserResult, mimeType)) {
                    List<HintFix> errorFixes = new ArrayList<HintFix>(2);
                    if (ErrorCheckingSupport.isErrorCheckingEnabledForFile(parserResult)) {
                        errorFixes.add(ErrorCheckingSupport.createErrorFixForFile(parserResult.getSnapshot(), false));
                    }
                    if (ErrorCheckingSupport.isErrorCheckingEnabledForMimetype(mimeType)) {
                        errorFixes.add(ErrorCheckingSupport.createErrorFixForMimeType(
                                parserResult.getSnapshot(), mimeType, false));
                    }

                    Snapshot snapshot = parserResult.getSnapshot();
                    Lines lines = new Lines(snapshot.getText());
                    Set<Integer> linesWithHints = new HashSet<Integer>();

                    for (Error error : errors) {
                        FileObject fo = error.getFile();
                        if (fo == null) {
                            continue;
                        }
                        boolean contains = false;
                        try {
                            int line = lines.getLineIndex(error.getStartPosition());
                            contains = !linesWithHints.add(line);
                        } catch (BadLocationException ex) {
                            LOGGER.log(Level.INFO, null, ex);
                        }

                        int start = snapshot.getOriginalOffset(error.getStartPosition());
                        int end = snapshot.getOriginalOffset(error.getEndPosition());
                        
                        if (start > -1 && end > -1 && start <= end) {
                            Hint h = new Hint(new JsErrorRule(),
                                    error.getDisplayName(),
                                    fo,
                                    new OffsetRange(start, end),
                                    contains ? Collections.<HintFix>emptyList() : errorFixes,
                                    100);
                            hints.add(h);
                        }
                    }
                }
            }

            FileObject fo = parserResult.getSnapshot().getSource().getFileObject();
            if (fo != null && !defaultFixes.isEmpty()) {
                Hint h = new Hint(new JsSwitchRule(),
                        Bundle.MSG_HINT_ENABLE_ERROR_CHECKS_FILE_DESCR(),
                        fo,
                        new OffsetRange(0, 0),
                        defaultFixes,
                        50);
                hints.add(h);
            }
        } else {
            //unhandled.addAll(errors);
            Collection<String> disableFixActionNames = new HashSet<String>();
            for (FilterableError e : parserResult.getErrors(true)) {
                if (e.isFiltered()) {
                FilterableError.SetFilterAction disableFilterAction = e.getDisableFilterAction();
                if (!disableFixActionNames.contains(disableFilterAction.getDisplayName())) {
                    disableFixActionNames.add(disableFilterAction.getDisplayName());
                    //add hint for reenabling the property
                    hints.add(new Hint(new JsSwitchRule(),
                            //                        getMessageKey(e.getKey(), true), //NOI18N
                            disableFilterAction.getDisplayName(),
                            parserResult.getSnapshot().getSource().getFileObject(),
                            new OffsetRange(0, 0),
                            Collections.<HintFix>singletonList(new ErrorCheckFix(disableFilterAction)),
                            10));
                }
            } else {
                Collection<FilterableError.SetFilterAction> enableFilterActions = e.getEnableFilterActions();
                List<HintFix> fixes = new ArrayList<HintFix>();
                for (FilterableError.SetFilterAction action : enableFilterActions) {
                    fixes.add(new ErrorCheckFix(action));
                }

                int astFrom = e.getStartPosition();
                int astTo = e.getEndPosition();
                
                Snapshot snapshot = context.parserResult.getSnapshot();
                int docFrom = snapshot.getOriginalOffset(astFrom);
                int docTo = snapshot.getOriginalOffset(astTo);
                if (docTo == -1 && astTo > snapshot.getText().length() && ((docFrom + 1) == astTo)) {
                    docTo = snapshot.getText().length();
                    docFrom = docTo - 1; 
                }
                if (docFrom == -1 || docTo == -1) {
                    //One of the error offsets falls to virtual source.
                    //The situation very likely means that the css parsing error
                    //is implied by missing content which is generated by some
                    //templating language in the runtime => ignore the error
                    continue;
                }

                Hint h = new Hint(new JsErrorRule(),
                        e.getDescription(),
                        e.getFile(),
                        new OffsetRange(docFrom, docTo),
                        fixes,
                        10);

                hints.add(h);
            }
            }
        }
    }

    @Override
    public void cancel() {
        cancel = true;
    }

    private void resume() {
        cancel = false;
    }

    @Override
    public List<Rule> getBuiltinRules() {
        return Collections.<Rule>emptyList();
    }

    @Override
    public RuleContext createRuleContext() {
        return new JsRuleContext();
    }

    private void invokeHint(JsAstRule rule, HintsManager manager, RuleContext context, List<Hint> suggestions, int caretOffset) {
        try {
            rule.computeHints((JsRuleContext)context, suggestions, caretOffset, manager);
        } catch (BadLocationException ble) {

        }
    }

    public class JsRuleContext extends RuleContext {

        private JsParserResult jsParserResult = null;

        public JsParserResult getJsParserResult() {
            if (jsParserResult == null) {
                jsParserResult = (JsParserResult)parserResult;
            }
            return jsParserResult;
        }

        public boolean isCancelled() {
            return cancel;
        }

    }

    private static class JsSwitchRule implements Rule.ErrorRule {

        @Override
        public Set<?> getCodes() {
            return Collections.emptySet();
        }

        @Override
        public boolean appliesTo(RuleContext context) {
            return true;
        }

        @NbBundle.Messages({
            "JsSwitchRule.displayName=Error checking"
        })
        @Override
        public String getDisplayName() {
            return Bundle.JsSwitchRule_displayName();
        }

        @Override
        public boolean showInTasklist() {
            return false;
        }

        @Override
        public HintSeverity getDefaultSeverity() {
            return HintSeverity.INFO;
        }
    }

    // XXX Rule or subclass ?
    private static class JsErrorRule implements Rule {

        @Override
        public boolean appliesTo(RuleContext context) {
            return true;
        }

        @NbBundle.Messages({
            "JsErrorRule.displayName=JavaScript Error"
        })
        @Override
        public String getDisplayName() {
            return Bundle.JsErrorRule_displayName();
        }

        @Override
        public boolean showInTasklist() {
            return true;
        }

        @Override
        public HintSeverity getDefaultSeverity() {
            return HintSeverity.ERROR;
        }
    }
    
    private static final class ErrorCheckFix implements HintFix {

        private final FilterableError.SetFilterAction action;

        public ErrorCheckFix(FilterableError.SetFilterAction action) {
            this.action = action;
        }

        @Override
        public String getDescription() {
            return action.getDisplayName();
        }

        @Override
        public void implement() throws Exception {
            action.run();
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
}
