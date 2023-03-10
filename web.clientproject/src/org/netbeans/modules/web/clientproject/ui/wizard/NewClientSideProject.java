/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.clientproject.ui.wizard;

import java.io.File;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.web.clientproject.util.ClientSideProjectUtilities;
import org.netbeans.modules.web.clientproject.api.util.ValidationUtilities;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public class NewClientSideProject extends JPanel implements HelpCtx.Provider {

    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private final String projectNameTemplate;


    public NewClientSideProject(String projectNameTemplate) {
        assert projectNameTemplate != null;
        this.projectNameTemplate = projectNameTemplate;

        initComponents();
        initProjectNameAndLocation();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.netbeans.modules.web.clientproject.ui.wizard.NewClientSideProject"); // NOI18N
    }

    private void initProjectNameAndLocation() {
        // default name & location
        File projectLocation = ProjectChooser.getProjectsFolder();
        projectLocationTextField.setText(projectLocation.getAbsolutePath());

        String projectName = projectNameTemplate;
        int index = 0;
        while ((new File(projectLocation, projectName)).exists()) {
            index++;
            projectName = projectNameTemplate + index;
        }
        projectNameTextField.setText(projectName);
        projectNameTextField.selectAll();
        updateProjectFolder();

        // listeners
        DocumentListener documentListener = new DefaultDocumentListener();
        projectNameTextField.getDocument().addDocumentListener(documentListener);
        projectLocationTextField.getDocument().addDocumentListener(documentListener);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // same problem as in #31086, initial focus on Cancel button
        projectNameTextField.requestFocus();
    }

    public String getProjectName() {
        return projectNameTextField.getText().trim();
    }

    public String getProjectLocation() {
        return projectLocationTextField.getText().trim();
    }

    public File getProjectDirectory() {
        return FileUtil.normalizeFile(new File(createdFolderTextField.getText()));
    }

    public final void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public final void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    public String getErrorMessage() {
        String error = validateProjectName();
        if (error != null) {
            return error;
        }
        error = validateProjectLocation();
        if (error != null) {
            return error;
        }
        return null;
    }

    @NbBundle.Messages("ClientSideProject.error.name.missing=Project name must be provided.")
    private String validateProjectName() {
        String projectName = getProjectName();
        if (projectName.isEmpty()) {
            return Bundle.ClientSideProject_error_name_missing();
        }
        return null;
    }

    @NbBundle.Messages({
        "ClientSideProject.error.location.invalid=Project location is not a valid path.",
        "ClientSideProject.error.location.alreadyProject=Project folder is already NetBeans project (maybe only in memory).",
        "ClientSideProject.error.location.notWritable=Project folder cannot be created.",
        "ClientSideProject.error.location.notEmpty=Project folder already exists and is not empty."
    })
    private String validateProjectLocation() {
        File projectLocation = FileUtil.normalizeFile(new File(getProjectLocation()).getAbsoluteFile());
        if (!projectLocation.isDirectory()) {
            return Bundle.ClientSideProject_error_location_invalid();
        }
        final File destFolder = getProjectDirectory();
        if (ClientSideProjectUtilities.isProject(destFolder)) {
            return Bundle.ClientSideProject_error_location_alreadyProject();
        }
        if (!ValidationUtilities.isValidFilename(destFolder)) {
            return Bundle.ClientSideProject_error_location_invalid();
        }

        File projLoc = destFolder;
        while (projLoc != null && !projLoc.exists()) {
            projLoc = projLoc.getParentFile();
        }
        if (projLoc == null || !projLoc.canWrite()) {
            return Bundle.ClientSideProject_error_location_notWritable();
        }

        if (FileUtil.toFileObject(projLoc) == null) {
            return Bundle.ClientSideProject_error_location_invalid();
        }

        File[] kids = destFolder.listFiles();
        if (destFolder.exists() && kids != null && kids.length > 0) {
            return Bundle.ClientSideProject_error_location_notEmpty();
        }
        return null;
    }

    void updateProjectFolder() {
        createdFolderTextField.setText(getProjectLocation() + File.separatorChar + getProjectName());
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        projectNameLabel = new javax.swing.JLabel();
        projectNameTextField = new javax.swing.JTextField();
        projectLocationLabel = new javax.swing.JLabel();
        projectLocationTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        createdFolderLabel = new javax.swing.JLabel();
        createdFolderTextField = new javax.swing.JTextField();

        projectNameLabel.setLabelFor(projectNameTextField);
        org.openide.awt.Mnemonics.setLocalizedText(projectNameLabel, org.openide.util.NbBundle.getMessage(NewClientSideProject.class, "NewClientSideProject.projectNameLabel.text")); // NOI18N

        projectLocationLabel.setLabelFor(projectLocationTextField);
        org.openide.awt.Mnemonics.setLocalizedText(projectLocationLabel, org.openide.util.NbBundle.getMessage(NewClientSideProject.class, "NewClientSideProject.projectLocationLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(NewClientSideProject.class, "NewClientSideProject.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        createdFolderLabel.setLabelFor(createdFolderTextField);
        org.openide.awt.Mnemonics.setLocalizedText(createdFolderLabel, org.openide.util.NbBundle.getMessage(NewClientSideProject.class, "NewClientSideProject.createdFolderLabel.text")); // NOI18N

        createdFolderTextField.setEditable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameLabel)
                    .addComponent(projectLocationLabel)
                    .addComponent(createdFolderLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(projectLocationTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(createdFolderTextField, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseButton))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectNameLabel)
                    .addComponent(projectNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectLocationLabel)
                    .addComponent(projectLocationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(createdFolderLabel)
                    .addComponent(createdFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    @NbBundle.Messages("ClientSideProject.dialog.location.title=Select Project Location")
    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        File workDir = null;
        String projectLocation = getProjectLocation();
        if (projectLocation != null && !projectLocation.isEmpty()) {
            File projDir = new File(projectLocation);
            if (projDir.isDirectory()) {
                workDir = projDir;
            }
        }
        if (workDir == null) {
            workDir = ProjectChooser.getProjectsFolder();
        }
        File projectDir = new FileChooserBuilder(NewClientSideProject.class)
                .setTitle(Bundle.ClientSideProject_dialog_location_title())
                .setDirectoriesOnly(true)
                .setDefaultWorkingDirectory(workDir)
                .forceUseOfDefaultWorkingDirectory(true)
                .showOpenDialog();
        if (projectDir != null) {
            projectLocationTextField.setText(FileUtil.normalizeFile(projectDir).getAbsolutePath());
        }
    }//GEN-LAST:event_browseButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel createdFolderLabel;
    private javax.swing.JTextField createdFolderTextField;
    private javax.swing.JLabel projectLocationLabel;
    private javax.swing.JTextField projectLocationTextField;
    private javax.swing.JLabel projectNameLabel;
    private javax.swing.JTextField projectNameTextField;
    // End of variables declaration//GEN-END:variables

    //~ Inner classes

    private final class DefaultDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            processChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            processChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            processChange();
        }

        private void processChange() {
            updateProjectFolder();
            fireChange();
        }

    }

}
