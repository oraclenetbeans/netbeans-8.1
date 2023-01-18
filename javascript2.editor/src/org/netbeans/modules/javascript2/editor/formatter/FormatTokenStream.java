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
package org.netbeans.modules.javascript2.editor.formatter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;

/**
 *
 * @author Petr Hejl
 */
public final class FormatTokenStream implements Iterable<FormatToken> {

    private final TreeMap<Integer, FormatToken> tokenPosition = new TreeMap<Integer, FormatToken>();

    private FormatToken firstToken;

    private FormatToken lastToken;

    private FormatTokenStream() {
        super();
    }

    public static FormatTokenStream create(TokenSequence<? extends JsTokenId> ts, int start, int end) {
        FormatTokenStream ret = new FormatTokenStream();
        int diff = ts.move(start);
        if (diff <= 0) {
            ts.movePrevious();
        }
        ret.addToken(FormatToken.forFormat(FormatToken.Kind.SOURCE_START));

        while (ts.moveNext() && ts.offset() < end) {
            Token<? extends JsTokenId> token = ts.token();
            JsTokenId id = token.id();
            switch (id) {
                case EOL:
                    ret.addToken(FormatToken.forAny(FormatToken.Kind.EOL, ts.offset(), token.text(), id));
                    break;
                case WHITESPACE:
                    ret.addToken(FormatToken.forAny(FormatToken.Kind.WHITESPACE, ts.offset(), token.text(), id));
                    break;
                case BLOCK_COMMENT:
                    ret.addToken(FormatToken.forAny(FormatToken.Kind.BLOCK_COMMENT, ts.offset(), token.text(), id));
                    break;
                case DOC_COMMENT:
                    ret.addToken(FormatToken.forAny(FormatToken.Kind.DOC_COMMENT, ts.offset(), token.text(), id));
                    break;
                case LINE_COMMENT:
                    ret.addToken(FormatToken.forAny(FormatToken.Kind.LINE_COMMENT, ts.offset(), token.text(), id));
                    break;
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
                case OPERATOR_PLUS:
                case OPERATOR_MINUS:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_BINARY_OPERATOR_WRAP));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_BINARY_OPERATOR));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_BINARY_OPERATOR));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_BINARY_OPERATOR_WRAP));
                    break;
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
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_ASSIGNMENT_OPERATOR));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_ASSIGNMENT_OPERATOR));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_ASSIGNMENT_OPERATOR_WRAP));
                    break;
                case OPERATOR_COMMA:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_COMMA));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_COMMA));
                    break;
                case OPERATOR_DOT:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_DOT));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_DOT));
                    break;
                case KEYWORD_IF:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_IF_KEYWORD));
                    break;
                case KEYWORD_WHILE:
                    // we do not put before here, we do it in visitor to put it
                    // only for do while
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_WHILE_KEYWORD));
                    break;
                case KEYWORD_FOR:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_FOR_KEYWORD));
                    break;
                case KEYWORD_WITH:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_WITH_KEYWORD));
                    break;
                case KEYWORD_SWITCH:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_SWITCH_KEYWORD));
                    break;
                case KEYWORD_CATCH:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_CATCH_KEYWORD));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_CATCH_KEYWORD));
                    break;
                case KEYWORD_ELSE:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_ELSE_KEYWORD));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    break;
                case KEYWORD_FINALLY:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_FINALLY_KEYWORD));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    break;
                case KEYWORD_VAR:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_VAR_KEYWORD));
                    break;
                case KEYWORD_NEW:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_NEW_KEYWORD));
                    break;
                case KEYWORD_TYPEOF:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_TYPEOF_KEYWORD));
                    break;
                case OPERATOR_SEMICOLON:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_SEMICOLON));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_SEMICOLON));
                    break;
                case BRACKET_LEFT_PAREN:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.AFTER_LEFT_PARENTHESIS));
                    break;
                case BRACKET_RIGHT_PAREN:
                    ret.addToken(FormatToken.forFormat(FormatToken.Kind.BEFORE_RIGHT_PARENTHESIS));
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    break;
                default:
                    ret.addToken(FormatToken.forText(ts.offset(), token.text(), id));
                    break;
            }
        }
        return ret;
    }

    @CheckForNull
    public static FormatToken getNextNonVirtual(FormatToken token) {
        FormatToken current = token.next();
        while (current != null && current.isVirtual()) {
            current = current.next();
        }
        return current;
    }

    @CheckForNull
    public static FormatToken getNextImportant(FormatToken token) {
        FormatToken current = token.next();
        while (current != null && (current.isVirtual()
                || current.getKind() == FormatToken.Kind.WHITESPACE
                || current.getKind() == FormatToken.Kind.EOL
                || current.getKind() == FormatToken.Kind.DOC_COMMENT
                || current.getKind() == FormatToken.Kind.BLOCK_COMMENT
                || current.getKind() == FormatToken.Kind.LINE_COMMENT)) {
            current = current.next();
        }
        return current;
    }

    @CheckForNull
    public static FormatToken getPreviousNonVirtual(FormatToken token) {
        FormatToken current = token.previous();
        while (current != null && current.isVirtual()) {
            current = current.previous();
        }
        return current;
    }

    public FormatToken getToken(int offset) {
        return tokenPosition.get(offset);
    }

    /**
     * Returns token containing the offset if any. Returned token does not
     * have to start at the offset but it has to cover it.
     * @param offset the give offset
     * @return token containing the offset or <code>null</code>
     */
    public FormatToken getCoveringToken(int offset) {
        Map.Entry<Integer, FormatToken> entry = tokenPosition.floorEntry(offset);
        if (entry != null) {
            FormatToken token = entry.getValue();
            if (!token.isVirtual()) {
                if (token.getOffset() == offset) {
                    return token;
                }
                int endPos = token.getOffset() + token.getText().length();
                if (offset >= token.getOffset() && offset < endPos) {
                    return token;
                }
            }
        }
        return null;
    }

    @Override
    public Iterator<FormatToken> iterator() {
        return new FormatTokenIterator();
    }

    public List<FormatToken> getTokens() {
        List<FormatToken> tokens = new ArrayList<FormatToken>((int) (tokenPosition.size() * 1.5));
        for (FormatToken token : this) {
            tokens.add(token);
        }
        return tokens;
    }

    public void addToken(FormatToken token) {
        if (firstToken == null) {
            firstToken = token;
            lastToken = token;
        } else {
            lastToken.setNext(token);
            token.setPrevious(lastToken);
            lastToken = token;
        }

        if (token.getOffset() >= 0) {
            tokenPosition.put(token.getOffset(), token);
        }
    }

    public void removeToken(FormatToken token) {
        assert token.isVirtual() : token;
        FormatToken previous = token.previous();
        FormatToken next = token.next();

        token.setNext(null);
        token.setPrevious(null);

        if (token.getOffset() >= 0) {
            tokenPosition.remove(token.getOffset());
        }

        if (previous == null) {
            assert firstToken == token;
            firstToken = next;
            next.setPrevious(null);
        }
        if (next == null) {
            assert lastToken == token;
            lastToken = previous;
            previous.setNext(null);
        }
        if (previous == null || next == null) {
            return;
        }
        previous.setNext(next);
        next.setPrevious(previous);
    }

    private class FormatTokenIterator implements Iterator<FormatToken> {

        private FormatToken current = firstToken;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public FormatToken next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            FormatToken ret = current;
            current = current.next();
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove operation not supported.");
        }

    }
}
