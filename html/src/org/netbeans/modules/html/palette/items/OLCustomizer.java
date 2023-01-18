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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.html.palette.items;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;



/**
 *
 * @author  Libor Kotouc, Alexey Butenko
 */
public class OLCustomizer extends javax.swing.JPanel {

    private Dialog dialog = null;
    private DialogDescriptor descriptor = null;
    private boolean dialogOK = false;

    OL ol;
            
    /**
     * Creates new form OLCustomizer
     */
    public OLCustomizer(OL ol) {
        this.ol = ol;
        
        initComponents();

        try {
            ((JSpinner.NumberEditor)jSpinner1.getEditor()).getTextField().getAccessibleContext().setAccessibleName(jSpinner1.getAccessibleContext().getAccessibleName());
            ((JSpinner.NumberEditor)jSpinner1.getEditor()).getTextField().getAccessibleContext().setAccessibleDescription(jSpinner1.getAccessibleContext().getAccessibleDescription());
        }catch (Exception e) {
        }
        if (ol.getType().equals(OL.DEFAULT))
            jRadioButton1.setSelected(true);
        else if (ol.getType().equals(OL.ARABIC_NUMBERS))
            jRadioButton2.setSelected(true);
        else if (ol.getType().equals(OL.LOWER_ALPHA))
            jRadioButton3.setSelected(true);
        else if (ol.getType().equals(OL.UPPER_ALPHA))
            jRadioButton4.setSelected(true);
        else if (ol.getType().equals(OL.LOWER_ROMAN))
            jRadioButton5.setSelected(true);
        else if (ol.getType().equals(OL.UPPER_ROMAN))
            jRadioButton6.setSelected(true);
    }
    
    public boolean showDialog() {
        
        dialogOK = false;
        
        String displayName = "";
        try {
            displayName = NbBundle.getBundle("org.netbeans.modules.html.palette.items.resources.Bundle").getString("NAME_html-OL"); // NOI18N
        }
        catch (Exception e) {}
        
        descriptor = new DialogDescriptor
                (this, NbBundle.getMessage(OLCustomizer.class, "LBL_Customizer_InsertPrefix") + " " + displayName, true,
                 DialogDescriptor.OK_CANCEL_OPTION, DialogDescriptor.OK_OPTION,
                 new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                        if (descriptor.getValue().equals(DialogDescriptor.OK_OPTION)) {
                            evaluateInput();
                            dialogOK = true;
                        }
                        dialog.dispose();
		     }
		 } 
                );
        
        dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_Dialog"));
        dialog.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_Dialog"));
        dialog.setVisible(true);
        
        return dialogOK;
    }
    
    private void evaluateInput() {
        
        int count = ((Integer)jSpinner1.getValue()).intValue();
        ol.setCount(count);

        String itemType = null;

        if (jRadioButton1.isSelected())
            itemType = OL.DEFAULT;
        if (jRadioButton2.isSelected())
            itemType = OL.ARABIC_NUMBERS;
        else if (jRadioButton3.isSelected())
            itemType = OL.LOWER_ALPHA;
        else if (jRadioButton4.isSelected())
            itemType = OL.UPPER_ALPHA;
        else if (jRadioButton5.isSelected())
            itemType = OL.LOWER_ROMAN;
        else if (jRadioButton6.isSelected())
            itemType = OL.UPPER_ROMAN;

        ol.setType(itemType);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jRadioButton6 = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jSpinner1 = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(OLCustomizer.class, "LBL_OL_Style")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 0);
        add(jLabel1, gridBagConstraints);
        jLabel1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_Style")); // NOI18N
        jLabel1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_Style")); // NOI18N

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton1, org.openide.util.NbBundle.getMessage(OLCustomizer.class, "LBL_OL_default")); // NOI18N
        jRadioButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 18);
        add(jRadioButton1, gridBagConstraints);
        jRadioButton1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_default")); // NOI18N
        jRadioButton1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_default")); // NOI18N

        buttonGroup1.add(jRadioButton2);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton2, "1, 2, 3, ...");
        jRadioButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 24);
        add(jRadioButton2, gridBagConstraints);
        jRadioButton2.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_arabic")); // NOI18N
        jRadioButton2.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_arabicnumbers")); // NOI18N

        buttonGroup1.add(jRadioButton3);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton3, "a, b, c, ...");
        jRadioButton3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 24);
        add(jRadioButton3, gridBagConstraints);
        jRadioButton3.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_loweralpha")); // NOI18N
        jRadioButton3.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_loweralpha")); // NOI18N

        buttonGroup1.add(jRadioButton4);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton4, "A, B, C, ...");
        jRadioButton4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 24);
        add(jRadioButton4, gridBagConstraints);
        jRadioButton4.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_upperalpha")); // NOI18N
        jRadioButton4.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_upperalpha")); // NOI18N

        buttonGroup1.add(jRadioButton5);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton5, "i, ii, iii, ...");
        jRadioButton5.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 24);
        add(jRadioButton5, gridBagConstraints);
        jRadioButton5.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_lowerroman")); // NOI18N
        jRadioButton5.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_lowerroman")); // NOI18N

        buttonGroup1.add(jRadioButton6);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButton6, "I, II, III, ...");
        jRadioButton6.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 12, 24);
        add(jRadioButton6, gridBagConstraints);
        jRadioButton6.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_upperroman")); // NOI18N
        jRadioButton6.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_upperroman")); // NOI18N

        jLabel2.setLabelFor(jSpinner1);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(OLCustomizer.class, "LBL_OL_Items")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 0);
        add(jLabel2, gridBagConstraints);
        jLabel2.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_Items")); // NOI18N
        jLabel2.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_Items")); // NOI18N

        jSpinner1.setModel(new SpinnerNumberModel(ol.getCount(), 0, Integer.MAX_VALUE, 1));
        jSpinner1.setEditor(new NumberEditor(jSpinner1, "#"));
        jSpinner1.setValue(new Integer(ol.getCount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 18);
        add(jSpinner1, gridBagConstraints);
        jSpinner1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSN_OL_Items_Spinner")); // NOI18N
        jSpinner1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(OLCustomizer.class, "ACSD_OL_Items_Spinner")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JRadioButton jRadioButton6;
    private javax.swing.JSpinner jSpinner1;
    // End of variables declaration//GEN-END:variables

    private class NumberEditor extends JSpinner.NumberEditor {

        public NumberEditor(JSpinner jSpinner, String decimalFormat) {
            super(jSpinner, decimalFormat);
        }

        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            try {
                return super.processKeyBinding(ks, e, condition, pressed);
            } finally {
                //Fix for #166154: passes Enter kb action to dialog
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (descriptor!=null) {
                        ActionListener al = descriptor.getButtonListener();
                        if (al!=null) {
                            al.actionPerformed(new ActionEvent(this,
                                                                ActionEvent.ACTION_PERFORMED,
                                                                "OK",           //NOI18N
                                                                e.getWhen(),
                                                                e.getModifiers()));
                        }
                    }
                }
            }
        }
    }
}
