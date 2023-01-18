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
package org.netbeans.modules.hibernate.loaders.cfg.multiview;

import java.awt.event.ActionListener;
import javax.swing.JTextField;

/**
 * 
 * 
 * @author  Dongmei Cao
 */
public class EventListenerPanel extends javax.swing.JPanel {

    private static final String[] listenerTypes = new String[]{
        "auto-flush", // NOI18N
        "merge", // NOI18N
        "create", // NOI18N
        "create-onflush", // NOI18N
        "delete", // NOI18N
        "dirty-check", // NOI18N
        "evict", // NOI18N
        "flush", // NOI18N
        "flush-entity", // NOI18N
        "load", // NOI18N
        "load-collection", // NOI18N
        "lock", // NOI18N
        "refresh", // NOI18N
        "replicate", // NOI18N
        "save-update", // NOI18N
        "save", // NOI18N
        "update", // NOI18N
        "pre-load", // NOI18N
        "pre-update", // NOI18N
        "pre-insert", // NOI18N
        "pre-delete", // NOI18N
        "post-load", // NOI18N
        "post-update", // NOI18N
        "post-insert", // NOI18N
        "post-delete", // NOI18N
        "post-commit-update", // NOI18N
        "post-commit-insert", // NOI18N
        "post-commit-delete" // NOI18N
    };

    /** Creates new form ResRefPanel */
    public EventListenerPanel() {
        initComponents();
    }

    public void initValues(String listenerClass) {
        this.listenerClassTextField.setText(listenerClass);

    }
    
    public void addBrowseClassActionListener( ActionListener listener ) {
        this.browseButton.addActionListener(listener);
    }

    public JTextField getListenerClassTextField() {
        return this.listenerClassTextField;
    }

    public String getListenerClass() {
        return this.listenerClassTextField.getText().trim();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        listenerClassLabel = new javax.swing.JLabel();
        listenerClassTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        listenerClassLabel.setLabelFor(listenerClassTextField);
        org.openide.awt.Mnemonics.setLocalizedText(listenerClassLabel, org.openide.util.NbBundle.getMessage(EventListenerPanel.class, "LBL_Listener_Class")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        add(listenerClassLabel, gridBagConstraints);
        listenerClassLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(EventListenerPanel.class, "LBL_Listener_Class")); // NOI18N
        listenerClassLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(EventListenerPanel.class, "LBL_Listener_Class")); // NOI18N

        listenerClassTextField.setColumns(20);
        listenerClassTextField.setPreferredSize(new java.awt.Dimension(200, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        add(listenerClassTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(EventListenerPanel.class, "LBL_Browse")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 12);
        add(browseButton, gridBagConstraints);
        browseButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(EventListenerPanel.class, "LBL_Browse")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel listenerClassLabel;
    private javax.swing.JTextField listenerClassTextField;
    // End of variables declaration//GEN-END:variables
}
