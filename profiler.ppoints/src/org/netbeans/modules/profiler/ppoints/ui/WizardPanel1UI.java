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

import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.modules.profiler.ppoints.ProfilingPointFactory;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "WizardPanel1UI_SelectProjectString=<Select Profiling Point project>",
    "WizardPanel1UI_PpTypeString=Profiling point &type:",
    "WizardPanel1UI_PpProjectString=Profiling point &project:",
    "WizardPanel1UI_DescriptionLabelText=Description:",
    "WizardPanel1UI_SupportedModesLabelText=Supported modes:",
    "WizardPanel1UI_MonitorModeString=Monitor",
    "WizardPanel1UI_CpuModeString=Methods",
    "WizardPanel1UI_MemoryModeString=Objects",
    "WizardPanel1UI_PpListAccessName=List of available Profiling Points",
    "WizardPanel1UI_ProjectsListAccessName=List of open projects"
})
public class WizardPanel1UI extends ValidityAwarePanel implements HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class PPointTypeTableModel extends DefaultTableModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            } else {
                return String.class;
            }
        } // TODO: enable once Scope is implemented
          //    public Class getColumnClass(int columnIndex) { return String.class; }

        public int getColumnCount() {
            return 2;
        } // TODO: enable once Scope is implemented
          //    public int getColumnCount() { return 1; }

        public int getRowCount() {
            return ppFactories.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return ppFactories[rowIndex];
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "PPointsWizardPanel1UI.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static final Icon MONITOR_ICON = Icons.getIcon(ProfilerIcons.MONITORING);
    private static final Icon CPU_ICON = Icons.getIcon(ProfilerIcons.CPU);
    private static final Icon MEMORY_ICON = Icons.getIcon(ProfilerIcons.MEMORY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DefaultTableModel ppointTypeTableModel;
    private Dimension initialMinSize;
    private JComboBox ppointProjectCombo;
    private JExtendedTable ppointTypeTable;
    private JLabel ppointDescriptionCaptionLabel;
    private JLabel ppointEffectiveCPULabel;
    private JLabel ppointEffectiveCaptionLabel;
    private JLabel ppointEffectiveMemoryLabel;
    private JLabel ppointEffectiveMonitorLabel;
    private JLabel ppointProjectLabel;
    private JLabel ppointTypeCaptionLabel;
    private JTextArea ppointDescriptionArea;
    private ProfilingPointFactory[] ppFactories = new ProfilingPointFactory[0];
    
    private boolean hasDefaultScope = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public WizardPanel1UI() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public Dimension getMinSize() {
        return initialMinSize;
    }

    public void setSelectedIndex(int index) {
        if (index == -1) {
            ppointTypeTable.clearSelection();
        } else {
            ppointTypeTable.setRowSelectionInterval(index, index);
        }
    }

    public int getSelectedIndex() {
        return ppointTypeTable.getSelectedRow();
    }

    public void setSelectedProject(Lookup.Provider project) {
        if (project != null) {
            ppointProjectCombo.setSelectedItem(project);
        }
        
        if ((project == null || !project.equals(ppointProjectCombo.getSelectedItem())) && (!Bundle.WizardPanel1UI_SelectProjectString().equals(ppointProjectCombo.getItemAt(0)))) {
            ppointProjectCombo.insertItemAt(Bundle.WizardPanel1UI_SelectProjectString(), 0);
            ppointProjectCombo.setSelectedItem(Bundle.WizardPanel1UI_SelectProjectString());
        }
    }

    public Lookup.Provider getSelectedProject() {
        if (ppointProjectCombo.getSelectedItem() instanceof Lookup.Provider) {
            return (Lookup.Provider) ppointProjectCombo.getSelectedItem();
        } else {
            return null;
        }
    }
    
    public boolean hasDefaultScope() {
        return hasDefaultScope;
    }

    public void init(final ProfilingPointFactory[] ppFactories) {
        this.ppFactories = ppFactories;
        initProjectsCombo();
        ppointTypeTableModel.fireTableDataChanged();
        ppointTypeTable.getColumnModel().getColumn(0)
                       .setMaxWidth(Math.max(ProfilingPointFactory.SCOPE_CODE_ICON.getIconWidth(),
                                             ProfilingPointFactory.SCOPE_GLOBAL_ICON.getIconWidth()) + 25); // TODO: enable once Scope is implemented

        refresh();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        ppointTypeCaptionLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(ppointTypeCaptionLabel, Bundle.WizardPanel1UI_PpTypeString());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 10);
        add(ppointTypeCaptionLabel, constraints);

        ppointTypeTableModel = new PPointTypeTableModel();
        ppointTypeTable = new JExtendedTable(ppointTypeTableModel);
        ppointTypeTable.getAccessibleContext().setAccessibleName(Bundle.WizardPanel1UI_ProjectsListAccessName());
        ppointTypeCaptionLabel.setLabelFor(ppointTypeTable);
        ppointTypeTable.setTableHeader(null);
        ppointTypeTable.setRowSelectionAllowed(true);
        ppointTypeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ppointTypeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    refresh();
                }
            });
        ppointTypeTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        ppointTypeTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        ppointTypeTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        ppointTypeTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        ppointTypeTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        ppointTypeTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        ppointTypeTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        ppointTypeTable.setDefaultRenderer(Integer.class, Utils.getScopeRenderer()); // TODO: enable once Scope is implemented
        ppointTypeTable.setDefaultRenderer(String.class, Utils.getPresenterRenderer());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 15, 12, 10);
        add(new JExtendedTablePanel(ppointTypeTable), constraints);

        ppointProjectLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(ppointProjectLabel, Bundle.WizardPanel1UI_PpProjectString());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 10);
        add(ppointProjectLabel, constraints);

        ppointProjectCombo = new JComboBox(new Object[] { Bundle.WizardPanel1UI_SelectProjectString()}) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                ;
            };
        ppointProjectLabel.getAccessibleContext().setAccessibleName(Bundle.WizardPanel1UI_ProjectsListAccessName());
        ppointProjectLabel.setLabelFor(ppointProjectCombo);
        ppointProjectCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    refresh();
                }
            });
        ppointProjectCombo.setRenderer(Utils.getProjectListRenderer());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 15, 12, 10);
        add(ppointProjectCombo, constraints);

        ppointDescriptionCaptionLabel = new JLabel(Bundle.WizardPanel1UI_DescriptionLabelText());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 10);
        add(ppointDescriptionCaptionLabel, constraints);

        ppointDescriptionArea = new JTextArea();
        ppointDescriptionArea.setOpaque(false);
        ppointDescriptionArea.setWrapStyleWord(true);
        ppointDescriptionArea.setLineWrap(true);
        ppointDescriptionArea.setEnabled(false);
        ppointDescriptionArea.setFont(UIManager.getFont("Label.font")); //NOI18N
        ppointDescriptionArea.setDisabledTextColor(UIManager.getColor("Label.foreground")); //NOI18N

        int rows = ppointDescriptionArea.getRows();
        ppointDescriptionArea.setRows(4);

        final int height = ppointDescriptionArea.getPreferredSize().height;
        ppointDescriptionArea.setRows(rows);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 15, 12, 10);

        JScrollPane ppointDescriptionAreaScroll = new JScrollPane(ppointDescriptionArea,
                                                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, height);
            }

            public Dimension getMinimumSize() {
                return new Dimension(super.getMinimumSize().width, height);
            }
        };

        ppointDescriptionAreaScroll.setBorder(BorderFactory.createEmptyBorder());
        ppointDescriptionAreaScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        ppointDescriptionAreaScroll.setOpaque(false);
        ppointDescriptionAreaScroll.getViewport().setOpaque(false);
        add(ppointDescriptionAreaScroll, constraints);

        int maxHeight = ppointDescriptionCaptionLabel.getPreferredSize().height;
        maxHeight = Math.max(maxHeight, MONITOR_ICON.getIconHeight());
        maxHeight = Math.max(maxHeight, CPU_ICON.getIconHeight());
        maxHeight = Math.max(maxHeight, MEMORY_ICON.getIconHeight());

        final int mheight = maxHeight;

        JPanel effectiveModesContainer = new JPanel(new GridBagLayout());

        ppointEffectiveCaptionLabel = new JLabel(Bundle.WizardPanel1UI_SupportedModesLabelText()) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, mheight);
                }

                public Dimension getMinimumSize() {
                    return new Dimension(super.getMinimumSize().width, mheight);
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveCaptionLabel, constraints);

        ppointEffectiveMonitorLabel = new JLabel(Bundle.WizardPanel1UI_MonitorModeString(), MONITOR_ICON, SwingConstants.LEFT);
        ppointEffectiveMonitorLabel.setVisible(false); // TODO: remove once Monitor mode will support Profiling Points
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveMonitorLabel, constraints);

        ppointEffectiveCPULabel = new JLabel(Bundle.WizardPanel1UI_CpuModeString(), CPU_ICON, SwingConstants.LEFT);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveCPULabel, constraints);

        ppointEffectiveMemoryLabel = new JLabel(Bundle.WizardPanel1UI_MemoryModeString(), MEMORY_ICON, SwingConstants.LEFT);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveMemoryLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(effectiveModesContainer, constraints);

        initialMinSize = getMinimumSize();
    }

    private void initProjectsCombo() {
        ppointProjectCombo.removeAllItems();
        
        ProfilingPointsManager manager = ProfilingPointsManager.getDefault();
        
        Lookup.Provider defaultScope = null;
        List<Lookup.Provider> providedScopes = manager.getProvidedScopes();
        for (Lookup.Provider providedScope : providedScopes) {
            if (providedScope != null) {
                if (defaultScope == null && manager.isDefaultScope(providedScope))
                    defaultScope = providedScope;
                ppointProjectCombo.addItem(providedScope);
            }
        }

        Lookup.Provider[] projects =
                ProjectUtilities.getSortedProjects(ProjectUtilities.getOpenedProjects());
        for (Lookup.Provider project : projects) {
            ppointProjectCombo.addItem(project);
        }

        hasDefaultScope = defaultScope != null;
        setSelectedProject(hasDefaultScope ? defaultScope : Utils.getCurrentProject());
    }

    private void refresh() {
        if (ppointProjectCombo.getSelectedItem() instanceof Lookup.Provider && (Bundle.WizardPanel1UI_SelectProjectString().equals(ppointProjectCombo.getItemAt(0)))) {
            ppointProjectCombo.removeItem(Bundle.WizardPanel1UI_SelectProjectString());
        }

        int selectedIndex = ppointTypeTable.getSelectedRow();

        if (selectedIndex != -1) {
            ProfilingPointFactory ppFactory = ppFactories[selectedIndex];
            ppointDescriptionArea.setText(ppFactory.getDescription());
            ppointEffectiveMonitorLabel.setVisible(ppFactory.supportsMonitor());
            ppointEffectiveCPULabel.setVisible(ppFactory.supportsCPU());
            ppointEffectiveMemoryLabel.setVisible(ppFactory.supportsMemory());
        } else {
            ppointDescriptionArea.setText(""); // NOI18N
            ppointEffectiveMonitorLabel.setVisible(false);
            ppointEffectiveCPULabel.setVisible(false);
            ppointEffectiveMemoryLabel.setVisible(false);
        }

        boolean ppointTypeSelected = selectedIndex != -1;
        boolean ppointProjectSelected = (ppointProjectCombo.getSelectedItem() != null)
                                        && ppointProjectCombo.getSelectedItem() instanceof Lookup.Provider;
        boolean isValid = ppointTypeSelected && ppointProjectSelected;

        if (isValid) {
            if (!areSettingsValid()) {
                fireValidityChanged(true);
            }
        } else {
            if (areSettingsValid()) {
                fireValidityChanged(false);
            }
        }
    }
}
