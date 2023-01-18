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

package org.netbeans.modules.subversion.remote.ui.wizards.urlpatternstep;

/**
 *
 * @author  Petr Kuzel
 */
public class URLPatternPanel extends javax.swing.JPanel {

    /**
     * Creates new form CheckoutPanel
     */
    public URLPatternPanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/subversion/remote/ui/wizards/urlpatternstep/Bundle"); // NOI18N
        setName(bundle.getString("CTL_Name")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_URLPattern_RepositoryHint")); // NOI18N

        jLabel4.setLabelFor(repositoryPathTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_URLPattern_RepositoryFolder")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseRepositoryButton, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_URLPattern_Browse")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(anyURLCheckBox, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_AnyUrl")); // NOI18N
        anyURLCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        anyURLCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_FolderName")); // NOI18N

        buttonGroup1.add(useFolderRadioButton);
        useFolderRadioButton.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(useFolderRadioButton, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_UseFolderName")); // NOI18N
        useFolderRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        useFolderRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));

        buttonGroup1.add(useSubfolderRadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(useSubfolderRadioButton, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_UseSubfolderName")); // NOI18N
        useSubfolderRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        useSubfolderRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "CTL_Preview")); // NOI18N

        depthComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" }));

        org.openide.awt.Mnemonics.setLocalizedText(previewLabel, " ");
        previewLabel.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Nb.ScrollPane.Border.color")));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(anyURLCheckBox)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(previewLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(repositoryPathTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseRepositoryButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(useFolderRadioButton)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(useSubfolderRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(depthComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 286, Short.MAX_VALUE)))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(repositoryPathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseRepositoryButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(anyURLCheckBox)
                .addGap(32, 32, 32)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useFolderRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(useSubfolderRadioButton)
                    .addComponent(depthComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(previewLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel4.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSD_RepositoryFolder")); // NOI18N
        browseRepositoryButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSD_BrowseFolders")); // NOI18N
        anyURLCheckBox.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSN_AnyUrl")); // NOI18N
        anyURLCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSD_AnyUrl")); // NOI18N
        useFolderRadioButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSN_UseFolderName")); // NOI18N
        useFolderRadioButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSD_UseFolderName")); // NOI18N
        useSubfolderRadioButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSN_UseSubfolderName")); // NOI18N
        useSubfolderRadioButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSD_UseSubfolderName")); // NOI18N
        depthComboBox.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSN_DepthComboBox")); // NOI18N
        depthComboBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(URLPatternPanel.class, "ACSD_DepthComboBox")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify                     
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JCheckBox anyURLCheckBox = new javax.swing.JCheckBox();
    final javax.swing.JButton browseRepositoryButton = new javax.swing.JButton();
    private javax.swing.ButtonGroup buttonGroup1;
    final javax.swing.JComboBox depthComboBox = new javax.swing.JComboBox();
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    final javax.swing.JLabel previewLabel = new javax.swing.JLabel();
    final javax.swing.JTextField repositoryPathTextField = new javax.swing.JTextField();
    final javax.swing.JRadioButton useFolderRadioButton = new javax.swing.JRadioButton();
    final javax.swing.JRadioButton useSubfolderRadioButton = new javax.swing.JRadioButton();
    // End of variables declaration//GEN-END:variables
    
}
