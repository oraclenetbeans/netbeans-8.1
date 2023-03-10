/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s): theanuradha@netbeans.org
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.actions.scm.ui;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import static org.netbeans.modules.maven.actions.scm.ui.Bundle.*;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.execute.BeanRunConfig;
import org.netbeans.modules.maven.options.MavenCommandSettings;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Anuradha G
 */
public class CheckoutUI extends javax.swing.JPanel {

    private static final File lastFolder = new File(System.getProperty("user.home")); //NOI18N

    private final JButton checkoutButton;
    private final Scm scm;
    private final MavenProject project;

    /** Creates new form CheckoutUI */
    @Messages({
        "# {0} - project name",
        "LBL_Description=<html>Check out {0} sources from repository</html>", 
        "BTN_Checkout=Check Out"})
    public CheckoutUI(MavenProject proj) {
        this.project = proj;
        this.scm = proj.getScm();
        StringBuilder buffer = new StringBuilder();
        buffer.append("<b>");//NOI18N

        buffer.append(proj.getArtifactId());
        buffer.append("</b>");//NOI18N

        buffer.append(":");//NOI18N

        buffer.append("<b>");//NOI18N

        buffer.append(proj.getVersion());
        buffer.append("</b>");//NOI18N

        initComponents();
        lblDescription.setText(LBL_Description(buffer.toString())); // NOI18N

        checkoutButton = new JButton(BTN_Checkout());//NOI18N
        //checkoutButton.setEnabled(false);//TODO validate 

        load();
        txtFolder.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateFolder();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                validateFolder();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                validateFolder();
            }
        });
        txtFolder.setText(ProjectChooser.getProjectsFolder().getAbsolutePath() + File.separator + project.getArtifactId());
    }

    @Messages({"LBL_Folder_Error=Folder not empty. The folder must be empty to continue.", "LBL_Folder=Local working copy path"})
    private void validateFolder() {

        File file = new File(txtFolder.getText().trim());
        if (file.exists() && file.list() != null && file.list().length > 0) {
            checkoutButton.setEnabled(false);
            lblFolderError.setForeground(Color.red);
            lblFolderError.setText(LBL_Folder_Error());
        } else {
            lblFolderError.setText(LBL_Folder());
            checkoutButton.setEnabled(true);
            lblFolderError.setForeground(Color.BLACK);
        }

    }

    private void load() {
        if (scm.getConnection() != null) {
            defaultConnection.setSelected(true);
            txtUrl.setText(scm.getConnection());


        } else {
            defaultConnection.setEnabled(false);

        }
        if (scm.getDeveloperConnection() != null) {
        } else {

            developerConnection.setEnabled(false);


        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        lblDescription = new javax.swing.JLabel();
        lblConnection = new javax.swing.JLabel();
        defaultConnection = new javax.swing.JRadioButton();
        developerConnection = new javax.swing.JRadioButton();
        lblLocalFolderDescription = new javax.swing.JLabel();
        txtFolder = new javax.swing.JTextField();
        btnFile = new javax.swing.JButton();
        lblLocalFolder = new javax.swing.JLabel();
        lblAuthenticationDescription = new javax.swing.JLabel();
        lblUser = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        lblPassword = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        lblActhenticationHint = new javax.swing.JLabel();
        chkPrintDebugInfo = new javax.swing.JCheckBox();
        txtUrl = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lblFolderError = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(lblDescription, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_Description")); // NOI18N

        lblConnection.setForeground(new java.awt.Color(0, 0, 102));
        org.openide.awt.Mnemonics.setLocalizedText(lblConnection, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_Connection")); // NOI18N

        buttonGroup1.add(defaultConnection);
        org.openide.awt.Mnemonics.setLocalizedText(defaultConnection, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_DefaultConnection")); // NOI18N
        defaultConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultConnectionActionPerformed(evt);
            }
        });

        buttonGroup1.add(developerConnection);
        org.openide.awt.Mnemonics.setLocalizedText(developerConnection, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_DeveloperConnection")); // NOI18N
        developerConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                developerConnectionActionPerformed(evt);
            }
        });

        lblLocalFolderDescription.setForeground(new java.awt.Color(0, 0, 102));
        org.openide.awt.Mnemonics.setLocalizedText(lblLocalFolderDescription, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_LocalFolderDescription")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnFile, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "BTN_Browse")); // NOI18N
        btnFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFileActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lblLocalFolder, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_LocalFolder")); // NOI18N

        lblAuthenticationDescription.setForeground(new java.awt.Color(0, 0, 102));
        org.openide.awt.Mnemonics.setLocalizedText(lblAuthenticationDescription, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_AuthenticationDescription")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblUser, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_User")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblPassword, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_Password")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblActhenticationHint, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_ActhenticationHint")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(chkPrintDebugInfo, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_PrintDebugInfo")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "CheckoutUI.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "CheckoutUI.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblFolderError, org.openide.util.NbBundle.getMessage(CheckoutUI.class, "LBL_Folder")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblLocalFolderDescription, javax.swing.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
                    .addComponent(chkPrintDebugInfo)
                    .addComponent(lblDescription, javax.swing.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
                    .addComponent(lblAuthenticationDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblUser)
                            .addComponent(lblPassword))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                            .addComponent(txtUser, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblActhenticationHint)
                        .addGap(166, 166, 166))
                    .addComponent(lblConnection, javax.swing.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(lblLocalFolder)
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(lblFolderError, javax.swing.GroupLayout.DEFAULT_SIZE, 586, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(txtFolder, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnFile))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(defaultConnection, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE))
                            .addComponent(txtUrl, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 566, Short.MAX_VALUE)
                            .addComponent(developerConnection, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblConnection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(developerConnection)
                    .addComponent(defaultConnection))
                .addGap(3, 3, 3)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addComponent(lblLocalFolderDescription)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnFile)
                    .addComponent(lblLocalFolder)
                    .addComponent(txtFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addComponent(lblFolderError, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAuthenticationDescription)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUser)
                    .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblActhenticationHint))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPassword)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(chkPrintDebugInfo)
                .addGap(18, 18, 18))
        );
    }// </editor-fold>//GEN-END:initComponents
    @Messages({"TIT_Choose=Choose Directory To Check Out", "LBL_Select=Select"})
    private void btnFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFileActionPerformed
        JFileChooser chooser = new JFileChooser(lastFolder);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(TIT_Choose());

        chooser.setMultiSelectionEnabled(false);
        if (txtFolder.getText().trim().length() > 0) {
            File fil = new File(txtFolder.getText().trim());
            if (fil.exists()) {
                chooser.setSelectedFile(fil);
            }
        }
        int ret = chooser.showDialog(SwingUtilities.getWindowAncestor(this), LBL_Select());
        if (ret == JFileChooser.APPROVE_OPTION) {
            txtFolder.setText(chooser.getSelectedFile().getAbsolutePath());
            txtFolder.requestFocusInWindow();
        }
    }//GEN-LAST:event_btnFileActionPerformed

    private void defaultConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultConnectionActionPerformed
        if (defaultConnection.isEnabled()) {
            txtUrl.setText(scm.getConnection());
        }
    }//GEN-LAST:event_defaultConnectionActionPerformed

    private void developerConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_developerConnectionActionPerformed
        if (developerConnection.isEnabled()) {
            txtUrl.setText(scm.getDeveloperConnection());
        }
    }//GEN-LAST:event_developerConnectionActionPerformed

    public File getCheckoutDirectory() {
        return FileUtil.normalizeFile(new File(txtFolder.getText().trim()));
    }


    public RunConfig getRunConfig() {
        BeanRunConfig brc = new BeanRunConfig();
        brc.setExecutionDirectory(getCheckoutDirectory().getParentFile());
        List<String> goals = new ArrayList<String>();
        goals.add(MavenCommandSettings.getDefault().getCommand(MavenCommandSettings.COMMAND_SCM_CHECKOUT));//NOI18N
        brc.setGoals(goals);
        brc.setTaskDisplayName(NbBundle.getMessage(CheckoutUI.class, "LBL_Checkout", project.getArtifactId() + " : " + project.getVersion()));
        brc.setExecutionName(brc.getTaskDisplayName());
        brc.setProperty("checkoutDirectory", getCheckoutDirectory().getAbsolutePath());//NOI18N
        brc.setProperty("connectionUrl", txtUrl.getText());//NOI18N
        if (txtUser.getText().trim().length() != 0) {
            brc.setProperty("username", txtUser.getText());//NOI18N
            brc.setProperty("password ", new String(txtPassword.getPassword()));//NOI18N
        }
        brc.setShowDebug(chkPrintDebugInfo.isSelected());
        brc.setShowError(chkPrintDebugInfo.isSelected());
        brc.setOffline(false);
        brc.setUpdateSnapshots(false);
        brc.setActivatedProfiles(Collections.<String>emptyList());
        brc.setInteractive(true);
        brc.setActionName("scm-checkout"); //NOI18N
        return brc;
    }

    public JButton getCheckoutButton() {
        return checkoutButton;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFile;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox chkPrintDebugInfo;
    private javax.swing.JRadioButton defaultConnection;
    private javax.swing.JRadioButton developerConnection;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblActhenticationHint;
    private javax.swing.JLabel lblAuthenticationDescription;
    private javax.swing.JLabel lblConnection;
    private javax.swing.JLabel lblDescription;
    private javax.swing.JLabel lblFolderError;
    private javax.swing.JLabel lblLocalFolder;
    private javax.swing.JLabel lblLocalFolderDescription;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblUser;
    private javax.swing.JTextField txtFolder;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtUrl;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
}
