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

package org.netbeans.modules.diff.options;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.util.NbBundle;
import org.openide.filesystems.FileUtil;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.modules.diff.DiffModuleConfig;
import org.netbeans.modules.diff.Utils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Mutex;
import org.openide.util.RequestProcessor;

/**
 * Diff Options panel.
 *
 * @author  Maros Sandor
 */
@OptionsPanelController.Keywords(keywords={"diff"}, location=OptionsDisplayer.ADVANCED, tabTitle="Diff")
class DiffOptionsPanel extends javax.swing.JPanel implements ActionListener, DocumentListener {

    private boolean isChanged;
    private final RequestProcessor.Task t = Utils.createParallelTask(new CheckCommand(true));
    
    /** Creates new form DiffOptionsPanel */
    public DiffOptionsPanel() {
        initComponents();
        internalDiff.addActionListener(this);
        externalDiff.addActionListener(this);
        ignoreWhitespace.addActionListener(this);
        ignoreAllWhitespace.addActionListener(this);
        ignoreCase.addActionListener(this);
        externalCommand.getDocument().addDocumentListener(this);
        refreshComponents();
    }

    void refreshComponents() {
        ignoreWhitespace.setEnabled(internalDiff.isSelected());
        ignoreAllWhitespace.setEnabled(internalDiff.isSelected());
        ignoreCase.setEnabled(internalDiff.isSelected());
        jLabel1.setEnabled(externalDiff.isSelected());
        externalCommand.setEnabled(externalDiff.isSelected());
        browseCommand.setEnabled(externalDiff.isSelected());
        checkExternalCommand();
    }

    public JTextField getExternalCommand() {
        return externalCommand;
    }

    public JRadioButton getExternalDiff() {
        return externalDiff;
    }

    public JCheckBox getIgnoreWhitespace() {
        return ignoreWhitespace;
    }

    public JCheckBox getIgnoreInnerWhitespace() {
        return ignoreAllWhitespace;
    }

    public JCheckBox getIgnoreCase() {
        return ignoreCase;
    }

    public JRadioButton getInternalDiff() {
        return internalDiff;
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }

    public boolean isChanged() {
        return isChanged;
    }    
    
    private void fireChanged() {
        boolean useInteralDiff = DiffModuleConfig.getDefault().isUseInteralDiff();
        if (internalDiff.isSelected() != useInteralDiff) {
            isChanged = true;
            return;
        }
        if (externalDiff.isSelected() == useInteralDiff) {
            isChanged = true;
            return;
        }
        if(ignoreWhitespace.isSelected() != DiffModuleConfig.getDefault().getOptions().ignoreLeadingAndtrailingWhitespace) {
            isChanged = true;
            return;
        }
        if(ignoreAllWhitespace.isSelected() != DiffModuleConfig.getDefault().getOptions().ignoreInnerWhitespace) {
            isChanged = true;
            return;
        }
        if(ignoreCase.isSelected() != DiffModuleConfig.getDefault().getOptions().ignoreCase) {
            isChanged = true;
            return;
        }
        if(!externalCommand.getText().equals(DiffModuleConfig.getDefault().getPreferences().get(DiffModuleConfig.PREF_EXTERNAL_DIFF_COMMAND, "diff {0} {1}"))) { // NOI18N
            checkExternalCommand();
            isChanged = true;
            return;
        }
        isChanged = false;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        internalDiff = new javax.swing.JRadioButton();
        ignoreWhitespace = new javax.swing.JCheckBox();
        ignoreAllWhitespace = new javax.swing.JCheckBox();
        ignoreCase = new javax.swing.JCheckBox();
        externalDiff = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        externalCommand = new javax.swing.JTextField();
        browseCommand = new javax.swing.JButton();
        lblWarningCommand = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 0, 5));

        buttonGroup1.add(internalDiff);
        org.openide.awt.Mnemonics.setLocalizedText(internalDiff, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "jRadioButton1.text")); // NOI18N
        internalDiff.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.openide.awt.Mnemonics.setLocalizedText(ignoreWhitespace, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "jCheckBox1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(ignoreAllWhitespace, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "DiffOptionsPanel.ignoreAllWhitespace.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(ignoreCase, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "DiffOptionsPanel.ignoreCase.text")); // NOI18N

        buttonGroup1.add(externalDiff);
        org.openide.awt.Mnemonics.setLocalizedText(externalDiff, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "jRadioButton2.text")); // NOI18N
        externalDiff.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel1.setLabelFor(externalCommand);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "jLabel1.text")); // NOI18N

        externalCommand.setText(org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "jTextField1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseCommand, org.openide.util.NbBundle.getMessage(DiffOptionsPanel.class, "jButton1.text")); // NOI18N
        browseCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseCommandActionPerformed(evt);
            }
        });

        lblWarningCommand.setForeground(javax.swing.UIManager.getDefaults().getColor("nb.errorForeground"));
        org.openide.awt.Mnemonics.setLocalizedText(lblWarningCommand, " "); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(internalDiff)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel1)
                        .addComponent(externalDiff)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ignoreWhitespace)
                    .addComponent(ignoreAllWhitespace)
                    .addComponent(ignoreCase)
                    .addComponent(lblWarningCommand)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(externalCommand, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseCommand)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(internalDiff)
                    .addComponent(ignoreWhitespace))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ignoreAllWhitespace)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ignoreCase)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(externalDiff)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(browseCommand)
                    .addComponent(externalCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblWarningCommand)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseCommandActionPerformed
        String execPath = externalCommand.getText();
        File oldFile = FileUtil.normalizeFile(new File(execPath));
        JFileChooser fileChooser = new AccessibleJFileChooser(NbBundle.getMessage(DiffOptionsPanel.class, "ACSD_BrowseFolder"), oldFile); // NOI18N
        fileChooser.setDialogTitle(NbBundle.getMessage(DiffOptionsPanel.class, "BrowseFolder_Title")); // NOI18N
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.showDialog(this, NbBundle.getMessage(DiffOptionsPanel.class, "BrowseFolder_OK")); // NOI18N
        File f = fileChooser.getSelectedFile();
        if (f != null) {
            externalCommand.setText(f.getAbsolutePath() + " {0} {1}");
        }
    }//GEN-LAST:event_browseCommandActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseCommand;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField externalCommand;
    private javax.swing.JRadioButton externalDiff;
    private javax.swing.JCheckBox ignoreAllWhitespace;
    private javax.swing.JCheckBox ignoreCase;
    private javax.swing.JCheckBox ignoreWhitespace;
    private javax.swing.JRadioButton internalDiff;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblWarningCommand;
    // End of variables declaration//GEN-END:variables

    @Override
    public void actionPerformed (ActionEvent e) {
        fireChanged();
        refreshComponents();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        fireChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        fireChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        fireChanged();
    }

    void checkExternalCommand () {
        if (getExternalDiff().isSelected()) {
            String cmd = getExternalCommand().getText();
            checkExternalCommand(cmd);
        } else {
            Mutex.EVENT.readAccess(new Runnable() {

                @Override
                public void run () {
                    lblWarningCommand.setText(" "); //NOI18N
                }
                
            });
        }
    }

    private void checkExternalCommand (final String cmd) {
        this.cmd = cmd;
        final boolean inAwt = EventQueue.isDispatchThread();
        if (inAwt) {
            t.schedule(250);
        } else {
            new CheckCommand(false).run();
        }
    }
    
    private String cmd;
    private final class CheckCommand implements Runnable {
        private final boolean inPanel;
        
        private CheckCommand (boolean notifyInPanel) {
            this.inPanel = notifyInPanel;
        }
        
        @Override
        public void run () {
            String toCheck = cmd;
            boolean invalid = false;
            if (toCheck.trim().isEmpty()) {
                invalid = true;
            } else {
                try {
                    Process p = Runtime.getRuntime().exec(toCheck);
                    p.destroy();
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run () {
                            lblWarningCommand.setText(" "); //NOI18N
                        }
                    });
                } catch (IOException e) {
                    invalid = true;
                }
            }
            if (invalid) {
                // the command seems invalid
                if (inPanel) {
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run () {
                            lblWarningCommand.setText(NbBundle.getMessage(DiffOptionsController.class, "MSG_InvalidDiffCommand")); //NOI18N
                        }
                    });
                } else {
                    DialogDisplayer.getDefault().notifyLater(
                        new NotifyDescriptor.Message(NbBundle.getMessage(DiffOptionsController.class, "MSG_InvalidDiffCommand"), NotifyDescriptor.WARNING_MESSAGE));
                }
            }
        }
    };
}
