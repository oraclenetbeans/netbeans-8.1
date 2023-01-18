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
package org.netbeans.modules.web.clientproject.api.build.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.web.clientproject.api.build.BuildTools.CustomizerSupport;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

public final class CustomizerPanel extends JPanel {

    private final ProjectCustomizer.Category category;
    private final CustomizerSupport customizerSupport;
    private final List<BuildTask> buildTasks = new CopyOnWriteArrayList<>();


    public CustomizerPanel(CustomizerSupport customizerSupport) {
        assert EventQueue.isDispatchThread();
        assert customizerSupport != null;

        this.customizerSupport = customizerSupport;
        category = customizerSupport.getCategory();

        initComponents();
        init();
    }

    private void init() {
        assignLabel.setText(customizerSupport.getHeader());
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_BUILD, "build", buildCheckBox, buildTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_CLEAN, "clean", cleanCheckBox, cleanTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_REBUILD, "clean build", rebuildCheckBox, rebuildTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_RUN, "run", runProjectCheckBox, runProjectTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_DEBUG, "debug", debugProjectCheckBox, debugProjectTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_TEST, "test", testProjectCheckBox, testProjectTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_RUN_SINGLE, "runfile", runFileCheckBox, runFileTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_DEBUG_SINGLE, "debugfile", debugFileCheckBox, debugFileTextField)); // NOI18N
        buildTasks.add(new BuildTask(ActionProvider.COMMAND_TEST_SINGLE, "testfile", testFileCheckBox, testFileTextField)); // NOI18N
        // default values
        for (BuildTask buildTask : buildTasks) {
            buildTask.setText(customizerSupport.getTask(buildTask.getCommandId()));
        }
        // listeners
        category.setStoreListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
            }
        });
    }

    @NbBundle.Messages("CustomizerPanel.error.field.empty=Field cannot be empty")
    void validateData() {
        assert EventQueue.isDispatchThread();
        for (BuildTask buildTask : buildTasks) {
            String text = buildTask.getText();
            if (text != null
                    && text.isEmpty()) {
                category.setErrorMessage(Bundle.CustomizerPanel_error_field_empty());
                category.setValid(false);
                return;
            }
        }
        category.setErrorMessage(" "); // NOI18N
        category.setValid(true);
    }

    void saveData() {
        assert !EventQueue.isDispatchThread();
        for (BuildTask buildTask : buildTasks) {
            customizerSupport.setTask(buildTask.getCommandId(), buildTask.getText());
        }
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        assignLabel = new JLabel();
        buildCheckBox = new JCheckBox();
        buildTextField = new JTextField();
        cleanCheckBox = new JCheckBox();
        cleanTextField = new JTextField();
        rebuildCheckBox = new JCheckBox();
        rebuildTextField = new JTextField();
        runProjectCheckBox = new JCheckBox();
        runProjectTextField = new JTextField();
        debugProjectCheckBox = new JCheckBox();
        debugProjectTextField = new JTextField();
        testProjectCheckBox = new JCheckBox();
        testProjectTextField = new JTextField();
        runFileCheckBox = new JCheckBox();
        runFileTextField = new JTextField();
        debugFileCheckBox = new JCheckBox();
        debugFileTextField = new JTextField();
        testFileCheckBox = new JCheckBox();
        testFileTextField = new JTextField();

        Mnemonics.setLocalizedText(assignLabel, "TITLE"); // NOI18N

        Mnemonics.setLocalizedText(buildCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.buildCheckBox.text")); // NOI18N

        buildTextField.setColumns(20);

        Mnemonics.setLocalizedText(cleanCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.cleanCheckBox.text")); // NOI18N

        cleanTextField.setColumns(20);

        Mnemonics.setLocalizedText(rebuildCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.rebuildCheckBox.text")); // NOI18N

        rebuildTextField.setColumns(20);

        Mnemonics.setLocalizedText(runProjectCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.runProjectCheckBox.text")); // NOI18N

        runProjectTextField.setColumns(20);

        Mnemonics.setLocalizedText(debugProjectCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.debugProjectCheckBox.text")); // NOI18N

        debugProjectTextField.setColumns(20);

        Mnemonics.setLocalizedText(testProjectCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.testProjectCheckBox.text")); // NOI18N

        testProjectTextField.setColumns(20);

        Mnemonics.setLocalizedText(runFileCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.runFileCheckBox.text")); // NOI18N

        runFileTextField.setColumns(20);

        Mnemonics.setLocalizedText(debugFileCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.debugFileCheckBox.text")); // NOI18N

        debugFileTextField.setColumns(20);

        Mnemonics.setLocalizedText(testFileCheckBox, NbBundle.getMessage(CustomizerPanel.class, "CustomizerPanel.testFileCheckBox.text")); // NOI18N

        testFileTextField.setColumns(20);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(cleanCheckBox)
                    .addComponent(rebuildCheckBox)
                    .addComponent(buildCheckBox)
                    .addComponent(runProjectCheckBox)
                    .addComponent(debugProjectCheckBox)
                    .addComponent(testProjectCheckBox)
                    .addComponent(runFileCheckBox)
                    .addComponent(debugFileCheckBox)
                    .addComponent(testFileCheckBox))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(runFileTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(rebuildTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(cleanTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(buildTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(runProjectTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(debugProjectTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(testProjectTextField)
                    .addComponent(debugFileTextField, GroupLayout.Alignment.TRAILING)
                    .addComponent(testFileTextField, GroupLayout.Alignment.TRAILING)))
            .addGroup(layout.createSequentialGroup()
                .addComponent(assignLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(assignLabel)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(buildCheckBox)
                    .addComponent(buildTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(cleanCheckBox)
                    .addComponent(cleanTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(rebuildCheckBox)
                    .addComponent(rebuildTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(runProjectCheckBox)
                    .addComponent(runProjectTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(debugProjectCheckBox)
                    .addComponent(debugProjectTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(testProjectCheckBox)
                    .addComponent(testProjectTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(runFileCheckBox)
                    .addComponent(runFileTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(debugFileCheckBox)
                    .addComponent(debugFileTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(testFileCheckBox)
                    .addComponent(testFileTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel assignLabel;
    private JCheckBox buildCheckBox;
    private JTextField buildTextField;
    private JCheckBox cleanCheckBox;
    private JTextField cleanTextField;
    private JCheckBox debugFileCheckBox;
    private JTextField debugFileTextField;
    private JCheckBox debugProjectCheckBox;
    private JTextField debugProjectTextField;
    private JCheckBox rebuildCheckBox;
    private JTextField rebuildTextField;
    private JCheckBox runFileCheckBox;
    private JTextField runFileTextField;
    private JCheckBox runProjectCheckBox;
    private JTextField runProjectTextField;
    private JCheckBox testFileCheckBox;
    private JTextField testFileTextField;
    private JCheckBox testProjectCheckBox;
    private JTextField testProjectTextField;
    // End of variables declaration//GEN-END:variables

    //~ Inner classes

    private final class BuildTask {

        private final String commandId;
        private final String defaultValue;
        private final JCheckBox checkBox;
        private final JTextField textField;


        BuildTask(String commandId, String defaultValue, JCheckBox checkBox, JTextField textField) {
            assert commandId != null;
            assert defaultValue != null;
            assert checkBox != null;
            assert textField != null;
            this.commandId = commandId;
            this.defaultValue = defaultValue;
            this.checkBox = checkBox;
            this.textField = textField;
            init();
        }

        private void init() {
            checkBox.addItemListener(new DefaultItemListener(textField));
            textField.getDocument().addDocumentListener(new DefaultDocumentListener());
        }

        public String getCommandId() {
            return commandId;
        }

        @CheckForNull
        public String getText() {
            if (!checkBox.isSelected()) {
                return null;
            }
            return textField.getText().trim();
        }

        public void setText(@NullAllowed String text) {
            boolean hasText = text != null;
            checkBox.setSelected(hasText);
            textField.setText(hasText ? text : defaultValue);
            textField.setEnabled(hasText);
        }

    }

    private final class DefaultItemListener implements ItemListener {

        private final JTextField textField;


        public DefaultItemListener(JTextField textField) {
            assert textField != null;
            this.textField = textField;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            textField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            validateData();
        }

    }

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
            validateData();
        }

    }

}
