/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript.grunt.ui.options;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.javascript.grunt.exec.GruntExecutable;
import org.netbeans.modules.javascript.grunt.util.FileUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

@OptionsPanelController.Keywords(keywords={"#KW.GruntOptionsPanel"}, location="Html5", tabTitle= "Grunt")
public class GruntOptionsPanel extends JPanel implements HelpCtx.Provider {

    private static final Logger LOGGER = Logger.getLogger(GruntOptionsPanel.class.getName());

    private final ChangeSupport changeSupport = new ChangeSupport(this);


    public GruntOptionsPanel() {
        assert EventQueue.isDispatchThread();
        initComponents();

        init();
    }

    @NbBundle.Messages({
        "# {0} - grunt file name",
        "GruntOptionsPanel.grunt.hint=Full path of Grunt file (typically {0}).",
    })
    private void init() {
        errorLabel.setText(" "); // NOI18N
        hintLabel.setText(Bundle.GruntOptionsPanel_grunt_hint(GruntExecutable.GRUNT_NAME));
        gruntTextField.getDocument().addDocumentListener(new DefaultDocumentListener());
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
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

    public String getGrunt() {
        return gruntTextField.getText();
    }

    public void setGrunt(String grunt) {
        gruntTextField.setText(grunt);
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.netbeans.modules.javascript.grunt.ui.options.GruntOptionsPanel"); // NOI18N
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gruntLabel = new JLabel();
        gruntTextField = new JTextField();
        gruntBrowseButton = new JButton();
        gruntSearchButton = new JButton();
        hintLabel = new JLabel();
        installLabel = new JLabel();
        errorLabel = new JLabel();

        gruntLabel.setLabelFor(gruntTextField);
        Mnemonics.setLocalizedText(gruntLabel, NbBundle.getMessage(GruntOptionsPanel.class, "GruntOptionsPanel.gruntLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(gruntBrowseButton, NbBundle.getMessage(GruntOptionsPanel.class, "GruntOptionsPanel.gruntBrowseButton.text")); // NOI18N
        gruntBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                gruntBrowseButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(gruntSearchButton, NbBundle.getMessage(GruntOptionsPanel.class, "GruntOptionsPanel.gruntSearchButton.text")); // NOI18N
        gruntSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                gruntSearchButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(hintLabel, "HINT"); // NOI18N

        Mnemonics.setLocalizedText(installLabel, NbBundle.getMessage(GruntOptionsPanel.class, "GruntOptionsPanel.installLabel.text")); // NOI18N
        installLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                installLabelMousePressed(evt);
            }
            public void mouseEntered(MouseEvent evt) {
                installLabelMouseEntered(evt);
            }
        });

        Mnemonics.setLocalizedText(errorLabel, "ERROR"); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(gruntLabel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hintLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(installLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(gruntTextField)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gruntBrowseButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gruntSearchButton))))
            .addGroup(layout.createSequentialGroup()
                .addComponent(errorLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(gruntLabel)
                    .addComponent(gruntTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(gruntBrowseButton)
                    .addComponent(gruntSearchButton))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(hintLabel)
                    .addComponent(installLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(errorLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void installLabelMouseEntered(MouseEvent evt) {//GEN-FIRST:event_installLabelMouseEntered
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_installLabelMouseEntered

    private void installLabelMousePressed(MouseEvent evt) {//GEN-FIRST:event_installLabelMousePressed
        try {
            HtmlBrowser.URLDisplayer.getDefault().showURL(new URL("http://gruntjs.com/getting-started")); // NOI18N
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }//GEN-LAST:event_installLabelMousePressed

    @NbBundle.Messages("GruntOptionsPanel.grunt.browse.title=Select Grunt")
    private void gruntBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_gruntBrowseButtonActionPerformed
        assert EventQueue.isDispatchThread();
        File file = new FileChooserBuilder(GruntOptionsPanel.class)
                .setFilesOnly(true)
                .setTitle(Bundle.GruntOptionsPanel_grunt_browse_title())
                .showOpenDialog();
        if (file != null) {
            gruntTextField.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_gruntBrowseButtonActionPerformed

    @NbBundle.Messages("GruntOptionsPanel.grunt.none=No Grunt executable was found.")
    private void gruntSearchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_gruntSearchButtonActionPerformed
        assert EventQueue.isDispatchThread();
        for (String grunt : FileUtils.findFileOnUsersPath(GruntExecutable.GRUNT_NAME)) {
            gruntTextField.setText(new File(grunt).getAbsolutePath());
            return;
        }
        // no grunt found
        StatusDisplayer.getDefault().setStatusText(Bundle.GruntOptionsPanel_grunt_none());
    }//GEN-LAST:event_gruntSearchButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel errorLabel;
    private JButton gruntBrowseButton;
    private JLabel gruntLabel;
    private JButton gruntSearchButton;
    private JTextField gruntTextField;
    private JLabel hintLabel;
    private JLabel installLabel;
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

}
