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

package org.netbeans.modules.apisupport.project.ui.wizard.codegenerator;

import java.awt.Component;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.apisupport.project.api.UIUtil;
import org.netbeans.modules.apisupport.project.ui.wizard.common.BasicWizardIterator;
import org.netbeans.modules.apisupport.project.ui.wizard.codegenerator.NewCodeGeneratorIterator.DataModel;
import org.netbeans.modules.apisupport.project.ui.wizard.common.WizardUtils;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * CodeGenerator API Support wizard Panel
 * @author Max Sauer
 */
public class CodeGeneratorPanel extends BasicWizardIterator.Panel {
    
    private DataModel data;

    /** Creates new form CodeGeneratorPanel */
    CodeGeneratorPanel(WizardDescriptor settings, NewCodeGeneratorIterator.DataModel data) {
        super(settings);
        this.data = data;
        initComponents();
        
        putClientProperty("NewFileWizard_Title", getMessage("LBL_CodeGeneratorPanel_Title"));
        
        DocumentListener dListener = new UIUtil.DocumentAdapter() {
            public void insertUpdate(DocumentEvent e) {
                checkValidity();
            }
        };
        
        if (data.getPackageName() != null) {
            packageNameCombo.setSelectedItem(data.getPackageName());
        }
        
        fileNametextField.getDocument().addDocumentListener(dListener);
        cpFileNameField.getDocument().addDocumentListener(dListener);
        mimeTypeTextField.getDocument().addDocumentListener(dListener);
        Component editorComp = packageNameCombo.getEditor().getEditorComponent();
        if (editorComp instanceof JTextComponent) {
            ((JTextComponent) editorComp).getDocument().addDocumentListener(dListener);
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

        fileNameLabel = new javax.swing.JLabel();
        fileNametextField = new javax.swing.JTextField();
        mimeTypeLabel = new javax.swing.JLabel();
        mimeTypeTextField = new javax.swing.JTextField();
        cpCheckBox = new javax.swing.JCheckBox();
        cpFileNameLabel = new javax.swing.JLabel();
        cpFileNameField = new javax.swing.JTextField();
        packageNameCombo = WizardUtils.createPackageComboBox(data.getSourceRootGroup());
        packageNameLabel = new javax.swing.JLabel();

        fileNameLabel.setLabelFor(fileNametextField);
        org.openide.awt.Mnemonics.setLocalizedText(fileNameLabel, org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.fileNameLabel.text")); // NOI18N

        fileNametextField.setText(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.fileNametextField.text")); // NOI18N

        mimeTypeLabel.setLabelFor(mimeTypeTextField);
        org.openide.awt.Mnemonics.setLocalizedText(mimeTypeLabel, org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.mimeTypeLabel.text")); // NOI18N

        mimeTypeTextField.setText(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.mimeTypeTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cpCheckBox, org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpCheckBox.text")); // NOI18N
        cpCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cpCheckBoxStateChanged(evt);
            }
        });

        cpFileNameLabel.setLabelFor(cpFileNameField);
        org.openide.awt.Mnemonics.setLocalizedText(cpFileNameLabel, org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpFileNameLabel.text")); // NOI18N

        cpFileNameField.setEditable(cpCheckBox.isSelected());
        cpFileNameField.setText(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpFileNameField.text")); // NOI18N

        packageNameCombo.setEditable(true);

        packageNameLabel.setLabelFor(packageNameCombo);
        org.openide.awt.Mnemonics.setLocalizedText(packageNameLabel, org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.packageNameLabel.text")); // NOI18N

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
                                    .addComponent(fileNameLabel)
                                    .addComponent(mimeTypeLabel)
                                    .addComponent(packageNameLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(fileNametextField, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                                    .addComponent(mimeTypeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                                    .addComponent(packageNameCombo, 0, 311, Short.MAX_VALUE)))
                            .addComponent(cpCheckBox)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(cpFileNameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cpFileNameField, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileNameLabel)
                    .addComponent(fileNametextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(packageNameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(packageNameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mimeTypeLabel)
                    .addComponent(mimeTypeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(cpCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cpFileNameLabel)
                    .addComponent(cpFileNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(123, Short.MAX_VALUE))
        );

        fileNameLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.fileNameLabel.AccessibleContext.accessibleDescription")); // NOI18N
        fileNametextField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.fileNametextField.AccessibleContext.accessibleName")); // NOI18N
        fileNametextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.fileNametextField.AccessibleContext.accessibleDescription")); // NOI18N
        mimeTypeLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.mimeTypeLabel.AccessibleContext.accessibleDescription")); // NOI18N
        mimeTypeTextField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.mimeTypeTextField.AccessibleContext.accessibleName")); // NOI18N
        mimeTypeTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.mimeTypeTextField.AccessibleContext.accessibleDescription")); // NOI18N
        cpCheckBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpCheckBox.AccessibleContext.accessibleDescription")); // NOI18N
        cpFileNameLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpFileNameLabel.AccessibleContext.accessibleDescription")); // NOI18N
        cpFileNameField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpFileNameField.AccessibleContext.accessibleName")); // NOI18N
        cpFileNameField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.cpFileNameField.AccessibleContext.accessibleDescription")); // NOI18N
        packageNameCombo.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.packageNameCombo.AccessibleContext.accessibleDescription")); // NOI18N
        packageNameLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.packageNameLabel.AccessibleContext.accessibleDescription")); // NOI18N

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.AccessibleContext.accessibleName")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CodeGeneratorPanel.class, "CodeGeneratorPanel.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void cpCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cpCheckBoxStateChanged
    cpFileNameField.setEditable(cpCheckBox.isSelected());//GEN-LAST:event_cpCheckBoxStateChanged
    checkValidity();
}                                       

    @Override
    protected String getPanelName() {
        return NbBundle.getMessage(CodeGeneratorPanel.class,"LBL_CodeGeneratorPanel_Title"); // NOI18N
    }

    @Override
    protected void storeToDataModel() {
        data.setMimeType(mimeTypeTextField.getText().trim());
        data.setFileName(normalize(fileNametextField.getText().trim()));
        data.setContextProviderRequired(cpCheckBox.isSelected());
        data.setProviderFileName(cpFileNameField.getText().trim());
        data.setPackageName(packageNameCombo.getEditor().getItem().toString());
        NewCodeGeneratorIterator.generateFileChanges(data);
    }

    @Override
    protected void readFromDataModel() {
        mimeTypeTextField.setText(data.getMimeType());
        fileNametextField.setText(data.getFileName());
        cpFileNameField.setText(data.getProviderFileName());
        cpCheckBox.setSelected(data.isContextProviderRequired());
        packageNameCombo.setSelectedItem(data.getPackageName());
    }

    @Override
    protected HelpCtx getHelp() {
        return new HelpCtx(CodeGeneratorPanel.class);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox cpCheckBox;
    private javax.swing.JTextField cpFileNameField;
    private javax.swing.JLabel cpFileNameLabel;
    private javax.swing.JLabel fileNameLabel;
    private javax.swing.JTextField fileNametextField;
    private javax.swing.JLabel mimeTypeLabel;
    private javax.swing.JTextField mimeTypeTextField;
    private javax.swing.JComboBox packageNameCombo;
    private javax.swing.JLabel packageNameLabel;
    // End of variables declaration//GEN-END:variables

    private String normalize(String trim) {
        if(trim.endsWith(".java"))
            return trim.substring(0, trim.length()-5);
        else return trim;
    }

    
    private boolean checkValidity() {
        final String fileName = fileNametextField.getText().trim();
        final String mimeType = mimeTypeTextField.getText().trim();
        if(fileName.length() == 0) {
            setWarning(getMessage("ERR_FN_EMPTY"), false);
            return false;
        }
        if(!Utilities.isJavaIdentifier(normalize(fileName))) {
            setError(getMessage("ERR_FN_INVALID"));
            return false;
        }
        if(mimeType.length() == 0) {
            setWarning(getMessage("ERR_MT_EMPTY"), false);
            return false;
        }
        
        String packName = packageNameCombo.getEditor().getItem().toString();
        if(packName.equals("")) {
            setWarning(getMessage("EMPTY_PACKAGE"), false);
            return false;
        }
        
        if (cpCheckBox.isSelected()) {
            String cpFileName = cpFileNameField.getText().trim();
            if (cpFileName.length() == 0) {
                setWarning(getMessage("ERR_FN_EMPTY"), false);
                return false;
            }
            if (!Utilities.isJavaIdentifier(normalize(cpFileName))) {
                setError(getMessage("ERR_FN_INVALID"));
                return false;
            }
        }
        markValid();
        return true;
    }
    
    private String getMessage(String key) {
        return NbBundle.getMessage(CodeGeneratorPanel.class, key);
    }
}
