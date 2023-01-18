/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */

package org.netbeans.modules.debugger.jpda.js.breakpoints;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.request.EventRequest;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.api.debugger.LazyDebuggerManagerListener;
import org.netbeans.api.debugger.jpda.CallStackFrame;
import org.netbeans.api.debugger.jpda.ClassLoadUnloadBreakpoint;
import org.netbeans.api.debugger.jpda.ClassVariable;
import org.netbeans.api.debugger.jpda.InvalidExpressionException;
import org.netbeans.api.debugger.jpda.JPDABreakpoint;
import org.netbeans.api.debugger.jpda.JPDAClassType;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.debugger.jpda.LocalVariable;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.api.debugger.jpda.ObjectVariable;
import org.netbeans.api.debugger.jpda.This;
import org.netbeans.api.debugger.jpda.Variable;
import org.netbeans.api.debugger.jpda.event.JPDABreakpointEvent;
import org.netbeans.api.debugger.jpda.event.JPDABreakpointListener;
import org.netbeans.modules.debugger.jpda.js.JSUtils;
import org.netbeans.modules.debugger.jpda.js.source.ObservableSet;
import org.netbeans.modules.debugger.jpda.js.source.Source;
import org.netbeans.modules.javascript2.debug.breakpoints.JSLineBreakpoint;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.openide.util.Exceptions;

/**
 * Manages creation/removal of Java breakpoints corresponding to JS breakpoints.
 * 
 * @author Martin
 */
// Description of JS breakpoint submission:
// Initially, submit ClassLoadUnloadBreakpoint for JSUtils.NASHORN_SCRIPT+"*" (scriptBP)
// when scriptBP is hit (ScriptsHandler.breakpointReached()), we have JPDAClassType scriptType,
// but we do not know the script name yet.
// Submit a MethodBreakpoint for scriptType class name that hits any method.
// As soon as the method breakpoint is hit, we retrieve the source and submit JS breakpoints.

@DebuggerServiceRegistration(types=LazyDebuggerManagerListener.class)
public class JSJavaBreakpointsManager extends DebuggerManagerAdapter {
    
    private static final String NASHORN_CONTEXT_CLASS = "jdk.nashorn.internal.runtime.Context"; // NOI18N
    private static final String NASHORN_CONTEXT_SOURCE_BIND_METHOD = "cacheClass";    // NOI18N
    
    private static final String NASHORN_FUNCTION_NODE_CLASS = "jdk.nashorn.internal.ir.FunctionNode";   // NOI18N
    private static final String NASHORN_FUNCTION_NODE_SET_CLASS = "setRootClass";   // NOI18N
    
    private static final Logger LOG = Logger.getLogger(JSJavaBreakpointsManager.class.getName());
    
    private final Map<JPDADebugger, ScriptsHandler> scriptHandlers = new HashMap<>();
    private final Map<URLEquality, Set<JSLineBreakpoint>> breakpointsByURL = new HashMap<>();
    private ClassLoadUnloadBreakpoint scriptBP;
    private MethodBreakpoint sourceBindBP;
    private MethodBreakpoint functionClassBP;
    private final Object sourceBreakpointsInitLock = new Object();
    
    public JSJavaBreakpointsManager() {
    }

    @Override
    public Breakpoint[] initBreakpoints() {
        initSourceBreakpoints();
        return new Breakpoint[] { scriptBP, sourceBindBP, functionClassBP };
    }
    
    private void initSourceBreakpoints() {
        synchronized (sourceBreakpointsInitLock) {
            if (scriptBP == null) {
                scriptBP = ClassLoadUnloadBreakpoint.create(JSUtils.NASHORN_SCRIPT+"*",
                                                            false,
                                                            ClassLoadUnloadBreakpoint.TYPE_CLASS_LOADED);
                scriptBP.setHidden(true);
                scriptBP.setSuspend(EventRequest.SUSPEND_NONE);

                sourceBindBP = MethodBreakpoint.create(NASHORN_CONTEXT_CLASS, NASHORN_CONTEXT_SOURCE_BIND_METHOD);
                sourceBindBP.setHidden(true);
                sourceBindBP.setSuspend(EventRequest.SUSPEND_EVENT_THREAD);
                
                functionClassBP = MethodBreakpoint.create(NASHORN_FUNCTION_NODE_CLASS, NASHORN_FUNCTION_NODE_SET_CLASS);
                functionClassBP.setHidden(true);
                functionClassBP.setSuspend(EventRequest.SUSPEND_EVENT_THREAD);
            }
        }
    }
    
    @Override
    public String[] getProperties() {
        return new String[] { DebuggerManager.PROP_BREAKPOINTS_INIT,
                              DebuggerManager.PROP_BREAKPOINTS,
                              DebuggerManager.PROP_DEBUGGER_ENGINES };
    }

    @Override
    public void breakpointAdded(Breakpoint breakpoint) {
        if (!(breakpoint instanceof JSLineBreakpoint)) {
            return ;
        }
        JSLineBreakpoint jslb = (JSLineBreakpoint) breakpoint;
        URL url = jslb.getURL();
        URLEquality urle = new URLEquality(url);
        synchronized (breakpointsByURL) {
            Set<JSLineBreakpoint> bpts = breakpointsByURL.get(urle);
            if (bpts == null) {
                bpts = new HashSet<>();
                breakpointsByURL.put(urle, bpts);
            }
            bpts.add(jslb);
        }
        synchronized (scriptHandlers) {
            for (ScriptsHandler sh : scriptHandlers.values()) {
                sh.addBreakpoint(jslb);
            }
        }
    }

    @Override
    public void breakpointRemoved(Breakpoint breakpoint) {
        if (!(breakpoint instanceof JSLineBreakpoint)) {
            return ;
        }
        JSLineBreakpoint jslb = (JSLineBreakpoint) breakpoint;
        URL url = jslb.getURL();
        URLEquality urle = new URLEquality(url);
        synchronized (breakpointsByURL) {
            Set<JSLineBreakpoint> bpts = breakpointsByURL.get(urle);
            if (bpts != null) {
                bpts.remove(jslb);
                if (bpts.isEmpty()) {
                    breakpointsByURL.remove(urle);
                }
            }
        }
        synchronized (scriptHandlers) {
            for (ScriptsHandler sh : scriptHandlers.values()) {
                sh.removeBreakpoint(jslb);
            }
        }
    }

    @Override
    public void engineAdded(DebuggerEngine engine) {
        JPDADebugger debugger = engine.lookupFirst(null, JPDADebugger.class);
        if (debugger == null) {
            return ;
        }
        synchronized (scriptHandlers) {
            if (scriptHandlers.containsKey(debugger)) {
                return ;
            }
        }
        initSourceBreakpoints();
        ScriptsHandler sh = new ScriptsHandler(debugger);
        scriptBP.addJPDABreakpointListener(sh);
        sourceBindBP.addJPDABreakpointListener(sh);
        functionClassBP.addJPDABreakpointListener(sh);
        synchronized (scriptHandlers) {
            scriptHandlers.put(debugger, sh);
        }
    }

    @Override
    public void engineRemoved(DebuggerEngine engine) {
        JPDADebugger debugger = engine.lookupFirst(null, JPDADebugger.class);
        if (debugger == null) {
            return ;
        }
        ScriptsHandler sh;
        synchronized (scriptHandlers) {
            sh = scriptHandlers.remove(debugger);
        }
        if (sh != null) {
            scriptBP.removeJPDABreakpointListener(sh);
            sourceBindBP.removeJPDABreakpointListener(sh);
            functionClassBP.removeJPDABreakpointListener(sh);
            scriptBP.enable();
            sh.destroy();
        }
    }
    
    private final class ScriptsHandler implements JPDABreakpointListener {
        
        private final JPDADebugger debugger;
        private final Map<MethodBreakpoint, JPDAClassType> scriptAccessBreakpoints = new HashMap<>();
        private final Map<URLEquality, Source> sourcesByURL = new HashMap<>();
        private final Map<Long, Source> sourcesById = new HashMap<>();
        private final Map<JSLineBreakpoint, LineBreakpointHandler> lineBreakpointHandlers = new HashMap<>();
        private boolean isSourceBind = false;
        
        ScriptsHandler(JPDADebugger debugger) {
            this.debugger = debugger;
        }
        
        void addBreakpoint(JSLineBreakpoint jslb) {
            URLEquality urleq = new URLEquality(jslb.getURL());
            Source source;
            synchronized (sourcesByURL) {
                source = sourcesByURL.get(urleq);
            }
            if (source != null) {
                createSourceLineBreakpoints(jslb, source);
            }
        }

        /** A new script class is loaded/initialized. */
        @Override
        public void breakpointReached(JPDABreakpointEvent event) {
            if (debugger != event.getDebugger()) {
                return ;
            }
            if (scriptBP == event.getSource()) {
                // A new script class is loaded.
                Variable scriptClass = event.getVariable();
                if (!(scriptClass instanceof ClassVariable)) {
                    return ;
                }
                //JPDAClassType scriptType = ((ClassVariable) scriptClass).getReflectedType();
                JPDAClassType scriptType;
                try {
                    scriptType = (JPDAClassType) scriptClass.getClass().getMethod("getReflectedType").invoke(scriptClass);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                    scriptType = null;
                }
                if (scriptType != null) {
                    // script class method breakpoint so that we know when the script class is actually accessed
                    // we can load the source object only after the script class is initialized
                    MethodBreakpoint scriptMethodBP = MethodBreakpoint.create(scriptType.getName(), "*getMap");
                    scriptMethodBP.setHidden(true);
                    scriptMethodBP.setSuspend(EventRequest.SUSPEND_EVENT_THREAD);
                    scriptMethodBP.setSession(debugger);
                    scriptMethodBP.addJPDABreakpointListener(this);
                    DebuggerManager.getDebuggerManager().addBreakpoint(scriptMethodBP);
                    synchronized (scriptAccessBreakpoints) {
                        scriptAccessBreakpoints.put(scriptMethodBP, scriptType);
                    }
                }
            } else if (sourceBindBP == event.getSource()) {
                Variable sourceVar = null;
                Variable scriptClass = null;
                try {
                    CallStackFrame csf = event.getThread().getCallStack(0, 1)[0];
                    LocalVariable[] localVariables = csf.getLocalVariables();
                    for (LocalVariable lv : localVariables) {
                        switch (lv.getName()) {
                            case "source":  sourceVar = lv;
                                            break;
                            case "clazz":   scriptClass = lv;
                                            break;
                        }
                    }
                } catch (AbsentInformationException aiex) {
                }
                if (scriptClass instanceof ObjectVariable) {
                    try {
                        Object jdiVal = scriptClass.getClass().getMethod("getJDIValue").invoke(scriptClass);
                        scriptClass = (Variable) debugger.getClass().getMethod("getVariable", com.sun.jdi.Value.class).invoke(debugger, jdiVal);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (sourceVar instanceof ObjectVariable && scriptClass instanceof ClassVariable) {
                    JPDAClassType scriptType;
                    try {
                        scriptType = (JPDAClassType) scriptClass.getClass().getMethod("getReflectedType").invoke(scriptClass);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                        scriptType = null;
                    }
                    if (scriptType != null) {
                        Source source = Source.getSource(debugger, scriptType, (ObjectVariable) sourceVar);
                        if (source != null) {
                            sourceCreated(source);
                        }
                    }
                    if (!isSourceBind) {
                        isSourceBind = true;
                        scriptBP.disable();
                    }
                }
            } else if (functionClassBP == event.getSource()) {
                Variable rootClass = null;
                Variable sourceVar = null;
                try {
                    CallStackFrame csf = event.getThread().getCallStack(0, 1)[0];
                    LocalVariable[] localVariables = csf.getLocalVariables();
                    for (LocalVariable lv : localVariables) {
                        switch (lv.getName()) {
                            case "rootClass": //rootClass = lv;
                                              try {
                                                Object classValue = lv.getClass().getMethod("getJDIValue").invoke(lv);
                                                if (classValue != null) {
                                                    rootClass = (Variable) debugger.getClass().getMethod("getVariable", com.sun.jdi.Value.class).invoke(debugger, classValue);
                                                }
                                              } catch (Exception ex) {};
                                              break;
//                            case "source":    sourceVar = lv;
//                                              break;
                        }
                    }
                    This thisVariable = csf.getThisVariable();
                    if (thisVariable != null) {
                        sourceVar = thisVariable.getField("source");
                    }
                } catch (AbsentInformationException aiex) {
                }
                
                if (rootClass instanceof ClassVariable && sourceVar instanceof ObjectVariable) {
                    long sourceVarId = ((ObjectVariable) sourceVar).getUniqueID();
                    Source source;
                    synchronized (sourcesByURL) {
                        source = sourcesById.get(sourceVarId);
                    }
                    if (source != null) {
                        source.addFunctionClass((ClassVariable) rootClass);
                    }
                }
            } else {
                // script class is fully set up, let's retrieve the source:
                MethodBreakpoint scriptMethodBP = (MethodBreakpoint) event.getSource();
                DebuggerManager.getDebuggerManager().removeBreakpoint(scriptMethodBP);
                JPDAClassType scriptType;
                synchronized (scriptAccessBreakpoints) {
                    scriptType = scriptAccessBreakpoints.remove(scriptMethodBP);
                }
                Source source = Source.getSource(scriptType);
                if (source != null) {
                    sourceCreated(source);
                }
            }
            event.resume();
        }
        
        private void sourceCreated(Source source) {
            URL url = source.getUrl();
            if (url != null) {
                URLEquality urleq = new URLEquality(url);
                synchronized (sourcesByURL) {
                    sourcesByURL.put(urleq, source);
                    sourcesById.put(source.getSourceVarId(), source);
                    //classTypesBySource.put(source, scriptType);
                }
                createSourceLineBreakpoints(urleq, source);
            }
        }
        
        /**
         * @param jslb the added breakpoint
         */
        private void createSourceLineBreakpoints(JSLineBreakpoint jslb, Source source) {
            URL url = source.getUrl();
            if (url == null) {
                return ;
            }
            URLEquality urle = new URLEquality(url);
            URLEquality bpurle = new URLEquality(jslb.getURL());
            if (urle.equals(bpurle)) {
                LineBreakpointHandler lbh = new LineBreakpointHandler(debugger, jslb, source);
                synchronized (lineBreakpointHandlers) {
                    lineBreakpointHandlers.put(jslb, lbh);
                }
            }
        }
        
        /**
         * @param urle consider all available breakpoints at this location
         */
        private void createSourceLineBreakpoints(URLEquality urle, Source source) {
            URL url = source.getUrl();
            if (url == null) {
                return ;
            }
            Set<JSLineBreakpoint> bpts;
            synchronized (breakpointsByURL) {
                bpts = breakpointsByURL.get(urle);
                if (bpts != null) {
                    bpts = new HashSet<>(bpts);
                }
            }
            if (bpts != null) {
                for (JSLineBreakpoint bp : bpts) {
                    URLEquality bpurle = new URLEquality(bp.getURL());
                    if (urle.equals(bpurle)) {
                        LineBreakpointHandler lbh = new LineBreakpointHandler(debugger, bp, source);
                        synchronized (lineBreakpointHandlers) {
                            lineBreakpointHandlers.put(bp, lbh);
                        }
                    }
                }
            }
        }
        
        void removeBreakpoint(JSLineBreakpoint jslb) {
            LineBreakpointHandler lbh;
            synchronized (lineBreakpointHandlers) {
                lbh = lineBreakpointHandlers.remove(jslb);
            }
            if (lbh != null) {
                lbh.destroy();
            }
        }
        
        void destroy() {
            Set<MethodBreakpoint> mbs;
            synchronized (scriptAccessBreakpoints) {
                mbs = new HashSet<>(scriptAccessBreakpoints.keySet());
                scriptAccessBreakpoints.clear();
            }
            for (MethodBreakpoint mb : mbs) {
                DebuggerManager.getDebuggerManager().removeBreakpoint(mb);
            }
            synchronized (sourcesByURL) {
                sourcesByURL.clear();
                sourcesById.clear();
            }
            synchronized (lineBreakpointHandlers) {
                for (LineBreakpointHandler lbh : lineBreakpointHandlers.values()) {
                    lbh.destroy();
                }
                lineBreakpointHandlers.clear();
            }
        }

    }
    
    private static final class LineBreakpointHandler {
        
        private final JPDADebugger debugger;
        private final JSLineBreakpoint jslb;
        private final Source source;
        private final List<LineBreakpoint> lbs = Collections.synchronizedList(new LinkedList<LineBreakpoint>());
        private final List<PropertyChangeListener> bpPropertyListeners = Collections.synchronizedList(new LinkedList<PropertyChangeListener>());
        private final PropertyChangeListener functionClassChangesListener;
        
        LineBreakpointHandler(JPDADebugger debugger, JSLineBreakpoint jslb, Source source) {
            this.debugger = debugger;
            this.jslb = jslb;
            this.source = source;
            LineBreakpoint lb = createLineBreakpoint(source.getClassType());
            this.lbs.add(lb);
            functionClassChangesListener = new FunctionClassChangesListener();
            ObservableSet<JPDAClassType> functionClassTypes = source.getFunctionClassTypes();
            functionClassTypes.addPropertyChangeListener(functionClassChangesListener);
            for (JPDAClassType fct : functionClassTypes) {
                LineBreakpoint flb = createLineBreakpoint(fct);
                lbs.add(flb);
            }
        }
        
        private LineBreakpoint createLineBreakpoint(JPDAClassType classType) {
            int lineNo = jslb.getLineNumber();
            lineNo += source.getContentLineShift();
            LineBreakpoint lb = LineBreakpoint.create("", lineNo);
            lb.setHidden(true);
            //lb.setPreferredClassType(source.getClassType());
            setPreferredClassType(lb, classType);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("LineBreakpointHandler.createLineBreakpoint() classtype = "+source.getClassType().getName());
            }
            lb.setSuspend(JPDABreakpoint.SUSPEND_EVENT_THREAD);
            lb.setSession(debugger);
            if (!jslb.isEnabled()) {
                lb.disable();
            }
            lb.setCondition(jslb.getCondition());
            DebuggerManager.getDebuggerManager().addBreakpoint(lb);
            PropertyChangeListener bpPropertyListener = new BPPropertyListener(lb);
            jslb.addPropertyChangeListener(bpPropertyListener);
            bpPropertyListeners.add(bpPropertyListener);
            return lb;
        }
        
        void destroy() {
            for (LineBreakpoint lb : lbs) {
                DebuggerManager.getDebuggerManager().removeBreakpoint(lb);
            }
            for (PropertyChangeListener bpPropertyListener : bpPropertyListeners) {
                jslb.removePropertyChangeListener(bpPropertyListener);
            }
            source.getFunctionClassTypes().removePropertyChangeListener(functionClassChangesListener);
        }
        
        private void setPreferredClassType(LineBreakpoint lb, JPDAClassType classType) {
            try {
                Method setPreferredClassTypeMethod = lb.getClass().getMethod("setPreferredClassType", JPDAClassType.class);
                setPreferredClassTypeMethod.setAccessible(true);
                setPreferredClassTypeMethod.invoke(lb, classType);
            } catch (NoSuchMethodException | SecurityException |
                     IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
                lb.setPreferredClassName(source.getClassType().getName());
            }
        }
        
        private class FunctionClassChangesListener implements PropertyChangeListener {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (ObservableSet.PROP_ELM_ADDED.equals(evt.getPropertyName())) {
                    JPDAClassType ct = (JPDAClassType) evt.getNewValue();
                    LineBreakpoint lb = createLineBreakpoint(ct);
                    lbs.add(lb);
                }
            }
            
        }
        
        private static class BPPropertyListener implements PropertyChangeListener {
            
            private final LineBreakpoint lb;
            
            BPPropertyListener(LineBreakpoint lb) {
                this.lb = lb;
            }

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                switch (propertyName) {
                    case Breakpoint.PROP_ENABLED:
                        if (Boolean.TRUE.equals(evt.getNewValue())) {
                            lb.enable();
                        }
                        if (Boolean.FALSE.equals(evt.getNewValue())) {
                            lb.disable();
                        }
                        break;
                    case JSLineBreakpoint.PROP_CONDITION:
                        lb.setCondition((String) evt.getNewValue());
                        break;
                }
            }
        }
    }
    

}
