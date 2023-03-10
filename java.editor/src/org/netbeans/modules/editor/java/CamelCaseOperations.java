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

package org.netbeans.modules.editor.java;

import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import static org.netbeans.modules.editor.java.JavaKit.JAVA_MIME_TYPE;
import org.netbeans.spi.editor.typinghooks.CamelCaseInterceptor;
import org.openide.util.NbPreferences;

/**
 *
 * @author Sandip V. Chitale (Sandip.Chitale@Sun.Com)
 */
/* package */ class CamelCaseOperations {

    static int nextCamelCasePosition(JTextComponent textComponent) throws BadLocationException {
        // get current caret position
        final int offset = textComponent.getCaretPosition();
        final Document doc = textComponent.getDocument();
        final int[] retOffset = new int[1];
        final BadLocationException[] retExc = new BadLocationException[1];
        doc.render(new Runnable() {
            @Override
            public void run() {
                try {
                    retOffset[0] = nextCamelCasePositionImpl(doc, offset);
                } catch (BadLocationException ex) {
                    retExc[0] = ex;
                }
            }
        });
        if (retExc[0] != null) {
            throw retExc[0];
        }
        return retOffset[0];
    }

    static int nextCamelCasePositionImpl(Document doc, int offset) throws BadLocationException {
        TokenHierarchy<Document> th = doc != null ? TokenHierarchy.get(doc) : null;
        List<TokenSequence<?>> embeddedSequences = th != null ? th.embeddedTokenSequences(offset, false) : null;
        TokenSequence<?> seq = embeddedSequences != null ? embeddedSequences.get(embeddedSequences.size() - 1) : null;

        if (seq  != null) seq.move(offset);

        Token t = seq != null && seq.moveNext() ? seq.offsetToken() : null;

        // is this an identifier
        if (t != null && t.id() == JavaTokenId.IDENTIFIER) { // NOI18N
            String image = t.text().toString();
            if (image != null && image.length() > 0) {
                int length = image.length();
                // is caret at the end of the identifier
                if (offset != (t.offset(th) + length)) {
                    int offsetInImage = offset - t.offset(th);
                    int start = offsetInImage + 1;
                    if (Character.isUpperCase(image.charAt(offsetInImage))) {
                        // if starting from a Uppercase char, first skip over follwing upper case chars
                        for (int i = start; i < length; i++) {
                            char charAtI = image.charAt(i);
                            if (!Character.isUpperCase(charAtI)) {
                                break;
                            }
                            start++;
                        }
                    }
                    for (int i = start; i < length; i++) {
                        char charAtI = image.charAt(i);
                        if (Character.isUpperCase(charAtI)) {
                            // return offset of next uppercase char in the identifier
                            return t.offset(th) + i;
                        }
                    }
                }
                return t.offset(th) + image.length();
            }
        }

        // not an identifier - simply return next word offset
        return Utilities.getNextWord((BaseDocument)doc, offset);
    }

    static int previousCamelCasePosition(JTextComponent textComponent) throws BadLocationException {
        // get current caret position
        final int offset = textComponent.getCaretPosition();
        final Document doc = textComponent.getDocument();
        final int[] retOffset = new int[1];
        final BadLocationException[] retExc = new BadLocationException[1];
        doc.render(new Runnable() {
            @Override
            public void run() {
                try {
                    retOffset[0] = previousCamelCasePositionImpl(doc, offset);
                } catch (BadLocationException ex) {
                    retExc[0] = ex;
                }
            }
        });
        if (retExc[0] != null) {
            throw retExc[0];
        }
        return retOffset[0];
    }

    static int previousCamelCasePositionImpl(Document doc, int offset) throws BadLocationException {
        TokenHierarchy<Document> th = doc != null ? TokenHierarchy.get(doc) : null;
        List<TokenSequence<?>> embeddedSequences = th != null ? th.embeddedTokenSequences(offset, false) : null;
        TokenSequence<?> seq = embeddedSequences != null && embeddedSequences.size() > 0 ? embeddedSequences.get(embeddedSequences.size() - 1) : null;

        if (seq  != null) seq.move(offset);

        Token t = seq != null && seq.moveNext() ? seq.offsetToken() : null;

        // is this an identifier
        if (t != null) {
            if (t.offset(th) == offset) {
                t = seq.movePrevious() ? seq.offsetToken() : null;
            }
            if (t != null && t.id() == JavaTokenId.IDENTIFIER) { // NOI18N
                String image = t.text().toString();
                if (image != null && image.length() > 0) {
                    int length = image.length();
                    int offsetInImage = offset - 1 - t.offset(th);
                    if (Character.isUpperCase(image.charAt(offsetInImage))) {
                        for (int i = offsetInImage - 1; i >= 0; i--) {
                            char charAtI = image.charAt(i);
                            if (!Character.isUpperCase(charAtI)) {
                                // return offset of previous uppercase char in the identifier
                                return t.offset(th) + i + 1;
                            }
                        }
                        return t.offset(th);
                    } else {
                        for (int i = offsetInImage - 1; i >= 0; i--) {
                            char charAtI = image.charAt(i);
                            if (Character.isUpperCase(charAtI)) {
                                // now skip over previous uppercase chars in the identifier
                                for (int j = i; j >= 0; j--) {
                                    char charAtJ = image.charAt(j);
                                    if (!Character.isUpperCase(charAtJ)) {
                                        // return offset of previous uppercase char in the identifier
                                        return t.offset(th) + j + 1;
                                    }
                                }
                                return t.offset(th);
                            }
                        }
                    }
                    return t.offset(th);
                }
            } else if (t != null && t.id() == JavaTokenId.WHITESPACE) { // NOI18N
                Token whitespaceToken = t;
                while (whitespaceToken != null && whitespaceToken.id() == JavaTokenId.WHITESPACE) {
                    int wsOffset = whitespaceToken.offset(th);
                    if (wsOffset == 0) {
                        //#145250: at the very beginning of a file
                        return 0;
                    }
                    whitespaceToken = seq.movePrevious() ? seq.offsetToken() : null;
                }
                if (whitespaceToken != null) {
                    return whitespaceToken.offset(th) + whitespaceToken.length();
                }
            }
        }

        // not an identifier - simply return previous word offset
        return Utilities.getPreviousWord((BaseDocument)doc, offset);
    }

    public static class JavaCamelCaseInterceptor implements CamelCaseInterceptor {

        private boolean isUsingCamelCase() {
            return NbPreferences.root().getBoolean("useCamelCaseStyleNavigation", true); // NOI18N
        }

        @Override
        public boolean beforeChange(MutableContext context) throws BadLocationException {
            return false;
        }

        @Override
        public void change(MutableContext context) throws BadLocationException {
            if (isUsingCamelCase()) {
                if (context.isBackward()) {
                    context.setNextWordOffset(CamelCaseOperations.previousCamelCasePosition(context.getComponent()));
                } else {
                    context.setNextWordOffset(CamelCaseOperations.nextCamelCasePosition(context.getComponent()));
                }
            }
        }

        @Override
        public void afterChange(MutableContext context) throws BadLocationException {
        }

        @Override
        public void cancelled(MutableContext context) {
        }

        @MimeRegistrations({
            @MimeRegistration(mimeType = JAVA_MIME_TYPE, service = CamelCaseInterceptor.Factory.class),
            @MimeRegistration(mimeType = "text/x-javadoc", service = CamelCaseInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-string", service = CamelCaseInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-character", service = CamelCaseInterceptor.Factory.class) //NOI18N
        })
        @MimeRegistration(mimeType = JAVA_MIME_TYPE, service = CamelCaseInterceptor.Factory.class)
        public static class JavaFactory implements CamelCaseInterceptor.Factory {

            @Override
            public CamelCaseInterceptor createCamelCaseInterceptor(MimePath mimePath) {
                return new JavaCamelCaseInterceptor();
            }
        }
    }
}
