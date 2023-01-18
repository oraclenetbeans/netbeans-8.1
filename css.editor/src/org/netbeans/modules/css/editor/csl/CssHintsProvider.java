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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.css.editor.csl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.Rule;
import org.netbeans.modules.csl.api.Rule.ErrorRule;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.css.lib.api.CssParserResult;
import org.netbeans.modules.css.lib.api.FilterableError;
import org.netbeans.modules.css.lib.api.FilterableError.SetFilterAction;
import org.openide.util.NbBundle;

/**
 *
 * @author mfukala@netbeans.org
 */
public class CssHintsProvider implements HintsProvider {

    /**
     * Compute hints applicable to the given compilation info and add to the
     * given result list.
     */
    @Override
    public void computeHints(HintsManager manager, RuleContext context, List<Hint> hints) {
    }

    /**
     * Compute any suggestions applicable to the given caret offset, and add to
     * the given suggestion list.
     */
    @Override
    public void computeSuggestions(HintsManager manager, RuleContext context, List<Hint> suggestions, int caretOffset) {
    }

    /**
     * Compute any suggestions applicable to the given caret offset, and add to
     * the given suggestion list.
     */
    @Override
    public void computeSelectionHints(HintsManager manager, RuleContext context, List<Hint> suggestions, int start, int end) {
    }

    /**
     * Process the errors for the given compilation info, and add errors and
     * warning descriptions into the provided hint list. Return any errors that
     * were not added as error descriptions (e.g. had no applicable error rule)
     */
    @Override
    public void computeErrors(HintsManager manager, RuleContext context, List<Hint> hints, List<Error> unhandled) {
        CssParserResult result = (CssParserResult) context.parserResult;
        Collection<String> disableFixActionNames = new HashSet<>();
        for (FilterableError e : result.getDiagnostics(true)) {
            if (e.isFiltered()) {
                FilterableError.SetFilterAction disableFilterAction = e.getDisableFilterAction();
                if (!disableFixActionNames.contains(disableFilterAction.getDisplayName())) {
                    disableFixActionNames.add(disableFilterAction.getDisplayName());
                    //add hint for reenabling the property
                    hints.add(new Hint(new CssRule(HintSeverity.WARNING),
                            //                        getMessageKey(e.getKey(), true), //NOI18N
                            disableFilterAction.getDisplayName(),
                            context.parserResult.getSnapshot().getSource().getFileObject(),
                            new OffsetRange(0, 0),
                            Collections.<HintFix>singletonList(new ErrorCheckFix(disableFilterAction)),
                            10));
                }
            } else {
                Collection<FilterableError.SetFilterAction> enableFilterActions = e.getEnableFilterActions();
                List<HintFix> fixes = new ArrayList<>();
                for (SetFilterAction action : enableFilterActions) {
                    fixes.add(new ErrorCheckFix(action));
                }

                int astFrom = e.getStartPosition();
                int astTo = e.getEndPosition();

                int docFrom = context.parserResult.getSnapshot().getOriginalOffset(astFrom);
                int docTo = context.parserResult.getSnapshot().getOriginalOffset(astTo);

                if (docFrom == -1 || docTo == -1) {
                    //One of the error offsets falls to virtual source.
                    //The situation very likely means that the css parsing error
                    //is implied by missing content which is generated by some
                    //templating language in the runtime => ignore the error
                    continue;
                }

                Hint h = new Hint(getCssRule(e.getSeverity()),
                        e.getDescription(),
                        e.getFile(),
                        new OffsetRange(docFrom, docTo),
                        fixes,
                        10);

                hints.add(h);
            }
        }
    }

    /**
     * Cancel in-progress processing of hints.
     */
    @Override
    public void cancel() {
        //todo implement
    }

    /**
     * <p>
     * Optional builtin Rules. Typically you don't use this; you register your
     * rules in your filesystem layer in the gsf-hints/mimetype1/mimetype2
     * folder, for example gsf-hints/text/x-ruby/. Error hints should go in the
     * "errors" folder, selection hints should go in the "selection" folder, and
     * all other hints should go in the "hints" folder (but note that you can
     * create localized folders and organize them under hints; these categories
     * are shown in the hints options panel. Hints returned from this method
     * will be placed in the "general" folder.
     * </p>
     * <p>
     * This method is primarily intended for rules that should be added
     * dynamically, for example for Rules that have a many different flavors yet
     * a single implementation class (such as JavaScript's StrictWarning rule
     * which wraps a number of builtin parser warnings.)
     *
     * @return A list of rules that are builtin, or null or an empty list when
     * there are no builtins
     */
    @Override
    public List<Rule> getBuiltinRules() {
        return null;
    }

    /**
     * Create a RuleContext object specific to this HintsProvider. This lets
     * implementations of this interface created subclasses of the RuleContext
     * that can be passed around to all the executed rules.
     *
     * @return A new instance of a RuleContext object
     */
    @Override
    public RuleContext createRuleContext() {
        return new RuleContext();
    }
    private static final CssRule ERROR_RULE = new CssRule(HintSeverity.ERROR);
    private static final CssRule WARNING_RULE = new CssRule(HintSeverity.WARNING);

    private static CssRule getCssRule(Severity s) {
        switch (s) {
            case WARNING:
                return WARNING_RULE;
            case ERROR:
                return ERROR_RULE;
            default:
                throw new AssertionError("Unexpected severity level"); //NOI18N
        }
    }

    private static final class CssRule implements ErrorRule {

        private final HintSeverity severity;

        private CssRule(HintSeverity severity) {
            this.severity = severity;
        }

        @Override
        public Set<?> getCodes() {
            return Collections.emptySet();
        }

        @Override
        public boolean appliesTo(RuleContext context) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "css"; //NOI18N //does this show up anywhere????
        }

        @Override
        public boolean showInTasklist() {
            return true;
        }

        @Override
        public HintSeverity getDefaultSeverity() {
            return severity;
        }
    }

    private static String getMessageKey(String errorKey, boolean enabled) {
        String param = null;
        String keyEnable = null;
        String keyDisable = null;
        if (CssAnalyser.isUnknownPropertyError(errorKey)) {
            keyEnable = "MSG_Disable_Ignore_Property"; //NOI18N
            keyDisable = "MSG_Enable_Ignore_Property"; //NOI18N
            param = CssAnalyser.getUnknownPropertyName(errorKey);
        } else {
            keyEnable = "MSG_Disable_Check"; //NOI18N
            keyDisable = "MSG_Enable_Check"; //NOI18N

        }
        return enabled
                ? NbBundle.getMessage(CssHintsProvider.class, keyEnable, param)
                : NbBundle.getMessage(CssHintsProvider.class, keyDisable, param);

    }

    private static final class ErrorCheckFix implements HintFix {

        private final SetFilterAction action;

        public ErrorCheckFix(SetFilterAction action) {
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
