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

package org.netbeans.modules.maven.execute;

import java.util.Map;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.execute.model.NetbeansActionMapping;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * run configuration backed up by model
 * @author mkleint
 */
public final class ModelRunConfig extends BeanRunConfig {
    
    private final NetbeansActionMapping model;
    private final boolean fallback;
    
    public ModelRunConfig(Project proj, NetbeansActionMapping mod, String actionName, FileObject selectedFile, Lookup lookup, boolean fallback) {
        model = mod;
        this.fallback = fallback;
        NbMavenProjectImpl nbprj = proj.getLookup().lookup(NbMavenProjectImpl.class);
        setProject(nbprj);
        String label = ProjectUtils.getInformation(proj).getDisplayName();
        setExecutionName(label);
        setTaskDisplayName(label);
        for (Map.Entry<String,String> entry : model.getProperties().entrySet()) {
            setProperty(entry.getKey(), entry.getValue());
        }
        setGoals(model.getGoals());
        setExecutionDirectory(ActionToGoalUtils.resolveProjectExecutionBasedir(mod, proj));
        setRecursive(mod.isRecursive());
        setActivatedProfiles(mod.getActivatedProfiles());
        setActionName(actionName);
        setFileObject(selectedFile);
        if (mod.getPreAction() != null) {
            setPreExecution(ActionToGoalUtils.createRunConfig(mod.getPreAction(), nbprj, lookup));
        }
        String react = mod.getReactor();
        if (react != null) {
            if ("am".equals(react) || "also-make".equals(react)) {
                setReactorStyle(ReactorStyle.ALSO_MAKE);
            } else if ("amd".equals(react) || "also-make-dependents".equals(react)) {
                setReactorStyle(ReactorStyle.ALSO_MAKE_DEPENDENTS);
            }
        }
    }

    public boolean isFallback() {
        return fallback;
    }
}
