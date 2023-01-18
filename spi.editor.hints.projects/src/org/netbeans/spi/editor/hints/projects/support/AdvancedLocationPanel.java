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
package org.netbeans.spi.editor.hints.projects.support;

import java.io.File;
import java.util.Objects;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.spi.editor.hints.projects.support.StandardProjectSettings.Standard;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author lahvac
 */
class AdvancedLocationPanel extends javax.swing.JPanel {
    private final Standard standard;

    public AdvancedLocationPanel(String currentHintFileLocation, Standard standard) {
        initComponents();
        this.fileLocation.setText(currentHintFileLocation);
        this.standard = standard;
        this.browse.setEnabled(standard.getProjectLocation() != null);
        this.fileLocation.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {
                enableDisable();
            }
            @Override public void removeUpdate(DocumentEvent e) {
                enableDisable();
            }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
        enableDisable();
    }
    
    public String getHintFileLocation() {
        return fileLocation.getText();
    }
    
    private void enableDisable() {
        toDefault.setEnabled(!Objects.equals(standard.getDefaultHintLocation(), fileLocation.getText()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        fileLocation = new javax.swing.JTextField();
        toDefault = new javax.swing.JButton();
        browse = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(AdvancedLocationPanel.class, "AdvancedLocationPanel.jLabel1.text")); // NOI18N

        fileLocation.setColumns(30);
        fileLocation.setText(org.openide.util.NbBundle.getMessage(AdvancedLocationPanel.class, "AdvancedLocationPanel.fileLocation.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(toDefault, org.openide.util.NbBundle.getMessage(AdvancedLocationPanel.class, "AdvancedLocationPanel.toDefault.text")); // NOI18N
        toDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toDefaultActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(browse, org.openide.util.NbBundle.getMessage(AdvancedLocationPanel.class, "AdvancedLocationPanel.browse.text")); // NOI18N
        browse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileLocation, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(toDefault)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(browse))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(fileLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toDefault)
                    .addComponent(browse))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void toDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toDefaultActionPerformed
        fileLocation.setText(standard.getDefaultHintLocation());
    }//GEN-LAST:event_toDefaultActionPerformed

    @Messages("LBL_Select=Select")
    private void browseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseActionPerformed
        JFileChooser jfc = new JFileChooser();
        
        jfc.setSelectedFile(new File(standard.getProjectLocation(), fileLocation.getText()));
        
        if (jfc.showDialog(this, Bundle.LBL_Select()) == JFileChooser.APPROVE_OPTION) {
            fileLocation.setText(standard.getProjectLocation().toURI().relativize(jfc.getSelectedFile().toURI()).getPath());
        }
    }//GEN-LAST:event_browseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browse;
    private javax.swing.JTextField fileLocation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton toDefault;
    // End of variables declaration//GEN-END:variables
}
