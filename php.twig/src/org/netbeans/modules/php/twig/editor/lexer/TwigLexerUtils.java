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
package org.netbeans.modules.php.twig.editor.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.api.Snapshot;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public final class TwigLexerUtils {

    private TwigLexerUtils() {
    }

    public static TokenSequence<? extends TokenId> getTwigMarkupTokenSequence(final Snapshot snapshot, final int offset) {
        TokenSequence<? extends TwigBlockTokenId> twigBlockTokenSequence = getTokenSequence(snapshot.getTokenHierarchy(), offset, TwigBlockTokenId.language());
        return twigBlockTokenSequence == null ? getTokenSequence(snapshot.getTokenHierarchy(), offset, TwigVariableTokenId.language()) : twigBlockTokenSequence;
    }

    public static TokenSequence<? extends TokenId> getTwigMarkupTokenSequence(final Document document, final int offset) {
        TokenSequence<? extends TwigBlockTokenId> twigBlockTokenSequence = getTwigBlockTokenSequence(document, offset);
        return twigBlockTokenSequence == null ? getTwigVariableTokenSequence(document, offset) : twigBlockTokenSequence;
    }

    public static TokenSequence<? extends TwigBlockTokenId> getTwigBlockTokenSequence(final Document document, final int offset) {
        TokenHierarchy<Document> th = TokenHierarchy.get(document);
        return getTokenSequence(th, offset, TwigBlockTokenId.language());
    }

    public static TokenSequence<? extends TwigVariableTokenId> getTwigVariableTokenSequence(final Document document, final int offset) {
        TokenHierarchy<Document> th = TokenHierarchy.get(document);
        return getTokenSequence(th, offset, TwigVariableTokenId.language());
    }

    public static TokenSequence<? extends TwigTopTokenId> getTwigTokenSequence(final Snapshot snapshot, final int offset) {
        return getTokenSequence(snapshot.getTokenHierarchy(), offset, TwigTopTokenId.language());
    }

    public static TokenSequence<? extends TwigTopTokenId> getTwigTokenSequence(final Document document, final int offset) {
        TokenHierarchy<Document> th = TokenHierarchy.get(document);
        return getTokenSequence(th, offset, TwigTopTokenId.language());
    }

    public static <L> TokenSequence<? extends L> getTokenSequence(final TokenHierarchy<?> th, final int offset, final Language<? extends L> language) {
        TokenSequence<? extends L> ts = th.tokenSequence(language);
        if (ts == null) {
            List<TokenSequence<?>> list = th.embeddedTokenSequences(offset, true);
            for (TokenSequence t : list) {
                if (t.language() == language) {
                    ts = t;
                    break;
                }
            }
            if (ts == null) {
                list = th.embeddedTokenSequences(offset, false);
                for (TokenSequence t : list) {
                    if (t.language() == language) {
                        ts = t;
                        break;
                    }
                }
            }
        }
        return ts;
    }

    public static List<OffsetRange> findForwardMatching(TokenSequence<? extends TwigTopTokenId> topTs, TwigTokenText start, TwigTokenText end) {
        return findForwardMatching(topTs, start, end, Collections.<TwigTokenText>emptyList());
    }

    public static List<OffsetRange> findForwardMatching(
            TokenSequence<? extends TwigTopTokenId> topTs,
            TwigTokenText start,
            TwigTokenText end,
            List<TwigTokenText> middle) {
        List<OffsetRange> result = new ArrayList<>();
        topTs.moveNext();
        int originalOffset = topTs.offset();
        int balance = 1;
        while (topTs.moveNext()) {
            Token<? extends TwigTopTokenId> token = topTs.token();
            if (token != null && (token.id() == TwigTopTokenId.T_TWIG_BLOCK || token.id() == TwigTopTokenId.T_TWIG_VAR)) {
                TokenSequence<TwigBlockTokenId> markupTs = topTs.embedded(TwigBlockTokenId.language());
                if (markupTs != null) {
                    markupTs.moveNext();
                    while (markupTs.moveNext()) {
                        Token<? extends TwigBlockTokenId> markupToken = markupTs.token();
                        if (start.matches(markupToken)) {
                            balance++;
                        } else if (end.matches(markupToken)) {
                            balance--;
                            if (balance == 0) {
                                result.add(new OffsetRange(markupTs.offset(), markupTs.offset() + markupToken.length()));
                                break;
                            }
                        } else if (matchesToken(middle, markupToken)) {
                            if (balance == 1) {
                                result.add(new OffsetRange(markupTs.offset(), markupTs.offset() + markupToken.length()));
                                break;
                            }
                        }
                    }
                    if (balance == 0) {
                        break;
                    }
                }
            }
        }
        topTs.move(originalOffset);
        return result;
    }

    private static boolean matchesToken(List<TwigTokenText> middle, Token<? extends TwigBlockTokenId> markupToken) {
        boolean result = false;
        for (TwigTokenText twigTokenText : middle) {
            if (twigTokenText.matches(markupToken)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static List<OffsetRange> findBackwardMatching(
            TokenSequence<? extends TwigTopTokenId> topTs,
            TwigTokenText start,
            TwigTokenText end,
            List<TwigTokenText> middle) {
        List<OffsetRange> result = new ArrayList<>();
        topTs.movePrevious();
        int originalOffset = topTs.offset();
        int balance = 1;
        while (topTs.movePrevious()) {
            Token<? extends TwigTopTokenId> token = topTs.token();
            if (token != null && (token.id() == TwigTopTokenId.T_TWIG_BLOCK || token.id() == TwigTopTokenId.T_TWIG_VAR)) {
                TokenSequence<TwigBlockTokenId> markupTs = topTs.embedded(TwigBlockTokenId.language());
                if (markupTs != null) {
                    markupTs.moveEnd();
                    while (markupTs.movePrevious()) {
                        Token<? extends TwigBlockTokenId> markupToken = markupTs.token();
                        if (start.matches(markupToken)) {
                            balance++;
                        } else if (end.matches(markupToken)) {
                            balance--;
                            if (balance == 0) {
                                result.add(new OffsetRange(markupTs.offset(), markupTs.offset() + markupToken.length()));
                                break;
                            }
                        } else if (matchesToken(middle, markupToken)) {
                            if (balance == 1) {
                                result.add(new OffsetRange(markupTs.offset(), markupTs.offset() + markupToken.length()));
                                break;
                            }
                        }
                    }
                    if (balance == 0) {
                        break;
                    }
                }
            }
        }
        topTs.move(originalOffset);
        return result;
    }

    public static List<OffsetRange> findBackwardMatching(TokenSequence<? extends TwigTopTokenId> topTs, TwigTokenText start, TwigTokenText end) {
        return findBackwardMatching(topTs, start, end, Collections.<TwigTokenText>emptyList());
    }

    public interface TwigTokenText {
        TwigTokenText NONE = new TwigTokenText() {

            @Override
            public boolean matches(Token<? extends TwigBlockTokenId> token) {
                return false;
            }
        };

        boolean matches(Token<? extends TwigBlockTokenId> token);
    }

    public static final class TwigTokenTextImpl implements TwigTokenText {
        private final TwigBlockTokenId tokenId;
        private final String tokenText;

        public static TwigTokenText create(TwigBlockTokenId tokenId, String tokenText) {
            return new TwigTokenTextImpl(tokenId, tokenText);
        }

        private TwigTokenTextImpl(TwigBlockTokenId tokenId, String tokenText) {
            this.tokenId = tokenId;
            this.tokenText = tokenText;
        }

        @Override
        public boolean matches(Token<? extends TwigBlockTokenId> token) {
            return token != null && token.id() == tokenId && tokenText.equals(token.text().toString());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.tokenId);
            hash = 71 * hash + Objects.hashCode(this.tokenText);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TwigTokenTextImpl other = (TwigTokenTextImpl) obj;
            if (this.tokenId != other.tokenId) {
                return false;
            }
            return Objects.equals(this.tokenText, other.tokenText);
        }

        @Override
        public String toString() {
            return "TwigTokenText{" + "tokenId=" + tokenId + ", tokenText=" + tokenText + '}';
        }

    }

}
