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

import jdk.nashorn.internal.ir.BinaryNode;
import jdk.nashorn.internal.ir.FunctionNode;
import jdk.nashorn.internal.ir.Node;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jdk.nashorn.internal.parser.TokenType;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.Rule;
import org.netbeans.modules.javascript2.editor.hints.JsHintsProvider.JsRuleContext;
import org.netbeans.modules.javascript2.editor.model.impl.ModelUtils;
import org.netbeans.modules.javascript2.editor.model.impl.PathNodeVisitor;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Pisl
 */
public class WeirdAssignment extends JsAstRule {

    @Override
    void computeHints(JsRuleContext context, List<Hint> hints, int offset, HintsProvider.HintsManager manager) {
        WeirdVisitor conventionVisitor = new WeirdVisitor(this);
        conventionVisitor.process(context, hints);
    }

    @Override
    public Set<?> getKinds() {
        return Collections.singleton(JsAstRule.JS_OTHER_HINTS);
    }

    @Override
    public String getId() {
        return "jsweirdassignment.hint";
    }

    @NbBundle.Messages("JsWeirdAssignmentDesc=Weird assignment hint informs you about assignments like x = x.")
    @Override
    public String getDescription() {
        return Bundle.JsWeirdAssignmentDesc();
    }

    @NbBundle.Messages("JsWeirdAssignmentDN=Weird assignment")
    @Override
    public String getDisplayName() {
        return Bundle.JsWeirdAssignmentDN();
    }
    
    private static class WeirdVisitor extends PathNodeVisitor {
        private List<Hint> hints;
        private JsRuleContext context;
        private final Rule rule;
        
        public WeirdVisitor(Rule rule) {
            this.rule = rule;
        }
        
        public void process(JsRuleContext context, List<Hint> hints) {
            this.hints = hints;
            this.context = context;
            FunctionNode root = context.getJsParserResult().getRoot();
            if (root != null) {
                context.getJsParserResult().getRoot().accept(this);
            }
        }

        @Override
        public Node enter(BinaryNode binaryNode) {
            if (binaryNode.tokenType() == TokenType.ASSIGN && binaryNode.lhs().toString().equals(binaryNode.rhs().toString())) {
                hints.add(new Hint(rule, Bundle.JsWeirdAssignmentDN(),
                        context.getJsParserResult().getSnapshot().getSource().getFileObject(),
                        ModelUtils.documentOffsetRange(context.getJsParserResult(), binaryNode.getStart(), binaryNode.getFinish()), null, 500));
            }
            return super.enter(binaryNode);
        }
    }
}
