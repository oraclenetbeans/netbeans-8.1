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

package org.netbeans.modules.debugger.jpda.ui.debugging;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.jpda.DeadlockDetector;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.JPDAThreadGroup;
import org.netbeans.modules.debugger.jpda.JPDADebuggerImpl;
import org.netbeans.modules.debugger.jpda.models.JPDAThreadGroupImpl;
import org.netbeans.modules.debugger.jpda.models.JPDAThreadImpl;
import org.netbeans.modules.debugger.jpda.ui.models.DebuggingNodeModel;
import org.netbeans.modules.debugger.jpda.util.WeakCacheMap;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.ui.DebuggingView;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Martin Entlicher
 */
@DebuggingView.DVSupport.Registration(path="netbeans-JPDASession")
public class DebuggingViewSupportImpl extends DebuggingView.DVSupport {
    
    private final JPDADebuggerImpl debugger;
    private final Map<JPDAThreadImpl, JPDADVThread> threadsMap = new WeakCacheMap<>();
    private final Map<JPDAThreadGroupImpl, JPDADVThreadGroup> threadGroupsMap = new WeakCacheMap<>();
    
    public DebuggingViewSupportImpl(ContextProvider lookupProvider) {
        debugger = (JPDADebuggerImpl) lookupProvider.lookupFirst(null, JPDADebugger.class);
        ChangeListener chl = new ChangeListener();
        debugger.addPropertyChangeListener(chl);
        debugger.getThreadsCollector().getDeadlockDetector().addPropertyChangeListener(chl);
    }
    
    @Override
    public DebuggingView.DVThread getCurrentThread() {
        JPDAThreadImpl currentThread = (JPDAThreadImpl) debugger.getCurrentThread();
        if (currentThread != null &&
                !(currentThread.isSuspended() || currentThread.isSuspendedNoFire()) &&
                !currentThread.isMethodInvoking()) {
            currentThread = null;
        }
        return get(currentThread);
    }

    @Override
    public STATE getState() {
        int state = debugger.getState();
        if (state == JPDADebugger.STATE_DISCONNECTED) {
            return STATE.DISCONNECTED;
        } else {
            return STATE.RUNNING;
        }
    }

    @Override
    public List<DebuggingView.DVThread> getAllThreads() {
        List<JPDAThread> threads = debugger.getThreadsCollector().getAllThreads();
        List<DebuggingView.DVThread> dvThreads = new ArrayList<>(threads.size());
        for (JPDAThread t : threads) {
            dvThreads.add(get(t));
        }
        return Collections.unmodifiableList(dvThreads);
    }

    @Override
    public String getDisplayName(DebuggingView.DVThread thread) {
        String name;
        try {
            JPDAThread jt = ((JPDADVThread) thread).getKey();
            name = DebuggingNodeModel.getDisplayName(jt, false);
            Session session = debugger.getSession();
            Session currSession = DebuggerManager.getDebuggerManager().getCurrentSession();
            if (session != currSession) {
                String str = NbBundle.getMessage(DebuggingViewSupportImpl.class, "CTL_Session",
                        session.getName());
                name = name.charAt(0) + str + ", " + name.substring(1);
            }
        } catch (UnknownTypeException e) {
            name = thread.getName();
        }
        return name;
    }

    @Override
    public Image getIcon(DebuggingView.DVThread thread) {
        return ImageUtilities.loadImage(DebuggingNodeModel.getIconBase(((JPDADVThread) thread).getKey()));
    }

    @Override
    public Session getSession() {
        return debugger.getSession();
    }

    @Override
    public void resume() {
        debugger.resume();
    }

    @Override
    public Set<DebuggingView.Deadlock> getDeadlocks() {
        Set<DeadlockDetector.Deadlock> dds = debugger.getThreadsCollector().getDeadlockDetector().getDeadlocks();
        if (dds == null) {
            return null;
        }
        Set<DebuggingView.Deadlock> dvds = new HashSet<DebuggingView.Deadlock>(dds.size());
        for (DeadlockDetector.Deadlock dd : dds) {
            Collection threads = dd.getThreads();
            dvds.add(createDeadlock(threads));
        }
        return dvds;
    }

    @Override
    protected List<DebuggingView.DVFilter> getFilters() {
        List<DebuggingView.DVFilter> list = new ArrayList<DebuggingView.DVFilter>();
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showSuspendedThreadsOnly));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showThreadGroups));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showSuspendTable));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showSystemThreads));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showMonitors));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showQualifiedNames));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.sortSuspend));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.sortAlphabetic));
        list.add(DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.sortNatural));
        return list;
    }
    
    private static Preferences preferences;
    public static Preferences getFilterPreferences() {
        if (preferences == null) {
            preferences = DebuggingView.DVFilter.getDefault(DebuggingView.DVFilter.DefaultFilter.showThreadGroups).getPreferences();
        }
        return preferences;
    }
    
    public JPDADVThread get(JPDAThread t) {
        if (t == null) {
            return null;
        }
        JPDADVThread dvt;
        synchronized (threadsMap) {
            dvt = threadsMap.get(t);
            if (dvt == null) {
                dvt = new JPDADVThread(this, (JPDAThreadImpl) t);
                threadsMap.put((JPDAThreadImpl) t, dvt);
            }
        }
        return dvt;
    }
    
    public JPDADVThread[] get(JPDAThread[] threads) {
        int n = threads.length;
        JPDADVThread[] dvThreads = new JPDADVThread[n];
        for (int i = 0; i < n; i++) {
            dvThreads[i] = get((JPDAThreadImpl) threads[i]);
        }
        return dvThreads;

    }
    
    public JPDADVThreadGroup get(JPDAThreadGroup tg) {
        if (tg == null) {
            return null;
        }
        JPDADVThreadGroup dvtg;
        synchronized (threadGroupsMap) {
            dvtg = threadGroupsMap.get(tg);
            if (dvtg == null) {
                dvtg = new JPDADVThreadGroup(this, (JPDAThreadGroupImpl) tg);
                threadGroupsMap.put((JPDAThreadGroupImpl) tg, dvtg);
            }
        }
        return dvtg;
    }
    
    public JPDADVThreadGroup[] get(JPDAThreadGroup[] threadGroups) {
        int n = threadGroups.length;
        JPDADVThreadGroup[] dvGroups = new JPDADVThreadGroup[n];
        for (int i = 0; i < n; i++) {
            dvGroups[i] = get((JPDAThreadGroupImpl) threadGroups[i]);
        }
        return dvGroups;
    }
    
    private class ChangeListener implements PropertyChangeListener {
        
        private STATE state = STATE.DISCONNECTED;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (JPDADebugger.PROP_THREAD_STARTED.equals(propertyName)) {
                firePropertyChange(DebuggingView.DVSupport.PROP_THREAD_STARTED,
                                   get((JPDAThreadImpl) evt.getOldValue()),
                                   get((JPDAThreadImpl) evt.getNewValue()));
            } else
            if (JPDADebugger.PROP_THREAD_DIED.equals(propertyName)) {
                firePropertyChange(DebuggingView.DVSupport.PROP_THREAD_DIED,
                                   get((JPDAThreadImpl) evt.getOldValue()),
                                   get((JPDAThreadImpl) evt.getNewValue()));
            } else
            if (JPDADebugger.PROP_CURRENT_THREAD.equals(propertyName)) {
                firePropertyChange(DebuggingView.DVSupport.PROP_CURRENT_THREAD,
                                   get((JPDAThreadImpl) evt.getOldValue()),
                                   get((JPDAThreadImpl) evt.getNewValue()));
            } else
            if (JPDADebugger.PROP_STATE.equals(propertyName)) {
                int ds = debugger.getState();
                if (ds == JPDADebugger.STATE_RUNNING && this.state != STATE.RUNNING) {
                    this.state = STATE.RUNNING;
                    firePropertyChange(DebuggingView.DVSupport.PROP_STATE, STATE.DISCONNECTED, STATE.RUNNING);
                } else
                if (ds == JPDADebugger.STATE_DISCONNECTED) {
                    firePropertyChange(DebuggingView.DVSupport.PROP_STATE, STATE.RUNNING, STATE.DISCONNECTED);
                }
            } else
            if (DeadlockDetector.PROP_DEADLOCK.equals(propertyName)) {
                firePropertyChange(DebuggingView.DVSupport.PROP_DEADLOCK, evt.getOldValue(), evt.getNewValue());
            }
        }
        
    }
    
}
