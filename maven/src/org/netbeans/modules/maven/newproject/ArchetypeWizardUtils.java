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

package org.netbeans.modules.maven.newproject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.maven.repository.RepositorySystem;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.maven.api.ModelUtils;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.archetype.Archetype;
import org.netbeans.modules.maven.api.archetype.ProjectInfo;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.netbeans.modules.maven.execute.BeanRunConfig;
import org.netbeans.modules.maven.model.ModelOperation;
import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.POMModel;
import static org.netbeans.modules.maven.newproject.Bundle.*;
import org.netbeans.modules.maven.options.MavenCommandSettings;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

/**
 * @author mkleint
 */
public class ArchetypeWizardUtils {

    /** {@code Map<String,String>} of custom archetype properties to define. */
    public static final String ADDITIONAL_PROPS = "additionalProps"; // NOI18N

    private static final Logger LOG = Logger.getLogger(ArchetypeWizardUtils.class.getName());

    private ArchetypeWizardUtils() {
    }

    @Messages({
        "RUN_Project_Creation=Project Creation",
        "RUN_Maven=Create project"
    })
    private static void runArchetype(File directory, ProjectInfo vi, Archetype arch, @NullAllowed Map<String,String> additional) throws IOException {
        BeanRunConfig config = new BeanRunConfig();
        config.setProperty("archetypeGroupId", arch.getGroupId()); //NOI18N
        config.setProperty("archetypeArtifactId", arch.getArtifactId()); //NOI18N
        config.setProperty("archetypeVersion", arch.getVersion()); //NOI18N
        String repo = arch.getRepository();
        config.setProperty("archetypeRepository", repo != null ? repo : RepositorySystem.DEFAULT_REMOTE_REPO_URL); //NOI18N
        config.setProperty("groupId", vi.groupId); //NOI18N
        config.setProperty("artifactId", vi.artifactId); //NOI18N
        config.setProperty("version", vi.version); //NOI18N
        final String pack = vi.packageName;
        if (pack != null && pack.trim().length() > 0) {
            config.setProperty("package", pack); //NOI18N
        }
        config.setProperty("basedir", directory.getAbsolutePath());//NOI18N
        
        Map<String, String> baseprops = new HashMap<String, String>(config.getProperties());
        
        if (additional != null) {
            for (Map.Entry<String,String> entry : additional.entrySet()) {
                if (baseprops.containsKey(entry.getKey())) {
                    //don't let the additional props overwrite the values for version, groupId or artifactId
                    continue;
                }
                String val = entry.getValue();
                //#208146 process the additional prop value through a simplistic extression resolution.
                for (Map.Entry<String, String> basePropEnt : baseprops.entrySet()) {
                    val = val.replace("${" + basePropEnt.getKey() + "}", basePropEnt.getValue());
                }
                config.setProperty(entry.getKey(), val);
            }
        }
        config.setActivatedProfiles(Collections.<String>emptyList());
        config.setExecutionDirectory(directory);
        config.setExecutionName(RUN_Project_Creation());
        config.setGoals(Collections.singletonList(MavenCommandSettings.getDefault().getCommand(MavenCommandSettings.COMMAND_CREATE_ARCHETYPENG))); //NOI18N

        //ExecutionRequest.setInteractive seems to have no influence on archetype plugin.
        config.setInteractive(false);
        config.setProperty("archetype.interactive", "false");//NOI18N
        //#136853 make sure to get the latest snapshot always..
        if (arch.getVersion().contains("SNAPSHOT")) { //NOI18N
            config.setUpdateSnapshots(true);
        }

        config.setTaskDisplayName(RUN_Maven());
        ExecutorTask task = RunUtils.executeMaven(config); //NOI18N
        task.result();
    }
    
   

    /**
     * Instantiates archetype stored in given wizard descriptor.
     */
    static Set<FileObject> instantiate(WizardDescriptor wiz) throws IOException {
        ProjectInfo vi = new ProjectInfo((String) wiz.getProperty("groupId"), (String) wiz.getProperty("artifactId"), (String) wiz.getProperty("version"), (String) wiz.getProperty("package")); //NOI18N

        Archetype arch = (Archetype) wiz.getProperty("archetype"); //NOI18N
        logUsage(arch.getGroupId(), arch.getArtifactId(), arch.getVersion());

        @SuppressWarnings("unchecked")
        Map<String,String> additional = (Map<String,String>) wiz.getProperty(ADDITIONAL_PROPS);

        File projFile = FileUtil.normalizeFile((File) wiz.getProperty(CommonProjectActions.PROJECT_PARENT_FOLDER)); // NOI18N
        createFromArchetype(projFile, vi, arch, additional, true);
        Set<FileObject> projects = openProjects(projFile, null);
        return projects;
    }

    private static final String loggerName = "org.netbeans.ui.metrics.maven"; // NOI18N
    private static final String loggerKey = "USG_PROJECT_CREATE_MAVEN"; // NOI18N

    // http://wiki.netbeans.org/UsageLoggingSpecification
    public static void logUsage(String groupId, String artifactId, String version) {
        LogRecord logRecord = new LogRecord(Level.INFO, loggerKey);
        logRecord.setLoggerName(loggerName);
        logRecord.setParameters(new Object[] {groupId + ":" + artifactId + ":" + version}); // NOI18N
        Logger.getLogger(loggerName).log(logRecord);
    }
    
    public static void createFromArchetype(File projDir, ProjectInfo vi, Archetype arch, @NullAllowed Map<String,String> additional, boolean updateLastUsedProjectDir) throws IOException {
        final File parent = projDir.getParentFile();
        if (parent == null) {
            throw new IOException("no parent of " + projDir);
        }
        if (updateLastUsedProjectDir && parent != null && parent.exists()) {
            ProjectChooser.setProjectsFolder(parent);
        }
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("could not create " + parent);
        }
        runArchetype(parent, vi, arch, additional);
    }

    public static Set<FileObject> openProjects(File dirF, File mainProjectDir) throws IOException {
        FileObject fDir = FileUtil.toFileObject(dirF);
        if (fDir == null) {
            return Collections.emptySet();
        }
        FileObject mainFO = mainProjectDir != null ? FileUtil.toFileObject(mainProjectDir) : null;
        return openProjects(fDir, mainFO);
    }
    static Set<FileObject> openProjects(FileObject fDir, FileObject mainFO) throws IOException {
        List<FileObject> resultList = new ArrayList<>();

        // the archetype generation didn't fail.
        resultList.add(fDir);
        processProjectFolder(fDir);
        collectProjects(fDir, mainFO, resultList);
        return new LinkedHashSet<>(resultList);
    }

    private static void collectProjects(FileObject fDir, FileObject mainFO, List<FileObject> resultList) {
        for (FileObject subfolder : fDir.getChildren()) {
            if (!subfolder.isFolder()) {
                continue;
            }
            if (ProjectManager.getDefault().isProject(subfolder)) {
                if (subfolder.equals(mainFO)) {
                    resultList.add(0, subfolder);
                } else {
                    resultList.add(subfolder);
                }
                processProjectFolder(subfolder);
                collectProjects(subfolder, mainFO, resultList);
            }
        }
    }

    private static void processProjectFolder(final FileObject fo) {
        try {
            Project prj = ProjectManager.getDefault().findProject(fo);
            if (prj == null) { //#143596
                return;
            }
            final NbMavenProject watch = prj.getLookup().lookup(NbMavenProject.class);
            if (watch != null) {
                watch.downloadDependencyAndJavadocSource(false);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static class AddDependencyOperation implements ModelOperation<POMModel> {
        private final String group;
        private final String artifact;
        private final String version;
        private final String type;

        public AddDependencyOperation(ProjectInfo info, String type) {
            this.group = info.groupId;
            this.artifact = info.artifactId;
            this.version = info.version;
            this.type = type;
        }

        @Override
        public void performOperation(POMModel model) {
            Dependency dep = ModelUtils.checkModelDependency(model, group, artifact, true);
            dep.setVersion(version);
            dep.setType(type);
        }
    }
}
