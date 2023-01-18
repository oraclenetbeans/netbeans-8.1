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

package org.netbeans.modules.mobility.jsr172.wizard;

import java.awt.Component;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import org.netbeans.modules.e2e.api.wsdl.wsdl2java.WSDL2Java;
import org.netbeans.modules.e2e.api.wsdl.wsdl2java.WSDL2Java.ValidationResult.ErrorLevel;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Michal Skvor
 */
public class ValidationNotifier extends javax.swing.JPanel {

    List<WSDL2Java.ValidationResult> validationResults;

    /** Creates new form ValidationNotifier */
    public ValidationNotifier(List<WSDL2Java.ValidationResult> validationResults) {
        this.validationResults = validationResults;

        initComponents();

        initData();

        initAccessibility();
    }

    private void initData() {
        validationList.setModel(new IconListModel(validationResults));
        validationList.setCellRenderer(new IconListRenderer());
    }

    private void initAccessibility() {
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(ClientInfo.class, "ACSD_Validation_Results")); // NOI18N

        validationList.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(ValidationNotifier.class, "ACSD_Validation_List")); // NOI18N
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        validationList = new javax.swing.JList();

        jLabel1.setText(org.openide.util.NbBundle.getMessage(ValidationNotifier.class, "LBL_Validation_Results")); // NOI18N

        jScrollPane1.setViewportView(validationList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList validationList;
    // End of variables declaration//GEN-END:variables

    private static final class IconListModel implements ListModel {

        private List<WSDL2Java.ValidationResult> validationResults;

        public IconListModel(List<WSDL2Java.ValidationResult> validationResults) {
            this.validationResults = validationResults;
        }

        @Override
        public int getSize() {
            return validationResults.size();
        }

        @Override
        public Object getElementAt(int index) {
            return validationResults.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener arg0) {
        }

        @Override
        public void removeListDataListener(ListDataListener arg0) {
        }
    }
    private static final Icon ICON_WARNING = ImageUtilities.loadImageIcon("org/netbeans/modules/mobility/jsr172/resources/warning.png", false); // NOI18N
    private static final Icon ICON_ERROR = ImageUtilities.loadImageIcon("org/netbeans/modules/mobility/jsr172/resources/error.png", false); // NOI18N

    private static final class IconListRenderer implements ListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList arg0, Object data,
                int arg2, boolean arg3, boolean arg4) {
            WSDL2Java.ValidationResult rowData = (WSDL2Java.ValidationResult) data;
            JLabel row = new JLabel();
            row.setText(rowData.getMessage());

            if (ErrorLevel.FATAL.equals(rowData.getErrorLevel())) {
                row.setIcon(ICON_ERROR);
            } else if (ErrorLevel.WARNING.equals(rowData.getErrorLevel())) {
                row.setIcon(ICON_WARNING);
            }

            return row;
        }
    }
}
