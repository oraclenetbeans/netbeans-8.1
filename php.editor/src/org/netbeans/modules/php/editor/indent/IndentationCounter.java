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
package org.netbeans.modules.php.editor.indent;

import java.util.Arrays;
import java.util.Collection;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.openide.util.Exceptions;

/**
 * Extracted from Tomasz Slota's PHPNewLineIndenter.
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class IndentationCounter {
    private static final Collection<PHPTokenId> CONTROL_STATEMENT_TOKENS = Arrays.asList(
            PHPTokenId.PHP_DO, PHPTokenId.PHP_WHILE, PHPTokenId.PHP_FOR,
            PHPTokenId.PHP_FOREACH, PHPTokenId.PHP_IF, PHPTokenId.PHP_ELSE);
    private Collection<ScopeDelimiter> scopeDelimiters;
    private final BaseDocument doc;
    private final int indentSize;
    private final int continuationSize;
    private final int itemsArrayDeclararionSize;

    public IndentationCounter(BaseDocument doc) {
        this.doc = doc;
        indentSize = CodeStyle.get(doc).getIndentSize();
        continuationSize = CodeStyle.get(doc).getContinuationIndentSize();
        itemsArrayDeclararionSize = CodeStyle.get(doc).getItemsInArrayDeclarationIndentSize();
        int initialIndentSize = CodeStyle.get(doc).getInitialIndent();
        scopeDelimiters = Arrays.asList(
                new ScopeDelimiter(PHPTokenId.PHP_SEMICOLON, 0),
                new ScopeDelimiter(PHPTokenId.PHP_OPENTAG, initialIndentSize),
                new ScopeDelimiter(PHPTokenId.PHP_CURLY_CLOSE, 0),
                new ScopeDelimiter(PHPTokenId.PHP_CURLY_OPEN, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_CASE, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_IF, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_ELSE, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_ELSEIF, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_WHILE, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_DO, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_FOR, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_FOREACH, indentSize),
                new ScopeDelimiter(PHPTokenId.PHP_DEFAULT, indentSize));
    }

    public Indentation count(int caretOffset) {
        Indentation result = Indentation.NONE;
        doc.readLock();
        try {
            result = countUnderReadLock(caretOffset);
        } finally {
            doc.readUnlock();
        }
        return result;
    }

    private Indentation countUnderReadLock(int caretOffset) {
        int newIndent = 0;
        try {
            boolean insideString = false;
            TokenSequence<? extends PHPTokenId> ts = LexUtilities.getPHPTokenSequence(doc, caretOffset);
            int caretLineStart = Utilities.getRowStart(doc, Utilities.getRowStart(doc, caretOffset) - 1);
            if (ts != null) {
                ts.move(caretOffset);
                ts.moveNext();

                boolean indentStartComment = false;

                boolean movePrevious = false;
                if (ts.token() == null) {
                    return Indentation.NONE;
                }
                if (ts.token().id() == PHPTokenId.PHP_OPENTAG) {
                    int neOffset = Utilities.getFirstNonWhiteBwd(doc, caretOffset - 1);
                    Indentation result = Indentation.NONE;
                    if (neOffset != -1) {
                        result = new IndentationImpl(Utilities.getRowIndent(doc, neOffset) + indentSize);
                    }
                    return result;
                }
                if (ts.token().id() == PHPTokenId.WHITESPACE && ts.moveNext()) {
                    movePrevious = true;
                }
                if (ts.token().id() == PHPTokenId.PHP_COMMENT
                        || ts.token().id() == PHPTokenId.PHP_LINE_COMMENT
                        || ts.token().id() == PHPTokenId.PHP_COMMENT_START
                        || ts.token().id() == PHPTokenId.PHP_COMMENT_END) {

                    if (ts.token().id() == PHPTokenId.PHP_COMMENT_START && ts.offset() >= caretOffset) {
                        indentStartComment = true;
                    } else {
                        if (!movePrevious) {
                            // don't indent comment - issue #173979
                            return Indentation.NONE;
                        } else {
                            if (ts.token().id() == PHPTokenId.PHP_LINE_COMMENT) {
                                ts.movePrevious();
                                CharSequence whitespace = ts.token().text();
                                if (ts.movePrevious() && ts.token().id() == PHPTokenId.PHP_LINE_COMMENT) {
                                    int index = 0;
                                    while (index < whitespace.length() && whitespace.charAt(index) != '\n') {
                                        index++;
                                    }
                                    if (index == whitespace.length()) {
                                        // don't indent if the line commnet continue
                                        // the last new line belongs to the line comment
                                        return Indentation.NONE;
                                    }
                                }
                                ts.moveNext();
                                movePrevious = false;
                            }
                        }
                    }
                }
                if (movePrevious) {
                    ts.movePrevious();
                }
                if ((ts.token().id() == PHPTokenId.PHP_ENCAPSED_AND_WHITESPACE || ts.token().id() == PHPTokenId.PHP_CONSTANT_ENCAPSED_STRING) && caretOffset > ts.offset()) {

                    int stringLineStart = Utilities.getRowStart(doc, ts.offset());

                    if (stringLineStart >= caretLineStart) {
                        // string starts on the same line:
                        // current line indent + continuation size
                        newIndent = Utilities.getRowIndent(doc, stringLineStart) + indentSize;
                    } else {
                        // string starts before:
                        // repeat indent from the previous line
                        newIndent = Utilities.getRowIndent(doc, caretLineStart);
                    }

                    insideString = true;
                }

                int bracketBalance = 0;
                int squaredBalance = 0;
                PHPTokenId previousTokenId = ts.token().id();
                while (!insideString && ts.movePrevious()) {
                    Token token = ts.token();
                    ScopeDelimiter delimiter = getScopeDelimiter(token);
                    int anchor = ts.offset();
                    int shiftAtAncor = 0;

                    if (delimiter != null) {
                        if (delimiter.tokenId == PHPTokenId.PHP_SEMICOLON) {
                            int casePosition = breakProceededByCase(ts); // is after break in case statement?
                            if (casePosition > -1) {
                                newIndent = Utilities.getRowIndent(doc, anchor);
                                if (Utilities.getRowStart(doc, casePosition) != caretLineStart) {
                                    // check that case is not on the same line, where enter was pressed
                                    newIndent -= indentSize;
                                }
                                break;
                            }

                            CodeB4BreakData codeB4BreakData = processCodeBeforeBreak(ts, indentStartComment);
                            anchor = codeB4BreakData.expressionStartOffset;
                            shiftAtAncor = codeB4BreakData.indentDelta;

                            if (codeB4BreakData.processedByControlStmt) {
                                newIndent = Utilities.getRowIndent(doc, anchor) - indentSize;
                            } else {
                                newIndent = Utilities.getRowIndent(doc, anchor) + delimiter.indentDelta + shiftAtAncor;
                            }
                            break;
                        } else if (delimiter.tokenId == PHPTokenId.PHP_CURLY_OPEN && ts.movePrevious()) {
                            int startExpression = LexUtilities.findStartTokenOfExpression(ts);
                            newIndent = Utilities.getRowIndent(doc, startExpression) + indentSize;
                            break;
                        }
                        if (anchor >= 0) {
                            newIndent = Utilities.getRowIndent(doc, anchor) + delimiter.indentDelta + shiftAtAncor;
                        }
                        break;
                    } else {
                        if (ts.token().id() == PHPTokenId.PHP_TOKEN) {
                            char ch = ts.token().text().charAt(0);
                            boolean continualIndent = false;
                            boolean indent = false;
                            switch (ch) {
                                case ')':
                                    bracketBalance++;
                                    break;
                                case '(':
                                    if (bracketBalance == 0) {
                                        continualIndent = true;
                                    }
                                    bracketBalance--;
                                    break;
                                case ']':
                                    squaredBalance++;
                                    break;
                                case '[':
                                    if (squaredBalance == 0) {
                                        continualIndent = true;
                                    }
                                    squaredBalance--;
                                    break;
                                case ',':
                                    continualIndent = true;
                                    break;
                                case '.':
                                    continualIndent = true;
                                    break;
                                case ':':
                                    if (isInTernaryOperatorStatement(ts)) {
                                        continualIndent = true;
                                    } else {
                                        indent = true;
                                    }
                                    break;
                                case '=':
                                    continualIndent = true;
                                    break;
                                default:
                                    //no-op
                            }
                            if (continualIndent || indent) {
                                ts.move(caretOffset);
                                ts.movePrevious();
                                int startExpression = LexUtilities.findStartTokenOfExpression(ts);
                                if (startExpression != -1) {
                                    if (continualIndent) {
                                        int offsetArrayDeclaration = offsetArrayDeclaration(startExpression, ts);
                                        if (offsetArrayDeclaration > -1) {
                                            newIndent = Utilities.getRowIndent(doc, offsetArrayDeclaration) + itemsArrayDeclararionSize;
                                        } else {
                                            newIndent = Utilities.getRowIndent(doc, startExpression) + continuationSize;
                                        }
                                    }
                                    if (indent) {
                                        newIndent = Utilities.getRowIndent(doc, startExpression) + indentSize;
                                    }
                                }
                                break;
                            }
                        } else if ((previousTokenId == PHPTokenId.PHP_OBJECT_OPERATOR
                                || ts.token().id() == PHPTokenId.PHP_OBJECT_OPERATOR
                                || ts.token().id() == PHPTokenId.PHP_PAAMAYIM_NEKUDOTAYIM) && bracketBalance <= 0) {
                            int startExpression = LexUtilities.findStartTokenOfExpression(ts);
                            if (startExpression != -1) {
                                int rememberOffset = ts.offset();
                                ts.move(startExpression);
                                ts.moveNext();
                                if (ts.token().id() != PHPTokenId.PHP_IF
                                        && ts.token().id() != PHPTokenId.PHP_WHILE
                                        && ts.token().id() != PHPTokenId.PHP_FOR
                                        && ts.token().id() != PHPTokenId.PHP_FOREACH) {
                                    newIndent = Utilities.getRowIndent(doc, startExpression) + continuationSize;
                                    break;
                                } else {
                                    ts.move(rememberOffset);
                                    ts.moveNext();
                                }

                            }
                        } else if (ts.token().id() == PHPTokenId.PHP_PUBLIC || ts.token().id() == PHPTokenId.PHP_PROTECTED
                                || ts.token().id() == PHPTokenId.PHP_PRIVATE || (ts.token().id() == PHPTokenId.PHP_VARIABLE && bracketBalance <= 0)) {
                            int startExpression = LexUtilities.findStartTokenOfExpression(ts);
                            if (startExpression != -1) {
                                newIndent = Utilities.getRowIndent(doc, startExpression) + continuationSize;
                                break;
                            }
                        }
                    }
                    previousTokenId = ts.token().id();
                }

                if (newIndent < 0) {
                    newIndent = 0;
                }
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new IndentationImpl(newIndent);
    }

    private static boolean isInTernaryOperatorStatement(TokenSequence<? extends PHPTokenId> ts) {
        boolean result = false;
        int originalOffset = ts.offset();
        ts.movePrevious();
        Token<? extends PHPTokenId> previousToken = LexUtilities.findPreviousToken(ts, Arrays.asList(PHPTokenId.PHP_TOKEN));
        if (previousToken != null && previousToken.id() == PHPTokenId.PHP_TOKEN && previousToken.text().charAt(0) == '?') {
            result = true;
        }
        ts.move(originalOffset);
        ts.moveNext();
        return result;
    }

    private CodeB4BreakData processCodeBeforeBreak(TokenSequence ts, boolean indentComment) {
        CodeB4BreakData retunValue = new CodeB4BreakData();
        int origOffset = ts.offset();
        Token token = ts.token();

        if (token.id() == PHPTokenId.PHP_SEMICOLON && ts.movePrevious()) {
            retunValue.expressionStartOffset = LexUtilities.findStartTokenOfExpression(ts);
            ts.move(retunValue.expressionStartOffset);
            ts.moveNext();
            retunValue.indentDelta = ts.token().id() == PHPTokenId.PHP_CASE || ts.token().id() == PHPTokenId.PHP_DEFAULT
                    ? indentSize : 0;
            retunValue.processedByControlStmt = false;
            ts.move(origOffset);
            ts.moveNext();
            return retunValue;
        }
        while (ts.movePrevious()) {
            token = ts.token();
            ScopeDelimiter delimiter = getScopeDelimiter(token);
            if (delimiter != null) {
                retunValue.expressionStartOffset = ts.offset();
                retunValue.indentDelta = delimiter.indentDelta;
                if (CONTROL_STATEMENT_TOKENS.contains(delimiter.tokenId)) {
                    retunValue.indentDelta = 0;
                }
                break;
            } else {
                if (indentComment && token.id() == PHPTokenId.WHITESPACE
                        && token.text().toString().indexOf('\n') != -1
                        && ts.moveNext()) {
                    retunValue.expressionStartOffset = ts.offset();
                    retunValue.indentDelta = 0;
                    break;
                }
            }
        }

        if (token.id() == PHPTokenId.PHP_OPENTAG && ts.moveNext()) {
            // we are at the begining of the php blog
            LexUtilities.findNext(ts, Arrays.asList(
                    PHPTokenId.WHITESPACE,
                    PHPTokenId.PHPDOC_COMMENT, PHPTokenId.PHPDOC_COMMENT_END, PHPTokenId.PHPDOC_COMMENT_START,
                    PHPTokenId.PHP_COMMENT, PHPTokenId.PHP_COMMENT_END, PHPTokenId.PHP_COMMENT_START,
                    PHPTokenId.PHP_LINE_COMMENT));
            retunValue.expressionStartOffset = ts.offset();
            retunValue.indentDelta = 0;
        }
        ts.move(origOffset);
        ts.moveNext();
        return retunValue;
    }

    /**
     * Returns of set of the array declaration, where is the exexpression.
     *
     * @param startExpression
     * @param ts
     * @return
     */
    private static int offsetArrayDeclaration(int startExpression, TokenSequence ts) {
        int result = -1;
        int origOffset = ts.offset();
        Token token;
        int balance = 0;
        int squaredBalance = 0;
        do {
            token = ts.token();
            if (token.id() == PHPTokenId.PHP_TOKEN) {
                switch (token.text().charAt(0)) {
                    case ')':
                        balance--;
                        break;
                    case '(':
                        balance++;
                        break;
                    case ']':
                        squaredBalance--;
                        break;
                    case '[':
                        squaredBalance++;
                        break;
                    default:
                        //no-op
                }
            }
        } while (ts.offset() > startExpression
                && !(token.id() == PHPTokenId.PHP_ARRAY && balance == 1)
                && !(token.id() == PHPTokenId.PHP_TOKEN && squaredBalance == 1)
                && ts.movePrevious());

        if ((token.id() == PHPTokenId.PHP_ARRAY && balance == 1)
                || (token.id() == PHPTokenId.PHP_TOKEN && squaredBalance == 1)) {
            result = ts.offset();
        }
        ts.move(origOffset);
        ts.moveNext();
        return result;
    }

    /**
     *
     * @param ts
     * @return -1 if is not by case or offset of the case keyword
     */
    private int breakProceededByCase(TokenSequence<? extends PHPTokenId> ts) {
        int retunValue = -1;
        int origOffset = ts.offset();

        if (ts.movePrevious()) {
            if (semicolonProceededByBreak(ts)) {
                while (ts.movePrevious()) {
                    PHPTokenId tid = ts.token().id();

                    if (tid == PHPTokenId.PHP_CASE) {
                        retunValue = ts.offset();
                        break;
                    } else if (CONTROL_STATEMENT_TOKENS.contains(tid)) {
                        break;
                    }
                }
            }
        }

        ts.move(origOffset);
        ts.moveNext();

        return retunValue;
    }

    private boolean semicolonProceededByBreak(TokenSequence ts) {
        boolean retunValue = false;

        if (ts.token().id() == PHPTokenId.PHP_BREAK) {
            retunValue = true;
        } else if (ts.token().id() == PHPTokenId.PHP_NUMBER) {
            int origOffset = ts.offset();

            if (ts.movePrevious()) {
                if (ts.token().id() == PHPTokenId.WHITESPACE) {
                    if (ts.movePrevious()) {
                        if (ts.token().id() == PHPTokenId.PHP_BREAK) {
                            retunValue = true;
                        }
                    }
                }
            }

            ts.move(origOffset);
            ts.moveNext();
        }

        return retunValue;
    }

    private ScopeDelimiter getScopeDelimiter(Token token) {
        // TODO: more efficient impl
        for (ScopeDelimiter scopeDelimiter : scopeDelimiters) {
            if (scopeDelimiter.matches(token)) {
                return scopeDelimiter;
            }
        }
        return null;
    }

    private static class CodeB4BreakData {
        int expressionStartOffset;
        boolean processedByControlStmt;
        int indentDelta;
    }

    private static class ScopeDelimiter {
        private PHPTokenId tokenId;
        private String tokenContent;
        private int indentDelta;

        public ScopeDelimiter(PHPTokenId tokenId, int indentDelta) {
            this(tokenId, null, indentDelta);
        }

        public ScopeDelimiter(PHPTokenId tokenId, String tokenContent, int indentDelta) {
            this.tokenId = tokenId;
            this.tokenContent = tokenContent;
            this.indentDelta = indentDelta;
        }

        public boolean matches(Token token) {
            if (tokenId != token.id()) {
                return false;
            }
            if (tokenContent != null
                    && TokenUtilities.equals(token.text(), tokenContent)) {
                return false;
            }
            return true;
        }
    }

    public interface Indentation {

        Indentation NONE = new Indentation() {

            @Override
            public int getIndentation() {
                return 0;
            }

            @Override
            public void modify(Context context) {
            }

        };

        int getIndentation();
        void modify(Context context);

    }

    private static final class IndentationImpl implements Indentation {
        private final int indentation;

        public IndentationImpl(int indentation) {
            this.indentation = indentation;
        }

        @Override
        public int getIndentation() {
            return indentation;
        }

        @Override
        public void modify(final Context context) {
            assert  context != null;
            context.document().render(new Runnable() {

                @Override
                public void run() {
                    modifyUnderWriteLock(context);
                }
            });
        }

        private void modifyUnderWriteLock(Context context) {
            try {
                context.modifyIndent(Utilities.getRowStart((BaseDocument) context.document(), context.caretOffset()), indentation);
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

    }

}

