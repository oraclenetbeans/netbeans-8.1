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

package org.netbeans.modules.php.project.connections.ftp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.UIResource;
import org.netbeans.modules.php.api.validation.ValidationResult;
import org.netbeans.modules.php.project.connections.ConfigManager.Configuration;
import org.netbeans.modules.php.project.connections.common.RemoteUtils;
import org.netbeans.modules.php.project.connections.ftp.FtpConfiguration.Encryption;
import org.netbeans.modules.php.project.connections.spi.RemoteConfigurationPanel;
import org.openide.awt.Mnemonics;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

/**
 * @author Tomas Mysik
 */
public final class FtpConfigurationPanel extends JPanel implements RemoteConfigurationPanel {
    private static final long serialVersionUID = 62342689756412730L;

    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private String error = null;
    private String warning = null;
    private boolean passwordRead = false;


    public FtpConfigurationPanel() {
        initComponents();
        init();

        // listeners
        registerListeners();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public boolean isValidConfiguration() {
        // cleanup
        setError(null);
        setWarning(null);

        // validate
        ValidationResult validationResult = new FtpConfigurationValidator()
                .validate(getHostName(), getPort(), isAnonymousLogin(), getUserName(), getInitialDirectory(), getTimeout(), getKeepAliveInterval(), isPassiveMode())
                .getResult();
        if (validationResult.hasErrors()) {
            setError(validationResult.getErrors().get(0).getMessage());
            return false;
        }
        if (validationResult.hasWarnings()) {
            setWarning(validationResult.getWarnings().get(0).getMessage());
        }
        return true;
    }

    @Override
    public String getError() {
        return error;
    }

    protected void setError(String error) {
        this.error = error;
    }

    @Override
    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    private void init() {
        populateEncryptionComboBox();
        setEnabledOnlyLoginSecured();
        setEnabledLoginCredentials();
    }

    void setEnabledLoginCredentials() {
        setEnabledLoginCredentials(!anonymousCheckBox.isSelected());
    }

    private void setEnabledLoginCredentials(boolean enabled) {
        userTextField.setEnabled(enabled);
        passwordTextField.setEnabled(enabled);
    }

    void setEnabledOnlyLoginSecured() {
        onlyLoginSecuredCheckBox.setEnabled(getEncryptionInternal() != Encryption.NONE);
    }

    private void populateEncryptionComboBox() {
        for (Encryption encryption : Encryption.values()) {
            encryptionComboBox.addItem(encryption);
        }
        encryptionComboBox.setRenderer(new EncryptionRenderer());
    }

    private void registerListeners() {
        DocumentListener documentListener = new DefaultDocumentListener();
        ActionListener actionListener = new DefaultActionListener();
        hostTextField.getDocument().addDocumentListener(documentListener);
        portTextField.getDocument().addDocumentListener(documentListener);
        encryptionComboBox.addActionListener(actionListener);
        onlyLoginSecuredCheckBox.addActionListener(actionListener); // ItemListener would be better
        userTextField.getDocument().addDocumentListener(documentListener);
        passwordTextField.getDocument().addDocumentListener(documentListener);
        anonymousCheckBox.addActionListener(actionListener);
        initialDirectoryTextField.getDocument().addDocumentListener(documentListener);
        timeoutTextField.getDocument().addDocumentListener(documentListener);
        keepAliveTextField.getDocument().addDocumentListener(documentListener);
        passiveModeCheckBox.addActionListener(actionListener);
        ignoreDisconnectErrorsCheckBox.addActionListener(actionListener);

        // internals
        anonymousCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabledLoginCredentials();
            }
        });
        encryptionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabledOnlyLoginSecured();
            }
        });
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    private Encryption getEncryptionInternal() {
        return (Encryption) encryptionComboBox.getSelectedItem();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        hostLabel = new JLabel();
        hostTextField = new JTextField();
        portLabel = new JLabel();
        portTextField = new JTextField();
        encryptionLabel = new JLabel();
        encryptionComboBox = new JComboBox<Encryption>();
        onlyLoginSecuredCheckBox = new JCheckBox();
        userLabel = new JLabel();
        userTextField = new JTextField();
        anonymousCheckBox = new JCheckBox();
        passwordLabel = new JLabel();
        passwordTextField = new JPasswordField();
        initialDirectoryLabel = new JLabel();
        initialDirectoryTextField = new JTextField();
        timeoutLabel = new JLabel();
        timeoutTextField = new JTextField();
        keepAliveLabel = new JLabel();
        keepAliveTextField = new JTextField();
        keepAliveInfoLabel = new JLabel();
        passiveModeCheckBox = new JCheckBox();
        passwordLabelInfo = new JLabel();
        ignoreDisconnectErrorsCheckBox = new JCheckBox();

        hostLabel.setLabelFor(hostTextField);
        Mnemonics.setLocalizedText(hostLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.hostLabel.text_1")); // NOI18N

        hostTextField.setMinimumSize(new Dimension(150, 19));

        portLabel.setLabelFor(portTextField);
        Mnemonics.setLocalizedText(portLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.portLabel.text_1")); // NOI18N

        portTextField.setMinimumSize(new Dimension(20, 19));

        encryptionLabel.setLabelFor(encryptionComboBox);
        Mnemonics.setLocalizedText(encryptionLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.encryptionLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(onlyLoginSecuredCheckBox, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.onlyLoginSecuredCheckBox.text")); // NOI18N

        userLabel.setLabelFor(userTextField);
        Mnemonics.setLocalizedText(userLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.userLabel.text_1")); // NOI18N

        Mnemonics.setLocalizedText(anonymousCheckBox, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.anonymousCheckBox.text_1")); // NOI18N

        passwordLabel.setLabelFor(passwordTextField);
        Mnemonics.setLocalizedText(passwordLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordLabel.text_1")); // NOI18N

        initialDirectoryLabel.setLabelFor(initialDirectoryTextField);
        Mnemonics.setLocalizedText(initialDirectoryLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.initialDirectoryLabel.text_1")); // NOI18N

        timeoutLabel.setLabelFor(timeoutTextField);
        Mnemonics.setLocalizedText(timeoutLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.timeoutLabel.text_1")); // NOI18N

        timeoutTextField.setMinimumSize(new Dimension(20, 19));

        keepAliveLabel.setLabelFor(keepAliveTextField);
        Mnemonics.setLocalizedText(keepAliveLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.keepAliveLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(keepAliveInfoLabel, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.keepAliveInfoLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(passiveModeCheckBox, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passiveModeCheckBox.text_1")); // NOI18N

        passwordLabelInfo.setLabelFor(this);
        Mnemonics.setLocalizedText(passwordLabelInfo, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordLabelInfo.text_1")); // NOI18N

        Mnemonics.setLocalizedText(ignoreDisconnectErrorsCheckBox, NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.ignoreDisconnectErrorsCheckBox.text")); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(passiveModeCheckBox)
                    .addComponent(ignoreDisconnectErrorsCheckBox))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(hostLabel)
                    .addComponent(userLabel)
                    .addComponent(passwordLabel)
                    .addComponent(initialDirectoryLabel)
                    .addComponent(timeoutLabel)
                    .addComponent(keepAliveLabel)
                    .addComponent(encryptionLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(onlyLoginSecuredCheckBox)
                        .addContainerGap(21, Short.MAX_VALUE))
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(userTextField)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(anonymousCheckBox))
                    .addComponent(passwordTextField)
                    .addComponent(initialDirectoryTextField)
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(hostTextField, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(portLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(portTextField, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(passwordLabelInfo)
                            .addComponent(encryptionComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(keepAliveTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)
                                    .addComponent(timeoutTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(keepAliveInfoLabel)))
                        .addContainerGap())))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {keepAliveTextField, portTextField, timeoutTextField});

        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(hostLabel)
                    .addComponent(portLabel)
                    .addComponent(portTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(hostTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(encryptionLabel)
                    .addComponent(encryptionComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(onlyLoginSecuredCheckBox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(userLabel)
                    .addComponent(anonymousCheckBox)
                    .addComponent(userTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(passwordLabel)
                    .addComponent(passwordTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(passwordLabelInfo)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(initialDirectoryLabel)
                    .addComponent(initialDirectoryTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(timeoutTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(timeoutLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(keepAliveLabel)
                    .addComponent(keepAliveTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(keepAliveInfoLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(passiveModeCheckBox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ignoreDisconnectErrorsCheckBox))
        );

        hostLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.hostLabel.AccessibleContext.accessibleName")); // NOI18N
        hostLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.hostLabel.AccessibleContext.accessibleDescription")); // NOI18N
        hostTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.hostTextField.AccessibleContext.accessibleName")); // NOI18N
        hostTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.hostTextField.AccessibleContext.accessibleDescription")); // NOI18N
        portLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.portLabel.AccessibleContext.accessibleName")); // NOI18N
        portLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.portLabel.AccessibleContext.accessibleDescription")); // NOI18N
        portTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.portTextField.AccessibleContext.accessibleName")); // NOI18N
        portTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.portTextField.AccessibleContext.accessibleDescription")); // NOI18N
        encryptionLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.encryptionLabel.AccessibleContext.accessibleName")); // NOI18N
        encryptionLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.encryptionLabel.AccessibleContext.accessibleDescription")); // NOI18N
        encryptionComboBox.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.encryptionComboBox.AccessibleContext.accessibleName")); // NOI18N
        encryptionComboBox.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.encryptionComboBox.AccessibleContext.accessibleDescription")); // NOI18N
        onlyLoginSecuredCheckBox.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.dataChannelSecuredCheckBox.AccessibleContext.accessibleName")); // NOI18N
        onlyLoginSecuredCheckBox.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.dataChannelSecuredCheckBox.AccessibleContext.accessibleDescription")); // NOI18N
        userLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.userLabel.AccessibleContext.accessibleName")); // NOI18N
        userLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.userLabel.AccessibleContext.accessibleDescription")); // NOI18N
        userTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.userTextField.AccessibleContext.accessibleName")); // NOI18N
        userTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.userTextField.AccessibleContext.accessibleDescription")); // NOI18N
        anonymousCheckBox.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.anonymousCheckBox.AccessibleContext.accessibleName")); // NOI18N
        anonymousCheckBox.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.anonymousCheckBox.AccessibleContext.accessibleDescription")); // NOI18N
        passwordLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordLabel.AccessibleContext.accessibleName")); // NOI18N
        passwordLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordLabel.AccessibleContext.accessibleDescription")); // NOI18N
        passwordTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordTextField.AccessibleContext.accessibleName")); // NOI18N
        passwordTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordTextField.AccessibleContext.accessibleDescription")); // NOI18N
        initialDirectoryLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.initialDirectoryLabel.AccessibleContext.accessibleName")); // NOI18N
        initialDirectoryLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.initialDirectoryLabel.AccessibleContext.accessibleDescription")); // NOI18N
        initialDirectoryTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.initialDirectoryTextField.AccessibleContext.accessibleName")); // NOI18N
        initialDirectoryTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.initialDirectoryTextField.AccessibleContext.accessibleDescription")); // NOI18N
        timeoutLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.timeoutLabel.AccessibleContext.accessibleName")); // NOI18N
        timeoutLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.timeoutLabel.AccessibleContext.accessibleDescription")); // NOI18N
        timeoutTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.timeoutTextField.AccessibleContext.accessibleName")); // NOI18N
        timeoutTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.timeoutTextField.AccessibleContext.accessibleDescription")); // NOI18N
        keepAliveLabel.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.keepAliveLabel.AccessibleContext.accessibleName")); // NOI18N
        keepAliveLabel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.keepAliveLabel.AccessibleContext.accessibleDescription")); // NOI18N
        keepAliveTextField.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.keepAliveTextField.AccessibleContext.accessibleName")); // NOI18N
        keepAliveTextField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.keepAliveTextField.AccessibleContext.accessibleDescription")); // NOI18N
        passiveModeCheckBox.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passiveModeCheckBox.AccessibleContext.accessibleName")); // NOI18N
        passiveModeCheckBox.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passiveModeCheckBox.AccessibleContext.accessibleDescription")); // NOI18N
        passwordLabelInfo.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordLabelInfo.AccessibleContext.accessibleName")); // NOI18N
        passwordLabelInfo.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.passwordLabelInfo.AccessibleContext.accessibleDescription")); // NOI18N
        ignoreDisconnectErrorsCheckBox.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.ignoreDisconnectErrorsCheckBox.AccessibleContext.accessibleName")); // NOI18N
        ignoreDisconnectErrorsCheckBox.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.ignoreDisconnectErrorsCheckBox.AccessibleContext.accessibleDescription")); // NOI18N

        getAccessibleContext().setAccessibleName(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.AccessibleContext.accessibleName")); // NOI18N
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FtpConfigurationPanel.class, "FtpConfigurationPanel.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox anonymousCheckBox;
    private JComboBox<Encryption> encryptionComboBox;
    private JLabel encryptionLabel;
    private JLabel hostLabel;
    private JTextField hostTextField;
    private JCheckBox ignoreDisconnectErrorsCheckBox;
    private JLabel initialDirectoryLabel;
    private JTextField initialDirectoryTextField;
    private JLabel keepAliveInfoLabel;
    private JLabel keepAliveLabel;
    private JTextField keepAliveTextField;
    private JCheckBox onlyLoginSecuredCheckBox;
    private JCheckBox passiveModeCheckBox;
    private JLabel passwordLabel;
    private JLabel passwordLabelInfo;
    private JPasswordField passwordTextField;
    private JLabel portLabel;
    private JTextField portTextField;
    private JLabel timeoutLabel;
    private JTextField timeoutTextField;
    private JLabel userLabel;
    private JTextField userTextField;
    // End of variables declaration//GEN-END:variables

    public String getHostName() {
        return hostTextField.getText();
    }

    public void setHostName(String hostName) {
        hostTextField.setText(hostName);
    }

    public String getPort() {
        return portTextField.getText();
    }

    public void setPort(String port) {
        portTextField.setText(port);
    }

    public String getEncryption() {
        return getEncryptionInternal().name();
    }

    public void setEncryption(String encryption) {
        encryptionComboBox.setSelectedItem(Encryption.valueOf(encryption));
        setEnabledOnlyLoginSecured();
    }

    public boolean isOnlyLoginSecured() {
        return onlyLoginSecuredCheckBox.isSelected();
    }

    public void setOnlyLoginSecured(boolean onlyLoginSecured) {
        onlyLoginSecuredCheckBox.setSelected(onlyLoginSecured);
    }

    public String getUserName() {
        return userTextField.getText();
    }

    public void setUserName(String userName) {
        userTextField.setText(userName);
    }

    public String getPassword() {
        return new String(passwordTextField.getPassword());
    }

    public void setPassword(String password) {
        passwordTextField.setText(password);
    }

    public boolean isAnonymousLogin() {
        return anonymousCheckBox.isSelected();
    }

    public void setAnonymousLogin(boolean anonymousLogin) {
        anonymousCheckBox.setSelected(anonymousLogin);
        setEnabledLoginCredentials();
    }

    public String getInitialDirectory() {
        return initialDirectoryTextField.getText();
    }

    public void setInitialDirectory(String initialDirectory) {
        initialDirectoryTextField.setText(initialDirectory);
    }

    public String getTimeout() {
        return timeoutTextField.getText();
    }

    public void setTimeout(String timeout) {
        timeoutTextField.setText(timeout);
    }

    public String getKeepAliveInterval() {
        return keepAliveTextField.getText();
    }

    public void setKeepAliveInterval(String keepAliveInterval) {
        keepAliveTextField.setText(keepAliveInterval);
    }

    public boolean isPassiveMode() {
        return passiveModeCheckBox.isSelected();
    }

    public void setPassiveMode(boolean passiveMode) {
        passiveModeCheckBox.setSelected(passiveMode);
    }

    public boolean getIgnoreDisconnectErrors() {
        return ignoreDisconnectErrorsCheckBox.isSelected();
    }

    public void setIgnoreDisconnectErrors(boolean ignoreDisconnectErrors) {
        ignoreDisconnectErrorsCheckBox.setSelected(ignoreDisconnectErrors);
    }

    @Override
    public void read(Configuration cfg) {
        setHostName(cfg.getValue(FtpConnectionProvider.HOST));
        setPort(cfg.getValue(FtpConnectionProvider.PORT));
        setEncryption(cfg.getValue(FtpConnectionProvider.ENCRYPTION));
        setOnlyLoginSecured(Boolean.valueOf(cfg.getValue(FtpConnectionProvider.ONLY_LOGIN_ENCRYPTED)));
        setUserName(cfg.getValue(FtpConnectionProvider.USER));
        setPassword(readPassword(cfg));
        setAnonymousLogin(Boolean.valueOf(cfg.getValue(FtpConnectionProvider.ANONYMOUS_LOGIN)));
        setInitialDirectory(cfg.getValue(FtpConnectionProvider.INITIAL_DIRECTORY));
        setTimeout(cfg.getValue(FtpConnectionProvider.TIMEOUT));
        setKeepAliveInterval(cfg.getValue(FtpConnectionProvider.KEEP_ALIVE_INTERVAL));
        setPassiveMode(Boolean.valueOf(cfg.getValue(FtpConnectionProvider.PASSIVE_MODE)));
        setIgnoreDisconnectErrors(Boolean.valueOf(cfg.getValue(FtpConnectionProvider.IGNORE_DISCONNECT_ERRORS)));
    }

    @Override
    public void store(Configuration cfg) {
        cfg.putValue(FtpConnectionProvider.HOST, getHostName());
        cfg.putValue(FtpConnectionProvider.PORT, getPort());
        cfg.putValue(FtpConnectionProvider.ENCRYPTION, getEncryption());
        cfg.putValue(FtpConnectionProvider.ONLY_LOGIN_ENCRYPTED, String.valueOf(isOnlyLoginSecured()));
        cfg.putValue(FtpConnectionProvider.USER, getUserName());
        cfg.putValue(FtpConnectionProvider.PASSWORD, getPassword(), true);
        cfg.putValue(FtpConnectionProvider.ANONYMOUS_LOGIN, String.valueOf(isAnonymousLogin()));
        cfg.putValue(FtpConnectionProvider.INITIAL_DIRECTORY, RemoteUtils.sanitizeUploadDirectory(getInitialDirectory(), false));
        cfg.putValue(FtpConnectionProvider.TIMEOUT, getTimeout());
        cfg.putValue(FtpConnectionProvider.KEEP_ALIVE_INTERVAL, getKeepAliveInterval());
        cfg.putValue(FtpConnectionProvider.PASSIVE_MODE, String.valueOf(isPassiveMode()));
        cfg.putValue(FtpConnectionProvider.IGNORE_DISCONNECT_ERRORS, String.valueOf(getIgnoreDisconnectErrors()));
    }

    // #200530
    /**
     * Read password from keyring, once it is needed.
     * @return password
     */
    private String readPassword(Configuration cfg) {
        if (!passwordRead) {
            passwordRead = true;
            return new FtpConfiguration(cfg).getPassword();
        }
        return cfg.getValue(FtpConnectionProvider.PASSWORD, true);
    }

    private final class DefaultDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            processUpdate();
        }
        @Override
        public void removeUpdate(DocumentEvent e) {
            processUpdate();
        }
        @Override
        public void changedUpdate(DocumentEvent e) {
            processUpdate();
        }
        private void processUpdate() {
            fireChange();
        }
    }

    private final class DefaultActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            fireChange();
        }
    }

    public static class EncryptionRenderer extends JLabel implements ListCellRenderer<Encryption>, UIResource {

        private static final long serialVersionUID = 567683279879854654L;


        public EncryptionRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Encryption> list, Encryption value, int index, boolean isSelected, boolean cellHasFocus) {
            setName("ComboBox.listRenderer"); // NOI18N
            // #175236
            if (value != null) {
                setText(value.getLabel());
            }
            setIcon(null);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }

        @Override
        public String getName() {
            String name = super.getName();
            return name == null ? "ComboBox.renderer" : name; // NOI18N
        }

    }

}
