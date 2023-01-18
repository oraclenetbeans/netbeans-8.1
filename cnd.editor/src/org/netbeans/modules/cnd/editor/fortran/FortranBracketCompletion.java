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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.cnd.editor.fortran;

import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.cnd.api.lexer.CndLexerUtilities;
import org.netbeans.cnd.api.lexer.FortranTokenId;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.modules.cnd.editor.fortran.options.FortranCodeStyle;
import org.netbeans.modules.cnd.utils.MIMENames;

/**
 * This static class groups the whole aspect of bracket
 * completion. It is defined to clearly separate the functionality
 * and keep actions clean.
 * The methods of the class are called from different actions as
 * KeyTyped, DeletePreviousChar.
 */
public enum FortranBracketCompletion {
    INSTANCE;

    /**
     * A hook method called after a character was inserted into the
     * document. The function checks for special characters for
     * completion ()[]'"{} and other conditions and optionally performs
     * changes to the doc and or caret (completes braces, moves caret,
     * etc.)
     * @param doc the document where the change occurred
     * @param dotPos position of the character insertion
     * @param caret caret
     * @param ch the character that was inserted
     * @throws BadLocationException if dotPos is not correct
     */
    void charInserted(BaseDocument doc,
            int dotPos,
            Caret caret,
            char ch) throws BadLocationException {
        if (!completionSettingEnabled()) {
            return;
        }
        Token<FortranTokenId> token = getToken(doc, dotPos);
        if (token == null) {
            return;
        }
        if (ch == ')' || ch == '(') {
            switch (token.id()) {
                case RPAREN:
                    skipClosingBracket(doc, caret, ch);
                    break;
                case LPAREN:
                    completeOpeningBracket(doc, dotPos, caret, ch);
                    break;
            }
        } else if (ch == '\"' || ch == '\'') {
            completeQuote(doc, dotPos, caret, ch);
        }
    }

    private Token<FortranTokenId> getToken(BaseDocument doc, int dotPos){
        FortranCodeStyle.get(doc).setupLexerAttributes(doc);
        TokenSequence<FortranTokenId> ts = CndLexerUtilities.getFortranTokenSequence(doc, dotPos);
        if (ts == null) {
            return null;
        }
        ts.move(dotPos);
        if (!ts.moveNext()) {
            return null;
        }
        return ts.token();
    }

    private TokenSequence<FortranTokenId> getTokenSequence(BaseDocument doc, int dotPos){
        FortranCodeStyle.get(doc).setupLexerAttributes(doc);
        TokenSequence<FortranTokenId> ts = CndLexerUtilities.getFortranTokenSequence(doc, dotPos);
        if (ts == null) {
            return null;
        }
        ts.move(dotPos);
        if (!ts.moveNext()) {
            return null;
        }
        return ts;
    }


    /**
     * A hook to be called after closing bracket ) or ] was inserted into
     * the document. The method checks if the bracket should stay there
     * or be removed and some existing bracket just skipped.
     *
     * @param doc the document
     * @param dotPos position of the inserted bracket
     * @param caret caret
     * @param theBracket the bracket character ']' or ')'
     */
    private void skipClosingBracket(BaseDocument doc, Caret caret, char theBracket)
            throws BadLocationException {
        int caretOffset = caret.getDot();
        if (isSkipClosingBracket(doc, caretOffset, FortranTokenId.RPAREN)) {
            doc.remove(caretOffset - 1, 1);
            caret.setDot(caretOffset); // skip closing bracket
        }
    }

    /**
     * Check whether the typed bracket should stay in the document
     * or be removed.
     * <br>
     * This method is called by <code>skipClosingBracket()</code>.
     *
     * @param doc document into which typing was done.
     * @param caretOffset
     */
    private boolean isSkipClosingBracket(BaseDocument doc, int caretOffset, FortranTokenId bracketId)
            throws BadLocationException {
        // First check whether the caret is not after the last char in the document
        // because no bracket would follow then so it could not be skipped.
        if (caretOffset == doc.getLength()) {
            return false; // no skip in this case
        }

        boolean skipClosingBracket = false; // by default do not remove
        // Examine token at the caret offset
        TokenSequence<FortranTokenId> ts = getTokenSequence(doc, caretOffset);
        if (ts == null) {
            return false;
        }
        // Check whether character follows the bracket is the same bracket
        if (ts.token().id() == bracketId) {
            FortranTokenId leftBracketId = FortranTokenId.LPAREN;

            // Skip all the brackets of the same type that follow the last one
            int lastRBracketIndex = ts.index();
            while (ts.moveNext() && ts.token().id() == bracketId) {
                lastRBracketIndex = ts.index();
            }
            // token var points to the last bracket in a group of two or more right brackets
            // Attempt to find the left matching bracket for it
            // Search would stop on an extra opening left brace if found
            int braceBalance = 0; // balance of '{' and '}'
            int bracketBalance = -1; // balance of the brackets or parenthesis
            boolean finished = false;
            while (!finished && ts.movePrevious()) {
                FortranTokenId id = ts.token().id();
                switch (id) {
                    case LPAREN:
                        if (id == bracketId) {
                            bracketBalance++;
                            if (bracketBalance == 0) {
                                if (braceBalance != 0) {
                                    // Here the bracket is matched but it is located
                                    // inside an unclosed brace block
                                    // e.g. ... ->( } a()|)
                                    // which is in fact illegal but it's a question
                                    // of what's best to do in this case.
                                    // We chose to leave the typed bracket
                                    // by setting bracketBalance to 1.
                                    // It can be revised in the future.
                                    bracketBalance = 1;
                                }
                                finished = true;
                            }
                        }
                        break;

                    case RPAREN:
                        if (id == bracketId) {
                            bracketBalance--;
                        }
                        break;
                }
            // done regardless of finished flag state
            }

            if (bracketBalance != 0) { // not found matching bracket
                // Remove the typed bracket as it's unmatched
                skipClosingBracket = true;

            } else { // the bracket is matched
                // Now check whether the bracket would be matched
                // when the closing bracket would be removed
                // i.e. starting from the original lastRBracket token
                // and search for the same bracket to the right in the text
                // The search would stop on an extra right brace if found
                braceBalance = 0;
                bracketBalance = 1; // simulate one extra left bracket
                ts.moveIndex(lastRBracketIndex);
                ts.moveNext();
//                token = lastRBracket.getNext();
                ts.moveNext(); // ???
                finished = false;
                while (!finished && ts.movePrevious()) {
                    FortranTokenId id = ts.token().id();
                    switch (id) {
                        case LPAREN:
                            if (id == leftBracketId) {
                                bracketBalance++;
                            }
                            break;

                        case RPAREN:
                            if (id == bracketId) {
                                bracketBalance--;
                                if (bracketBalance == 0) {
                                    if (braceBalance != 0) {
                                        // Here the bracket is matched but it is located
                                        // inside an unclosed brace block
                                        // which is in fact illegal but it's a question
                                        // of what's best to do in this case.
                                        // We chose to leave the typed bracket
                                        // by setting bracketBalance to -1.
                                        // It can be revised in the future.
                                        bracketBalance = -1;
                                    }
                                    finished = true;
                                }
                            }
                            break;
                    }
                // done regardless of finished flag state
                }

                // If bracketBalance == 0 the bracket would be matched
                // by the bracket that follows the last right bracket.
                skipClosingBracket = (bracketBalance == 0);
            }
        }
        return skipClosingBracket;
    }

    /**
     * Check for various conditions and possibly add a pairing bracket
     * to the already inserted.
     * @param doc the document
     * @param dotPos position of the opening bracket (already in the doc)
     * @param caret caret
     * @param theBracket the bracket that was inserted
     */
    private void completeOpeningBracket(BaseDocument doc,
            int dotPos,
            Caret caret,
            char theBracket) throws BadLocationException {
        if (isCompletablePosition(doc, dotPos + 1)) {
            String matchinBracket = "" + matching(theBracket);
            doc.insertString(dotPos + 1, matchinBracket, null);
            caret.setDot(dotPos + 1);
        }
    }

    private boolean isEscapeSequence(BaseDocument doc, int dotPos) throws BadLocationException {
        if (dotPos <= 0) {
            return false;
        }
        char previousChar = doc.getChars(dotPos - 1, 1)[0];
        return previousChar == '\\';
    }

    /**
     * Check for conditions and possibly complete an already inserted
     * quote .
     * @param doc the document
     * @param dotPos position of the opening bracket (already in the doc)
     * @param caret caret
     * @param theBracket the character that was inserted
     */
    private void completeQuote(BaseDocument doc, int dotPos, Caret caret,
            char theBracket)
            throws BadLocationException {
        if (isEscapeSequence(doc, dotPos)) {
            return;
        }
        FortranTokenId[] tokenIds = new FortranTokenId[]{FortranTokenId.STRING_LITERAL};
        if ((posWithinQuotes(doc, dotPos + 1, theBracket, tokenIds) && isCompletablePosition(doc, dotPos + 1)) &&
                (isUnclosedStringAtLineEnd(doc, dotPos + 1, tokenIds) &&
                ((doc.getLength() == dotPos + 1) ||
                (doc.getLength() != dotPos + 1 && doc.getChars(dotPos + 1, 1)[0] != theBracket)))) {
            doc.insertString(dotPos + 1, "" + theBracket, null);
            caret.setDot(dotPos + 1);
        } else {
            char[] charss = doc.getChars(dotPos + 1, 1);
            // System.out.println("NOT Within string, " + new String(charss));
            if (charss != null && charss[0] == theBracket) {
                doc.remove(dotPos + 1, 1);
            }
        }
    }

    /**
     * Checks whether dotPos is a position at which bracket and quote
     * completion is performed. Brackets and quotes are not completed
     * everywhere but just at suitable places .
     * @param doc the document
     * @param dotPos position to be tested
     */
    private boolean isCompletablePosition(BaseDocument doc, int dotPos)
            throws BadLocationException {
        if (dotPos == doc.getLength()) {// there's no other character to test
            return true;
        } else {
            // test that we are in front of ) , " or '
            char chr = doc.getChars(dotPos, 1)[0];
            return (chr == ')' ||
                    chr == ',' ||
                    chr == '\"' ||
                    chr == '\'' ||
                    chr == ' ' ||
                    chr == '-' ||
                    chr == '+' ||
                    chr == '|' ||
                    chr == '&' ||
                    chr == ']' ||
                    chr == '}' ||
                    chr == '\n' ||
                    chr == '\t' ||
                    chr == ';');
        }
    }

    /**
     * Returns true if bracket completion is enabled in options.
     */
    private boolean completionSettingEnabled() {
        Preferences prefs = MimeLookup.getLookup(MIMENames.FORTRAN_MIME_TYPE).lookup(Preferences.class);
        return prefs.getBoolean(SimpleValueNames.COMPLETION_PAIR_CHARACTERS, true);
    }

    /**
     * Returns for an opening bracket or quote the appropriate closing
     * character.
     */
    private char matching(char theBracket) {
        switch (theBracket) {
            case '(':
                return ')';
            case '\'':
                return '\''; // NOI18N
            default:
                return ' ';
        }
    }

    /**
     * Generalized posWithingString to any token and delimiting
     * character. It works for tokens are delimited by *quote* and
     * extend up to the other *quote* or whitespace in case of an
     * incomplete token.
     * @param doc the document
     * @param dotPos position to be tested
     */
    private boolean posWithinQuotes(BaseDocument doc, int dotPos, char quote, FortranTokenId[] tokenIDs) {
        TokenSequence<FortranTokenId> cppTS = getTokenSequence(doc, dotPos);
        if (cppTS != null && matchIDs(cppTS.token().id(), tokenIDs)) {
            return (dotPos - cppTS.offset() == 1 || DocumentUtilities.getText(doc).charAt(dotPos - 1) != quote);
        }
        return false;
    }

    private boolean isUnclosedStringAtLineEnd(BaseDocument doc, int dotPos, FortranTokenId[] tokenIDs) {
        int lastNonWhiteOffset;
        try {
            lastNonWhiteOffset = Utilities.getRowLastNonWhite(doc, dotPos);
        } catch (BadLocationException e) {
            return false;
        }
        TokenSequence<FortranTokenId> cppTS = getTokenSequence(doc, lastNonWhiteOffset);
        if (cppTS != null) {
            return matchIDs(cppTS.token().id(), tokenIDs);
        }
        return false;
    }

    private boolean matchIDs(FortranTokenId toCheck, FortranTokenId[] checkWith) {
        for (int i = checkWith.length - 1; i >= 0; i--) {
            if (toCheck == checkWith[i]) {
                return true;
            }
        }
        return false;
    }
}
