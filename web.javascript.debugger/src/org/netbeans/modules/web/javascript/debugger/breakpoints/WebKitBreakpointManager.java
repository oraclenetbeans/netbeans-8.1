/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.javascript.debugger.breakpoints;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.project.Project;
import org.netbeans.modules.javascript2.debug.breakpoints.JSBreakpointStatus;
import org.netbeans.modules.javascript2.debug.breakpoints.JSLineBreakpoint;
import org.netbeans.modules.web.javascript.debugger.breakpoints.DOMNode.PathNotFoundException;
import org.netbeans.modules.web.javascript.debugger.browser.ProjectContext;
import org.netbeans.modules.web.webkit.debugging.api.BreakpointException;
import org.netbeans.modules.web.webkit.debugging.api.Debugger;
import org.netbeans.modules.web.webkit.debugging.api.WebKitDebugging;
import org.netbeans.modules.web.webkit.debugging.api.debugger.CallFrame;
import org.netbeans.modules.web.webkit.debugging.api.dom.Node;
import org.netbeans.modules.web.webkit.debugging.api.dom.NodeAnnotator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.text.Line;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Martin
 */
abstract class WebKitBreakpointManager implements PropertyChangeListener {
    
    private static final Logger LOG = Logger.getLogger(WebKitBreakpointManager.class.getName());
    protected final Debugger d;
    private final Breakpoint b;
    private static final RequestProcessor rp = new RequestProcessor(WebKitBreakpointManager.class);
    private static final Image DOM_BREAKPOINT_BADGE = ImageUtilities.loadImage("org/netbeans/modules/web/javascript/debugger/resources/DOMBreakpoint.png", false); // NOI18N

    protected WebKitBreakpointManager(Debugger d, Breakpoint b) {
        this.d = d;
        this.b = b;
        b.addPropertyChangeListener(this);
    }
    
    public static WebKitBreakpointManager create(Debugger d, ProjectContext pc, JSLineBreakpoint lb) {
        return new WebKitLineBreakpointManager(d, pc, lb);
    }
    
    public static WebKitBreakpointManager create(WebKitDebugging wd, ProjectContext pc, DOMBreakpoint db) {
        return new WebKitDOMBreakpointManager(wd, pc, db);
    }

    public static WebKitBreakpointManager create(Debugger d, EventsBreakpoint eb) {
        return new WebKitEventsBreakpointManager(d, eb);
    }
    
    public static WebKitBreakpointManager create(Debugger d, XHRBreakpoint xb) {
        return new WebKitXHRBreakpointManager(d, xb);
    }
    
    public boolean canAdd() {
        return b.isEnabled();
    }
    
    public abstract void add();

    public abstract void remove();

    public void destroy() {
        remove();
        b.removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (!Breakpoint.PROP_ENABLED.equals(event.getPropertyName())) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            rp.post(new Runnable() {
                @Override
                public void run() {
                    propertyChange(event);
                }
            });
            return ;
        }
        Breakpoint b = (Breakpoint) event.getSource();
        if (b.isEnabled()) {
            add();
        } else {
            remove();
        }
    }
    
    @NbBundle.Messages({
        "MSG_BRKP_Resolved=Successfully resolved at current line.",
        "MSG_BRKP_Unresolved=Not resolved/inactive at current line."
    })
    private static final class WebKitLineBreakpointManager extends WebKitBreakpointManager 
        implements Debugger.Listener, ChangeListener {
        
        private final JSLineBreakpoint lb;
        private org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b;
        private final Object brkptLock = new Object();
        private final AtomicBoolean lineChanged = new AtomicBoolean(false);
        private final AtomicBoolean resubmitting = new AtomicBoolean(false);
        private final ThreadLocal<Boolean> ignoreLineUpdate = new ThreadLocal<Boolean>();
        private ProjectContext pc;
        
        public WebKitLineBreakpointManager(Debugger d, ProjectContext pc, JSLineBreakpoint lb) {
            super(d, lb);
            this.lb = lb;
            this.pc = pc;
            pc.addChangeSupport(this);
        }

        @Override
        public void add() {
            synchronized (brkptLock) {
                if (b != null) {
                    return ;
                }
            }
            URL curl = d.getConnectionURL();
            if (curl != null && lb.getFileObject() != null) {
                String url = LineBreakpointUtils.getURLString(lb, pc.getProject(), curl);
                url = reformatFileURL(url);
                org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint br = null;
                try {
                    br = d.addLineBreakpoint(url, lb.getLine().getLineNumber(), null, lb.getCondition());
                } catch (BreakpointException bex) {
                    JSBreakpointStatus.setInvalid(lb, bex.getLocalizedMessage());
                }
                if (br != null) {
                    br.addPropertyChangeListener(this);
                    long brLine = br.getLineNumber();
                    if (brLine >= 0) {
                        List<Breakpoint> duplicateBreakpoints = checkDuplicateBreakpoints(lb, brLine);
                        if (duplicateBreakpoints != null) {
                            // Leave just the first one there:
                            for (int i = 1; i < duplicateBreakpoints.size(); i++) {
                                DebuggerManager.getDebuggerManager().removeBreakpoint(duplicateBreakpoints.get(i));
                            }
                            synchronized (brkptLock) {
                                b = br;
                            }
                            DebuggerManager.getDebuggerManager().removeBreakpoint(lb);
                            return ;
                        }
                        ignoreLineUpdate.set(Boolean.TRUE);
                        try {
                            lb.setLine((int) brLine);
                        } finally {
                            ignoreLineUpdate.remove();
                        }
                        JSBreakpointStatus.setValid(lb, Bundle.MSG_BRKP_Resolved());
                    } else {
                        JSBreakpointStatus.setInvalid(lb, Bundle.MSG_BRKP_Unresolved());
                    }
                    synchronized (brkptLock) {
                        b = br;
                    }
                    d.addListener(this);
                }
            }
        }

        @Override
        public void remove() {
            org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint brkpt;
            synchronized (brkptLock) {
                brkpt = b;
                if (brkpt == null) {
                    return ;
                }
            }
            brkpt.removePropertyChangeListener(this);
            d.removeListener(this);
            if (d.isEnabled()) {
                d.removeLineBreakpoint(brkpt);
            }
            synchronized (brkptLock) {
                b = null;
            }
            JSBreakpointStatus.resetValidity(lb);
        }
        
        private void resubmit() {
            if (SwingUtilities.isEventDispatchThread()) {
                rp.post(new Runnable() {
                    @Override
                    public void run() {
                        resubmit();
                    }
                });
                return ;
            }
            org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint brkpt;
            synchronized (brkptLock) {
                brkpt = b;
            }
            if (brkpt != null) {
                d.removeLineBreakpoint(brkpt);
                URL curl = d.getConnectionURL();
                if (curl != null) {
                    String url = LineBreakpointUtils.getURLString(lb, pc.getProject(), curl);
                    url = reformatFileURL(url);
                    resubmitting.set(false);
                    try {
                        brkpt = d.addLineBreakpoint(url, lb.getLine().getLineNumber(), null, lb.getCondition());
                    } catch (BreakpointException bex) {
                        brkpt = null;
                        JSBreakpointStatus.setInvalid(lb, bex.getLocalizedMessage());
                    }
                    synchronized (brkptLock) {
                        b = brkpt;
                    }
                }
            }
        }
        
        // changes "file:/some" to "file:///some"
        private static String reformatFileURL(String tabToDebug) {
            if (!tabToDebug.startsWith("file:")) {
                return tabToDebug;
            }
            tabToDebug = tabToDebug.substring(5);
            while (tabToDebug.length() > 0 && tabToDebug.startsWith("/")) {
                tabToDebug = tabToDebug.substring(1);
            }
            return "file:///"+tabToDebug;
        }

        @Override
        public void paused(List<CallFrame> callStack, String reason) {}

        @Override
        public void resumed() {}

        @Override
        public void reset() {
            if (lineChanged.getAndSet(false)) {
                resubmitting.set(true);
                rp.post(new Runnable() {
                    @Override
                    public void run() {
                        resubmit();
                        resubmitting.set(false);
                    }
                });
            }
        }

        @Override
        public void enabled(boolean enabled) {
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();
            if (JSLineBreakpoint.PROP_LINE.equals(propertyName)) {
                Boolean ignore = ignoreLineUpdate.get();
                if (ignore != null && ignore.booleanValue()) {
                    return ;
                }
                resubmit();
            } else if (JSLineBreakpoint.PROP_CONDITION.equals(propertyName)) {
                resubmit();
            } else if (JSLineBreakpoint.PROP_LINE_NUMBER.equals(propertyName) ||
                       JSLineBreakpoint.PROP_FILE.equals(propertyName)) {
                lineChanged.set(true);
            } else if (org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint.PROP_LOCATION.equals(propertyName)) {
                org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint brkpt;
                synchronized (brkptLock) {
                    brkpt = b;
                }
                if (brkpt == null) {
                    return ;
                }
                int lineNumber = (int) brkpt.getLineNumber();
                List<Breakpoint> duplicateBreakpoints = checkDuplicateBreakpoints(lb, lineNumber);
                if (duplicateBreakpoints != null) {
                    // Leave just the first one there:
                    for (int i = 1; i < duplicateBreakpoints.size(); i++) {
                        DebuggerManager.getDebuggerManager().removeBreakpoint(duplicateBreakpoints.get(i));
                    }
                    DebuggerManager.getDebuggerManager().removeBreakpoint(lb);
                    return ;
                }
                if (resubmitting.get()) {
                    // Ignore location update 
                    return ;
                }
                ignoreLineUpdate.set(Boolean.TRUE);
                try {
                    lb.setLine(lineNumber);
                } finally {
                    ignoreLineUpdate.remove();
                }
                if (lineNumber >= 0) {
                    JSBreakpointStatus.setValid(lb, Bundle.MSG_BRKP_Resolved());
                } else {
                    JSBreakpointStatus.setInvalid(lb, Bundle.MSG_BRKP_Unresolved());
                }
            } else {
                super.propertyChange(event);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            // project context has changed and line breakpoints need to
            // be refreshed:
            resubmit();
        }
        
    }
    
    /**
     * Check if there are already some existing breakpoints at line <code>lineNumber</code>
     * whose properties are equal to <code>lb</code>. If there are, delete them,
     * to get rid of duplicates.
     * 
     * @param lb
     * @param lineNumber
     */
    private static List<Breakpoint> checkDuplicateBreakpoints(JSLineBreakpoint lb, long lineNumber) {
        FileObject fo = lb.getLine().getLookup().lookup(FileObject.class);
        if (fo == null) {
            return null;
        }
        List<Breakpoint> breakpointsToRemove = null;
        for (Breakpoint b : DebuggerManager.getDebuggerManager().getBreakpoints()) {
            if (b instanceof JSLineBreakpoint) {
                if (b == lb) {
                    continue;
                }
                JSLineBreakpoint tlb = (JSLineBreakpoint) b;
                Line tl = tlb.getLine();
                if (tl == null) {
                    continue;
                }
                if (tl.getLineNumber() != lineNumber) {
                    continue;
                }
                FileObject tfo = tl.getLookup().lookup(FileObject.class);
                if (!fo.equals(tfo)) {
                    continue;
                }
                if (!compareBreakpointProperties(lb, tlb)) {
                    continue;
                }
                if (breakpointsToRemove == null) {
                    breakpointsToRemove = new ArrayList<Breakpoint>();
                }
                breakpointsToRemove.add(b);
            }
        }
        if (breakpointsToRemove != null) {
            // Remove all invalid breakpoints right away:
            for (int i = 0; i < breakpointsToRemove.size(); i++) {
                Breakpoint b = breakpointsToRemove.get(i);
                if (Breakpoint.VALIDITY.INVALID == b.getValidity()) {
                    DebuggerManager.getDebuggerManager().removeBreakpoint(b);
                    breakpointsToRemove.remove(b);
                    i--;
                }
            }
            if (breakpointsToRemove.isEmpty()) {
                breakpointsToRemove = null;
            }
        }
        return breakpointsToRemove;
    }
    
    /**
     * Compare two line breakpoints but ignore their lines.
     * @return <code>true</code> when the breakpoints equals, <code>false</code>
     * otherwise.
     */
    private static boolean compareBreakpointProperties(JSLineBreakpoint lb1, JSLineBreakpoint lb2) {
        String gn1 = lb1.getGroupName();
        String gn2 = lb2.getGroupName();
        if (gn1 != null && !gn1.equals(gn2) || gn1 == null && gn2 != null ||
            lb1.getHitCountFilter() != lb2.getHitCountFilter() ||
            lb1.getHitCountFilteringStyle() != lb2.getHitCountFilteringStyle()) {
            
            return false;
        }
        return true;
    }

    @NbBundle.Messages({
        "MSG_DOM_BRKP_Resolved=Successfully resolved on current node.",
        "MSG_DOM_BRKP_Unresolved=Not resolved/inactive on current node."
    })
    private static final class WebKitDOMBreakpointManager extends WebKitBreakpointManager implements ChangeListener {
        
        private final WebKitDebugging wd;
        private final ProjectContext pc;
        private final DOMBreakpoint db;
        private Node node;
        private Map<org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint, DOMBreakpoint.Type> bps;
        
        public WebKitDOMBreakpointManager(WebKitDebugging wd, ProjectContext pc, DOMBreakpoint db) {
            super(wd.getDebugger(), db);
            this.wd = wd;
            this.pc = pc;
            this.db = db;
            pc.addChangeSupport(this);
        }

        @Override
        public void add() {
            if (bps != null) {
                return ;
            }
            Project project = pc.getProject();
            if (project != null) {
                URL urlBP = db.getURL();
                FileObject fo = URLMapper.findFileObject(urlBP);
                if (fo != null) {
                    FileObject projectDirectory = project.getProjectDirectory();
                    if (!FileUtil.isParentOf(projectDirectory, fo)) {
                        // Belongs somewhere else
                        return;
                    }
                }
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("WebKitDOMBreakpointManager.add(): breakpoint URL = '"+db.getURL()+"'");
            }
            DOMNode dn = db.getNode();
            dn.addPropertyChangeListener(this);
            try {
                dn.bindTo(wd.getDOM());
            } catch (PathNotFoundException pex) {
                db.setValidity(pex);
                return ;
            }
            Node n = dn.getNode();
            if (n != null) {
                addTo(n);
            }
        }
        
        private void addTo(Node node) {
            this.node = node;
            Set<DOMBreakpoint.Type> types = db.getTypes();
            if (types.isEmpty()) {
                return ;
            }
            bps = new HashMap<org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint, DOMBreakpoint.Type>(types.size());
            boolean added = false;
            for (DOMBreakpoint.Type type : types) {
                org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b = 
                        d.addDOMBreakpoint(node, type.getTypeString());
                if (b != null) {
                    added = true;
                    bps.put(b, type);
                }
            }
            if (added) {
                db.setValid(Bundle.MSG_DOM_BRKP_Resolved());
            } else {
                db.setInvalid(Bundle.MSG_DOM_BRKP_Unresolved());
            }
            NodeAnnotator.getDefault().annotate(node, DOM_BREAKPOINT_BADGE);
        }

        @Override
        public void remove() {
            DOMNode dn = db.getNode();
            dn.unbind();
            dn.removePropertyChangeListener(this);
            removeBreakpoints();
        }
        
        private void removeBreakpoints() {
            Node theNode = this.node;
            this.node = null;
            if (theNode != null) {
                removeBreakpoints(theNode);
            }
        }
        
        private void removeBreakpoints(Node theNode) {
            if (bps == null) {
                return ;
            }
            if (d.isEnabled()) {
                for (org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b : bps.keySet()) {
                    if (b.getBreakpointID() != null) {
                        d.removeLineBreakpoint(b);
                    } else {
                        d.removeDOMBreakpoint(theNode, bps.get(b).getTypeString());
                    }
                }
                db.resetValidity();
            }
            bps = null;
            NodeAnnotator.getDefault().annotate(theNode, null);
        }

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if (SwingUtilities.isEventDispatchThread()) {
                rp.post(new Runnable() {
                    @Override
                    public void run() {
                        propertyChange(event);
                    }
                });
                return ;
            }
            String propertyName = event.getPropertyName();
            if (DOMNode.PROP_NODE_CHANGED.equals(propertyName)) {
                Node oldNode = (Node) event.getOldValue();
                if (oldNode != null) {
                    removeBreakpoints();
                }
                Node newNode = (Node) event.getNewValue();
                if (newNode != null) {
                    addTo(newNode);
                }
            } else if (DOMNode.PROP_NODE_PATH_FAILED.equals(propertyName)) {
                removeBreakpoints();
                db.setValidity((DOMNode.PathNotFoundException) event.getNewValue());
            } else if (DOMBreakpoint.PROP_TYPES.equals(propertyName)) {
                Node theNode = node;
                if (theNode != null) {
                    removeBreakpoints(theNode);
                    addTo(theNode);
                }
            } else if (DOMBreakpoint.PROP_NODE.equals(propertyName)) {
                DOMNode oldNode = (DOMNode) event.getOldValue();
                oldNode.unbind();
                oldNode.removePropertyChangeListener(this);
                removeBreakpoints();
                add();
            } else {
                super.propertyChange(event);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            // project context has changed and DOM breakpoint needs to be
            // refreshed:
            remove();
            add();
        }
        
    }
    
    private static final class WebKitEventsBreakpointManager extends WebKitBreakpointManager {
        
        private EventsBreakpoint eb;
        private Map<String, org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint> bps;

        public WebKitEventsBreakpointManager(Debugger d, EventsBreakpoint eb) {
            super(d, eb);
            this.eb = eb;
        }

        @Override
        public void add() {
            Set<String> events = eb.getEvents();
            bps = new HashMap<String, org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint>(events.size());
            for (String event : events) {
                org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b;
                if (eb.isInstrumentationEvent(event)) {
                    b = d.addInstrumentationBreakpoint(event);
                } else {
                    b = d.addEventBreakpoint(event);
                }
                if (b != null) {
                    bps.put(event, b);
                }
            }
            
        }

        @Override
        public void remove() {
            if (bps == null) {
                return ;
            }
            if (d.isEnabled()) {
                boolean removed = true;
                for (org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b : bps.values()) {
                    if (b.getBreakpointID() != null) {
                        d.removeLineBreakpoint(b);
                    } else {
                        removed = false;
                    }
                }
                if (!removed) {
                    Set<String> events = eb.getEvents();
                    for (String event : events) {
                        if (eb.isInstrumentationEvent(event)) {
                            d.removeInstrumentationBreakpoint(event);
                        } else {
                            d.removeEventBreakpoint(event);
                        }
                    }
                }
            }
            bps = null;
        }

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if (!eb.isEnabled()) {
                super.propertyChange(event);
                return ;
            }
            if (SwingUtilities.isEventDispatchThread()) {
                rp.post(new Runnable() {
                    @Override
                    public void run() {
                        propertyChange(event);
                    }
                });
                return ;
            }
            String propertyName = event.getPropertyName();
            if (EventsBreakpoint.PROP_EVENTS.equals(propertyName) && bps != null) {
                Object newValue = event.getNewValue();
                Object oldValue = event.getOldValue();
                if (newValue != null) {
                    String newEvent = (String) newValue;
                    org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b;
                    if (eb.isInstrumentationEvent(newEvent)) {
                        b = d.addInstrumentationBreakpoint(newEvent);
                    } else {
                        b = d.addEventBreakpoint(newEvent);
                    }
                    if (b != null) {
                        bps.put(newEvent, b);
                    }
                } else if (oldValue != null) {
                    String oldEvent = (String) oldValue;
                    org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b =
                            bps.remove(oldEvent);
                    if (b != null) {
                        if (eb.isInstrumentationEvent(oldEvent)) {
                            d.removeInstrumentationBreakpoint(oldEvent);
                        } else {
                            d.removeEventBreakpoint(oldEvent);
                        }
                    }
                } else { // total refresh
                    remove();
                    add();
                }
            } else {
                super.propertyChange(event);
            }
        }
        
    }
    
    private static final class WebKitXHRBreakpointManager extends WebKitBreakpointManager {

        private final XHRBreakpoint xb;
        private org.netbeans.modules.web.webkit.debugging.api.debugger.Breakpoint b;
        private String lastUrlSubstring;
        
        public WebKitXHRBreakpointManager(Debugger d, XHRBreakpoint xb) {
            super(d, xb);
            this.xb = xb;
        }

        @Override
        public void add() {
            if (b != null) {
                return ;
            }
            String urlSubstring = xb.getUrlSubstring();
            b = d.addXHRBreakpoint(urlSubstring);
            lastUrlSubstring = urlSubstring;
        }

        @Override
        public void remove() {
            if (b == null) {
                return ;
            }
            if (d.isEnabled()) {
                if (b.getBreakpointID() != null) {
                    d.removeLineBreakpoint(b);
                } else {
                    d.removeXHRBreakpoint(lastUrlSubstring);
                }
            }
            b = null;
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (XHRBreakpoint.PROP_URL_SUBSTRING.equals(event.getPropertyName())) {
                if (SwingUtilities.isEventDispatchThread()) {
                    rp.post(new Runnable() {
                        @Override
                        public void run() {
                            remove();
                            add();
                        }
                    });
                    return ;
                } else {
                    remove();
                    add();
                }
            } else {
                super.propertyChange(event);
            }
        }
        
    }

}
