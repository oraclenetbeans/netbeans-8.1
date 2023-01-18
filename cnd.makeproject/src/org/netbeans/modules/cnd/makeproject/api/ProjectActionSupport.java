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
package org.netbeans.modules.cnd.makeproject.api;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.cnd.api.project.NativeProject;
import org.netbeans.modules.cnd.api.project.NativeProjectChangeSupport;
import org.netbeans.modules.cnd.api.remote.PathMap;
import org.netbeans.modules.cnd.api.remote.RemoteFileUtil;
import org.netbeans.modules.cnd.api.remote.RemoteSyncSupport;
import org.netbeans.modules.cnd.makeproject.MakeOptions;
import org.netbeans.modules.cnd.makeproject.api.BuildActionsProvider.BuildAction;
import org.netbeans.modules.cnd.makeproject.api.BuildActionsProvider.OutputStreamHandler;
import org.netbeans.modules.nativeexecution.api.execution.IOTabsController.IOTabFactory;
import org.netbeans.modules.nativeexecution.api.execution.IOTabsController.InputOutputTab;
import org.netbeans.modules.nativeexecution.api.execution.IOTabsController.TabsGroup;
import org.netbeans.modules.cnd.makeproject.api.ProjectActionEvent.PredefinedType;
import org.netbeans.modules.cnd.makeproject.api.ProjectActionEvent.Type;
import org.netbeans.modules.cnd.makeproject.api.configurations.ConfigurationDescriptorProvider;
import org.netbeans.modules.cnd.makeproject.api.configurations.DebuggerChooserConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.ui.CustomizerNode;
import org.netbeans.modules.cnd.makeproject.api.runprofiles.RunProfile;
import org.netbeans.modules.cnd.makeproject.runprofiles.ui.RerunArguments;
import org.netbeans.modules.cnd.makeproject.ui.MakeLogicalViewProvider;
import org.netbeans.modules.cnd.makeproject.ui.SelectExecutablePanel;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.netbeans.modules.dlight.api.terminal.TerminalSupport;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionListener;
import org.netbeans.modules.nativeexecution.api.execution.IOTabsController;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager.CancellationException;
import org.netbeans.modules.nativeexecution.api.util.HostInfoUtils;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils.ExitStatus;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 * Most of the code here came from DefaultProjectActionHandler as result of
 * refactoring.
 */
public class ProjectActionSupport {

    private static ProjectActionSupport instance;
    private static final RequestProcessor RP = new RequestProcessor("ProjectActionSupport.refresh", 1); // NOI18N
    private static final RequestProcessor tasksProcessor = new RequestProcessor("ProjectActionSupport.tasks", 50); // NOI18N
    private static final Logger LOGGER = Logger.getLogger("org.netbeans.modules.cnd.makeproject"); // NOI18N
    private final List<ProjectActionHandlerFactory> handlerFactories;

    private ProjectActionSupport() {
        handlerFactories = new ArrayList<>(
                Lookup.getDefault().lookupAll(ProjectActionHandlerFactory.class));
    }

    /**
     * Singleton pattern: instance getter.
     *
     * @return singleton instance
     */
    public static synchronized ProjectActionSupport getInstance() {
        if (instance == null) {
            instance = new ProjectActionSupport();
        }
        return instance;
    }

    private static boolean isFileOperationsIntensive(ProjectActionEvent pae) {
        Type type = pae.getType();
        if (type == PredefinedType.PRE_BUILD || type == PredefinedType.BUILD || type == PredefinedType.CLEAN || type == PredefinedType.BUILD_TESTS) {
            return true;
        }
        return false;
    }

    private static void refreshProjectFilesOnFinish(final ProjectActionEvent curPAE, final FileOperationsNotifier fon) {
        try {
            if (curPAE.getType() != PredefinedType.RUN && !fon.isLastExpectedEvent(curPAE)) {
                return;
            }
            final Project project = curPAE.getProject();
            final Set<File> files = new HashSet<>();
            final Set<FileObject> fileObjects = new HashSet<>();
            FileObject projectFileObject = project.getProjectDirectory();
            File f = FileUtil.toFile(projectFileObject);
            if (f != null) {
                files.add(f);
            } else {
                fileObjects.add(projectFileObject);
            }
            Sources sources = ProjectUtils.getSources(project);
            SourceGroup[] groups = sources.getSourceGroups(Sources.TYPE_GENERIC);
            for (SourceGroup sourceGroup : groups) {
                FileObject rootFolder = sourceGroup.getRootFolder();
                File file = FileUtil.toFile(rootFolder);
                if (file != null) {
                    files.add(file);
                } else {
                    fileObjects.add(rootFolder);
                }
            }
            // IZ#201761  -  Too long refreshing file system after build.
            // refresh can take a lot of time for slow file systems
            // so we use worker and schedule it out of build process
            final Runnable refresher = new Runnable() {

                @Override
                public void run() {
                    final File[] array = files.toArray(new File[files.size()]);
                    if (array.length > 0) {
                        FileUtil.refreshFor(array);
                    }
                    if (!fileObjects.isEmpty()) {
                        for (FileObject fo : fileObjects) {
                            FileSystemProvider.scheduleRefresh(fo);
                        }
                    }
                }
            };
            // Always redirect into RP, otherwise status of build is not displayed for a long time
            RP.post(new Runnable() {

                @Override
                public void run() {
                    FileUtil.runAtomicAction(refresher);
                    fon.onFinish(curPAE);
                    MakeLogicalViewProvider.refreshBrokenItems(project);
                }
            });
            ConfigurationDescriptorProvider.SnapShot snapShot = curPAE.getContext().lookup(ConfigurationDescriptorProvider.SnapShot.class);
            if (snapShot != null) {
                ConfigurationDescriptorProvider cdp = project.getLookup().lookup(ConfigurationDescriptorProvider.class);
                cdp.endModifications(snapShot, true, LOGGER);
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Cannot refresh project files", e);
        }
    }

    /**
     * Checks if given action type can be handled. All registered handler
     * factories are asked.
     *
     * @param conf
     * @param type
     * @return
     */
    public boolean canHandle(MakeConfiguration conf, Lookup context, ProjectActionEvent.Type type) {
        if (conf != null) {
            DebuggerChooserConfiguration chooser = conf.getDebuggerChooserConfiguration();
            CustomizerNode node = chooser.getNode();
            if (node instanceof ProjectActionHandlerFactory) {
                if (((ProjectActionHandlerFactory) node).canHandle(type, context, conf)) {
                    return true;
                }
            }
        }
        for (ProjectActionHandlerFactory factory : handlerFactories) {
            if (factory.canHandle(type, context, conf)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes an array of project actions asynchronously.
     *
     * @param paes project actions
     */
    public void fireActionPerformed(ProjectActionEvent[] paes) {
        fireActionPerformed(paes, null);
    }

    public void fireActionPerformed(ProjectActionEvent[] paes, ProjectActionHandler preferredHandler) {
        submitTask(new EventsProcessor(paes, preferredHandler));
    }

    private static void submitTask(final EventsProcessor eventsProcessor) {
        tasksProcessor.post(eventsProcessor);
    }

    /**
     * checks whether the project is ok (not deleted)
     */
    private static boolean checkProject(ProjectActionEvent pae) {
        Project project = pae.getProject();
        if (project != null) { // paranoidal null checks are better than latent NPE :)
            if (CndUtils.isUnitTestMode() || OpenProjects.getDefault().isProjectOpen(project)) { // OpenProjects don't work in test mode
                FileObject projectDirectory = project.getProjectDirectory();
                if (projectDirectory != null) {
                    FileObject nbproject = projectDirectory.getFileObject(MakeConfiguration.NBPROJECT_FOLDER); // NOI18N
                    return nbproject != null && nbproject.isValid();
                }
            }
        }
        return false;
    }

////////////////////////////////////////////////////////////////////////////////
    private static final class ProjectFileOperationsNotifier {

        private final NativeProjectChangeSupport npcs;
        private final ProjectActionEvent startPAE;
        private ProjectActionEvent finishPAE;

        public ProjectFileOperationsNotifier(NativeProjectChangeSupport npcs, ProjectActionEvent startPAE) {
            this.npcs = npcs;
            this.startPAE = startPAE;
        }

        @Override
        public String toString() {
            return "ProjectFileOperationsNotifier{" + "npcs=" + npcs + ", startPAE=" + startPAE + ", finishPAE=" + finishPAE + '}'; // NOI18N
        }
    }

    private static final class FileOperationsNotifier {

        private final Map<Project, ProjectFileOperationsNotifier> prjNotifier;

        public FileOperationsNotifier(Map<Project, ProjectFileOperationsNotifier> prjNotifier) {
            this.prjNotifier = prjNotifier;
        }

        private void onStart(ProjectActionEvent curPAE) {
            ProjectFileOperationsNotifier notifier = prjNotifier.get(curPAE.getProject());
            if (notifier != null && (notifier.npcs != null) && (curPAE == notifier.startPAE)) {
                notifier.npcs.fireFileOperationsStarted();
            }
        }

        public void onFinish(ProjectActionEvent curPAE) {
            ProjectFileOperationsNotifier notifier = prjNotifier.get(curPAE.getProject());
            if (notifier != null && (notifier.npcs != null) && (curPAE == notifier.finishPAE)) {
                notifier.npcs.fireFileOperationsFinished();
            }
        }

        private void finishAll() {
            for (ProjectFileOperationsNotifier notifier : prjNotifier.values()) {
                if (notifier.npcs != null) {
                    notifier.npcs.fireFileOperationsFinished();
                }
            }
        }

        private boolean isLastExpectedEvent(ProjectActionEvent curPAE) {
            ProjectFileOperationsNotifier notifier = prjNotifier.get(curPAE.getProject());
            if (notifier != null && (notifier.npcs != null) && (curPAE == notifier.finishPAE)) {
                return true;
            }
            return false;
        }
    }

    private final class EventsProcessor implements Runnable {

        private final TabsGroup tabs;
        private final ProjectActionEvent[] paes;
        private final AtomicReference<ProjectActionHandler> activeHandlerRef = new AtomicReference<>(null);
        private final StopAction stopAction = new StopAction(activeHandlerRef);
        private final RerunAction rerunAction = new RerunAction(this);
        private final RerunModAction rerunModAction = new RerunModAction(this);
        private final TermAction ta = new TermAction(this);
        private List<BuildAction> additional;
        private final ProjectActionHandler customHandler;
        private final FileOperationsNotifier fon;

        public EventsProcessor(ProjectActionEvent[] paes, ProjectActionHandler customHandler) {
            this.paes = paes;
            this.customHandler = customHandler;
            fon = getFileOperationsNotifier(paes);
            tabs = IOTabsController.getDefault().openTabsGroup(getTabName(paes), MakeOptions.getInstance().getReuse());
        }

        private Action[] getActions(String name) {
            List<Action> list = new ArrayList<>();
            list.add(stopAction);
            list.add(rerunAction);
            for(int i = 0; i < paes.length; i++) {
                if (paes[i].getType() == PredefinedType.RUN) {
                    list.add(rerunModAction);
                    break;
                }
            }
            list.add(ta);
            if (additional == null) {
                additional = BuildActionsProvider.getDefault().getActions(name, paes);
            }
            // TODO: actions should have acces to output writer. Action should listen output writer.
            // Provide parameter outputListener for DefaultProjectActionHandler.ProcessChangeListener
            list.addAll(additional);
            return list.toArray(new Action[list.size()]);
        }

        private String getTabName(ProjectActionEvent[] paes) {
            String projectName = ProjectUtils.getInformation(paes[0].getProject()).getDisplayName();
            StringBuilder name = new StringBuilder(projectName);
            name.append(" ("); // NOI18N
            int counter = 0;
            boolean skipDebug = paes.length > 1;//skip debug if more then on Debug Action (build, debug) as Debug will be in separate tab
            for (int i = 0; i < paes.length; i++) {
                if (counter >= 2) {
                    name.append("..."); // NOI18N
                    break;
                }
                final Type type = paes[i].getType();

                boolean isDebugAction = type == PredefinedType.DEBUG
                        || type == PredefinedType.DEBUG_STEPINTO
                        || type == PredefinedType.DEBUG_TEST
                        || type == PredefinedType.DEBUG_STEPINTO_TEST;
                if (skipDebug && isDebugAction) {
                    continue;
                }
                if (counter > 0) {
                    name.append(", "); // NOI18N
                }
                counter++;
                name.append(paes[i].getActionName());

            }
            name.append(")"); // NOI18N
            if (paes.length > 0) {
                MakeConfiguration conf = paes[0].getConfiguration();
                if (!conf.getDevelopmentHost().isLocalhost()) {
                    String hkey = conf.getDevelopmentHost().getHostKey();
                    name.append(" - ").append(hkey); //NOI18N
                }
            }
            return name.toString();
        }

        private ProgressHandle createProgressHandle(final InputOutputTab ioTab, final ProjectActionHandler handlerToUse) {
            final Cancellable cancel = handlerToUse.canCancel() ? new Cancellable() {

                @Override
                public boolean cancel() {
                    stopAction.actionPerformed(null);
                    return true;
                }
            } : null;

            return ProgressHandleFactory.createHandle(ioTab.getName(), cancel, new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    ioTab.select();
                }
            });
        }

        @Override
        public void run() {
            tabs.lockAndReset();
            LifecycleManager.getDefault().saveAll();
            rerunAction.setEnabled(false);
            rerunModAction.setEnabled(false);
            stopAction.setEnabled(false);

            final AtomicInteger currentEventIndex = new AtomicInteger(-1);
            final AtomicReference<InputOutputTab> currentIORef = new AtomicReference<>(null);

            try {
                for (final ProjectActionEvent currentEvent : paes) {
                    currentEventIndex.incrementAndGet();

                    if (!ProjectActionSupport.checkProject(currentEvent)) {
                        return;
                    }

                    final Type type = currentEvent.getType();

                    boolean isDebugAction = type == PredefinedType.DEBUG
                            || type == PredefinedType.DEBUG_STEPINTO
                            || type == PredefinedType.DEBUG_TEST
                            || type == PredefinedType.DEBUG_STEPINTO_TEST;
                    // Validate executable
                    boolean isRunAction = (type == PredefinedType.RUN
                            || isDebugAction
                            || type == PredefinedType.CUSTOM_ACTION);

                    if ((isRunAction || type == PredefinedType.CHECK_EXECUTABLE)) {
                        if (!checkExecutable(currentEvent)) {
                            return;
                        }
                    }
                    int consoleType = currentEvent.getProfile().getConsoleType().getValue();
                    if (consoleType == RunProfile.CONSOLE_TYPE_EXTERNAL
                            && !currentEvent.getConfiguration().getDevelopmentHost().isLocalhost()) {
                        consoleType = RunProfile.CONSOLE_TYPE_DEFAULT;
                    }

                    if (consoleType == RunProfile.CONSOLE_TYPE_DEFAULT) {
                        consoleType = RunProfile.getDefaultConsoleType();
                    }

                    final IOTabFactory tabFactory;
                    final String tabBaseName;

                    if (isRunAction && consoleType == RunProfile.CONSOLE_TYPE_INTERNAL) {
                        tabBaseName = getTabName(new ProjectActionEvent[]{currentEvent});
                        tabFactory = new IOTabFactory() {

                            @Override
                            public InputOutput createNewTab(final String tabName) {
                                IOProvider ioProvider = IOProvider.get("Terminal"); // NOI18N
                                if (ioProvider == null) {
                                    ioProvider = IOProvider.getDefault();
                                }
                                return ioProvider.getIO(tabName, getActions(currentEvent.getActionName()));
                            }
                        };
                    } else if (isDebugAction) {
                        //this if fix of bz#249112 - Input doesn't work in "Standard Output" mode (gdb debugger)
                        //if this is RUN action then input  is required
                        //some changes in org.netbeans.core.output2.NbIO broke the possibility to reuse IO
                        //the flag inputClosed in NbIO was set to true and that was the reason
                        //user could not imput anything
                        tabBaseName = getTabName(new ProjectActionEvent[]{currentEvent});
                        tabFactory = new IOTabFactory() {

                            @Override
                            public InputOutput createNewTab(final String tabName) {
                                return IOProvider.getDefault().getIO(tabName, getActions(currentEvent.getActionName()));
                            }
                        };
                    } else {
                        tabBaseName = getTabName(paes);
                        tabFactory = new IOTabFactory() {

                            @Override
                            public InputOutput createNewTab(final String tabName) {
                                return IOProvider.getDefault().getIO(tabName, getActions(currentEvent.getActionName()));
                            }
                        };
                    }

                    final InputOutputTab ioTab = tabs.getTab(tabBaseName, tabFactory);
//                    if (isRunAction) {
//                        ioTab.resetIO();
//                    }

                    InputOutputTab prevIO = currentIORef.getAndSet(ioTab);
                    if (prevIO != null && prevIO != ioTab) {
                        prevIO.closeOutput();
                    }

                    ProjectActionHandler handlerToUse = null;
                    if (type == PredefinedType.CUSTOM_ACTION && customHandler != null) {
                        handlerToUse = customHandler;
                    } else {
                        for (ProjectActionHandlerFactory factory : handlerFactories) {
                            if (factory.canHandle(currentEvent)) {
                                handlerToUse = factory.createHandler();
                                break;
                            }
                        }
                    }

                    if (handlerToUse == null) {
                        // TODO: reporting
                        break;
                    }

                    final CountDownLatch eventProcessed = new CountDownLatch(1);
                    final AtomicBoolean stepFailed = new AtomicBoolean(true);
                    final ExecutionListener eventExecutionListener = new ExecutionListener() {

                        @Override
                        public void executionStarted(int pid) {
                            try {
                                final ProjectActionHandler handler = activeHandlerRef.get();
                                stopAction.setEnabled(handler != null && handler.canCancel());
                                if (additional != null) {
                                    for (BuildAction action : additional) {
                                        try {
                                            action.setStep(currentEventIndex.get());
                                            action.executionStarted(pid);
                                        } catch (Throwable th) {
                                        }
                                    }
                                }
                            } finally {
                                fon.onStart(currentEvent);
                            }
                        }

                        @Override
                        public void executionFinished(final int rc) {
                            try {
                                stopAction.setEnabled(false);
                                stepFailed.set(rc != 0);
                                if (additional != null) {
                                    for (Action action : additional) {
                                        try {
                                            ((ExecutionListener) action).executionFinished(rc);
                                        } catch (Throwable th) {
                                        }
                                    }
                                }
                            } finally {
                                eventProcessed.countDown();
                            }
                        }
                    };

                    ProgressHandle progressHandle = null;

                    try {
                        initHandler(handlerToUse, currentEvent, paes);
                        activeHandlerRef.set(handlerToUse);
                        handlerToUse.addExecutionListener(eventExecutionListener);
                        progressHandle = createProgressHandle(ioTab, handlerToUse);
                        progressHandle.start();
                        handlerToUse.execute(IOTabsController.getInputOutput(ioTab));
                        try {
                            eventProcessed.await();
                        } catch (InterruptedException ex) {
                            Thread.interrupted();
                            break;
                        }

                        refreshProjectFilesOnFinish(currentEvent, fon);
                        if (stepFailed.get()) {
                            break;
                        }
                    } finally {
                        handlerToUse.removeExecutionListener(eventExecutionListener);
                        activeHandlerRef.set(null);
                        if (progressHandle != null) {
                            progressHandle.finish();
                        }
                        stopAction.setEnabled(false);
                    }
                }
            } catch (IllegalStateException ex) {
                //thrown when connection is broken
                ex.printStackTrace();
            } finally {
                tabs.unlockAndCloseOutput();
                rerunAction.setEnabled(true);
                rerunModAction.setEnabled(true);
                stopAction.setEnabled(false);
                fon.finishAll();
            }
        }

        private void initHandler(final ProjectActionHandler handler, final ProjectActionEvent pae, final ProjectActionEvent[] paes) {
            if (additional == null) {
                additional = BuildActionsProvider.getDefault().getActions(pae.getActionName(), paes);
            }
            List<OutputStreamHandler> streamHandlers = new ArrayList<>();
            for (BuildAction action : additional) {
                if (action instanceof OutputStreamHandler) {
                    streamHandlers.add((OutputStreamHandler) action);
                }
            }
            handler.init(pae, paes, streamHandlers);
        }

        private FileOperationsNotifier getFileOperationsNotifier(ProjectActionEvent[] paes) {
            Map<Project, ProjectFileOperationsNotifier> prj2Notifier = new HashMap<>();
            for (ProjectActionEvent pae : paes) {
                if (isFileOperationsIntensive(pae)) {
                    Project project = pae.getProject();
                    ProjectFileOperationsNotifier notifer = prj2Notifier.get(project);
                    if (notifer == null) {
                        NativeProjectChangeSupport npcs = null;
                        try {
                            npcs = project.getLookup().lookup(NativeProjectChangeSupport.class);
                            if (npcs == null) {
                                NativeProject nativeProject = project.getLookup().lookup(NativeProject.class);
                                if (nativeProject instanceof NativeProjectChangeSupport) {
                                    npcs = (NativeProjectChangeSupport) nativeProject;
                                }
                            }
                        } catch (Exception e) {
                            // This may be ok. The project could have been removed ....
                            System.err.println("getNativeProject " + e);
                        }
                        notifer = new ProjectFileOperationsNotifier(npcs, pae);
                        prj2Notifier.put(project, notifer);
                    }
                    notifer.finishPAE = pae;
                }
            }
            return new FileOperationsNotifier(prj2Notifier);
        }

        public NativeProjectChangeSupport getNativeProjectChangeSupport(Project project) {
            NativeProject nativeProject = null;
            try {
                nativeProject = project.getLookup().lookup(NativeProject.class);
            } catch (Exception e) {
                // This may be ok. The project could have been removed ....
                System.err.println("getNativeProject " + e);
            }
            if (nativeProject instanceof NativeProjectChangeSupport) {
                return (NativeProjectChangeSupport) nativeProject;
            } else {
                return null;
            }
        }

        private final String FILE_LOCATIONS[] = {
            "/usr/bin", //NOI18N
            "/usr/sbin", //NOI18N
            "/bin" //NOI18N
        };    

        private boolean checkExecutable(ProjectActionEvent pae) {
            // Check if something is specified
            String executable = pae.getExecutable();
            if (executable.length() == 0) {
                SelectExecutablePanel panel = new SelectExecutablePanel(pae);
                DialogDescriptor descriptor = new DialogDescriptor(panel, getString("SELECT_EXECUTABLE")); // NOI18N
                panel.setDialogDescriptor(descriptor);
                DialogDisplayer.getDefault().notify(descriptor);
                if (descriptor.getValue() == DialogDescriptor.OK_OPTION) {
                    final String selectedExecutable = panel.getExecutable();
                    final MakeConfiguration projectConfiguration = pae.getConfiguration();

                    // Modify Configuration ...
                    RunProfile runProfile = projectConfiguration.getProfile();
                    if (runProfile != null) {
                        String currentValue = runProfile.getRunCommand().getValue();
                        if (currentValue.isEmpty()) {
                            // Will set this field to default macro
                            runProfile.getRunCommand().setValue("\"" + MakeConfiguration.CND_OUTPUT_PATH_MACRO + "\""); // NOI18N
                        }
//                      } else if (currentValue.indexOf(CND_OUTPUT_PATH_MACRO) < 0) {
//                          String relativeToRunDir = ProjectSupport.toProperPath(runProfile.getRunDirectory(), executable, MakeProjectOptions.getPathMode());
//                          runProfile.getRunCommand().setValue(relativeToRunDir);
//                          return;
//                      }
                    }

                    String relativeToBaseDir = ProjectSupport.toProperPath(projectConfiguration.getBaseFSPath(), selectedExecutable, pae.getProject());
                    projectConfiguration.getMakefileConfiguration().getOutput().setValue(relativeToBaseDir);

                    // Modify pae ...
                    pae.setExecutable(selectedExecutable);
                    pae.setFinalExecutable();
                    return true;
                }
                return false;
            }
            // Check existence of executable
            if (!CndPathUtilities.isPathAbsolute(executable)) { // NOI18N
                //executable is relative to run directory - convert to absolute and check. Should be safe (?).
                String runDir = pae.getProfile().getRunDir();
                if (runDir != null) {
                    runDir = runDir.trim();
                    if (runDir.startsWith("~/") || runDir.startsWith("~\\") || runDir.equals("~")) { // NOI18N
                        try {
                            if (pae.getConfiguration().getDevelopmentHost().getExecutionEnvironment().isLocal()) {
                                runDir = HostInfoUtils.getHostInfo(pae.getConfiguration().getDevelopmentHost().getExecutionEnvironment()).getUserDirFile().getAbsolutePath() + runDir.substring(1);
                            } else {
                                runDir = HostInfoUtils.getHostInfo(pae.getConfiguration().getDevelopmentHost().getExecutionEnvironment()).getUserDir() + runDir.substring(1);
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ProjectActionSupport.class.getName()).log(Level.INFO, "", ex);  // NOI18N
                        } catch (CancellationException ex) {
                            Logger.getLogger(ProjectActionSupport.class.getName()).log(Level.INFO, "", ex);  // NOI18N
                        }
                    }
                }
                if (runDir == null || runDir.length() == 0) {
                    executable = CndPathUtilities.toAbsolutePath(pae.getConfiguration().getBaseFSPath(), executable);
                } else {
                    runDir = CndPathUtilities.toAbsolutePath(pae.getConfiguration().getBaseFSPath(), runDir);
                    FileSystem fs = pae.getConfiguration().getFileSystem();
                    if (pae.getConfiguration().getBaseDir().equals(runDir)) {
                        // In case if runDir is .
                        executable = CndPathUtilities.toAbsolutePath(fs, runDir, executable);
                    } else {
                        executable = CndPathUtilities.toAbsolutePath(fs,runDir, CndPathUtilities.getBaseName(executable));
                    }
                }
                executable = CndPathUtilities.normalizeSlashes(executable);
            }
            if (CndPathUtilities.isPathAbsolute(executable)) {
                MakeConfiguration conf = pae.getConfiguration();
                boolean ok = true;

                if (conf != null && !conf.getDevelopmentHost().isLocalhost()) {
                    final ExecutionEnvironment execEnv = conf.getDevelopmentHost().getExecutionEnvironment();
                    if (!pae.isFinalExecutable()) {
                        PathMap mapper = RemoteSyncSupport.getPathMap(pae.getProject());
                        if (mapper != null) {
                            String anExecutable = mapper.getRemotePath(executable, true);
                            if (anExecutable != null) {
                                executable = anExecutable;
                            }
                        } else {
                            LOGGER.log(Level.SEVERE, "Path Mapper not found for project {0} - using local path {1}", new Object[]{pae.getProject(), executable}); //NOI18N
                        }
                    }

                    
                    String testUtility = HostInfoUtils.searchFile(execEnv, Arrays.asList(FILE_LOCATIONS), "test", true); //NOI18N                    
                    if (testUtility != null) { //otherwise will try to run/debug
                        ExitStatus res = ProcessUtils.execute(execEnv, testUtility, "-x", executable, "-a", "-f", executable); // NOI18N
                        ok = res.isOK();
                    }
                } else {
                    // FIXUP: getExecutable should really return fully qualified name to executable including .exe
                    // but it is too late to change now. For now try both with and without.
                    File file = new File(executable);
                    if (!file.exists() && Utilities.isWindows()) {
                        file = CndFileUtils.createLocalFile(executable + ".exe"); // NOI18N
                    }
                    if (!file.exists() || file.isDirectory()) {
                        ok = false;
                    }
                }
                if (!ok) {
                    String value = pae.getProfile().getRunCommand().getValue();
                    String errormsg = getString("EXECUTABLE_DOESNT_EXISTS", executable); // NOI18N
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(errormsg, NotifyDescriptor.ERROR_MESSAGE));
                    return false;
                }
            }

            // Finally set pae.executable to a real, verified file with an absolute
            // path that reflects file location on a target host (local or remote)

            pae.setExecutable(executable);
            pae.setFinalExecutable();

            return true;
        }
    }

// VK: inlined since it's used once; and caller should know not only return status,
// but mapped name as well => it's easier to inline
//    /**
//     * Verify a remote executable exists, is executable, and is not a directory.
//     *
//     * @param execEnv The remote host
//     * @param executable The file to remotely check
//     * @return true if executable exists and is an executable, otherwise false
//     */
//    private static boolean verifyRemoteExecutable(ExecutionEnvironment execEnv, String executable) {
//        PathMap mapper = HostInfoProvider.getMapper(execEnv);
//        String remoteExecutable = mapper.getRemotePath(executable);
//        CommandProvider cmd = Lookup.getDefault().lookup(CommandProvider.class);
//        if (cmd != null) {
//            return cmd.run(execEnv, "test -x " + remoteExecutable + " -a -f " + remoteExecutable, null) == 0; // NOI18N
//        }
//        return false;
//    }
    private static final class StopAction extends AbstractAction {

        private final AtomicReference<ProjectActionHandler> activeHandlerRef;

        public StopAction(AtomicReference<ProjectActionHandler> activeHandlerRef) {
            this.activeHandlerRef = activeHandlerRef;
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/cnd/makeproject/ui/resources/stop.png", false)); // NOI18N
            putValue(Action.SHORT_DESCRIPTION, getString("TargetExecutor.StopAction.stop")); // NOI18N
            //System.out.println("handleEvents 1 " + handleEvents);
            //setEnabled(false); // initially, until ready
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) {
                return;
            }
            setEnabled(false);
            ProjectActionHandler handler = activeHandlerRef.getAndSet(null);
            if (handler != null) {
                handler.cancel();
            }
        }
    }

    private static final class RerunAction extends AbstractAction {

        private final EventsProcessor eventsProcessor;

        public RerunAction(final EventsProcessor eventsProcessor) {
            this.eventsProcessor = eventsProcessor;
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/cnd/makeproject/ui/resources/rerun.png", false)); // NOI18N
            putValue(Action.SHORT_DESCRIPTION, getString("TargetExecutor.RerunAction.rerun")); // NOI18N
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            submitTask(eventsProcessor);
        }
    }

    private static final class RerunModAction extends AbstractAction {

        private final EventsProcessor eventsProcessor;

        public RerunModAction(final EventsProcessor eventsProcessor) {
            this.eventsProcessor = eventsProcessor;
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/cnd/makeproject/ui/resources/rerun-mod.png", false)); // NOI18N
            putValue(Action.SHORT_DESCRIPTION, getString("TargetExecutor.RerunAction.rerun-mod")); // NOI18N
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            if (new RerunArguments(eventsProcessor.paes).showMe()) {
                submitTask(eventsProcessor);
            } else {
                setEnabled(true);
            }
        }
    }

    private static final class TermAction extends AbstractAction {

        private final EventsProcessor handleEvents;

        public TermAction(EventsProcessor handleEvents) {
            this.handleEvents = handleEvents;
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/dlight/terminal/ui/term.png", false)); // NOI18N
            putValue(Action.SHORT_DESCRIPTION, getString("TargetExecutor.TermAction.text")); // NOI18N
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = handleEvents.paes.length - 1; i >= 0; i--) {
                ProjectActionEvent pae = handleEvents.paes[i];
                if (ProjectActionSupport.checkProject(pae)) {
                    String projectName = ProjectUtils.getInformation(pae.getProject()).getDisplayName();
                    String dir = pae.getProfile().getRunDirectory();
                    ExecutionEnvironment env = pae.getConfiguration().getDevelopmentHost().getExecutionEnvironment();
                    if (env.isRemote()) {
                        if (RemoteFileUtil.getProjectSourceExecutionEnvironment(pae.getProject()).isLocal()) {
                            PathMap pathMap = RemoteSyncSupport.getPathMap(pae.getProject());
                            if (pathMap != null) {
                                String aDir = pathMap.getRemotePath(dir);
                                if (aDir != null) {
                                    dir = aDir;
                                }
                            } else {
                                LOGGER.log(Level.SEVERE, "Path Mapper not found for project {0} - using local path {1}", new Object[]{pae.getProject(), dir}); //NOI18N
                            }
                        }
                    }
                    TerminalSupport.openTerminal(getString("TargetExecutor.TermAction.tabTitle", projectName, env.getDisplayName()), env, dir); // NOI18N
                }
                break;
            }
        }
    }

    /**
     * Look up i18n strings here
     */
    private static String getString(String s) {
        return NbBundle.getMessage(ProjectActionSupport.class, s);
    }

    private static String getString(String s, String... arg) {
        return NbBundle.getMessage(ProjectActionSupport.class, s, arg);
    }
}
