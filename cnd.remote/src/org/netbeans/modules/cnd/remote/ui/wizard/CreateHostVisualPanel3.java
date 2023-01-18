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

package org.netbeans.modules.cnd.remote.ui.wizard;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.text.html.HTMLEditorKit;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.api.toolchain.PlatformTypes;
import org.netbeans.modules.cnd.remote.server.RemoteServerRecord;
import org.netbeans.modules.cnd.remote.sync.SyncUtils;
import org.netbeans.modules.cnd.spi.remote.RemoteSyncFactory;
import org.openide.util.NbBundle;

/*package*/ final class CreateHostVisualPanel3 extends JPanel {

    public CreateHostVisualPanel3(CreateHostData data) {
        this.data = data;
        initComponents();
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(getClass(), "CreateHostVisualPanel3.Title");//NOI18N
    }

    private final CreateHostData data;
    private CompilerSetManager compilerSetManager;

    void init() {
        textHostDisplayName.setText(data.getExecutionEnvironment().getDisplayName());
        // here we know for sure that it is created and initialized
        compilerSetManager = data.getCacheManager().getCompilerSetManagerCopy(data.getExecutionEnvironment(), false);
        labelPlatformValue.setText(PlatformTypes.toString(compilerSetManager.getPlatform()));
        labelUsernameValue.setText(data.getExecutionEnvironment().getUser());
        labelHostnameValue.setText(data.getExecutionEnvironment().getHost());
        cbDefaultToolchain.setModel(new DefaultComboBoxModel(compilerSetManager.getCompilerSets().toArray()));
        cbDefaultToolchain.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel out = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    out.setText(""); //NOI18N
                } else if (value instanceof String) {
                    // BasicComboBoxUI replaces null with empty string
                    assert ((String) value).trim().isEmpty();
                    out.setText(""); //NOI18N
                } else {
                    CompilerSet cset = (CompilerSet) value;
                    out.setText(cset.getName());
                }
                return out;
            }
        });
        boolean selected = false;
        for(CompilerSet cs : compilerSetManager.getCompilerSets()) {
            if (compilerSetManager.isDefaultCompilerSet(cs)) {
                cbDefaultToolchain.setSelectedItem(cs);
                selected = true;
                break;
            }
        }
        if (!selected && compilerSetManager.getCompilerSets().size() > 0) {
            cbDefaultToolchain.setSelectedItem(0);
        }
        cbDefaultToolchain.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                compilerSetManager.setDefault((CompilerSet) cbDefaultToolchain.getSelectedItem());
            }
        });
        List<CompilerSet> sets2 = compilerSetManager.getCompilerSets();
        final String html = "<html>"; // NOI18N
        StringBuilder st = new StringBuilder(html);
        for (CompilerSet set : sets2) {
            if (st.length() > html.length()) {
                st.append("<br>\n"); //NOI18N
            }
            st.append(set.getName()).append(" (").append(set.getDirectory()).append(")");//NOI18N
        }
        RemoteServerRecord record = (RemoteServerRecord) ServerList.get(data.getExecutionEnvironment());
        if (record != null && record.hasProblems()) {
            st.append("<br><br>\n"); // NOI18N
            st.append("<font color=red>"); // NOI18N
            st.append(record.getProblems().replace("\n", "<br>\n")); // NOI18N
        }
        st.append("</html>"); // NOI18N
        jTextArea1.setEditorKit(new HTMLEditorKit());
        jTextArea1.setText(st.toString());

        SyncUtils.arrangeComboBox(cbSyncMode, data.getExecutionEnvironment());
        cbSyncMode.setSelectedItem(record.getSyncFactory());
    }

    String getHostDisplayName() {
        return textHostDisplayName.getText();
    }

    RemoteSyncFactory getRemoteSyncFactory() {
        return (RemoteSyncFactory) cbSyncMode.getSelectedItem();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        syncButtonGroup = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        textHostDisplayName = new javax.swing.JTextField();
        labelPlatform = new javax.swing.JLabel();
        labelHostname = new javax.swing.JLabel();
        labelUsername = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cbDefaultToolchain = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JEditorPane();
        labelPlatformValue = new javax.swing.JLabel();
        labelHostnameValue = new javax.swing.JLabel();
        labelUsernameValue = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cbSyncMode = new javax.swing.JComboBox();

        setPreferredSize(new java.awt.Dimension(534, 409));
        setRequestFocusEnabled(false);

        jLabel1.setLabelFor(textHostDisplayName);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.jLabel1.text")); // NOI18N

        labelPlatform.setLabelFor(labelPlatformValue);
        labelPlatform.setText(org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.labelPlatform.text")); // NOI18N

        labelHostname.setLabelFor(labelHostnameValue);
        labelHostname.setText(org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.labelHostname.text")); // NOI18N

        labelUsername.setLabelFor(labelUsernameValue);
        labelUsername.setText(org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.labelUsername.text")); // NOI18N

        jLabel2.setLabelFor(jTextArea1);
        jLabel2.setText(org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.jLabel2.text")); // NOI18N

        jLabel3.setLabelFor(cbDefaultToolchain);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.jLabel3.text")); // NOI18N

        jTextArea1.setEditable(false);
        jTextArea1.setOpaque(false);
        jScrollPane1.setViewportView(jTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(labelPlatformValue, org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.labelPlatformValue.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelHostnameValue, org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.labelHostnameValue.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelUsernameValue, org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.labelUsernameValue.text")); // NOI18N

        jLabel4.setLabelFor(cbSyncMode);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(CreateHostVisualPanel3.class, "CreateHostVisualPanel3.jLabel4.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textHostDisplayName, javax.swing.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelPlatform)
                    .addComponent(labelHostname)
                    .addComponent(labelUsername))
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelUsernameValue)
                    .addComponent(labelHostnameValue)
                    .addComponent(labelPlatformValue))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbSyncMode, 0, 376, Short.MAX_VALUE)
                    .addComponent(cbDefaultToolchain, 0, 376, Short.MAX_VALUE)))
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textHostDisplayName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelPlatform)
                    .addComponent(labelPlatformValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelHostname)
                    .addComponent(labelHostnameValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelUsername)
                    .addComponent(labelUsernameValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cbDefaultToolchain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbSyncMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(29, 29, 29))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox cbDefaultToolchain;
    private javax.swing.JComboBox cbSyncMode;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JEditorPane jTextArea1;
    private javax.swing.JLabel labelHostname;
    private javax.swing.JLabel labelHostnameValue;
    private javax.swing.JLabel labelPlatform;
    private javax.swing.JLabel labelPlatformValue;
    private javax.swing.JLabel labelUsername;
    private javax.swing.JLabel labelUsernameValue;
    private javax.swing.ButtonGroup syncButtonGroup;
    private javax.swing.JTextField textHostDisplayName;
    // End of variables declaration//GEN-END:variables
}

