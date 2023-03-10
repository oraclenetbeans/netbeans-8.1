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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.cnd.makeproject.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cnd.api.project.NativeProject;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.api.remote.ServerListUI;
import org.netbeans.modules.cnd.api.remote.ServerRecord;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.makeproject.BrokenReferencesSupport;
import org.netbeans.modules.cnd.makeproject.MakeProjectUtils;
import org.netbeans.modules.cnd.makeproject.api.configurations.CompilerSet2Configuration;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationDescriptor;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationDescriptorProvider;
import org.netbeans.modules.cnd.makeproject.api.configurations.DevelopmentHostConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.util.NbBundle;
import org.openide.util.Pair;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.Presenter;

public class RemoteDevelopmentAction extends AbstractAction implements Presenter.Menu, Presenter.Popup {

    /** Key for remembering project in JMenuItem
     */
    private static final String HOST_ENV = "org.netbeans.modules.cnd.makeproject.ui.RemoteHost"; // NOI18N
    private static final String DATA = "org.netbeans.modules.cnd.makeproject.ui.RemoteDevelopmentAction.Data"; // NOI18N
    private static final RequestProcessor RP = new RequestProcessor("RemoteDevelopmentAction", 1); // NOI18N
    private JMenu subMenu;
    private final Project[] projects;

    /** 
     * Factory method: returns RemoteDevelopmentAction or null if it's impossible to create it 
     * (e. g. if one of the projects is full remote)
     */
    public static RemoteDevelopmentAction create(Project[] projects) {
        for (Project p : projects) {
            if (!MakeProjectUtils.canChangeHost(p)) {
                return null;
            }
        }
        return new RemoteDevelopmentAction(projects);
    }
    
    private RemoteDevelopmentAction(Project[] projects) {
        super(NbBundle.getMessage(RemoteDevelopmentAction.class, "LBL_RemoteDevelopmentAction_Name"), // NOI18N
                null);
        this.projects = projects;
    }

    public void actionPerformed(java.awt.event.ActionEvent ev) {
        // no operation
    }

    public JMenuItem getPopupPresenter() {
        createSubMenu();
        return subMenu;
    }

    public JMenuItem getMenuPresenter() {
        createSubMenu();
        return subMenu;
    }

    private void createSubMenu() {
        if (subMenu == null) {
            String label = NbBundle.getMessage(RemoteDevelopmentAction.class, "LBL_RemoteDevelopmentAction_Name"); // NOI18N
            subMenu = new JMenu(label);
        }

        subMenu.removeAll();
        
        // find out whether all projects are set to one and the same host;
        // if yes, then we'll make it selected in submenu
        ExecutionEnvironment currExecEnv = null;
        boolean envsAreSame = true;
        subMenu.setEnabled(true);
        
        final List<Pair<Project, MakeConfiguration>> projectsAndConfs = new ArrayList<>();
        
        for (Project project : projects) {
            // paranoidal: should be already checked in create() factory method
            if (!MakeProjectUtils.canChangeHost(project)) {
                subMenu.setEnabled(false);
                return;
            }
            ConfigurationDescriptorProvider pdp = project.getLookup().lookup(ConfigurationDescriptorProvider.class);
            if (pdp == null || pdp.getConfigurationDescriptor() == null || pdp.getConfigurationDescriptor().getActiveConfiguration() == null) {
                subMenu.setEnabled(false);
                return;
            }
            final MakeConfiguration mconf = pdp.getConfigurationDescriptor().getActiveConfiguration();
            if (mconf == null) {
                subMenu.setEnabled(false);
                return;
            }
            ExecutionEnvironment e = mconf.getDevelopmentHost().getExecutionEnvironment();
            if (e != null && envsAreSame) {
                if (currExecEnv == null) {
                    currExecEnv = e;
                } else if (!currExecEnv.equals(e)) {
                    currExecEnv = null;
                    envsAreSame = false;
                }
            }
            projectsAndConfs.add(Pair.of(project, mconf));
        }
        

        ActionListener jmiActionListener = new MenuItemActionListener();
        for (ServerRecord record : ServerList.getRecords()) {
            JRadioButtonMenuItem jmi = new JRadioButtonMenuItem(record.getServerDisplayName(), record.getExecutionEnvironment().equals(currExecEnv));
            subMenu.add(jmi);
            jmi.putClientProperty(HOST_ENV, record.getExecutionEnvironment());
            jmi.putClientProperty(DATA, projectsAndConfs);
            jmi.addActionListener(jmiActionListener);
        }

        // Now add the Manage... action. Do it in it's own menu item othervise it will get shifted to the right.
        subMenu.add(new JSeparator());
        final JMenuItem managePlatformsItem = new JMenuItem(NbBundle.getMessage(RemoteDevelopmentAction.class, "LBL_ManagePlatforms_Name")); // NOI18N
        subMenu.add(managePlatformsItem);
        managePlatformsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                AtomicReference<ExecutionEnvironment> selectedEnv = new AtomicReference<>();
                if (ServerListUI.showServerListDialog(selectedEnv)) {
                    ExecutionEnvironment env = selectedEnv.get();
                    if (env != null) {
                        setRemoteDevelopmentHost(managePlatformsItem, env, projectsAndConfs);
                    }
                }
            }
        });
    }

    private static void setRemoteDevelopmentHost(final Object source, final ExecutionEnvironment execEnv, final List<Pair<Project, MakeConfiguration>> projectsAndConfs) {
        if (SwingUtilities.isEventDispatchThread()) {
            RP.post(new Runnable(){
                public void run() {
                    _setRemoteDevelopmentHost(source, execEnv, projectsAndConfs);
                }
            });
        } else {
            _setRemoteDevelopmentHost(source, execEnv, projectsAndConfs);
        }
    }

    private static void _setRemoteDevelopmentHost(Object source, ExecutionEnvironment execEnv, List<Pair<Project, MakeConfiguration>> projectsAndConfs) {
        ServerRecord record = ServerList.get(execEnv);
        if (!record.isSetUp()) {
            if (!record.setUp()) {
                return; //true;
            }
        }
        for (Pair<Project, MakeConfiguration> pac : projectsAndConfs) {
            _setRemoteDevelopmentHost(source, pac.second(), execEnv, pac.first());
        }
    }

    private static void _setRemoteDevelopmentHost(Object source, MakeConfiguration mconf, ExecutionEnvironment execEnv, Project project) {
        if (mconf != null && execEnv != null) {
            DevelopmentHostConfiguration dhc = new DevelopmentHostConfiguration(execEnv);
            DevelopmentHostConfiguration oldDhc = mconf.getDevelopmentHost();
            if (dhc.getExecutionEnvironment() == oldDhc.getExecutionEnvironment()) {
                return; //true;
            }
            mconf.setDevelopmentHost(dhc);
            // try to use the same compiler set
            CompilerSet2Configuration oldCS = mconf.getCompilerSet();
            if (oldCS.isDefaultCompilerSet()) {
                mconf.setCompilerSet(new CompilerSet2Configuration(dhc));
            } else {
                String oldCSName = oldCS.getName();
                CompilerSetManager csm = CompilerSetManager.get(dhc.getExecutionEnvironment());
                CompilerSet newCS = csm.getCompilerSet(oldCSName);
                // if not found => use default from new host
                newCS = (newCS == null) ? csm.getDefaultCompilerSet() : newCS;
                mconf.setCompilerSet(new CompilerSet2Configuration(dhc, newCS));
            }
//                    PlatformConfiguration platformConfiguration = mconf.getPlatform();
//                    platformConfiguration.propertyChange(new PropertyChangeEvent(
//                            jmi, DevelopmentHostConfiguration.PROP_DEV_HOST, oldDhc, dhc));
            //FIXUP: please send PropertyChangeEvent to MakeConfiguration listeners
            //when you do this changes
            //see cnd.tha.THAMainProjectAction which should use huck to get these changes
            NativeProject npp = project.getLookup().lookup(NativeProject.class);
            if(npp instanceof PropertyChangeListener) {
                ((PropertyChangeListener)npp).propertyChange(new PropertyChangeEvent(source, ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE, null, mconf));
            }
            ConfigurationDescriptorProvider configurationDescriptorProvider = project.getLookup().lookup(ConfigurationDescriptorProvider.class);
            ConfigurationDescriptor configurationDescriptor = configurationDescriptorProvider.getConfigurationDescriptor();
            configurationDescriptor.setModified();
            BrokenReferencesSupport.updateProblems(project);
        }
    }

    private static class MenuItemActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JMenuItem) {
                JMenuItem jmi = (JMenuItem) e.getSource();
                ExecutionEnvironment execEnv = (ExecutionEnvironment) jmi.getClientProperty(HOST_ENV);
                
                List<Pair<Project, MakeConfiguration>> projectsAndConfs = (List<Pair<Project, MakeConfiguration>>) jmi.getClientProperty(DATA);
                setRemoteDevelopmentHost(jmi, execEnv, projectsAndConfs);
            }
        }
    }
}
