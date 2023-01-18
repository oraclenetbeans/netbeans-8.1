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

package org.netbeans.modules.php.project.ui.actions.tests;

import java.awt.Component;
import java.awt.EventQueue;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.UIResource;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotificationLineSupport;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

public final class SelectFilePanel extends JPanel {
    private static final long serialVersionUID = 8464321687132132L;

    // @GuardedBy("EDT")
    final List<FileObject> sourceRoots;
    private final DefaultListModel<FileObject> model;

    private DialogDescriptor dialogDescriptor;
    private NotificationLineSupport notificationLineSupport;

    private SelectFilePanel(List<FileObject> sourceRoots, List<FileObject> files) {
        assert EventQueue.isDispatchThread();
        assert sourceRoots != null;
        assert !sourceRoots.isEmpty();
        assert files.size() > 1;

        this.sourceRoots = sourceRoots;

        initComponents();

        model = new DefaultListModel<>();
        for (FileObject fo : files) {
            assert isParentOf(sourceRoots, fo) : fo + " not underneath " + sourceRoots;
            model.addElement(fo);
        }

        selectFileList.setCellRenderer(new FileListCellRenderer());
        selectFileList.setModel(model);
        selectFileList.setSelectedIndex(0);
        selectFileList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                validateSelection();
            }
        });
    }

    public static FileObject open(List<FileObject> sourceRoots, List<FileObject> files) {
        final SelectFilePanel panel = new SelectFilePanel(sourceRoots, files);
        panel.dialogDescriptor = new DialogDescriptor(
                panel,
                NbBundle.getMessage(SelectFilePanel.class, "LBL_SelectFile"),
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);
        panel.notificationLineSupport = panel.dialogDescriptor.createNotificationLineSupport();
        panel.validateSelection();
        if (DialogDisplayer.getDefault().notify(panel.dialogDescriptor) == DialogDescriptor.OK_OPTION) {
            return panel.getSelectedFile();
        }
        return null;
    }

    private boolean isParentOf(List<FileObject> folders, FileObject file) {
        for (FileObject folder : folders) {
            if (FileUtil.isParentOf(folder, file)) {
                return true;
            }
        }
        return false;
    }

    private FileObject getSelectedFile() {
        return selectFileList.getSelectedValue();
    }

    void validateSelection() {
        assert notificationLineSupport != null;

        if (getSelectedFile() == null) {
            notificationLineSupport.setErrorMessage(NbBundle.getMessage(SelectFilePanel.class, "MSG_NoFileSelected"));
            dialogDescriptor.setValid(false);
            return;
        }
        notificationLineSupport.clearMessages();
        dialogDescriptor.setValid(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectFileLabel = new JLabel();
        selectFileScrollPane = new JScrollPane();
        selectFileList = new JList<FileObject>();

        selectFileLabel.setLabelFor(selectFileList);
        Mnemonics.setLocalizedText(selectFileLabel, NbBundle.getMessage(SelectFilePanel.class, "SelectFilePanel.selectFileLabel.text")); // NOI18N

        selectFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectFileScrollPane.setViewportView(selectFileList);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(selectFileScrollPane, GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                    .addComponent(selectFileLabel))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(selectFileLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(selectFileScrollPane, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel selectFileLabel;
    private JList<FileObject> selectFileList;
    private JScrollPane selectFileScrollPane;
    // End of variables declaration//GEN-END:variables

    private final class FileListCellRenderer extends JLabel implements ListCellRenderer<FileObject>, UIResource {

        private static final long serialVersionUID = 987974324657654541L;


        public FileListCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FileObject> list, FileObject value, int index, boolean isSelected, boolean cellHasFocus) {
            // #93658: GTK needs name to render cell renderer "natively"
            setName("ComboBox.listRenderer"); // NOI18N

            assert EventQueue.isDispatchThread();
            String relativePath = getRelativePath(sourceRoots, value);
            assert relativePath != null : value + " not underneath any of: " + sourceRoots;
            setText(relativePath);
            setToolTipText(relativePath);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }

        // #93658: GTK needs name to render cell renderer "natively"
        @Override
        public String getName() {
            String name = super.getName();
            return name == null ? "ComboBox.renderer" : name;  // NOI18N
        }

        private String getRelativePath(List<FileObject> folders, FileObject file) {
            for (FileObject folder : folders) {
                String relativePath = FileUtil.getRelativePath(folder, file);
                if (relativePath != null) {
                    return folder.getNameExt() + "/" + relativePath; // NOI18N
                }
            }
            return null;
        }

    }

}
