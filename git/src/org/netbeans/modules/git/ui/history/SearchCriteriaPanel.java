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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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

package org.netbeans.modules.git.ui.history;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.SwingUtilities;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.git.ui.history.SearchExecutor.Mode;

/**
 * Packages search criteria in Search History panel.
 *
 * @author Maros Sandor
 */
class SearchCriteriaPanel extends javax.swing.JPanel {
    
    private static final DateFormat [] dateFormats = new DateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"),  // NOI18N
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),  // NOI18N
        new SimpleDateFormat("yyyy-MM-dd HH:mm"),  // NOI18N
        new SimpleDateFormat("yyyy-MM-dd") // NOI18N
    };
    private Mode mode = Mode.LOCAL;
    
    /** Creates new form SearchCriteriaPanel */
    public SearchCriteriaPanel() {
        initComponents();
        showMergesChkBox.setSelected(GitModuleConfig.getDefault().getShowHistoryMerges());
        tfLimit.setText(Integer.toString(SearchExecutor.DEFAULT_LIMIT));
    }

    public Date getFrom() {
        if (mode != Mode.LOCAL) {
            return null;
        }
        String s = tfFrom.getText().trim();
        if (s.isEmpty()) {
            return null;
        }
        return parseDate(s);
    }

    public Date getTo() {
        if (mode != Mode.LOCAL) {
            return null;
        }
        String s = tfTo.getText().trim();
        if(s.isEmpty()) {
            return null;
        }
        return parseDate(s);
    }

    String getBranch () {
        String s = tfBranch.getText().trim();
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    public void setBranch (String branch) {
        tfBranch.setText(branch);
    }

    /**
     *
     * @return limit for shown changesets, -1 for no limit
     */
    public int getLimit() {
        String s = tfLimit.getText().trim();
        Integer retval = -1;
        try {
            retval = Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            retval = -1;
        }
        if (retval <= 0) {
            retval = -1;
        }
        return retval;
    }

    public void setLimit (int limit) {
        if (limit > 0) {
            tfLimit.setText(Integer.toString(limit));
        } else {
            tfLimit.setText("");
        }
    }

    public String getCommitMessage() {
        String s = tfCommitMessage.getText().trim();
        return s.isEmpty() ? null : s;
    }

    public String getUsername() {
        String s = tfUsername.getText().trim();
        return s.isEmpty() ? null : s;
    }

    boolean isIncludeMerges() {
        return showMergesChkBox.isSelected();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tfCommitMessage.requestFocusInWindow();
            }
        });
    }
    
    void setupRemoteSearch (Mode mode) {
        this.mode = mode;
        branchLabel.setEnabled(false);
        tfBranch.setEnabled(false);
        btnSelectBranch.setEnabled(false);
        fromLabel.setVisible(false);
        tfFrom.setVisible(false);
        fromInfoLabel.setVisible(false);
        toLabel.setVisible(false);
        tfTo.setVisible(false);
        toInfoLabel.setVisible(false);
    }
    
    String getToRevision () {
        String branch = getBranch();
        switch (mode) {
            case REMOTE_IN:
                return null;
            case REMOTE_OUT:
                return branch;
            default:
                String toRevision;
                if (branch == null) {
                    toRevision = getTo() == null ? tfTo.getText().trim() : null;
                } else {
                    toRevision = branch;
                    if (getTo() == null && !tfTo.getText().trim().isEmpty()) {
                        throw new IllegalArgumentException(Bundle.MSG_IllegalSearchArgument_bothBranchAndTo());
                    }
                }
                return toRevision != null && toRevision.isEmpty() ? null : toRevision;
        }
    }
    
    String getFromRevision () {
        String branch = getBranch();
        switch (mode) {
            case REMOTE_IN:
                return branch;
            case REMOTE_OUT:
                return null;
            default:
                String fromRevision = getFrom() == null ? tfFrom.getText().trim() : null;
                return fromRevision != null && fromRevision.isEmpty() ? null : fromRevision;
        }
    }
    
    Mode getMode () {
        return mode;
    }

    private static Date parseDate (String strDate) {
        Date date = null;
        if (strDate != null) {
            for (DateFormat fd : dateFormats) {
                try {
                    date = fd.parse(strDate);
                } catch (ParseException ex) { }
            }
        }
        return date;
    }
    
    boolean validateUserInput () {
        return true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        commitMessageLabel = new javax.swing.JLabel();
        tfCommitMessage = new javax.swing.JTextField();
        usernameLabel = new javax.swing.JLabel();
        tfUsername = new javax.swing.JTextField();
        fromLabel = new javax.swing.JLabel();
        fromInfoLabel = new javax.swing.JLabel();
        toLabel = new javax.swing.JLabel();
        toInfoLabel = new javax.swing.JLabel();
        limitLabel = new javax.swing.JLabel();
        showMergesChkBox = new javax.swing.JCheckBox();
        branchLabel = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 0, 11));

        commitMessageLabel.setLabelFor(tfCommitMessage);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/git/ui/history/Bundle"); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(commitMessageLabel, bundle.getString("CTL_UseCommitMessage")); // NOI18N
        commitMessageLabel.setToolTipText(bundle.getString("TT_CommitMessage")); // NOI18N

        tfCommitMessage.setColumns(20);

        usernameLabel.setLabelFor(tfUsername);
        org.openide.awt.Mnemonics.setLocalizedText(usernameLabel, bundle.getString("CTL_UseUsername")); // NOI18N
        usernameLabel.setToolTipText(bundle.getString("TT_Username")); // NOI18N

        tfUsername.setColumns(20);

        fromLabel.setLabelFor(tfFrom);
        org.openide.awt.Mnemonics.setLocalizedText(fromLabel, bundle.getString("CTL_UseFrom")); // NOI18N
        fromLabel.setToolTipText(bundle.getString("TT_From")); // NOI18N

        tfFrom.setColumns(20);

        org.openide.awt.Mnemonics.setLocalizedText(fromInfoLabel, bundle.getString("CTL_FromHint")); // NOI18N

        toLabel.setLabelFor(tfTo);
        org.openide.awt.Mnemonics.setLocalizedText(toLabel, bundle.getString("CTL_UseTo")); // NOI18N
        toLabel.setToolTipText(bundle.getString("TT_To")); // NOI18N

        tfTo.setColumns(20);

        org.openide.awt.Mnemonics.setLocalizedText(toInfoLabel, bundle.getString("CTL_ToHint")); // NOI18N

        limitLabel.setLabelFor(tfLimit);
        org.openide.awt.Mnemonics.setLocalizedText(limitLabel, bundle.getString("CTL_UseLimit")); // NOI18N
        limitLabel.setToolTipText(bundle.getString("TT_Limit")); // NOI18N

        tfLimit.setColumns(10);

        showMergesChkBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(showMergesChkBox, org.openide.util.NbBundle.getMessage(SearchCriteriaPanel.class, "CTL_ShowMerge")); // NOI18N
        showMergesChkBox.setToolTipText(org.openide.util.NbBundle.getMessage(SearchCriteriaPanel.class, "TT_ShowMerges")); // NOI18N
        showMergesChkBox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        tfBranch.setColumns(20);

        branchLabel.setLabelFor(tfBranch);
        org.openide.awt.Mnemonics.setLocalizedText(branchLabel, bundle.getString("CTL_UseBranch")); // NOI18N
        branchLabel.setToolTipText(bundle.getString("TT_Branch")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnSelectBranch, org.openide.util.NbBundle.getMessage(SearchCriteriaPanel.class, "SearchCriteriaPanel.btnSelectBranch.text")); // NOI18N
        btnSelectBranch.setToolTipText(org.openide.util.NbBundle.getMessage(SearchCriteriaPanel.class, "SearchCriteriaPanel.btnSelectBranch.TTtext")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(limitLabel)
                            .addComponent(commitMessageLabel)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(usernameLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfUsername, javax.swing.GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tfLimit, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(showMergesChkBox))
                    .addComponent(tfCommitMessage))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(branchLabel)
                    .addComponent(toLabel)
                    .addComponent(fromLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfFrom, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
                    .addComponent(tfTo)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(toInfoLabel)
                            .addComponent(fromInfoLabel))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tfBranch, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSelectBranch)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(commitMessageLabel)
                    .addComponent(tfCommitMessage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(branchLabel)
                    .addComponent(tfBranch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSelectBranch))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tfFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fromInfoLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(fromLabel)
                                .addGap(30, 30, 30)
                                .addComponent(toLabel)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toInfoLabel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tfUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(usernameLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(limitLabel)
                            .addComponent(tfLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(showMergesChkBox))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel branchLabel;
    final javax.swing.JButton btnSelectBranch = new javax.swing.JButton();
    private javax.swing.JLabel commitMessageLabel;
    private javax.swing.JLabel fromInfoLabel;
    private javax.swing.JLabel fromLabel;
    private javax.swing.JLabel limitLabel;
    private javax.swing.JCheckBox showMergesChkBox;
    final javax.swing.JTextField tfBranch = new javax.swing.JTextField();
    private javax.swing.JTextField tfCommitMessage;
    final javax.swing.JTextField tfFrom = new javax.swing.JTextField();
    final javax.swing.JTextField tfLimit = new javax.swing.JTextField();
    final javax.swing.JTextField tfTo = new javax.swing.JTextField();
    private javax.swing.JTextField tfUsername;
    private javax.swing.JLabel toInfoLabel;
    private javax.swing.JLabel toLabel;
    private javax.swing.JLabel usernameLabel;
    // End of variables declaration//GEN-END:variables
    
}
