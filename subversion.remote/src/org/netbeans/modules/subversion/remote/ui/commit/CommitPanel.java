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

package org.netbeans.modules.subversion.remote.ui.commit;

import org.netbeans.modules.versioning.util.common.CommitMessageMouseAdapter;
import javax.swing.LayoutStyle;
import java.awt.event.ActionEvent;
import org.netbeans.modules.versioning.util.common.SectionButton;
import org.netbeans.modules.versioning.util.UndoRedoSupport;
import java.awt.Component;
import java.awt.Container;
import org.netbeans.modules.versioning.hooks.SvnHook;
import org.netbeans.modules.versioning.util.ListenersSupport;
import org.netbeans.modules.versioning.util.VersioningListener;
import org.netbeans.modules.versioning.util.Utils;
import org.netbeans.modules.versioning.util.StringSelector;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.spellchecker.api.Spellchecker;
import org.netbeans.modules.versioning.hooks.SvnHookContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.Action;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.BoxLayout.Y_AXIS;
import javax.swing.KeyStroke;
import static javax.swing.SwingConstants.SOUTH;
import static javax.swing.SwingConstants.WEST;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import javax.swing.text.Keymap;
import org.netbeans.modules.subversion.remote.SvnFileNode;
import org.netbeans.modules.subversion.remote.SvnModuleConfig;
import org.netbeans.modules.subversion.remote.ui.diff.MultiDiffPanel;
import org.netbeans.modules.subversion.remote.ui.diff.Setup;
import org.netbeans.modules.subversion.remote.util.SvnUtils;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.util.AutoResizingPanel;
import org.netbeans.modules.versioning.util.PlaceholderPanel;
import org.netbeans.modules.versioning.util.TemplateSelector;
import org.netbeans.modules.versioning.util.VerticallyNonResizingPanel;
import org.openide.awt.TabbedPaneFactory;
import org.openide.filesystems.FileSystem;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.CallbackSystemAction;

/**
 *
 * @author  pk97937
 * @author  Marian Petras
 */
public class CommitPanel extends AutoResizingPanel implements PreferenceChangeListener, TableModelListener, ChangeListener, PropertyChangeListener {

    private final AutoResizingPanel basePanel = new AutoResizingPanel();
    static final Object EVENT_SETTINGS_CHANGED = new Object();
    private static final boolean DEFAULT_DISPLAY_FILES = true;
    private static final boolean DEFAULT_DISPLAY_HOOKS = false;

    final JLabel filesLabel = new JLabel();
    private final JPanel filesPanel = new JPanel(new GridLayout(1, 1));
    private final SectionButton filesSectionButton = new SectionButton();
    private final JPanel filesSectionPanel = new JPanel();
    private final SectionButton hooksSectionButton = new SectionButton();
    private final PlaceholderPanel hooksSectionPanel = new PlaceholderPanel();
    private final JLabel jLabel1 = new JLabel();
    private final JLabel jLabel2 = new JLabel();
    private final JScrollPane jScrollPane1 = new JScrollPane();
    private final JTextArea messageTextArea = new JTextArea();
    private final JLabel recentLink = new JLabel();
    private final JLabel templateLink = new JLabel();
    final PlaceholderPanel progressPanel = new PlaceholderPanel();

    private CommitTable commitTable;
    private Collection<SvnHook> hooks = Collections.emptyList();
    private SvnHookContext hookContext;
    private JTabbedPane tabbedPane;
    private final HashMap<VCSFileProxy, MultiDiffPanel> displayedDiffs = new HashMap<>();
    private UndoRedoSupport um;
    private final Preferences prefs;
    private final FileSystem fileSystem;

    /** Creates new form CommitPanel */
    public CommitPanel(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        prefs = SvnModuleConfig.getDefault(fileSystem).getPreferences();
        initComponents();
        initInteraction();
    }

    void setHooks(Collection<SvnHook> hooks, SvnHookContext context) {
        if (hooks == null) {
            hooks = Collections.emptyList();
        }
        this.hooks = hooks;
        this.hookContext = context;
    }

    void setCommitTable(CommitTable commitTable) {
        this.commitTable = commitTable;
    }

    void setErrorLabel(String htmlErrorLabel) {
        jLabel2.setText(htmlErrorLabel);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        prefs.addPreferenceChangeListener(this);
        commitTable.getTableModel().addTableModelListener(this);
        listenerSupport.fireVersioningEvent(EVENT_SETTINGS_CHANGED);
        initCollapsibleSections();
        TemplateSelector ts = new TemplateSelector(prefs);
        if (ts.isAutofill()) {
            messageTextArea.setText(ts.getTemplate());
        } else {
            String lastCommitMessage = SvnModuleConfig.getDefault(fileSystem).getLastCanceledCommitMessage();
            if (lastCommitMessage.isEmpty() && new StringSelector.RecentMessageSelector(prefs).isAutoFill()) {
                List<String> messages = Utils.getStringList(prefs, CommitAction.RECENT_COMMIT_MESSAGES);
                if (messages.size() > 0) {
                    lastCommitMessage = messages.get(0);
                }
            }
            messageTextArea.setText(lastCommitMessage);
        }
        messageTextArea.selectAll();
        um = UndoRedoSupport.register(messageTextArea);
    }

    @Override
    public void removeNotify() {
        commitTable.getTableModel().removeTableModelListener(this);
        prefs.removePreferenceChangeListener(this);
        if (um != null) {
            um.unregister();
            um = null;
        }
        super.removeNotify();
    }

    private void initCollapsibleSections() {
        initSectionButton(filesSectionButton, filesSectionPanel,
                          "initFilesPanel",                             //NOI18N
                          DEFAULT_DISPLAY_FILES);
        if(!hooks.isEmpty()) {
            Mnemonics.setLocalizedText(hooksSectionButton, (hooks.size() == 1)
                                           ? hooks.iterator().next().getDisplayName()
                                           : getMessage("LBL_Advanced")); // NOI18N                 
            initSectionButton(hooksSectionButton, hooksSectionPanel,
                              "initHooksPanel",                         //NOI18N
                              DEFAULT_DISPLAY_HOOKS);
        } else {
            hooksSectionButton.setVisible(false);
        }
    }

    private void initSectionButton(final SectionButton button,
                                   final JPanel panel,
                                   final String initPanelMethodName,
                                   final boolean defaultSectionDisplayed) {
        if (defaultSectionDisplayed) {
            displaySection(panel, initPanelMethodName);
        } else {
            hideSection(panel);
        }
        button.setSelected(defaultSectionDisplayed);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (panel.isVisible()) {
                    hideSection(panel);
                } else {
                    displaySection(panel, initPanelMethodName);
                }
            }
        });
    }

    private void displaySection(Container sectionPanel,
                                String initPanelMethodName) {
        if (sectionPanel.getComponentCount() == 0) {
            invokeInitPanelMethod(initPanelMethodName);
        }
        sectionPanel.setVisible(true);
        enlargeVerticallyAsNecessary();
    }

    private void hideSection(JPanel sectionPanel) {
        sectionPanel.setVisible(false);
    }

    private void invokeInitPanelMethod(String methodName) {
        try {
            getClass().getDeclaredMethod(methodName).invoke(this);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Do NOT remove this method. It may seem unused but is in fact called from {@link #invokeInitPanelMethod(java.lang.String)}
     * through reflection.
     */
    private void initFilesPanel() {

        /* this method is called using reflection from 'invokeInitPanelMethod()' */

        filesPanel.add(commitTable.getComponent());
        filesPanel.setPreferredSize(new Dimension(0, 2 * messageTextArea.getPreferredSize().height));

        filesSectionPanel.setLayout(new BoxLayout(filesSectionPanel, Y_AXIS));
        filesSectionPanel.add(filesLabel);
        filesSectionPanel.add(makeVerticalStrut(filesLabel, filesPanel, RELATED));
        filesSectionPanel.add(filesPanel);

        filesLabel.setAlignmentX(LEFT_ALIGNMENT);
        filesPanel.setAlignmentX(LEFT_ALIGNMENT);
    }

    /**
     * Do NOT remove this method. It may seem unused but is in fact called from {@link #invokeInitPanelMethod(java.lang.String)}
     * through reflection.
     */
    private void initHooksPanel() {

        /* this method is called using reflection from 'invokeInitPanelMethod()' */

        assert !hooks.isEmpty();

        if (hooks.size() == 1) {
            hooksSectionPanel.add(hooks.iterator().next().createComponent(hookContext));
        } else {
            JTabbedPane hooksTabbedPane = new JTabbedPane();
            for (SvnHook hook : hooks) {
                hooksTabbedPane.add(hook.createComponent(hookContext),
                                    hook.getDisplayName().replaceAll("\\&", "")); //NOI18N
            }
            hooksSectionPanel.add(hooksTabbedPane);
        }
    }

    String getCommitMessage() {
        return SvnUtils.fixLineEndings(messageTextArea.getText());
    }

    private void onBrowseRecentMessages() {
        StringSelector.RecentMessageSelector selector = new StringSelector.RecentMessageSelector(prefs);
        String message = selector.getRecentMessage(getMessage("CTL_CommitForm_RecentTitle"), //NOI18N
                                               getMessage("CTL_CommitForm_RecentPrompt"), //NOI18N
            Utils.getStringList(prefs, CommitAction.RECENT_COMMIT_MESSAGES));
        if (message != null) {
            messageTextArea.replaceSelection(message);
        }
    }

    private void onTemplate() {
        TemplateSelector ts = new TemplateSelector(prefs);
        if(ts.show("org.netbeans.modules.subversion.ui.commit.TemplatePanel")) { //NOI18N
            messageTextArea.setText(ts.getTemplate());
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getKey().startsWith(SvnModuleConfig.PROP_COMMIT_EXCLUSIONS)) {
            Runnable inAWT = new Runnable() {
                @Override
                public void run() {
                    commitTable.dataChanged();
                    listenerSupport.fireVersioningEvent(EVENT_SETTINGS_CHANGED);
                }
            };
            // this can be called from a background thread - e.g. change of exclusion status in Versioning view
            if (EventQueue.isDispatchThread()) {
                inAWT.run();
            } else {
                EventQueue.invokeLater(inAWT);
            }
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        listenerSupport.fireVersioningEvent(EVENT_SETTINGS_CHANGED);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    // <editor-fold defaultstate="collapsed" desc="UI Layout Code">
    private void initComponents() {

        jLabel1.setLabelFor(messageTextArea);
        Mnemonics.setLocalizedText(jLabel1, getMessage("CTL_CommitForm_Message")); // NOI18N

        recentLink.setIcon(new ImageIcon(getClass().getResource("/org/netbeans/modules/subversion/remote/resources/icons/recent_messages.png"))); // NOI18N
        recentLink.setToolTipText(getMessage("CTL_CommitForm_RecentMessages")); // NOI18N

        templateLink.setIcon(new ImageIcon(getClass().getResource("/org/netbeans/modules/subversion/remote/resources/icons/load_template.png"))); // NOI18N
        templateLink.setToolTipText(getMessage("CTL_CommitForm_LoadTemplate")); // NOI18N

        messageTextArea.setColumns(70);    //this determines the preferred width of the whole dialog
        messageTextArea.setLineWrap(true);
        messageTextArea.setRows(4);
        messageTextArea.setTabSize(4);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setMinimumSize(new Dimension(100, 18));
        jScrollPane1.setViewportView(messageTextArea);
        messageTextArea.getAccessibleContext().setAccessibleName(getMessage("ACSN_CommitForm_Message")); // NOI18N
        messageTextArea.getAccessibleContext().setAccessibleDescription(getMessage("ACSD_CommitForm_Message")); // NOI18N
        messageTextArea.addMouseListener(new CommitMessageMouseAdapter());

        Mnemonics.setLocalizedText(filesSectionButton, getMessage("LBL_CommitDialog_FilesToCommit")); // NOI18N
        Mnemonics.setLocalizedText(filesLabel, getMessage("CTL_CommitForm_FilesToCommit")); // NOI18N

        Mnemonics.setLocalizedText(hooksSectionButton, getMessage("LBL_Advanced")); // NOI18N

        JPanel topPanel = new VerticallyNonResizingPanel();
        topPanel.setLayout(new BoxLayout(topPanel, X_AXIS));
        topPanel.add(jLabel1);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(recentLink);
        topPanel.add(makeHorizontalStrut(recentLink, templateLink, RELATED));
        topPanel.add(templateLink);
        jLabel1.setAlignmentY(BOTTOM_ALIGNMENT);
        recentLink.setAlignmentY(BOTTOM_ALIGNMENT);
        templateLink.setAlignmentY(BOTTOM_ALIGNMENT);

        JPanel bottomPanel = new VerticallyNonResizingPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, X_AXIS));
        bottomPanel.add(progressPanel);
        progressPanel.setAlignmentY(CENTER_ALIGNMENT);

        basePanel.setLayout(new BoxLayout(basePanel, Y_AXIS));
        basePanel.add(topPanel);
        basePanel.add(makeVerticalStrut(jLabel1, jScrollPane1, RELATED));
        basePanel.add(jScrollPane1);
        basePanel.add(makeVerticalStrut(jScrollPane1, filesSectionButton, RELATED));
        filesSectionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, filesSectionButton.getMaximumSize().height));
        basePanel.add(filesSectionButton);
        basePanel.add(makeVerticalStrut(filesSectionButton, filesSectionPanel, RELATED));
        basePanel.add(filesSectionPanel);
        basePanel.add(makeVerticalStrut(filesSectionPanel, hooksSectionButton, RELATED));
        hooksSectionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, hooksSectionButton.getMaximumSize().height));
        basePanel.add(hooksSectionButton);
        basePanel.add(makeVerticalStrut(hooksSectionButton, hooksSectionPanel, RELATED));
        basePanel.add(hooksSectionPanel);
        basePanel.add(makeVerticalStrut(hooksSectionPanel, jLabel2, RELATED));
        basePanel.add(jLabel2);
        basePanel.add(makeVerticalStrut(hooksSectionPanel, bottomPanel, RELATED));
        basePanel.add(bottomPanel);
        setLayout(new BoxLayout(this, Y_AXIS));
        add(basePanel);
        topPanel.setAlignmentX(LEFT_ALIGNMENT);
        jScrollPane1.setAlignmentX(LEFT_ALIGNMENT);
        filesSectionButton.setAlignmentX(LEFT_ALIGNMENT);
        filesSectionPanel.setAlignmentX(LEFT_ALIGNMENT);
        hooksSectionButton.setAlignmentX(LEFT_ALIGNMENT);
        hooksSectionPanel.setAlignmentX(LEFT_ALIGNMENT);
        bottomPanel.setAlignmentX(LEFT_ALIGNMENT);

        basePanel.setBorder(createEmptyBorder(26,                       //top
                                    getContainerGap(WEST),    //left
                                    0,                        //bottom
                                    15));                     //right

        getAccessibleContext().setAccessibleName(getMessage("ACSN_CommitDialog")); // NOI18N
        getAccessibleContext().setAccessibleDescription(getMessage("ACSD_CommitDialog")); // NOI18N
    }// </editor-fold>

    private void initInteraction() {
        recentLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        recentLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onBrowseRecentMessages();
            }
        });
        templateLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        templateLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onTemplate();
            }
        });
        Spellchecker.register (messageTextArea);
    }

    private Component makeVerticalStrut(JComponent compA,
                                        JComponent compB,
                                        LayoutStyle.ComponentPlacement relatedUnrelated) {
        int height = LayoutStyle.getInstance().getPreferredGap(
                            compA,
                            compB,
                            relatedUnrelated,
                            SOUTH,
                            this);
        return Box.createVerticalStrut(height);
    }

    private Component makeHorizontalStrut(JComponent compA,
                                          JComponent compB,
                                          LayoutStyle.ComponentPlacement relatedUnrelated) {
        int width = LayoutStyle.getInstance().getPreferredGap(
                            compA,
                            compB,
                            relatedUnrelated,
                            WEST,
                            this);
        return Box.createHorizontalStrut(width);
    }

    private int getContainerGap(int direction) {
        return LayoutStyle.getInstance().getContainerGap(this,
                                                               direction,
                                                               null);
    }

    private static String getMessage(String msgKey) {
        return NbBundle.getMessage(CommitPanel.class, msgKey);
    }

    ListenersSupport listenerSupport = new ListenersSupport(this);
    public void addVersioningListener(VersioningListener listener) {
        listenerSupport.addListener(listener);
    }

    public void removeVersioningListener(VersioningListener listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == tabbedPane && tabbedPane.getSelectedComponent() == basePanel) {
            commitTable.setModifiedFiles(new HashSet<>(getModifiedFiles().keySet()));
        }
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);

        // XXX #250546 Reason of overriding: to process global shortcut.
        if ((JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT == condition) && (ret == false) && !e.isConsumed()) {

            Keymap km = Lookup.getDefault().lookup(Keymap.class);
            Action action = (km != null) ? km.getAction(ks) : null;

            if (action == null) {
                return false;
            }

            if (action instanceof CallbackSystemAction) {
                CallbackSystemAction csAction = (CallbackSystemAction) action;
                if (tabbedPane != null) {
                    Action a = tabbedPane.getActionMap().get(csAction.getActionMapKey());
                    if (a != null) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Utilities.keyToString(ks)));
                        return true;
                    }
                }
            }
            return false;
        } else {
            return ret;
        }
    }

    void openDiff (SvnFileNode[] nodes) {
        for (SvnFileNode node : nodes) {
            if (tabbedPane == null) {
                initializeTabs();
            }
            VCSFileProxy file = node.getFile();
            MultiDiffPanel panel = displayedDiffs.get(file);
            if (panel == null) {
                panel = new MultiDiffPanel(file, Setup.REVISION_BASE, Setup.REVISION_CURRENT, false); // switch the last parameter to true if editable diff works poorly
                displayedDiffs.put(file, panel);
            }
            if (tabbedPane.indexOfComponent(panel) == -1) {
                tabbedPane.addTab(file.getName(), panel);
            }
            tabbedPane.setSelectedComponent(panel);
            tabbedPane.requestFocusInWindow();
            panel.requestActive();
        }
        revalidate();
        repaint();
    }

    /**
     * Returns save cookies available for files in the commit table
     * @return
     */
    SaveCookie[] getSaveCookies() {
        return getModifiedFiles().values().toArray(new SaveCookie[0]);
    }
    
    /**
     * Returns editor cookies available for modified and not open files in the commit table
     * @return
     */
    EditorCookie[] getEditorCookies() {
        LinkedList<EditorCookie> allCookies = new LinkedList<>();
        for (Map.Entry<VCSFileProxy, MultiDiffPanel> e : displayedDiffs.entrySet()) {
            EditorCookie[] cookies = e.getValue().getEditorCookies(true);
            if (cookies.length > 0) {
                allCookies.add(cookies[0]);
            }
        }
        return allCookies.toArray(new EditorCookie[allCookies.size()]);
    }

    /**
     * Returns true if trying to commit from the commit tab or the user confirmed his action
     * @return
     */
    boolean canCommit() {
        boolean result = true;
        if (tabbedPane != null && tabbedPane.getSelectedComponent() != basePanel) {
            NotifyDescriptor nd = new NotifyDescriptor(NbBundle.getMessage(CommitPanel.class, "MSG_CommitDialog_CommitFromDiff"), //NOI18N
                    NbBundle.getMessage(CommitPanel.class, "LBL_CommitDialog_CommitFromDiff"), //NOI18N
                    NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE, null, NotifyDescriptor.YES_OPTION);
            result = NotifyDescriptor.YES_OPTION == DialogDisplayer.getDefault().notify(nd);
        }
        return result;
    }

    private void initializeTabs () {
         tabbedPane = TabbedPaneFactory.createCloseButtonTabbedPane();
         tabbedPane.addPropertyChangeListener(this);
         tabbedPane.addTab(NbBundle.getMessage(CommitPanel.class, "CTL_CommitDialog_Tab_Commit"), basePanel); //NOI18N
         tabbedPane.setPreferredSize(basePanel.getPreferredSize());
         add(tabbedPane);
         tabbedPane.addChangeListener(this);
    }
    
    @Override
    public void propertyChange (PropertyChangeEvent evt) {
        if (TabbedPaneFactory.PROP_CLOSE.equals(evt.getPropertyName())) {
            JComponent comp = (JComponent) evt.getNewValue();
            removeTab(comp);
        }
    }
    
    private void removeTab (JComponent comp) {
        if (basePanel != comp && tabbedPane != null && tabbedPane.getTabCount() > 1) {
            tabbedPane.remove(comp);
            revalidate();
        }
    }

    private HashMap<VCSFileProxy, SaveCookie> getModifiedFiles () {
        HashMap<VCSFileProxy, SaveCookie> modifiedFiles = new HashMap<>();
        for (Map.Entry<VCSFileProxy, MultiDiffPanel> e : displayedDiffs.entrySet()) {
            SaveCookie[] cookies = e.getValue().getSaveCookies(false);
            if (cookies.length > 0) {
                modifiedFiles.put(e.getKey(), cookies[0]);
            }
        }
        return modifiedFiles;
    }
}
