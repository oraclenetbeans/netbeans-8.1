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
package org.netbeans.modules.javascript.grunt.exec;

import java.awt.EventQueue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.project.Project;
import org.netbeans.modules.javascript.grunt.GruntBuildTool;
import org.netbeans.modules.javascript.grunt.file.Gruntfile;
import org.netbeans.modules.javascript.grunt.options.GruntOptions;
import org.netbeans.modules.javascript.grunt.options.GruntOptionsValidator;
import org.netbeans.modules.javascript.grunt.ui.options.GruntOptionsPanelController;
import org.netbeans.modules.javascript.grunt.util.FileUtils;
import org.netbeans.modules.javascript.grunt.util.GruntUtils;
import org.netbeans.modules.web.clientproject.api.util.StringUtilities;
import org.netbeans.modules.web.common.api.ExternalExecutable;
import org.netbeans.modules.web.common.api.ValidationResult;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.InputOutput;

public class GruntExecutable {

    static final Logger LOGGER = Logger.getLogger(GruntExecutable.class.getName());

    public static final String GRUNT_NAME;

    private static final String HELP_PARAM = "--help"; // NOI18N
    private static final String NO_COLOR_PARAM = "--no-color"; // NOI18N

    protected final Project project;
    protected final String gruntPath;


    static {
        if (Utilities.isWindows()) {
            GRUNT_NAME = "grunt.cmd"; // NOI18N
        } else {
            GRUNT_NAME = "grunt"; // NOI18N
        }
    }


    GruntExecutable(String gruntPath, @NullAllowed Project project) {
        assert gruntPath != null;
        this.gruntPath = gruntPath;
        this.project = project;
    }

    @CheckForNull
    public static GruntExecutable getDefault(@NullAllowed Project project, boolean showOptions) {
        ValidationResult result = new GruntOptionsValidator()
                .validateGrunt()
                .getResult();
        if (validateResult(result) != null) {
            if (showOptions) {
                OptionsDisplayer.getDefault().open(GruntOptionsPanelController.OPTIONS_PATH);
            }
            return null;
        }
        return createExecutable(GruntOptions.getInstance().getGrunt(), project);
    }

    private static GruntExecutable createExecutable(String grunt, Project project) {
        if (Utilities.isMac()) {
            return new MacGruntExecutable(grunt, project);
        }
        return new GruntExecutable(grunt, project);
    }

    String getCommand() {
        return gruntPath;
    }

    @NbBundle.Messages({
        "# {0} - project name",
        "GruntExecutable.run=Grunt ({0})",
    })
    public Future<Integer> run(String... args) {
        assert !EventQueue.isDispatchThread();
        assert project != null;
        String projectName = GruntUtils.getProjectDisplayName(project);
        Future<Integer> task = getExecutable(Bundle.GruntExecutable_run(projectName))
                .additionalParameters(getRunParams(args))
                .run(getDescriptor());
        assert task != null : gruntPath;
        return task;
    }

    public Future<List<String>> listTasks() {
        final GruntTasksLineProcessor gruntTasksLineProcessor = new GruntTasksLineProcessor();
        Future<Integer> task = getExecutable("list grunt tasks") // NOI18N
                .noInfo(true)
                .additionalParameters(Arrays.asList(NO_COLOR_PARAM, HELP_PARAM))
                .redirectErrorStream(false)
                .run(getSilentDescriptor(), new ExecutionDescriptor.InputProcessorFactory2() {
                    @Override
                    public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                        return InputProcessors.bridge(gruntTasksLineProcessor);
                    }
                });
        assert task != null : gruntPath;
        return new TaskList(task, gruntTasksLineProcessor);
    }

    private ExternalExecutable getExecutable(String title) {
        assert title != null;
        return new ExternalExecutable(getCommand())
                .workDir(getWorkDir())
                .displayName(title)
                .optionsPath(GruntOptionsPanelController.OPTIONS_PATH)
                .noOutput(false);
    }

    private ExecutionDescriptor getDescriptor() {
        assert project != null;
        return ExternalExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                .showSuspended(true)
                .optionsPath(GruntOptionsPanelController.OPTIONS_PATH)
                .outLineBased(true)
                .errLineBased(true)
                .postExecution(new Runnable() {
                    @Override
                    public void run() {
                        // #246886
                        FileUtil.refreshFor(getWorkDir());
                    }
                });
    }

    private static ExecutionDescriptor getSilentDescriptor() {
        return new ExecutionDescriptor()
                .inputOutput(InputOutput.NULL)
                .inputVisible(false)
                .frontWindow(false)
                .showProgress(false)
                .charset(StandardCharsets.UTF_8)
                .outLineBased(true);
    }

    private File getWorkDir() {
        if (project == null) {
            return FileUtils.TMP_DIR;
        }
        Gruntfile gruntfile = GruntBuildTool.forProject(project).getGruntfile();
        if (gruntfile.exists()) {
            return gruntfile.getFile().getParentFile();
        }
        File workDir = FileUtil.toFile(project.getProjectDirectory());
        assert workDir != null : project.getProjectDirectory();
        return workDir;
    }

    private List<String> getRunParams(String... args) {
        return getParams(Arrays.asList(args));
    }

    List<String> getParams(List<String> params) {
        assert params != null;
        return params;
    }

    @CheckForNull
    private static String validateResult(ValidationResult result) {
        if (result.isFaultless()) {
            return null;
        }
        if (result.hasErrors()) {
            return result.getFirstErrorMessage();
        }
        return result.getFirstWarningMessage();
    }

    //~ Inner classes

    private static final class MacGruntExecutable extends GruntExecutable {

        private static final String BASH_COMMAND = "/bin/bash -lc"; // NOI18N


        MacGruntExecutable(String gruntPath, Project project) {
            super(gruntPath, project);
        }

        @Override
        String getCommand() {
            return BASH_COMMAND;
        }

        @Override
        List<String> getParams(List<String> params) {
            StringBuilder sb = new StringBuilder(200);
            sb.append("\""); // NOI18N
            sb.append(gruntPath);
            sb.append("\" \""); // NOI18N
            sb.append(StringUtilities.implode(super.getParams(params), "\" \"")); // NOI18N
            sb.append("\""); // NOI18N
            return Collections.singletonList(sb.toString());
        }

    }

    static final class GruntTasksLineProcessor implements LineProcessor {

        private static final String AVAILABLE_TASKS = "Available tasks"; // NOI18N
        private static final String NO_TASKS = "(no tasks found)"; // NOI18N

        final List<String> tasks = new ArrayList<>();

        private int state = 0;
        private int spaceIndex = -1;


        @Override
        public void processLine(String line) {
            LOGGER.log(Level.FINE, line);
            switch (state) {
                case 0:
                    if (AVAILABLE_TASKS.equals(line)) {
                        state = 1;
                    }
                    break;
                case 1:
                    if (!StringUtilities.hasText(line)) {
                        state = 2;
                    } else if (NO_TASKS.equals(line.trim())) {
                        state = 2;
                    } else {
                        if (spaceIndex == -1) {
                            String task = StringUtilities.explode(line.trim(), "  ").get(0); // NOI18N
                            assert StringUtilities.hasText(task) : line;
                            spaceIndex = line.indexOf(task) + task.length();
                        }
                        String task = line.substring(0, spaceIndex).trim();
                        if (StringUtilities.hasText(task)) {
                            tasks.add(task);
                        }
                    }
                    break;
                default:
                    // noop
            }
        }

        @Override
        public void reset() {
            // noop
        }

        @Override
        public void close() {
            // noop
        }

        public List<String> getTasks() {
            return Collections.unmodifiableList(tasks);
        }

    }

    private static final class TaskList implements Future<List<String>> {

        private final Future<Integer> task;
        private final GruntTasksLineProcessor convertor;

        // @GuardedBy("this")
        private List<String> gruntTasks = null;


        TaskList(Future<Integer> task, GruntTasksLineProcessor convertor) {
            assert task != null;
            assert convertor != null;
            this.task = task;
            this.convertor = convertor;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public List<String> get() throws InterruptedException, ExecutionException {
            try {
                task.get();
            } catch (CancellationException ex) {
                // cancelled by user
                LOGGER.log(Level.FINE, null, ex);
            }
            return getGruntTasks();
        }

        @Override
        public List<String> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                task.get(timeout, unit);
            } catch (CancellationException ex) {
                // cancelled by user
                LOGGER.log(Level.FINE, null, ex);
            }
            return getGruntTasks();
        }

        private synchronized List<String> getGruntTasks() {
            if (gruntTasks != null) {
                return Collections.unmodifiableList(gruntTasks);
            }
            List<String> tasks = new ArrayList<>(convertor.getTasks());
            Collections.sort(tasks);
            gruntTasks = new CopyOnWriteArrayList<>(tasks);
            return Collections.unmodifiableList(gruntTasks);
        }

    }

}
