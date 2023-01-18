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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.EnhancedTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProfilingPointsWindowUI_AllProjectsString=All Projects",
    "ProfilingPointsWindowUI_ProjectLabelText=Pr&oject:",
    "ProfilingPointsWindowUI_InclSubprojCheckboxText=in&clude open subprojects",
    "ProfilingPointsWindowUI_AddButtonToolTip=Add Profiling Point",
    "ProfilingPointsWindowUI_RemoveButtonToolTip=Delete Profiling Point(s)",
    "ProfilingPointsWindowUI_EditButtonToolTip=Edit Profiling Point",
    "ProfilingPointsWindowUI_DisableButtonToolTip=Enable/Disable Profiling Point(s)",
    "ProfilingPointsWindowUI_ShowSourceItemText=Show in Source",
    "ProfilingPointsWindowUI_ShowStartItemText=Show Start in Source",
    "ProfilingPointsWindowUI_ShowEndItemText=Show End in Source",
    "ProfilingPointsWindowUI_ShowReportItemText=Show Report",
    "ProfilingPointsWindowUI_EnableItemText=Enable",
    "ProfilingPointsWindowUI_DisableItemText=Disable",
    "ProfilingPointsWindowUI_EnableDisableItemText=Enable/Disable",
    "ProfilingPointsWindowUI_EditItemText=Edit",
    "ProfilingPointsWindowUI_RemoveItemText=Delete",
    "ProfilingPointsWindowUI_ScopeColumnName=Scope",
    "ProfilingPointsWindowUI_ProjectColumnName=Project",
    "ProfilingPointsWindowUI_PpColumnName=Profiling Point",
    "ProfilingPointsWindowUI_ResultsColumnName=Results",
    "ProfilingPointsWindowUI_ScopeColumnToolTip=Profiling Point scope",
    "ProfilingPointsWindowUI_ProjectColumnToolTip=Project for which the Profiling Point is defined",
    "ProfilingPointsWindowUI_PpColumnToolTip=Profiling Point",
    "ProfilingPointsWindowUI_ResultsColumnToolTip=Data or current state of the Profiling Point",
    "ProfilingPointsWindowUI_NoStartDefinedMsg=No start point defined for this Profiling Point",
    "ProfilingPointsWindowUI_NoEndDefinedMsg=No end point defined for this Profiling Point"
})
public class ProfilingPointsWindowUI extends JPanel implements ActionListener, ListSelectionListener, PropertyChangeListener,
                                                               MouseListener, MouseMotionListener, KeyListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final Icon PPOINT_ADD_ICON = Icons.getIcon(ProfilingPointsIcons.ADD);
    private static final Icon PPOINT_REMOVE_ICON = Icons.getIcon(ProfilingPointsIcons.REMOVE);
    private static final Icon PPOINT_EDIT_ICON = Icons.getIcon(ProfilingPointsIcons.EDIT);
    private static final Icon PPOINT_ENABLE_DISABLE_ICON = Icons.getIcon(ProfilingPointsIcons.ENABLE_DISABLE);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected String[] columnNames;
    protected TableCellRenderer[] columnRenderers;
    protected String[] columnToolTips;
    protected Class[] columnTypes;
    protected int[] columnWidths;
    protected boolean sortOrder; // Defines the sorting order (ascending or descending)
    protected int sortBy; // Defines sorting criteria (concrete values provided in subclasses)
    private ExtendedTableModel profilingPointsTableModel;
    private JButton addButton;
    private JButton disableButton;
    private JButton editButton;
    private JButton removeButton;
    private JCheckBox dependenciesCheckbox;
    private JComboBox projectsCombo;
    private JExtendedTable profilingPointsTable;
    private JLabel projectLabel;
    private JMenuItem disableItem;
    private JMenuItem editItem;
    private JMenuItem enableDisableItem;
    private JMenuItem enableItem;
    private JMenuItem removeItem;
    private JMenuItem showEndInSourceItem;
    private JMenuItem showInSourceItem;
    private JMenuItem showReportItem;
    private JMenuItem showStartInSourceItem;
    private JPopupMenu profilingPointsPopup;
    private ProfilingPoint[] profilingPoints = new ProfilingPoint[0];
    private boolean profilingInProgress = false;
    private int initialSortingColumn;
    private int minProfilingPointColumnWidth; // minimal width of Profiling Point column

    private boolean internalComboChange;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProfilingPointsWindowUI() {
        setDefaultSorting();
        initColumnsData();
        initComponents();
        updateProjectsCombo();
        notifyProfilingStateChanged();
        ProfilingPointsManager.getDefault().addPropertyChangeListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // NOTE: this method only sets sortBy and sortOrder, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_ASC);
    }

    public Lookup.Provider getSelectedProject() {
        return (projectsCombo.getSelectedItem() instanceof Lookup.Provider) ? (Lookup.Provider) projectsCombo.getSelectedItem() : null;
    }

    // NOTE: this method only sets sortBy and sortOrder, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        if (sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            initialSortingColumn = sColumn;
            sortBy = getSortBy(initialSortingColumn);
            sortOrder = sOrder;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == projectsCombo) {
            if (!internalComboChange) refreshProfilingPoints();
        } else if (e.getSource() == addButton) {
            SystemAction.get(InsertProfilingPointAction.class).performAction(getSelectedProject());
        } else if (e.getSource() == removeButton) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();
            ProfilingPoint[] selectedProfilingPoints = new ProfilingPoint[selectedRows.length];

            for (int i = 0; i < selectedRows.length; i++) {
                selectedProfilingPoints[i] = getProfilingPointAt(selectedRows[i]);
            }

            ProfilingPointsManager.getDefault().removeProfilingPoints(selectedProfilingPoints);
        } else if (e.getSource() == editButton) {
            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(profilingPointsTable.getSelectedRow());
            selectedProfilingPoint.customize(false, false);
        } else if (e.getSource() == disableButton) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            for (int i : selectedRows) {
                ProfilingPoint selectedProfilingPoint = getProfilingPointAt(i);
                selectedProfilingPoint.setEnabled(!selectedProfilingPoint.isEnabled());
                repaint();
            }
        } else if (e.getSource() == showInSourceItem) {
            CodeProfilingPoint selectedProfilingPoint = (CodeProfilingPoint) getProfilingPointAt(profilingPointsTable
                                                                                                                                                                                                                                                                       .getSelectedRow());
            Utils.openLocation(selectedProfilingPoint.getLocation());
        } else if (e.getSource() == showStartInSourceItem) {
            CodeProfilingPoint.Paired selectedProfilingPoint = (CodeProfilingPoint.Paired) getProfilingPointAt(profilingPointsTable
                                                                                                               .getSelectedRow());
            CodeProfilingPoint.Location location = selectedProfilingPoint.getStartLocation();

            if (location == null) {
                ProfilerDialogs.displayWarning(
                        Bundle.ProfilingPointsWindowUI_NoStartDefinedMsg());
            } else {
                Utils.openLocation(location);
            }
        } else if (e.getSource() == showEndInSourceItem) {
            CodeProfilingPoint.Paired selectedProfilingPoint = (CodeProfilingPoint.Paired) getProfilingPointAt(profilingPointsTable
                                                                                                               .getSelectedRow());
            CodeProfilingPoint.Location location = selectedProfilingPoint.getEndLocation();

            if (location == null) {
                ProfilerDialogs.displayWarning(
                        Bundle.ProfilingPointsWindowUI_NoEndDefinedMsg());
            } else {
                Utils.openLocation(location);
            }
        } else if (e.getSource() == showReportItem) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            if (selectedRows.length == 0) {
                return;
            }

            for (int selectedRow : selectedRows) {
                ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
                selectedProfilingPoint.showResults(null);
            }
        } else if (e.getSource() == enableItem) {
            int selectedRow = profilingPointsTable.getSelectedRow();

            if (selectedRow == -1) {
                return;
            }

            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            selectedProfilingPoint.setEnabled(true);
        } else if (e.getSource() == disableItem) {
            int selectedRow = profilingPointsTable.getSelectedRow();

            if (selectedRow == -1) {
                return;
            }

            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            selectedProfilingPoint.setEnabled(false);
        } else if (e.getSource() == enableDisableItem) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            if (selectedRows.length == 0) {
                return;
            }

            for (int selectedRow : selectedRows) {
                ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
                selectedProfilingPoint.setEnabled(!selectedProfilingPoint.isEnabled());
            }
        } else if (e.getSource() == editItem) {
            int selectedRow = profilingPointsTable.getSelectedRow();

            if (selectedRow == -1) {
                return;
            }

            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            selectedProfilingPoint.customize(false, false);
        } else if (e.getSource() == removeItem) {
            deletePPs();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            if (selectedRows.length != 0) {
                Rectangle rowBounds = profilingPointsTable.getCellRect(selectedRows[0], 1, true);
                showProfilingPointsPopup(e.getComponent(), rowBounds.x + 20,
                                         rowBounds.y + (profilingPointsTable.getRowHeight() / 2));
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            deletePPs();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON1_MASK) {
            int clickedRow = profilingPointsTable.rowAtPoint(e.getPoint());

            if ((clickedRow != -1) && (e.getClickCount() == 2)) {
                ProfilingPoint profilingPoint = getProfilingPointAt(clickedRow);

                if (profilingPoint instanceof CodeProfilingPoint) {
                    Utils.openLocation(((CodeProfilingPoint) profilingPoint).getLocation());
                }
            }
        } else if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            int clickedRow = profilingPointsTable.rowAtPoint(e.getPoint());
            int selectedRowCount = profilingPointsTable.getSelectedRowCount();
            if ((clickedRow != -1) && (selectedRowCount != 0)) {
                if (selectedRowCount == 1)
                    profilingPointsTable.setRowSelectionInterval(clickedRow, clickedRow);
                showProfilingPointsPopup(e.getComponent(), e.getX(), e.getY());

                return;
            }
        }

        dispatchResultsRendererEvent(e);
    }

    public void mouseDragged(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mouseEntered(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mouseExited(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mouseMoved(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mousePressed(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            int clickedRow = profilingPointsTable.rowAtPoint(e.getPoint());

            if (clickedRow != -1) {
                int[] selectedRows = profilingPointsTable.getSelectedRows();

                if (selectedRows.length == 0) {
                    profilingPointsTable.setRowSelectionInterval(clickedRow, clickedRow);
                } else {
                    boolean changeSelection = true;

                    for (int selectedRow : selectedRows) {
                        if (selectedRow == clickedRow) {
                            changeSelection = false;
                        }
                    }

                    if (changeSelection) {
                        profilingPointsTable.setRowSelectionInterval(clickedRow, clickedRow);
                    }
                }
            }
        }

        dispatchResultsRendererEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void notifyProfilingStateChanged() {
        profilingInProgress = ProfilingPointsManager.getDefault().isProfilingSessionInProgress();
        updateButtons();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == ProfilingPointsManager.PROPERTY_PROJECTS_CHANGED) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updateProjectsCombo(); } // also refreshes profiling points
            });
        } else if (evt.getPropertyName() == ProfilingPointsManager.PROPERTY_PROFILING_POINTS_CHANGED) {
            refreshProfilingPoints();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        updateButtons();
    }

    protected void initColumnsData() {
        minProfilingPointColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        EnhancedTableCellRenderer scopeRenderer = Utils.getScopeRenderer();
        EnhancedTableCellRenderer projectRenderer = Utils.getProjectRenderer();
        EnhancedTableCellRenderer profilingPointRenderer = Utils.getPresenterRenderer();

        columnNames = new String[] { 
            Bundle.ProfilingPointsWindowUI_ScopeColumnName(), 
            Bundle.ProfilingPointsWindowUI_ProjectColumnName(), 
            Bundle.ProfilingPointsWindowUI_PpColumnName(), 
            Bundle.ProfilingPointsWindowUI_ResultsColumnName() 
        };
        columnToolTips = new String[] { 
            Bundle.ProfilingPointsWindowUI_ScopeColumnToolTip(), 
            Bundle.ProfilingPointsWindowUI_ProjectColumnToolTip(), 
            Bundle.ProfilingPointsWindowUI_PpColumnToolTip(), 
            Bundle.ProfilingPointsWindowUI_ResultsColumnToolTip()
        };
        columnTypes = new Class[] { Integer.class, Lookup.Provider.class, ProfilingPoint.class, ProfilingPoint.ResultsRenderer.class };
        columnRenderers = new TableCellRenderer[] { scopeRenderer, projectRenderer, profilingPointRenderer, null // dynamic
                          };
        columnWidths = new int[] { 50, 165, -1, // dynamic
            200 };
    }

    private void setColumnsData() {
        TableColumnModel colModel = profilingPointsTable.getColumnModel();
        colModel.getColumn(2).setPreferredWidth(minProfilingPointColumnWidth);

        //    colModel.getColumn(1).setPreferredWidth(minProfilingPointColumnWidth); // TODO: revert use column 2 once Scope is enabled
        int index;

        for (int i = 0; i < colModel.getColumnCount(); i++) {
            index = profilingPointsTableModel.getRealColumn(i);
            colModel.getColumn(i).setPreferredWidth((index == 2) ? minProfilingPointColumnWidth : columnWidths[index]);
            //      colModel.getColumn(i).setPreferredWidth(index == 1 ? minProfilingPointColumnWidth : columnWidths[index]); // TODO: revert use column 2 once Scope is enabled
            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
    }

    private ProfilingPoint getProfilingPointAt(int row) {
        return (ProfilingPoint) profilingPointsTable.getValueAt(row, 0);
    }

    private int getSortBy(int column) {
        switch (column) {
            case 0:
                return ProfilingPointsManager.SORT_BY_SCOPE;
            case 1:
                return ProfilingPointsManager.SORT_BY_PROJECT;
            case 2:
                return ProfilingPointsManager.SORT_BY_NAME; // TODO: revert use column 2 once Scope is enabled
            case 3:
                return ProfilingPointsManager.SORT_BY_RESULTS; // TODO: revert use column 3 once Scope is enabled
            default:
                return CommonConstants.SORTING_COLUMN_DEFAULT;
        }
    }

    private void createProfilingPointsTable() {
        profilingPointsTableModel = new ExtendedTableModel(new SortableTableModel() {
                public String getColumnName(int col) {
                    return columnNames[col];
                }

                public int getRowCount() {
                    return profilingPoints.length;
                }

                public int getColumnCount() {
                    return columnNames.length;
                }

                public Class getColumnClass(int col) {
                    return columnTypes[col];
                }

                public Object getValueAt(int row, int col) {
                    return profilingPoints[row];
                }

                public String getColumnToolTipText(int col) {
                    return columnToolTips[col];
                }

                public void sortByColumn(int column, boolean order) {
                    sortBy = getSortBy(column);
                    sortOrder = order;
                    refreshProfilingPoints();
                }

                /**
                 * @param column The table column index
                 * @return Initial sorting for the specified column - if true, ascending, if false descending
                 */
                public boolean getInitialSorting(int column) {
                    return true;
                }
            });

        profilingPointsTable = new JExtendedTable(profilingPointsTableModel) {
                public TableCellRenderer getCellRenderer(int row, int column) {
                    if (getColumnClass(column) == ProfilingPoint.ResultsRenderer.class) {
                        return getProfilingPointAt(row).getResultsRenderer();
                    } else {
                        return super.getCellRenderer(row, column);
                    }
                }

                public void doLayout() {
                    int columnsWidthsSum = 0;
                    int realFirstColumn = -1;

                    int index;

                    for (int i = 0; i < profilingPointsTableModel.getColumnCount(); i++) {
                        index = profilingPointsTableModel.getRealColumn(i);

                        if (index == 2) {
                            //          if (index == 1) { // TODO: revert use column 2 once Scope is enabled
                            realFirstColumn = i;
                        } else {
                            columnsWidthsSum += getColumnModel().getColumn(i).getPreferredWidth();
                        }
                    }

                    if (realFirstColumn != -1) {
                        getColumnModel().getColumn(realFirstColumn)
                            .setPreferredWidth(Math.max(getWidth() - columnsWidthsSum, minProfilingPointColumnWidth));
                    }

                    super.doLayout();
                }
                ;
            };
        //    profilingPointsTable.getAccessibleContext().setAccessibleName(TABLE_ACCESS_NAME);
        profilingPointsTableModel.setTable(profilingPointsTable);
        profilingPointsTableModel.setInitialSorting(initialSortingColumn, sortOrder);
        profilingPointsTable.setRowSelectionAllowed(true);
        profilingPointsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        profilingPointsTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        profilingPointsTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        profilingPointsTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        profilingPointsTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        profilingPointsTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        profilingPointsTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        profilingPointsTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        profilingPointsTable.getSelectionModel().addListSelectionListener(this);
        profilingPointsTable.addMouseListener(this);
        profilingPointsTable.addMouseMotionListener(this);
        profilingPointsTable.addKeyListener(this);

        setColumnsData();
    }

    private void deletePPs() {
        int[] selectedRows = profilingPointsTable.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        List<ProfilingPoint> pointsToRemove = new ArrayList();

        for (int selectedRow : selectedRows) {
            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            pointsToRemove.add(selectedProfilingPoint);
        }

        for (ProfilingPoint pointToRemove : pointsToRemove) {
            ProfilingPointsManager.getDefault().removeProfilingPoint(pointToRemove);
        }
    }

    private void dispatchResultsRendererEvent(MouseEvent e) {
        int column = profilingPointsTable.columnAtPoint(e.getPoint());

        if (column != 3) {
            //    if (column != 2) { // TODO: revert to 3 once Scope is enabled
            profilingPointsTable.setCursor(Cursor.getDefaultCursor()); // Workaround for forgotten Hand cursor from HTML renderer, TODO: fix it!

            return;
        }

        int row = profilingPointsTable.rowAtPoint(e.getPoint());

        if (row == -1) {
            return;
        }

        ProfilingPoint profilingPoint = getProfilingPointAt(row);
        ProfilingPoint.ResultsRenderer resultsRenderer = profilingPoint.getResultsRenderer();
        Rectangle cellRect = profilingPointsTable.getCellRect(row, column, true);
        MouseEvent mouseEvent = new MouseEvent(profilingPointsTable, e.getID(), e.getWhen(), e.getModifiers(),
                                               e.getX() - cellRect.x, e.getY() - cellRect.y, e.getClickCount(),
                                               e.isPopupTrigger(), e.getButton());
        resultsRenderer.dispatchMouseEvent(mouseEvent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        ProfilerToolbar toolbar = ProfilerToolbar.create(true);

        projectLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(projectLabel, Bundle.ProfilingPointsWindowUI_ProjectLabelText());
        projectLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        projectsCombo = new JComboBox(new Object[] { Bundle.ProfilingPointsWindowUI_AllProjectsString() }) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }

                public Dimension getPreferredSize() {
                    return new Dimension(200, super.getPreferredSize().height);
                }
                ;
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                ;
            };
        projectLabel.setLabelFor(projectsCombo);
        projectsCombo.addActionListener(this);
        projectsCombo.setRenderer(Utils.getProjectListRenderer());
        toolbar.add(projectLabel);
        toolbar.add(projectsCombo);

        if (ProfilingPointsUIHelper.get().displaySubprojectsOption()) {
            dependenciesCheckbox = new JCheckBox();
            dependenciesCheckbox.setOpaque(false);
            UIUtils.addBorder(dependenciesCheckbox, BorderFactory.createEmptyBorder(0, 4, 0, 3));
            org.openide.awt.Mnemonics.setLocalizedText(dependenciesCheckbox, Bundle.ProfilingPointsWindowUI_InclSubprojCheckboxText());
            dependenciesCheckbox.setSelected(ProfilerIDESettings.getInstance().getIncludeProfilingPointsDependencies());
            toolbar.add(dependenciesCheckbox);
            dependenciesCheckbox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ProfilerIDESettings.getInstance().setIncludeProfilingPointsDependencies(dependenciesCheckbox.isSelected());
                        refreshProfilingPoints();
                    }
                });
        }

        toolbar.addSeparator();

        addButton = new JButton(PPOINT_ADD_ICON);
        addButton.setToolTipText(Bundle.ProfilingPointsWindowUI_AddButtonToolTip());
        addButton.addActionListener(this);
        toolbar.add(addButton);

        removeButton = new JButton(PPOINT_REMOVE_ICON);
        removeButton.setToolTipText(Bundle.ProfilingPointsWindowUI_RemoveButtonToolTip());
        removeButton.addActionListener(this);
        toolbar.add(removeButton);

        toolbar.addSeparator();

        editButton = new JButton(PPOINT_EDIT_ICON);
        editButton.setToolTipText(Bundle.ProfilingPointsWindowUI_EditButtonToolTip());
        editButton.addActionListener(this);
        toolbar.add(editButton);

        disableButton = new JButton(PPOINT_ENABLE_DISABLE_ICON);
        disableButton.setToolTipText(Bundle.ProfilingPointsWindowUI_DisableButtonToolTip());
        disableButton.addActionListener(this);
        toolbar.add(disableButton);

        createProfilingPointsTable();

        JExtendedTablePanel tablePanel = new JExtendedTablePanel(profilingPointsTable);
        tablePanel.clearBorders();

        showInSourceItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_ShowSourceItemText());
        showInSourceItem.setFont(showInSourceItem.getFont().deriveFont(Font.BOLD));
        showInSourceItem.addActionListener(this);
        showStartInSourceItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_ShowStartItemText());
        showStartInSourceItem.setFont(showInSourceItem.getFont().deriveFont(Font.BOLD));
        showStartInSourceItem.addActionListener(this);
        showEndInSourceItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_ShowEndItemText());
        showEndInSourceItem.addActionListener(this);
        showReportItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_ShowReportItemText());
        showReportItem.addActionListener(this);
        enableItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_EnableItemText());
        enableItem.addActionListener(this);
        disableItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_DisableItemText());
        disableItem.addActionListener(this);
        enableDisableItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_EnableDisableItemText());
        enableDisableItem.addActionListener(this);
        editItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_EditItemText());
        editItem.addActionListener(this);
        removeItem = new JMenuItem(Bundle.ProfilingPointsWindowUI_RemoveItemText());
        removeItem.addActionListener(this);

        profilingPointsPopup = new JPopupMenu();
        profilingPointsPopup.add(showInSourceItem);
        profilingPointsPopup.add(showStartInSourceItem);
        profilingPointsPopup.add(showEndInSourceItem);
        profilingPointsPopup.add(showReportItem);
        profilingPointsPopup.addSeparator();
        profilingPointsPopup.add(editItem);
        profilingPointsPopup.add(enableItem);
        profilingPointsPopup.add(disableItem);
        profilingPointsPopup.add(enableDisableItem);
        profilingPointsPopup.addSeparator();
        profilingPointsPopup.add(removeItem);

        add(toolbar.getComponent(), BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
    }

    private void refreshProfilingPoints() {
        int[] selectedRows = profilingPointsTable.getSelectedRows();
        ProfilingPoint[] selectedProfilingPoints = new ProfilingPoint[selectedRows.length];

        for (int i = 0; i < selectedRows.length; i++) {
            selectedProfilingPoints[i] = getProfilingPointAt(selectedRows[i]);
        }

        List<ProfilingPoint> sortedProfilingPoints = ProfilingPointsManager.getDefault()
                                                                           .getSortedProfilingPoints(getSelectedProject(),
                                                                                                     sortBy, sortOrder);
        profilingPoints = sortedProfilingPoints.toArray(new ProfilingPoint[sortedProfilingPoints.size()]);
        profilingPointsTableModel.fireTableDataChanged();

        if (selectedProfilingPoints.length > 0) {
            profilingPointsTable.selectRowsByInstances(selectedProfilingPoints, 0, true);
        }

        repaint();
    }

    private void showProfilingPointsPopup(Component source, int x, int y) {
        int[] selectedRows = profilingPointsTable.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        boolean singleSelection = selectedRows.length == 1;
        ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRows[0]);

        showInSourceItem.setVisible(!singleSelection || selectedProfilingPoint instanceof CodeProfilingPoint.Single);
        showInSourceItem.setEnabled(singleSelection);

        showStartInSourceItem.setVisible(singleSelection && selectedProfilingPoint instanceof CodeProfilingPoint.Paired);

        showEndInSourceItem.setVisible(singleSelection && selectedProfilingPoint instanceof CodeProfilingPoint.Paired);

        showReportItem.setEnabled(true);

        enableItem.setVisible(singleSelection && !selectedProfilingPoint.isEnabled());
        enableItem.setEnabled(!profilingInProgress);

        disableItem.setVisible(singleSelection && selectedProfilingPoint.isEnabled());
        disableItem.setEnabled(!profilingInProgress);

        enableDisableItem.setVisible(!singleSelection);
        enableDisableItem.setEnabled(!profilingInProgress);

        editItem.setEnabled(singleSelection && !profilingInProgress);

        removeItem.setEnabled(!profilingInProgress);

        profilingPointsPopup.show(source, x, y);
    }

    private void updateButtons() {
        int[] selectedRows = profilingPointsTable.getSelectedRows();
        addButton.setEnabled(!profilingInProgress);

        if (selectedRows.length == 0) {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
            disableButton.setEnabled(false);
        } else if (selectedRows.length == 1) {
            editButton.setEnabled(!profilingInProgress);
            removeButton.setEnabled(!profilingInProgress);
            disableButton.setEnabled(!profilingInProgress);
        } else {
            editButton.setEnabled(false);
            removeButton.setEnabled(!profilingInProgress);
            disableButton.setEnabled(!profilingInProgress);
        }
    }

    private void updateProjectsCombo() {
        Lookup.Provider[] projects = ProjectUtilities.getSortedProjects(ProjectUtilities.getOpenedProjects());
        List items = new ArrayList(projects.length + 1);
        items.addAll(Arrays.asList(projects));

        items.add(0, Bundle.ProfilingPointsWindowUI_AllProjectsString());

        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) projectsCombo.getModel();
        Object selectedItem = projectsCombo.getSelectedItem();

        internalComboChange = true;

        comboModel.removeAllElements();

        for (int i = 0; i < items.size(); i++) {
            comboModel.addElement(items.get(i));
        }

        if ((selectedItem != null) && (comboModel.getIndexOf(selectedItem) != -1)) {
            projectsCombo.setSelectedItem(selectedItem);
        } else {
            projectsCombo.setSelectedItem(Bundle.ProfilingPointsWindowUI_AllProjectsString());
        }

        internalComboChange = false;

        refreshProfilingPoints();
    }
}
