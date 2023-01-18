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

package org.netbeans.modules.debugger.jpda;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.ClassType;
import com.sun.jdi.Locatable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

import com.sun.jdi.event.StepEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Properties;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.jpda.CallStackFrame;
import org.netbeans.api.debugger.jpda.JPDABreakpoint;
import org.netbeans.api.debugger.jpda.JPDAStep;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.api.debugger.jpda.SmartSteppingFilter;
import org.netbeans.api.debugger.jpda.Variable;
import org.netbeans.api.debugger.jpda.event.JPDABreakpointEvent;
import org.netbeans.api.debugger.jpda.event.JPDABreakpointListener;
import org.netbeans.modules.debugger.jpda.actions.CompoundSmartSteppingListener;
import org.netbeans.modules.debugger.jpda.actions.SmartSteppingFilterImpl;

import org.netbeans.modules.debugger.jpda.jdi.ClassNotPreparedExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ObjectCollectedExceptionWrapper;
import org.netbeans.modules.debugger.jpda.util.Executor;
import org.netbeans.modules.debugger.jpda.actions.StepIntoActionProvider;
import org.netbeans.modules.debugger.jpda.jdi.ClassTypeWrapper;
import org.netbeans.modules.debugger.jpda.jdi.IllegalThreadStateExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.InvalidRequestStateExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.InvalidStackFrameExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.LocatableWrapper;
import org.netbeans.modules.debugger.jpda.jdi.LocationWrapper;
import org.netbeans.modules.debugger.jpda.jdi.MethodWrapper;
import org.netbeans.modules.debugger.jpda.jdi.MirrorWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ReferenceTypeWrapper;
import org.netbeans.modules.debugger.jpda.jdi.StackFrameWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ThreadReferenceWrapper;
import org.netbeans.modules.debugger.jpda.jdi.TypeComponentWrapper;
import org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.VirtualMachineWrapper;
import org.netbeans.modules.debugger.jpda.jdi.event.EventWrapper;
import org.netbeans.modules.debugger.jpda.jdi.event.LocatableEventWrapper;
import org.netbeans.modules.debugger.jpda.jdi.request.BreakpointRequestWrapper;
import org.netbeans.modules.debugger.jpda.jdi.request.EventRequestManagerWrapper;
import org.netbeans.modules.debugger.jpda.jdi.request.EventRequestWrapper;
import org.netbeans.modules.debugger.jpda.jdi.request.StepRequestWrapper;
import org.netbeans.modules.debugger.jpda.models.JPDAThreadImpl;
import org.netbeans.modules.debugger.jpda.models.ReturnVariableImpl;
import org.netbeans.spi.debugger.jpda.EditorContext.Operation;
import org.openide.util.Exceptions;


public class JPDAStepImpl extends JPDAStep implements Executor {
    
    private static final Logger logger = Logger.getLogger("org.netbeans.modules.debugger.jpda.step"); // NOI18N

    private static final String INIT = "<init>"; // NOI18N

    /** The source tree with location info of this step */
    //private ASTL stepASTL;
    private Operation[] currentOperations;
    private Operation lastOperation;
    private ExpressionPool.Interval currentExpInterval;
    private MethodExitBreakpointListener lastMethodExitBreakpointListener;
    private Set<BreakpointRequest> operationBreakpoints;
    private StepRequest boundaryStepRequest;
    //private SingleThreadedStepWatch stepWatch;
    private final Set<EventRequest> requestsToCancel = new HashSet<EventRequest>();
    private volatile StepPatternDepth stepPatternDepth;
    private boolean ignoreStepFilters = false;
    private boolean steppingFromFilteredLocation;
    private boolean steppingFromCompoundFilteredLocation;
    private StopHereCheck stopHereCheck;
    private final Properties p;
    
    private final Session session;
    
    public JPDAStepImpl(JPDADebugger debugger, Session session, int size, int depth) {
        super(debugger, size, depth);
        this.session = session;
        p = Properties.getDefault().getProperties("debugger.options.JPDA"); // NOI18N
    }
    
    @Override
    public void addStep(JPDAThread tr) {
        JPDADebuggerImpl debuggerImpl = (JPDADebuggerImpl) debugger;
        JPDAThreadImpl trImpl = (JPDAThreadImpl) tr;
        VirtualMachine vm = debuggerImpl.getVirtualMachine();
        if (vm == null) {
            return ; // The session has finished
        }
        SourcePath sourcePath = ((JPDADebuggerImpl) debugger).getEngineContext();
        boolean[] setStoppedStateNoContinue = new boolean[] { false };
        int suspendPolicy = debuggerImpl.getSuspend();
        Lock lock;
        if (suspendPolicy == JPDADebugger.SUSPEND_EVENT_THREAD) {
            lock = trImpl.accessLock.writeLock();
        } else {
            lock = debuggerImpl.accessLock.writeLock();
        }
        lock.lock();
        try {
            ((JPDAThreadImpl) tr).waitUntilMethodInvokeDone();
            EventRequestManager erm = VirtualMachineWrapper.eventRequestManager(vm);
            //Remove all step requests -- TODO: Do we want it?
            List<StepRequest> stepRequests = EventRequestManagerWrapper.stepRequests(erm);
            try {
                EventRequestManagerWrapper.deleteEventRequests(erm, stepRequests);
            } catch (InvalidRequestStateExceptionWrapper irex) {
                List<StepRequest> assureDeletedAllstepRequests = new ArrayList<StepRequest>(stepRequests);
                for (StepRequest sr : assureDeletedAllstepRequests) {
                    try {
                        EventRequestManagerWrapper.deleteEventRequest(erm, sr);
                    } catch (InvalidRequestStateExceptionWrapper ex) {}
                }
            }
            for (StepRequest stepRequest : stepRequests) {
                //SingleThreadedStepWatch.stepRequestDeleted(stepRequest);
                debuggerImpl.getOperator().unregister(stepRequest);
            }
            steppingFromFilteredLocation = !getSmartSteppingFilterImpl ().stopHere(tr.getClassName());
            steppingFromCompoundFilteredLocation = !getCompoundSmartSteppingListener ().stopHere
                               (session, tr, getSmartSteppingFilterImpl ());
            int size = getSize();
            boolean stepAdded = false;
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Step "+((size == JPDAStep.STEP_OPERATION) ? "operation" : "line")
                        +" "+((getDepth() == JPDAStep.STEP_INTO) ? "into" :
                            ((getDepth() == JPDAStep.STEP_OVER) ? "over" : "out"))
                        +" in thread "+tr.getName()+", at: "+tr.getClassName()+"."+tr.getMethodName()+":"+tr.getLineNumber(null));
            }
            if (size == JPDAStep.STEP_OPERATION) {
                stepAdded = addOperationStep(trImpl, false, sourcePath,
                                             setStoppedStateNoContinue);
                if (!stepAdded) {
                    size = JPDAStep.STEP_LINE;
                    logger.log(Level.FINE, "Operation step changed to line step");
                }
            }
            logger.fine("  Operation step added = "+stepAdded);
            if (!stepAdded) {
                StepRequest stepRequest = EventRequestManagerWrapper.createStepRequest(
                    VirtualMachineWrapper.eventRequestManager(vm),
                    trImpl.getThreadReference(),
                    size,
                    getDepth()
                );
                //stepRequest.addCountFilter(1); - works bad with exclusion filters!
                String[] exclusionPatterns;
                if (ignoreStepFilters || steppingFromFilteredLocation) {
                    exclusionPatterns = null;
                } else {
                    exclusionPatterns = debuggerImpl.getSmartSteppingFilter().getExclusionPatterns();
                    for (int i = 0; i < exclusionPatterns.length; i++) {
                        StepRequestWrapper.addClassExclusionFilter(stepRequest, exclusionPatterns [i]);
                        logger.finer("   add pattern: "+exclusionPatterns[i]);
                    }
                }
                debuggerImpl.getOperator().register(stepRequest, this);
                EventRequestWrapper.setSuspendPolicy(stepRequest, debugger.getSuspend());
                boolean useStepFilters = p.getBoolean("UseStepFilters", true);
                boolean stepThrough = useStepFilters && p.getBoolean("StepThroughFilters", false);
                if (!stepThrough && exclusionPatterns != null && exclusionPatterns.length > 0) {
                    StepPatternDepth spd = new StepPatternDepth();
                    spd.exclusionPatterns = exclusionPatterns;
                    spd.stackDepth = tr.getStackDepth();
                    stepPatternDepth = spd;
                } else {
                    stepPatternDepth = null;
                }
                logger.fine("Set stepPatternDepth to "+stepPatternDepth);

                try {
                    EventRequestWrapper.enable(stepRequest);
                    trImpl.setInStep(true, stepRequest);
                    requestsToCancel.add(stepRequest);
                } catch (IllegalThreadStateException itsex) {
                    // the thread named in the request has died.
                    debuggerImpl.getOperator().unregister(stepRequest);
                    stepRequest = null;
                } catch (ObjectCollectedExceptionWrapper ex) {
                    debuggerImpl.getOperator().unregister(stepRequest);
                    stepRequest = null;
                } catch (InvalidRequestStateExceptionWrapper ex) {
                    debuggerImpl.getOperator().unregister(stepRequest);
                    stepRequest = null;
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Step request submitted: "+stepRequest+", size = "+size+", depth = "+getDepth());
                }
            }
        } catch (InternalExceptionWrapper ex) {
        } catch (VMDisconnectedExceptionWrapper ex) {
        } catch (ObjectCollectedExceptionWrapper ex) {
        } finally {
            lock.unlock();
        }
        if (setStoppedStateNoContinue[0]) {
            debuggerImpl.setStoppedStateNoContinue(trImpl.getThreadReference());
        }
    }
    
    private boolean addOperationStep(JPDAThreadImpl tr, boolean lineStepExec,
                                     SourcePath sourcePath,
                                     boolean[] setStoppedStateNoContinue) throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper, ObjectCollectedExceptionWrapper {
        ThreadReference trRef = tr.getThreadReference();
        StackFrame sf;
        try {
            sf = ThreadReferenceWrapper.frame(trRef, 0);
        } catch (IncompatibleThreadStateException itsex) {
            return false;
        } catch (IllegalThreadStateExceptionWrapper itsex) {
            return false;
        } catch (IndexOutOfBoundsException ioobex) {
            return false; // No frame exists?
        }
        Location loc = LocatableWrapper.location(sf);
        Session currentSession = DebuggerManager.getDebuggerManager().getCurrentSession();
        String language = currentSession == null ? null : currentSession.getCurrentLanguage();
        String url = sourcePath.getURL(loc, language);
        ExpressionPool exprPool = ((JPDADebuggerImpl) debugger).getExpressionPool();
        ExpressionPool.Expression expr = exprPool.getExpressionAt(loc, url);
        if (expr == null) {
            return false;
        }
        Operation[] ops = expr.getOperations();
        
        //Operation operation = null;
        int opIndex = -1;
        int codeIndex = (int) LocationWrapper.codeIndex(loc);
        if (codeIndex <= ops[0].getBytecodeIndex()) {
            if (!lineStepExec) {
                tr.clearLastOperations();
            }
            // We're at the beginning. Just take the first operation
            if (!ops[0].equals(tr.getCurrentOperation())) {
                opIndex = expr.findNextOperationIndex(codeIndex - 1);
                if (opIndex >= 0 && ops[opIndex].getBytecodeIndex() == codeIndex) {
                    tr.setCurrentOperation(ops[opIndex]);
                    if (lineStepExec) {
                        return false;
                    }
                    if (! getHidden()) {
                        setStoppedStateNoContinue[0] = true;
                    }
                    return true;
                }
            }
        }
        Operation currentOp = tr.getCurrentOperation();
        if (currentOp != null) {
            Operation theLastOperation = null;
            java.util.List<Operation> lastOperations = tr.getLastOperations();
            if (lastOperations != null && lastOperations.size() > 0) {
                theLastOperation = lastOperations.get(lastOperations.size() - 1);
            }
            if (theLastOperation == currentOp) {
                // We're right after some operation
                // Check, whether there is some other operation directly on this
                // position. If yes, it must be executed next.
                for (Operation op : ops) {
                    if (op.getBytecodeIndex() == codeIndex) {
                        tr.setCurrentOperation(op);
                        if (! getHidden()) {
                            setStoppedStateNoContinue[0] = true;
                        }
                        return true;
                    }
                }
            }
        }
        this.lastOperation = currentOp;
        VirtualMachine vm = MirrorWrapper.virtualMachine(loc);
        if (lastOperation != null) {
             // Set the method exit breakpoint to get the return value
            String methodName = lastOperation.getMethodName();
            // We can not get return values from constructors. Do not submit method exit breakpoint.
            if (methodName != null && !INIT.equals(methodName) &&
                vm.canGetMethodReturnValues()) {

                // TODO: Would be nice to know which ObjectReference we're executing the method on
                MethodBreakpoint mb = MethodBreakpoint.create(lastOperation.getMethodClassType(), methodName);
                mb.setClassFilters(createClassFilters(vm, lastOperation.getMethodClassType(), methodName));
                mb.setThreadFilters(debugger, new JPDAThread[] { tr });
                //mb.setMethodName(methodName);
                mb.setBreakpointType(MethodBreakpoint.TYPE_METHOD_EXIT);
                mb.setHidden(true);
                mb.setSuspend(JPDABreakpoint.SUSPEND_NONE);
                lastMethodExitBreakpointListener = new MethodExitBreakpointListener(mb);
                mb.addJPDABreakpointListener(lastMethodExitBreakpointListener);
                mb.setSession(debugger);
                DebuggerManager.getDebuggerManager().addBreakpoint(mb);
            }
        }
        tr.holdLastOperations(true);
        ExpressionPool.OperationLocation[] nextOperationLocations;
        if (opIndex < 0) {
            nextOperationLocations = expr.findNextOperationLocations(codeIndex);
        } else {
            Location[] locations = expr.getLocations();
            ExpressionPool.OperationLocation[] opLoc = new ExpressionPool.OperationLocation[ops.length - opIndex];
            int opLocIndex = 0;
            for (int i = opIndex; i < opLoc.length; i++) {
                if (i == opIndex || locations[i].codeIndex() > locations[opIndex].codeIndex()) {
                    opLoc[opLocIndex++] = new ExpressionPool.OperationLocation(ops[i], locations[i], i);
                }
            }
            if (opLocIndex == opLoc.length) {
                nextOperationLocations = opLoc;
            } else {
                nextOperationLocations = new ExpressionPool.OperationLocation[opLocIndex];
                System.arraycopy(opLoc, 0, nextOperationLocations, 0, opLocIndex);
            }
        }
        boolean isNextOperationFromDifferentExpression = false;
        if (nextOperationLocations != null) {
            //Location[] locations = expr.getLocations();
            /*if (opIndex < 0) {
                // search for an operation on the next line
                expr = exprPool.getExpressionAt(locations[locations.length - 1], url);
                if (expr == null) {
                    logger.log(Level.FINE, "No next operation is available.");
                    return false;
                }
                ops = expr.getOperations();
                opIndex = 0;
                locations = expr.getLocations();
            }*/
            this.operationBreakpoints = new HashSet<BreakpointRequest>();
            // We need to submit breakpoints on the desired operation and all subsequent ones,
            // because some might be skipped due to conditional execution.
            for (int ni = 0; ni < nextOperationLocations.length; ni++) {
                Location nloc = nextOperationLocations[ni].getLocation();
                if (nextOperationLocations[ni].getIndex() < 0) {
                    isNextOperationFromDifferentExpression = true;
                    Operation[] newOps = new Operation[ops.length + 1];
                    System.arraycopy(ops, 0, newOps, 0, ops.length);
                    newOps[ops.length] = nextOperationLocations[ni].getOperation();
                    ops = newOps;
                }
                BreakpointRequest brReq = EventRequestManagerWrapper.createBreakpointRequest(
                        VirtualMachineWrapper.eventRequestManager(vm),
                        nloc);
                operationBreakpoints.add(brReq);
                ((JPDADebuggerImpl) debugger).getOperator().register(brReq, this);
                EventRequestWrapper.setSuspendPolicy(brReq, debugger.getSuspend());
                BreakpointRequestWrapper.addThreadFilter(brReq, trRef);
                EventRequestWrapper.putProperty(brReq, "thread", trRef); // NOI18N
                try {
                    EventRequestWrapper.enable(brReq);
                } catch (InvalidRequestStateExceptionWrapper ex) {
                    Exceptions.printStackTrace(ex);
                }
                tr.setInStep(true, brReq);
                requestsToCancel.add(brReq);
            }
        } else if (lineStepExec) {
            return false;
        }
        
        // We need to also submit a step request so that we're sure that we end up at least on the next execution line
        /*
        //Location lastLocation = nextOperationLocations[nextOperationLocations.length - 1].getOperation().getMethod;
        int[] codeIndexIntervals = expr.getCodeIndexIntervals();
        int lastCodeIndex = codeIndexIntervals[codeIndexIntervals.length - 1];
        Location boundaryLocation = MethodWrapper.locationOfCodeIndex(LocationWrapper.method(loc), lastCodeIndex);
        BreakpointRequest brReq = EventRequestManagerWrapper.createBreakpointRequest(
                        VirtualMachineWrapper.eventRequestManager(vm),
                        boundaryLocation);
        ((JPDADebuggerImpl) debugger).getOperator().register(brReq, this);
        EventRequestWrapper.setSuspendPolicy(brReq, debugger.getSuspend());
        BreakpointRequestWrapper.addThreadFilter(brReq, trRef);
        EventRequestWrapper.putProperty(brReq, "thread", trRef); // NOI18N
        try {
            EventRequestWrapper.enable(brReq);
            requestsToCancel.add(brReq);
        } catch (InvalidRequestStateExceptionWrapper ex) {
            Exceptions.printStackTrace(ex);
            ((JPDADebuggerImpl) debugger).getOperator().unregister(brReq);
            brReq = null;
            return false;
        } finally {
            boundaryBreakpointRequest = brReq;
        }
        */
        boolean isBSR = setUpBoundaryStepRequest(
                         VirtualMachineWrapper.eventRequestManager(vm),
                         trRef,
                         isNextOperationFromDifferentExpression);
        if (!isBSR) {
            return false;
        }
        this.currentOperations = ops;
        this.currentExpInterval = expr.getInterval();
        return true;
    }
    
    private boolean setUpBoundaryStepRequest(EventRequestManager erm,
                                             ThreadReference trRef,
                                             boolean isNextOperationFromDifferentExpression)
                    throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper, ObjectCollectedExceptionWrapper {
        boundaryStepRequest = EventRequestManagerWrapper.createStepRequest(
            erm,
            trRef,
            StepRequest.STEP_LINE,
            StepRequest.STEP_OVER
        );
        if (isNextOperationFromDifferentExpression) {
            EventRequestWrapper.addCountFilter(boundaryStepRequest, 2);
        } else {
            EventRequestWrapper.addCountFilter(boundaryStepRequest, 1);
        }
        ((JPDADebuggerImpl) debugger).getOperator().register(boundaryStepRequest, this);
        EventRequestWrapper.setSuspendPolicy(boundaryStepRequest, debugger.getSuspend());
        try {
            EventRequestWrapper.enable (boundaryStepRequest);
            requestsToCancel.add(boundaryStepRequest);
        } catch (IllegalThreadStateException itsex) {
            // the thread named in the request has died.
            ((JPDADebuggerImpl) debugger).getOperator().unregister(boundaryStepRequest);
            boundaryStepRequest = null;
            return false;
        } catch (InvalidRequestStateExceptionWrapper ex) {
            Exceptions.printStackTrace(ex);
            ((JPDADebuggerImpl) debugger).getOperator().unregister(boundaryStepRequest);
            boundaryStepRequest = null;
            return false;
        }
        return true;
    }
    
    @Override
    public boolean exec (final Event event) {
        final EventRequest eventRequest;
        try {
            eventRequest = EventWrapper.request(event);
            stepDone(eventRequest);
        } catch (InternalExceptionWrapper ex) {
            return false;
        } catch (VMDisconnectedExceptionWrapper ex) {
            return false;
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JPDAStepImpl.exec("+event+"), is boundaryStepRequest = "+(eventRequest == boundaryStepRequest));
        }
        // TODO: Check the location, follow the smart-stepping logic!
        SourcePath sourcePath = ((JPDADebuggerImpl) debugger).getEngineContext();
        boolean stepAdded = false;
        boolean[] setStoppedStateNoContinue = new boolean[] { false };
        JPDADebuggerImpl debuggerImpl = (JPDADebuggerImpl)debugger;
        JPDAThreadImpl tr = (JPDAThreadImpl)debuggerImpl.getCurrentThread();
        tr.accessLock.readLock().lock();
        try {
            VirtualMachine vm = debuggerImpl.getVirtualMachine();
            if (vm == null) {
                return false; // The session has finished
            }
            if (currentOperations != null) {
                if (eventRequest == boundaryStepRequest) {
                    // A line step was finished, we need to check if the execution
                    // of current expression has finished or not...
                    try {
                        Location loc = LocatableWrapper.location((Locatable) event);
                        final String language = session == null ? null : session.getCurrentLanguage();
                        int l = LocationWrapper.lineNumber(loc, language);
                        if (currentExpInterval.contains(l)) {
                            // The expression did not finish yet, we're suspended
                            // somewhere in the middle. Continue...
                            EventRequestManager erm = VirtualMachineWrapper.eventRequestManager(vm);
                            try {
                                EventRequestManagerWrapper.deleteEventRequest(erm, eventRequest);
                            } catch (InvalidRequestStateExceptionWrapper ex) {}
                            // silently unregister the old boundary step
                            EventRequestWrapper.putProperty (eventRequest, "executor", null); // NOI18N
                            boolean isBSR;
                            try {
                                isBSR = setUpBoundaryStepRequest(
                                        erm,
                                        tr.getThreadReference(),
                                        false);
                            } catch (ObjectCollectedExceptionWrapper ex) {
                                isBSR = false;
                            }
                            // We're in the middle of the expression,
                            // continue if we manage to submit another boundary step
                            return isBSR;
                        }
                    } catch (InternalExceptionWrapper ex) {
                    } catch (VMDisconnectedExceptionWrapper ex) {
                        return false;
                    }
                }
            }
            
            Variable returnValue = null;
            MethodExitBreakpointListener mebl = lastMethodExitBreakpointListener;
            if (mebl != null) {
                returnValue = mebl.getReturnValue();
            }
            try {
                EventRequestManager erm = VirtualMachineWrapper.eventRequestManager(vm);
                try {
                    EventRequestManagerWrapper.deleteEventRequest(erm, eventRequest);
                } catch (InvalidRequestStateExceptionWrapper ex) {}
                debuggerImpl.getOperator().unregister(eventRequest);
                /*if (eventRequest instanceof StepRequest) {
                    SingleThreadedStepWatch.stepRequestDeleted((StepRequest) eventRequest);
                }*/
                removed(eventRequest); // Clean-up
            } catch (InternalExceptionWrapper ex) {
            } catch (VMDisconnectedExceptionWrapper ex) {
                return false;
            }
            if (lastMethodExitBreakpointListener != null) {
                lastMethodExitBreakpointListener.destroy();
                lastMethodExitBreakpointListener = null;
            }
            if (mebl != null) {
                lastOperation.setReturnValue(returnValue);
            } else if (vm.canGetMethodReturnValues() &&
                       lastOperation != null && INIT.equals(lastOperation.getMethodName())) {
                // Set Void as a return value of constructor:
                lastOperation.setReturnValue(new ReturnVariableImpl((JPDADebuggerImpl) debugger, vm.mirrorOfVoid(), "", INIT));
            }
            if (lastOperation != null) {
                tr.addLastOperation(lastOperation);
            }
            logger.fine("Have stepPatternDepth : "+stepPatternDepth);
            if (stepPatternDepth != null) {
                StepPatternDepth newStepPatternDepth = null;
                try {
                    int sd = tr.getStackDepth();
                    logger.fine("Current stack depth = "+sd);
                    if (sd > (stepPatternDepth.stackDepth + 1)) {
                        // There are some (possibly filtered) stack frames in between.
                        // StepThroughFilters is false, therefore we should step out if we can not stop here:
                        boolean haveFilteredClassOnStack = false;
                        if (!steppingFromFilteredLocation) {
                            CallStackFrame[] callStack = tr.getCallStack();
                            int c1 = 1;
                            int c2 = callStack.length - stepPatternDepth.stackDepth;
                            for (int i = c1; i < c2; i++) {
                                // TODO: use debuggerImpl.stopHere(callStack[i])
                                String className = callStack[i].getClassName();
                                if (stepPatternDepth.isFiltered(className)) {
                                    haveFilteredClassOnStack = true;
                                    break;
                                }
                            }
                            logger.fine("haveFilteredClassOnStack = "+haveFilteredClassOnStack);
                        }
                        if (haveFilteredClassOnStack) {
                            StepRequest stepRequest = EventRequestManagerWrapper.createStepRequest(
                                VirtualMachineWrapper.eventRequestManager(vm),
                                tr.getThreadReference(),
                                StepRequest.STEP_LINE,
                                StepRequest.STEP_OUT
                            );
                            EventRequestWrapper.addCountFilter(stepRequest, 1);
                            String[] exclusionPatterns = debuggerImpl.getSmartSteppingFilter().getExclusionPatterns();
                            // JDI is inconsistent!!! Step into steps *through* filters, but step out does *NOT*
                            //for (int i = 0; i < exclusionPatterns.length; i++) {
                                //StepRequestWrapper.addClassExclusionFilter(stepRequest, exclusionPatterns [i]);
                            //}
                            if (sd > (stepPatternDepth.stackDepth + 2)) {
                                // There's still something perhaps filterable in beteen
                                newStepPatternDepth = new StepPatternDepth();
                                newStepPatternDepth.exclusionPatterns = exclusionPatterns;
                                newStepPatternDepth.stackDepth = stepPatternDepth.stackDepth;
                            }
                            
                            debuggerImpl.getOperator ().register (stepRequest, this);
                            EventRequestWrapper.setSuspendPolicy (stepRequest, debugger.getSuspend ());
                            try {
                                EventRequestWrapper.enable (stepRequest);
                                requestsToCancel.add(stepRequest);
                            } catch (IllegalThreadStateException itsex) {
                                // the thread named in the request has died.
                                debuggerImpl.getOperator ().unregister (stepRequest);
                            } catch (InvalidRequestStateExceptionWrapper irse) {
                                Exceptions.printStackTrace(irse);
                            }
                            return true;
                        }
                    }
                } catch (AbsentInformationException aiex) {
                } catch (InternalExceptionWrapper iex) {
                } catch (ObjectCollectedExceptionWrapper ocex) {
                } catch (VMDisconnectedExceptionWrapper vmdex) {
                    return false;
                } finally {
                    stepPatternDepth = newStepPatternDepth;
                }
            }
            Operation currentOperation = null;
            boolean addExprStep = false;
            if (currentOperations != null) {
                try {
                    if (eventRequest instanceof BreakpointRequest) {
                        long codeIndex = LocationWrapper.codeIndex(
                                BreakpointRequestWrapper.location((BreakpointRequest) eventRequest));
                        for (int i = 0; i < currentOperations.length; i++) {
                            if (currentOperations[i].getBytecodeIndex() == codeIndex) {
                                currentOperation = currentOperations[i];
                                break;
                            }
                        }
                    } else {
                        // A line step was finished, the execution of current expression
                        // has finished, we need to check the expression on this line.
                        // We already know, that currentExpInterval does not contain the line
                        // Check expressions on this line
                        addExprStep = true;
                    }
                } catch (InternalExceptionWrapper ex) {
                } catch (VMDisconnectedExceptionWrapper ex) {
                    return false;
                }
                this.currentOperations = null;
            }
            logger.fine("Current operation = "+currentOperation+", addExprStep = "+addExprStep);
            tr.setCurrentOperation(currentOperation);
            try {
                //int suspendPolicy = debugger.getSuspend();
                if (addExprStep) {
                    try {
                        stepAdded = addOperationStep(tr, true, sourcePath,
                                                     setStoppedStateNoContinue);
                    } catch (InternalExceptionWrapper ex) {
                        return false;
                    } catch (ObjectCollectedExceptionWrapper ex) {
                        return false;
                    }
                }
                logger.fine("stepAdded = "+stepAdded);
                if (!stepAdded) {
                    if ((eventRequest instanceof StepRequest) && shouldNotStopHere((StepEvent) event)) {
                        logger.fine("We should not stop here => resuming");
                        return true; // Resume
                    }
                }
            } catch (VMDisconnectedExceptionWrapper ex) {
                return false;
            }
        } finally {
            tr.accessLock.readLock().unlock();
        }
        if (stepAdded) {
            if (setStoppedStateNoContinue[0]) {
                debuggerImpl.setStoppedStateNoContinue(tr.getThreadReference());
            }
            return true; // Resume
        }
        firePropertyChange(PROP_STATE_EXEC, null, null);
        if (getHidden()) {
            return true; // Resume
        } else {
            tr.holdLastOperations(false);
            return false;
        }
    }
    
    @Override
    public void removed(EventRequest eventRequest) {
        try {
            stepDone(eventRequest);
        } catch (InternalExceptionWrapper ex) {
        } catch (VMDisconnectedExceptionWrapper ex) {
            return ;
        }
        if (lastMethodExitBreakpointListener != null) {
            lastMethodExitBreakpointListener.destroy();
            lastMethodExitBreakpointListener = null;
        }
        JPDADebuggerImpl debuggerImpl = (JPDADebuggerImpl)debugger;
        VirtualMachine vm = debuggerImpl.getVirtualMachine();
        if (vm == null) {
            return ; // The session has finished
        }
        try {
            EventRequestManager erm = VirtualMachineWrapper.eventRequestManager(vm);
            if (operationBreakpoints != null) {
                for (Iterator<BreakpointRequest> it = operationBreakpoints.iterator(); it.hasNext(); ) {
                    BreakpointRequest br = it.next();
                    try {
                        EventRequestManagerWrapper.deleteEventRequest(erm, br);
                    } catch (InvalidRequestStateExceptionWrapper ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    debuggerImpl.getOperator().unregister(br);
                }
                this.operationBreakpoints = null;
            }
            if (boundaryStepRequest != null) {
                try {
                    EventRequestManagerWrapper.deleteEventRequest(erm, boundaryStepRequest);
                } catch (InvalidRequestStateExceptionWrapper ex) {
                    Exceptions.printStackTrace(ex);
                }
                //SingleThreadedStepWatch.stepRequestDeleted(boundaryStepRequest);
                debuggerImpl.getOperator().unregister(boundaryStepRequest);
            }
        } catch (VMDisconnectedExceptionWrapper e) {
        } catch (InternalExceptionWrapper e) {
        }
        
    }

    private void stepDone(EventRequest er) throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper {
        JPDAThreadImpl t;
        if (er instanceof StepRequest) {
            StepRequest sr = (StepRequest) er;
            t = ((JPDADebuggerImpl) debugger).getThread(StepRequestWrapper.thread(sr));
        } else {
            ThreadReference tr = (ThreadReference) EventRequestWrapper.getProperty(er, "thread"); // NOI18N
            if (tr != null) {
                t = ((JPDADebuggerImpl) debugger).getThread(tr);
            } else {
                t = null;
            }
        }
        if (t != null) {
            t.setInStep(false, null);
        }
    }

    /**
     * Returns all class names, which are subclasses of <code>className</code>
     * and contain method <code>methodName</code>
     */
    private static String[] createClassFilters(VirtualMachine vm, String className, String methodName) throws VMDisconnectedExceptionWrapper {
        return createClassFilters(vm, className, methodName, new ArrayList<String>()).toArray(new String[] {});
    }
    
    private static List<String> createClassFilters(VirtualMachine vm, String className, String methodName, List<String> filters) throws VMDisconnectedExceptionWrapper {
        List<ReferenceType> classTypes = VirtualMachineWrapper.classesByName0(vm, className);
        for (ReferenceType type : classTypes) {
            try {
                List<Method> methods;
                try {
                    methods = ReferenceTypeWrapper.methodsByName0(type, methodName);
                } catch (ClassNotPreparedExceptionWrapper ex) {
                    continue;
                }
                boolean hasNonStatic = methods.isEmpty();
                for (Method method : methods) {
                    if (!filters.contains(ReferenceTypeWrapper.name(type))) {
                        filters.add(ReferenceTypeWrapper.name(type));
                    }
                    if (!TypeComponentWrapper.isStatic(method)) {
                        hasNonStatic = true;
                    }
                }
                if (hasNonStatic && type instanceof ClassType) {
                    ClassType clazz = (ClassType) type;
                    ClassType superClass;
                    superClass = ClassTypeWrapper.superclass(clazz);
                    if (superClass != null) {
                        createClassFilters(vm, ReferenceTypeWrapper.name(superClass), methodName, filters);
                    }
                }
            } catch (InternalExceptionWrapper ex) {
            } catch (ObjectCollectedExceptionWrapper ex) {
            }
        }
        return filters;
    }
    
    /**
     * Checks for synthetic methods and smart-stepping...
     */
    private boolean shouldNotStopHere(StepEvent event) {
        JPDADebuggerImpl debuggerImpl = (JPDADebuggerImpl) debugger;
        // 2) init info about current state
        boolean useStepFilters = p.getBoolean("UseStepFilters", true);
        boolean stepThrough = useStepFilters && p.getBoolean("StepThroughFilters", false);
        try {
            ThreadReference tr = LocatableEventWrapper.thread (event);
            JPDAThreadImpl t = debuggerImpl.getThread (tr);
            t.accessLock.readLock().lock();
            try {
                try {
                    if (!ThreadReferenceWrapper.isSuspended(tr)) {
                        return false;   // Already running.
                    }
                    // Synthetic method?
                    Location loc = StackFrameWrapper.location(ThreadReferenceWrapper.frame(tr, 0));
                    Method m = LocationWrapper.method(loc);
                    boolean doStepAgain = false;
                    int doStepDepth = getDepth();

                    boolean filterSyntheticMethods = useStepFilters && p.getBoolean("FilterSyntheticMethods", true);
                    boolean filterStaticInitializers = useStepFilters && p.getBoolean("FilterStaticInitializers", false);
                    boolean filterConstructors = useStepFilters && p.getBoolean("FilterConstructors", false);
                    int syntheticStep = isSyntheticMethod(m, loc);
                    if (filterSyntheticMethods && syntheticStep != 0) {
                        //S ystem.out.println("In synthetic method -> STEP INTO again");
                        doStepAgain = true;
                        if (syntheticStep > 0) {
                            doStepDepth = syntheticStep;
                        }
                    }
                    if (filterStaticInitializers && MethodWrapper.isStaticInitializer(m) ||
                        filterConstructors && MethodWrapper.isConstructor(m)) {
                        
                        doStepAgain = true;
                        doStepDepth = StepRequest.STEP_OUT;
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Method"+m+" is a static initializer, or constructor - will step out.");
                        }
                    }
                    if (useStepFilters && !ignoreStepFilters && !doStepAgain) {
                        boolean stop;
                        if (steppingFromCompoundFilteredLocation) {
                            stop = true;
                        } else {
                            stop = getCompoundSmartSteppingListener ().stopHere
                                   (session, t, debuggerImpl.getSmartSteppingFilter());
                        }
                        if (stop && !steppingFromFilteredLocation) {
                            String[] exclusionPatterns = debuggerImpl.getSmartSteppingFilter().getExclusionPatterns();
                            String className = ReferenceTypeWrapper.name(LocationWrapper.declaringType(loc));
                            for (String pattern : exclusionPatterns) {
                                if (match(className, pattern)) {
                                    stop = false;
                                    break;
                                }
                            }
                        }
                        if (!stop) {
                            doStepAgain = true;
                            EventRequest request = EventWrapper.request(event);
                            if (request instanceof StepRequest) {
                                doStepDepth = ((StepRequest) request).depth();
                            }
                        }
                    }
                    
                    if (stopHereCheck != null) {
                        doStepAgain = !stopHereCheck.stopHere(!doStepAgain);
                    }

                    if (doStepAgain) {
                        //S ystem.out.println("In synthetic method -> STEP OVER/OUT again");

                        VirtualMachine vm = debuggerImpl.getVirtualMachine ();
                        if (vm == null) {
                            return false; // The session has finished
                        }
                        StepRequest stepRequest = EventRequestManagerWrapper.createStepRequest(
                            VirtualMachineWrapper.eventRequestManager(vm),
                            tr,
                            StepRequest.STEP_LINE,
                            doStepDepth
                        );
                        //EventRequestWrapper.addCountFilter(stepRequest, 1);
                        String[] exclusionPatterns;
                        if (ignoreStepFilters || steppingFromFilteredLocation) {
                            exclusionPatterns = null;
                        } else {
                            exclusionPatterns = debuggerImpl.getSmartSteppingFilter().getExclusionPatterns();
                            if (doStepDepth != StepRequest.STEP_OUT) {
                                for (int i = 0; i < exclusionPatterns.length; i++) {
                                    StepRequestWrapper.addClassExclusionFilter(stepRequest, exclusionPatterns [i]);
                                }
                            }
                        }
                        debuggerImpl.getOperator ().register (stepRequest, this);
                        EventRequestWrapper.setSuspendPolicy (stepRequest, debugger.getSuspend ());
                        if (!stepThrough && exclusionPatterns != null && exclusionPatterns.length > 0) {
                            StepPatternDepth spd = new StepPatternDepth();
                            spd.exclusionPatterns = exclusionPatterns;
                            spd.stackDepth = t.getStackDepth();
                            stepPatternDepth = spd;
                        } else {
                            stepPatternDepth = null;
                        }
                        logger.fine("Set stepPatternDepth to "+stepPatternDepth);
                        try {
                            EventRequestWrapper.enable (stepRequest);
                            requestsToCancel.add(stepRequest);
                        } catch (IllegalThreadStateException itsex) {
                            // the thread named in the request has died.
                            debuggerImpl.getOperator ().unregister (stepRequest);
                        } catch (InvalidRequestStateExceptionWrapper irse) {
                            Exceptions.printStackTrace(irse);
                        }
                        return true;
                    }
                } catch (IncompatibleThreadStateException | InvalidStackFrameExceptionWrapper e) {
                    Exceptions.printStackTrace(e);
                    return false;
                } catch (IllegalThreadStateExceptionWrapper | ObjectCollectedExceptionWrapper e) {
                    return false;
                }

                // Not synthetic
                if (debuggerImpl.stopHere(t)) {
                    //S ystem.out.println("/nStepAction.exec end - do not resume");
                    return false; // do not resume
                }

                // do not stop here -> start smart stepping!
                VirtualMachine vm = debuggerImpl.getVirtualMachine ();
                if (vm == null) {
                    return false; // The session has finished
                }
                int depth;
                Map properties = session.lookupFirst(null, Map.class);
                boolean smartSteppingStepOut = properties != null && properties.containsKey (StepIntoActionProvider.SS_STEP_OUT);
                if (!stepThrough || smartSteppingStepOut) {
                    depth = StepRequest.STEP_OUT;
                } else {
                    depth = ((StepRequest) event.request()).depth(); // Use the original depth instead of StepRequest.STEP_INTO, which we do not want when stepping over or out.
                }
                StepRequest stepRequest = EventRequestManagerWrapper.createStepRequest(
                    VirtualMachineWrapper.eventRequestManager(vm),
                    tr,
                    StepRequest.STEP_LINE,
                    depth
                );
                if (logger.isLoggable(Level.FINE)) {
                    try {
                        logger.fine("Can not stop at " + ThreadReferenceWrapper.frame(tr, 0) + ", smart-stepping. Submitting step = " + stepRequest + "; depth = " + depth);
                    } catch (InternalExceptionWrapper ex) {
                        logger.throwing(getClass().getName(), "shouldNotStopHere", ex);
                    } catch (VMDisconnectedExceptionWrapper ex) {
                        logger.throwing(getClass().getName(), "shouldNotStopHere", ex);
                    } catch (ObjectCollectedExceptionWrapper ex) {
                        logger.throwing(getClass().getName(), "shouldNotStopHere", ex);
                    } catch (IllegalThreadStateExceptionWrapper ex) {
                        logger.throwing(getClass().getName(), "shouldNotStopHere", ex);
                    } catch (IncompatibleThreadStateException ex) {
                        logger.throwing(getClass().getName(), "shouldNotStopHere", ex);
                    }
                }
                String[] exclusionPatterns;
                if (steppingFromFilteredLocation) {
                    exclusionPatterns = null;
                } else {
                    exclusionPatterns = debuggerImpl.getSmartSteppingFilter().getExclusionPatterns();
                    for (int i = 0; i < exclusionPatterns.length; i++) {
                        StepRequestWrapper.addClassExclusionFilter(stepRequest, exclusionPatterns [i]);
                        logger.finer("   add pattern: "+exclusionPatterns[i]);
                    }
                }
                if (!stepThrough && exclusionPatterns != null && exclusionPatterns.length > 0) {
                    StepPatternDepth spd = new StepPatternDepth();
                    spd.exclusionPatterns = exclusionPatterns;
                    spd.stackDepth = t.getStackDepth();
                    stepPatternDepth = spd;
                } else {
                    stepPatternDepth = null;
                }
                logger.fine("Set stepPatternDepth to "+stepPatternDepth);

                debuggerImpl.getOperator ().register (stepRequest, this);
                EventRequestWrapper.setSuspendPolicy (stepRequest, debugger.getSuspend ());
                try {
                    EventRequestWrapper.enable (stepRequest);
                    requestsToCancel.add(stepRequest);
                } catch (IllegalThreadStateException itsex) {
                    // the thread named in the request has died.
                    debuggerImpl.getOperator ().unregister (stepRequest);
                } catch (ObjectCollectedExceptionWrapper ocex) {
                    // the thread named in the request was collected.
                    debuggerImpl.getOperator ().unregister (stepRequest);
                } catch (InvalidRequestStateExceptionWrapper irse) {
                    Exceptions.printStackTrace(irse);
                }
            } finally {
                t.accessLock.readLock().unlock();
            }
            return true; // resume
        } catch (InternalExceptionWrapper e) {
            return false;
        } catch (VMDisconnectedExceptionWrapper e) {
            return false;
        }
    }
    
    private static boolean match(String name, String pattern) {
        if (pattern.startsWith ("*")) {
            return name.endsWith (pattern.substring (1));
        } else if (pattern.endsWith ("*")) {
            return name.startsWith (
                pattern.substring (0, pattern.length () - 1)
            );
        }
        return name.equals (pattern);
    }

    /** Cancel this step - remove all submitted event requests. */
    public void cancel() {
        JPDADebuggerImpl debuggerImpl = (JPDADebuggerImpl) debugger;
        VirtualMachine vm = debuggerImpl.getVirtualMachine();
        if (vm == null) return ;
        try {
            EventRequestManager erm = VirtualMachineWrapper.eventRequestManager(vm);
            for (EventRequest er : requestsToCancel) {
                try {
                    EventRequestManagerWrapper.deleteEventRequest(erm, er);
                } catch (InvalidRequestStateExceptionWrapper ex) {}
                debuggerImpl.getOperator ().unregister (er);
            }
        } catch (VMDisconnectedExceptionWrapper e) {
        } catch (InternalExceptionWrapper e) {
        }
    }
    
    /**
     * Test whether the method is considered to be synthetic
     * @param m The method
     * @param loc The current location in that method
     * @return  0 when not synthetic
     *          positive when suggested step depth is returned
     *          negative when is synthetic and no further step depth is suggested.
     */
    public static int isSyntheticMethod(Method m, Location loc) throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper {
        String name = TypeComponentWrapper.name(m);
        if (name.startsWith("lambda$")) {                                       // NOI18N
            int lineNumber = LocationWrapper.lineNumber(loc);
            if (lineNumber == 1) {
                // We're in the initialization of the Lambda. We need to step over it.
                return StepRequest.STEP_OVER;
            }
            return 0; // Do not treat Lambda methods as synthetic, because they contain user code.
        } else {
            // Do check the class for being Lambda synthetic class:
            ReferenceType declaringType = LocationWrapper.declaringType(loc);
            try {
                String className = ReferenceTypeWrapper.name(declaringType);
                if (className.contains("$$Lambda$")) {                          // NOI18N
                    // Lambda synthetic class
                    return -1;
                }
            } catch (ObjectCollectedExceptionWrapper ex) {
            }
        }
        return TypeComponentWrapper.isSynthetic(m) ? -1 : 0;
    }
    
    private SmartSteppingFilterImpl smartSteppingFilter;

    private SmartSteppingFilterImpl getSmartSteppingFilterImpl () {
        if (smartSteppingFilter == null) {
            smartSteppingFilter = (SmartSteppingFilterImpl) session.lookupFirst(null, SmartSteppingFilter.class);
        }
        return smartSteppingFilter;
    }

    private CompoundSmartSteppingListener compoundSmartSteppingListener;

    private CompoundSmartSteppingListener getCompoundSmartSteppingListener () {
        if (compoundSmartSteppingListener == null)
            compoundSmartSteppingListener = session.lookupFirst(null, CompoundSmartSteppingListener.class);
        return compoundSmartSteppingListener;
    }

    public void setIgnoreStepFilters(boolean ignoreStepFilters) {
        this.ignoreStepFilters = ignoreStepFilters;
    }
    
    public void setStopHereCheck(StopHereCheck stopHereCheck) {
        this.stopHereCheck = stopHereCheck;
    }
    
    public static interface StopHereCheck {
        
        public boolean stopHere(boolean willStop);
    }

    public static final class MethodExitBreakpointListener implements JPDABreakpointListener {
        
        private final MethodBreakpoint mb;
        private Variable returnValue;
        
        public MethodExitBreakpointListener(MethodBreakpoint mb) {
            this.mb = mb;
        }
        
        @Override
        public void breakpointReached(JPDABreakpointEvent event) {
            returnValue = event.getVariable();
        }
        
        public Variable getReturnValue() {
            return returnValue;
        }
        
        public void destroy() {
            mb.removeJPDABreakpointListener(this);
            DebuggerManager.getDebuggerManager().removeBreakpoint(mb);
        }
        
    }
    
    private static final class StepPatternDepth {
        
        String[] exclusionPatterns;
        int stackDepth;

        private boolean isFiltered(String className) {
            for (int i = 0; i < exclusionPatterns.length; i++) {
                String p = exclusionPatterns[i];
                if (p.startsWith("*") && className.endsWith(p.substring(1))) {
                    return true;
                }
                if (p.endsWith("*") && className.startsWith(p.substring(0, p.length() - 1))) {
                    return true;
                }
                if (className.equals(p)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "StepPatternDepth: "+Arrays.asList(exclusionPatterns)+", stackDepth = "+stackDepth;
        }
        
    }
    
    /*public static final class SingleThreadedStepWatch implements Runnable {
        
        private static final int DELAY = 5000;
        
        private static final RequestProcessor stepWatchRP = new RequestProcessor("Debugger Step Watch", 1);
        
        private static final Map<StepRequest, SingleThreadedStepWatch> STEP_WATCH_POOL = new HashMap<StepRequest, SingleThreadedStepWatch>();
        
        private RequestProcessor.Task watchTask;
        private JPDADebuggerImpl debugger;
        private StepRequest request;
        private Dialog dialog;
        private List<JPDAThread> resumedThreads;
        
        public SingleThreadedStepWatch(JPDADebuggerImpl debugger, StepRequest request) {
            this.debugger = debugger;
            this.request = request;
            watchTask = stepWatchRP.post(this, DELAY);
            synchronized (STEP_WATCH_POOL) {
                STEP_WATCH_POOL.put(request, this);
            }
        }
        
        public static void stepRequestDeleted(StepRequest request) {
            SingleThreadedStepWatch stepWatch;
            synchronized (STEP_WATCH_POOL) {
                stepWatch = STEP_WATCH_POOL.remove(request);
            }
            if (stepWatch != null) stepWatch.done();
        }
        
        public void done() {
            synchronized (this) {
                if (watchTask == null) return;
                watchTask.cancel();
                watchTask = null;
                if (dialog != null) {
                    dialog.setVisible(false);
                }
                if (resumedThreads != null) {
                    synchronized (debugger.LOCK) {
                        suspendThreads(resumedThreads);
                    }
                    resumedThreads = null;
                }
            }
            synchronized (STEP_WATCH_POOL) {
                STEP_WATCH_POOL.remove(request);
            }
        }
    
        public void run() {
            synchronized (this) {
                if (watchTask == null) return ; // We're done
                try {
                    if (request.thread().isSuspended()) {
                        watchTask.schedule(DELAY);
                        return ;
                    }
                    if (request.thread().status() == ThreadReference.THREAD_STATUS_ZOMBIE) {
                        // Do not wait for zombie!
                        return ;
                    }
                } catch (VMDisconnectedException vmdex) {
                    // Do not wait for finished/disconnected threads
                    return ;
                }
                if (!request.isEnabled()) {
                    return ;
                }
                Boolean resumeDecision = debugger.getSingleThreadStepResumeDecision();
                if (resumeDecision != null) {
                    if (resumeDecision.booleanValue()) {
                        doResume();
                    }
                    return ;
                }
            }
            String message = NbBundle.getMessage(JPDAStepImpl.class, "SingleThreadedStepBlocked");
            JCheckBox cb = new JCheckBox(NbBundle.getMessage(JPDAStepImpl.class, "RememberDecision"));
            final boolean[] yes = new boolean[] { false, false };
            DialogDescriptor dd = new DialogDescriptor(
                    //message,
                    createDlgPanel(message, cb),
                    new NotifyDescriptor.Confirmation(message, NotifyDescriptor.YES_NO_OPTION).getTitle(),
                    true,
                    NotifyDescriptor.YES_NO_OPTION,
                    null,
                    new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            synchronized (yes) {
                                yes[0] = evt.getSource() == NotifyDescriptor.YES_OPTION;
                                yes[1] = evt.getSource() == NotifyDescriptor.NO_OPTION;
                            }
                        }
                    });
            dd.setMessageType(NotifyDescriptor.QUESTION_MESSAGE);
            Dialog theDialog;
            synchronized (this) {
                dialog = org.openide.DialogDisplayer.getDefault().createDialog(dd);
                theDialog = dialog;
            }
            theDialog.setVisible(true);
            boolean doResume;
            synchronized (yes) {
                doResume = yes[0];
            }
            synchronized (this) {
                dialog = null;
                if (watchTask == null) return ;
                if ((yes[0] || yes[1]) && cb.isSelected()) {
                    debugger.setSingleThreadStepResumeDecision(Boolean.valueOf(yes[0]));
                }
                if (doResume) {
                    doResume();
                }
            }
            /*
            Object option = org.openide.DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Confirmation(message, NotifyDescriptor.YES_NO_OPTION));
            if (NotifyDescriptor.YES_OPTION == option) {
                debugger.resume();
            }
             */  /*
        }
        
        private static JPanel createDlgPanel(String message, JCheckBox cb) {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            JTextArea area = new JTextArea(message);
            Color color = UIManager.getColor("Label.background"); // NOI18N
            if (color != null) {
                area.setBackground(color);
            }
            //area.setLineWrap(true);
            //area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setTabSize(4); // looks better for module sys messages than 8
            panel.add(area, c);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new java.awt.Insets(12, 0, 0, 0);
            panel.add(cb, c);
            return panel;
        }
        
        private void doResume() {
            synchronized (debugger.LOCK) {
                List<JPDAThread> suspendedThreads = new ArrayList<JPDAThread>();
                JPDAThreadGroup[] tgs = debugger.getTopLevelThreadGroups();
                for (JPDAThreadGroup tg: tgs) {
                    fillSuspendedThreads(tg, suspendedThreads);
                }
                resumeThreads(suspendedThreads);
                resumedThreads = suspendedThreads;
            }
        }
        
        private static void fillSuspendedThreads(JPDAThreadGroup tg, List<JPDAThread> sts) {
            for (JPDAThread t : tg.getThreads()) {
                if (t.isSuspended()) sts.add(t);
            }
            for (JPDAThreadGroup tgg : tg.getThreadGroups()) {
                fillSuspendedThreads(tgg, sts);
            }
        }
        
        private static void suspendThreads(List<JPDAThread> ts) {
            for (JPDAThread t : ts) {
                t.suspend();
            }
        }
        
        private static void resumeThreads(List<JPDAThread> ts) {
            for (JPDAThread t : ts) {
                t.resume();
            }
        }
        
    }*/

}
