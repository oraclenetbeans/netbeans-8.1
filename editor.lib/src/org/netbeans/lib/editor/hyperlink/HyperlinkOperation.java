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

package org.netbeans.lib.editor.hyperlink;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.editor.settings.EditorStyleConstants;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.JumpList;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.spi.editor.highlighting.HighlightAttributeValue;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.openide.util.Utilities;

/**
 *
 * @author Jan Lahoda
 */
public class HyperlinkOperation implements MouseListener, MouseMotionListener, PropertyChangeListener, KeyListener {

    private static Logger LOG = Logger.getLogger(HyperlinkOperation.class.getName());
    private static final String KEY = "hyperlink-operation"; //NOI18N
    
    private JTextComponent component;
    private Document       currentDocument;
    private String         operationMimeType;
    private Cursor         oldComponentsMouseCursor;
    private boolean        hyperlinkUp;
    private boolean        listenersSetUp;

    private boolean        hyperlinkEnabled;
    private int            actionKeyMask;
    private int            altActionKeyMask;
    
    public static void ensureRegistered(JTextComponent component, String mimeType) {
        if (component.getClientProperty(KEY) == null) {
            component.putClientProperty(KEY, new HyperlinkOperation(component, mimeType));
        }
    }
    
    private static synchronized Cursor getMouseCursor(HyperlinkType type) {
        switch (type) {
            case GO_TO_DECLARATION:
            case ALT_HYPERLINK:
                return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            default:
                throw new UnsupportedOperationException();
        }
    }
    
    private static synchronized boolean isHyperlinkMouseCursor(Cursor c) {
        return    c == Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
               || c == Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
               || c == Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
    
    /** Creates a new instance of HoveringImpl */
    private HyperlinkOperation(JTextComponent component, String mimeType) {
        this.component = component;
        this.operationMimeType  = mimeType;
        this.oldComponentsMouseCursor = null;
        this.hyperlinkUp = false;
        this.listenersSetUp = false;
        
        readSettings();
        
        if (hyperlinkEnabled) {
            component.addPropertyChangeListener("document", this); // NOI18N
        }
    }
    
    private void documentUpdated() {
        if (!hyperlinkEnabled)
            return ;
        
        currentDocument = component.getDocument();
        
        if (currentDocument instanceof BaseDocument) {
            if (!listenersSetUp) {
                component.addMouseListener(this);
                component.addMouseMotionListener(this);
                component.addKeyListener(this);
                listenersSetUp = true;
            }
        }
    }
    
    private void readSettings() {
        String hyperlinkActivationKeyPropertyValue = System.getProperty("org.netbeans.lib.editor.hyperlink.HyperlinkOperation.activationKey");
        
        if (hyperlinkActivationKeyPropertyValue != null) {
            if ("off".equals(hyperlinkActivationKeyPropertyValue)) { // NOI18N
                this.hyperlinkEnabled = false;
                this.actionKeyMask = (-1);
            } else {
                this.hyperlinkEnabled = true;
                this.actionKeyMask = (-1);
                
                for (int cntr = 0; cntr < hyperlinkActivationKeyPropertyValue.length(); cntr++) {
                    int localMask = 0;
                    
                    switch (hyperlinkActivationKeyPropertyValue.charAt(cntr)) {
                        case 'S': localMask = InputEvent.SHIFT_DOWN_MASK; break;
                        case 'C': localMask = InputEvent.CTRL_DOWN_MASK;  break;
                        case 'A': localMask = InputEvent.ALT_DOWN_MASK;   break;
                        case 'M': localMask = InputEvent.META_DOWN_MASK;  break;
                        default:
                            LOG.warning("Incorrect value of org.netbeans.lib.editor.hyperlink.HyperlinkOperation.activationKey property (only letters CSAM are allowed): " + hyperlinkActivationKeyPropertyValue.charAt(cntr));
                    }
                    
                    if (localMask == 0) {
                        //some problem, ignore
                        this.actionKeyMask = (-1);
                        break;
                    }
                    
                    if (this.actionKeyMask == (-1))
                        this.actionKeyMask = localMask;
                    else
                        this.actionKeyMask |= localMask;
                }
                
                if (this.actionKeyMask == (-1)) {
                    LOG.warning("Some problem with property org.netbeans.lib.editor.hyperlink.HyperlinkOperation.activationKey, more information might be given above. Falling back to the default behaviour.");
                } else {
                    return;
                }
            }
        }
        
        this.hyperlinkEnabled = true;

        Preferences prefs = MimeLookup.getLookup(DocumentUtilities.getMimeType(component)).lookup(Preferences.class);
        // there is in Mac preferences shortcut for META_MASK, by default we use CTRL_DOWN_MASK
        this.actionKeyMask = prefs.getInt(SimpleValueNames.HYPERLINK_ACTIVATION_MODIFIERS, InputEvent.CTRL_DOWN_MASK);
        // there is in Mac preferences shortcut for "META_DONW_MASK | InputEvent.ALT_DOWN_MASK", by default we use Ctrl+Alt
        this.altActionKeyMask = prefs.getInt(SimpleValueNames.ALT_HYPERLINK_ACTIVATION_MODIFIERS, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
    }
    
    public void mouseMoved(MouseEvent e) {
        HyperlinkType type = getHyperlinkType(e);
        
        if (type != null) {
            int position = component.viewToModel(e.getPoint());
            
            if (position < 0) {
                unHyperlink(true);
                
                return ;
            }
            
            performHyperlinking(position, type);
        } else {
            unHyperlink(true);
        }
    }
    
    public void mouseDragged(MouseEvent e) {
        //ignored
    }
    
    private HyperlinkType getHyperlinkType(InputEvent e) {
        int modifiers = e.getModifiers() | e.getModifiersEx();
        if ((modifiers & altActionKeyMask) == altActionKeyMask) {
            return HyperlinkType.ALT_HYPERLINK;
        } else if ((modifiers & actionKeyMask) == actionKeyMask) {
            return HyperlinkType.GO_TO_DECLARATION;
        }
        return null;
    }
    
    private void performHyperlinking(int position, HyperlinkType type) {
        final BaseDocument doc = (BaseDocument) component.getDocument();
        doc.readLock();
        try {
            HyperlinkProviderExt provider = findProvider(position, type);
            if (provider != null) {
                int[] offsets = provider.getHyperlinkSpan(doc, position, type);
                if (offsets != null) {
                    makeHyperlink(type, provider, offsets[0], offsets[1], position);
                }
            } else {
                unHyperlink(true);
            }
        } finally {
            doc.readUnlock();
        }

    }
    
    private void performAction(int position, HyperlinkType type) {
        HyperlinkProviderExt provider = findProvider(position, type);
        
        if (provider != null) {
            unHyperlink(true);
            
            //make sure the position is correct and the JumpList works:
            component.getCaret().setDot(position);
            JumpList.checkAddEntry(component, position);
            
            provider.performClickAction(component.getDocument(), position, type);
        }
    }
    
    private HyperlinkProviderExt findProvider(int position, HyperlinkType type) {
        Object mimeTypeObj = component.getDocument().getProperty(BaseDocument.MIME_TYPE_PROP);  //NOI18N
        String mimeType;
        
        if (mimeTypeObj instanceof String)
            mimeType = (String) mimeTypeObj;
        else {
            mimeType = this.operationMimeType;
        }
        
        Collection<? extends HyperlinkProviderExt> extProviders = getHyperlinkProviderExts(mimeType);
        
        for (HyperlinkProviderExt provider : extProviders) {
            if (provider.getSupportedHyperlinkTypes().contains(type) && provider.isHyperlinkPoint(component.getDocument(), position, type)) {
                return provider;
            }
        }
        
        if (type != HyperlinkType.GO_TO_DECLARATION) {
            return null;
        }
        
        Collection<? extends HyperlinkProvider> providers = getHyperlinkProviders(mimeType);
        
        for (final HyperlinkProvider provider : providers) {
            if (provider.isHyperlinkPoint(component.getDocument(), position)) {
                return new HyperlinkProviderExt() {
                    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
                        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION);
                    }
                    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
                        return provider.isHyperlinkPoint(doc, offset);
                    }
                    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
                        return provider.getHyperlinkSpan(doc, offset);
                    }
                    public void performClickAction(Document doc, int offset, HyperlinkType type) {
                        provider.performClickAction(doc, offset);
                    }
                    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
                        return null;
                    }
                };
            }
        }
        
        return null;
    }
    
    private synchronized void makeHyperlink(HyperlinkType type, HyperlinkProviderExt provider, final int start, final int end, final int offset) {
        boolean makeCursorSnapshot = true;
        
        if (hyperlinkUp) {
            unHyperlink(false);
            makeCursorSnapshot = false;
        }
        
        OffsetsBag prepare = new OffsetsBag(component.getDocument());

        FontColorSettings fcs = MimeLookup.getLookup(MimePath.EMPTY).lookup(FontColorSettings.class);
        AttributeSet hyperlinksHighlight = fcs.getFontColors("hyperlinks"); //NOI18N
        prepare.addHighlight(start, end, AttributesUtilities.createComposite(
            hyperlinksHighlight != null ? hyperlinksHighlight : defaultHyperlinksHighlight,
            AttributesUtilities.createImmutable(EditorStyleConstants.Tooltip, new TooltipResolver(provider, offset, type))));

        getBag(currentDocument).setHighlights(prepare);

        hyperlinkUp = true;

        if (makeCursorSnapshot) {
            if (component.isCursorSet()) {
                oldComponentsMouseCursor = component.getCursor();
            } else {
                oldComponentsMouseCursor = null;
            }
            component.setCursor(getMouseCursor(type));
        }
    }
    
    private synchronized void unHyperlink(boolean removeCursor) {
        if (!hyperlinkUp)
            return ;
        
        getBag(currentDocument).clear();
        
        if (removeCursor) {
            if (component.isCursorSet() && isHyperlinkMouseCursor(component.getCursor())) {
                component.setCursor(oldComponentsMouseCursor);
            }
            oldComponentsMouseCursor = null;
        }
        
        hyperlinkUp = false;
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        if (currentDocument != component.getDocument())
            documentUpdated();
    }
    
    public void keyTyped(KeyEvent e) {
        //ignored
    }

    public void keyReleased(KeyEvent e) {
        if (getHyperlinkType(e) == null) {
            unHyperlink(true);
        }
    }

    public void keyPressed(KeyEvent e) {
        HyperlinkType type = getHyperlinkType(e);
        Point mousePos = null;
        try {
            mousePos = component.getMousePosition();
        } catch (NullPointerException npe) {
            // #199407 - on systems without mouse this can happen 
            // instead of returning null
        }
        
        if (type != null && mousePos != null) {
            int position = component.viewToModel(mousePos);

            if (position < 0) {
                unHyperlink(true);

                return;
            }

            performHyperlinking(position, type);
        } else {
            unHyperlink(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        //ignored
    }

    public void mousePressed(MouseEvent e) {
        //ignored
    }

    public void mouseExited(MouseEvent e) {
        //ignored
    }

    public void mouseEntered(MouseEvent e) {
        //ignored
    }

    public void mouseClicked(MouseEvent e) {
        boolean activate = false;
        HyperlinkType type = getHyperlinkType(e);
        if ( type != null ) {
            activate = !e.isPopupTrigger() && e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e);
        } else if ( Utilities.isWindows() && e.getClickCount() == 1 && SwingUtilities.isMiddleMouseButton(e) ) {
            activate = true;
            type = HyperlinkType.GO_TO_DECLARATION;
        }
        
        if ( activate ) {
            int position = component.viewToModel(e.getPoint());
            
            if (position < 0) {
                return ;
            }
            
            performAction(position, type);
        }
    }
    
    private static Object BAG_KEY = new Object();
    
    private static OffsetsBag getBag(Document doc) {
        OffsetsBag bag = (OffsetsBag) doc.getProperty(BAG_KEY);
        
        if (bag == null) {
            doc.putProperty(BAG_KEY, bag = new OffsetsBag(doc));
        }
        
        return bag;
    }
    
    /**
     * Gets the list of <code>HyperlinkProvider</code>s for a given mime type.
     *
     * @param mimeType mime type to get the <code>HyperlinkProvider</code>s for
     *
     * @return The list of <code>HyperlinkProvider<code>s available for the given mime type.
     */
    public static Collection<? extends HyperlinkProvider> getHyperlinkProviders(String mimeType) {
        MimePath mimePath = MimePath.parse(mimeType);
        return MimeLookup.getLookup(mimePath).lookupAll(HyperlinkProvider.class);
    }

    /**
     * Gets the list of <code>HyperlinkProvider</code>s for a given mime type.
     *
     * @param mimeType mime type to get the <code>HyperlinkProvider</code>s for
     *
     * @return The list of <code>HyperlinkProvider<code>s available for the given mime type.
     */
    public static Collection<? extends HyperlinkProviderExt> getHyperlinkProviderExts(String mimeType) {
        MimePath mimePath = MimePath.parse(mimeType);
        return MimeLookup.getLookup(mimePath).lookupAll(HyperlinkProviderExt.class);
    }
    
    private static AttributeSet defaultHyperlinksHighlight = AttributesUtilities.createImmutable(StyleConstants.Foreground, Color.BLUE, StyleConstants.Underline, Color.BLUE);
    
    public static final class HighlightFactoryImpl implements HighlightsLayerFactory {
        public HighlightsLayer[] createLayers(Context context) {
            return new HighlightsLayer[] {
                HighlightsLayer.create(HyperlinkOperation.class.getName(), ZOrder.SHOW_OFF_RACK.forPosition(450), true, getBag(context.getDocument()))
            };
        }
    }

    private static final class TooltipResolver implements HighlightAttributeValue<CharSequence> {

        private static final String HYPERLINK_LISTENER = "TooltipResolver.hyperlinkListener"; //NOI18N
        private HyperlinkProviderExt provider;
        private int offset;
        private HyperlinkType type;

        public TooltipResolver(HyperlinkProviderExt provider, int offset, HyperlinkType type) {
            this.provider = provider;
            this.offset = offset;
            this.type = type;
        }

        public CharSequence getValue(JTextComponent component, Document document, Object attributeKey, int startOffset, int endOffset) {
            try {
                String tooltipText = provider.getTooltipText(document, offset, type);
                HyperlinkListener hl = (HyperlinkListener)document.getProperty(HYPERLINK_LISTENER);
                return hl != null ? new TooltipInfo(tooltipText, hl) : tooltipText;
            } finally {
                document.putProperty(HYPERLINK_LISTENER, null);
            }
        }        
    }
    
    public static final class TooltipInfo implements CharSequence {
        
        private final String tooltipText;
        private final HyperlinkListener listener;

        private TooltipInfo(String tooltipText, HyperlinkListener listener) {
            this.tooltipText = tooltipText;
            this.listener = listener;
        }

        public HyperlinkListener getListener() {
            return listener;
        }

        @Override
        public int length() {
            return tooltipText.length();
        }

        @Override
        public char charAt(int index) {
            return tooltipText.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return tooltipText.subSequence(start, end);
        }

        @Override
        public String toString() {
            return tooltipText;
        }
    }
}
