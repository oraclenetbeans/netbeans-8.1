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

package org.netbeans.lib.profiler.ui.components.treetable;

import java.awt.event.MouseWheelEvent;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreePath;


/**
 * A subclass of JPanel that provides additional fuctionality for displaying JTreeTable.
 * JTreeTablePanel provides JScrollPane for displaying JTreeTable and JScrollBar for JTree
 * column of JTreeTable if necessary.
 *
 * @author Jiri Sedlacek
 */
public class JTreeTablePanel extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //-----------------------------------------------------------------------
    // Custom TreeTable Viewport
    private static class CustomTreeTableViewport extends JViewport {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JTableHeader tableHeader;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CustomTreeTableViewport(JTreeTable treeTable) {
            super();
            setView(treeTable);
            setBackground(treeTable.getBackground());
            this.tableHeader = treeTable.getTableHeader();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (UIConstants.SHOW_TABLE_VERTICAL_GRID) {
                paintVerticalLines(g);
            }
        }

        private void paintVerticalLines(Graphics g) {
            Component view = getView();
            int linesTop = view == null ? 0 : view.getHeight();
            int linesBottom = getHeight() - 1;
            if (linesTop > 0 && linesTop <= linesBottom) {
                g.setColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
                int columnCount = tableHeader.getColumnModel().getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    Rectangle cellRect = tableHeader.getHeaderRect(i);
                    int cellX = cellRect.x + cellRect.width - 1;
                    g.drawLine(cellX, linesTop, cellX, linesBottom);
                }
            }
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    CustomTreeTableViewport treeTableViewport;
    protected JPanel scrollBarPanel;
    protected JScrollBar scrollBar;
    protected JScrollPane treeTableScrollPane;
    protected JTreeTable treeTable;

    private int invisibleRowsCount = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of JTreeTablePanel */
    public JTreeTablePanel(JTreeTable treeTable) {
        super(new BorderLayout());
        this.treeTable = treeTable;

        initComponents();
        hookHeaderColumnResize();
        hookScrollBarValueChange();
        hookTreeCollapsedExpanded();

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        updateScrollBar(true);
                    }
                }
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void clearBorders() {
        treeTableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        treeTableScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
    }
    
    public void setCorner(String key, java.awt.Component corner) {
        treeTableScrollPane.setCorner(key, corner);
    }

    public JScrollPane getScrollPane() {
        return treeTableScrollPane;
    }

    private void hookHeaderColumnResize() {
        treeTable.getTableHeader().getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent e) {
                treeTableViewport.repaint();
            }

            public void columnMoved(TableColumnModelEvent e) {
                treeTableViewport.repaint();
            }

            public void columnRemoved(TableColumnModelEvent e) {
                treeTableViewport.repaint();
            }

            public void columnMarginChanged(ChangeEvent e) {
                treeTableViewport.repaint();
                updateScrollBar(true);
            }

            public void columnSelectionChanged(ListSelectionEvent e) {}
        });
    }

    private void hookScrollBarValueChange() {
        scrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(final AdjustmentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        treeTable.setTreeCellOffsetX(e.getValue());
                        if (!e.getValueIsAdjusting()) updateScrollBar(false);
                    }
                });
            }
        });
    }

    private void hookTreeCollapsedExpanded() {
        treeTable.getTree().addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent event) {
                updateSB();
            }
            public void treeExpanded(TreeExpansionEvent event) {
                updateSB();
            }
            private void updateSB() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { updateScrollBar(false); }
                });
            }
        });
    }

    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder());

        treeTableScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        treeTableViewport = new CustomTreeTableViewport(treeTable);
        treeTableScrollPane.setViewport(treeTableViewport);
        // Enable vertical scrollbar only if needed
        final JScrollBar vScrollBar = treeTableScrollPane.getVerticalScrollBar();
        vScrollBar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Component c = treeTableViewport.getView();
                vScrollBar.setEnabled(JTreeTablePanel.this.isEnabled() &&
                vScrollBar.getVisibleAmount() < vScrollBar.getMaximum());
            }
        });
        vScrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!e.getValueIsAdjusting()) updateScrollBar(false);
            }
        });
        scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
        scrollBar.setUnitIncrement(10);
        scrollBarPanel = new JPanel(new BorderLayout());
        scrollBarPanel.add(scrollBar, BorderLayout.WEST);
        treeTable.setTreeCellOffsetX(0);
        scrollBarPanel.setVisible(false);
        scrollBar.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(scrollBar, e);
            }
        });
        
        MouseWheelListener[] listeners = treeTableScrollPane.getMouseWheelListeners();
        if (listeners != null && listeners.length == 1) {
            final MouseWheelListener listener = listeners[0];
            treeTableScrollPane.removeMouseWheelListener(listener);
            treeTableScrollPane.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (onlyShift(e) && treeTable.columnAtPoint(e.getPoint()) == 0) {
                        scroll(scrollBar, e);
                    } else {
                        listener.mouseWheelMoved(e);
                    }
                    treeTable.mouseWheelMoved(e);
                }
            });
        }

        add(treeTableScrollPane, BorderLayout.CENTER);
        add(scrollBarPanel, BorderLayout.SOUTH);
    }
    
    private static void scroll(final JScrollBar scroller, final MouseWheelEvent event) {
        if (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int unitsToScroll = event.getUnitsToScroll();
            int direction = unitsToScroll < 0 ? -1 : 1;
            if (unitsToScroll != 0) {
                int increment = scroller.getUnitIncrement(direction);
                int oldValue = scroller.getValue();
                int newValue = oldValue + increment * unitsToScroll;
                newValue = Math.max(Math.min(newValue, scroller.getMaximum() -
                           scroller.getVisibleAmount()), scroller.getMinimum());
                if (oldValue != newValue) scroller.setValue(newValue);
            }
            event.consume();
        }
    }
    
    private static boolean onlyShift(MouseEvent e) {
        return e.isShiftDown() && !(e.isAltDown() || e.isAltGraphDown() ||
                                    e.isControlDown() || e.isMetaDown());
    }

    private void updateScrollBar(boolean updateWidth) {
        if (!isShowing()) return;

        boolean refreshScrollBar = false;

        JTree tree = treeTable.getTree();
        Point viewPos = treeTableViewport.getViewPosition();
        int viewHeight = treeTableViewport.getHeight();
        TreePath firstVisiblePath = tree.getClosestPathForLocation(viewPos.x, viewPos.y);
        TreePath lastVisiblePath =  tree.getClosestPathForLocation(viewPos.x, viewPos.y + viewHeight - 1);
        int firstVisibleRow = tree.getRowForPath(firstVisiblePath);
        int lastVisibleRow = tree.getRowForPath(lastVisiblePath);

        if (firstVisibleRow < 0) return;

        Rectangle size = new Rectangle();
        for (int row = firstVisibleRow; row <= lastVisibleRow; row++)
            size.add(tree.getRowBounds(row));

        int treeWidth = size.width + 3; // +3 means extra right margin
        int columnWidth = treeTable.getColumnModel().getColumn(0).getWidth();
        int treeOffset = treeTable.getTreeCellOffsetX();
        int maximum = Math.max(treeWidth - columnWidth, treeOffset);

        if (scrollBarPanel.isVisible() && maximum <= 0) {
            int firstInvisibleRow = lastVisibleRow + 1;
            int lastInvisibleRow = Math.min(lastVisibleRow + getInvisibleRowsCount(),
                                            treeTable.getRowCount() - 1);
            if (firstInvisibleRow <= lastInvisibleRow) {
                size = new Rectangle();
                for (int row = firstInvisibleRow; row <= lastInvisibleRow; row++)
                    size.add(tree.getRowBounds(row));
                size.width += 3;
                int maximum2 = Math.max(size.width - columnWidth, treeOffset);
                if (maximum2 > 0) {
                    treeWidth = size.width;
                    maximum = maximum2;
                }
            }
        }

        if (maximum <= 0) {
            if (scrollBarPanel.isVisible()) {
                treeTable.setTreeCellOffsetX(0);
                scrollBarPanel.setVisible(false);
                refreshScrollBar = true;
            }
        } else {
            int value = treeOffset;
            int extent = treeWidth;
            if (!scrollBarPanel.isVisible()) {
                scrollBarPanel.setVisible(true);
                refreshScrollBar = true;
            }
            scrollBar.setValues(value, extent, 0, maximum + extent);
        }

        if (updateWidth) {
            Dimension dim = scrollBar.getPreferredSize();
            dim.width = treeTable.getColumnModel().getColumn(0).getWidth();
            scrollBar.setPreferredSize(dim);
            scrollBar.setBlockIncrement((int)((float)scrollBar.getModel().getExtent() * 0.95f));
            refreshScrollBar = true;
        }

        if (refreshScrollBar) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    scrollBar.invalidate();
                    validate();
                    repaint();
                }
            });
        }
    }

    private int getInvisibleRowsCount() {
        if (invisibleRowsCount == -1)
            invisibleRowsCount =
                    (int)Math.ceil((float)scrollBar.getPreferredSize().height /
                                   (float)treeTable.getRowHeight());

        return invisibleRowsCount;
    }
    
}
