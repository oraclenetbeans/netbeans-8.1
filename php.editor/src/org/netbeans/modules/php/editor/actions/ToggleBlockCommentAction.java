/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.actions;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Action;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseAction;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.modules.csl.api.CslActions;
import static org.netbeans.modules.php.api.util.FileUtils.PHP_MIME_TYPE;
import org.netbeans.modules.php.editor.csl.PHPLanguage;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.openide.util.Exceptions;

/**
 *
 * @author Petr Pisl
 */
public class ToggleBlockCommentAction extends BaseAction {

    @EditorActionRegistration(name = ExtKit.toggleCommentAction, mimeType = PHP_MIME_TYPE)
    public static ToggleBlockCommentAction create(Map<String, ?> attrs) {
        return new ToggleBlockCommentAction(attrs);
    }

    static final long serialVersionUID = -1L;
    private static final String FORCE_COMMENT = "force-comment"; //NOI18N
    private static final String FORCE_UNCOMMENT = "force-uncomment"; //NOI18N

    public ToggleBlockCommentAction(Map<String, ?> attrs) {
        super(null);
        if (attrs != null) {
            String actionName = (String) attrs.get(Action.NAME);
            if (actionName == null) {
                throw new IllegalArgumentException("Null Action.NAME attribute for action " + this.getClass()); //NOI18N
            }
            putValue(Action.NAME, actionName);
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt, JTextComponent target) {
        final AtomicBoolean processedHere = new AtomicBoolean(false);
        if (target != null) {
            if (!target.isEditable() || !target.isEnabled() || !(target.getDocument() instanceof BaseDocument)) {
                target.getToolkit().beep();
                return;
            }
            final int caretOffset = Utilities.isSelectionShowing(target) ? target.getSelectionStart() : target.getCaretPosition();
            final BaseDocument doc = (BaseDocument) target.getDocument();
            doc.runAtomic(new Runnable() {

                @Override
                public void run() {
                    performCustomAction(doc, caretOffset, processedHere);
                }
            });
            if (!processedHere.get()) {
                performDefaultAction(evt, target);
            }
        }
    }

    private void performCustomAction(BaseDocument doc, int caretOffset, AtomicBoolean processedHere) {
        TokenSequence<PHPTokenId> ts = LexUtilities.getPHPTokenSequence(doc, caretOffset);
        if (ts != null) {
            ts.move(caretOffset);
            ts.moveNext();
            if (isAroundPhpComment(ts, caretOffset)) {
                processedHere.set(true);
            } else if (ts.token().id() != PHPTokenId.T_INLINE_HTML) {
                boolean newLineSomewhereBeforeCaretOffset = false;
                if (isNewLineBeforeCaretOffset(ts, caretOffset)) {
                    newLineSomewhereBeforeCaretOffset = true;
                }
                while (!newLineSomewhereBeforeCaretOffset && ts.movePrevious() && ts.token().id() != PHPTokenId.PHP_OPENTAG) {
                    if (isNewLineBeforeCaretOffset(ts, caretOffset)) {
                        newLineSomewhereBeforeCaretOffset = true;
                    }
                }
                if (!newLineSomewhereBeforeCaretOffset && ts.token().id() == PHPTokenId.PHP_OPENTAG) {
                    processedHere.set(true);
                    int possibleChangeOffset = ts.offset() + ts.token().length();
                    int possibleWhitespaceLength = 0;
                    boolean possibleLineComment = false;
                    if (ts.moveNext() && ts.token().id() == PHPTokenId.PHP_LINE_COMMENT) {
                        possibleLineComment = true;
                    } else if (ts.token().id() == PHPTokenId.WHITESPACE) {
                        possibleWhitespaceLength = ts.token().length();
                        if (ts.moveNext() && ts.token().id() == PHPTokenId.PHP_LINE_COMMENT) {
                            possibleLineComment = true;
                        }
                    }
                    final boolean lineComment = possibleLineComment;
                    final int changeOffset = lineComment ? possibleChangeOffset + possibleWhitespaceLength : possibleChangeOffset;
                    final int length = lineComment ? ts.offset() + ts.token().length() + countForgoingWhitespaces(ts) - changeOffset : 0;
                    try {
                        if (!lineComment) {
                            if (forceDirection(true)) {
                                doc.insertString(changeOffset, " " + PHPLanguage.LINE_COMMENT_PREFIX, null);
                            }
                        } else {
                            if (forceDirection(false)) {
                                doc.remove(changeOffset, length);
                            }
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }
    }

    private int findLastNewLineBeforeOffset(String text, int offset) {
        int result = -1;
        if (offset >= 0 && offset < text.length()) {
            String textUntilOffset = text.substring(0, offset);
            result = textUntilOffset.lastIndexOf("\n"); //NOI18N
        }
        return result;
    }

    private void performDefaultAction(ActionEvent evt, JTextComponent target) {
        BaseAction action = (BaseAction) CslActions.createToggleBlockCommentAction();
        if (getValue(FORCE_COMMENT) != null) {
            action.putValue(FORCE_COMMENT, getValue(FORCE_COMMENT));
        }
        if (getValue(FORCE_UNCOMMENT) != null) {
            action.putValue(FORCE_UNCOMMENT, getValue(FORCE_UNCOMMENT));
        }
        action.actionPerformed(evt, target);
    }

    private boolean isAroundPhpComment(final TokenSequence<PHPTokenId> ts, int caretOffset) {
        Token<PHPTokenId> token = ts.token();
        boolean result = isPhpComment(token.id());
        if (!result && PHPTokenId.WHITESPACE.equals(token.id())
                && findLastNewLineBeforeOffset(token.text().toString(), caretOffset - ts.offset()) == -1 && ts.movePrevious()) { //NOI18N
            result = isPhpComment(ts.token().id());
            ts.moveNext();
        }
        return result;
    }

    private static boolean isPhpComment(final PHPTokenId tokenId) {
        return PHPTokenId.PHP_COMMENT.equals(tokenId) || PHPTokenId.PHP_COMMENT_START.equals(tokenId)
                || PHPTokenId.PHP_COMMENT_END.equals(tokenId) || PHPTokenId.PHPDOC_COMMENT.equals(tokenId)
                || PHPTokenId.PHPDOC_COMMENT_START.equals(tokenId) || PHPTokenId.PHPDOC_COMMENT_END.equals(tokenId);
    }

    private static int countForgoingWhitespaces(final TokenSequence<PHPTokenId> tokenSequence) {
        int result = 0;
        tokenSequence.moveNext();
        String commentedText = tokenSequence.token().text().toString();
        for (int i = 0; i < commentedText.length(); i++) {
            if (Character.isWhitespace(commentedText.charAt(i))) {
                result++;
            } else {
                break;
            }
        }
        tokenSequence.movePrevious();
        return result;
    }

    private static boolean isNewLineBeforeCaretOffset(final TokenSequence<PHPTokenId> ts, final int caretOffset) {
        boolean result = false;
        int indexOfNewLine = ts.token().text().toString().indexOf("\n"); //NOI18N
        if (indexOfNewLine != -1) {
            int absoluteIndexOfNewLine = ts.offset() + indexOfNewLine;
            result = caretOffset > absoluteIndexOfNewLine;
        }
        return result;
    }

    private boolean forceDirection(boolean comment) {
        Object fComment = getValue(FORCE_COMMENT);
        Object fUncomment = getValue(FORCE_UNCOMMENT);

        Object force = comment ? fComment : fUncomment;
        if (force instanceof Boolean) {
            return ((Boolean) force).booleanValue();
        }
        return fComment == null && fUncomment == null;
    }
}
