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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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

package org.netbeans.modules.mercurial.ui.wizards;

public final class ClonePathsPanel extends javax.swing.JPanel {
    
    /** Creates new form CloneVisualPanel2 */
    public ClonePathsPanel() {
        initComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        defaultPullPathLabel = new javax.swing.JLabel();
        defaultPushPathLabel = new javax.swing.JLabel();
        defaultValuesButton = new javax.swing.JButton();

        setName(org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "pathsPanel.Name")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "defaultLabel.Name")); // NOI18N

        defaultPullPathLabel.setLabelFor(defaultPullPathField);
        org.openide.awt.Mnemonics.setLocalizedText(defaultPullPathLabel, org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "defaultPullLabel.Name")); // NOI18N

        defaultPullPathField.setColumns(30);
        defaultPullPathField.setEditable(false);

        org.openide.awt.Mnemonics.setLocalizedText(changePullPathButton, org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "changePullPushPath.Name")); // NOI18N
        changePullPathButton.setDefaultCapable(false);

        defaultPushPathLabel.setLabelFor(defaultPushPathField);
        org.openide.awt.Mnemonics.setLocalizedText(defaultPushPathLabel, org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "defaultPushLabel.Name")); // NOI18N

        defaultPushPathField.setColumns(30);
        defaultPushPathField.setEditable(false);

        org.openide.awt.Mnemonics.setLocalizedText(changePushPathButton, org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "changePullPushPath.Name")); // NOI18N
        changePushPathButton.setDefaultCapable(false);

        org.openide.awt.Mnemonics.setLocalizedText(defaultValuesButton, org.openide.util.NbBundle.getMessage(ClonePathsPanel.class, "setDefaultValues.Name")); // NOI18N
        defaultValuesButton.setDefaultCapable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(defaultPullPathLabel)
                    .addComponent(defaultPushPathLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(defaultPullPathField)
                    .addComponent(defaultPushPathField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(changePushPathButton)
                    .addComponent(changePullPathButton)))
            .addComponent(defaultValuesButton)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(defaultPullPathLabel)
                    .addComponent(defaultPullPathField)
                    .addComponent(changePullPathButton))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(defaultPushPathLabel)
                    .addComponent(defaultPushPathField)
                    .addComponent(changePushPathButton))
                .addGap(18, 18, 18)
                .addComponent(defaultValuesButton))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JButton changePullPathButton = new javax.swing.JButton();
    final javax.swing.JButton changePushPathButton = new javax.swing.JButton();
    final javax.swing.JTextField defaultPullPathField = new javax.swing.JTextField();
    private javax.swing.JLabel defaultPullPathLabel;
    final javax.swing.JTextField defaultPushPathField = new javax.swing.JTextField();
    private javax.swing.JLabel defaultPushPathLabel;
    javax.swing.JButton defaultValuesButton;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
    
}

