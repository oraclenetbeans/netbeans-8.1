/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.javascript2.editor.api.lexer;

import java.util.Arrays;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.lexer.Language;

import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.javascript2.editor.lexer.JsDocumentationTokenId;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.util.Exceptions;


/**
 * Utilities associated with lexing or analyzing the document at the
 * lexical level, unlike AstUtilities which is contains utilities
 * to analyze parsed information about a document.
 *
 * @author Petr Hejl
 * @author Tor Norbye
 */
public final class LexUtilities {

    private LexUtilities() {
        super();
    }

    /** Find the JavaScript token sequence (in case it's embedded in something else at the top level */
    public static TokenSequence<? extends JsTokenId> getJsTokenSequence(Document doc, int offset) {
        TokenHierarchy<Document> th = TokenHierarchy.get(doc);
        return getTokenSequence(th, offset, JsTokenId.javascriptLanguage());
    }

    @CheckForNull
    public static TokenSequence<? extends JsTokenId> getTokenSequence(Snapshot snapshot,
            int offset, Language<JsTokenId> language) {
        TokenHierarchy<?> th = snapshot.getTokenHierarchy();
        return getTokenSequence(th, offset, language);
    }

    @CheckForNull
    public static TokenSequence<? extends JsTokenId> getTokenSequence(Document doc,
            int offset, Language<JsTokenId> language) {
        TokenHierarchy<Document> th = TokenHierarchy.get(doc);
        return getTokenSequence(th, offset, language);
    }

    @CheckForNull
    public static TokenSequence<? extends JsTokenId> getJsTokenSequence(Snapshot snapshot,
            int offset) {
        TokenHierarchy<?> th = snapshot.getTokenHierarchy();
        return getTokenSequence(th, offset, JsTokenId.javascriptLanguage());
    }

    @CheckForNull
    public static TokenSequence<? extends JsTokenId> getJsTokenSequence(TokenHierarchy<?> th, int offset) {
        return getTokenSequence(th, offset, JsTokenId.javascriptLanguage());
    }

    public static TokenSequence<? extends JsDocumentationTokenId> getJsDocumentationTokenSequence(TokenHierarchy<?> th, int offset) {
        return getTokenSequence(th, offset, JsDocumentationTokenId.language());
    }

    public static TokenSequence<? extends JsDocumentationTokenId> getJsDocumentationTokenSequence(Snapshot snapshot, int offset) {
        return getTokenSequence(snapshot.getTokenHierarchy(), offset, JsDocumentationTokenId.language());
    }

    /** Find the JavaScript token sequence (in case it's embedded in something else at the top level */
    @CheckForNull
    public static <K> TokenSequence<? extends K> getTokenSequence(TokenHierarchy<?> th,
            int offset, Language<? extends K> language) {
        TokenSequence<? extends K> ts = th.tokenSequence(language);

        if (ts == null) {
            // Possibly an embedding scenario such as an HTML file
            // First try with backward bias true
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

    /** Find the NEXT JavaScript sequence in the buffer starting from the given offset */
    @SuppressWarnings("unchecked")
    public static TokenSequence<? extends JsTokenId> getNextJsTokenSequence(Document doc, int fromOffset, int max, Language<JsTokenId> language) {
        TokenHierarchy<Document> th = TokenHierarchy.get(doc);
        TokenSequence<?> ts = th.tokenSequence();
        ts.move(fromOffset);

        return findNextJsTokenSequence(ts, fromOffset, max, language);
    }

    @SuppressWarnings("unchecked")
    private static TokenSequence<? extends JsTokenId> findNextJsTokenSequence(TokenSequence<?> ts, int fromOffset, int max, Language<JsTokenId> language) {
        if (ts.language() == language) {
            if (!ts.moveNext()) {
                return null;
            }
            return (TokenSequence<? extends JsTokenId>) ts;
        }

        while (ts.moveNext() && ts.offset() <= max) {
            int offset = ts.offset();

            TokenSequence<?> ets = ts.embedded();
            if (ets != null) {
                ets.move(offset);
                TokenSequence<? extends JsTokenId> result = findNextJsTokenSequence(ets, fromOffset, max, language);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /** Search forwards in the token sequence until a token of type <code>down</code> is found */
    public static OffsetRange findFwd(Document doc, TokenSequence<? extends JsTokenId> ts, TokenId up,
        TokenId down) {
        int balance = 0;

        while (ts.moveNext()) {
            Token<? extends JsTokenId> token = ts.token();
            TokenId id = token.id();
            
            if (id == up) {
                balance++;
            } else if (id == down) {
                if (balance == 0) {
                    return new OffsetRange(ts.offset(), ts.offset() + token.length());
                }

                balance--;
            }
        }

        return OffsetRange.NONE;
    }

    public static OffsetRange findBwd(Document doc, TokenSequence<? extends JsTokenId> ts, TokenId up,
        TokenId down) {
        int balance = 0;

        while (ts.movePrevious()) {
            Token<? extends JsTokenId> token = ts.token();
            TokenId id = token.id();

            if (id == up) {
                if (balance == 0) {
                    return new OffsetRange(ts.offset(), ts.offset() + token.length());
                }

                balance++;
            } else if (id == down) {
                balance--;
            }
        }

        return OffsetRange.NONE;
    }

    public static Token<? extends JsTokenId> getToken(Document doc, int offset, Language<JsTokenId> language) {
        TokenSequence<? extends JsTokenId> ts = getPositionedSequence(doc, offset, language);

        if (ts != null) {
            return ts.token();
        }

        return null;
    }

    public static char getTokenChar(Document doc, int offset, Language<JsTokenId> language) {
        Token<? extends JsTokenId> token = getToken(doc, offset, language);

        if (token != null) {
            String text = token.text().toString();

            if (text.length() > 0) { // Usually true, but I could have gotten EOF right?

                return text.charAt(0);
            }
        }

        return 0;
    }

    /**
     * The same as braceBalance but generalized to any pair of matching
     * tokens.
     * @param open the token that increses the count
     * @param close the token that decreses the count
     */
    public static int getTokenBalance(Document doc, TokenId open, TokenId close,
            int offset, Language<JsTokenId> language) throws BadLocationException {
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getTokenSequence(doc, offset, language);
        if (ts == null) {
            return 0;
        }

        // get balance from start index of the tokenSequence
        ts.moveIndex(0);

        if (!ts.moveNext()) {
            return 0;
        }

        int balance = 0;

        do {
            Token t = ts.token();

            if (t.id() == open) {
                balance++;
            } else if (t.id() == close) {
                balance--;
            }
        } while (ts.moveNext());

        return balance;
    }

    /**
     * Return true iff the line for the given offset is a JavaScript comment line.
     * This will return false for lines that contain comments (even when the
     * offset is within the comment portion) but also contain code.
     */
    public static boolean isCommentOnlyLine(BaseDocument doc, int offset, Language<JsTokenId> language)
            throws BadLocationException {

        int begin = Utilities.getRowFirstNonWhite(doc, offset);

        if (begin == -1) {
            return false; // whitespace only
        }

        Token<? extends JsTokenId> token = LexUtilities.getToken(doc, begin, language);
        if (token != null) {
            return token.id() == JsTokenId.LINE_COMMENT;
        }

        return false;
    }

    /** Compute the balance of begin/end tokens on the line */
    public static int getLineBalance(BaseDocument doc, int offset, TokenId up, TokenId down) {
        try {
            int begin = Utilities.getRowStart(doc, offset);
            int end = Utilities.getRowEnd(doc, offset);

            TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(doc, begin);
            if (ts == null) {
                return 0;
            }

            ts.move(begin);

            if (!ts.moveNext()) {
                return 0;
            }

            int balance = 0;

            do {
                Token<? extends JsTokenId> token = ts.token();
                TokenId id = token.id();

                if (id == up) {
                    balance++;
                } else if (id == down) {
                    balance--;
                }
            } while (ts.moveNext() && (ts.offset() <= end));

            return balance;
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);

            return 0;
        }
    }

    public static TokenSequence<? extends JsTokenId> getPositionedSequence(
            Document doc, int offset, Language<JsTokenId> language) {
        return getPositionedSequence(doc, offset, true, language);
    }

    public static TokenSequence<? extends JsTokenId> getPositionedSequence(
            Snapshot snapshot, int offset, Language<JsTokenId> language) {
        return getPositionedSequence(snapshot, offset, true, language);
    }

    public static TokenSequence<? extends JsTokenId> getJsPositionedSequence(
            Document doc, int offset) {
        return getPositionedSequence(doc, offset, true, JsTokenId.javascriptLanguage());
    }

    public static TokenSequence<? extends JsTokenId> getJsPositionedSequence(
            Snapshot snapshot, int offset) {
        return getPositionedSequence(snapshot, offset, true, JsTokenId.javascriptLanguage());
    }

    public static TokenSequence<? extends JsTokenId> getPositionedSequence(Document doc,
            int offset, boolean lookBack, Language<JsTokenId> language) {
        TokenHierarchy<Document> th = TokenHierarchy.get(doc);
        return _getPosSeq(getTokenSequence(th, offset, language), offset, lookBack);
    }

    private static TokenSequence<? extends JsTokenId> getPositionedSequence(Snapshot snapshot,
            int offset, boolean lookBack, Language<JsTokenId> language) {
        TokenHierarchy<?> th = snapshot.getTokenHierarchy();
        return _getPosSeq(getTokenSequence(th, offset, language), offset, lookBack);
    }

    private static <K> TokenSequence<? extends K> _getPosSeq(TokenSequence<? extends K> ts, int offset, boolean lookBack) {
        if (ts != null) {
            ts.move(offset);

            if (!lookBack && !ts.moveNext()) {
                return null;
            } else if (lookBack && !ts.moveNext() && !ts.movePrevious()) {
                return null;
            }

            return ts;
        }

        return null;
    }
    
    public static int getLexerOffset(JsParserResult info, int astOffset) {
        return info.getSnapshot().getOriginalOffset(astOffset);
    }
    
    public static OffsetRange getLexerOffsets(JsParserResult info, OffsetRange astRange) {
        int rangeStart = astRange.getStart();
        int start = info.getSnapshot().getOriginalOffset(rangeStart);
        if (start == rangeStart) {
            return astRange;
        } else if (start == -1) {
            return OffsetRange.NONE;
        } else {
            // Assumes the translated range maintains size
            return new OffsetRange(start, start + astRange.getLength());
        }
    }
    
    /**
     * Finds the first next token that is not in the ignore list
     * @param ts
     * @param ignores list of ignored tokens
     * @return 
     */
    public static Token<? extends JsTokenId> findNext(TokenSequence<? extends JsTokenId> ts, List<JsTokenId> ignores) {
        if (ignores.contains(ts.token().id())) {
            while (ts.moveNext() && ignores.contains(ts.token().id())) {}
        }
        return ts.token();
    }

    /**
     * Finds the first previous token that is not in the ignore list
     * @param ts
     * @param ignores
     * @return 
     */
    public static Token<? extends JsTokenId> findPrevious(TokenSequence<? extends JsTokenId> ts, List<JsTokenId> ignores) {
        if (ignores.contains(ts.token().id())) {
            while (ts.movePrevious() && ignores.contains(ts.token().id())) {}
        }
        return ts.token();
    }

    /**
     * Finds the first next token from the input list.
     * @param ts
     * @param lookfor
     * @return 
     */
    public static Token<? extends JsTokenId> findNextToken(TokenSequence<? extends JsTokenId> ts, List<JsTokenId> lookfor) {
        if (!lookfor.contains(ts.token().id())) {
            while (ts.moveNext() && !lookfor.contains(ts.token().id())) {}
        }
        return ts.token();
    }

    /**
     * Finds the first previous toke from the input list.
     * @param ts
     * @param lookfor
     * @return 
     */
    public static Token<? extends JsTokenId> findPreviousToken(TokenSequence<? extends JsTokenId> ts, List<JsTokenId> lookfor) {
        if (!lookfor.contains(ts.token().id())) {
            while (ts.movePrevious() && !lookfor.contains(ts.token().id())) {}
        }
        return ts.token();
    }

    public static Token<?extends JsTokenId> findNextIncluding(TokenSequence<?extends JsTokenId> ts, List<JsTokenId> includes) {
        while (ts.moveNext() && !includes.contains(ts.token().id())) {}
        return ts.token();
    }

    public static Token<?extends JsTokenId> findPreviousIncluding(TokenSequence<?extends JsTokenId> ts, List<JsTokenId> includes) {
            while (ts.movePrevious() && !includes.contains(ts.token().id())) {}
        return ts.token();
    }

    public static Token<?extends JsTokenId> findNextNonWsNonComment(TokenSequence<?extends JsTokenId> ts) {
        return findNext(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.EOL, JsTokenId.LINE_COMMENT, JsTokenId.BLOCK_COMMENT, JsTokenId.DOC_COMMENT));
    }

    public static Token<?extends JsTokenId> findPreviousNonWsNonComment(TokenSequence<?extends JsTokenId> ts) {
        return findPrevious(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.EOL, JsTokenId.LINE_COMMENT, JsTokenId.BLOCK_COMMENT, JsTokenId.DOC_COMMENT));
    }

    public static OffsetRange getMultilineRange(Document doc, TokenSequence<? extends JsTokenId> ts) {
        int index = ts.index();
        OffsetRange offsetRange = findMultilineRange(ts);
        ts.moveIndex(index);
        ts.moveNext();
        return offsetRange;
    }

    private static OffsetRange findMultilineRange(TokenSequence<? extends JsTokenId> ts) {
        int startOffset = ts.offset();
        JsTokenId id = ts.token().id();
        switch (id) {
            case KEYWORD_ELSE:
                ts.moveNext();
                id = ts.token().id();
                break;
            case KEYWORD_IF:
            case KEYWORD_FOR:
            case KEYWORD_WHILE:
                ts.moveNext();
                if (!skipParenthesis(ts, false)) {
                    return OffsetRange.NONE;
                }
                id = ts.token().id();
                break;
            default:
                return OffsetRange.NONE;
        }

        boolean eolFound = false;
        int lastEolOffset = ts.offset();

        // skip whitespaces and comments
        if (id == JsTokenId.WHITESPACE || id == JsTokenId.LINE_COMMENT || id == JsTokenId.BLOCK_COMMENT || id == JsTokenId.DOC_COMMENT || id == JsTokenId.EOL) {
            if (ts.token().id() == JsTokenId.EOL) {
                lastEolOffset = ts.offset();
                eolFound = true;
            }
            while (ts.moveNext() && (
                    ts.token().id() == JsTokenId.WHITESPACE ||
                    ts.token().id() == JsTokenId.LINE_COMMENT ||
                    ts.token().id() == JsTokenId.EOL ||
                    ts.token().id() == JsTokenId.BLOCK_COMMENT ||
                    ts.token().id() == JsTokenId.DOC_COMMENT)) {
                if (ts.token().id() == JsTokenId.EOL) {
                    lastEolOffset = ts.offset();
                    eolFound = true;
                }
            }
        }
        // if we found end of sequence or end of line
        if (ts.token() == null || (ts.token().id() != JsTokenId.BRACKET_LEFT_CURLY && eolFound)) {
            return new OffsetRange(startOffset, lastEolOffset);
        }
        return  OffsetRange.NONE;
    }

    /**
     * Tries to skip parenthesis
     */
    public static boolean skipParenthesis(TokenSequence<?extends JsTokenId> ts, boolean back) {
        int balance = 0;

        Token<?extends JsTokenId> token = ts.token();
        if (token == null) {
            return false;
        }

        TokenId id = token.id();

//        // skip whitespaces
//        if (id == JsTokenId.WHITESPACE) {
//            while (ts.moveNext() && ts.token().id() == JsTokenId.WHITESPACE) {}
//        }
        if (id == JsTokenId.WHITESPACE || id == JsTokenId.EOL) {
            while ((back ? ts.movePrevious() : ts.moveNext()) && (ts.token().id() == JsTokenId.WHITESPACE || ts.token().id() == JsTokenId.EOL)) {}
        }

        // if current token is not left parenthesis
        if (ts.token().id() != (back ? JsTokenId.BRACKET_RIGHT_PAREN : JsTokenId.BRACKET_LEFT_PAREN)) {
            return false;
        }

        do {
            token = ts.token();
            id = token.id();

            if (id == (back ? JsTokenId.BRACKET_RIGHT_PAREN : JsTokenId.BRACKET_LEFT_PAREN)) {
                balance++;
            } else if (id == (back ? JsTokenId.BRACKET_LEFT_PAREN : JsTokenId.BRACKET_RIGHT_PAREN)) {
                if (balance == 0) {
                    return false;
                } else if (balance == 1) {
                    //int length = ts.offset() + token.length();
                    if (back) {
                        ts.movePrevious();
                    } else {
                        ts.moveNext();
                    }
                    return true;
                }

                balance--;
            }
        } while (back ? ts.movePrevious() : ts.moveNext());

        return false;
    }

    public static boolean isBinaryOperator(JsTokenId id, JsTokenId previous) {
        switch (id) {
            case OPERATOR_GREATER:
            case OPERATOR_LOWER:
            case OPERATOR_EQUALS:
            case OPERATOR_EQUALS_EXACTLY:
            case OPERATOR_LOWER_EQUALS:
            case OPERATOR_GREATER_EQUALS:
            case OPERATOR_NOT_EQUALS:
            case OPERATOR_NOT_EQUALS_EXACTLY:
            case OPERATOR_AND:
            case OPERATOR_OR:
            case OPERATOR_MULTIPLICATION:
            case OPERATOR_DIVISION:
            case OPERATOR_BITWISE_AND:
            case OPERATOR_BITWISE_OR:
            case OPERATOR_BITWISE_XOR:
            case OPERATOR_MODULUS:
            case OPERATOR_LEFT_SHIFT_ARITHMETIC:
            case OPERATOR_RIGHT_SHIFT_ARITHMETIC:
            case OPERATOR_RIGHT_SHIFT:
            case OPERATOR_ASSIGNMENT:
            case OPERATOR_PLUS_ASSIGNMENT:
            case OPERATOR_MINUS_ASSIGNMENT:
            case OPERATOR_MULTIPLICATION_ASSIGNMENT:
            case OPERATOR_DIVISION_ASSIGNMENT:
            case OPERATOR_BITWISE_AND_ASSIGNMENT:
            case OPERATOR_BITWISE_OR_ASSIGNMENT:
            case OPERATOR_BITWISE_XOR_ASSIGNMENT:
            case OPERATOR_MODULUS_ASSIGNMENT:
            case OPERATOR_LEFT_SHIFT_ARITHMETIC_ASSIGNMENT:
            case OPERATOR_RIGHT_SHIFT_ARITHMETIC_ASSIGNMENT:
            case OPERATOR_RIGHT_SHIFT_ASSIGNMENT:
            case OPERATOR_DOT:
                return true;
            case OPERATOR_PLUS:
            case OPERATOR_MINUS:
                if (previous != null && (previous == JsTokenId.IDENTIFIER || previous == JsTokenId.NUMBER
                        || previous == JsTokenId.REGEXP_END || previous == JsTokenId.STRING_END
                        || previous == JsTokenId.BRACKET_RIGHT_BRACKET || previous == JsTokenId.BRACKET_RIGHT_CURLY
                        || previous == JsTokenId.BRACKET_RIGHT_PAREN)) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }
}
