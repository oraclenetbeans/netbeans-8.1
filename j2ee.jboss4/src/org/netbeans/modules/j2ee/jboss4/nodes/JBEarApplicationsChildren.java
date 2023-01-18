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

package org.netbeans.modules.j2ee.jboss4.nodes;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import org.netbeans.modules.j2ee.jboss4.JBDeploymentManager;
import org.netbeans.modules.j2ee.jboss4.JBRemoteAction;
import org.netbeans.modules.j2ee.jboss4.JBoss5ProfileServiceProxy;
import org.netbeans.modules.j2ee.jboss4.nodes.actions.Refreshable;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * It describes children nodes of the EJB Modules node. Implements
 * Refreshable interface and due to it can be refreshed via ResreshModulesAction.
 *
 * @author Michal Mocnak
 */
public class JBEarApplicationsChildren extends JBAsyncChildren implements Refreshable {

    private static final Logger LOGGER = Logger.getLogger(JBEarApplicationsChildren.class.getName());

    private final JBAbilitiesSupport abilitiesSupport;

    private final Lookup lookup;

    JBEarApplicationsChildren(Lookup lookup) {
        this.lookup = lookup;
        this.abilitiesSupport = new JBAbilitiesSupport(lookup);
    }

    @Override
    public void updateKeys() {
        setKeys(new Object[]{Util.WAIT_NODE});
        getExecutorService().submit(abilitiesSupport.isJB7x() ? new JBoss7EarApplicationNodeUpdater() : new JBossEarApplicationNodeUpdater(), 0);
    }

    class JBossEarApplicationNodeUpdater implements Runnable {

        List keys = new ArrayList();

        @Override
        public void run() {
            try {
                lookup.lookup(JBDeploymentManager.class).invokeRemoteAction(new JBRemoteAction<Void>() {

                    @Override
                    public Void action(MBeanServerConnection connection, JBoss5ProfileServiceProxy profileService) throws Exception {
                        // Query to the jboss4 server
                        ObjectName searchPattern;
                        String propertyName;
                        if (abilitiesSupport.isRemoteManagementSupported()
                                && (abilitiesSupport.isJB4x() || abilitiesSupport.isJB6x())) {
                            searchPattern = new ObjectName("jboss.management.local:j2eeType=J2EEApplication,*"); // NOI18N
                            propertyName = "name"; // NOI18N
                        } else {
                            searchPattern = new ObjectName("jboss.j2ee:service=EARDeployment,*"); // NOI18N
                            propertyName = "url"; // NOI18N
                        }

                        Method method = connection.getClass().getMethod("queryMBeans", new Class[] {ObjectName.class, QueryExp.class});
                        method = Util.fixJava4071957(method);
                        Set managedObj = (Set) method.invoke(connection, new Object[] {searchPattern, null});

                        // Query results processing
                        for (Iterator it = managedObj.iterator(); it.hasNext();) {
                            try {
                                ObjectName elem = ((ObjectInstance) it.next()).getObjectName();
                                String name = elem.getKeyProperty(propertyName);

                                if (abilitiesSupport.isRemoteManagementSupported()
                                        && (abilitiesSupport.isJB4x() || abilitiesSupport.isJB6x())) {
                                    if (name.endsWith(".sar") || name.endsWith(".deployer")) { // NOI18N
                                        continue;
                                    }
                                } else {
                                    name = name.substring(1, name.length() - 1); // NOI18N
                                }

                                keys.add(new JBEarApplicationNode(name, lookup));
                            } catch (Exception ex) {
                                LOGGER.log(Level.INFO, null, ex);
                            }
                        }
                        return null;
                    }
                });
            } catch (ExecutionException ex) {
                LOGGER.log(Level.INFO, null, ex);
            }

            setKeys(keys);
        }
    }

    class JBoss7EarApplicationNodeUpdater implements Runnable {

        List keys = new ArrayList();

        @Override
        public void run() {
            try {
                final JBDeploymentManager dm = (JBDeploymentManager) lookup.lookup(JBDeploymentManager.class);
                dm.invokeLocalAction(new Callable<Void>() {

                    @Override
                    public Void call() {
                        try {
                            Target[] targets = dm.getTargets();
                            ModuleType moduleType = ModuleType.EAR;

                            //Get all deployed EAR files.
                            TargetModuleID[] modules = dm.getAvailableModules(moduleType, targets);
                            // Module list may be null if nothing is deployed.
                            if (modules != null) {
                                for (int intModule = 0; intModule < modules.length; intModule++) {
                                    keys.add(new JBEarApplicationNode(modules[intModule].getModuleID(), lookup));
                                }
                            }
                        } catch (TargetException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (IllegalStateException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        return null;
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, null, ex);
            }

            setKeys(keys);
        }
    }

    @Override
    protected void addNotify() {
        updateKeys();
    }

    @Override
    protected void removeNotify() {
        setKeys(java.util.Collections.EMPTY_SET);
    }

    @Override
    protected org.openide.nodes.Node[] createNodes(Object key) {
        if (key instanceof JBEarApplicationNode){
            return new Node[]{(JBEarApplicationNode)key};
        }

        if (key instanceof String && key.equals(Util.WAIT_NODE)){
            return new Node[]{Util.createWaitNode()};
        }

        return null;
    }

}
