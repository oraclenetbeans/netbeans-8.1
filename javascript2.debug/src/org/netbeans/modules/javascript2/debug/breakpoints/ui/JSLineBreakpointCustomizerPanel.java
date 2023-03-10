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
package org.netbeans.modules.javascript2.debug.breakpoints.ui;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Properties;
import org.netbeans.modules.javascript2.debug.JSUtils;
import org.netbeans.modules.javascript2.debug.breakpoints.JSLineBreakpoint;
import org.netbeans.spi.debugger.ui.Controller;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.Line;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;

/**
 *
 * @author david
 */
public class JSLineBreakpointCustomizerPanel extends javax.swing.JPanel 
                                             implements ControllerProvider, HelpCtx.Provider {

    private static final int MAX_SAVED_CONDITIONS = 10;
    private static final RequestProcessor RP = new RequestProcessor(JSLineBreakpointCustomizerPanel.class);
    
    private final Controller controller;
    private final JSLineBreakpoint lb;
    private boolean createBreakpoint;
    
    private static JSLineBreakpoint createBreakpoint() {
        Line line = JSUtils.getCurrentLine();
        if (line == null) {
            return null;
        }
        return new JSLineBreakpoint(line);
    }
    
    /**
     * Creates new form LineBreakpointCustomizer
     */
    public JSLineBreakpointCustomizerPanel() {
        this(createBreakpoint());
        createBreakpoint = true;
    }
    
    /**
     * Creates new form LineBreakpointCustomizer
     */
    public JSLineBreakpointCustomizerPanel(JSLineBreakpoint lb) {
        this.lb = lb;
        initComponents();
        controller = createController();
        if (lb != null) {
            Line line = lb.getLine();
            FileObject fo = line.getLookup().lookup(FileObject.class);
            if (fo != null) {
                File file = FileUtil.toFile(fo);
                if (file != null) {
                    fileTextField.setText(file.getAbsolutePath());
                } else {
                    fileTextField.setText(fo.toURL().toExternalForm());
                }
            }
            lineTextField.setText(Integer.toString(line.getLineNumber() + 1));
            Object[] conditions = getSavedConditions();
            conditionComboBox.setModel(new DefaultComboBoxModel(conditions));
            String condition = lb.getCondition();
            if (condition != null && !condition.isEmpty()) {
                conditionCheckBox.setSelected(true);
                conditionComboBox.setEnabled(true);
                conditionComboBox.getEditor().setItem(condition);
            } else {
                conditionCheckBox.setSelected(false);
                conditionComboBox.setEnabled(false);
            }
        }
    }
    
    private static Object[] getSavedConditions() {
        return Properties.getDefault().getProperties("debugger.javascript").
                getArray("BPConditions", new Object[0]);
    }
    
    private static void saveCondition(String condition) {
        Object[] savedConditions = getSavedConditions();
        Object[] conditions = null;
        boolean containsCondition = false;
        for (int i = 0; i < savedConditions.length; i++) {
            Object c = savedConditions[i];
            if (condition.equals(c)) {
                containsCondition = true;
                conditions = savedConditions;
                if (i > 0) {
                    System.arraycopy(conditions, 0, conditions, 1, i);
                    conditions[0] = condition;
                }
                break;
            }
        }
        if (!containsCondition) {
            if (savedConditions.length < MAX_SAVED_CONDITIONS) {
                conditions = new Object[savedConditions.length + 1];
                conditions[0] = condition;
                System.arraycopy(savedConditions, 0, conditions, 1, savedConditions.length);
            } else {
                conditions = savedConditions;
                System.arraycopy(conditions, 0, conditions, 1, conditions.length - 1);
                conditions[0] = condition;
            }
        }
        Properties.getDefault().getProperties("debugger.javascript").
                setArray("BPConditions", conditions);
    }
    
    protected Controller createController() {
        return new CustomizerController();
    }
    
    public Controller getController() {
        return controller;
    }
    
    protected RequestProcessor getUpdateRP() {
        return RP;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileLabel = new javax.swing.JLabel();
        fileTextField = new javax.swing.JTextField();
        lineLabel = new javax.swing.JLabel();
        lineTextField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        conditionCheckBox = new javax.swing.JCheckBox();
        conditionComboBox = new javax.swing.JComboBox();

        fileLabel.setLabelFor(fileTextField);
        org.openide.awt.Mnemonics.setLocalizedText(fileLabel, org.openide.util.NbBundle.getMessage(JSLineBreakpointCustomizerPanel.class, "JSLineBreakpointCustomizerPanel.fileLabel.text")); // NOI18N

        fileTextField.setToolTipText(org.openide.util.NbBundle.getMessage(JSLineBreakpointCustomizerPanel.class, "JSLineBreakpointCustomizerPanel.fileTextField.toolTipText")); // NOI18N

        lineLabel.setLabelFor(lineTextField);
        org.openide.awt.Mnemonics.setLocalizedText(lineLabel, org.openide.util.NbBundle.getMessage(JSLineBreakpointCustomizerPanel.class, "JSLineBreakpointCustomizerPanel.lineLabel.text")); // NOI18N

        lineTextField.setToolTipText(org.openide.util.NbBundle.getMessage(JSLineBreakpointCustomizerPanel.class, "JSLineBreakpointCustomizerPanel.lineTextField.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(conditionCheckBox, org.openide.util.NbBundle.getMessage(JSLineBreakpointCustomizerPanel.class, "JSLineBreakpointCustomizerPanel.conditionCheckBox.text")); // NOI18N
        conditionCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conditionCheckBoxActionPerformed(evt);
            }
        });

        conditionComboBox.setEditable(true);
        conditionComboBox.setToolTipText(org.openide.util.NbBundle.getMessage(JSLineBreakpointCustomizerPanel.class, "JSLineBreakpointCustomizerPanel.conditionComboBox.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(conditionCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(conditionComboBox, 0, 330, Short.MAX_VALUE))
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lineLabel)
                            .addComponent(fileLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fileTextField)
                            .addComponent(lineTextField))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileLabel)
                    .addComponent(fileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lineLabel)
                    .addComponent(lineTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(conditionCheckBox)
                    .addComponent(conditionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void conditionCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conditionCheckBoxActionPerformed
        conditionComboBox.setEnabled(conditionCheckBox.isSelected());
        if (conditionCheckBox.isSelected()) {
            conditionComboBox.requestFocusInWindow();
        }
    }//GEN-LAST:event_conditionCheckBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox conditionCheckBox;
    private javax.swing.JComboBox conditionComboBox;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JTextField fileTextField;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lineLabel;
    private javax.swing.JTextField lineTextField;
    // End of variables declaration//GEN-END:variables

    @Override
    public HelpCtx getHelpCtx() {
        return new org.openide.util.HelpCtx("NetbeansDebuggerLineBreakpointJavaScript"); // NOI18N
    }
    
    private static String toURL(String filePath) {
        URI uri = null;
        try {
            uri = URI.create(filePath);
        } catch (Exception ex) {
        }
        if (uri == null || !uri.isAbsolute()) {
            File f = new File(filePath);
            uri = f.toURI();
        }
        try {
            return uri.toURL().toExternalForm();
        } catch (MalformedURLException ex) {
        }
        return filePath;
    }
    
    private class CustomizerController implements Controller {

        @Override
        public boolean ok() {
            JSLineBreakpoint lb = JSLineBreakpointCustomizerPanel.this.lb;
            String fileStr = toURL(fileTextField.getText());
            int lineNumber;
            try {
                lineNumber = Integer.parseInt(lineTextField.getText());
            } catch (NumberFormatException nfex) {
                return false;
            }
            lineNumber--;
            Line line = JSUtils.getLine(fileStr, lineNumber);
            if (line == null) {
                return false;
            }
            if (lb != null) {
                updateBreakpoint(line);
            } else {
                lb = new JSLineBreakpoint(line);
            }
            String condition = null;
            if (conditionCheckBox.isSelected()) {
                condition = conditionComboBox.getSelectedItem().toString().trim();
            }
            if (condition != null && !condition.isEmpty()) {
                lb.setCondition(condition);
                saveCondition(condition);
            } else {
                lb.setCondition(null);
            }
            if (createBreakpoint) {
                DebuggerManager.getDebuggerManager().addBreakpoint(lb);
            }
            return true;
        }
        
        private void updateBreakpoint(final Line line) {
            getUpdateRP().post(new Runnable() {
                @Override
                public void run() {
                    lb.setLine(line);
                }
            });
        }

        @Override
        public boolean cancel() {
            return true;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }
        
    }
}
