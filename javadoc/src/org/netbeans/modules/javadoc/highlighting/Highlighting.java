/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javadoc.highlighting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.api.java.lexer.JavadocTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenChange;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenHierarchyEvent;
import org.netbeans.api.lexer.TokenHierarchyListener;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.spi.editor.highlighting.HighlightsSequence;
import org.netbeans.spi.editor.highlighting.support.AbstractHighlightsContainer;
import org.openide.util.WeakListeners;

/**
 * @author Jan Becicka
 */
public class Highlighting extends AbstractHighlightsContainer implements TokenHierarchyListener {

    private static final Logger LOG = Logger.getLogger(Highlighting.class.getName());
    
    public static final String LAYER_ID = "org.netbeans.modules.javadoc.highlighting"; //NOI18N
    
    private AttributeSet fontColor;
    
    private Document document;
    private TokenHierarchy<? extends Document> hierarchy = null;
    private long version = 0;
    
    /** Creates a new instance of Highlighting */
    public Highlighting(Document doc) {
        AttributeSet firstLineFontColor = MimeLookup.getLookup(MimePath.get("text/x-java")).lookup(FontColorSettings.class).getTokenFontColors("javadoc-first-sentence");
        AttributeSet commentFontColor = MimeLookup.getLookup(MimePath.get("text/x-java")).lookup(FontColorSettings.class).getTokenFontColors("comment");
        Collection<Object> attrs = new LinkedList<Object>();
        for (Enumeration<?> e = firstLineFontColor.getAttributeNames(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object value = firstLineFontColor.getAttribute(key);
            
            if (!commentFontColor.containsAttribute(key, value)) {
                attrs.add(key);
                attrs.add(value);
            }
        }
        fontColor = AttributesUtilities.createImmutable(attrs.toArray());
        this.document = doc;
        hierarchy = TokenHierarchy.get(document);
        if (hierarchy != null) {
            hierarchy.addTokenHierarchyListener(WeakListeners.create(TokenHierarchyListener.class, this, hierarchy));
        }
    }

    @Override
    public HighlightsSequence getHighlights(int startOffset, int endOffset) {
        synchronized(this) {
            if (hierarchy.isActive()) {
                return new HSImpl(version, hierarchy, startOffset, endOffset);
            } else {
                return HighlightsSequence.EMPTY;
            }
        }
    }

    // ----------------------------------------------------------------------
    //  TokenHierarchyListener implementation
    // ----------------------------------------------------------------------

    @Override
    public void tokenHierarchyChanged(TokenHierarchyEvent evt) {
        TokenChange<?> tc = evt.tokenChange();
        int affectedArea [] = null;
        
        TokenSequence<? extends TokenId> seq = tc.currentTokenSequence();
        if (seq.language().equals(JavadocTokenId.language())) {
            // Change inside javadoc
            int [] firstSentence = findFirstSentence(seq);
            if (firstSentence != null) {
                if (tc.offset() <= firstSentence[1]) {
                    // Change before the end of the first sentence
                    affectedArea = firstSentence;
                }
            } else {
                // XXX: need the embedding token (i.e. JavaTokenId.JAVADOC_COMMENT*)
                // and fire a change in its whole area
                affectedArea = new int [] { tc.offset(), evt.affectedEndOffset() };
            }
        } else {
            // The change may or may not involve javadoc, so reset everyting.
            // It would be more efficient to traverse the changed area and
            // find out whether it really involves javadoc or not.
            affectedArea = new int [] { tc.offset(), evt.affectedEndOffset() };
        }
        
        if (affectedArea != null) {
            synchronized (this) {
                version++;
            }

            fireHighlightsChange(affectedArea[0], affectedArea[1]);
        }
    }

    // ----------------------------------------------------------------------
    //  Private implementation
    // ----------------------------------------------------------------------

    private int [] findFirstSentence(TokenSequence<? extends TokenId> seq) {
        seq.moveStart();
        if (seq.moveNext()) {
            int start = seq.offset();
            do {
                if (seq.token().id() == JavadocTokenId.DOT) {
                    if (seq.moveNext()) {
                        if (isWhiteSpace(seq.token())) {
                            return new int [] { start, seq.offset()};
                        }
                        seq.movePrevious();
                     }
                } else if (seq.token().id() == JavadocTokenId.TAG) {
                    if (seq.movePrevious()) {
                        if (!seq.token().text().toString().trim().endsWith("{")) {
                            //not an inline tag
                            return new int [] { start, seq.offset()};
                        }
                    }
                    seq.moveNext();
                }
            } while (seq.moveNext());
        }
        return null;
    }

    private static boolean isWhiteSpace(Token<? extends TokenId> token) {
        if (token == null || token.id() != JavadocTokenId.OTHER_TEXT) {
            return false;
        }
        String ws = " \t\n";
        return ws.indexOf(token.text().charAt(0)) >= 0;
    }

    private final class HSImpl implements HighlightsSequence {
        
        private long version;
        private TokenHierarchy<? extends Document> scanner;
        private List<TokenSequence<? extends TokenId>> sequences;
        private int startOffset;
        private int endOffset;
        
        private List<Integer> lines = null;
        private int linesIdx = -1;
        
        public HSImpl(long version, TokenHierarchy<? extends Document> scanner, int startOffset, int endOffset) {
            this.version = version;
            this.scanner = scanner;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.sequences = null;
        }

        public boolean moveNext() {
            synchronized (Highlighting.this) {
                checkVersion();
                
                if (sequences == null) {
                    // initialize
                    TokenSequence<?> tokenSequence = scanner.tokenSequence();
                    if (tokenSequence==null) {
                        //#199027
                        //inactive hierarchy, no next
                        return false;
                    }
                    TokenSequence<?> seq = tokenSequence.subSequence(startOffset, endOffset);
                    sequences = new ArrayList<TokenSequence<? extends TokenId>>();
                    sequences.add(seq);
                }

                if (lines != null) {
                    if (linesIdx + 2 < lines.size()) {
                        linesIdx += 2;
                        return true;
                    }
                    
                    lines = null;
                    linesIdx = -1;
                }
                
                while (!sequences.isEmpty()) {
                    TokenSequence<? extends TokenId> seq = sequences.get(sequences.size() - 1);

                    if (seq.language().equals(JavadocTokenId.language())) {
                        int [] firstSentence = findFirstSentence(seq);
                        sequences.remove(sequences.size() - 1);

                        if (firstSentence != null) {
                            lines = splitByLines(firstSentence[0], firstSentence[1]);
                            if (lines != null) {
                                linesIdx = 0;
                                return true;
                            }
                        }
                    } else {
                        boolean hasNextToken;

                        while (true == (hasNextToken = seq.moveNext())) {
                            TokenSequence<?> embeddedSeq = seq.embedded();
                            if (embeddedSeq != null) {
                                sequences.add(sequences.size(), embeddedSeq);
                                break;
                            }
                        }

                        if (!hasNextToken) {
                            sequences.remove(sequences.size() - 1);
                        }
                    }
                }

                return false;
            }
        }

        public int getStartOffset() {
            synchronized (Highlighting.this) {
                checkVersion();
                
                if (sequences == null) {
                    throw new NoSuchElementException("Call moveNext() first."); //NOI18N
                }

                if (lines != null) {
                    return lines.get(linesIdx);
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        public int getEndOffset() {
            synchronized (Highlighting.this) {
                checkVersion();
                
                if (sequences == null) {
                    throw new NoSuchElementException("Call moveNext() first."); //NOI18N
                }

                if (lines != null) {
                    return lines.get(linesIdx + 1);
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        public AttributeSet getAttributes() {
            synchronized (Highlighting.this) {
                checkVersion();
                
                if (sequences == null) {
                    throw new NoSuchElementException("Call moveNext() first."); //NOI18N
                }

                if (lines != null) {
                    return fontColor;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }
        
        private void checkVersion() {
            if (this.version != Highlighting.this.version) {
                throw new ConcurrentModificationException();
            }
        }
        
        private List<Integer> splitByLines(int sentenceStart, int sentenceEnd) {
            ArrayList<Integer> lines = new ArrayList<Integer>();
            int offset = sentenceStart;
            
            try {
                while (offset < sentenceEnd) {
                    Element lineElement = document.getDefaultRootElement().getElement(
                        document.getDefaultRootElement().getElementIndex(offset));

                    int rowStart = offset == sentenceStart ? offset : lineElement.getStartOffset();
                    int rowEnd = lineElement.getEndOffset();

                    String line = document.getText(rowStart, rowEnd - rowStart);
                    int idx = 0;
                    while (idx < line.length() && 
                        (line.charAt(idx) == ' ' || 
                        line.charAt(idx) == '\t' || 
                        line.charAt(idx) == '*'))
                    {
                        idx++;
                    }

                    if (rowStart + idx < rowEnd) {
                        lines.add(rowStart + idx);
                        lines.add(Math.min(rowEnd, sentenceEnd));
                    }

                    offset = rowEnd + 1;
                }
            } catch (BadLocationException e) {
                LOG.log(Level.WARNING, "Can't determine javadoc first sentence", e);
            }
            
            return lines.isEmpty() ? null : lines;
        }
    } // End of HSImpl class
}
