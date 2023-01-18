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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javacard.project.deps.ui;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

final class ChooseOriginPanelVisual extends JPanel implements DocumentListener {
    private InitialDepKind kind;
    private final ChangeSupport supp = new ChangeSupport(this);
    private final WizardDescriptor wiz;

    public void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
    }

    public void fireChange() {
        supp.fireChange();
    }

    public void addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
    }

    /** Creates new form DependencyVisualPanel2 */
    ChooseOriginPanelVisual(WizardDescriptor wiz) {
        this.wiz = wiz;
        initComponents();
        originField.getDocument().addDocumentListener(this);
        sourcesField.getDocument().addDocumentListener(this);
        HelpCtx.setHelpIDString(this, "org.netbeans.modules.javacard.DependenciesPanel"); //NOI18N
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(ChooseOriginPanelVisual.class, 
                "WIZARD_STEP_CHOOSE_ORIGIN"); //NOI18N
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        originLbl = new javax.swing.JLabel();
        originField = new javax.swing.JTextField();
        browseOriginButton = new javax.swing.JButton();
        sourcesLabel = new javax.swing.JLabel();
        sourcesField = new javax.swing.JTextField();
        browseSourcesButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(originLbl, org.openide.util.NbBundle.getMessage(ChooseOriginPanelVisual.class, "ChooseOriginPanelVisual.originLbl.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 12, 0);
        add(originLbl, gridBagConstraints);

        originField.setText(org.openide.util.NbBundle.getMessage(ChooseOriginPanelVisual.class, "ChooseOriginPanelVisual.originField.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 140;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 5, 12, 5);
        add(originField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(browseOriginButton, org.openide.util.NbBundle.getMessage(ChooseOriginPanelVisual.class, "ChooseOriginPanelVisual.browseOriginButton.text")); // NOI18N
        browseOriginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onBrowseOrigin(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 12, 0);
        add(browseOriginButton, gridBagConstraints);

        sourcesLabel.setLabelFor(sourcesField);
        org.openide.awt.Mnemonics.setLocalizedText(sourcesLabel, org.openide.util.NbBundle.getMessage(ChooseOriginPanelVisual.class, "ChooseOriginPanelVisual.sourcesLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 12, 0);
        add(sourcesLabel, gridBagConstraints);

        sourcesField.setText(org.openide.util.NbBundle.getMessage(ChooseOriginPanelVisual.class, "ChooseOriginPanelVisual.sourcesField.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 140;
        gridBagConstraints.insets = new java.awt.Insets(3, 5, 12, 5);
        add(sourcesField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(browseSourcesButton, org.openide.util.NbBundle.getMessage(ChooseOriginPanelVisual.class, "ChooseOriginPanelVisual.browseSourcesButton.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 12, 0);
        add(browseSourcesButton, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void onBrowseOrigin(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onBrowseOrigin
        if (kind == InitialDepKind.PROJECT) {
            JFileChooser chooser = ProjectChooser.projectChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                originField.setText(f.getAbsolutePath());
            }
        } else {
            File f;
            if ((f = new FileChooserBuilder(ChooseOriginPanelVisual.class).
                    setFileFilter(new ArchiveFileFilter()).
                    setTitle(kind.toString()).
                    setFilesOnly(true).
                    showOpenDialog()) != null) {
                originField.setText(f.getAbsolutePath());
            }
        }
    }//GEN-LAST:event_onBrowseOrigin

    boolean valid() {
        if (kind == null) {
            return false;
        }
        if (originField.getText().trim().length() == 0) {
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, 
                    NbBundle.getMessage(ChooseOriginPanelVisual.class, "ERR_ORIGIN_NOT_SET")); //NOI18N
            return false;
        }
        File f = new File (originField.getText().trim());
        boolean e = f.exists();
        boolean result = e && kind == InitialDepKind.PROJECT ? f.isDirectory() : f.isFile();
        if (!result) {
            String key = e ? kind == InitialDepKind.PROJECT ?
                "ERR_EXPECTING_DIR" : "ERR_EXPECTING_FILE" : "ERR_NON_EXISTENT_FILE"; //NOI18N
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, NbBundle.getMessage(ChooseOriginPanelVisual.class, key, f.getName()));
            return result;
        }
        String s = sourcesField.getText();
        if (s.trim().length() > 0) {
            f = new File (s);
            if (!f.exists()) {
                wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                        NbBundle.getMessage(ChooseOriginPanelVisual.class,
                        "ERR_SOURCE_FILE_DOES_NOT_EXIST", f.getName())); //NOI18N
                return false;
            }
        }
        if (result) {
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
        }
        return result;
    }

    File getOriginFile() {
        return valid() ? new File (originField.getText().trim()) : null;
    }

    File getSourceFile() {
        return !sourcesField.isVisible() ? null : valid() ? sourcesField.getText().trim().length() == 0 ? null : new File (sourcesField.getText().trim()) : null;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseOriginButton;
    private javax.swing.JButton browseSourcesButton;
    private javax.swing.JTextField originField;
    private javax.swing.JLabel originLbl;
    private javax.swing.JTextField sourcesField;
    private javax.swing.JLabel sourcesLabel;
    // End of variables declaration//GEN-END:variables

    void setDepKind(InitialDepKind kind) {
        if (kind != null) {
            switch (kind) {
                case PROJECT :
                    originLbl.setText(NbBundle.getMessage(
                            ChooseOriginPanelVisual.class, "LBL_PROJECT")); //NOI18N
                    break;
                default :
                    originLbl.setText(NbBundle.getMessage(
                            ChooseOriginPanelVisual.class, "LBL_FILE")); //NOI18N
            }
        }
        this.kind = kind;
        sourcesField.setVisible (kind != InitialDepKind.PROJECT);
        sourcesLabel.setVisible (kind != InitialDepKind.PROJECT);
        browseSourcesButton.setVisible (kind != InitialDepKind.PROJECT);
    }

    public void insertUpdate(DocumentEvent e) {
        fireChange();
    }

    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }

    public void changedUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
}

