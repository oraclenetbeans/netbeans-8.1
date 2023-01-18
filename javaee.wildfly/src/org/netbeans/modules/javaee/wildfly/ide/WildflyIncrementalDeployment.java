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
package org.netbeans.modules.javaee.wildfly.ide;

import java.io.File;
import java.io.IOException;
import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.api.AppChangeDescriptor;
import org.netbeans.modules.j2ee.deployment.plugins.api.DeploymentChangeDescriptor;
import org.netbeans.modules.j2ee.deployment.plugins.spi.DeploymentContext;
import org.netbeans.modules.j2ee.deployment.plugins.spi.IncrementalDeployment;
import org.netbeans.modules.j2ee.deployment.plugins.spi.IncrementalDeployment2;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfiguration;
import org.netbeans.modules.javaee.wildfly.WildflyDeploymentManager;
import org.netbeans.modules.javaee.wildfly.ide.commands.WildflyExplodedDeployer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Emmanuel Hugonnet (ehsavoie) <ehsavoie@netbeans.org>
 */
public class WildflyIncrementalDeployment extends IncrementalDeployment implements IncrementalDeployment2 {

    private final WildflyDeploymentManager dm;
    private final File deploymentDir;
    private final WildflyExplodedDeployer deployer;

    public WildflyIncrementalDeployment(WildflyDeploymentManager dm) {
        this.dm = dm;
        deployer = new WildflyExplodedDeployer(dm);
        String dir;
        try {
            dir = dm.getClient().getDeploymentDirectory();
        } catch (IOException ex) {
            dir = null;
        }
        if (dir != null && !dir.isEmpty()) {
            this.deploymentDir = new File(dir);
        } else {
            this.deploymentDir = null;
        }
    }

    @Override
    public ProgressObject initialDeploy(Target target, J2eeModule app, ModuleConfiguration configuration, File dir) {
        return deployer.deploy(target, app.getType(), dir);
    }

    @Override
    public boolean canFileDeploy(Target target, J2eeModule deployable) {
        return deployable != null && !J2eeModule.Type.CAR.equals(deployable.getType())
                && !J2eeModule.Type.RAR.equals(deployable.getType()) && this.deploymentDir != null && this.deploymentDir.exists()
                && this.deploymentDir.isDirectory();
    }

    @Override
    public File getDirectoryForNewApplication(Target target, J2eeModule app, ModuleConfiguration configuration) {
        String baseName = app.getUrl();
        if(baseName.indexOf(File.separatorChar) >= 0) {
            baseName = baseName.substring(baseName.lastIndexOf(File.separatorChar) +1, baseName.length());
        }
        String extension = getExtension(app.getType());
        try {
            if (app.getArchive() != null) {
                if (baseName.isEmpty()) {
                    baseName = app.getArchive().getNameExt();
                }
                extension = '.' + app.getArchive().getExt();
            }
            baseName = addExtension(baseName, extension);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new File(this.deploymentDir, baseName);
    }

    private String addExtension(final String baseName, final String extension) {
        if (!baseName.endsWith(extension)) {
            return baseName + extension;
        }
        return baseName;
    }

    private String getExtension(final J2eeModule.Type type) {
        if (J2eeModule.Type.WAR == type) {
            return ".war";
        }
        if (J2eeModule.Type.RAR == type) {
            return ".rar";
        }
        if (J2eeModule.Type.EJB == type) {
            return ".jar";
        }
        if (J2eeModule.Type.EAR == type) {
            return ".ear";
        }
        return ".jar";
    }

    @Override
    public File getDirectoryForNewModule(File appDir, String uri, J2eeModule module, ModuleConfiguration configuration) {
        try {
            if(appDir != null && appDir.exists() && appDir.isDirectory()) {
                return new File(appDir, module.getArchive().getNameExt());
            }
            return new File(this.deploymentDir, module.getArchive().getNameExt());
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public File getDirectoryForModule(TargetModuleID module) {
        return new File(this.deploymentDir, appendModuleToPath("", module));
    }

    private String appendModuleToPath(String path, TargetModuleID module) {
        String relPath = module.getModuleID() + File.separatorChar + path;
        if(module.getParentTargetModuleID() == null) {
            return relPath;
        }
        return appendModuleToPath(path, module.getParentTargetModuleID());
    }

    @Override
    public ProgressObject initialDeploy(Target target, DeploymentContext context) {
        return initialDeploy(target, context.getModule(), null, context.getModuleFile());
    }

    @Override
    public ProgressObject deployOnSave(TargetModuleID module, DeploymentChangeDescriptor desc) {
        File moduleFile;
        try {
            moduleFile = new File(dm.getClient().getDeploymentDirectory(), module.getModuleID());
            return deployer.redeploy(module, moduleFile);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;

    }

    @Override
    public ProgressObject incrementalDeploy(TargetModuleID module, AppChangeDescriptor changes) {
        boolean redeploy = changes.classesChanged() || changes.descriptorChanged()
                || changes.ejbsChanged() || changes.manifestChanged() || changes.serverDescriptorChanged();
        if (changes instanceof DeploymentChangeDescriptor) {
            DeploymentChangeDescriptor deploymentChanges = (DeploymentChangeDescriptor) changes;
            redeploy = redeploy || deploymentChanges.serverResourcesChanged();
        }

        if (!redeploy) {
            WildflyProgressObject progress = new WildflyProgressObject(module);
            progress.fireProgressEvent(module, new WildflyDeploymentStatus(
                                    ActionType.EXECUTE, CommandType.DISTRIBUTE, StateType.COMPLETED,
                                    NbBundle.getMessage(WildflyIncrementalDeployment.class, "MSG_Deployment_Completed")));
            return progress;
        }
        return null;
    }

    @Override
    public boolean isDeployOnSaveSupported() {
        return true;
    }

    @Override
    public ProgressObject incrementalDeploy(TargetModuleID module, DeploymentContext context) {
        return dm.redeploy(new TargetModuleID[]{module}, context);
    }

}
