/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.symfony2.ui.options;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.symfony2.commands.InstallerExecutable;
import org.netbeans.modules.php.symfony2.options.Symfony2Options;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

/**
 * Panel for SYmfony2 options.
 */
@NbBundle.Messages({
        "LBL_ZipFilesFilter=Zip File (*.zip)",
        "PhpOptions.Symfony2.keywordsTabTitle=Frameworks & Tools"
})
@OptionsPanelController.Keywords(keywords={"php", "symfony", "symfony2", "framework", "sf", "sf2"},
        location=UiUtils.OPTIONS_PATH, tabTitle= "#PhpOptions.Symfony2.keywordsTabTitle")
public class Symfony2OptionsPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(Symfony2OptionsPanel.class.getName());

    private static final String INSTALLER_LAST_FOLDER_SUFFIX = ".installer"; // NOI18N
    private static final String SANDBOX_LAST_FOLDER_SUFFIX = ".sandbox"; // NOI18N
    private static final FileFilter ZIP_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            return f.isFile()
                    && f.getName().toLowerCase().endsWith(".zip"); // NOI18N
        }
        @Override
        public String getDescription() {
            return Bundle.LBL_ZipFilesFilter();
        }
    };

    private final ChangeSupport changeSupport = new ChangeSupport(this);


    public Symfony2OptionsPanel() {
        assert EventQueue.isDispatchThread();
        initComponents();
        init();
    }

    @NbBundle.Messages({
        "# {0} - installer file name",
        "Symfony2OptionsPanel.installer.hint=Full path of Symfony Installer (typically {0}).",
    })
    private void init() {
        installerInfoLabel.setText(Bundle.Symfony2OptionsPanel_installer_hint(InstallerExecutable.NAME));
        errorLabel.setText(" "); // NOI18N
        enableComponents();
        initListeners();
    }

    private void initListeners() {
        DefaultDocumentListener defaultDocumentListener = new DefaultDocumentListener();
        installerTextField.getDocument().addDocumentListener(defaultDocumentListener);
        sandboxTextField.getDocument().addDocumentListener(defaultDocumentListener);
        DefaultItemListener defaultItemListener = new DefaultItemListener();
        installerRadioButton.addItemListener(defaultItemListener);
        sandboxRadioButton.addItemListener(defaultItemListener);
        ignoreCacheCheckBox.addItemListener(defaultItemListener);
    }

    public boolean isUseInstaller() {
        return installerRadioButton.isSelected();
    }

    public void setUseInstaller(boolean useInstaller) {
        if (useInstaller) {
            installerRadioButton.setSelected(true);
        } else {
            sandboxRadioButton.setSelected(true);
        }
    }

    public String getInstaller() {
        return installerTextField.getText();
    }

    public void setInstaller(String installer) {
        installerTextField.setText(installer);
    }

    public String getSandbox() {
        return sandboxTextField.getText();
    }

    public void setSandbox(String sandbox) {
        sandboxTextField.setText(sandbox);
    }

    public boolean getIgnoreCache() {
        return ignoreCacheCheckBox.isSelected();
    }

    public void setIgnoreCache(boolean ignoreCache) {
        ignoreCacheCheckBox.setSelected(ignoreCache);
    }

    public void setError(String message) {
        errorLabel.setText(" "); // NOI18N
        errorLabel.setForeground(UIManager.getColor("nb.errorForeground")); // NOI18N
        errorLabel.setText(message);
    }

    public void setWarning(String message) {
        errorLabel.setText(" "); // NOI18N
        errorLabel.setForeground(UIManager.getColor("nb.warningForeground")); // NOI18N
        errorLabel.setText(message);
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    void enableComponents() {
        boolean useInstaller = installerRadioButton.isSelected();
        installerTextField.setEnabled(useInstaller);
        installerBrowseButton.setEnabled(useInstaller);
        installerSearchButton.setEnabled(useInstaller);
        sandboxTextField.setEnabled(!useInstaller);
        sandboxBrowseButton.setEnabled(!useInstaller);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        useButtonGroup = new ButtonGroup();
        installerRadioButton = new JRadioButton();
        installerTextField = new JTextField();
        installerBrowseButton = new JButton();
        installerSearchButton = new JButton();
        installerInfoLabel = new JLabel();
        sandboxRadioButton = new JRadioButton();
        sandboxTextField = new JTextField();
        sandboxBrowseButton = new JButton();
        sandboxInfoLabel = new JLabel();
        ignoreCacheCheckBox = new JCheckBox();
        errorLabel = new JLabel();
        noteLabel = new JLabel();
        downloadLabel = new JLabel();

        useButtonGroup.add(installerRadioButton);
        Mnemonics.setLocalizedText(installerRadioButton, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.installerRadioButton.text")); // NOI18N

        Mnemonics.setLocalizedText(installerBrowseButton, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.installerBrowseButton.text")); // NOI18N
        installerBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                installerBrowseButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(installerSearchButton, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.installerSearchButton.text")); // NOI18N
        installerSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                installerSearchButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(installerInfoLabel, "HINT"); // NOI18N

        useButtonGroup.add(sandboxRadioButton);
        Mnemonics.setLocalizedText(sandboxRadioButton, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.sandboxRadioButton.text")); // NOI18N

        Mnemonics.setLocalizedText(sandboxBrowseButton, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.sandboxBrowseButton.text")); // NOI18N
        sandboxBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                sandboxBrowseButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(sandboxInfoLabel, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.sandboxInfoLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(ignoreCacheCheckBox, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.ignoreCacheCheckBox.text")); // NOI18N

        Mnemonics.setLocalizedText(errorLabel, "ERROR"); // NOI18N

        Mnemonics.setLocalizedText(noteLabel, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.noteLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(downloadLabel, NbBundle.getMessage(Symfony2OptionsPanel.class, "Symfony2OptionsPanel.downloadLabel.text")); // NOI18N
        downloadLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                downloadLabelMouseEntered(evt);
            }
            public void mousePressed(MouseEvent evt) {
                downloadLabelMousePressed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(downloadLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(ignoreCacheCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(errorLabel)
                    .addComponent(noteLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sandboxRadioButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addComponent(sandboxInfoLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(sandboxTextField))))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(sandboxBrowseButton))
            .addGroup(layout.createSequentialGroup()
                .addComponent(installerRadioButton)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addComponent(installerInfoLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(installerTextField)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(installerBrowseButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(installerSearchButton))))
        );
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(installerRadioButton)
                    .addComponent(installerTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(installerSearchButton)
                    .addComponent(installerBrowseButton))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(installerInfoLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(sandboxRadioButton)
                    .addComponent(sandboxTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(sandboxBrowseButton))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(sandboxInfoLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(ignoreCacheCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(noteLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(downloadLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(errorLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    @NbBundle.Messages("LBL_SelectSandbox=Select Symfony Standard Edition (.zip)")
    private void sandboxBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_sandboxBrowseButtonActionPerformed
        File sandbox = new FileChooserBuilder(Symfony2OptionsPanel.class.getName() + SANDBOX_LAST_FOLDER_SUFFIX)
                .setTitle(Bundle.LBL_SelectSandbox())
                .setFilesOnly(true)
                .setFileFilter(ZIP_FILE_FILTER)
                .showOpenDialog();
        if (sandbox != null) {
            sandbox = FileUtil.normalizeFile(sandbox);
            sandboxTextField.setText(sandbox.getAbsolutePath());
        }
    }//GEN-LAST:event_sandboxBrowseButtonActionPerformed

    private void downloadLabelMouseEntered(MouseEvent evt) {//GEN-FIRST:event_downloadLabelMouseEntered
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_downloadLabelMouseEntered

    private void downloadLabelMousePressed(MouseEvent evt) {//GEN-FIRST:event_downloadLabelMousePressed
        try {
            URL url = new URL("http://symfony.com/download"); // NOI18N
            HtmlBrowser.URLDisplayer.getDefault().showURL(url);
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }//GEN-LAST:event_downloadLabelMousePressed

    @NbBundle.Messages("Symfony2OptionsPanel.installer.browse.title=Select Symfony Installer")
    private void installerBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_installerBrowseButtonActionPerformed
        assert EventQueue.isDispatchThread();
        File file = new FileChooserBuilder(Symfony2OptionsPanel.class.getName() + INSTALLER_LAST_FOLDER_SUFFIX)
                .setFilesOnly(true)
                .setTitle(Bundle.Symfony2OptionsPanel_installer_browse_title())
                .showOpenDialog();
        if (file != null) {
            installerTextField.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_installerBrowseButtonActionPerformed

    @NbBundle.Messages({
        "Symfony2OptionsPanel.search.installer.title=Symfony Installers",
        "Symfony2OptionsPanel.search.installer=&Symfony Installers:",
        "Symfony2OptionsPanel.search.installer.pleaseWaitPart=Symfony Installers",
        "Symfony2OptionsPanel.search.installer.notFound=No Symfony Installers found.",
    })
    private void installerSearchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_installerSearchButtonActionPerformed
        assert EventQueue.isDispatchThread();
        String file = UiUtils.SearchWindow.search(new UiUtils.SearchWindow.SearchWindowSupport() {
            @Override
            public List<String> detect() {
                return FileUtils.findFileOnUsersPath(InstallerExecutable.NAME);
            }
            @Override
            public String getWindowTitle() {
                return Bundle.Symfony2OptionsPanel_search_installer_title();
            }
            @Override
            public String getListTitle() {
                return Bundle.Symfony2OptionsPanel_search_installer();
            }
            @Override
            public String getPleaseWaitPart() {
                return Bundle.Symfony2OptionsPanel_search_installer_pleaseWaitPart();
            }
            @Override
            public String getNoItemsFound() {
                return Bundle.Symfony2OptionsPanel_search_installer_notFound();
            }
        });
        if (file != null) {
            installerTextField.setText(file);
        }
    }//GEN-LAST:event_installerSearchButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel downloadLabel;
    private JLabel errorLabel;
    private JCheckBox ignoreCacheCheckBox;
    private JButton installerBrowseButton;
    private JLabel installerInfoLabel;
    private JRadioButton installerRadioButton;
    private JButton installerSearchButton;
    private JTextField installerTextField;
    private JLabel noteLabel;
    private JButton sandboxBrowseButton;
    private JLabel sandboxInfoLabel;
    private JRadioButton sandboxRadioButton;
    private JTextField sandboxTextField;
    private ButtonGroup useButtonGroup;
    // End of variables declaration//GEN-END:variables

    //~ Inner classes

    private final class DefaultDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            processUpdate();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            processUpdate();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            processUpdate();
        }

        private void processUpdate() {
            fireChange();
        }

    }

    private final class DefaultItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            enableComponents();
            fireChange();
        }

    }

}
