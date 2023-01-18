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

package org.netbeans.modules.profiler.heapwalk;

import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.global.Platform;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.v2.SnapshotsWindow;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;


/**
 * Manages HeapWalker instances & TopComponents
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapWalkerManager_CannotOpenHeapWalkerMsg=Failed to open the heap dump.",
    "HeapWalkerManager_CannotDeleteHeapDumpMsg=<html><b>Deleting heap dump failed</b><br><br>Please try to delete the file once more. If it fails<br>again, restart the IDE and repeat the action.</html>"
})
public class HeapWalkerManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set dumpsBeingDeleted = new HashSet();
    private List<File> heapDumps = new ArrayList();
    private List<HeapWalker> heapWalkers = new ArrayList();

    final private RequestProcessor heapwalkerRp = new RequestProcessor(HeapWalkerManager.class);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private HeapWalkerManager() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    private static class Singleton {
        final private static HeapWalkerManager INSTANCE = new HeapWalkerManager();
    }
    
    public static HeapWalkerManager getDefault() {
        return Singleton.INSTANCE;
    }

    public boolean isHeapWalkerOpened(File file) {
        return getHeapWalker(file) != null;
    }

    public void closeAllHeapWalkers() {
        HeapWalker[] heapWalkerArr;
        synchronized (this) {
            heapWalkerArr = heapWalkers.toArray(new HeapWalker[heapWalkers.size()]);
        }
        for (HeapWalker hw : heapWalkerArr) {
            closeHeapWalker(hw);
        }
    }

    public void closeHeapWalker(final HeapWalker hw) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final TopComponent tc = getTopComponent(hw);

                    if (tc == null) {
                        ErrorManager.getDefault().log("Cannot resolve TopComponent for HeapWalker [" + hw.getHeapDumpFile() + "]"); // NOI18N

                        return;
                    }

                    tc.close();
                    FileObject folder = FileUtil.toFileObject(hw.getHeapDumpFile().getParentFile());
                    SnapshotsWindow.instance().refreshFolder(folder, false);
//                    if (ProfilerControlPanel2.hasDefault())
//                        ProfilerControlPanel2.getDefault().refreshSnapshotsList(); // Refresh to display closed HW using plain font
                }
            });
    }

    public void deleteHeapDump(final File file) {
        HeapWalker hw = getHeapWalker(file);

        if (hw != null) {
            dumpsBeingDeleted.add(file);
            closeHeapWalker(hw);
        } else {
            deleteHeapDumpImpl(file, 15);
        }
    }

    // should only be called from HeapWalkerUI.componentClosed
    public synchronized void heapWalkerClosed(HeapWalker hw) {
        final TopComponent tc = getTopComponent(hw);

        if (tc == null) {
            return;
        }

        final File file = hw.getHeapDumpFile();
        heapDumps.remove(file);
        heapWalkers.remove(hw);

        if (dumpsBeingDeleted.remove(file)) {
            BrowserUtils.performTask(new Runnable() {
                    public void run() {
                        deleteHeapDumpImpl(file, 15);
                    }
                });

        }
    }

    public void openHeapWalker(final File heapDump) {
        String heapDumpPath;
        
        try {
            heapDumpPath = heapDump.getCanonicalPath();
        } catch (IOException ex) {
            ProfilerDialogs.displayError(Bundle.HeapWalkerManager_CannotOpenHeapWalkerMsg(), null, ex.getLocalizedMessage());
            return;
        }
        synchronized (heapDumpPath.intern()) {
            HeapWalker hw = getHeapWalker(heapDump);

            if (hw == null) {
                try {
                    hw = new HeapWalker(heapDump);
                } catch (IOException e) {
                    ProfilerDialogs.displayError(Bundle.HeapWalkerManager_CannotOpenHeapWalkerMsg(), null, e.getLocalizedMessage());
                } catch (Exception e) {
                    Logger.getLogger(HeapWalkerManager.class.getName()).log(Level.SEVERE, null, e);
                }
            }

            if (hw != null) {
                openHeapWalker(hw);
            } else {
                ProfilerLogger.severe("Cannot create HeapWalker [" + heapDump + "]"); // NOI18N
            }
        }
    }

    public synchronized void openHeapWalker(final HeapWalker hw) {
        if (!heapWalkers.contains(hw)) {
            heapDumps.add(hw.getHeapDumpFile());
            heapWalkers.add(hw);
        }
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final TopComponent tc = getTopComponent(hw);

                    if (tc == null) {
                        ProfilerLogger.severe("Cannot resolve TopComponent for HeapWalker [" + hw.getHeapDumpFile() + "]"); // NOI18N

                        return;
                    }

                    tc.open();
                    //        tc.requestActive(); // For some reason steals focus from Dump Heap button in ProfilerControlPanel2 and causes http://www.netbeans.org/issues/show_bug.cgi?id=92425
                    tc.requestVisible(); // Workaround for the above problem
                    
                    FileObject folder = FileUtil.toFileObject(hw.getHeapDumpFile().getParentFile());
                    SnapshotsWindow.instance().refreshFolder(folder, false);
//                    if (ProfilerControlPanel2.hasDefault())
//                        ProfilerControlPanel2.getDefault().refreshSnapshotsList(); // Refresh to display opened HW using bold font
                }
            });
    }

    public void openHeapWalkers(File[] heapDumps) {
        for (File heapDump : heapDumps) {
            openHeapWalker(heapDump);
        }
    }

    private synchronized HeapWalker getHeapWalker(File heapDump) {
        int hdIndex = heapDumps.indexOf(heapDump);

        return (hdIndex == -1) ? null : heapWalkers.get(hdIndex);
    }

    private TopComponent getTopComponent(HeapWalker hw) {
        assert SwingUtilities.isEventDispatchThread();
        return hw.getTopComponent();
    }

    private void deleteHeapDumpImpl(final File file, final int retries) {
        heapwalkerRp.post(new Runnable() {
                public void run() {
                    if (!file.delete()) {
                        if ((retries > 0) && Platform.isWindows()) {
                            System.gc();

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ex) {
                            }

                            deleteHeapDumpImpl(file, retries - 1);
                        } else {
                            ProfilerDialogs.displayError(Bundle.HeapWalkerManager_CannotDeleteHeapDumpMsg());
                        }
                    } else {
                        FileObject folder = FileUtil.toFileObject(file.getParentFile());
                        SnapshotsWindow.instance().refreshFolder(folder, true);
//                        if (ProfilerControlPanel2.hasDefault())
//                            ProfilerControlPanel2.getDefault().refreshSnapshotsList();
                    }
                }
            });
    }
}
