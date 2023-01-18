/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

/*
 * RevertCommitPanel.java
 *
 * Created on May 25, 2011, 2:28:01 PM
 */
package org.netbeans.modules.git.remote.ui.revert;

import org.netbeans.modules.git.remote.ui.repository.RevisionDialog;

/**
 *
 * @author ondra
 */
public class RevertCommitPanel extends javax.swing.JPanel {
    private final RevisionDialog revisionPanel;

    /** Creates new form RevertCommitPanel */
    public RevertCommitPanel (RevisionDialog revisionPanel) {
        this.revisionPanel = revisionPanel;
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        org.netbeans.modules.git.remote.ui.repository.RevisionDialog revisionDialog1 = revisionPanel;
        jScrollPane1 = new javax.swing.JScrollPane();
        lblCommitMessage = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RevertCommitPanel.class, "RevertCommitPanel.jLabel1.text")); // NOI18N

        cbCommit.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cbCommit, org.openide.util.NbBundle.getMessage(RevertCommitPanel.class, "RevertCommitPanel.cbCommit.text")); // NOI18N
        cbCommit.setToolTipText(org.openide.util.NbBundle.getMessage(RevertCommitPanel.class, "RevertCommitPanel.cbCommit.TTtext")); // NOI18N

        txtCommitMessage.setColumns(20);
        txtCommitMessage.setLineWrap(true);
        txtCommitMessage.setRows(5);
        txtCommitMessage.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtCommitMessage);

        lblMessageWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/git/remote/resources/icons/info.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lblMessageWarning, org.openide.util.NbBundle.getMessage(RevertCommitPanel.class, "RevertCommitPanel.lblMessageWarning.text")); // NOI18N

        lblCommitMessage.setLabelFor(txtCommitMessage);
        org.openide.awt.Mnemonics.setLocalizedText(lblCommitMessage, org.openide.util.NbBundle.getMessage(RevertCommitPanel.class, "RevertCommitPanel.lblCommitMessage.text")); // NOI18N

        javax.swing.GroupLayout commitMessagePanelLayout = new javax.swing.GroupLayout(commitMessagePanel);
        commitMessagePanel.setLayout(commitMessagePanelLayout);
        commitMessagePanelLayout.setHorizontalGroup(
            commitMessagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commitMessagePanelLayout.createSequentialGroup()
                .addGroup(commitMessagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblCommitMessage)
                    .addComponent(lblMessageWarning))
                .addContainerGap(24, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE)
        );
        commitMessagePanelLayout.setVerticalGroup(
            commitMessagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commitMessagePanelLayout.createSequentialGroup()
                .addComponent(lblCommitMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMessageWarning)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(cbCommit)
                            .addComponent(revisionDialog1, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(commitMessagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(14, 14, 14))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(revisionDialog1, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cbCommit)
                .addGap(4, 4, 4)
                .addComponent(commitMessagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JCheckBox cbCommit = new javax.swing.JCheckBox();
    final javax.swing.JPanel commitMessagePanel = new javax.swing.JPanel();
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCommitMessage;
    final javax.swing.JLabel lblMessageWarning = new javax.swing.JLabel();
    final javax.swing.JTextArea txtCommitMessage = new javax.swing.JTextArea();
    // End of variables declaration//GEN-END:variables
}
