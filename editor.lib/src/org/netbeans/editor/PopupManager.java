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

package org.netbeans.editor;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;
import org.openide.util.Parameters;


/**
 *  Popup manager allows to display an arbitrary popup component
 *  over the underlying text component.
 *
 *  @author  Martin Roskanin, Miloslav Metelka
 *  @since   03/2002
 */
public class PopupManager {

    private static final Logger LOG = Logger.getLogger(PopupManager.class.getName());
    
    private JComponent popup = null;
    private final JTextComponent textComponent;

    /** Place popup always above cursor */
    public static final Placement Above = new Placement("Above"); //NOI18N
    
    /** Place popup always below cursor */
    public static final Placement Below = new Placement("Below"); //NOI18N
    
    /** Place popup to larger area. i.e. if place below cursor is 
        larger than place above, then popup will be placed below cursor. */
    public static final Placement Largest = new Placement("Largest"); //NOI18N
    
    /** Place popup above cursor. If a place above cursor is insufficient, 
        then popup will be placed below cursor. */
    public static final Placement AbovePreferred = new Placement("AbovePreferred"); //NOI18N
    
    /** Place popup below cursor. If a place below cursor is insufficient, 
        then popup will be placed above cursor. */
    public static final Placement BelowPreferred = new Placement("BelowPreferred"); //NOI18N
    
    /**
     * Place the popup on a fixed point of the view, measured from top-left corner
     */
    public static final Placement FixedPoint = new Placement("TopLeft");
    
    /** Place popup inside the scrollbar's viewport */
    public static final HorizontalBounds ViewPortBounds = new HorizontalBounds("ViewPort"); //NOI18N
    
    /** Place popup inside the whole scrollbar */
    public static final HorizontalBounds ScrollBarBounds = new HorizontalBounds("ScrollBar"); //NOI18N
    
    private final KeyListener keyListener;
    
    private final TextComponentListener componentListener;
    
    /** Creates a new instance of PopupManager */
    public PopupManager(JTextComponent textComponent) {
        this.textComponent = textComponent;
        keyListener = new PopupKeyListener();
        textComponent.addKeyListener(keyListener);
        componentListener = new TextComponentListener();
        textComponent.addComponentListener(componentListener);
    }
    
    /** Install popup component to textComponent root pane
     *  based on caret coordinates with the <CODE>Largest</CODE> placement.
     *  Note: Make sure the component is properly uninstalled later,
     *  if it is not necessary. See issue #35325 for details.
     *  @param popup popup component to be installed into
     *  root pane of the text component.
     */
    public void install(JComponent popup) {
        if (textComponent == null) return;
        int caretPos = textComponent.getCaret().getDot();
        try {
            Rectangle caretBounds = textComponent.modelToView(caretPos);
            install(popup, caretBounds, Largest);
        } catch (BadLocationException e) {
            // do not install if the caret position is invalid
        }
    }

    /** Removes popup component from textComponent root pane
     *  @param popup popup component to be removed from
     *  root pane of the text component.
     */
    public void uninstall(JComponent popup) {
        JComponent oldPopup = this.popup;

        if (oldPopup != null) {
            if (oldPopup.isVisible()) {
                oldPopup.setVisible(false);
            }
            removeFromRootPane(oldPopup);
            this.popup = null;
        }

        if (popup != null && popup != oldPopup) {
            if (popup.isVisible()) {
                popup.setVisible(false);
            }
            removeFromRootPane(popup);
        }
    }

    public void install(
        JComponent popup, Rectangle cursorBounds,
        Placement placement, HorizontalBounds horizontalBounds, int horizontalAdjustment, int verticalAdjustment
    ) {
//        /* Uninstall the old popup from root pane
//         * and install the new one. Even in case
//         * they are the same objects it's necessary
//         * to cover the workspace switches etc.
//         */
//        if (this.popup != null) {
//            // if i.e. completion is visible and tooltip is being installed,
//            // completion popup should be closed.
//            if (this.popup.isVisible() && this.popup!=popup) this.popup.setVisible(false);
//            removeFromRootPane(this.popup);
//        }

        if (this.popup != null && this.popup != popup) {
            uninstall(null);
        }

        assert this.popup == null || this.popup == popup : "this.popup=" + this.popup + ", popup=" + popup; //NOI18N

        if (popup != null) {
            if (this.popup == null) {
                this.popup = popup;
                installToRootPane(this.popup);
            } // else this.popup == popup
        
            // Update the bounds of the popup
            Rectangle bounds = computeBounds(this.popup, textComponent,
                cursorBounds, placement, horizontalBounds);

            LOG.log(Level.FINE, "computed-bounds={0}", bounds); //NOI18N
            if (bounds != null){
                // Convert to layered pane's coordinates

                if (horizontalBounds == ScrollBarBounds && placement != FixedPoint){
                    bounds.x = 0;
                }

                JRootPane rp = textComponent.getRootPane();
                if (rp!=null){
                    bounds = SwingUtilities.convertRectangle(textComponent, bounds,
                        rp.getLayeredPane());
                    if (bounds.y < 0) {
                        bounds.y = 0;
                    }
                }

                if (horizontalBounds == ScrollBarBounds){
                    if (textComponent.getParent() instanceof JViewport){
                        int shift = textComponent.getParent().getX();
                        Rectangle viewBounds = ((JViewport)textComponent.getParent()).getViewRect();
                        bounds.x += viewBounds.x;
                        bounds.x -= shift;
                        bounds.width += shift;
                    }
                }

                bounds.x = bounds.x + horizontalAdjustment;
                bounds.y = bounds.y + verticalAdjustment;
                bounds.width = bounds.width - horizontalAdjustment;
                bounds.height = bounds.height - verticalAdjustment;

                LOG.log(Level.FINE, "setting bounds={0} on {1}", new Object [] { bounds, this.popup }); //NOI18N
                this.popup.setBounds(bounds);

            } else { // can't fit -> hide
                this.popup.setVisible(false);
            }
        }
    }
    
    public void install(JComponent popup, Rectangle cursorBounds, Placement placement, HorizontalBounds horizontalBounds) {
        install(popup, cursorBounds, placement, horizontalBounds, 0, 0);
    }
    
    public void install(JComponent popup, Rectangle cursorBounds, Placement placement) {
        install(popup, cursorBounds, placement, ViewPortBounds);
    }
    
    /** Returns installed popup panel component */
    public JComponent get(){
        return popup;
    }
    

    /** Install popup panel to current textComponent root pane */
    private void installToRootPane(JComponent c) {
        JRootPane rp = textComponent.getRootPane();
        if (rp != null) {
            rp.getLayeredPane().add(c, JLayeredPane.POPUP_LAYER, 0);
        }
    }

    /** Remove popup panel from previous textComponent root pane */
    private void removeFromRootPane(JComponent c) {
        JRootPane rp = c.getRootPane();
        if (rp != null) {
            rp.getLayeredPane().remove(c);
        }
    }

    /** Variation of the method for computing the bounds
     * for the concrete view component. As the component can possibly
     * be placed in a scroll pane it's first necessary
     * to translate the cursor bounds and also translate
     * back the resulting popup bounds.
     * @param popup  popup panel to be displayed
     * @param view component over which the popup is displayed.
     * @param cursorBounds the bounds of the caret or mouse cursor
     *    relative to the upper-left corner of the visible view.
     * @param placement where to place the popup panel according to
     *    the cursor position.
     * @return bounds of popup panel relative to the upper-left corner
     *    of the underlying view component.
     *    <CODE>null</CODE> if there is no place to display popup.
     */
    protected static Rectangle computeBounds(JComponent popup,
    JComponent view, Rectangle cursorBounds, Placement placement, HorizontalBounds horizontalBounds) {
        
        if (horizontalBounds == null) horizontalBounds = ViewPortBounds;
        
        Rectangle ret;
        Component viewParent = view.getParent();
        
        if (viewParent instanceof JViewport) {
            Rectangle viewBounds = ((JViewport)viewParent).getViewRect();

            Rectangle translatedCursorBounds = (Rectangle)cursorBounds.clone();
            if (placement != FixedPoint) {
                translatedCursorBounds.translate(-viewBounds.x, -viewBounds.y);
            }

            ret = computeBounds(popup, viewBounds.width, viewBounds.height,
                translatedCursorBounds, placement, horizontalBounds);
            
            if (ret != null) { // valid bounds
                ret.translate(viewBounds.x, viewBounds.y);
            }
            
        } else { // not in scroll pane
            ret = computeBounds(popup, view.getWidth(), view.getHeight(),
                cursorBounds, placement);
        }
        
        return ret;
    }

    protected static Rectangle computeBounds(JComponent popup,
    JComponent view, Rectangle cursorBounds, Placement placement) {
        return computeBounds(popup, view, cursorBounds, placement, ViewPortBounds);
    }    
    
    /** Computes a best-fit bounds of popup panel
     *  according to available space in the underlying view
     *  (visible part of the pane).
     *  The placement is first evaluated and put into the popup's client property
     *  by <CODE>popup.putClientProperty(Placement.class, actual-placement)</CODE>.
     *  The actual placement is <UL>
     *  <LI> <CODE>Above</CODE> if the original placement was <CODE>Above</CODE>.
     *  Or if the original placement was <CODE>AbovePreferred</CODE>
     *  or <CODE>Largest</CODE>
     *  and there is more space above the cursor than below it.
     *  <LI> <CODE>Below</CODE> if the original placement was <CODE>Below</CODE>.
     *  Or if the original placement was <CODE>BelowPreferred</CODE>
     *  or <CODE>Largest</CODE>
     *  and there is more space below the cursor than above it.
     *  <LI> <CODE>AbovePreferred</CODE> if the original placement
     *  was <CODE>AbovePreferred</CODE>
     *  and there is less space above the cursor than below it.
     *  <LI> <CODE>BelowPreferred</CODE> if the original placement
     *  was <CODE>BelowPreferred</CODE>
     *  and there is less space below the cursor than above it.
     *  <P>Once the placement client property is set
     *  the <CODE>popup.setSize()</CODE> is called with the size of the area
     *  above/below the cursor (indicated by the placement).
     *  The popup responds by updating its size to the equal or smaller
     *  size. If it cannot physically fit into the requested area
     *  it can call
     *  <CODE>putClientProperty(Placement.class, null)</CODE>
     *  on itself to indicate that it cannot fit. The method scans
     *  the content of the client property upon return from
     *  <CODE>popup.setSize()</CODE> and if it finds null there it returns
     *  null bounds in that case. The only exception is
     *  if the placement was either <CODE>AbovePreferred</CODE>
     *  or <CODE>BelowPreferred</CODE>. In that case the method
     *  gives it one more try
     *  by attempting to fit the popup into (bigger) complementary
     *  <CODE>Below</CODE> and <CODE>Above</CODE> areas (respectively).
     *  The popup either fits into these (bigger) areas or it again responds
     *  by returning <CODE>null</CODE> in the client property in which case
     *  the method finally gives up and returns null bounds.
     *   
     *  @param popup popup panel to be displayed
     *  @param viewWidth width of the visible view area.
     *  @param viewHeight height of the visible view area.
     *  @param cursorBounds the bounds of the caret or mouse cursor
     *    relative to the upper-left corner of the visible view
     *  @param originalPlacement where to place the popup panel according to
     *    the cursor position
     *  @return bounds of popup panel relative to the upper-left corner
     *    of the underlying view.
     *    <CODE>null</CODE> if there is no place to display popup.
     */
    protected static Rectangle computeBounds(
        JComponent popup,
        int viewWidth, 
        int viewHeight, 
        Rectangle cursorBounds, 
        Placement originalPlacement, 
        HorizontalBounds horizontalBounds)
    {
        Parameters.notNull("popup", popup); //NOI18N
        Parameters.notNull("cursorBounds", cursorBounds); //NOI18N
        Parameters.notNull("originalPlacement", originalPlacement); //NOI18N
        
        // Compute available height above the cursor
        int aboveCursorHeight = cursorBounds.y;
        int belowCursorY = cursorBounds.y + cursorBounds.height;
        int belowCursorHeight = viewHeight - belowCursorY;
        
        Dimension prefSize = popup.getPreferredSize();
        final int width = Math.min(viewWidth, prefSize.width);
        
        popup.setSize(width, Integer.MAX_VALUE);
        prefSize = popup.getPreferredSize();
        Placement placement = determinePlacement(originalPlacement, prefSize, aboveCursorHeight, belowCursorHeight);
        
        Rectangle popupBounds = null;
        
        for(;;) { // do one or two passes
            popup.putClientProperty(Placement.class, placement);

            int maxHeight = (placement == Above || placement == AbovePreferred) ? aboveCursorHeight : belowCursorHeight;
            int height = Math.min(prefSize.height, maxHeight);
            popup.setSize(width, height);
            popupBounds = popup.getBounds();
            
            Placement updatedPlacement = (Placement)popup.getClientProperty(Placement.class);

            if (updatedPlacement != placement) { // popup does not fit with the orig placement
                if (placement == AbovePreferred && updatedPlacement == null) {
                    placement = Below;
                    continue;
                    
                } else if (placement == BelowPreferred && updatedPlacement == null) {
                    placement = Above;
                    continue;
                }
            }
            
            if (updatedPlacement == null) {
                popupBounds = null;
            }
            
            break;
        }
        
        if (popupBounds != null) {
            if (placement == FixedPoint) {
                popupBounds.x = cursorBounds.x;
                popupBounds.y = cursorBounds.y;
            } else {
                //place popup according to caret position and Placement
                popupBounds.x = Math.min(cursorBounds.x, viewWidth - popupBounds.width);

                popupBounds.y = (placement == Above || placement == AbovePreferred)
                    ? (aboveCursorHeight - popupBounds.height)
                    : belowCursorY;
            }
        }

        return popupBounds;
    }

    protected static Rectangle computeBounds(
        JComponent popup,
        int viewWidth, 
        int viewHeight, 
        Rectangle cursorBounds, 
        Placement placement) 
    {
        return computeBounds(popup, viewWidth, viewHeight, cursorBounds, placement, ViewPortBounds);
    }    

    private static Placement determinePlacement(Placement placement, Dimension prefSize, int aboveCursorHeight, int belowCursorHeight) {
        // Resolve *Preferred placements first
        if (placement == AbovePreferred) {
            placement = (prefSize.height <= aboveCursorHeight) ? Above : Largest;
        } else if (placement == BelowPreferred) {
            placement = (prefSize.height <= belowCursorHeight) ? Below : Largest;
        }
        
        // Resolve Largest placement
        if (placement == Largest) {
            placement = (aboveCursorHeight < belowCursorHeight) ? Below : Above;
        }
        
        return placement;
    }
    
    /** Popup's key filter */
    private final class PopupKeyListener implements KeyListener{
        
        public @Override void keyTyped(KeyEvent e) {
            // no-op
        }

        public @Override void keyReleased(KeyEvent e) {
            // no-op
        }
        
        public @Override void keyPressed(KeyEvent e){
            if (e != null && popup != null && popup.isShowing()) {
                
                // get popup's registered keyboard actions
                ActionMap am = popup.getActionMap();
                InputMap  im = popup.getInputMap();
                
                // check whether popup registers keystroke
                KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
                Object obj = im.get(ks);
                LOG.log(Level.FINE, "Keystroke for event {0}: {1}; action-map-key={2}", new Object [] { e, ks, obj }); //NOI18N
                if (obj != null && 
                    !obj.equals("tooltip-no-action") && obj.equals("tooltip-hide-action") //NOI18N ignore ToolTipSupport installed actions
                ) {
                    // if yes, gets the popup's action for this keystroke, perform it 
                    // and consume key event
                    Action action = am.get(obj);
                    LOG.log(Level.FINE, "Popup component''s action: {0}, {1}", new Object [] { action, action != null ? action.getValue(Action.NAME) : null }); //NOI18N

                    if (action != null) {
                        action.actionPerformed(null);
                        e.consume();
                        return;
                    }
                }

                if (e.getKeyCode() != KeyEvent.VK_CONTROL &&
                    e.getKeyCode() != KeyEvent.VK_SHIFT &&
                    e.getKeyCode() != KeyEvent.VK_ALT &&
                    e.getKeyCode() != KeyEvent.VK_ALT_GRAPH &&
                    e.getKeyCode() != KeyEvent.VK_META
                ) {
                    // hide tooltip if any was shown
                    Utilities.getEditorUI(textComponent).getToolTipSupport().setToolTipVisible(false);
                }
            }
        }
    } // End of PopupKeyListener class
    
    private final class TextComponentListener extends ComponentAdapter {
        public @Override void componentHidden(ComponentEvent evt) {
            install(null); // hide popup
        }
    } // End of TextComponentListener class
    
    /** Placement of popup panel specification */
    public static final class Placement {
        
        private final String representation;
        
        private Placement(String representation) {
            this.representation = representation;
        }
        
        public @Override String toString() {
            return representation;
        }
        
    } // End of Placement class
    
    /** Horizontal bounds of popup panel specification */
    public static final class HorizontalBounds {
        
        private final String representation;
        
        private HorizontalBounds(String representation) {
            this.representation = representation;
        }
        
        public @Override String toString() {
            return representation;
        }
        
    } // End of HorizontalBounds class
    
}

