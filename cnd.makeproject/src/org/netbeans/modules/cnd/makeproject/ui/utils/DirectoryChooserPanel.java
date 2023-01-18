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
package org.netbeans.modules.cnd.makeproject.ui.utils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import org.netbeans.modules.cnd.api.remote.RemoteFileUtil;
import org.netbeans.modules.cnd.makeproject.api.MakeProjectOptions;
import org.netbeans.modules.cnd.makeproject.api.ProjectSupport;
import org.netbeans.modules.cnd.makeproject.api.configurations.BooleanConfiguration;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.netbeans.modules.cnd.utils.FSPath;
import org.netbeans.modules.cnd.utils.FileFilterFactory;
import org.netbeans.modules.cnd.utils.ui.ListEditorPanel;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public class DirectoryChooserPanel extends javax.swing.JPanel implements HelpCtx.Provider, PropertyChangeListener {

    private final MyListEditorPanel myListEditorPanel;
    private final FSPath baseDir;
    private final boolean addPathPanel;
    private final BooleanConfiguration inheritValues;
    private final PropertyEditorSupport editor;
    private final boolean onlyFolders;
    private final HelpCtx helpCtx;

    public DirectoryChooserPanel(FSPath baseDir, List<String> data, boolean addPathPanel, BooleanConfiguration inheritValues, 
            String inheritText, PropertyEditorSupport editor, PropertyEnv env, boolean onlyFolders, HelpCtx helpCtx) {
        this.baseDir = baseDir;
        this.addPathPanel = addPathPanel;
        this.inheritValues = inheritValues;
        this.editor = editor;
        this.onlyFolders = onlyFolders;
        this.helpCtx = helpCtx;
        initComponents();
        myListEditorPanel = new MyListEditorPanel(data);
        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        listPanel.add(myListEditorPanel, gridBagConstraints);
        if (inheritValues != null) {
            inheritEditorPane.setFont(UIManager.getFont("Label.font")); //NOI18N
            inheritEditorPane.setBackground(inheritPanel.getBackground());
            inheritEditorPane.setText(inheritText);
            inheritEditorPane.setCaretPosition(0);
            setPreferredSize(new java.awt.Dimension(450, 330));
            inheritCheckBox.setSelected(inheritValues.getValue());
        } else {
            remove(inheritPanel);
            //setPreferredSize(new java.awt.Dimension(450, 350));
            setPreferredSize(new java.awt.Dimension(450, 270));
        }

        env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        env.addPropertyChangeListener(this);

        // Accessibility
        inheritCheckBox.getAccessibleContext().setAccessibleDescription(getString("INHERIT_CHECKBOX_AD"));
    }

    public void setInstructionsText(String txt) {
        //instructionsTextArea.setText(txt);
    }

    private List<String> getListData() {
        return myListEditorPanel.getListData();
    }

    private Object getPropertyValue() throws IllegalStateException {
        return new ArrayList<>(getListData());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PropertyEnv.PROP_STATE.equals(evt.getPropertyName()) && evt.getNewValue() == PropertyEnv.STATE_VALID) {
            editor.setValue(getPropertyValue());
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return helpCtx;
//        return new HelpCtx("RuntimeSearchDirectories"); // NOI18N
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        listPanel = new javax.swing.JPanel();
        inheritPanel = new javax.swing.JPanel();
        panel = new javax.swing.JPanel();
        inheritLabel = new javax.swing.JLabel();
        inheritCheckBox = new javax.swing.JCheckBox();
        scrollPane = new javax.swing.JScrollPane();
        inheritEditorPane = new javax.swing.JEditorPane();

        setPreferredSize(new java.awt.Dimension(323, 223));
        setLayout(new java.awt.GridBagLayout());

        listPanel.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        add(listPanel, gridBagConstraints);

        inheritPanel.setLayout(new java.awt.BorderLayout());

        panel.setLayout(new java.awt.GridBagLayout());

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/makeproject/ui/utils/Bundle"); // NOI18N
        inheritLabel.setText(bundle.getString("INHERITED_VALUES_LBL")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        panel.add(inheritLabel, gridBagConstraints);

        inheritCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(inheritCheckBox, bundle.getString("INHERIT_CHECKBOX_LBL")); // NOI18N
        inheritCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inheritCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        panel.add(inheritCheckBox, gridBagConstraints);

        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        inheritEditorPane.setEditable(false);
        scrollPane.setViewportView(inheritEditorPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panel.add(scrollPane, gridBagConstraints);

        inheritPanel.add(panel, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        add(inheritPanel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void inheritCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inheritCheckBoxActionPerformed
        inheritValues.setValue(inheritCheckBox.isSelected());
    }//GEN-LAST:event_inheritCheckBoxActionPerformed

    private class MyListEditorPanel extends ListEditorPanel.MultipleChoiseListEditorPanel<String> {

        public MyListEditorPanel(List<String> objects) {
            super(objects);
            getDefaultButton().setVisible(false);
        }

        @Override
        public Collection<String> addSeveralAction() {
            final String chooser_key = "makeproject.DirectoryChooser"; //NOI18N
            final ExecutionEnvironment env = FileSystemProvider.getExecutionEnvironment(baseDir.getFileSystem());
            String seed = RemoteFileUtil.getCurrentChooserFile(chooser_key, env);
            if (seed == null) {
                seed = baseDir.getPath();
            }
            JFileChooser fileChooser;
            if (DirectoryChooserPanel.this.onlyFolders) {
                fileChooser = RemoteFileUtil.createFileChooser(env, getString("ADD_DIRECTORY_DIALOG_TITLE"), getString("ADD_DIRECTORY_BUTTON_TXT"),
                        JFileChooser.DIRECTORIES_ONLY, null, seed, true);
            } else {
                fileChooser = RemoteFileUtil.createFileChooser(env, getString("ADD_DIRECTORY_OR_FILE_DIALOG_TITLE"), getString("ADD_DIRECTORY_OR_FILE_BUTTON_TXT"),
                        JFileChooser.FILES_AND_DIRECTORIES, null, seed, true);
                fileChooser.addChoosableFileFilter(FileFilterFactory.getHeaderSourceFileFilter());
                fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
            }
            PathPanel pathPanel = null;
            if (addPathPanel) {
                pathPanel = new PathPanel();
            }
            fileChooser.setAccessory(pathPanel);
            fileChooser.setMultiSelectionEnabled(true);
            int ret = fileChooser.showOpenDialog(this);
            if (ret == JFileChooser.CANCEL_OPTION) {
                return null;
            }
            final File[] selectedFiles = fileChooser.getSelectedFiles();
            List<String> result = new ArrayList<>();
            for (File selectedFile : selectedFiles) {
                String itemPath = CndPathUtilities.naturalizeSlashes(selectedFile.getPath());
                RemoteFileUtil.setCurrentChooserFile(chooser_key, selectedFile.isFile() ? selectedFile.getParentFile().getPath() : itemPath, env);
                itemPath = ProjectSupport.toProperPath(
                        baseDir,
                        itemPath,
                        MakeProjectOptions.getPathMode());
                itemPath = CndPathUtilities.naturalizeSlashes(baseDir.getFileSystem(), itemPath);
                itemPath = CndPathUtilities.normalizeSlashes(itemPath);
                result.add(itemPath);
            }
            return result;
        }

        @Override
        public String getListLabelText() {
            return getString("DIRECTORIES_LABEL_TXT");
        }

        @Override
        public char getListLabelMnemonic() {
            return getString("DIRECTORIES_LABEL_MN").charAt(0);
        }

        @Override
        public String getAddButtonText() {
            return getString("ADD_BUTTON_TXT");
        }

        @Override
        public char getAddButtonMnemonics() {
            return getString("ADD_BUTTON_MN").charAt(0);
        }

        @Override
        public String getRenameButtonText() {
            return getString("EDIT_BUTTON_TXT");
        }

        @Override
        public char getRenameButtonMnemonics() {
            return getString("EDIT_BUTTON_MN").charAt(0);
        }

        @Override
        public String getDownButtonText() {
            return getString("DOWN_BUTTON_TXT");
        }

        @Override
        public char getDownButtonMnemonics() {
            return getString("DOWN_BUTTON_MN").charAt(0);
        }

        @Override
        public String copyAction(String o) {
            return o;
        }

        @Override
        public void editAction(String o, int i) {
            String s = o;

            NotifyDescriptor.InputLine notifyDescriptor = new NotifyDescriptor.InputLine(getString("EDIT_DIALOG_LABEL_TXT"), getString("EDIT_DIALOG_TITLE_TXT"));
            notifyDescriptor.setInputText(s);
            DialogDisplayer.getDefault().notify(notifyDescriptor);
            if (notifyDescriptor.getValue() != NotifyDescriptor.OK_OPTION) {
                return;
            }
            String newS = notifyDescriptor.getInputText().trim();
            replaceElement(o, newS, i);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox inheritCheckBox;
    private javax.swing.JEditorPane inheritEditorPane;
    private javax.swing.JLabel inheritLabel;
    private javax.swing.JPanel inheritPanel;
    private javax.swing.JPanel listPanel;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    private static String getString(String key) {
        return NbBundle.getMessage(DirectoryChooserPanel.class, key);
    }
}
