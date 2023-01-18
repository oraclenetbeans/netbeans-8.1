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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

/*
 * PackagesPanel.java
 *
 * Created on Jan 19, 2010, 3:30:05 PM
 */

package org.netbeans.modules.maven.osgi.customizer;

import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.api.customizer.support.SelectedItemsTable;
import org.netbeans.modules.maven.api.customizer.support.SelectedItemsTable.SelectedItemsTableModel;
import org.openide.util.HelpCtx;

/**
 *
 * @author dafe
 */
public class PackagesPanel extends javax.swing.JPanel implements HelpCtx.Provider {

    private final SelectedItemsTableModel tableModel;
    private final FelixExportPersister exportPersist;

    /** Creates new form PackagesPanel */
    public PackagesPanel(ModelHandle2 handle, Project prj) {
        exportPersist = new FelixExportPersister(prj, handle);
        tableModel = new SelectedItemsTableModel(exportPersist);

        initComponents();
        if (exportPersist.isIsDefined()) {
            rbCustom.setSelected(true);
            exportTable.setEnabled(true);
        } else {
            rbDefaults.setSelected(true);
            exportTable.setEnabled(false);
        }
        jScrollPane1.getViewport().setBackground(exportTable.getBackground());
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
        rbDefaults = new javax.swing.JRadioButton();
        rbCustom = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        exportTable = new SelectedItemsTable(tableModel);
        jLabel1 = new javax.swing.JLabel();

        buttonGroup1.add(rbDefaults);
        rbDefaults.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbDefaults, org.openide.util.NbBundle.getMessage(PackagesPanel.class, "PackagesPanel.rbDefaults.text")); // NOI18N
        rbDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbDefaultsActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbCustom);
        org.openide.awt.Mnemonics.setLocalizedText(rbCustom, org.openide.util.NbBundle.getMessage(PackagesPanel.class, "PackagesPanel.rbCustom.text")); // NOI18N
        rbCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbCustomActionPerformed(evt);
            }
        });

        exportTable.setEnabled(false);
        jScrollPane1.setViewportView(exportTable);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(PackagesPanel.class, "PackagesPanel.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(rbDefaults)
                                    .addComponent(rbCustom))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane1)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbDefaults)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbCustom)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void rbDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbDefaultsActionPerformed
        exportTable.setEnabled(false);
        exportPersist.setDefault(true);
    }//GEN-LAST:event_rbDefaultsActionPerformed

    private void rbCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbCustomActionPerformed
        exportTable.setEnabled(true);
        exportPersist.setDefault(false);
    }//GEN-LAST:event_rbCustomActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTable exportTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JRadioButton rbCustom;
    private javax.swing.JRadioButton rbDefaults;
    // End of variables declaration//GEN-END:variables
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("maven_settings");
    }
}
