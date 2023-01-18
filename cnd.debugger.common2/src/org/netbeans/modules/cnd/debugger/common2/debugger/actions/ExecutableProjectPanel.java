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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.debugger.common2.debugger.actions;

import org.netbeans.modules.cnd.debugger.common2.debugger.api.EngineDescriptor;
import org.netbeans.modules.cnd.debugger.common2.debugger.api.EngineType;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.event.DocumentListener;
import javax.swing.JButton;

import org.openide.util.NbBundle;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.filesystems.FileUtil;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.project.ProjectUtils;


import org.netbeans.modules.cnd.debugger.common2.debugger.NativeDebuggerManager;
import org.netbeans.modules.cnd.debugger.common2.debugger.api.EngineCapability;
import org.netbeans.modules.cnd.debugger.common2.utils.IpeUtils;
import java.awt.event.ItemEvent;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.modules.cnd.api.remote.HostInfoProvider;
import org.netbeans.modules.cnd.api.remote.PathMap;
import org.netbeans.modules.cnd.debugger.common2.debugger.remote.Host;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationDescriptorProvider;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationSupport;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.filesystems.FileSystem;
import org.openide.util.RequestProcessor;

/**
 * Chooser for executable and project used by AttachPanel
 *
 * SHOULD factor with very identical code in CapturePanel and DebugCorePanel!
 */
public final class ExecutableProjectPanel extends javax.swing.JPanel {

//    private DocumentListener executableValidateListener = null;
    private static final RequestProcessor RP = new RequestProcessor("ExecutableProjectPanel", 1); //NOI18N
    private final JButton actionButton;
    private boolean noproject;
    private final EngineDescriptor debuggerType;

    private static Project lastSelectedProject = null;

    public ExecutableProjectPanel(JButton actionButton, EngineDescriptor debuggerType) {
        this.actionButton = actionButton;
        this.debuggerType = debuggerType;
        initialize();
    }

    private void initialize() {
        initComponents();
        // NOI18N
        errorLabel.setForeground(javax.swing.UIManager.getColor("nb.errorForeground")); // NOI18N
        initGui();

//        executableValidateListener = new ExecutableValidateListener();
//        executableComboBox.setModel(new DefaultComboBoxModel(exePaths));
//        ((JTextField) executableComboBox.getEditor().getEditorComponent()).
//                getDocument().addDocumentListener(executableValidateListener);

//	validateExecutablePath();
        projectComboBox.addItemListener(new java.awt.event.ItemListener() {
            @Override
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    projectChanged();
                }
            }
        });
    }

    private void projectChanged() {
        // Validate that project toolchain family is the same as debugger type
        Project selectedProject = getSelectedProject();
        if (selectedProject != null) {
            final MakeConfiguration conf = ConfigurationSupport.getProjectActiveConfiguration(selectedProject);
            if (conf != null) {
                EngineType projectDebuggerType = NativeDebuggerManager.debuggerType(conf);
                if (debuggerType.getType() != projectDebuggerType) {
                    setError("ERROR_WRONG_FAMILY", false); // NOI18N
                    return;
                }
            }
        }
        clearError();
    }

    public String getExecutablePath() {
        return executableField.getText();
//        String exe;
//	exe = ((JTextField) executableComboBox.getEditor().
//		getEditorComponent()).getText();
//	return exe;

        /* LATER, for Project only IDE
         if (DebuggerManager.isStandalone()) {
         exe = ((JTextField) executableComboBox.getEditor().
         getEditorComponent()).getText();
         } else {
         // get executable from project
         Project project = getSelectedProject();
         ConfigurationDescriptorProvider pdp = (ConfigurationDescriptorProvider)
         project.getLookup().lookup(ConfigurationDescriptorProvider.class);
         if (pdp == null) {
         return null;
         }
         MakeConfigurationDescriptor projectDescriptor =
         (MakeConfigurationDescriptor) pdp.getConfigurationDescriptor();
         MakeConfiguration configuration =
         (MakeConfiguration) projectDescriptor.getConfs().getActive();

         MakeArtifact maf = new MakeArtifact(projectDescriptor, configuration);
         exe = maf.getWorkingDirectory() + '/' + maf.getOutput();
         }
         return exe;
         */
    }

//    public void setExecutablePaths(String[] paths) {
//        executableComboBox.setModel(new DefaultComboBoxModel(paths));
//    }
//
    public void setExecutablePath(String hostName, String path) {
        executableField.setText(path);
//        ((JTextField) executableComboBox.getEditor().getEditorComponent()).setText(path);
        setSelectedProjectByPath(hostName, path);
    }

//    public JComboBox getPathComboBox() {
//        return executableComboBox;
//    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        executableLabel = new javax.swing.JLabel();
        executableField = new javax.swing.JTextField();
//        executableComboBox = new javax.swing.JComboBox();
//        executableBrowseButton = new javax.swing.JButton();
        projectLabel = new javax.swing.JLabel();
        projectComboBox = new javax.swing.JComboBox();
        errorLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        executableLabel.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("EXECUTABLE_MN").charAt(0));
        executableLabel.setLabelFor(executableField);
        executableLabel.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("EXECUTABLE_LBL"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        // LATER, for Project only IDE
        // if (DebuggerManager.Standalone()) {
        add(executableLabel, gridBagConstraints);
        //}

        executableField.setEditable(false);
        executableField.setToolTipText(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("ProgramPathname"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
        // LATER, for Project only IDE
        //if (DebuggerManager.Standalone()) {
        add(executableField, gridBagConstraints);
        //}

//        Catalog.setAccessibleDescription(executableBrowseButton,
//                "ACSD_ExecutableBrowse");   // NOI18N
//        executableBrowseButton.setMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("EXECUTABLEBROWSE_BUTTON_MN").charAt(0));
//        executableBrowseButton.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("BROWSE_BUTTON_TXT"));
//        executableBrowseButton.addActionListener(new java.awt.event.ActionListener() {
//
//            public void actionPerformed(java.awt.event.ActionEvent evt) {
//                executableBrowseButtonActionPerformed(evt);
//            }
//        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
        // LATER, for Project only IDE
        //if (DebuggerManager.get().Standalone()) {
//            add(executableBrowseButton, gridBagConstraints);
        //}

        projectLabel.setDisplayedMnemonic(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("ASSOCIATED_PROJECT_MN").charAt(0));
        projectLabel.setLabelFor(projectComboBox);
        projectLabel.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/debugger/common2/debugger/actions/Bundle").getString("ASSOCIATED_PROJECT_LBL"));
        Catalog.setAccessibleDescription(projectComboBox,
                "ACSD_Project");    // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        if (!NativeDebuggerManager.isStandalone() && !NativeDebuggerManager.isPL()) {
            add(projectLabel, gridBagConstraints);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 6, 0);
        if (!NativeDebuggerManager.isStandalone() && !NativeDebuggerManager.isPL()) {
            add(projectComboBox, gridBagConstraints);
        }

        errorLabel.setText(" "); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(errorLabel, gridBagConstraints);

    }
    // </editor-fold>//GEN-END:initComponents
//    private void executableBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {
//        //GEN-FIRST:event_executableBrowseButtonActionPerformed
//        String startFolder = getExecutablePath();
//        // Show the file chooser
//        FileChooser fileChooser = new FileChooser(
//                getString("SelectExecutable"),
//                getString("CHOOSER_BUTTON"),
//                FileChooser.FILES_ONLY,
//                new FileFilter[]{FileFilterFactory.getElfExecutableFileFilter()},
//                startFolder,
//                false);
//        int ret = fileChooser.showOpenDialog(this);
//        if (ret == FileChooser.CANCEL_OPTION) {
//            return;
//        }
//        ((JTextField) executableComboBox.getEditor().getEditorComponent()).setText(
//                fileChooser.getSelectedFile().getPath());
//    }//GEN-LAST:event_executableBrowseButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel errorLabel;
//    private javax.swing.JButton executableBrowseButton;
    private javax.swing.JTextField executableField;
//    private javax.swing.JComboBox executableComboBox;
    private javax.swing.JLabel executableLabel;
    private javax.swing.JComboBox projectComboBox;
    private javax.swing.JLabel projectLabel;
    // End of variables declaration//GEN-END:variables

    public static class ProjectCBItem {
        private ProjectInformation pinfo;

        public ProjectCBItem(ProjectInformation pinfo) {
            this.pinfo = pinfo;
        }

        @Override
        public String toString() {
            return pinfo.getDisplayName();
        }

        public Project getProject() {
            return pinfo.getProject();
        }

        public ProjectInformation getProjectInformation() {
            return pinfo;
        }
    }

    public static void fillProjectsCombo(javax.swing.JComboBox comboBox, Project selectedProject) {
        if (selectedProject == null) {
            selectedProject = OpenProjects.getDefault().getMainProject();
        }
        for (Project proj : OpenProjects.getDefault().getOpenProjects()) {
            // include only cnd projects (see IZ 164690)
            if (proj.getLookup().lookup(ConfigurationDescriptorProvider.class) != null) {
                ProjectInformation pinfo = ProjectUtils.getInformation(proj);
                ProjectCBItem pi = new ProjectCBItem(pinfo);
                comboBox.addItem(pi);
                if (selectedProject != null && proj == selectedProject) {
                    comboBox.setSelectedItem(pi);
                }
            }
        }
    }

    public void initGui() {
        projectComboBox.removeAllItems();

        // fake items
        projectComboBox.addItem(getString("NO_PROJECT"));
        projectComboBox.addItem(getString("NEW_PROJECT"));

        fillProjectsCombo(projectComboBox, lastSelectedProject);

        // clear executable
        executableField.setText("");
    }

    public boolean validateExecutablePath() {
        String exePath = getExecutablePath().trim();

        // If debugger cannot automatically derive executable, user needs to
        // provide one. It's validity will be checked further below.
	if (! debuggerType.hasCapability(EngineCapability.DERIVE_EXECUTABLE) &&
	    IpeUtils.isEmpty(exePath)) {
            setError("ERROR_NEED_EXEC", true); // NOI18N
            return false;
        }
//        String pName = IpeUtils.getBaseName(exePath);

//        if (!DebuggerManager.isStandalone()) {
//	    // IDE, match project
//            // check with auto text if it is set up as well
//            if ((autoString.length() == 0 || exePath.equals(autoString)) &&
//                    !matchProject(pName)) {
//                setLastProject();
//            }
//        }

        // validate executable
//	if (exePath.length() == 0 || exePath.equals(autoString)) {
//	    clearError();
//	    return true;
//	} 

        File exeFile = new File(exePath);
        // TODO
        // Need a remote file validation
        // or disable error for remote debugging
        if (!exeFile.exists()) {
            setError("ERROR_DONTEXIST", true); // NOI18N
            return false;
        }
        if (exeFile.isDirectory()) {
            setError("ERROR_NOTAEXEFILE", true); // NOI18N
            return false;
        }

        FileObject fo = FileUtil.toFileObject(exeFile);
        if (fo == null) {
            setError("ERROR_NOTAEXEFILE", true); // NOI18N
            return false;
        }
        DataObject dataObject = null;
        try {
            dataObject = DataObject.find(fo);
        } catch (Exception e) {
            setError("ERROR_DONTEXIST", true); // NOI18N
            return false;
        }
        if (!MIMENames.isBinary(IpeUtils.getMime(dataObject))) {
            setError("ERROR_NOTAEXEFILE", true); // NOI18N
            return false;
        }

        clearError();
        return true;
    }

    public void setError(String errorMsg, boolean disable) {
        errorLabel.setText(getString(errorMsg));
        if (disable) {
            projectComboBox.setEnabled(false);
        }
        if (actionButton != null) {
            actionButton.setEnabled(false);
        }
    }

    /**
     * Public method to enabled/disable everything in this panel
     */
    @Override
    public void setEnabled(boolean val) {
        executableField.setEnabled(val);
//        executableBrowseButton.setEnabled(val);
        projectComboBox.setEnabled(val);
//        if (validateExecutablePath()) {
//	    // setLastProject();
//        }
    }

    private void clearError() {
        errorLabel.setText(" "); // NOI18N
        projectComboBox.setEnabled(true);
        if (actionButton != null) {
            actionButton.setEnabled(true);
        }
    }

    // ModifiedDocumentListener
    public class ExecutableValidateListener implements DocumentListener {

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) {
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) {
            if (validateExecutablePath()) {
                // ;
            }
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) {
            if (validateExecutablePath()) {
                // ;
            }
        }
    }

    public Project getSelectedProject() {
        Object selectedItem = projectComboBox.getSelectedItem();
        if (selectedItem instanceof ProjectCBItem) {
            noproject = false;
            return ((ProjectCBItem)selectedItem).getProject();
        }
        // set noproject if NO_PROJECT is selected
        noproject = (projectComboBox.getSelectedIndex() == 0);
        return null;
    }

    /*package*/ String getSelectedProjectPath() {
        Project prj = getSelectedProject();
        return (prj != null)? prj.getProjectDirectory().getPath(): "";
    }

    /*package*/ void setSelectedProjectByPath(final String hostName, final String path) {
        if (path == null || path.length() == 0) {
            projectComboBox.setSelectedIndex(0);
            return;
        }
 
        projectComboBox.setEnabled(false);
        
        RP.post(new Runnable() {
            @Override
            public void run() {
                final ProjectCBItem prj = getProjectByPath(hostName, path);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (prj != null) {
                            projectComboBox.setSelectedItem(prj);
                        } else {
                            projectComboBox.setSelectedIndex(0);
                        }
                        
                        projectComboBox.setEnabled(true);
                    }
                });
            }
        });
    }

    /*package*/ boolean containsProjectWithPath(String hostName, String path) {
        return getProjectByPath(hostName, path) != null;
    }

    private ProjectCBItem getProjectByPath(String hostName, String path) {
        final ExecutionEnvironment executionEnvironment = Host.byName(hostName).executionEnvironment();

        FileSystem fs = FileSystemProvider.getFileSystem(executionEnvironment);
        FileObject f = null;
        Project prj = null;
        
        int pos = -1;
        do {
            pos = path.indexOf(" ", pos + 1); // NOI18N
            f = CndFileUtils.toFileObject(fs, pos == -1 ? path : path.substring(0, pos));
            if (f != null && f.isValid()) {
                prj = FileOwnerQuery.getOwner(f);
                if (prj != null) {
                    break;
                }
            }
        } while (pos != -1);

        for (int i = 0; i < projectComboBox.getModel().getSize(); i++) {
            Object item = projectComboBox.getModel().getElementAt(i);
            if (item instanceof ProjectCBItem) {
                if (((ProjectCBItem) item).getProject().equals(prj)) {
                    return (ProjectCBItem) item;
                }
            }
        }
        //if not found there could be corner case: we are in remote and in shared folder (/home/user)
        //in this case the default behaviuor would be return Full Remote project
        //let's try to get  local project
        final boolean isRemote = executionEnvironment.isRemote() && ConnectionManager.getInstance().isConnectedTo(executionEnvironment);
        if (!isRemote) {
            return null;
        }

        PathMap map = HostInfoProvider.getMapper(executionEnvironment);

        String filePath = map.getLocalPath(path);
        if (filePath == null) {
            return null;
        }
        
        do {
            f = CndFileUtils.toFileObject(FileSystemProvider.getFileSystem(ExecutionEnvironmentFactory.getLocal()), filePath);
            if (f != null && f.isValid()) {
                prj = FileOwnerQuery.getOwner(f);
                if (prj != null) {
                    break;
                }
            }

            filePath = CndPathUtilities.getDirName(filePath);
        } while (filePath != null);

        if (f == null) {
            return null;
        }

        for (int i = 0; i < projectComboBox.getModel().getSize(); i++) {
            Object item = projectComboBox.getModel().getElementAt(i);
            if (item instanceof ProjectCBItem) {
                if (((ProjectCBItem) item).getProject().equals(prj)) {
                    return (ProjectCBItem) item;
                }
            }
        }
        
        return null;
    }

    /** Look up i18n strings here */
    private ResourceBundle bundle;

    private String getString(String s) {
        if (bundle == null) {
            bundle = NbBundle.getBundle(ExecutableProjectPanel.class);
        }
        return bundle.getString(s);
    }

    public boolean asynchronous() {
        return false;
    }

    public void setLastSelectedProject(Project l) {
        lastSelectedProject = l;
    }

    public boolean getNoProject() {
        return noproject;
    }
}