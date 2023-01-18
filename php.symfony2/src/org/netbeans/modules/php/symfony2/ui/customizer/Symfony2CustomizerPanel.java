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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.symfony2.ui.customizer;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.symfony2.ui.options.Symfony2OptionsPanelController;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

public class Symfony2CustomizerPanel extends JPanel {

    private static final long serialVersionUID = -464365465761315L;

    private final FileObject sources;
    private final ChangeSupport changeSupport = new ChangeSupport(this);


    public Symfony2CustomizerPanel(FileObject sources) {
        this.sources = sources;

        initComponents();
        init();
        setFieldsEnabled(enabledCheckBox.isSelected());
    }

    private void init() {
        enabledCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                fireChange();
                setFieldsEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        appDirTextField.getDocument().addDocumentListener(new DocumentListener() {
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
                fireChange();
            }
        });
    }

    public boolean isSupportEnabled() {
        return enabledCheckBox.isSelected();
    }

    public void setSupportEnabled(boolean enabled) {
        enabledCheckBox.setSelected(enabled);
    }

    public String getAppDirectory() {
        return appDirTextField.getText().replace(File.separatorChar, '/'); // NOI18N
    }

    public void setAppDirectory(String appDir) {
        appDirTextField.setText(appDir.replace('/', File.separatorChar)); // NOI18N
    }

    public boolean isIgnoreCacheDirectory() {
        return ignoreCacheDirectoryCheckBox.isSelected();
    }

    public void setIgnoreCacheDirectory(boolean ignore) {
        ignoreCacheDirectoryCheckBox.setSelected(ignore);
    }

    public void addChangeListener(ChangeListener changeListener) {
        changeSupport.addChangeListener(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener) {
        changeSupport.removeChangeListener(changeListener);
    }

    final void setFieldsEnabled(boolean enabled) {
        appDirTextField.setEnabled(enabled);
        appDirBrowseButton.setEnabled(enabled);
        ignoreCacheDirectoryCheckBox.setEnabled(enabled);
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        infoLabel = new JLabel();
        optionsLabel = new JLabel();
        enabledCheckBox = new JCheckBox();
        enabledInfoLabel = new JLabel();
        appDirLabel = new JLabel();
        appDirTextField = new JTextField();
        appDirBrowseButton = new JButton();
        ignoreCacheDirectoryCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(infoLabel, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.infoLabel.text"));
        Mnemonics.setLocalizedText(optionsLabel, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.optionsLabel.text"));
        optionsLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                optionsLabelMouseEntered(evt);
            }
            public void mousePressed(MouseEvent evt) {
                optionsLabelMousePressed(evt);
            }
        });
        Mnemonics.setLocalizedText(enabledCheckBox, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.enabledCheckBox.text"));
        Mnemonics.setLocalizedText(enabledInfoLabel, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.enabledInfoLabel.text"));
        Mnemonics.setLocalizedText(appDirLabel, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.appDirLabel.text"));
        Mnemonics.setLocalizedText(appDirBrowseButton, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.appDirBrowseButton.text"));
        appDirBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                appDirBrowseButtonActionPerformed(evt);
            }
        });

        ignoreCacheDirectoryCheckBox.setSelected(true);
        Mnemonics.setLocalizedText(ignoreCacheDirectoryCheckBox, NbBundle.getMessage(Symfony2CustomizerPanel.class, "Symfony2CustomizerPanel.ignoreCacheDirectoryCheckBox.text"));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup()
                .addComponent(appDirLabel)

                .addPreferredGap(ComponentPlacement.RELATED).addComponent(appDirTextField).addPreferredGap(ComponentPlacement.RELATED).addComponent(appDirBrowseButton)).addGroup(layout.createSequentialGroup()

                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(ignoreCacheDirectoryCheckBox).addComponent(enabledCheckBox)).addContainerGap()).addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)

                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(enabledInfoLabel).addGroup(layout.createSequentialGroup()
                        .addComponent(infoLabel)

                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(optionsLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(enabledCheckBox)

                .addPreferredGap(ComponentPlacement.RELATED).addComponent(enabledInfoLabel).addPreferredGap(ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(appDirLabel).addComponent(appDirTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(appDirBrowseButton)).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(ignoreCacheDirectoryCheckBox).addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(infoLabel).addComponent(optionsLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void optionsLabelMouseEntered(MouseEvent evt) {//GEN-FIRST:event_optionsLabelMouseEntered
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_optionsLabelMouseEntered

    private void optionsLabelMousePressed(MouseEvent evt) {//GEN-FIRST:event_optionsLabelMousePressed
        UiUtils.showOptions(Symfony2OptionsPanelController.OPTIONS_SUBPATH);
    }//GEN-LAST:event_optionsLabelMousePressed

    @NbBundle.Messages("Symfony2CustomizerPanel.browseAppDir.title=Select \"app\" directory")
    private void appDirBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_appDirBrowseButtonActionPerformed
        File appDir = new FileChooserBuilder(Symfony2CustomizerPanel.class)
                .setTitle(Bundle.Symfony2CustomizerPanel_browseAppDir_title())
                .setDirectoriesOnly(true)
                .setDefaultWorkingDirectory(FileUtil.toFile(sources))
                .forceUseOfDefaultWorkingDirectory(true)
                .showOpenDialog();
        if (appDir != null) {
            appDir = FileUtil.normalizeFile(appDir);
            FileObject fo = FileUtil.toFileObject(appDir);
            if (FileUtil.isParentOf(sources, fo)) {
                // it is ok
                String relativePath = FileUtil.getRelativePath(sources, fo);
                assert relativePath != null : sources + " not parent of " + fo;
                setAppDirectory(relativePath);
            } else {
                // not ok
                appDirTextField.setText(appDir.getAbsolutePath());
            }
        }
    }//GEN-LAST:event_appDirBrowseButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton appDirBrowseButton;
    private JLabel appDirLabel;
    private JTextField appDirTextField;
    private JCheckBox enabledCheckBox;
    private JLabel enabledInfoLabel;
    private JCheckBox ignoreCacheDirectoryCheckBox;
    private JLabel infoLabel;
    private JLabel optionsLabel;
    // End of variables declaration//GEN-END:variables

}
