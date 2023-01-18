/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javascript.nodejs.exec;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.netbeans.api.extexecution.print.ConvertedLine;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.project.Project;
import org.netbeans.modules.javascript.nodejs.file.PackageJson;
import org.netbeans.modules.javascript.nodejs.options.NodeJsOptions;
import org.netbeans.modules.javascript.nodejs.options.NodeJsOptionsValidator;
import org.netbeans.modules.javascript.nodejs.platform.NodeJsSupport;
import org.netbeans.modules.javascript.nodejs.preferences.NodeJsPreferences;
import org.netbeans.modules.javascript.nodejs.preferences.NodeJsPreferencesValidator;
import org.netbeans.modules.javascript.nodejs.ui.customizer.NodeJsCustomizerProvider;
import org.netbeans.modules.javascript.nodejs.ui.options.NodeJsOptionsPanelController;
import org.netbeans.modules.javascript.nodejs.util.FileUtils;
import org.netbeans.modules.javascript.nodejs.util.NodeJsUtils;
import org.netbeans.modules.javascript.nodejs.util.StringUtils;
import org.netbeans.modules.javascript.nodejs.util.ValidationUtils;
import org.netbeans.modules.javascript.v8debug.api.Connector;
import org.netbeans.modules.javascript.v8debug.api.DebuggerOptions;
import org.netbeans.modules.web.common.api.ExternalExecutable;
import org.netbeans.modules.web.common.api.ValidationResult;
import org.netbeans.modules.web.common.api.Version;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.Pair;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

public class NodeExecutable {

    static final Logger LOGGER = Logger.getLogger(NodeExecutable.class.getName());

    @org.netbeans.api.annotations.common.SuppressWarnings(value = "MS_MUTABLE_ARRAY", justification = "Just internal usage")
    public static final String[] NODE_NAMES;
    public static final int DEFAULT_DEBUG_PORT = 9292;

    private static final String IO_NAME;

    private static final String DEBUG_BRK_COMMAND = "--debug-brk=%d"; // NOI18N
    private static final String DEBUG_COMMAND = "--debug=%d"; // NOI18N
    private static final String VERSION_PARAM = "--version"; // NOI18N

    // versions of node executables
    private static final ConcurrentMap<String, Version> VERSIONS = new ConcurrentHashMap<>();
    private static final Version UNKNOWN_VERSION = Version.fromDottedNotationWithFallback("0.0"); // NOI18N

    protected final Project project;
    protected final String nodePath;


    static {
        if (Utilities.isWindows()) {
            NODE_NAMES = new String[] {"node.exe"}; // NOI18N
            IO_NAME = "iojs.exe"; // NOI18N
        } else {
            NODE_NAMES = new String[] {"node", "nodejs"}; // NOI18N
            IO_NAME = "iojs"; // NOI18N
        }
    }


    NodeExecutable(String nodePath, @NullAllowed Project project) {
        assert nodePath != null;
        this.nodePath = nodePath;
        this.project = project;
    }

    @CheckForNull
    public static NodeExecutable getDefault(@NullAllowed Project project, boolean showOptions) {
        ValidationResult result = new NodeJsOptionsValidator()
                .validateNode(false)
                .getResult();
        if (validateResult(result) != null) {
            if (showOptions) {
                OptionsDisplayer.getDefault().open(NodeJsOptionsPanelController.OPTIONS_PATH);
            }
            return null;
        }
        return createExecutable(NodeJsOptions.getInstance().getNode(), project);
    }

    @CheckForNull
    public static NodeExecutable forProject(Project project, boolean showCustomizer) {
        assert project != null;
        return forProjectInternal(project, showCustomizer);
    }

    @CheckForNull
    public static NodeExecutable forPath(String path) {
        ValidationResult result = new ValidationResult();
        ValidationUtils.validateNode(result, path);
        if (validateResult(result) != null) {
            return null;
        }
        return createExecutable(path, null);
    }

    @CheckForNull
    private static NodeExecutable forProjectInternal(@NullAllowed Project project, boolean showCustomizer) {
        if (project == null) {
            return getDefault(null, showCustomizer);
        }
        NodeJsPreferences preferences = NodeJsSupport.forProject(project).getPreferences();
        if (preferences.isDefaultNode()) {
            return getDefault(project, showCustomizer);
        }
        String node = preferences.getNode();
        ValidationResult result = new NodeJsPreferencesValidator()
                .validateNode(node)
                .getResult();
        if (validateResult(result) != null) {
            if (showCustomizer) {
                NodeJsCustomizerProvider.openCustomizer(project, result);
            }
            return null;
        }
        assert node != null;
        return createExecutable(node, project);
    }

    private static NodeExecutable createExecutable(String node, Project project) {
        if (Utilities.isMac()) {
            return new MacNodeExecutable(node, project);
        }
        return new NodeExecutable(node, project);
    }

    String getCommand() {
        return nodePath;
    }

    public boolean isIojs() {
        File node = new File(new ExternalExecutable(nodePath).getExecutable());
        if (node.getName().equals(IO_NAME)) {
            return true;
        }
        // #250534 - selected "node" file in io.js sources?
        File iojs = new File(node.getParentFile(), IO_NAME);
        // do not check if iojs exists but simply immediately compare their sizes
        return node.length() == iojs.length();
    }

    public void resetVersion() {
        VERSIONS.remove(nodePath);
    }

    public boolean versionDetected() {
        return VERSIONS.containsKey(nodePath);
    }

    @CheckForNull
    public Version getVersion() {
        Version version = VERSIONS.get(nodePath);
        if (version == UNKNOWN_VERSION) {
            return null;
        }
        if (version != null) {
            return version;
        }
        return getAndStoreVersion();
    }

    @NbBundle.Messages({
        "NodeExecutable.version.detecting=Detecting node version..."
    })
    @CheckForNull
    private Version getAndStoreVersion() {
        VersionOutputProcessorFactory versionOutputProcessorFactory = new VersionOutputProcessorFactory();
        try {
            getExecutable("node version") // NOI18N
                    .additionalParameters(getVersionParams())
                    .runAndWait(getSilentDescriptor(), versionOutputProcessorFactory, Bundle.NodeExecutable_version_detecting());
            String detectedVersion = versionOutputProcessorFactory.getVersion();
            if (detectedVersion != null) {
                Version version = Version.fromDottedNotationWithFallback(detectedVersion);
                // #255872 - for node.js, use latest 0.12
                if (!isIojs()
                        && !Integer.valueOf("0").equals(version.getMajor())) { // NOI18N
                    version = Version.fromDottedNotationWithFallback("0.12.7"); // NOI18N
                }
                VERSIONS.put(nodePath, version);
                return version;
            }
            // no version detected, store UNKNOWN_VERSION
            VERSIONS.put(nodePath, UNKNOWN_VERSION);
            return null;
        } catch (CancellationException ex) {
            // cancelled, cannot happen
            assert false;
        } catch (ExecutionException ex) {
            LOGGER.log(Level.INFO, null, ex);
        }
        return null;
    }

    @NbBundle.Messages({
        "# {0} - project name",
        "NodeExecutable.run=Node.js ({0})",
    })
    @CheckForNull
    public AtomicReference<Future<Integer>> run(File script, String args) {
        assert project != null;
        String projectName = NodeJsUtils.getProjectDisplayName(project);
        AtomicReference<Future<Integer>> taskRef = new AtomicReference<>();
        Future<Integer> task = getExecutable(Bundle.NodeExecutable_run(projectName))
                .additionalParameters(getRunParams(script, args))
                .run(getDescriptor(taskRef));
        assert task != null : nodePath;
        taskRef.set(task);
        return taskRef;
    }

    @NbBundle.Messages({
        "# {0} - project name",
        "NodeExecutable.debug=Node.js ({0})",
    })
    @CheckForNull
    public AtomicReference<Future<Integer>> debug(int port, File script, String args) {
        assert project != null;
        String projectName = NodeJsUtils.getProjectDisplayName(project);
        AtomicReference<Future<Integer>> taskRef = new AtomicReference<>();
        final Future<Integer> task = getExecutable(Bundle.NodeExecutable_run(projectName))
                .additionalParameters(getDebugParams(port, script, args))
                .run(getDescriptor(taskRef, new DebugInfo(project, taskRef, port)));
        assert task != null : nodePath;
        taskRef.set(task);
        return taskRef;
    }

    private ExternalExecutable getExecutable(String title) {
        assert title != null;
        return new ExternalExecutable(getCommand())
                .workDir(getWorkDir())
                .displayName(title)
                .optionsPath(NodeJsOptionsPanelController.OPTIONS_PATH)
                .noOutput(false);
    }

    private ExecutionDescriptor getDescriptor(AtomicReference<Future<Integer>> taskRef) {
        return getDescriptor(taskRef, null);
    }

    private ExecutionDescriptor getDescriptor(final AtomicReference<Future<Integer>> taskRef, @NullAllowed final DebugInfo debugInfo) {
        assert project != null;
        assert taskRef != null;
        List<URL> sourceRoots = NodeJsSupport.forProject(project).getSourceRoots();
        return ExternalExecutable.DEFAULT_EXECUTION_DESCRIPTOR
                .frontWindowOnError(false)
                .showSuspended(true)
                .optionsPath(NodeJsOptionsPanelController.OPTIONS_PATH)
                .outLineBased(true)
                .errLineBased(true)
                .outConvertorFactory(new LineConvertorFactoryImpl(sourceRoots, debugInfo))
                .rerunCallback(new ExecutionDescriptor.RerunCallback() {
                    @Override
                    public void performed(Future<Integer> task) {
                        taskRef.set(task);
                    }
                });
    }

    private static ExecutionDescriptor getSilentDescriptor() {
        return new ExecutionDescriptor()
                .inputOutput(InputOutput.NULL)
                .inputVisible(false)
                .frontWindow(false)
                .showProgress(false)
                .charset(StandardCharsets.UTF_8);
    }

    private File getWorkDir() {
        if (project == null) {
            return FileUtils.TMP_DIR;
        }
        PackageJson packageJson = NodeJsSupport.forProject(project).getPackageJson();
        if (packageJson.exists()) {
            return new File(packageJson.getPath()).getParentFile();
        }
        File sourceRoot = NodeJsUtils.getSourceRoot(project);
        if (sourceRoot != null) {
            return sourceRoot;
        }
        File workDir = FileUtil.toFile(project.getProjectDirectory());
        assert workDir != null : project.getProjectDirectory();
        return workDir;
    }

    private List<String> getRunParams(File script, String args) {
        return getParams(getScriptArgsParams(script, args));
    }

    private List<String> getDebugParams(int port, File script, String args) {
        List<String> params = new ArrayList<>();
        params.add(String.format(getDebugCommand(), port));
        params.addAll(getScriptArgsParams(script, args));
        return getParams(params);
    }

    private String getDebugCommand() {
        if (DebuggerOptions.getInstance().isBreakAtFirstLine()) {
            return DEBUG_BRK_COMMAND;
        }
        return DEBUG_COMMAND;
    }

    private List<String> getVersionParams() {
        return getParams(Collections.singletonList(VERSION_PARAM));
    }

    private List<String> getScriptArgsParams(File script, String args) {
        assert script != null;
        List<String> params = new ArrayList<>();
        params.add(script.getAbsolutePath());
        if (StringUtils.hasText(args)) {
            params.addAll(Arrays.asList(Utilities.parseParameters(args)));
        }
        return params;
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
            return result.getErrors().get(0).getMessage();
        }
        return result.getWarnings().get(0).getMessage();
    }

    //~ Inner classes

    private static final class MacNodeExecutable extends NodeExecutable {

        private static final String BASH_COMMAND = "/bin/bash -lc"; // NOI18N


        MacNodeExecutable(String nodePath, Project project) {
            super(nodePath, project);
        }

        @Override
        String getCommand() {
            return BASH_COMMAND;
        }

        @Override
        List<String> getParams(List<String> params) {
            StringBuilder sb = new StringBuilder(200);
            sb.append("\""); // NOI18N
            sb.append(nodePath);
            sb.append("\" \""); // NOI18N
            sb.append(StringUtils.implode(super.getParams(params), "\" \"")); // NOI18N
            sb.append("\""); // NOI18N
            return Collections.singletonList(sb.toString());
        }

    }

    static class VersionOutputProcessorFactory implements ExecutionDescriptor.InputProcessorFactory2 {

        private static final Pattern VERSION_PATTERN = Pattern.compile("^v([\\d\\.]+)$"); // NOI18N

        volatile String version;


        @Override
        public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
            return InputProcessors.bridge(new LineProcessor() {

                @Override
                public void processLine(String line) {
                    assert version == null : version + " :: " + line;
                    version = parseVersion(line);
                }

                @Override
                public void reset() {
                }

                @Override
                public void close() {
                }

            });
        }

        @CheckForNull
        public String getVersion() {
            return version;
        }

        public String parseVersion(String line) {
            Matcher matcher = VERSION_PATTERN.matcher(line);
            if (matcher.matches()) {
                return matcher.group(1);
            }
            LOGGER.log(Level.INFO, "Unexpected node.js version line: {0}", line);
            return null;
        }

    }

    private static final class LineConvertorFactoryImpl implements ExecutionDescriptor.LineConvertorFactory {

        private final List<File> files;
        private final DebugInfo debugInfo;


        public LineConvertorFactoryImpl(List<URL> sourceRoots, @NullAllowed DebugInfo debugInfo) {
            assert sourceRoots != null;
            files = new CopyOnWriteArrayList<>(toFiles(sourceRoots));
            this.debugInfo = debugInfo;
        }

        @Override
        public LineConvertor newLineConvertor() {
            return new LineConvertorImpl(new FileLineParser(files), debugInfo);
        }

        private List<File> toFiles(List<URL> sourceRoots) {
            List<File> result = new ArrayList<>(sourceRoots.size());
            for (URL sourceRoot : sourceRoots) {
                try {
                    result.add(Utilities.toFile(sourceRoot.toURI()));
                } catch (URISyntaxException ex) {
                    LOGGER.log(Level.INFO, null, ex);
                }
            }
            return result;
        }

    }

    private static final class LineConvertorImpl implements LineConvertor {

        private static final RequestProcessor RP = new RequestProcessor("node.js debugger starter/connector"); // NOI18N

        private final FileLineParser fileLineParser;
        @NullAllowed
        private final DebugInfo debugInfo;
        @NullAllowed
        final CountDownLatch debuggerCountDownLatch;


        volatile boolean debugging = false;


        public LineConvertorImpl(FileLineParser fileLineParser, @NullAllowed DebugInfo debugInfo) {
            assert fileLineParser != null;
            this.fileLineParser = fileLineParser;
            this.debugInfo = debugInfo;
            if (debugInfo == null) {
                debuggerCountDownLatch = null;
            } else {
                debuggerCountDownLatch = new CountDownLatch(1);
                RP.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            assert debuggerCountDownLatch != null;
                            boolean expected = debuggerCountDownLatch.await(5, TimeUnit.SECONDS);
                            // #252451
                            if (!expected) {
                                LOGGER.log(Level.INFO, "Connect node.js debugger timeout elapsed");
                            }
                            connectDebugger();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
        }

        @Override
        public List<ConvertedLine> convert(String line) {
            // debugger?
            if (debugInfo != null
                    && !debugging) {
                if (line.toLowerCase(Locale.US).startsWith("debugger listening on port")) { // NOI18N
                    assert debuggerCountDownLatch != null;
                    debuggerCountDownLatch.countDown();
                }
            }
            // process output
            OutputListener outputListener = null;
            Pair<File, Integer> fileLine = fileLineParser.getOutputFileLine(line);
            if (fileLine != null) {
                outputListener = new FileOutputListener(fileLine.first(), fileLine.second());
            }
            return Collections.singletonList(ConvertedLine.forText(line, outputListener));
        }

        void connectDebugger() {
            assert debugInfo != null;
            Connector.Properties props = createConnectorProperties("localhost", debugInfo.port, debugInfo.project); // NOI18N
            try {
                Connector.connect(props, new Runnable() {
                    @Override
                    public void run() {
                        debugging = false;
                        assert debugInfo != null;
                        assert debugInfo.project != null;
                        assert debugInfo.taskRef != null;
                        Future<Integer> task = debugInfo.taskRef.get();
                        assert task != null : debugInfo.project.getProjectDirectory();
                        task.cancel(true);
                    }
                });
                debugging = true;
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "cannot run node.js debugger", ex);
                warnCannotDebug(ex);
            }
        }

        private static Connector.Properties createConnectorProperties(String host, int port, Project project) {
            List<File> sourceRoots = NodeJsUtils.getSourceRoots(project);
            List<File> siteRoots = NodeJsUtils.getSiteRoots(project);
            List<String> localPaths = new ArrayList<>(sourceRoots.size());
            List<String> localPathsExclusionFilter = Collections.emptyList();
            for (File src : sourceRoots) {
                localPaths.add(src.getAbsolutePath());
                for (File site : siteRoots) {
                    if (FileUtils.isSubdirectoryOf(src, site) && !src.equals(site)) {
                        if (localPathsExclusionFilter.isEmpty()) {
                            localPathsExclusionFilter = new ArrayList<>();
                        }
                        localPathsExclusionFilter.add(site.getAbsolutePath());
                    }
                }
            }
            return new Connector.Properties(host, port, localPaths, Collections.<String>emptyList(), localPathsExclusionFilter);
        }

        @NbBundle.Messages({
            "# {0} - reason",
            "LineConvertorImpl.warn.debug=Cannot run debugger. Reason:\n\n{0}",
        })
        protected void warnCannotDebug(IOException ex) {
            NotifyDescriptor.Message descriptor = new NotifyDescriptor.Message(Bundle.LineConvertorImpl_warn_debug(ex), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(descriptor);
        }

    }

    private static final class FileOutputListener implements OutputListener {

        final File file;
        final int line;


        public FileOutputListener(File file, int line) {
            assert file != null;
            this.file = file;
            this.line = line;
        }

        @Override
        public void outputLineSelected(OutputEvent ev) {
            // noop
        }

        @Override
        public void outputLineAction(OutputEvent ev) {
            RequestProcessor.getDefault().post(new Runnable() {
                @Override
                public void run() {
                    FileUtils.openFile(file, line);
                }
            });
        }

        @Override
        public void outputLineCleared(OutputEvent ev) {
            // noop
        }

    }

    static final class FileLineParser {

        // at node.js:906:3
        // (/home/gapon/NetBeansProjects/JsLibrary6/src/main.js:9:1)
        // ^/home/gapon/NetBeansProjects/JsLibrary6/src/main.js:9$
        static final Pattern OUTPUT_FILE_LINE_PATTERN = Pattern.compile("(?:at |\\(|^)(?<FILE>[^:(]+?):(?<LINE>\\d+)(?::\\d+)?(?:\\)|$)"); // NOI18N

        private final List<File> sourceRoots;


        public FileLineParser(List<File> sourceRoots) {
            assert sourceRoots != null;
            this.sourceRoots = sourceRoots;
        }

        @CheckForNull
        Pair<File, Integer> getOutputFileLine(String line) {
            Pair<String, Integer> fileNameLine = getOutputFileNameLine(line);
            if (fileNameLine == null) {
                return null;
            }
            String filepath = fileNameLine.first();
            Integer lineNumber = fileNameLine.second();
            // complete path?
            File file = new File(filepath);
            if (file.isFile()) {
                return Pair.of(file, lineNumber);
            }
            // incomplete path?
            for (File sourceRoot : sourceRoots) {
                file = new File(sourceRoot, filepath);
                if (file.isFile()) {
                    return Pair.of(file, lineNumber);
                }
            }
            return null;
        }

        @CheckForNull
        static Pair<String, Integer> getOutputFileNameLine(String line) {
            Matcher matcher = OUTPUT_FILE_LINE_PATTERN.matcher(line.trim());
            if (!matcher.find()) {
                return null;
            }
            return Pair.of(matcher.group("FILE"), Integer.valueOf(matcher.group("LINE"))); // NOI18N
        }

    }

    private static final class DebugInfo {

        @NonNull
        public final Project project;
        @NonNull
        public final AtomicReference<Future<Integer>> taskRef;
        public final int port;


        public DebugInfo(Project project, AtomicReference<Future<Integer>> taskRef, int port) {
            assert project != null;
            assert taskRef != null;
            this.project = project;
            this.taskRef = taskRef;
            this.port = port;
        }

    }

}
