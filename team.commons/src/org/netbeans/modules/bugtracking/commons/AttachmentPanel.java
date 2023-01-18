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
package org.netbeans.modules.bugtracking.commons;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Stola
 */
public class AttachmentPanel extends javax.swing.JPanel implements DocumentListener, ActionListener {
    static final String PROP_DELETED = "attachmentDeleted"; // NOI18N
    private final AttachmentsPanel.NBBugzillaCallback nbCallback;
    private final ChangeSupport supp;
    
    public AttachmentPanel(AttachmentsPanel.NBBugzillaCallback nbCallback) {
        this.nbCallback = nbCallback;
        this.supp = new ChangeSupport(this);
        initComponents();
        setBackground( UIUtils.getSectionPanelBackground() );
        initFileTypeCombo();
        attachListeners();
    }

    private void initFileTypeCombo() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        ResourceBundle bundle = NbBundle .getBundle(AttachmentPanel.class);
        model.addElement(new FileType(null, bundle.getString("AttachmentPanel.fileType.automatic"))); // NOI18N
        model.addElement(new FileType("text/plain", bundle.getString("AttachmentPanel.fileType.textPlain"))); // NOI18N
        model.addElement(new FileType("text/html", bundle.getString("AttachmentPanel.fileType.textHTML"))); // NOI18N
        model.addElement(new FileType("application/xml", bundle.getString("AttachmentPanel.fileType.applicationXML"))); // NOI18N
        model.addElement(new FileType("image/gif", bundle.getString("AttachmentPanel.fileType.imageGIF"))); // NOI18N
        model.addElement(new FileType("image/jpeg", bundle.getString("AttachmentPanel.fileType.imageJPEG"))); // NOI18N
        model.addElement(new FileType("image/png", bundle.getString("AttachmentPanel.fileType.imagePNG"))); // NOI18N
        model.addElement(new FileType("application/octet-stream", bundle.getString("AttachmentPanel.fileType.binary"))); // NOI18N
        fileTypeCombo.setModel(model);
    }

    void setAttachment(File f, String description, String conntentType, boolean isPatch) {
        descriptionField.setText(description);
        
        fileField.setText(f.getAbsolutePath());
        patchChoice.setSelected(isPatch);
        int c = fileTypeCombo.getItemCount();
        if(conntentType != null) {
            for (int i = 0; i < c; i++) {
                Object o = fileTypeCombo.getItemAt(i);
                if(o instanceof FileType) {
                    String ct = ((FileType)o).contentType;
                    if(ct != null && ct.equals(conntentType)) {
                        fileTypeCombo.setSelectedItem(o);
                        break;
                    }
                }
            }
        }
    }
    
    public File getFile() {
        File file = null;
        if (!isDeleted()) {
            file = new File(fileField.getText());
        }
        return file;
    }

    public String getDescription() {
        return descriptionField.getText();
    }

    public String getContentType() {
        String contentType = null;
        Object value = fileTypeCombo.getSelectedItem();
        if (value instanceof FileType) {
            contentType = ((FileType)value).getContentType();
        } else {
            contentType = value.toString();
        }
        return contentType;
    }

    public boolean isPatch() {
        return patchChoice.isSelected();
    }

    public boolean isDeleted() {
        return !isVisible();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        deleteButton = new org.netbeans.modules.bugtracking.commons.LinkButton();
        descriptionLabel = new javax.swing.JLabel();
        descriptionField = new javax.swing.JTextField();
        fileTypeLabel = new javax.swing.JLabel();

        fileField.setColumns(30);

        browseButton.setText(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        deleteButton.setText(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.deleteButton.text")); // NOI18N
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        descriptionLabel.setLabelFor(descriptionField);
        descriptionLabel.setText(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.descriptionLabel.text")); // NOI18N

        fileTypeLabel.setLabelFor(fileTypeCombo);
        fileTypeLabel.setText(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.fileTypeLabel.text")); // NOI18N

        fileTypeCombo.setEditable(true);

        patchLabel.setLabelFor(patchChoice);
        patchLabel.setText(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.patchLabel.text")); // NOI18N

        patchChoice.setBorder(null);
        patchChoice.setMargin(new java.awt.Insets(0, 0, 0, 0));
        patchChoice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                patchChoiceActionPerformed(evt);
            }
        });

        viewButton.setText(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.viewButton.text")); // NOI18N
        viewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fileField, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(fileTypeLabel)
                            .addComponent(descriptionLabel)
                            .addComponent(patchLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fileTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(descriptionField)
                            .addComponent(patchChoice))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(viewButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton)
                    .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(viewButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(descriptionLabel)
                    .addComponent(descriptionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileTypeLabel)
                    .addComponent(fileTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(patchLabel)
                    .addComponent(patchChoice))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileField.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.fileField.AccessibleContext.accessibleName")); // NOI18N
        fileField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.fileField.AccessibleContext.accessibleDescription")); // NOI18N
        browseButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.browseButton.AccessibleContext.accessibleDescription")); // NOI18N
        deleteButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.deleteButton.AccessibleContext.accessibleDescription")); // NOI18N
        descriptionField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.descriptionField.AccessibleContext.accessibleDescription")); // NOI18N
        fileTypeCombo.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.fileTypeCombo.AccessibleContext.accessibleDescription")); // NOI18N
        patchChoice.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.patchChoice.AccessibleContext.accessibleName")); // NOI18N
        patchChoice.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AttachmentPanel.class, "AttachmentPanel.patchChoice.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        setVisible(false);
        firePropertyChange(PROP_DELETED, null, null);
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        File attachment = new FileChooserBuilder(AttachmentPanel.class).showOpenDialog();
        if (attachment != null) {
            attachment = FileUtil.normalizeFile(attachment);
            fileField.setText(attachment.getAbsolutePath());
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void patchChoiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_patchChoiceActionPerformed
        fileTypeCombo.setEnabled(!patchChoice.isSelected());
        if (patchChoice.isSelected()) {
            // Select text/plain
            fileTypeCombo.setSelectedIndex(1);
        }
    }//GEN-LAST:event_patchChoiceActionPerformed

    private void viewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewButtonActionPerformed
        nbCallback.showLogFile();
    }//GEN-LAST:event_viewButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JButton browseButton = new javax.swing.JButton();
    private org.netbeans.modules.bugtracking.commons.LinkButton deleteButton;
    private javax.swing.JTextField descriptionField;
    private javax.swing.JLabel descriptionLabel;
    final javax.swing.JTextField fileField = new javax.swing.JTextField();
    final javax.swing.JComboBox fileTypeCombo = new javax.swing.JComboBox();
    private javax.swing.JLabel fileTypeLabel;
    final javax.swing.JCheckBox patchChoice = new javax.swing.JCheckBox();
    final javax.swing.JLabel patchLabel = new javax.swing.JLabel();
    final org.netbeans.modules.bugtracking.commons.LinkButton viewButton = new org.netbeans.modules.bugtracking.commons.LinkButton();
    // End of variables declaration//GEN-END:variables

    void addChangeListener (ChangeListener changeListener) {
        supp.addChangeListener(changeListener);
    }

    private void attachListeners () {
        fileField.getDocument().addDocumentListener(this);
        descriptionField.getDocument().addDocumentListener(this);
        fileTypeCombo.addActionListener(this);
        patchChoice.addActionListener(this);
    }

    @Override
    public void insertUpdate (DocumentEvent e) {
        fieldUpdated(e.getDocument());
    }

    @Override
    public void removeUpdate (DocumentEvent e) {
        fieldUpdated(e.getDocument());
    }

    @Override
    public void changedUpdate (DocumentEvent e) { }

    @Override
    public void actionPerformed (ActionEvent e) {
        if (e.getSource() == fileTypeCombo
                || e.getSource() == patchChoice) {
            supp.fireChange();
        }
    }

    private void fieldUpdated (Document document) {
        if (document == fileField.getDocument()
                || document == descriptionField.getDocument()) {
            supp.fireChange();
        }
    }

    static class FileType {
        private String contentType;
        private String displayName;

        FileType(String contentType, String displayName) {
            this.contentType = contentType;
            this.displayName = displayName;
        }

        public String getContentType() {
            return contentType;
        }

        @Override
        public String toString() {
            return displayName + ((contentType == null) ? "" : " (" + contentType + ')'); // NOI18N
        }
    }

}
