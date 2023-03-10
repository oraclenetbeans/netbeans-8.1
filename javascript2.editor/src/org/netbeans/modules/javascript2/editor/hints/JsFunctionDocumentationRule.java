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

import jdk.nashorn.internal.ir.FunctionNode;
import jdk.nashorn.internal.ir.IdentNode;
import jdk.nashorn.internal.ir.Node;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.Rule;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.javascript2.editor.doc.spi.DocParameter;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.hints.JsHintsProvider.JsRuleContext;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.model.impl.ModelUtils;
import org.netbeans.modules.javascript2.editor.model.impl.PathNodeVisitor;
import org.openide.util.NbBundle;

/**
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public class JsFunctionDocumentationRule extends JsAstRule {

    public static final String JSDOCUMENTATION_OPTION_HINTS = "jsdocumentation.option.hints"; //NOI18N

    @Override
    void computeHints(JsRuleContext context, List<Hint> hints, int offset, HintsProvider.HintsManager manager) {
        Map<?, List<? extends AstRule>> allHints = manager.getHints();
        List<? extends AstRule> conventionHints = allHints.get(JSDOCUMENTATION_OPTION_HINTS);
        Rule undocumentedParameterRule = null;
        Rule incorrectDocumentationRule = null;
        if (conventionHints != null) {
            for (AstRule astRule : conventionHints) {
                if (manager.isEnabled(astRule)) {
                    if (astRule instanceof UndocumentedParameterRule) {
                        undocumentedParameterRule = astRule;
                    } else if (astRule instanceof IncorrectDocumentationRule) {
                        incorrectDocumentationRule = astRule;
                    }
                }
            }
        }
        JsFunctionDocumentationVisitor conventionVisitor = new JsFunctionDocumentationVisitor(
                undocumentedParameterRule,
                incorrectDocumentationRule);
        conventionVisitor.process(context, hints);
    }

    @Override
    public boolean appliesTo(RuleContext context) {
        if (context instanceof JsHintsProvider.JsRuleContext) {
            return true;
        }
        return false;
    }

    @Override
    public boolean showInTasklist() {
        return true;
    }

    @Override
    public HintSeverity getDefaultSeverity() {
        return HintSeverity.WARNING;
    }


    @Override
    public boolean getDefaultEnabled() {
        return true;
    }

    @Override
    public JComponent getCustomizer(Preferences node) {
        return null;
    }

    @Override
    public Set<?> getKinds() {
        return Collections.singleton(JSDOCUMENTATION_OPTION_HINTS);
    }

    @Override
    public String getId() {
        return "jsdocumentation.hint"; //NOI18N
    }

    @Override
    @NbBundle.Messages("JsDocumentationHintDesc=JavaScript Documentation Hints")
    public String getDescription() {
        return Bundle.JsDocumentationHintDesc();
    }

    @Override
    @NbBundle.Messages("JsDocumentationHintDisplayName=JavaScript Documentation")
    public String getDisplayName() {
        return Bundle.JsDocumentationHintDisplayName();
    }

    @NbBundle.Messages({
        "# {0} - parameter name which is incorectly specified", "IncorrectDocumentationRuleDisplayDescription=Incorrect Documentation: {0}",
        "# {0} - parameter name which is undocumented", "UndocumentedParameterRuleDisplayDescription=Undocumented Parameters: {0}"})
    private static final class JsFunctionDocumentationVisitor extends PathNodeVisitor {

        private List<Hint> hints;
        private JsHintsProvider.JsRuleContext context;
        private final Rule undocumentedParameterRule;
        private final Rule incorrectDocumentationRule;

        private JsFunctionDocumentationVisitor(Rule undocumentedParameterRule, Rule incorrectDocumentationRule) {
            this.incorrectDocumentationRule = incorrectDocumentationRule;
            this.undocumentedParameterRule = undocumentedParameterRule;
        }

        public void process(JsHintsProvider.JsRuleContext context, List<Hint> hints) {
            this.hints = hints;
            this.context = context;
            FunctionNode root = context.getJsParserResult().getRoot();
            if (root != null) {
                context.getJsParserResult().getRoot().accept(this);
            }
        }

        @Override
        public Node enter(FunctionNode fn) {
            JsDocumentationHolder docHolder = context.getJsParserResult().getDocumentationHolder();
            if (fn.getParent() == null
                    || docHolder.getCommentForOffset(fn.getStart(), docHolder.getCommentBlocks()) == null) {
                return super.enter(fn);
            }

            List<DocParameter> docParameters = docHolder.getParameters(fn);
            List<IdentNode> funcParameters = fn.getParameters();

            // undocumented parameter related
            String missingParameters = missingParameters(funcParameters, docParameters);
            if (!missingParameters.isEmpty() && undocumentedParameterRule != null) {
                hints.add(new Hint(
                        undocumentedParameterRule,
                        Bundle.UndocumentedParameterRuleDisplayDescription(missingParameters),
                        context.getJsParserResult().getSnapshot().getSource().getFileObject(),
                        ModelUtils.documentOffsetRange(context.getJsParserResult(), fn.getIdent().getStart(), fn.getIdent().getFinish()),
                        null,
                        600));
            }

            // incorect documentation related
            String superfluousParameters = superfluousParameters(funcParameters, docParameters);
            if (!superfluousParameters.isEmpty() && incorrectDocumentationRule != null) {
                hints.add(new Hint(
                        incorrectDocumentationRule,
                        Bundle.IncorrectDocumentationRuleDisplayDescription(superfluousParameters),
                        context.getJsParserResult().getSnapshot().getSource().getFileObject(),
                        ModelUtils.documentOffsetRange(context.getJsParserResult(), fn.getIdent().getStart(), fn.getIdent().getFinish()),
                        null,
                        600));
            }

            return super.enter(fn);
        }

        private String missingParameters(List<IdentNode> functionParams, List<DocParameter> documentationParams) {
            StringBuilder sb = new StringBuilder();
            String delimiter = ""; //NOI18N
            for (IdentNode identNode : functionParams) {
                if (!containFunctionParamName(documentationParams, identNode.getName())) {
                    sb.append(delimiter).append(identNode.getName());
                    delimiter = ", "; //NOI18N
                }
            }
            return sb.toString();
        }

        private boolean containFunctionParamName(List<DocParameter> documentationParams, String functionParamName) {
            for (DocParameter docParameter : documentationParams) {
                if (docParameter.getParamName() != null
                        && docParameter.getParamName().getName().equals(functionParamName)) {
                    return true;
                }
            }
            return false;
        }

        private String superfluousParameters(List<IdentNode> functionParams, List<DocParameter> documentationParams) {
            StringBuilder sb = new StringBuilder();
            String delimiter = ""; //NOI18N
            for (DocParameter docParameter : documentationParams) {
                if (docParameter.isOptional()) {
                    continue;
                }
                Identifier paramName = docParameter.getParamName();
                if (paramName != null && !containDocParamName(functionParams, paramName.getName())) {
                    sb.append(delimiter).append(docParameter.getParamName().getName());
                    delimiter = ", "; //NOI18N
                }
            }
            return sb.toString();
        }

        private boolean containDocParamName(List<IdentNode> functionParams, String documentationParamName) {
            for (IdentNode identNode : functionParams) {
                if (identNode.getName().equals(documentationParamName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
