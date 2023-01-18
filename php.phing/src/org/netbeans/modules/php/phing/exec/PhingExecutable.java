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
package org.netbeans.modules.php.phing.exec;

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
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.api.executable.PhpExecutable;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.phing.PhingBuildTool;
import org.netbeans.modules.php.phing.file.BuildXml;
import org.netbeans.modules.php.phing.options.PhingOptions;
import org.netbeans.modules.php.phing.options.PhingOptionsValidator;
import org.netbeans.modules.php.phing.ui.options.PhingOptionsPanelController;
import org.netbeans.modules.php.phing.util.PhingUtils;
import org.netbeans.modules.web.clientproject.api.util.StringUtilities;
import org.netbeans.modules.web.common.api.ValidationResult;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.InputOutput;

public class PhingExecutable {

    static final Logger LOGGER = Logger.getLogger(PhingExecutable.class.getName());

    public static final String PHING_NAME = "phing"; // NOI18N
    @org.netbeans.api.annotations.common.SuppressWarnings(value = "MS_MUTABLE_ARRAY", justification = "No need to worry, noone will change it") // NOI18N
    public static final String[] PHING_NAMES;

    private static final String LOGGER_PARAM = "-logger"; // NOI18N
    private static final String LOGGER_COLOR_VALUE = "phing.listener.AnsiColorLogger"; // NOI18N
    private static final String LIST_PARAM = "-list"; // NOI18N
    private static final String QUIET_PARAM = "-quiet"; // NOI18N

    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir")); // NOI18N

    private final Project project;
    private final String phingPath;


    static {
        PHING_NAMES = new String[] {
            Utilities.isWindows() ? PHING_NAME + ".bat" : PHING_NAME, // NOI18N
            PHING_NAME + ".php", // NOI18N
            PHING_NAME + "-latest.phar", // NOI18N
        };
    }


    PhingExecutable(String phingPath, @NullAllowed Project project) {
        assert phingPath != null;
        this.phingPath = phingPath;
        this.project = project;
    }

    @CheckForNull
    public static PhingExecutable getDefault(@NullAllowed Project project, boolean showOptions) {
        ValidationResult result = new PhingOptionsValidator()
                .validatePhing()
                .getResult();
        if (validateResult(result) != null) {
            if (showOptions) {
                UiUtils.showOptions(PhingOptionsPanelController.OPTIONS_SUBPATH);
            }
            return null;
        }
        return new PhingExecutable(PhingOptions.getInstance().getPhing(), project);
    }

    private String getCommand() {
        return phingPath;
    }

    @NbBundle.Messages({
        "# {0} - project name",
        "PhingExecutable.run=Phing ({0})",
    })
    public Future<Integer> run(String... args) {
        assert !EventQueue.isDispatchThread();
        assert project != null;
        String projectName = PhingUtils.getProjectDisplayName(project);
        Future<Integer> task = getExecutable(Bundle.PhingExecutable_run(projectName))
                .additionalParameters(getRunParams(args))
                .run(getDescriptor());
        assert task != null : phingPath;
        return task;
    }

    public Future<List<String>> listTargets() {
        final PhingTargetsLineProcessor phingTargetsLineProcessor = new PhingTargetsLineProcessor();
        Future<Integer> task = getExecutable("list phing targets") // NOI18N
                .noInfo(true)
                .additionalParameters(Arrays.asList(QUIET_PARAM, LIST_PARAM))
                .redirectErrorStream(false)
                .run(getSilentDescriptor(), new ExecutionDescriptor.InputProcessorFactory2() {
                    @Override
                    public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                        return InputProcessors.bridge(phingTargetsLineProcessor);
                    }
                });
        assert task != null : phingPath;
        return new TargetList(task, phingTargetsLineProcessor);
    }

    private PhpExecutable getExecutable(String title) {
        assert title != null;
        return new PhpExecutable(getCommand())
                .workDir(getWorkDir())
                .displayName(title)
                .optionsSubcategory(PhingOptionsPanelController.OPTIONS_SUBPATH);
    }

    private List<String> getRunParams(String[] args) {
        List<String> params = new ArrayList<>(args.length + 2);
        params.add(LOGGER_PARAM);
        params.add(LOGGER_COLOR_VALUE);
        params.addAll(Arrays.asList(args));
        return params;
    }

    private ExecutionDescriptor getDescriptor() {
        assert project != null;
        return PhpExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                .showSuspended(true)
                .optionsPath(PhingOptionsPanelController.OPTIONS_PATH)
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
            return TMP_DIR;
        }
        BuildXml buildXml = PhingBuildTool.forProject(project).getBuildXml();
        if (buildXml.exists()) {
            return buildXml.getFile().getParentFile();
        }
        File workDir = FileUtil.toFile(project.getProjectDirectory());
        assert workDir != null : project.getProjectDirectory();
        return workDir;
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

    private static final class PhingTargetsLineProcessor implements LineProcessor {

        private static final String MAIN_TARGETS = "Main targets:"; // NOI18N
        private static final String SUBTARGETS = "Subtargets:"; // NOI18N

        final List<String> targets = new ArrayList<>();

        boolean collecting = false;


        @Override
        public void processLine(String line) {
            String trimmed = line.trim();
            if (collecting) {
                if (!SUBTARGETS.equals(trimmed)) {
                    if (StringUtilities.hasText(trimmed.replace('-', ' '))) { // NOI18N
                        targets.add(StringUtilities.explode(trimmed, " ").get(0)); // NOI18N
                    }
                }
            } else {
                collecting = MAIN_TARGETS.equals(trimmed)
                        || SUBTARGETS.equals(trimmed);
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

        public List<String> getTargets() {
            return Collections.unmodifiableList(targets);
        }

    }

    private static final class TargetList implements Future<List<String>> {

        private final Future<Integer> task;
        private final PhingTargetsLineProcessor processor;

        // @GuardedBy("this")
        private List<String> phingTargets = null;


        TargetList(Future<Integer> task, PhingTargetsLineProcessor processor) {
            assert task != null;
            assert processor != null;
            this.task = task;
            this.processor = processor;
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
            return getPhingTargets();
        }

        @Override
        public List<String> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                task.get(timeout, unit);
            } catch (CancellationException ex) {
                // cancelled by user
                LOGGER.log(Level.FINE, null, ex);
            }
            return getPhingTargets();
        }

        private synchronized List<String> getPhingTargets() {
            if (phingTargets != null) {
                return Collections.unmodifiableList(phingTargets);
            }
            List<String> targets = new ArrayList<>(processor.getTargets());
            Collections.sort(targets);
            phingTargets = new CopyOnWriteArrayList<>(targets);
            return Collections.unmodifiableList(phingTargets);
        }

    }

}
