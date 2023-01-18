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
package org.netbeans.modules.javascript2.editor.jsdoc;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.jsdoc.model.DescriptionElement;
import org.netbeans.modules.javascript2.editor.jsdoc.model.JsDocElement;
import org.netbeans.modules.javascript2.editor.jsdoc.model.JsDocElementType;
import org.netbeans.modules.javascript2.editor.jsdoc.model.JsDocElementUtils;
import org.netbeans.modules.javascript2.editor.lexer.JsDocumentationTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.parsing.api.Snapshot;

/**
 * Parses jsDoc comment blocks.
 * It can return map of these blocks, their end offset in the snapshot.
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public class JsDocParser {

    private static final Logger LOGGER = Logger.getLogger(JsDocParser.class.getName());

    /**
     * Parses given snapshot and returns map of all jsDoc blocks.
     * @param snapshot snapshot to parse
     * @return map of blocks, key is end offset of each block
     */
    public static Map<Integer, JsDocComment> parse(Snapshot snapshot) {
        Map<Integer, JsDocComment> blocks = new HashMap<Integer, JsDocComment>();

        if (snapshot == null || snapshot.getTokenHierarchy() == null) {
            return blocks;
        }
        
        TokenSequence tokenSequence = snapshot.getTokenHierarchy().tokenSequence(JsTokenId.javascriptLanguage());
        if (tokenSequence == null) {
            return blocks;
        }

        while (tokenSequence.moveNext()) {
            if (tokenSequence.token().id() == JsTokenId.DOC_COMMENT) {
                JsDocCommentType commentType = getCommentType(tokenSequence.token().text());
                LOGGER.log(Level.FINEST, "JsDocParser:comment block offset=[{0}-{1}],type={2},text={3}", new Object[]{
                    tokenSequence.offset(), tokenSequence.offset() + tokenSequence.token().length(), commentType, tokenSequence.token().text()});

                OffsetRange offsetRange = new OffsetRange(tokenSequence.offset(), tokenSequence.offset() + tokenSequence.token().length());
                if (commentType == JsDocCommentType.DOC_NO_CODE_START
                        || commentType == JsDocCommentType.DOC_NO_CODE_END
                        || commentType == JsDocCommentType.DOC_SHARED_TAG_END) {
                    blocks.put(offsetRange.getEnd(), new JsDocComment(offsetRange, commentType, Collections.<JsDocElement>emptyList()));
                    continue;
                } else {
                    blocks.put(offsetRange.getEnd(), parseCommentBlock(tokenSequence, offsetRange, commentType));
                    continue;
                }
            }
        }

        return blocks;
    }

    private static boolean isTextToken(Token<? extends JsDocumentationTokenId> token) {
        return (token.id() != JsDocumentationTokenId.ASTERISK && token.id() != JsDocumentationTokenId.COMMENT_DOC_START);
//        return (token.id() != JsDocumentationTokenId.ASTERISK && token.id() != JsDocumentationTokenId.COMMENT_SHARED_BEGIN
//                && token.id() != JsDocumentationTokenId.COMMENT_DOC_START);
    }

    private static TokenSequence getEmbeddedJsDocTS(TokenSequence ts) {
        return ts.embedded(JsDocumentationTokenId.language());
    }

    private static JsDocComment parseCommentBlock(TokenSequence ts, OffsetRange range, JsDocCommentType commentType) {
        TokenSequence ets = getEmbeddedJsDocTS(ts);

        List<JsDocElement> jsDocElements = new ArrayList<JsDocElement>();
        Token<? extends JsDocumentationTokenId> token;
        JsDocElementType type = null;
        boolean afterDescription = false;
        StringBuilder sb = new StringBuilder();
        int offset = ts.offset();
        while (ets.moveNext()) {
            token = ets.token();
            if (!isTextToken(token)) {
                boolean isAsterixType = false; 
                if (token.id() == JsDocumentationTokenId.ASTERISK) {   
                    // we need to check type {*}
                    String currentText = sb.toString();
                    int curlyOpen = currentText.lastIndexOf('{');
                    if (curlyOpen > -1) {
                        String afterText = currentText.substring(curlyOpen + 1);
                        if (afterText.trim().isEmpty() && afterText.indexOf('\n') == -1) {
                            isAsterixType = true;
                        }
                    }
                }
                if (!isAsterixType) {
                    continue;
                }
            }

            JsDocElementType elementType = getJsDocKeywordType(CharSequenceUtilities.toString(token.text()));
            if ((token.id() == JsDocumentationTokenId.KEYWORD && elementType != JsDocElementType.UNKNOWN
                    && elementType != JsDocElementType.LINK) || token.id() == JsDocumentationTokenId.COMMENT_END) {
                if (sb.toString().trim().isEmpty()) {
                    // simple tag
                    if (type != null) {
                        jsDocElements.add(JsDocElementUtils.createElementForType(type, "", -1));
                    }
                } else {
                    // store first description
                    if (!afterDescription) {
                        //TODO - distinguish description and inline comments
                        jsDocElements.add(DescriptionElement.create(JsDocElementType.CONTEXT_SENSITIVE, sb.toString().trim()));
                    } else {
                        jsDocElements.add(JsDocElementUtils.createElementForType(type, sb.toString().trim(), offset));
                    }
                    sb = new StringBuilder();
                }

                while (ets.moveNext() && ets.token().id() == JsDocumentationTokenId.WHITESPACE) {
                    continue;
                }

                offset = ets.offset();
                if (token.id() != JsDocumentationTokenId.COMMENT_END) {
                    ets.movePrevious();
                }
                afterDescription = true;
                type = JsDocElementType.fromString(CharSequenceUtilities.toString(token.text()));
            } else {
                sb.append(token.text());
            }
        }

        return new JsDocComment(range, commentType, jsDocElements);
    }

    private static JsDocCommentType getCommentType(CharSequence text) {
        //TODO - move that into some constatns holder
        if (CharSequenceUtilities.startsWith(text, "/**#")) { //NOI18N
            if (CharSequenceUtilities.textEquals(text, "/**#nocode+*/")) { //NOI18N
                return JsDocCommentType.DOC_NO_CODE_START;
            } else if (CharSequenceUtilities.textEquals(text, "/**#nocode-*/")) {
                return JsDocCommentType.DOC_NO_CODE_END;
            } else if (CharSequenceUtilities.startsWith(text, "/**#@+")) { //NOI18N
                return JsDocCommentType.DOC_SHARED_TAG_START;
            } else if (CharSequenceUtilities.textEquals(text, "/**#@-*/")) { //NOI18N
                return JsDocCommentType.DOC_SHARED_TAG_END;
            }
        }
        return JsDocCommentType.DOC_COMMON;
    }

    private static JsDocElementType getJsDocKeywordType(String string) {
        return JsDocElementType.fromString(string);
    }
}
