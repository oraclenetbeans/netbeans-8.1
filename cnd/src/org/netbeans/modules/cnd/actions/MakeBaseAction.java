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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.actions;

import java.awt.Frame;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cnd.api.toolchain.PredefinedToolKind;
import org.netbeans.modules.nativeexecution.api.ExecutionListener;
import org.netbeans.modules.cnd.api.remote.RemoteSyncSupport;
import org.netbeans.modules.cnd.api.remote.RemoteSyncWorker;
import org.netbeans.modules.cnd.builds.MakeExecSupport;
import org.netbeans.modules.cnd.spi.toolchain.CompilerLineConvertor;
import org.netbeans.modules.cnd.settings.MakeSettings;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetUtils;
import org.netbeans.modules.cnd.spi.toolchain.ToolchainProject;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.ui.ModalMessageDlg;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionDescriptor;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionService;
import org.netbeans.modules.nativeexecution.api.execution.PostMessageDisplayer;
import org.netbeans.modules.nativeexecution.api.util.HelperLibraryUtility;
import org.netbeans.modules.nativeexecution.api.util.MacroMap;
import org.netbeans.modules.nativeexecution.api.util.WindowsSupport;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.WindowManager;

/**
 * Base class for Make Actions ...
 */
public abstract class MakeBaseAction extends AbstractExecutorRunAction {

    @Override
    protected boolean accept(DataObject object) {
        return object != null && object.getCookie(MakeExecSupport.class) != null;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        for (int i = 0; i < activatedNodes.length; i++){
            performAction(activatedNodes[i], "");// NOI18N
        }
    }

    protected void performAction(Node node, String target) {
        performAction(node, target, null, null, getProject(node), null, null);
    }

    protected Future<Integer> performAction(final Node node, final String target, final ExecutionListener listener, final Writer outputListener, final Project project,
                                 final List<String> additionalEnvironment, final InputOutput inputOutput) {
        if (SwingUtilities.isEventDispatchThread()) {
            final ModalMessageDlg.LongWorker runner = new ModalMessageDlg.LongWorker() {
                private NativeExecutionService es;
                @Override
                public void doWork() {
                    es = MakeBaseAction.this.prepare(node, target, listener, outputListener, project, additionalEnvironment, inputOutput);
                }
                @Override
                public void doPostRunInEDT() {
                    if (es != null) {
                        es.run();
                    }
                }
            };
            Frame mainWindow = WindowManager.getDefault().getMainWindow();
            String title = getString("DLG_TITLE_Prepare", "make"); // NOI18N
            String msg = getString("MSG_TITLE_Prepare", "make"); // NOI18N
            ModalMessageDlg.runLongTask(mainWindow, title, msg, runner, null);
        } else {
            NativeExecutionService es = prepare(node, target, listener, outputListener, project, additionalEnvironment, inputOutput);
            if (es != null) {
                return es.run();
            }
        }
        return null;
    }

    private NativeExecutionService prepare(Node node, String target, final ExecutionListener listener, final Writer outputListener,
                                Project project, List<String> additionalEnvironment, InputOutput inputOutput) {
        if (MakeSettings.getDefault().getSaveAll()) {
            LifecycleManager.getDefault().saveAll();
        }
        DataObject dataObject = node.getCookie(DataObject.class);
        final FileObject makeFileObject = dataObject.getPrimaryFile();
        // Build directory
        FileObject buildDirFileObject = getBuildDirectory(node,PredefinedToolKind.MakeTool);
        if (buildDirFileObject == null) {
            trace("Run folder folder is  null"); //NOI18N
            return null;
        } 
        String buildDir = buildDirFileObject.getPath();
        // Executable
        String executable = getCommand(node, project, PredefinedToolKind.MakeTool, "make"); // NOI18N
        // Arguments
        String[] args;
        if (target.length() == 0) {
            args = new String[]{"-f", makeFileObject.getNameExt()/*, "MAKE="+executable*/}; // NOI18N
        } else {
            args = new String[]{"-f", makeFileObject.getNameExt(), target/*, "MAKE="+executable*/}; // NOI18N
        }
        final ExecutionEnvironment execEnv = getExecutionEnvironment(makeFileObject, project);
        if (FileSystemProvider.getExecutionEnvironment(buildDirFileObject).isLocal()) {
            buildDir = convertToRemoteIfNeeded(execEnv, buildDir, project);
        }
        if (buildDir == null) {
            trace("Run folder folder is null"); //NOI18N
            return null;
        }
        Map<String, String> envMap = getEnv(execEnv, node, project, additionalEnvironment);
        if (isSunStudio(node, project)) {
            envMap.put("SPRO_EXPAND_ERRORS", ""); // NOI18N
        }

        if (inputOutput == null) {
            // Tab Name
            String tabName = execEnv.isLocal() ? getString("MAKE_LABEL", node.getName(), target) : getString("MAKE_REMOTE_LABEL", node.getName(), target, execEnv.getDisplayName()); // NOI18N
            InputOutput _tab = IOProvider.getDefault().getIO(tabName, false); // This will (sometimes!) find an existing one.
            _tab.closeInputOutput(); // Close it...
            InputOutput tab = IOProvider.getDefault().getIO(tabName, true); // Create a new ...
            try {
                tab.getOut().reset();
            } catch (IOException ioe) {
            }
            inputOutput = tab;
        }
        RemoteSyncWorker syncWorker = RemoteSyncSupport.createSyncWorker(project, inputOutput.getOut(), inputOutput.getErr());
        if (syncWorker != null) {
            if (!syncWorker.startup(envMap)) {
                trace("RemoteSyncWorker is not started up"); //NOI18N
                return null;
            }
        }
        final CompilerSet compilerSet = getCompilerSet(project);
        // See bug #229794 (and #228730)
        if (execEnv.isLocal() && Utilities.isWindows() && executable.contains("make") && CompilerSetUtils.isMsysBased(compilerSet)) { // NOI18N
            envMap.put("MAKE", WindowsSupport.getInstance().convertToMSysPath(executable)); // NOI18N
        }
        
        MacroMap mm = MacroMap.forExecEnv(execEnv);
        mm.putAll(envMap);
        
        if (envMap.containsKey("__CND_TOOLS__")) { // NOI18N
            try {
                if (BuildTraceHelper.isMac(execEnv)) {
                    String what = BuildTraceHelper.INSTANCE.getLibraryName(execEnv);
                    if (what.indexOf(':') > 0) {
                        what = what.substring(0,what.indexOf(':'));
                    }
                    String where = BuildTraceHelper.INSTANCE.getLDPaths(execEnv);
                    if (where.indexOf(':') > 0) {
                        where = where.substring(0,where.indexOf(':'));
                    }
                    String lib = where+'/'+what;
                    mm.prependPathVariable(BuildTraceHelper.getLDPreloadEnvName(execEnv),lib);
                } else {
                    mm.prependPathVariable(BuildTraceHelper.getLDPreloadEnvName(execEnv), BuildTraceHelper.INSTANCE.getLibraryName(execEnv)); // NOI18N
                    mm.prependPathVariable(BuildTraceHelper.getLDPathEnvName(execEnv), BuildTraceHelper.INSTANCE.getLDPaths(execEnv)); // NOI18N
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        StringBuilder argsFlat = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            argsFlat.append(" "); // NOI18N
            argsFlat.append(args[i]);
        }
        traceExecutable(executable, buildDir, args, execEnv.toString(), mm.toMap());
        
        ProcessChangeListener processChangeListener = new ProcessChangeListener(listener, outputListener,
                new CompilerLineConvertor(project, compilerSet, execEnv, makeFileObject.getParent(), inputOutput), syncWorker); // NOI18N

        NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(execEnv).
                setExecutable(executable).
                setWorkingDirectory(buildDir).
                setArguments(args).
                unbufferOutput(false).
                addNativeProcessListener(processChangeListener);

        npb.getEnvironment().putAll(mm);
        npb.redirectError();
        
        NativeExecutionDescriptor descr = new NativeExecutionDescriptor().controllable(true).
                frontWindow(true).
                inputVisible(true).
                showProgress(!CndUtils.isStandalone()).
                inputOutput(inputOutput).
                outLineBased(true).
                postExecution(processChangeListener).
                postMessageDisplayer(new PostMessageDisplayer.Default("Make")). // NOI18N
                errConvertorFactory(processChangeListener).
                outConvertorFactory(processChangeListener);
        
        descr.noReset(true);
        inputOutput.getOut().println("cd '"+buildDir+"'"); //NOI18N
        inputOutput.getOut().println(executable+" "+argsFlat); //NOI18N
        return NativeExecutionService.newService(npb, descr, "make"); // NOI18N
    }

    private CompilerSet getCompilerSet(Project project) {
        CompilerSet set = null;
        if (project != null) {
            ToolchainProject toolchain = project.getLookup().lookup(ToolchainProject.class);
            if (toolchain != null) {
                set = toolchain.getCompilerSet();
            }
        }
        if (set == null) {
            set = CompilerSetManager.get(ExecutionEnvironmentFactory.getLocal()).getDefaultCompilerSet();
        }
        return set;
    }
    private static final class BuildTraceHelper extends HelperLibraryUtility {
        private static final BuildTraceHelper INSTANCE = new BuildTraceHelper();
        private BuildTraceHelper() {
            super("org.netbeans.modules.cnd.actions", "bin/${osname}-${platform}${_isa}/libBuildTrace.${soext}"); // NOI18N
        }
    }

}
