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

/*
 * WsimportOptionsPanel.java
 *
 * Created on Sep 15, 2008, 10:02:05 AM
 */
package org.netbeans.modules.websvc.customization.jaxwssettings.panel;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.websvc.api.jaxws.project.config.WsimportOption;
import org.netbeans.modules.websvc.api.jaxws.project.config.WsimportOptions;
import org.openide.util.NbBundle;

/**
 *
 * @author rico
 */
public class WsimportOptionsPanel extends javax.swing.JPanel {

    private List<WsimportOption> wsimportOptions;
    private List<WsimportOption> jaxbOptions;
    private List<String> reservedOptions;
    private String[] columnNames;
    private WsimportOptions wsimportOptionParent;

    /** Creates new form WsimportOptionsPanel */
    public WsimportOptionsPanel(List<WsimportOption> wsimportOptions, List<WsimportOption> jaxbOptions, WsimportOptions wsimportOptionParent, String jvmOptions) {

        this.wsimportOptions = wsimportOptions;
        this.jaxbOptions = jaxbOptions;
        this.wsimportOptionParent = wsimportOptionParent;
        columnNames = new String[] {
            NbBundle.getMessage(WsimportOptionsPanel.class,"HEADING_OPTION"),
            NbBundle.getMessage(WsimportOptionsPanel.class,"HEADING_VALUE")
        };
        reservedOptions = getReservedOptions();
        initComponents();
        jvmTextField.setText(jvmOptions);
        
    }

    private List<String> getReservedOptions() {
        if (reservedOptions == null) {
            reservedOptions = new ArrayList<String>();
            reservedOptions.add("destdir"); //NOI18N
            reservedOptions.add("wsdl"); //NOI18N
            reservedOptions.add("sourcedestdir"); //NOI18N
            reservedOptions.add("catalog"); //NOI18N
        }
        return reservedOptions;
    }

    public List<WsimportOption> getWsimportOptions(){
        return wsimportOptionsPanel.getOptions();
    }

    public List<WsimportOption> getJaxbOptions(){
        return jaxbOptionsPanel.getOptions();
    }
    
    public String getJvmOptions(){
        return jvmTextField.getText().trim();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        wsimportOptionsPanel = new OptionsPanel(columnNames, wsimportOptions, reservedOptions, wsimportOptionParent);
        jaxbOptionsPanel = new OptionsPanel(columnNames, jaxbOptions, null, wsimportOptionParent);
        wsimportLabel = new javax.swing.JLabel();
        jaxbLabel = new javax.swing.JLabel();
        jvmLabel = new javax.swing.JLabel();
        jvmDescriptionLabel = new javax.swing.JLabel();
        jvmTextField = new javax.swing.JTextField();

        wsimportLabel.setLabelFor(wsimportOptionsPanel);
        org.openide.awt.Mnemonics.setLocalizedText(wsimportLabel, org.openide.util.NbBundle.getMessage(WsimportOptionsPanel.class, "LBL_WSIMPORT_OPTIONS")); // NOI18N

        jaxbLabel.setLabelFor(jaxbOptionsPanel);
        org.openide.awt.Mnemonics.setLocalizedText(jaxbLabel, org.openide.util.NbBundle.getMessage(WsimportOptionsPanel.class, "Label_JAXB_OPTIONS")); // NOI18N

        jvmLabel.setLabelFor(jvmTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jvmLabel, org.openide.util.NbBundle.getMessage(WsimportOptionsPanel.class, "WsimportOptionsPanel.jvmLabel.text")); // NOI18N

        jvmDescriptionLabel.setText(org.openide.util.NbBundle.getMessage(WsimportOptionsPanel.class, "WsimportOptionsPanel.jvmDescriptionLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jaxbOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE)
                    .addComponent(wsimportOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(wsimportLabel)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jvmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jvmDescriptionLabel)
                                    .addComponent(jvmTextField))))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jaxbLabel)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(wsimportLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wsimportOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jvmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jvmTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jvmDescriptionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jaxbLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jaxbOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                .addGap(18, 18, 18))
        );

        jvmTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(WsimportOptionsPanel.class, "WsimportOptionsPanel.jvmTextField.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jaxbLabel;
    private org.netbeans.modules.websvc.customization.jaxwssettings.panel.OptionsPanel jaxbOptionsPanel;
    private javax.swing.JLabel jvmDescriptionLabel;
    private javax.swing.JLabel jvmLabel;
    private javax.swing.JTextField jvmTextField;
    private javax.swing.JLabel wsimportLabel;
    private org.netbeans.modules.websvc.customization.jaxwssettings.panel.OptionsPanel wsimportOptionsPanel;
    // End of variables declaration//GEN-END:variables
}
