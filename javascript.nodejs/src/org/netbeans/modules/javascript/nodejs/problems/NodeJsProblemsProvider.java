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
package org.netbeans.modules.javascript.nodejs.problems;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.javascript.nodejs.exec.NodeExecutable;
import org.netbeans.modules.javascript.nodejs.options.NodeJsOptions;
import org.netbeans.modules.javascript.nodejs.options.NodeJsOptionsValidator;
import org.netbeans.modules.javascript.nodejs.platform.NodeJsSupport;
import org.netbeans.modules.javascript.nodejs.preferences.NodeJsPreferences;
import org.netbeans.modules.javascript.nodejs.preferences.NodeJsPreferencesValidator;
import org.netbeans.modules.javascript.nodejs.util.NodeJsUtils;
import org.netbeans.modules.javascript.nodejs.util.ValidationUtils;
import org.netbeans.modules.web.clientproject.api.WebClientProjectConstants;
import org.netbeans.modules.web.common.api.ValidationResult;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.ProjectProblemsProvider;
import org.netbeans.spi.project.ui.support.ProjectProblemsProviderSupport;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

@ProjectServiceProvider(service = ProjectProblemsProvider.class, projectType = "org-netbeans-modules-web-clientproject") // NOI18N
public final class NodeJsProblemsProvider implements ProjectProblemsProvider {

    private final ProjectProblemsProviderSupport problemsProviderSupport = new ProjectProblemsProviderSupport(this);
    private final Project project;
    private final PreferenceChangeListener optionsListener = new OptionsListener();
    private final PreferenceChangeListener preferencesListener = new PreferencesListener();
    private final ChangeListener projectSourcesListener = new ProjectSourcesListener();
    private final FileChangeListener nodeSourcesListener = new NodeSourcesListener();

    // @GuardedBy("this")
    private NodeJsSupport nodeJsSupport;


    public NodeJsProblemsProvider(Project project) {
        assert project != null;
        this.project = project;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        problemsProviderSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        problemsProviderSupport.removePropertyChangeListener(listener);
    }

    @NbBundle.Messages({
        "# {0} - message",
        "NodeJsProblemProvider.error=Node.js: {0}",
    })
    @Override
    public Collection<? extends ProjectProblem> getProblems() {
        return problemsProviderSupport.getProblems(new ProjectProblemsProviderSupport.ProblemsCollector() {
            @Override
            public Collection<? extends ProjectProblem> collectProblems() {
                if (!getNodeJsSupport().getPreferences().isEnabled()) {
                    return Collections.emptyList();
                }
                Collection<ProjectProblemsProvider.ProjectProblem> currentProblems = new ArrayList<>();
                checkProjectSources(currentProblems);
                checkOptions(currentProblems);
                checkPreferences(currentProblems);
                checkNodeSources(currentProblems);
                return currentProblems;
            }
        });
    }

    synchronized NodeJsSupport getNodeJsSupport() {
        if (nodeJsSupport == null) {
            nodeJsSupport = NodeJsSupport.forProject(project);
            addListeners();
        }
        return nodeJsSupport;
    }

    private void addListeners() {
        assert nodeJsSupport != null;
        // preferences
        nodeJsSupport.getPreferences().addPreferenceChangeListener(preferencesListener);
        // options
        NodeJsOptions options = NodeJsOptions.getInstance();
        options.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, optionsListener, options));
        // project sources
        Sources sources = ProjectUtils.getSources(project);
        sources.addChangeListener(WeakListeners.change(projectSourcesListener, sources));
        // node sources
        FileUtil.addFileChangeListener(nodeSourcesListener, NodeJsUtils.getNodeSources());
    }

    @NbBundle.Messages({
        "NodeJsProblemProvider.sources.none.title=No Source folder defined",
        "# {0} - project name",
        "NodeJsProblemProvider.sources.none.description=Node.js runs JavaScript files underneath Source folder. But no Source folder is defined in project {0}.",
    })
    void checkProjectSources(Collection<ProjectProblem> currentProblems) {
        if (NodeJsUtils.getSourceRoots(project).isEmpty()) {
            ProjectProblem problem = ProjectProblem.createWarning(
                    Bundle.NodeJsProblemProvider_sources_none_title(),
                    Bundle.NodeJsProblemProvider_sources_none_description(NodeJsUtils.getProjectDisplayName(project)),
                    new CustomizerProblemResolver(project, "NO_SOURCES", WebClientProjectConstants.CUSTOMIZER_SOURCES_IDENT)); // NOI18N
            currentProblems.add(problem);
        }
    }

    void checkOptions(Collection<ProjectProblem> currentProblems) {
        NodeJsPreferences preferences = getNodeJsSupport().getPreferences();
        assert preferences.isEnabled() : project.getProjectDirectory().getNameExt();
        ValidationResult validationResult = new NodeJsOptionsValidator()
                .validate(preferences.isDefaultNode(), false)
                .getResult();
        if (validationResult.isFaultless()) {
            return;
        }
        String message = validationResult.getFirstErrorMessage();
        if (message == null) {
            message = validationResult.getFirstWarningMessage();
        }
        assert message != null : "Message should be found for invalid options";
        message = Bundle.NodeJsProblemProvider_error(message);
        ProjectProblem problem = ProjectProblem.createError(
                message,
                message,
                new OptionsProblemResolver());
        currentProblems.add(problem);
    }

    void checkPreferences(Collection<ProjectProblem> currentProblems) {
        ValidationResult validationResult = new NodeJsPreferencesValidator()
                .validate(project, false)
                .getResult();
        if (validationResult.isFaultless()) {
            return;
        }
        String message = validationResult.getFirstErrorMessage();
        if (message == null) {
            message = validationResult.getFirstWarningMessage();
        }
        assert message != null : "Message should be found for invalid preferences: " + project.getProjectDirectory().getNameExt();
        message = Bundle.NodeJsProblemProvider_error(message);
        ProjectProblem problem = ProjectProblem.createError(
                message,
                message,
                new CustomizerProblemResolver(project, "INVALID_PREFERENCES", validationResult)); // NOI18N
        currentProblems.add(problem);
    }

    @NbBundle.Messages("NodeJsProblemProvider.node.sources=Missing node.js sources")
    void checkNodeSources(Collection<ProjectProblem> currentProblems) {
        final NodeExecutable node = NodeExecutable.forProject(project, false);
        if (node == null) {
            // already handled
            return;
        }
        if (EventQueue.isDispatchThread()
                && !node.versionDetected()) {
            // avoid ui flickering
            RequestProcessor.getDefault().post(new Runnable() {
                @Override
                public void run() {
                    node.getVersion();
                    fireProblemsChanged();
                }
            });
            return;
        }
        File nodeSources = NodeJsUtils.getNodeSources(project);
        if (nodeSources == null
                || !nodeSources.isDirectory()) {
            // no or incorrect sources
            String message = Bundle.NodeJsProblemProvider_error(Bundle.NodeJsProblemProvider_node_sources());
            ProjectProblem problem = ProjectProblem.createError(
                    message,
                    message,
                    new NodeSourcesProblemResolver(project));
            currentProblems.add(problem);
            return;
        }
        ValidationResult result = new ValidationResult();
        ValidationUtils.validateNodeSources(result, nodeSources.getAbsolutePath());
        if (!result.isFaultless()) {
            String message = result.getFirstErrorMessage();
            if (message == null) {
                message = result.getFirstWarningMessage();
            }
            assert message != null : result;
            ProjectProblem problem = ProjectProblem.createWarning(
                    message,
                    message,
                    new NodeSourcesProblemResolver(project));
            currentProblems.add(problem);
        }
    }

    void fireProblemsChanged() {
        problemsProviderSupport.fireProblemsChange();
    }

    //~ Inner classes

    private final class OptionsListener implements PreferenceChangeListener {

        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
            fireProblemsChanged();
        }

    }

    private final class PreferencesListener implements PreferenceChangeListener {

        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
            fireProblemsChanged();
        }

    }

    private final class ProjectSourcesListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            fireProblemsChanged();
        }

    }

    private final class NodeSourcesListener extends FileChangeAdapter {

        @Override
        public void fileFolderCreated(FileEvent fe) {
            fireProblemsChanged();
        }

    }

}
