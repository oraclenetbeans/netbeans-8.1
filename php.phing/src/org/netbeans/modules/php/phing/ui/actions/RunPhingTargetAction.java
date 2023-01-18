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
package org.netbeans.modules.php.phing.ui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.phing.PhingBuildTool;
import org.netbeans.modules.php.phing.exec.PhingExecutable;
import org.netbeans.modules.php.phing.file.PhingTargets;
import org.netbeans.modules.php.phing.ui.options.PhingOptionsPanelController;
import org.netbeans.modules.php.phing.util.PhingUtils;
import org.netbeans.modules.web.clientproject.api.build.BuildTools;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

@ActionID(id = "org.netbeans.modules.php.phing.ui.actions.RunPhingTargetAction", category = "Build")
@ActionRegistration(displayName = "#RunPhingTargetAction.name", lazy = false)
@ActionReferences({
    @ActionReference(path = "Loaders/text/x-ant+xml/Actions", position = 150),
    // XXX cannot be added here since empty space is shown if action is disabled :/
    //@ActionReference(path = "Editors/text/x-ant+xml/Popup", position = 850),
    @ActionReference(path = "Projects/org-netbeans-modules-php-project/Actions", position = 120),
})
@NbBundle.Messages("RunPhingTargetAction.name=Phing Targets")
public final class RunPhingTargetAction extends AbstractAction implements ContextAwareAction, Presenter.Popup {

    static final Logger LOGGER = Logger.getLogger(RunPhingTargetAction.class.getName());

    private final Project project;


    public RunPhingTargetAction() {
        this(null);
    }

    public RunPhingTargetAction(Project project) {
        this.project = project;
        setEnabled(project != null);
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        // hide this action in IDE Options > Keymap
        putValue(Action.NAME, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        assert false;
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        Project contextProject = context.lookup(Project.class);
        if (contextProject != null) {
            // project action
            return createAction(contextProject);
        }
        // file action?
        FileObject fileObject = context.lookup(FileObject.class);
        if (fileObject == null) {
            DataObject dataObject = context.lookup(DataObject.class);
            if (dataObject != null) {
                fileObject = dataObject.getPrimaryFile();
            }
        }
        if (fileObject != null) {
            contextProject = FileOwnerQuery.getOwner(fileObject);
            if (contextProject == null) {
                return this;
            }
            if (contextProject.getProjectDirectory().equals(fileObject.getParent())) {
                // build.xml directly in project dir
                return createAction(contextProject);
            }
        }
        return this;
    }

    private Action createAction(Project contextProject) {
        assert contextProject != null;
        PhingBuildTool phingBuildTool = PhingBuildTool.inProject(contextProject);
        if (phingBuildTool == null) {
            return this;
        }
        if (!phingBuildTool.isEnabled()) {
            return this;
        }
        return new RunPhingTargetAction(contextProject);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (project == null) {
            return new Actions.MenuItem(this, false);
        }
        return BuildTools.getDefault().createTasksMenu(new TasksMenuSupportImpl(project));
    }

    //~ Inner classes

    private static final class TasksMenuSupportImpl implements BuildTools.TasksMenuSupport {

        private final Project project;
        private final PhingTargets phingTargets;


        public TasksMenuSupportImpl(Project project) {
            assert project != null;
            this.project = project;
            phingTargets = PhingBuildTool.forProject(project).getPhingTargets();
        }

        @Override
        public Project getProject() {
            return project;
        }

        @Override
        public String getIdentifier() {
            return PhingBuildTool.IDENTIFIER;
        }

        @NbBundle.Messages({
            "TasksMenuSupportImpl.targets.label=&Target(s)",
            "TasksMenuSupportImpl.targets.loading=Loading Targets...",
            "TasksMenuSupportImpl.targets.reload=Reload Targets",
            "TasksMenuSupportImpl.targets.run.advanced=Run Target(s)",
            "TasksMenuSupportImpl.phing.configure=Configure Phing...",
        })
        @Override
        public String getTitle(BuildTools.TasksMenuSupport.Title title) {
            switch (title) {
                case MENU:
                    return Bundle.RunPhingTargetAction_name();
                case LOADING_TASKS:
                    return Bundle.TasksMenuSupportImpl_targets_loading();
                case RELOAD_TASKS:
                    return Bundle.TasksMenuSupportImpl_targets_reload();
                case CONFIGURE_TOOL:
                    return Bundle.TasksMenuSupportImpl_phing_configure();
                case RUN_ADVANCED:
                    return Bundle.TasksMenuSupportImpl_targets_run_advanced();
                case TASKS_LABEL:
                    return Bundle.TasksMenuSupportImpl_targets_label();
                case BUILD_TOOL_EXEC:
                    return PhingExecutable.PHING_NAME;
                default:
                    assert false : "Unknown title: " + title;
            }
            return null;
        }

        @Override
        public String getDefaultTaskName() {
            return PhingTargets.DEFAULT_TARGET;
        }

        @Override
        public Future<List<String>> getTasks() {
            return new TargetsFuture(phingTargets);
        }

        @Override
        public void runTask(String... args) {
            assert !EventQueue.isDispatchThread();
            PhingExecutable phing = PhingExecutable.getDefault(project, true);
            if (phing != null) {
                PhingUtils.logUsagePhingBuild();
                phing.run(args);
            }
        }

        @Override
        public void reloadTasks() {
            assert !EventQueue.isDispatchThread();
            phingTargets.reset();
            try {
                phingTargets.loadTargets(null, null);
            } catch (ExecutionException | TimeoutException ex) {
                LOGGER.log(Level.INFO, null, ex);
            }
        }

        @Override
        public void configure() {
            OptionsDisplayer.getDefault().open(PhingOptionsPanelController.OPTIONS_PATH);
        }

    }

    private static final class TargetsFuture implements Future<List<String>> {

        private final PhingTargets phingTargets;


        public TargetsFuture(PhingTargets phingTargets) {
            assert phingTargets != null;
            this.phingTargets = phingTargets;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return phingTargets.getTargets() != null;
        }

        @Override
        public List<String> get() throws InterruptedException, ExecutionException {
            try {
                return phingTargets.loadTargets(null, null);
            } catch (TimeoutException ex) {
                assert false;
            }
            return null;
        }

        @Override
        public List<String> get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return phingTargets.loadTargets(timeout, unit);
        }

    }

}
