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
package org.netbeans.modules.cnd.makeproject.ui;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.cnd.api.remote.PathMap;
import org.netbeans.modules.cnd.api.remote.RemoteFileUtil;
import org.netbeans.modules.cnd.api.remote.RemoteSyncSupport;
import org.netbeans.modules.cnd.api.toolchain.PlatformTypes;
import org.netbeans.modules.cnd.makeproject.api.ProjectActionEvent;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.netbeans.modules.cnd.utils.FileFilterFactory;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.openide.DialogDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

public class SelectExecutablePanel extends javax.swing.JPanel {

    private final JList exeList;
    private final FileFilterFactory.AbstractFileAndFileObjectFilter elfExecutableFileFilter = FileFilterFactory.getElfExecutableFileFilter();
    private final FileFilterFactory.AbstractFileAndFileObjectFilter exeExecutableFileFilter = FileFilterFactory.getPeExecutableFileFilter();
    private final FileFilterFactory.AbstractFileAndFileObjectFilter machOExecutableFileFilter = FileFilterFactory.getMacOSXExecutableFileFilter();
    private final DocumentListener documentListener;
    private DialogDescriptor dialogDescriptor;
    private final MakeConfiguration conf;
    private final FileObject buildWorkingDirFO;
    private final PathMap mapper;
    private static final RequestProcessor RP = new RequestProcessor("SelectExecutable",1); //NOI18N
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final Map<String,FileObject> searchResult = new TreeMap<>();
    private final String wd;

    private boolean resetList = false;

    /** Creates new form SelectExecutable */
    public SelectExecutablePanel(ProjectActionEvent pae) {
        this.conf = pae.getConfiguration();
        initComponents();
        instructionsTextArea.setBackground(getBackground());
        mapper = RemoteSyncSupport.getPathMap(pae.getProject());
        String wd = conf.getMakefileConfiguration().getAbsBuildCommandWorkingDir();
        if (mapper != null) {
            String aWd = mapper.getRemotePath(conf.getMakefileConfiguration().getAbsBuildCommandWorkingDir(), true);
            if (aWd != null) {
                wd = aWd;
            }
        }
        this.wd = wd;

        buildWorkingDirFO = RemoteFileUtil.getFileObject(wd, conf.getDevelopmentHost().getExecutionEnvironment());
        exeList = new JList();
        executableList.setViewportView(exeList);
        exeList.addListSelectionListener(new MyListSelectionListener());

        documentListener = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                validateExe();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateExe();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateExe();
            }
        };
        executableTextField.getDocument().addDocumentListener(documentListener);

        setPreferredSize(new java.awt.Dimension(600, 300));

        validateExe();
        progress.setText(NbBundle.getMessage(SelectExecutablePanel.class, "Search_In_Progress")); //NOI18N
        progress.setIcon(ImageUtilities.loadImageIcon("org/netbeans/modules/cnd/makeproject/ui/resources/exclamation.gif", false)); //NOI18N
        RP.post(new Runnable() {

            @Override
            public void run() {
                findAllExecutables(buildWorkingDirFO);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        progress.setVisible(false);
                    }
                });
            }
        });
        this.addHierarchyListener(new HierarchyListener() {

            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if (e.getChangeFlags() == HierarchyEvent.SHOWING_CHANGED) {
                    if (!e.getChanged().isVisible()){
                        canceled.set(true);
                    }
                }
            }
        });
    }

    public void setDialogDescriptor(DialogDescriptor dialogDescriptor) {
        this.dialogDescriptor = dialogDescriptor;
        validateExe();
    }

    private final class MyListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (resetList) {
                return;
            }
            if (e.getValueIsAdjusting() == false) {
                int i = exeList.getSelectedIndex();
                if (i >= 0) {
                    executableTextField.setText((String) exeList.getSelectedValue());
                    validateExe();
                }
            }
        }
    }

    private void validateExe() {
        String errorText = null;
        if (executableTextField.getText().length() == 0) {
            errorText = getString("NO_EXE_ERROR");
        } else {
            String executablePath = executableTextField.getText();
            FileObject exe = null;
            if (!CndPathUtilities.isPathAbsolute(executablePath)) {
                if (buildWorkingDirFO != null) {
                    exe = buildWorkingDirFO.getFileObject(executablePath);
                }
            } else {
                executablePath = CndFileUtils.normalizeAbsolutePath(executablePath);
                exe = RemoteFileUtil.getFileObject(
                        executablePath,
                        conf.getDevelopmentHost().getExecutionEnvironment());
            }
            if (!CndPathUtilities.isPathAbsolute(executablePath)
                    && (buildWorkingDirFO == null || ! buildWorkingDirFO.isValid())) {
                errorText = NbBundle.getMessage(SelectExecutablePanel.class, "WRONG_WORKING_DIR", wd); //NOI18N
            } else if (exe == null || !exe.isValid()) {
                errorText = getString("EXE_DOESNT_EXISTS"); //NOI18N
            } else if (exe.isFolder() || (!elfExecutableFileFilter.accept(exe) && !exeExecutableFileFilter.accept(exe) && !machOExecutableFileFilter.accept(exe))) {
                errorText = getString("FILE_NOT_AN_EXECUTABLE"); //NOI18N
            }
        }
        if (errorText != null) {
            errorLabel.setText(errorText);
            if (dialogDescriptor != null) {
                dialogDescriptor.setValid(false);
            }
        } else {
            errorLabel.setText(" "); // NOI18N
            if (dialogDescriptor != null) {
                dialogDescriptor.setValid(true);
            }
        }
    }

    public String getExecutable() {
        String path = executableTextField.getText();
        if (mapper != null) {
            path = mapper.getLocalPath(path);
        }
        return path;
    }

    private void findAllExecutables(FileObject root) {
        if (root == null || !root.isValid() || !root.isFolder()) {
            // Something is wrong
            return;
        }
        addExecutables(root);
    }

    private void addExecutables(FileObject dir) {
        ArrayList<FileObject> downPrev = new ArrayList<>();
        downPrev.add(dir);
        while (!downPrev.isEmpty()) {
            ArrayList<FileObject> downNext = new ArrayList<>();
            for (FileObject  folder : downPrev) {
                if (canceled.get()) {
                    return;
                }
                folder.refresh();
                FileObject[] files = folder.getChildren();
                if (files == null) {
                    continue;
                }
                for (int i = 0; i < files.length; i++) {
                    if (canceled.get()) {
                        return;
                    }
                    if (files[i].isFolder()) {
                        // FIXUP: is this the best way to deal with files under SCCS?
                        // Unfortunately the SCCS directory contains data files with the same
                        // suffixes as the the source files, and a simple file filter based on
                        // a file's suffix cannot see the difference between the source file and
                        // the data file. Only the source file should be added.
                        final String aName = files[i].getName();
                        if (aName.equals("SCCS") || aName.equals("CVS") || aName.equals(".hg") || aName.equals("SunWS_cache") || aName.equals(".svn")) // NOI18N
                        {
                            continue;
                        }
                        downNext.add(files[i]);
                    } else {
                        if (FileFilterFactory.getAllFileFilter().accept(files[i])) {
                            continue;
                        }
                        if (conf.getDevelopmentHost().getBuildPlatform() == PlatformTypes.PLATFORM_WINDOWS) {
                            if (exeExecutableFileFilter.accept(files[i])) {
                                searchResult.put(files[i].getPath(), files[i]);
                                updateList();
                            }
                        } else if (conf.getDevelopmentHost().getBuildPlatform() == PlatformTypes.PLATFORM_MACOSX) {
                            if (machOExecutableFileFilter.accept(files[i])) {
                                searchResult.put(files[i].getPath(), files[i]);
                                updateList();
                            }
                        } else {
                            if (elfExecutableFileFilter.accept(files[i])) {
                                searchResult.put(files[i].getPath(), files[i]);
                                updateList();
                            }
                        }
                    }
                }
            }
            downPrev = downNext;
        }
    }

    private void updateList() {
        final List<String> keySet = new ArrayList<>(searchResult.keySet());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DefaultListModel model = new DefaultListModel();
                Object selected = exeList.getSelectedValue();
                Object first = null;
                for(String path : keySet) {
                    if (first == null) {
                        first = path;
                    }
                    model.addElement(path);
                }
                resetList = true;
                exeList.setModel(model);
                if (selected != null) {
                    exeList.setSelectedValue(selected, true);
                }
                resetList = false;
                if (selected == null) {
                    if (executableTextField.getText().isEmpty()) {
                        exeList.setSelectedValue(first, true);
                    }
                }
            }
        });
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        instructionsTextArea = new javax.swing.JTextArea();
        ExecutableListLabel = new javax.swing.JLabel();
        executableList = new javax.swing.JScrollPane();
        list = new javax.swing.JList();
        executableLabel = new javax.swing.JLabel();
        executableTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        errorLabel = new javax.swing.JLabel();
        progress = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        instructionsTextArea.setEditable(false);
        instructionsTextArea.setLineWrap(true);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/cnd/makeproject/ui/Bundle"); // NOI18N
        instructionsTextArea.setText(bundle.getString("GUIDANCE_TEXT")); // NOI18N
        instructionsTextArea.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        add(instructionsTextArea, gridBagConstraints);

        ExecutableListLabel.setLabelFor(executableList);
        org.openide.awt.Mnemonics.setLocalizedText(ExecutableListLabel, bundle.getString("LIST_LABEL_TEXT")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 12, 0, 12);
        add(ExecutableListLabel, gridBagConstraints);

        executableList.setViewportView(list);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
        add(executableList, gridBagConstraints);

        executableLabel.setLabelFor(executableTextField);
        org.openide.awt.Mnemonics.setLocalizedText(executableLabel, bundle.getString("EXECUTABLE_TEXT")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 12, 0, 12);
        add(executableLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        add(executableTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, bundle.getString("BROWSE_BUTTON_TEXT")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 12);
        add(browseButton, gridBagConstraints);

        errorLabel.setForeground(new java.awt.Color(255, 51, 51));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 12, 0, 0);
        add(errorLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 12, 0, 0);
        add(progress, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        final String chooser_key = "SelectExecutablePanel"; //NOI18N
        String seed;
        if (executableTextField.getText().length() > 0) {
            seed = executableTextField.getText();
        } else if (RemoteFileUtil.getCurrentChooserFile(chooser_key, conf.getDevelopmentHost().getExecutionEnvironment()) != null) {
            seed = RemoteFileUtil.getCurrentChooserFile(chooser_key, conf.getDevelopmentHost().getExecutionEnvironment());
        } else {
            seed = System.getProperty("user.home"); // NOI18N
        }
        FileFilter[] filters;
        if (conf.getDevelopmentHost().getBuildPlatform() == PlatformTypes.PLATFORM_WINDOWS) {
            filters = new FileFilter[]{FileFilterFactory.getPeExecutableFileFilter()};
        } else if (conf.getDevelopmentHost().getBuildPlatform() == PlatformTypes.PLATFORM_MACOSX) {
            filters = new FileFilter[]{FileFilterFactory.getMacOSXExecutableFileFilter()};
        } else {
            filters = new FileFilter[]{FileFilterFactory.getElfExecutableFileFilter()};
        }
        JFileChooser fileChooser = RemoteFileUtil.createFileChooser(
                conf.getDevelopmentHost().getExecutionEnvironment(),
                getString("CHOOSER_TITLE_TXT"),
                getString("CHOOSER_BUTTON_TXT"),
                JFileChooser.FILES_ONLY,
                filters,
                seed,
                false);
        
        int ret = fileChooser.showOpenDialog(this);
        if (ret == JFileChooser.CANCEL_OPTION) {
            return;
        }
        final File selectedFile = fileChooser.getSelectedFile();

        String path = CndPathUtilities.normalizeSlashes(selectedFile.getPath());
        RemoteFileUtil.setCurrentChooserFile(chooser_key, selectedFile.getParentFile().getPath(), conf.getDevelopmentHost().getExecutionEnvironment());
        executableTextField.setText(path);
    }//GEN-LAST:event_browseButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ExecutableListLabel;
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JLabel executableLabel;
    private javax.swing.JScrollPane executableList;
    private javax.swing.JTextField executableTextField;
    private javax.swing.JTextArea instructionsTextArea;
    private javax.swing.JList list;
    private javax.swing.JLabel progress;
    // End of variables declaration//GEN-END:variables

    /** Look up i18n strings here */
    private static String getString(String s) {
        return NbBundle.getMessage(SelectExecutablePanel.class, s);
    }
}
