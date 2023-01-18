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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.glassfish.javaee.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author peterw99
 */
public class DebugPortQuery extends javax.swing.JPanel {

    /** Creates new form DebugPortQuery */
    public DebugPortQuery() {
        initComponents();
    }

    public void setDebugPort(String debugPort) {
        try {
            int port = Integer.valueOf(debugPort);
            if(port > 0 && port < 65536) {
                debugPortSpinner.getModel().setValue(Integer.valueOf(port));
            } else {
                Logger.getLogger("glassfish-javaee").log(
                        Level.INFO, "Debug port out of range in DebugPortQuery");
            }
        } catch(NumberFormatException ex) {
            Logger.getLogger("glassfish-javaee").log(
                    Level.INFO, "Invalid debug port, using default = 9009.", ex);
        }
    }

    public String getDebugPort() {
        Object value = debugPortSpinner.getModel().getValue();
        return value != null ? value.toString() : "";
    }

    public boolean shouldPersist() {
        return noAskCheck.getModel().isSelected();
    }

    @Override
    public Dimension getPreferredSize() {
        // Bump height by 16% because GroupLayout doesn't resize properly when
        // label text uses HTML to wrap lines.
        Dimension preferredSize = super.getPreferredSize();
        preferredSize.height = preferredSize.height * 29 / 25;
        return preferredSize;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        debugPortLable = new javax.swing.JLabel();
        debugPortSpinner = new javax.swing.JSpinner();
        noAskCheck = new javax.swing.JCheckBox();

        debugPortLable.setText(org.openide.util.NbBundle.getMessage(DebugPortQuery.class, "DebugPortQuery.debugPortLable.text")); // NOI18N
        debugPortLable.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        debugPortSpinner.setModel(new javax.swing.SpinnerNumberModel(9009, 1, 65535, 1));

        noAskCheck.setSelected(true);
        noAskCheck.setText(org.openide.util.NbBundle.getMessage(DebugPortQuery.class, "DebugPortQuery.noAskCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(debugPortLable, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(debugPortSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(204, 204, 204))
                    .addComponent(noAskCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(debugPortLable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(debugPortSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(noAskCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel debugPortLable;
    private javax.swing.JSpinner debugPortSpinner;
    private javax.swing.JCheckBox noAskCheck;
    // End of variables declaration//GEN-END:variables

}
