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
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.repository.register;

import java.net.URISyntaxException;
import javax.swing.JButton;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryPreferences;
import static org.netbeans.modules.maven.repository.register.Bundle.*;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author  Anuradha
 */
public class RepositoryRegisterUI extends javax.swing.JPanel {

    private boolean modify = false;
    private boolean alreadyFilled = false;

    /** Creates new form RepositoryRegisterUI */
    public RepositoryRegisterUI() {
        initComponents();
        validateInfo();
    }
    
     @Messages(
        "LBL_Repo_EDIT=Ok"
     )
     public RepositoryRegisterUI(RepositoryInfo info) {
        this();
        modify(info);
        validateInfo();
        btnOK.setText(LBL_Repo_EDIT());
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
        btnOK = new javax.swing.JButton();
        lblHeader = new javax.swing.JLabel();
        lblRepoId = new javax.swing.JLabel();
        txtRepoId = new javax.swing.JTextField();
        lblRepoName = new javax.swing.JLabel();
        txtRepoName = new javax.swing.JTextField();
        lblRepoUrl = new javax.swing.JLabel();
        txtRepoUrl = new javax.swing.JTextField();
        lblValidate = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        btnOK.setText(org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "CMB_Repo_ADD", new Object[] {})); // NOI18N
        btnOK.setEnabled(false);

        lblHeader.setText(org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "LBL_Repo_Register_Header", new Object[] {})); // NOI18N

        lblRepoId.setLabelFor(txtRepoId);
        org.openide.awt.Mnemonics.setLocalizedText(lblRepoId, org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "LBL_Repo_ID", new Object[] {})); // NOI18N

        txtRepoId.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtRepoIdKeyReleased(evt);
            }
        });

        lblRepoName.setLabelFor(txtRepoName);
        org.openide.awt.Mnemonics.setLocalizedText(lblRepoName, org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "LBL_Repo_Name", new Object[] {})); // NOI18N

        txtRepoName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtRepoNameKeyReleased(evt);
            }
        });

        lblRepoUrl.setLabelFor(txtRepoUrl);
        org.openide.awt.Mnemonics.setLocalizedText(lblRepoUrl, org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "LBL_Repo_URL", new Object[] {})); // NOI18N

        txtRepoUrl.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtRepoUrlKeyReleased(evt);
            }
        });

        lblValidate.setForeground(new java.awt.Color(204, 0, 0));

        jLabel1.setText(org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "RepositoryRegisterUI.jLabel1.text")); // NOI18N
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblRepoId)
                    .addComponent(lblRepoName)
                    .addComponent(lblRepoUrl))
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtRepoId)
                    .addComponent(txtRepoName)
                    .addComponent(txtRepoUrl))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(lblValidate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(456, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(lblHeader, javax.swing.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblHeader)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRepoId)
                    .addComponent(txtRepoId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRepoName)
                    .addComponent(txtRepoName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRepoUrl)
                    .addComponent(txtRepoUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 57, Short.MAX_VALUE)
                .addComponent(lblValidate, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        txtRepoId.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "RepositoryRegisterUI.txtRepoId.AccessibleContext.accessibleDescription")); // NOI18N
        txtRepoName.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "RepositoryRegisterUI.txtRepoName.AccessibleContext.accessibleDescription")); // NOI18N
        txtRepoUrl.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RepositoryRegisterUI.class, "RepositoryRegisterUI.txtRepoUrl.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void selectRemoteRepo(boolean checkValidity) {
    txtRepoUrl.setEnabled(true);
    if (checkValidity) {
        validateInfo();
    }
}

private void txtRepoIdKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRepoIdKeyReleased
    validateInfo();
}//GEN-LAST:event_txtRepoIdKeyReleased

private void txtRepoNameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRepoNameKeyReleased
    validateInfo();
}//GEN-LAST:event_txtRepoNameKeyReleased

private void txtRepoUrlKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRepoUrlKeyReleased
    validateInfo();
}//GEN-LAST:event_txtRepoUrlKeyReleased

    private void modify(RepositoryInfo info) {
        modify = true;
        txtRepoId.setEnabled(false);
        txtRepoId.setText(info.getId());
        txtRepoName.setText(info.getName());
        if (info.isLocal()) {
            throw new IllegalStateException( "cannot modify local repository definition");
        } else if (info.isRemoteDownloadable()) {
           txtRepoUrl.setText(info.getRepositoryUrl());
        }
    }
    
    public RepositoryInfo getRepositoryInfo() throws URISyntaxException {
      return new RepositoryInfo(txtRepoId.getText().trim(),
              txtRepoName.getText().trim(),
              null,
              txtRepoUrl.getText().trim());
    }

    @Messages({
        "LBL_Repo_id_Error1=Repository Id can't be empty",
        "LBL_Repo_id_slash=Slashes (/) not allowed in Repository ID",
        "LBL_Repo_id_Error2=Repository Id already exist",
        "LBL_Repo_Name_Error1=Repository Name can't be empty",
        "LBL_Repo_Path_Error=Invalid Repository Path",
        "LBL_Repo_Url_Error=Repository URL can't be empty",
        "LBL_Repo_Url_Http=Repository URL has to be of protocol http or https"
    })
    private void validateInfo() {
        //check repo id
        String id = txtRepoId.getText().trim();
        if (id.length() == 0) {
            btnOK.setEnabled(false);
            lblValidate.setText(LBL_Repo_id_Error1());
            return;
        }
        if (id.indexOf('/') != -1) {
            btnOK.setEnabled(false);
            lblValidate.setText(LBL_Repo_id_slash());
            return;
        }
        if (!modify) {
            RepositoryInfo info = RepositoryPreferences.getInstance().getRepositoryInfoById(id);
            if (info != null && (info.isLocal() || info.isRemoteDownloadable())) {
                btnOK.setEnabled(false);
                lblValidate.setText(LBL_Repo_id_Error2());
                return;
            } else if (info != null && !alreadyFilled) {
                txtRepoUrl.setText(info.getRepositoryUrl());
                txtRepoName.setText(info.getName());
                selectRemoteRepo(false);
                alreadyFilled = true;
            }
        }

        //check repo name
        if (txtRepoName.getText().trim().length() == 0) {
            btnOK.setEnabled(false);
            lblValidate.setText(LBL_Repo_Name_Error1());
            return;
        }

        //check repo url
        String url = txtRepoUrl.getText().trim();
        if (url.length() == 0) {
            btnOK.setEnabled(false);
            lblValidate.setText(LBL_Repo_Url_Error());
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            btnOK.setEnabled(false);
            lblValidate.setText(LBL_Repo_Url_Http());
            return;
        }


        lblValidate.setText("");
        btnOK.setEnabled(true);
    }

    public JButton getButton() {
        return btnOK;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOK;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblHeader;
    private javax.swing.JLabel lblRepoId;
    private javax.swing.JLabel lblRepoName;
    private javax.swing.JLabel lblRepoUrl;
    private javax.swing.JLabel lblValidate;
    private javax.swing.JTextField txtRepoId;
    private javax.swing.JTextField txtRepoName;
    private javax.swing.JTextField txtRepoUrl;
    // End of variables declaration//GEN-END:variables

}
