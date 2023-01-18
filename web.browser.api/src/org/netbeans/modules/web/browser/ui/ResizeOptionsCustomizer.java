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
package org.netbeans.modules.web.browser.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.modules.web.browser.api.ResizeOption;
import org.netbeans.modules.web.browser.api.ResizeOptions;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 * Shows an editable table listing all defined screen sizes.
 * 
 * @author S. Aubrecht
 */
class ResizeOptionsCustomizer extends javax.swing.JPanel {

    private final TableModel model;
    private final ArrayList<ResizeOption> options;

    /**
     * Creates new form ResizeOptionsCustomizer
     */
    public ResizeOptionsCustomizer() {
        options = new ArrayList<ResizeOption>( ResizeOptions.getDefault().loadAll() );
        initComponents();
        model = new TableModel();
        tblOptions.setModel( model );
        tblOptions.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged( ListSelectionEvent e ) {
                enableButtons();
            }
        });
        tblOptions.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        enableButtons();
//        tblOptions.setDefaultEditor( ResizeOption.Type.class, new combo);
        DefaultTableCellRenderer colorRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue( Object value ) {
                if( value instanceof ResizeOption ) {
                    Icon icon = BrowserResizeButton.toIcon( (ResizeOption)value );
                    setIcon( icon );
                    setHorizontalAlignment( JLabel.CENTER );
                }
            }
        };
        tblOptions.setDefaultRenderer( ResizeOption.class, colorRenderer );

        JComboBox combo = new JComboBox();
        combo.addItem( ResizeOption.Type.DESKTOP );
        combo.addItem( ResizeOption.Type.TABLET_LANDSCAPE );
        combo.addItem( ResizeOption.Type.SMARTPHONE_PORTRAIT );
        combo.addItem( ResizeOption.Type.SMARTPHONE_LANDSCAPE );
        combo.addItem( ResizeOption.Type.SMARTPHONE_PORTRAIT );
        combo.addItem( ResizeOption.Type.WIDESCREEN );
        combo.addItem( ResizeOption.Type.NETBOOK );
        combo.setRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {
                Component res = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
                setText( null );
                setIcon( BrowserResizeButton.toIcon( (ResizeOption.Type)value ) );
                setHorizontalAlignment( JLabel.CENTER );
                return res;
            }
        });

        tblOptions.setDefaultEditor( ResizeOption.class, new DefaultCellEditor( combo ) );

        tblOptions.setRowHeight( combo.getPreferredSize().height );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lblTable = new javax.swing.JLabel();
        scrollTable = new javax.swing.JScrollPane();
        tblOptions = new javax.swing.JTable();
        btnNew = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        btnMoveUp = new javax.swing.JButton();
        btnMoveDown = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new java.awt.GridBagLayout());

        lblTable.setText(NbBundle.getMessage(ResizeOptionsCustomizer.class, "ResizeOptionsCustomizer.lblTable.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(lblTable, gridBagConstraints);

        scrollTable.setViewportView(tblOptions);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(scrollTable, gridBagConstraints);

        btnNew.setText(NbBundle.getMessage(ResizeOptionsCustomizer.class, "ResizeOptionsCustomizer.btnNew.text")); // NOI18N
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 0);
        add(btnNew, gridBagConstraints);

        btnRemove.setText(NbBundle.getMessage(ResizeOptionsCustomizer.class, "ResizeOptionsCustomizer.btnRemove.text")); // NOI18N
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 15, 0, 0);
        add(btnRemove, gridBagConstraints);

        btnMoveUp.setText(NbBundle.getMessage(ResizeOptionsCustomizer.class, "ResizeOptionsCustomizer.btnMoveUp.text")); // NOI18N
        btnMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMoveUpActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 0);
        add(btnMoveUp, gridBagConstraints);

        btnMoveDown.setText(NbBundle.getMessage(ResizeOptionsCustomizer.class, "ResizeOptionsCustomizer.btnMoveDown.text")); // NOI18N
        btnMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMoveDownActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 15, 0, 0);
        add(btnMoveDown, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void btnNewActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_btnNewActionPerformed
        ResizeOption newOption = ResizeOption.create( ResizeOption.Type.CUSTOM, NbBundle.getMessage(ResizeOptionsCustomizer.class, "Lbl_DefaultResizeDisplayName"), 1024, 768, true, false );
        options.add( newOption );
        model.fireTableRowsInserted( options.size()-1, options.size()-1 );
        tblOptions.getSelectionModel().setSelectionInterval( options.size()-1, options.size()-1 );
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnRemoveActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_btnRemoveActionPerformed
        int selIndex = tblOptions.getSelectedRow();
        if( selIndex < 0 )
            return;
        ResizeOption ro = options.get( selIndex );
        if( ro.isDefault() )
            return;
        options.remove( selIndex );
        model.fireTableRowsDeleted( selIndex, selIndex );
        selIndex = Math.min( selIndex, options.size()-1 );
        tblOptions.getSelectionModel().setSelectionInterval( selIndex, selIndex );
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void btnMoveUpActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_btnMoveUpActionPerformed
        switchRows( -1 );
    }//GEN-LAST:event_btnMoveUpActionPerformed

    private void btnMoveDownActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_btnMoveDownActionPerformed
        switchRows( 1 );

    }//GEN-LAST:event_btnMoveDownActionPerformed

    private void switchRows( int direction ) {
        int selIndex = tblOptions.getSelectedRow();
        if( selIndex < 0 )
            return;
        ResizeOption current = options.get( selIndex );
        ResizeOption next = options.get( selIndex+direction );
        options.set( selIndex+direction, current );
        options.set( selIndex, next );
        int index1 = direction < 0 ? selIndex+direction : selIndex;
        int index2 = direction < 0 ? selIndex : selIndex+direction;
        model.fireTableRowsUpdated( index1, index2 );
        tblOptions.getSelectionModel().setSelectionInterval( selIndex+direction, selIndex+direction );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnMoveDown;
    private javax.swing.JButton btnMoveUp;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnRemove;
    private javax.swing.JLabel lblTable;
    private javax.swing.JScrollPane scrollTable;
    private javax.swing.JTable tblOptions;
    // End of variables declaration//GEN-END:variables

    public boolean showCustomizer() {
        DialogDescriptor descriptor = new DialogDescriptor( this, NbBundle.getMessage(ResizeOptionsCustomizer.class, "Title_CUSTOMIZE_WINDOW_SETTINGS"), true, DialogDescriptor.DEFAULT_OPTION, null, null );
        descriptor.setHelpCtx( new HelpCtx("org.netbeans.modules.web.browser.ui.ResizeOptionsCustomizer") );
        Dialog dlg = DialogDisplayer.getDefault().createDialog( descriptor );
        dlg.setVisible( true );
        return descriptor.getValue() == DialogDescriptor.OK_OPTION;
    }

    List<ResizeOption> getResizeOptions() {
        return new ArrayList<ResizeOption>( options );
    }

    private void enableButtons() {
        int selIndex = tblOptions.getSelectedRow();
        ResizeOption ro = selIndex < 0 ? null : options.get( selIndex );
        btnRemove.setEnabled( null != ro && !ro.isDefault() );
        btnMoveUp.setEnabled( selIndex > 0 );;
        btnMoveDown.setEnabled( selIndex >= 0 && selIndex < tblOptions.getRowCount()-1 );
    }

    private class TableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return options.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public Object getValueAt( int rowIndex, int columnIndex ) {
            ResizeOption ro = options.get( rowIndex );
            switch( columnIndex ) {
                case 0:
                    return ro;
                case 1:
                    return ro.getDisplayName();
                case 2:
                    return ro.getWidth();
                case 3:
                    return ro.getHeight();
                case 4:
                    return ro.isShowInToolbar();
            }
            return null;
        }

        @Override
        public Class<?> getColumnClass( int columnIndex ) {
            if( 0 == columnIndex )
                return ResizeOption.class;
            if( 4 == columnIndex )
                return Boolean.class;
            return super.getColumnClass( columnIndex );
        }

        @Override
        public String getColumnName( int column ) {
            switch( column ) {
                case 0: return NbBundle.getMessage(ResizeOptionsCustomizer.class, "Col_TYPE");
                case 1: return NbBundle.getMessage(ResizeOptionsCustomizer.class, "Col_NAME");
                case 2: return NbBundle.getMessage(ResizeOptionsCustomizer.class, "Col_WIDTH");
                case 3: return NbBundle.getMessage(ResizeOptionsCustomizer.class, "Col_HEIGHT");
                case 4: return NbBundle.getMessage(ResizeOptionsCustomizer.class, "Col_TOOLBAR");
            }
            return super.getColumnName( column );
        }

        @Override
        public boolean isCellEditable( int rowIndex, int columnIndex ) {
            return true;
        }

        @Override
        public void setValueAt( Object aValue, int rowIndex, int columnIndex ) {
            if( null == aValue )
                return;

            ResizeOption current = options.get( rowIndex );
            ResizeOption.Type type = current.getType();
            String name = current.getDisplayName();
            int width = current.getWidth();
            int height = current.getHeight();
            boolean toolbar = current.isShowInToolbar();
            boolean isDefault = current.isDefault();
            boolean doSetValue = true;
            switch( columnIndex ) {
                case 0:
                    if( aValue instanceof ResizeOption.Type ) {
                        type = (ResizeOption.Type)aValue;
                    } break;
                case 1: {
                    name = aValue.toString().trim();
                    if( name.isEmpty() )
                        doSetValue = false;
                } break;
                case 2: {
                    int num = -1;
                    try {
                        num = Integer.parseInt( aValue.toString() );
                    } catch( NumberFormatException e ) {
                        //ignore
                    }
                    if( num > 0 ) {
                        width = num;
                    } else {
                        doSetValue = false;
                    }
                } break;
                case 3: {
                    int num = -1;
                    try {
                        num = Integer.parseInt( aValue.toString() );
                    } catch( NumberFormatException e ) {
                        //ignore
                    }
                    if( num > 0 ) {
                        height = num;
                    } else {
                        doSetValue = false;
                    }
                } break;
                case 4: {
                    if( aValue instanceof Boolean ) {
                        toolbar = ((Boolean)aValue).booleanValue();
                    }
                } break;
            }

            if( doSetValue ) {
                ResizeOption newOption = ResizeOption.create( type, name, width, height, toolbar, isDefault );
                options.set( rowIndex, newOption );
                fireTableRowsUpdated( rowIndex, rowIndex );
            }
        }

    }
}
