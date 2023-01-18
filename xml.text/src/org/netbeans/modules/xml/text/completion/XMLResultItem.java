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

package org.netbeans.modules.xml.text.completion;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.text.*;
import javax.swing.Icon;

import org.netbeans.editor.*;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.xml.api.model.GrammarResult;
import org.netbeans.modules.xml.api.model.DescriptionSource;
import org.netbeans.modules.xml.text.api.XMLDefaultTokenContext;
import org.netbeans.modules.xml.text.syntax.XMLSyntaxSupport;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.swing.plaf.LFCustoms;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 * This class carries result information required by NetBeans Editor module.
 *
 * @author  Petr Kuzel
 * @author  Sandeep Randhawa
 */
class XMLResultItem implements CompletionItem {
    private static final Logger LOG = Logger.getLogger(XMLResultItem.class.getName());
    
    private static final Color COLOR = new Color(64, 64, 255);
    
    // text to be diplayed to user
    public final String displayText;
    private final String replacementText;
    // icon to be diplayed
    public javax.swing.Icon icon;
    public Color foreground;
    public Color background;
    public Color selectionForeground;
    public Color selectionBackground;
    private static JLabel rubberStamp = new JLabel();
    private boolean shift = false;
    private final int position;
    
    static {
        rubberStamp.setOpaque( true );
    }
    
    {
        foreground = LFCustoms.getTextFgColor();
        background = UIManager.getColor( "Tree.background" ); // NOI18N
        if (background ==null) {
            background = Color.white;
        }
        
        selectionForeground = UIManager.getColor("List.selectionForeground"); //NOI18N
        if (selectionForeground == null) {
            selectionForeground =  Color.black;
        }
        selectionBackground = UIManager.getColor("List.selectionBackground"); // NOI18N
        if (selectionBackground == null) {
            selectionBackground = new Color(204, 204, 255);
        }
    }
    
    /**
     *
     * @param replacementText replacement text that is used as display name too
     */
    public XMLResultItem(int position, String replacementText){
        this(position, replacementText, null);
    }
    
    /**
     * @param displayText text to display or null if replacementText is OK
     */
    public XMLResultItem(int position, String replacementText, String displayText) {
        this.replacementText = replacementText;
        this.displayText = displayText != null ? displayText : replacementText;
        this.position = position;
    }
    
    /**
     * Insert following text into document.
     */
    public String getReplacementText(int modifiers){
        return displayText;
    }
    
    
    protected Icon getIcon(){
        return icon;
    }
    
    /**
     * Computes length of the original text to be deleted prior to the replacement.
     * It is a hack to allow ValueResultItems to delete extra suffixes after inserting
     * a new value (see defect #228865). The original code in replaceText ignores entirely the parameter 'len',
     * so the correct len cannot be passed from an overriding method: other code may send
     * invalid lengths, which were ignored before the fix. 
     * 
     * The fix affects just the ValueResultItem then.
     * 
     */
    /* package */ int getDeleteLength(String currentText, String replaceToText, int len) {
        return len;
    }
    /**
     * Actually replaces a piece of document by passes text.
     * @param component a document source
     * @param text a string to be inserted
     * @param offset the target offset
     * @param len a length that should be removed before inserting text
     */
    boolean replaceText( JTextComponent component, final String replaceToText, int offset, int len) {
        BaseDocument doc = (BaseDocument)component.getDocument();
        doc.atomicLock();
        int replacementLength = replaceToText.length();
        try {
            String currentText = doc.getText(offset, 
                    (doc.getLength() - offset) < replacementLength ?
                        (doc.getLength() - offset) : replacementLength) ;
            //fix for #86792
            if(("<"+currentText+">").equals(("</")+replaceToText))
                return true;
            if(!replaceToText.equals(currentText)) {
                //fix for 137717
                String str = doc.getText(offset-1, 1);
                if(str != null && str.equals("&")) {
                    offset--;
                    currentText = doc.getText(offset,
                        (doc.getLength() - offset) < replacementLength ?
                            (doc.getLength() - offset) : replacementLength) ;
                }
                //
                // Length correction here. See the issue #141320.
                // See also issue #228865
                len = getFirstDiffPosition(currentText, replaceToText);
                //getDeleteLength(currentText, replaceToText, len);
                //
                // if the text is going to remove isn't the same as that is going
                // to be inserted, then only move the caret position
                if (len == replacementLength) {
                    component.setCaretPosition(offset + len);
                } else {
                    //+++ fix for issues #166462, #173122, #173691
                    String selectedText = component.getSelectedText(),
                           replacingText = replaceToText;
                    if (selectedText != null) {
                        len = selectedText.length();
                    } else if (! isTextRemovingAllowable(component, doc, replaceToText, offset)) {
                        if (len > 0) {
                            replacingText = replaceToText.substring(len);
                            offset += len;
                        }
                        len = 0;
                    } else {
                        len = getDeleteLength(currentText, replaceToText, len);
                    }
                    if (len > 0) {
                        doc.remove(offset, len);
                    }
                    doc.insertString(offset, replacingText, null);
                }
            } else {
                int newCaretPos = component.getCaret().getDot() + replacementLength - len;
                //#82242 workaround - the problem is that in some situations
                //1) result item is created and it remembers the remove length
                //2) document is changed
                //3) RI is substituted.
                //this situation shouldn't happen imho and is a problem of CC infrastructure
                component.setCaretPosition(newCaretPos < doc.getLength() ? newCaretPos : doc.getLength());
            }
            //reformat the line
            //((ExtFormatter)doc.getFormatter()).reformat(doc, Utilities.getRowStart(doc, offset), offset+text.length(), true);
        } catch (BadLocationException exc) {
            return false;    //not sucessfull
            // } catch (IOException e) {
            //     return false;
        } finally {
            doc.atomicUnlock();
        }
        return true;
    }

    //+++ fix for issues #166462, #173122
    //    (http://www.netbeans.org/issues/show_bug.cgi?id=166462)
    //    (http://www.netbeans.org/issues/show_bug.cgi?id=173122)
    private boolean isTextRemovingAllowable(JTextComponent component,
        BaseDocument doc, String replaceToText, int offset) throws BadLocationException {
        XMLSyntaxSupport support = (XMLSyntaxSupport)
            org.netbeans.editor.Utilities.getSyntaxSupport(component);
        TokenItem tokenItem = support.getTokenChain(offset, doc.getLength());
        boolean isTextRemovingAllowable = (tokenItem == null);
        if (! isTextRemovingAllowable) {
            String tokenItemImage = tokenItem.getImage();
            if ((tokenItemImage != null) && (tokenItemImage.length() > 0)) {
                // See also issue #228865. In this case, the token also may include a prefix
                // of the replacement value
                String replaceInclPrefix;
                int offs = Math.max(0, offset - tokenItem.getOffset());
                replaceInclPrefix = tokenItemImage.substring(0, offs) + replaceToText;
                int diffPos = getFirstDiffPosition(tokenItemImage, replaceInclPrefix);
                diffPos = diffPos == 0 ? 1 : diffPos;
                if ((diffPos > -1) && 
                    (diffPos <= Math.min(tokenItemImage.length(), replaceInclPrefix.length()))) {
                    String
                        strImg = tokenItemImage.length() >= diffPos ?
                            tokenItemImage.substring(0, diffPos) : tokenItemImage,
                        strText = replaceInclPrefix.length() >= diffPos ?
                            replaceInclPrefix.substring(0, diffPos) : replaceInclPrefix;

                    TokenID tokenID = tokenItem.getTokenID();
                    int id = (tokenID != null ? tokenID.getNumericID() : -1);

                    isTextRemovingAllowable = (id == XMLDefaultTokenContext.TAG_ID ?
                        ! strImg.startsWith(strText) /* <= for tags */ :
                        strImg.startsWith(strText)   /* <= for attributes */);
                }
            }
        }
        return isTextRemovingAllowable;
    }

    /**
     * Calculates the index of the first difference between two strings. 
     * If they are differenent starting the first character, then 0 is returned.
     * If one of the string completely starts with another one, then the length
     * of the shorter string is returned.
     * @param str1
     * @param str2
     * @return
     */
    private int getFirstDiffPosition(String str1, String str2) {
        int lastCharIndex = Math.min(str1.length(), str2.length());
        for (int index = 0; index < lastCharIndex; index++) {
            if (str1.charAt(index) != str2.charAt(index)) {
                return index;
            }
        }
        return lastCharIndex;
    }

    public boolean substituteCommonText( JTextComponent c, int offset, int len, int subLen ) {
        return replaceText( c, getReplacementText(0).substring( 0, subLen ), offset, len );
    }
    
    /**
     * Just translate <code>shift</code> to proper modifier
     */
    public final boolean substituteText( JTextComponent c, int offset, int len, boolean shift ) {
        int modifier = shift ? java.awt.event.InputEvent.SHIFT_MASK : 0;
        return substituteText(c, offset, len, modifier);
    }
    
    public boolean substituteText( JTextComponent c, int offset, int len, int modifiers ){
        return replaceText(c, getReplacementText(modifiers), offset, len);
    }
    
    /** @return Properly colored JLabel with text gotten from <CODE>getPaintText()</CODE>. */
    public java.awt.Component getPaintComponent( javax.swing.JList list, boolean isSelected, boolean cellHasFocus ) {
        // The space is prepended to avoid interpretation as HTML Label
        if (getIcon() != null) rubberStamp.setIcon(getIcon());
        
        rubberStamp.setText( displayText );
        if (isSelected) {
            rubberStamp.setBackground(selectionBackground);
            rubberStamp.setForeground(selectionForeground);
        } else {
            rubberStamp.setBackground(background);
            rubberStamp.setForeground(foreground);
        }
        return rubberStamp;
    }
    
    public final String getItemText() {
        return replacementText;
    }

    @Override
    public String toString() {
        return getItemText();
    }
    
    Color getPaintColor() { return LFCustoms.shiftColor(COLOR); }
    
    ////////////////////////////////////////////////////////////////////////////////
    ///////////////////methods from CompletionItem interface////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    public CompletionTask createDocumentationTask() {
        return null; //no documentation supported for now
        //return new AsyncCompletionTask(new DocQuery(this));
    }
    
    /**
     * Helper method for result items providing documentation.
     * @return 
     */
    protected CompletionTask doCreateDocumentationTask(final GrammarResult res) {
        return new CompletionTask() {
            public void query(CompletionResultSet resultSet) {
                CompletionDocumentation cd = create();
                if (cd != null) {
                    resultSet.setDocumentation(cd);
                }
                resultSet.finish();
            }
            public void refresh(CompletionResultSet resultSet) {
                query(resultSet);
            }
            public void cancel() {}

            
            private CompletionDocumentation create() {
                String doc;
                
                doc = res.getDescription();
                if (!(res instanceof DescriptionSource)) {
                    if (doc == null) {
                        return null;
                    }
                    return new Docum(doc);
                } else {
                    DescriptionSource ds = (DescriptionSource)res;
                    if (doc == null && ds.getContentURL() == null) {
                        return null;
                    }
                    return new ExtDocum(ds,  doc);
                }
            }
        };
    }

    /**
     * Extended documentation, based on the {@link DescriptionSource} SPI.
     */
    private static class ExtDocum extends URLDocum implements CompletionDocumentation {
        private DescriptionSource src;
        private String doc;

        ExtDocum(DescriptionSource src, String doc) {
            super(src.getContentURL(), src.isExternal());
            this.src = src;
            this.doc = doc;
        }

        @Override
        public String getText() {
            if (doc == null) {
                doc = src.getDescription();
                if (doc == null) {
                    doc = super.getText();
                }
            }
            return doc;
        }

        @Override
        public CompletionDocumentation resolveLink(String link) {
            try {
                DescriptionSource target = src.resolveLink(link);
                if (target != null) {
                    return new ExtDocum(target, null);
                }
                
                URL base = src.getContentURL();
                if (base == null) {
                    // sorry, cannot resolve.
                    return null;
                }
                
                URL targetURL = new URL(base, link);
                
                // leave the VM as soon as possible. This hack uses URLMappers
                // to find out whether URL (converted to FO and back) can be
                // represented outside the VM
                boolean external = true;
                FileObject f = URLMapper.findFileObject(targetURL);
                if (f != null) {
                    external = URLMapper.findURL(f, URLMapper.EXTERNAL) != null;
                }
                return new URLDocum(targetURL, external);
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }

        @Override
        public Action getGotoSourceAction() {
            return null;
        }
    }
    
    /**
     * Pure URL documentation item. Resolves links, if original URL was able
     * to open externally, the derived URLs do it as well.
     */
    private static class URLDocum implements CompletionDocumentation {
        URL content;
        boolean external;
        
        URLDocum(URL content, boolean external) {
            this.content = content;
            this.external = external;
        }
        
        URLDocum(boolean external) {
            this.external = external;
        }

        @Override
        public Action getGotoSourceAction() {
            return null;
        }

        @Override
        public String getText() {
            if (content == null) {
                return null;
            }
            FileObject f = URLMapper.findFileObject(content);
            if (f != null) {
                try {
                    return new String(f.asBytes(), FileEncodingQuery.getEncoding(f));
                } catch (IOException x) {
                    LOG.log(Level.INFO, "Could not load " + content, x);
                }
            }
            return null;
        }

        @Override
        public URL getURL() {
            return external ? content : null;
        }

        @Override
        public CompletionDocumentation resolveLink(String link) {
            if (content == null) {
                return null;
            }
            try {
                return new URLDocum(new URL(content, link), external);
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }
        
    }
    
    private static class Docum implements CompletionDocumentation {
        private String doc;
        
        private Docum(String doc) {
            this.doc = doc;
        }

        @Override
        public String getText() {
            return doc;
        }

        @Override
        public URL getURL() {
            return null;
        }

        @Override
        public CompletionDocumentation resolveLink(String link) {
            return null;
        }

        @Override
        public Action getGotoSourceAction() {
            return null;
        }

    }
    
    public CompletionTask createToolTipTask() {
        return null;
    }
    
    public void defaultAction(JTextComponent component) {
        int substOffset = getSubstituteOffset();
        if (substOffset == -1)
            substOffset = component.getCaretPosition();
        
        if(!shift) Completion.get().hideAll();
        substituteText(component, substOffset, component.getCaretPosition() - substOffset, shift);
    }
    
    static int substituteOffset = -1;
    
    public int getSubstituteOffset() {
        return substituteOffset;
    }
    
    public CharSequence getInsertPrefix() {
        return getItemText();
    }
    
    public Component getPaintComponent(boolean isSelected) {
        XMLCompletionResultItemPaintComponent paintComponent =
            new XMLCompletionResultItemPaintComponent.StringPaintComponent(getPaintColor());
        paintComponent.setIcon(icon);
        paintComponent.setSelected(isSelected);
        paintComponent.setString(getItemText());
        return paintComponent;
    }
    
    public int getPreferredWidth(Graphics g, Font defaultFont) {
        Component renderComponent = getPaintComponent(false);
        renderComponent.setFont(defaultFont);
        return renderComponent.getPreferredSize().width;
    }
    
    public int getSortPriority() {
        return position;
    }
    
    public CharSequence getSortText() {
        return getItemText();
    }
    
    public boolean instantSubstitution(JTextComponent component) {
        defaultAction(component);
        return true;
    }
    
    public void processKeyEvent(KeyEvent e) {
        shift = (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED && e.isShiftDown());
    }
    
    public void render(Graphics g, Font defaultFont,
            Color defaultColor, Color backgroundColor,
            int width, int height, boolean selected) {
        Component renderComponent = getPaintComponent(selected);
        renderComponent.setFont(defaultFont);
        renderComponent.setForeground(defaultColor);
        renderComponent.setBackground(backgroundColor);
        renderComponent.setBounds(0, 0, width, height);
        XMLCompletionResultItemPaintComponent xmlComp = (XMLCompletionResultItemPaintComponent)renderComponent;
        // already set in getPaintComponent, but someone might override the getter, without an  icon.
        xmlComp.setIcon(icon);
        xmlComp.paintComponent(g);
    }
    
}
