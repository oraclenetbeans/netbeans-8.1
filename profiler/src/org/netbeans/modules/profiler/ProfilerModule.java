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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.profiler;

import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbBundle;
import javax.swing.*;
import org.netbeans.modules.profiler.api.ProfilerDialogs;


/**
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "ProfilerModule_CalibrationFailedMessage=Calibration failed.\nPlease check your setup and run the calibration again.",
    "ProfilerModule_ExitingFromProfileMessage=Profiling session is currently in progress\nDo you want to stop the current session and exit the IDE?",
    "ProfilerModule_QuestionDialogCaption=Question",
    "ProfilerModule_ExitingFromAttachMessage=Profiling session is currently in progress\nDo you want to detach from the target application and exit the IDE?"
})
public final class ProfilerModule extends ModuleInstall {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String LIBS_DIR = "lib"; //NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Called when the IDE is about to exit. The default implementation returns <code>true</code>.
     * The module may cancel the exit if it is not prepared to be shut down.
     *
     * @return <code>true</code> if it is ok to exit the IDE
     */
    public boolean closing() {
        if (!NetBeansProfiler.isInitialized()) return true;
        final int state = Profiler.getDefault().getProfilingState();
        final int mode = Profiler.getDefault().getProfilingMode();

        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
            if (mode == Profiler.MODE_PROFILE) {
                if (!ProfilerDialogs.displayConfirmation(
                        Bundle.ProfilerModule_ExitingFromProfileMessage(), 
                        Bundle.ProfilerModule_QuestionDialogCaption())) {
                    return false;
                }

                Profiler.getDefault().stopApp();
            } else {
                if (!ProfilerDialogs.displayConfirmation(
                        Bundle.ProfilerModule_ExitingFromAttachMessage(), 
                        Bundle.ProfilerModule_QuestionDialogCaption())) {
                    return false;
                }

                Profiler.getDefault().detachFromApp();
            }
        }

        // cleanup before exiting the IDE, always returns true
//        if (LiveResultsWindow.hasDefault()) {
//            LiveResultsWindow.getDefault().ideClosing();
//        }

        return true;
    }

    /**
     * Called when an already-installed module is restored (during IDE startup).
     * Should perform whatever initializations are required.
     * <p>Note that it is possible for module code to be run before this method
     * is called, and that code must be ready nonetheless. For example, data loaders
     * might be asked to recognize a file before the module is "restored". For this
     * reason, but more importantly for general performance reasons, modules should
     * avoid doing anything here that is not strictly necessary - often by moving
     * initialization code into the place where the initialization is actually first
     * required (if ever). This method should serve as a place for tasks that must
     * be run once during every startup, and that cannot reasonably be put elsewhere.
     * <p>Basic programmatic services are available to the module at this stage -
     * for example, its class loader is ready for general use, any objects registered
     * declaratively to lookup (e.g. system options or services) are ready to be
     * queried, and so on.
     */
    public void restored() {
        super.restored();
        MiscUtils.setVerbosePrint(); // for EA, we want as many details in the log file as possible to be able to resolve user problems
                                     // Settings have to be load on startup at least for the following calibration (saved calibration data loading) stuff
                                     // to run correctly - it needs to know the saved JVM executable file/version to run.

        MiscUtils.deleteHeapTempFiles();
    }

    /**
     * Called when the module is uninstalled (from a running IDE).
     * Should remove whatever functionality from the IDE that it had registered.
     */
    public void uninstalled() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        // stop or detach from any profiling in progress
                        final int state = Profiler.getDefault().getProfilingState();
                        final int mode = Profiler.getDefault().getProfilingMode();

                        if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
                            if (mode == Profiler.MODE_PROFILE) {
                                Profiler.getDefault().stopApp();
                            } else {
                                Profiler.getDefault().detachFromApp();
                            }
                        }

// NB is performing IDE reset after uninstall anyway; no need to close the windows explicitly
//                        // force closing of all windows
//                        ProfilerControlPanel2.closeIfOpened();
//                        TelemetryOverviewPanel.closeIfOpened();
//                        LiveResultsWindow.closeIfOpened();
//                        TelemetryWindow.closeIfOpened();
//                        ThreadsWindow.closeIfOpened();
//                        SnapshotResultsWindow.closeAllWindows();
//                        ProfilingPointsWindow.closeIfOpened();

                        // perform any shutdown
                        ((NetBeansProfiler) Profiler.getDefault()).shutdown();

                        ResetResultsAction.getInstance().actionPerformed(null); // cleanup client data
                    }
                });
        } catch (Exception e) {
            ProfilerLogger.log(e);
        }

        // proceed with uninstall
        super.uninstalled();
    }
}
