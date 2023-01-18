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
package org.netbeans.modules.team.commons.treelist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.plaf.ListUI;
import org.openide.util.Utilities;

/**
 * Special list displaying model items that provide custom renderers. Also paints
 * mouse-over effect.
 *
 * @author S. Aubrecht
 */
public final class SelectionList extends JList<ListNode> {

    static final int INSETS_LEFT = 5;
    static final int INSETS_TOP = 5;
    static final int INSETS_BOTTOM = 5;
    static final int INSETS_RIGHT = 5;

    private static final int MAX_VISIBLE_ROWS = 10;
    
    private int mouseOverRow = -1;

    private final RendererImpl renderer = new RendererImpl();
    static final int ROW_HEIGHT = Math.max(16, new JLabel("X").getPreferredSize().height); // NOI18N
    private final ListListener nodeListener;

    public SelectionList() {
        setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        setBorder( BorderFactory.createEmptyBorder() );
        setOpaque( false );
        setBackground( new Color(0,0,0,0) );
        setFixedCellHeight(ROW_HEIGHT + INSETS_TOP + INSETS_BOTTOM + 2);
        setCellRenderer(renderer);
        setVisibleRowCount( MAX_VISIBLE_ROWS );
        
        ToolTipManager.sharedInstance().registerComponent(this);

        addFocusListener( new FocusAdapter() {

            @Override
            public void focusGained( FocusEvent e ) {
                if( getSelectedIndex() < 0 && isShowing() && getModel().getSize() > 0 ) {
                    setSelectedIndex( 0 );
                }
            }
        });
        
        nodeListener = new ListListener() {
            @Override
            public void contentChanged(ListNode node) {
                int index = ((DefaultListModel) getModel()).indexOf(node);
                if (index >= 0) {
                    repaintRow(index);
                }
            }
            @Override
            public void contentSizeChanged(ListNode node) {
                // resize the whole dialog in case this is in one
                SelectionListModel model = (SelectionListModel) getModel();
                int index = model.indexOf(node);
                if (index >= 0) {
                    model.fireContentsChanged(node, index, index);
                    Container p = SelectionList.this;
                    while((p = p.getParent()) != null) {
                        if(p instanceof JDialog) {
                            invalidate();
                            revalidate();
                            ((JDialog)p).pack();
                            return;
                        }
                    }
                }
            }
        };

        MouseAdapter adapter = new MouseAdapter() {

            @Override
            public void mouseEntered( MouseEvent e ) {
                mouseMoved( e );
            }

            @Override
            public void mouseExited( MouseEvent e ) {
                setMouseOver( -1 );
            }

            @Override
            public void mouseMoved( MouseEvent e ) {
                int row = locationToIndex( e.getPoint() );
                setMouseOver( row );
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.isConsumed()) {
                    return;
                }
                int index = locationToIndex(e.getPoint());
                if (index < 0 || index >= getModel().getSize() || e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                if( e.getClickCount() > 1  ) {
                    Object value = getModel().getElementAt(index);
                    if( value instanceof ListNode ) {
                        ListNode node = (ListNode) value;

                        if( null != node ) {
                            ActionListener al = node.getDefaultAction();
                            if (null != al) {
                                al.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.paramString()));
                            }
                        }
                    }
                } else {
                    int row = locationToIndex( e.getPoint() );
                    if( row >= 0 && row == getSelectedIndex() ) {
                        e.consume();
                        clearSelection();
                        setSelectedIndex( row );
                    }
                }
            }
        };

        addMouseMotionListener( adapter );

        addMouseListener( adapter );
    }

    int getMouseOverRow() {
        return mouseOverRow;
    }

    private void setMouseOver( int newRow ) {
        int oldRow = mouseOverRow;
        mouseOverRow = newRow;
        repaintRow( oldRow );
        repaintRow( mouseOverRow );
    }

    private void repaintRow( int row ) {
        if( row >= 0 && row < getModel().getSize() ) {
            Rectangle rect = getCellBounds( row, row );
            if( null != rect )
                repaint( rect );
        }
    }

    @Override
    public void setUI(ListUI ui) {
        super.setUI(new SelectionListUI());
    }

    /**
     * Show popup menu from actions provided by node at given index (if any).
     *
     * @param rowIndex
     * @param location
     */
    private void showPopupMenuAt(int rowIndex, Point location) {
        ListNode node = getModel().getElementAt(rowIndex);
        Action[] actions = node.getPopupActions();

        if (null == actions || actions.length == 0) {
            return;
        }
        JPopupMenu popup = Utilities.actionsToPopup(actions, this);
        popup.show(this, location.x, location.y);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event != null) {
            Point p = event.getPoint();
            int index = locationToIndex(p);
            ListCellRenderer<? super ListNode> r = getCellRenderer();
            Rectangle cellBounds;

            if (index != -1 && r != null && (cellBounds =
                    getCellBounds(index, index)) != null
                    && cellBounds.contains(p.x, p.y)) {
                ListSelectionModel lsm = getSelectionModel();
                Component rComponent = r.getListCellRendererComponent(
                        this, getModel().getElementAt(index), index,
                        lsm.isSelectedIndex(index),
                        (hasFocus() && (lsm.getLeadSelectionIndex()
                        == index)));

                if (rComponent instanceof JComponent) {
                    rComponent.setBounds(cellBounds);
                    rComponent.doLayout();
                    MouseEvent newEvent;

                    p.translate(-cellBounds.x, -cellBounds.y);
                    newEvent = new MouseEvent(rComponent, event.getID(),
                            event.getWhen(),
                            event.getModifiers(),
                            p.x, p.y, event.getClickCount(),
                            event.isPopupTrigger());

                    String tip = ((JComponent) rComponent).getToolTipText(
                            newEvent);

                    if (tip != null) {
                        return tip;
                    }
                }
            }
        }
        return super.getToolTipText();
    }

    @Override
    public int getVisibleRowCount() {
        return Math.min( MAX_VISIBLE_ROWS, getModel().getSize() );
    }

    public void setItems( List<ListNode> items ) {
        SelectionListModel model = new SelectionListModel();
        for( ListNode item : items ) {
            model.addElement( item );
            item.setListener(nodeListener);
        }
        setModel( model );
    }    

    static class RendererImpl extends DefaultListCellRenderer {

        public RendererImpl() {
        }

        @Override
        public Component getListCellRendererComponent( JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
            if (!(value instanceof ListNode)) {
                //shoudln't happen
                return new JLabel();
            }
            if( list instanceof SelectionList ) {
                isSelected |= index == ((SelectionList)list).getMouseOverRow();
            }
            ListNode node = (ListNode) value;
            int rowHeight = list.getFixedCellHeight();
            int rowWidth = list.getWidth();
            JScrollPane scroll = ( JScrollPane ) SwingUtilities.getAncestorOfClass( JScrollPane.class, list);
            if( null != scroll )
                rowWidth = scroll.getViewport().getWidth();
            Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
            Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();

            return node.getListRenderer(foreground, background, isSelected, cellHasFocus, rowHeight, rowWidth);
        }
    }

    private static class SelectionListUI extends AbstractListUI {

        @Override
        boolean showPopupAt( int rowIndex, Point location ) {
            if (!(list instanceof SelectionList)) {
                return false;
            }

            ((SelectionList) list).showPopupMenuAt(rowIndex, location);
            return true;
        }
    }
    
    private static class SelectionListModel extends DefaultListModel<ListNode> {
        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1); 
        }
    }
    
}
