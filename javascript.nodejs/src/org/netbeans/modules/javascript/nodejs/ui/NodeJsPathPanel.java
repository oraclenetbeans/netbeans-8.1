/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript.nodejs.ui;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.javascript.nodejs.exec.NodeExecutable;
import org.netbeans.modules.javascript.nodejs.util.FileUtils;
import org.netbeans.modules.javascript.nodejs.util.NodeJsUtils;
import org.netbeans.modules.web.clientproject.api.network.NetworkException;
import org.netbeans.modules.web.clientproject.api.util.StringUtilities;
import org.netbeans.modules.web.common.api.Version;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

public final class NodeJsPathPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(NodeJsPathPanel.class.getName());

    private static final RequestProcessor RP = new RequestProcessor(NodeJsPathPanel.class);

    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private final RequestProcessor.Task versionTask;

    volatile File nodeSources = null;


    public NodeJsPathPanel() {
        initComponents();
        init();

        versionTask = RP.create(new Runnable() {
            @Override
            public void run() {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setVersion();
                    }
                });
            }
        });
    }

    @NbBundle.Messages({
        "# {0} - node.js file name",
        "NodeJsPathPanel.node.hint1=Full path of node file (typically {0}).",
        "# {0} - node.js file name",
        "# {1} - node.js alternative file name",
        "NodeJsPathPanel.node.hint2=Full path of node file (typically {0} or {1}).",
    })
    private void init() {
        sourcesTextField.setText(" "); // NOI18N
        String[] nodes = NodeExecutable.NODE_NAMES;
        if (nodes.length > 1) {
            nodeHintLabel.setText(Bundle.NodeJsPathPanel_node_hint2(nodes[0], nodes[1]));
        } else {
            nodeHintLabel.setText(Bundle.NodeJsPathPanel_node_hint1(nodes[0]));
        }
        // listeners
        nodeTextField.getDocument().addDocumentListener(new NodeDocumentListener());
        sourcesTextField.getDocument().addDocumentListener(new DefaultDocumentListener());
    }

    public String getNode() {
        return nodeTextField.getText();
    }

    public void setNode(String node) {
        nodeTextField.setText(node);
    }

    @CheckForNull
    public String getNodeSources() {
        if (nodeSources != null) {
            return nodeSources.getAbsolutePath();
        }
        return null;
    }

    public void setNodeSources(String nodeSources) {
        if (StringUtilities.hasText(nodeSources)) {
            this.nodeSources = new File(nodeSources);
            setNodeSourcesDescription();
        }
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    public void enablePanel(boolean enabled) {
        assert EventQueue.isDispatchThread();
        nodeLabel.setEnabled(enabled);
        nodeTextField.setEnabled(enabled);
        nodeBrowseButton.setEnabled(enabled);
        nodeSearchButton.setEnabled(enabled);
        nodeHintLabel.setEnabled(enabled);
        nodeInstallLabel.setVisible(enabled);
        sourcesLabel.setEnabled(enabled);
        sourcesTextField.setEnabled(enabled);
        versionInfoLabel.setEnabled(enabled);
        selectSourcesButton.setEnabled(enabled);
        downloadSourcesButton.setEnabled(false);
        if (enabled) {
            if (nodeSources != null) {
                setNodeSourcesDescription();
            }
            setVersion();
        }
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    void detectVersion() {
        versionTask.schedule(100);
    }

    @NbBundle.Messages("NodeJsPathPanel.version.detecting=detecting...")
    void setVersion() {
        assert EventQueue.isDispatchThread();
        downloadSourcesButton.setEnabled(false);
        if (nodeSources == null) {
            setNodeSourcesDescription(Bundle.NodeJsPathPanel_version_detecting());
        }
        final String nodePath = getNode();
        RP.post(new Runnable() {
            @Override
            public void run() {
                String version = null;
                NodeExecutable node = NodeExecutable.forPath(nodePath);
                if (node != null) {
                    Version nodeVersion = node.getVersion();
                    if (nodeVersion != null) {
                        version = nodeVersion.toString();
                    }
                }
                final String versionRef = version;
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (versionRef != null) {
                            downloadSourcesButton.setEnabled(true);
                        }
                        if (nodeSources == null) {
                            setNodeSourcesDescription(versionRef);
                        }
                    }
                });
            }
        });
    }

    @NbBundle.Messages({
        "# {0} - version",
        "NodeJsPathPanel.sources.exists=Sources for version {0} already exist. Download again?",
        "NodeJsPathPanel.sources.downloading=Downloading...",
        "NodeJsPathPanel.download.success=Node.js sources downloaded successfully.",
        "# {0} - file URL",
        "NodeJsPathPanel.download.failure=File {0} cannot be downloaded.",
        "NodeJsPathPanel.download.error=Error occured during download (see IDE log).",
    })
    private void downloadSources() {
        assert EventQueue.isDispatchThread();
        downloadSourcesButton.setEnabled(false);
        String nodePath = getNode();
        final NodeExecutable node = NodeExecutable.forPath(nodePath);
        assert node != null : nodePath;
        final Version version = node.getVersion();
        assert version != null : nodePath;
        if (NodeJsUtils.hasNodeSources(version)) {
            nodeSources = null;
            setNodeSourcesDescription(version.toString());
            NotifyDescriptor.Confirmation confirmation = new NotifyDescriptor.Confirmation(
                    Bundle.NodeJsPathPanel_sources_exists(version.toString()),
                    NotifyDescriptor.YES_NO_OPTION);
            if (DialogDisplayer.getDefault().notify(confirmation) == NotifyDescriptor.NO_OPTION) {
                downloadSourcesButton.setEnabled(true);
                return;
            }
        }
        sourcesTextField.setText(Bundle.NodeJsPathPanel_sources_downloading());
        RP.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (FileUtils.downloadNodeSources(version, node.isIojs())) {
                        StatusDisplayer.getDefault().setStatusText(Bundle.NodeJsPathPanel_download_success());
                    }
                    nodeSources = null;
                } catch (NetworkException ex) {
                    LOGGER.log(Level.INFO, null, ex);
                    informUser(Bundle.NodeJsPathPanel_download_failure(ex.getFailedRequests().get(0)));
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, null, ex);
                    informUser(Bundle.NodeJsPathPanel_download_error());
                }
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setNodeSourcesDescription(version.toString());
                        downloadSourcesButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void informUser(String message) {
        NotifyDescriptor descriptor = new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE);
        DialogDisplayer.getDefault().notifyLater(descriptor);
    }

    private void setNodeSourcesDescription() {
        assert EventQueue.isDispatchThread();
        File nodeSourcesRef = nodeSources;
        assert nodeSourcesRef != null;
        sourcesTextField.setText(nodeSourcesRef.getAbsolutePath());
    }

    @NbBundle.Messages({
        "# {0} - node.js version",
        "NodeJsPathPanel.sources.downloaded=Downloaded (version {0})",
        "# {0} - node.js version",
        "NodeJsPathPanel.sources.not.downloaded=Not downloaded (version {0})",
        "NodeJsPathPanel.sources.na=Not available",
    })
    private void setNodeSourcesDescription(@NullAllowed String version) {
        assert EventQueue.isDispatchThread();
        String text;
        if (version == null) {
            text = Bundle.NodeJsPathPanel_sources_na();
        } else if (NodeJsUtils.hasNodeSources(version)) {
            text = Bundle.NodeJsPathPanel_sources_downloaded(version);
        } else {
            text = Bundle.NodeJsPathPanel_sources_not_downloaded(version);
        }
        sourcesTextField.setText(text);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
     * Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nodeLabel = new JLabel();
        nodeTextField = new JTextField();
        nodeBrowseButton = new JButton();
        nodeSearchButton = new JButton();
        nodeHintLabel = new JLabel();
        nodeInstallLabel = new JLabel();
        sourcesLabel = new JLabel();
        sourcesTextField = new JTextField();
        downloadSourcesButton = new JButton();
        selectSourcesButton = new JButton();
        versionInfoLabel = new JLabel();

        Mnemonics.setLocalizedText(nodeLabel, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.nodeLabel.text")); // NOI18N

        Mnemonics.setLocalizedText(nodeBrowseButton, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.nodeBrowseButton.text")); // NOI18N
        nodeBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nodeBrowseButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(nodeSearchButton, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.nodeSearchButton.text")); // NOI18N
        nodeSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nodeSearchButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(nodeHintLabel, "HINT"); // NOI18N

        Mnemonics.setLocalizedText(nodeInstallLabel, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.nodeInstallLabel.text")); // NOI18N
        nodeInstallLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                nodeInstallLabelMousePressed(evt);
            }
            public void mouseEntered(MouseEvent evt) {
                nodeInstallLabelMouseEntered(evt);
            }
        });

        Mnemonics.setLocalizedText(sourcesLabel, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.sourcesLabel.text")); // NOI18N

        sourcesTextField.setEditable(false);
        sourcesTextField.setColumns(30);

        Mnemonics.setLocalizedText(downloadSourcesButton, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.downloadSourcesButton.text")); // NOI18N
        downloadSourcesButton.setEnabled(false);
        downloadSourcesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadSourcesButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(selectSourcesButton, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.selectSourcesButton.text")); // NOI18N
        selectSourcesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                selectSourcesButtonActionPerformed(evt);
            }
        });

        Mnemonics.setLocalizedText(versionInfoLabel, NbBundle.getMessage(NodeJsPathPanel.class, "NodeJsPathPanel.versionInfoLabel.text")); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(nodeLabel)
                    .addComponent(sourcesLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(nodeTextField)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nodeBrowseButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nodeSearchButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(nodeHintLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(nodeInstallLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addComponent(versionInfoLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sourcesTextField, GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(downloadSourcesButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(selectSourcesButton))))
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(nodeLabel)
                    .addComponent(nodeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(nodeBrowseButton)
                    .addComponent(nodeSearchButton))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(nodeHintLabel)
                    .addComponent(nodeInstallLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(sourcesLabel)
                    .addComponent(downloadSourcesButton)
                    .addComponent(selectSourcesButton)
                    .addComponent(sourcesTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionInfoLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    @NbBundle.Messages("NodeJsPathPanel.node.browse.title=Select node")
    private void nodeBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_nodeBrowseButtonActionPerformed
        assert EventQueue.isDispatchThread();
        File file = new FileChooserBuilder(NodeJsPathPanel.class)
                .setFilesOnly(true)
                .setTitle(Bundle.NodeJsPathPanel_node_browse_title())
                .showOpenDialog();
        if (file != null) {
            nodeTextField.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_nodeBrowseButtonActionPerformed

    @NbBundle.Messages("NodeJsPathPanel.node.none=No node executable was found.")
    private void nodeSearchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_nodeSearchButtonActionPerformed
        assert EventQueue.isDispatchThread();
        for (String node : FileUtils.findFileOnUsersPath(NodeExecutable.NODE_NAMES)) {
            nodeTextField.setText(new File(node).getAbsolutePath());
            return;
        }
        // no node found
        StatusDisplayer.getDefault().setStatusText(Bundle.NodeJsPathPanel_node_none());
    }//GEN-LAST:event_nodeSearchButtonActionPerformed

    private void downloadSourcesButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadSourcesButtonActionPerformed
        downloadSources();
    }//GEN-LAST:event_downloadSourcesButtonActionPerformed

    private void nodeInstallLabelMouseEntered(MouseEvent evt) {//GEN-FIRST:event_nodeInstallLabelMouseEntered
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_nodeInstallLabelMouseEntered

    private void nodeInstallLabelMousePressed(MouseEvent evt) {//GEN-FIRST:event_nodeInstallLabelMousePressed
        try {
            HtmlBrowser.URLDisplayer.getDefault().showURL(new URL("http://nodejs.org/")); // NOI18N
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }//GEN-LAST:event_nodeInstallLabelMousePressed

    @NbBundle.Messages("NodeJsPathPanel.sources.browse.title=Select node.js sources")
    private void selectSourcesButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_selectSourcesButtonActionPerformed
        assert EventQueue.isDispatchThread();
        File sources = new FileChooserBuilder(NodeJsPathPanel.class)
                .setDirectoriesOnly(true)
                .setTitle(Bundle.NodeJsPathPanel_sources_browse_title())
                .showOpenDialog();
        if (sources != null) {
            nodeSources = sources;
            setNodeSourcesDescription();
        }
    }//GEN-LAST:event_selectSourcesButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton downloadSourcesButton;
    private JButton nodeBrowseButton;
    private JLabel nodeHintLabel;
    private JLabel nodeInstallLabel;
    private JLabel nodeLabel;
    private JButton nodeSearchButton;
    private JTextField nodeTextField;
    private JButton selectSourcesButton;
    private JLabel sourcesLabel;
    private JTextField sourcesTextField;
    private JLabel versionInfoLabel;
    // End of variables declaration//GEN-END:variables

    //~ Inner classes

    private final class NodeDocumentListener implements DocumentListener {

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
            NodeExecutable node = NodeExecutable.forPath(getNode());
            if (node != null) {
                node.resetVersion();
            }
            detectVersion();
        }

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

}
