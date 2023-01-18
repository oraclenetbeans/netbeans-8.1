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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.toolchain.ui.options;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.api.remote.ServerRecord;
import org.netbeans.modules.cnd.api.remote.ServerUpdateCache;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.api.toolchain.ui.ToolsCacheManager;
import org.netbeans.modules.cnd.toolchain.compilerset.CompilerSetImpl;
import org.netbeans.modules.cnd.toolchain.compilerset.CompilerSetManagerAccessorImpl;
import org.netbeans.modules.cnd.toolchain.compilerset.CompilerSetManagerImpl;
import org.netbeans.modules.cnd.utils.ui.ModalMessageDlg;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.util.WindowsSupport;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Sergey Grinev
 */
public final class ToolsCacheManagerImpl extends ToolsCacheManager {

    private static final List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
    private ServerUpdateCache serverUpdateCache;
    private final HashMap<ExecutionEnvironment, CompilerSetManager> copiedManagers =
            new HashMap<ExecutionEnvironment, CompilerSetManager>();
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private Cancellable longTaskCancelable;

    public ToolsCacheManagerImpl(boolean initialize) {
        if (initialize) {
            for (ServerRecord record : ServerList.getRecords()) {
                CompilerSetManager csm = CompilerSetManager.get(record.getExecutionEnvironment());
                addCompilerSetManager(csm);
            }
        }
    }

    @Override
    public ServerUpdateCache getServerUpdateCache() {
        return serverUpdateCache;
    }

    @Override
    public void setHosts(Collection<? extends ServerRecord> list) {
        if (serverUpdateCache == null) {
            serverUpdateCache = new ServerUpdateCache();
        }
        serverUpdateCache.setHosts(list);
    }

    @Override
    public void setDefaultRecord(ServerRecord defaultRecord) {
        serverUpdateCache.setDefaultRecord(defaultRecord);
    }

    @Override
    public void applyChanges() {
        applyChanges(null);
    }

    @Override
    public void discardChanges() {
        clear();
    }

    @Override
    public CompilerSetManager getCompilerSetManagerCopy(ExecutionEnvironment env, boolean initialize) {
        CompilerSetManagerImpl out;
        synchronized(canceled) {
            out = (CompilerSetManagerImpl) copiedManagers.get(env);
        }
        if (out == null) {
            out = (CompilerSetManagerImpl) CompilerSetManagerAccessorImpl.getDeepCopy(env, initialize);
            if (out.getCompilerSets().size() == 1 && out.getCompilerSets().get(0).getName().equals(CompilerSetImpl.None)) {
                out.remove(out.getCompilerSets().get(0));
            }
            synchronized(canceled) {
                copiedManagers.put(env, out);
            }
        }
        return out;
    }

    @Override
    public void addCompilerSetManager(CompilerSetManager newCsm) {
        synchronized(canceled) {
            copiedManagers.put(((CompilerSetManagerImpl)newCsm).getExecutionEnvironment(), newCsm);
        }
    }

    public CompilerSetManagerImpl restoreCompilerSets(CompilerSetManagerImpl oldCsm) {

        ExecutionEnvironment execEnv = oldCsm.getExecutionEnvironment();

        ServerRecord record = ServerList.get(execEnv);
        if (record.isOffline()) {
            record.validate(true);
            if (record.isOffline()) {
                return null;
            }
        }

        CompilerSetManagerImpl newCsm = CompilerSetManagerAccessorImpl.create(execEnv);
        String progressMessage = NbBundle.getMessage(CompilerSetManagerImpl.class, "PROGRESS_TEXT", execEnv.getDisplayName()); // NOI18N
        final AtomicBoolean cancel = new AtomicBoolean(false);
        longTaskCancelable = new Cancellable() {
            @Override
            public boolean cancel() {
                cancel.set(true);
                return true;
            }
        };
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle(progressMessage, longTaskCancelable);
        progressHandle.start();
        try {
            newCsm.initialize(false, true, null);
            while (newCsm.isPending()) {
                if (cancel.get()) {
                    if (newCsm.cancel()) {
                        return null;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    return null;
                }
            }
        } finally {
            progressHandle.finish();
            longTaskCancelable = null;
        }

        if (cancel.get()) {
            return null;
        }

        List<CompilerSet> list = oldCsm.getCompilerSets();
        for (CompilerSet cs : list) {
            if (!cs.isAutoGenerated()) {
                String name = cs.getName();
                String newName = newCsm.getUniqueCompilerSetName(name);
                if (!name.equals(newName)) {
                    // FIXUP: show a dialog with renamed custom sets. Can't do now because of UI freeze.
                    ((CompilerSetImpl) cs).setName(newName);
                }
                newCsm.add(cs);
            }
        }

        String oldDefaultName;
        CompilerSet oldDefault = oldCsm.getDefaultCompilerSet();
        if (oldDefault != null) {
            oldDefaultName = oldDefault.getName();
        } else {
            oldDefaultName = null;
        }

        CompilerSet newDefault = newCsm.getCompilerSet(oldDefaultName);
        if (newDefault != null) {
            newCsm.setDefault(newDefault);
        }

        if (execEnv.isLocal()) {
            WindowsSupport.getInstance().init();
        }

        if (canceled.get()) {
            return null;
        }
        addCompilerSetManager(newCsm);

        return newCsm;
    }

    public void applyChanges(final ServerRecord selectedRecord) {
        Cancellable aLongTaskCancelable = longTaskCancelable;
        if (aLongTaskCancelable != null) {
            aLongTaskCancelable.cancel();
        }
        final ModalMessageDlg.LongWorker runner = new ModalMessageDlg.LongWorker() {
            @Override
            public void doWork() {
                List<ExecutionEnvironment> liveServers = null;
                if (serverUpdateCache != null) {
                    liveServers = new ArrayList<ExecutionEnvironment>();
                    ServerList.set(serverUpdateCache.getHosts(), serverUpdateCache.getDefaultRecord());
                    for (ServerRecord rec : serverUpdateCache.getHosts()) {
                        liveServers.add(rec.getExecutionEnvironment());
                    }
                    serverUpdateCache = null;
                } else {
                    if (selectedRecord == null) {
                        ServerRecord defaultRecord = ServerList.getDefaultRecord();
                        if (defaultRecord == null) { // or is it a paranoya?
                            ServerList.setDefaultRecord(ServerList.get(ExecutionEnvironmentFactory.getLocal()));
                        }
                    } else {
                        ServerList.setDefaultRecord(selectedRecord);
                    }
                }

                ServerList.save();
                saveCompileSetManagers(liveServers);
            }
            @Override
            public void doPostRunInEDT() {
                fireChange(ToolsCacheManagerImpl.this);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            Frame mainWindow = WindowManager.getDefault().getMainWindow();
            String title = NbBundle.getMessage(ToolsCacheManagerImpl.class, "DLG_TITLE_ApplyChanges"); // NOI18N
            String msg = NbBundle.getMessage(ToolsCacheManagerImpl.class, "MSG_TITLE_ApplyChanges"); // NOI18N
            ModalMessageDlg.runLongTask(mainWindow, title, msg, runner, null);
        } else {
            runner.doWork();
            runner.doPostRunInEDT();
        }
    }

    public Collection<? extends ServerRecord> getHosts() {
        if (serverUpdateCache != null) {
            return serverUpdateCache.getHosts();
        }
        return ServerList.getRecords();
    }

    public ServerRecord getDefaultHostRecord() {
        if (serverUpdateCache != null) {
            return serverUpdateCache.getDefaultRecord();
        } else {
            return ServerList.getDefaultRecord();
        }
    }

    public boolean hasCache() {
        return serverUpdateCache != null;
    }

    public void clear() {
        synchronized(canceled) {
            canceled.set(false);
            serverUpdateCache = null;
            copiedManagers.clear();
        }
    }

    public void cancel() {
        synchronized(canceled) {
            canceled.set(true);
            Cancellable aLongTaskCancelable = longTaskCancelable;
            if (aLongTaskCancelable != null) {
                aLongTaskCancelable.cancel();
            }
            serverUpdateCache = null;
            copiedManagers.clear();
        }
    }

    private synchronized void saveCompileSetManagers(List<ExecutionEnvironment> liveServers) {
        Collection<CompilerSetManager> allCSMs = new ArrayList<CompilerSetManager>();
        synchronized(canceled) {
            for (ExecutionEnvironment copiedServer : copiedManagers.keySet()) {
                if (liveServers == null || liveServers.contains(copiedServer)) {
                    allCSMs.add(copiedManagers.get(copiedServer));
                }
            }
            copiedManagers.clear();
        }
        CompilerSetManagerAccessorImpl.setManagers(allCSMs, liveServers);
    }

    public static void addChangeListener(ChangeListener l) {
        synchronized (changeListeners) {
            changeListeners.add(l);
        }
    }

    public static void removeChangeListener(ChangeListener l) {
        synchronized (changeListeners) {
            changeListeners.remove(l);
        }
    }

    private static void fireChange(ToolsCacheManagerImpl manager) {
        ChangeListener[] listenersCopy;
        synchronized (changeListeners) {
            listenersCopy = new ChangeListener[changeListeners.size()];
            changeListeners.toArray(listenersCopy);
        }
        ChangeEvent ev = new ChangeEvent(manager);
        for (ChangeListener l : listenersCopy) {
            l.stateChanged(ev);
        }
    }
}
