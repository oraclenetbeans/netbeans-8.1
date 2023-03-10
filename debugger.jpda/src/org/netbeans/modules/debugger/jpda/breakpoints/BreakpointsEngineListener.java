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

package org.netbeans.modules.debugger.jpda.breakpoints;

import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerListener;
import org.netbeans.api.debugger.LazyActionsManagerListener;
import org.netbeans.api.debugger.Properties;
import org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.Watch;

import org.netbeans.api.debugger.jpda.ClassLoadUnloadBreakpoint;
import org.netbeans.api.debugger.jpda.ExceptionBreakpoint;
import org.netbeans.api.debugger.jpda.FieldBreakpoint;
import org.netbeans.api.debugger.jpda.JPDABreakpoint;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.api.debugger.jpda.ThreadBreakpoint;

import org.netbeans.modules.debugger.jpda.SourcePath;
import org.netbeans.modules.debugger.jpda.JPDADebuggerImpl;
import org.openide.util.Mutex;


/**
 * Listens on JPDADebugger.PROP_STATE and DebuggerManager.PROP_BREAKPOINTS, and
 * and creates XXXBreakpointImpl classes for all JPDABreakpoints.
 *
 * @author   Jan Jancura
 */
@LazyActionsManagerListener.Registration(path="netbeans-JPDASession/Java")
public class BreakpointsEngineListener extends LazyActionsManagerListener 
implements PropertyChangeListener, DebuggerManagerListener {
    
    private static final Logger logger = Logger.getLogger("org.netbeans.modules.debugger.jpda.breakpoints"); // NOI18N

    private JPDADebuggerImpl        debugger;
    private SourcePath              engineContext;
    private SourceRootsCache        sourceRootsCache;
    private boolean                 started = false;
    private Session                 session;
    private BreakpointsReader       breakpointsReader;


    public BreakpointsEngineListener (ContextProvider lookupProvider) {
        debugger = (JPDADebuggerImpl) lookupProvider.lookupFirst 
            (null, JPDADebugger.class);
        engineContext = lookupProvider.lookupFirst(null, SourcePath.class);
        sourceRootsCache = new SourceRootsCache(engineContext);
        session = lookupProvider.lookupFirst(null, Session.class);
        debugger.addPropertyChangeListener (
            JPDADebugger.PROP_STATE,
            this
        );
        breakpointsReader = PersistenceManager.findBreakpointsReader();
    }
    
    @Override
    protected void destroy () {
        debugger.removePropertyChangeListener (
            JPDADebugger.PROP_STATE,
            this
        );
        DebuggerManager.getDebuggerManager ().removeDebuggerListener (
            DebuggerManager.PROP_BREAKPOINTS,
            this
        );
        removeBreakpointImpls ();
    }
    
    @Override
    public String[] getProperties () {
        return new String[] {"asd"};
    }

    @Override
    public void propertyChange (java.beans.PropertyChangeEvent evt) {
        if (debugger.getState () == JPDADebugger.STATE_RUNNING) {
            if (started) return;
            started = true;
            DebuggerManager.getDebuggerManager ().addDebuggerListener (
                DebuggerManager.PROP_BREAKPOINTS,
                this
            );
            createBreakpointImpls ();
        }
        if (debugger.getState () == JPDADebugger.STATE_DISCONNECTED) {
            DebuggerManager.getDebuggerManager ().removeDebuggerListener (
                DebuggerManager.PROP_BREAKPOINTS,
                this
            );
            removeBreakpointImpls ();
            started = false;
        }
    }
    
    @Override
    public void actionPerformed (Object action) {
//        if (action == ActionsManager.ACTION_FIX)
//            fixBreakpointImpls ();
    }
    
    private boolean acceptBreakpoint(Breakpoint breakpoint) {
        if (breakpoint instanceof JPDABreakpoint) {
            JPDADebugger bDebugger = ((JPDABreakpoint) breakpoint).getSession();
            if (bDebugger == null || bDebugger.equals(debugger)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void breakpointAdded (final Breakpoint breakpoint) {
        if (!acceptBreakpoint(breakpoint)) {
            return ;
        }
        final boolean[] started = new boolean[] { false };
        if (!Mutex.EVENT.isReadAccess() && debugger.accessLock.readLock().tryLock()) { // Was already locked or can be easily acquired
            try {
                createBreakpointImpl ((JPDABreakpoint) breakpoint);
            } finally {
                debugger.accessLock.readLock().unlock();
            }
            return ;
        } // Otherwise:
        debugger.getRequestProcessor().post(new Runnable() {
            @Override
            public void run() {
                debugger.accessLock.readLock().lock();
                try {
                    synchronized (started) {
                        started[0] = true;
                        started.notify();
                    }
                    createBreakpointImpl ((JPDABreakpoint) breakpoint);
                } finally {
                    debugger.accessLock.readLock().unlock();
                }
            }
        });
        if (!Mutex.EVENT.isReadAccess()) { // AWT should not wait for debugger.LOCK
            synchronized (started) {
                if (!started[0]) {
                    try {
                        started.wait();
                    } catch (InterruptedException iex) {}
                }
            }
        }
    }    

    @Override
    public void breakpointRemoved (final Breakpoint breakpoint) {
        if (!acceptBreakpoint(breakpoint)) {
            return ;
        }
        final boolean[] started = new boolean[] { false };
        if (!Mutex.EVENT.isReadAccess() && debugger.accessLock.readLock().tryLock()) { // Was already locked or can be easily acquired
            try {
                removeBreakpointImpl (breakpoint);
            } finally {
                debugger.accessLock.readLock().unlock();
            }
            return ;
        } // Otherwise:
        debugger.getRequestProcessor().post(new Runnable() {
            @Override
            public void run() {
                debugger.accessLock.readLock().lock();
                try {
                    synchronized (started) {
                        started[0] = true;
                        started.notify();
                    }
                    removeBreakpointImpl (breakpoint);
                } finally {
                    debugger.accessLock.readLock().unlock();
                }
            }
        });
        if (!Mutex.EVENT.isReadAccess()) { // AWT should not wait for debugger.LOCK
            synchronized (started) {
                if (!started[0]) {
                    try {
                        started.wait();
                    } catch (InterruptedException iex) {}
                }
            }
        }
    }
    

    @Override
    public Breakpoint[] initBreakpoints () {return new Breakpoint [0];}
    @Override
    public void initWatches () {}
    @Override
    public void sessionAdded (Session session) {}
    @Override
    public void sessionRemoved (Session session) {}
    @Override
    public void watchAdded (Watch watch) {}
    @Override
    public void watchRemoved (Watch watch) {}
    @Override
    public void engineAdded (DebuggerEngine engine) {}
    @Override
    public void engineRemoved (DebuggerEngine engine) {}


    // helper methods ..........................................................
    
    private final Map<JPDABreakpoint, BreakpointImpl> breakpointToImpl = new IdentityHashMap<>();
    private final BreakpointImpl preliminaryBreakpointImpl = new PreliminaryBreakpointImpl();
    
    private void createBreakpointImpls () {
        Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
        int i, k = bs.length;
        for (i = 0; i < k; i++) {
            if (acceptBreakpoint(bs[i])) {
                createBreakpointImpl ((JPDABreakpoint) bs [i]);
            }
        }
        Properties p = Properties.getDefault().getProperties("debugger.options.JPDA");
        boolean doCatchExceptions = p.getBoolean("CatchExceptions", false);
        if (doCatchExceptions) {
            ExceptionBreakpoint eb = ExceptionBreakpoint.create(java.lang.Throwable.class.getName(), ExceptionBreakpoint.TYPE_EXCEPTION_UNCATCHED);
            createBreakpointImpl(eb);
        }
    }
    
    private void removeBreakpointImpls () {
        Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
        int i, k = bs.length;
        for (i = 0; i < k; i++) {
            boolean removed = removeBreakpointImpl (bs [i]);
            if (removed && bs[i] instanceof JPDABreakpoint) {
                JPDABreakpoint jb = (JPDABreakpoint) bs[i];
                JPDADebugger bDebugger = jb.getSession();
                if (bDebugger != null && bDebugger.equals(debugger)) {
                    // A hidden breakpoint submitted just for this one session. Remove it with the end of the session.
                    DebuggerManager.getDebuggerManager ().removeBreakpoint(jb);
                }
            }
        }
    }
    
    public BreakpointImpl getBreakpointImpl(Breakpoint bp) {
        synchronized (breakpointToImpl) {
            return breakpointToImpl.get(bp);
        }
    }
    
    public void fixBreakpointImpls () {
        List<BreakpointImpl> bpis;
        synchronized (breakpointToImpl) {
            bpis = new ArrayList<BreakpointImpl>(breakpointToImpl.size());
            Iterator<BreakpointImpl> i = breakpointToImpl.values ().iterator ();
            while (i.hasNext ()) {
                BreakpointImpl bpi = i.next();
                if (bpi == preliminaryBreakpointImpl) {
                    continue;
                }
                bpis.add(bpi);
            }
        }
        for (BreakpointImpl bpi : bpis) {
            bpi.fixed ();
        }
    }

    private void createBreakpointImpl (JPDABreakpoint b) {
        synchronized (breakpointToImpl) {
            if (breakpointToImpl.containsKey (b)) return;
            breakpointToImpl.put(b, preliminaryBreakpointImpl);
        }
        BreakpointImpl bpi;
        if (b instanceof LineBreakpoint) {
            bpi = new LineBreakpointImpl (
                    (LineBreakpoint) b,
                    breakpointsReader,
                    debugger,
                    session,
                    sourceRootsCache
                    );
        } else
        if (b instanceof ExceptionBreakpoint) {
            bpi = new ExceptionBreakpointImpl (
                    (ExceptionBreakpoint) b,
                    debugger,
                    session,
                    sourceRootsCache
                    );
        } else
        if (b instanceof MethodBreakpoint) {
            bpi = new MethodBreakpointImpl (
                    (MethodBreakpoint) b,
                    debugger,
                    session,
                    sourceRootsCache
                    );
        } else
        if (b instanceof FieldBreakpoint) {
            bpi = new FieldBreakpointImpl (
                    (FieldBreakpoint) b,
                    debugger,
                    session,
                    sourceRootsCache
                    );
        } else
        if (b instanceof ThreadBreakpoint) {
            bpi = new ThreadBreakpointImpl (
                    (ThreadBreakpoint) b,
                    debugger,
                    session
                    );
        } else
        if (b instanceof ClassLoadUnloadBreakpoint) {
            bpi = new ClassBreakpointImpl (
                    (ClassLoadUnloadBreakpoint) b,
                    debugger,
                    session,
                    sourceRootsCache
                    );
        } else {
            bpi = null;
        }
        BreakpointImpl bpiToRemove = null;
        synchronized (breakpointToImpl) {
            if (bpi == null) {
                breakpointToImpl.remove(b);
            } else {
                if (!breakpointToImpl.containsKey(b)) {
                    // There already was a request to remove this
                    bpiToRemove = bpi;
                } else {
                    breakpointToImpl.put(b, bpi);
                }
            }
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("BreakpointsEngineListener: created impl "+bpi+" for "+b);
        }
        if (bpiToRemove != null) {
            bpiToRemove.remove();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("BreakpointsEngineListener: removed impl "+bpiToRemove);
            }
        }
    }

    private boolean removeBreakpointImpl (Breakpoint b) {
        BreakpointImpl impl;
        synchronized (breakpointToImpl) {
            impl = breakpointToImpl.remove(b);
            if (impl == null) return false;
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("BreakpointsEngineListener: removed impl "+impl+" for "+b);
        }
        impl.remove ();
        return true;
    }

    private static final class PreliminaryBreakpointImpl extends BreakpointImpl {

        public PreliminaryBreakpointImpl() {
            super(null, null, null, null);
        }

        @Override
        protected void setRequests() {}

        @Override
        protected EventRequest createEventRequest(EventRequest oldRequest) throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean processCondition(Event event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean exec(Event event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removed(EventRequest eventRequest) {}

        @Override
        protected void remove() {
            // do nothing
        }

        @Override
        public int getCurrentHitCount() {
            return -1;
        }

        @Override
        public int getHitCountsTillBreak() {
            return -1;
        }

        @Override
        public void resetHitCounts() {
        }
        
    }
}
