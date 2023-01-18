/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.editor.hints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.model.impl.ModelUtils;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Pisl
 */
public class IncorrectReturnStatement extends JsAstRule {

    @Override
    void computeHints(JsHintsProvider.JsRuleContext context, List<Hint> hints, int offset, HintsProvider.HintsManager manager) throws BadLocationException {
        Snapshot snapshot = context.getJsParserResult().getSnapshot();
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(snapshot, context.lexOffset);
        OffsetRange returnOffsetRange;
        if (ts != null) {
            while (ts.moveNext()) {
                Token<? extends JsTokenId> token = LexUtilities.findNextIncluding(ts, Arrays.asList(JsTokenId.KEYWORD_RETURN));
                if (token != null) {
                    JsTokenId tokenId = token.id();
                    if (tokenId == JsTokenId.KEYWORD_RETURN) {
                        returnOffsetRange = new OffsetRange(ts.offset(), ts.offset() + token.length());
                        if (ts.moveNext()) {
                            token = LexUtilities.findNext(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.BLOCK_COMMENT));
                            tokenId = token.id();
                            if (tokenId == JsTokenId.EOL) {
                                addHint(context, hints, offset, JS_OTHER_HINTS, returnOffsetRange);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addHint(JsHintsProvider.JsRuleContext context, List<Hint> hints, int offset, String name, OffsetRange range) throws BadLocationException {
        hints.add(new Hint(this, Bundle.JsIncorrectReturnStatementHintDesc(),
                context.getJsParserResult().getSnapshot().getSource().getFileObject(),
                ModelUtils.documentOffsetRange(context.getJsParserResult(),
                        range.getStart(), range.getEnd()), null, 600));
    }

    @Override
    public Set<?> getKinds() {
        return Collections.singleton(JsAstRule.JS_OTHER_HINTS);
    }

    @Override
    public String getId() {
        return "jsincorrectreturnstatement.hint";
    }

    @NbBundle.Messages({
        "JsIncorrectReturnStatementDesc=Incorrect return statemnt.",
        "JsIncorrectReturnStatementHintDesc=Incorrect return statment"})
    @Override
    public String getDescription() {
        return Bundle.JsIncorrectReturnStatementDesc();
    }

    @Override
    public String getDisplayName() {
        return Bundle.JsIncorrectReturnStatementHintDesc();
    }

    @Override
    public HintSeverity getDefaultSeverity() {
        return HintSeverity.WARNING;
    }

    @Override
    public boolean getDefaultEnabled() {
        return true;
    }

}
