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

package org.netbeans.modules.maven.j2ee.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.netbeans.api.extexecution.startup.StartupExtender;
import org.netbeans.api.project.Project;
import org.netbeans.modules.j2ee.deployment.devmodules.api.Deployment;
import org.netbeans.modules.j2ee.deployment.devmodules.api.Deployment.DeploymentException;
import org.netbeans.modules.j2ee.deployment.devmodules.spi.J2eeModuleProvider;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.execute.ActiveJ2SEPlatformProvider;
import org.netbeans.modules.maven.api.execute.ExecutionContext;
import org.netbeans.modules.maven.api.execute.LateBoundPrerequisitesChecker;
import org.netbeans.modules.maven.api.execute.PrerequisitesChecker;
import org.netbeans.modules.maven.api.execute.RunConfig;
import static org.netbeans.modules.maven.j2ee.ui.customizer.impl.CustomizerRunWeb.PROP_ALWAYS_BUILD_BEFORE_RUNNING;
import org.netbeans.modules.maven.j2ee.web.WebModuleProviderImpl;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.util.Exceptions;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;


@ProjectServiceProvider(service = {PrerequisitesChecker.class, LateBoundPrerequisitesChecker.class}, projectType={
    "org-netbeans-modules-maven/" + NbMavenProject.TYPE_WAR,
    "org-netbeans-modules-maven/" + NbMavenProject.TYPE_EJB,
    "org-netbeans-modules-maven/" + NbMavenProject.TYPE_EAR,
    "org-netbeans-modules-maven/" + NbMavenProject.TYPE_APPCLIENT,
    "org-netbeans-modules-maven/" + NbMavenProject.TYPE_OSGI
})
public class PrerequisitesCheckerImpl implements PrerequisitesChecker, LateBoundPrerequisitesChecker {

    private final List<String> SINGLE_ACTIONS = Arrays.asList(new String[] {
        ActionProvider.COMMAND_RUN_SINGLE + ".deploy",
        ActionProvider.COMMAND_DEBUG_SINGLE + ".deploy",
        ActionProvider.COMMAND_PROFILE + ".deploy"
    });

    private final List applicableActions = Arrays.asList(new String[] {
        ActionProvider.COMMAND_RUN,
        ActionProvider.COMMAND_RUN_SINGLE + ".deploy",
        ActionProvider.COMMAND_DEBUG,
        ActionProvider.COMMAND_DEBUG_SINGLE + ".deploy",
        ActionProvider.COMMAND_PROFILE,
        ActionProvider.COMMAND_PROFILE + ".deploy"
    });
    

    @Override
    public boolean checkRunConfig(RunConfig config) {
        String actionName = config.getActionName();
        if (ActionProvider.COMMAND_PROFILE_TEST_SINGLE.equals(actionName)) {
            StartupExtender.StartMode mode = StartupExtender.StartMode.TEST_PROFILE;
            for (Map.Entry<? extends String, ? extends String> entry : config.getProperties().entrySet()) {
                if (entry.getKey().equals("exec.args")) {
                    List<String> args = new ArrayList<String>();
                    InstanceContent ic = new InstanceContent();
                    Project p = config.getProject();
                    if (p != null) {
                        ic.add(p);
                        ActiveJ2SEPlatformProvider pp = p.getLookup().lookup(ActiveJ2SEPlatformProvider.class);
                        if (pp != null) {
                            ic.add(pp.getJavaPlatform());
                        }
                    }
                    for (StartupExtender group : StartupExtender.getExtenders(new AbstractLookup(ic), mode)) {
                        args.addAll(group.getArguments());
                    }
                    if (!args.isEmpty()) {
                        StringBuilder b = new StringBuilder();
                        for (String arg : args) {
                            b.append(arg).append(' ');
                        }
                        b.append(entry.getValue());
                        config.setProperty(entry.getKey(), b.toString());
                    }
                }
            }            
        }    

        // To be able to skip standard run behavior we need to set this property
        // with respect to the current CoS/DoS setting --> See issue 230565
        Boolean alwaysBuild = (Boolean) config.getProject().getProjectDirectory().getAttribute(PROP_ALWAYS_BUILD_BEFORE_RUNNING);
        if (alwaysBuild == null) {
            alwaysBuild = Boolean.FALSE;
        }

        // When profiling, do not skip build --> See issue #241464
        if (DeploymentHelper.isProfileMode(config)) {
            alwaysBuild = Boolean.TRUE;
        }

        Boolean standardExecution = Boolean.FALSE;
        // Perform standard execution when running single main file --> See issue #241703
        if ("run.single.main".equals(actionName)) { // NOI18N
            standardExecution = Boolean.TRUE;
        }

        config.setInternalProperty(ExecutionConstants.SKIP_BUILD, !alwaysBuild); //NOI18N
        config.setInternalProperty(ExecutionConstants.STANDARD_EXECUTION, standardExecution);

        if (!applicableActions.contains(actionName)) {
            return true;
        }

        // Checking if the Servlet URI is set --> See issue #227324
        if (SINGLE_ACTIONS.contains(actionName)) {
            String urlPath = config.getProperties().get(ExecutionChecker.CLIENTURLPART);
            if (urlPath == null || "".equals(urlPath)) {
                return false;
            }
        }

        J2eeModuleProvider provider = config.getProject().getLookup().lookup(J2eeModuleProvider.class);
        if (provider != null) {
            checkWarInplace(config, provider);
        }
        return true;
    }

    private void checkWarInplace(RunConfig config, J2eeModuleProvider provider) {
        if (provider instanceof WebModuleProviderImpl) {
            Iterator it = config.getGoals().iterator();
            boolean inplace = false;
            while (it.hasNext()) {
                String goal = (String) it.next();
                if (goal.indexOf(":inplace") > -1) { //NOI18N
                    inplace = true;
                    break;
                }
            }
            ((WebModuleProviderImpl) provider).getModuleImpl().setWarInplace(inplace);
        }
    }

    @Override
    public boolean checkRunConfig(RunConfig config, ExecutionContext con) {
        String actionName = config.getActionName();
        if (!(ActionProvider.COMMAND_CLEAN.equals(actionName) || ActionProvider.COMMAND_REBUILD.equals(actionName))) {
            return true;
        }
        J2eeModuleProvider provider = config.getProject().getLookup().lookup(J2eeModuleProvider.class);
        if (provider != null) {
            String serverID = provider.getServerID();
            String serverInstanceID = provider.getServerInstanceID();

            if (serverID == null ||
                serverInstanceID == null ||
                ExecutionChecker.DEV_NULL.equals(serverID) ||
                ExecutionChecker.DEV_NULL.equals(serverInstanceID)) {

                return true;
            }
            try {
                Deployment.getDefault ().undeploy(provider, false, new DeploymentLogger(con.getInputOutput().getOut()));
            } catch (DeploymentException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return true;
    }
    
}
