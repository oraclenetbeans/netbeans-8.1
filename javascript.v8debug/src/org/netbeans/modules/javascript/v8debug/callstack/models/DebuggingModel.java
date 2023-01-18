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

package org.netbeans.modules.javascript.v8debug.callstack.models;

import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.lib.v8debug.V8Frame;
import org.netbeans.lib.v8debug.V8Script;
import org.netbeans.lib.v8debug.vars.ReferencedValue;
import org.netbeans.lib.v8debug.vars.V8Function;
import org.netbeans.lib.v8debug.vars.V8Object;
import org.netbeans.lib.v8debug.vars.V8ScriptValue;
import org.netbeans.lib.v8debug.vars.V8Value;
import org.netbeans.modules.javascript.v8debug.V8Debugger;
import org.netbeans.modules.javascript.v8debug.V8DebuggerSessionProvider;
import org.netbeans.modules.javascript.v8debug.frames.CallFrame;
import org.netbeans.modules.javascript.v8debug.frames.CallStack;
import org.netbeans.modules.javascript2.debug.models.ViewModelSupport;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.DebuggerServiceRegistrations;
import org.netbeans.spi.viewmodel.ExtendedNodeModel;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.util.NbBundle;
import org.openide.util.datatransfer.PasteType;

/**
 *
 * @author Martin Entlicher
 */
@DebuggerServiceRegistrations({
 @DebuggerServiceRegistration(path=V8DebuggerSessionProvider.SESSION_NAME+"/DebuggingView",
                              types={ TreeModel.class, ExtendedNodeModel.class }),
 @DebuggerServiceRegistration(path=V8DebuggerSessionProvider.SESSION_NAME+"/CallStackView",
                              types={ TreeModel.class, ExtendedNodeModel.class })
})
public class DebuggingModel extends ViewModelSupport implements TreeModel, ExtendedNodeModel {
    
    //@StaticResource(searchClasspath = true)
    private static final String ICON_CALL_STACK =
            "org/netbeans/modules/debugger/resources/threadsView/call_stack_16.png";
    //@StaticResource(searchClasspath = true)
    private static final String ICON_EMPTY =
            "org/netbeans/modules/debugger/resources/empty.gif";
    
    private static final Object DBG_RUNNING_NODE = new Object();
    
    private final V8Debugger dbg;
    
    public DebuggingModel(ContextProvider contextProvider) {
        dbg = contextProvider.lookupFirst(null, V8Debugger.class);
        V8Debugger.Listener changeListener = new ChangeListener();
        dbg.addListener(changeListener);
    }

    @Override
    public Object getRoot() {
        return ROOT;
    }

    @Override
    public Object[] getChildren(Object parent, int from, int to) throws UnknownTypeException {
        if (parent == ROOT) {
            if (dbg.isSuspended()) {
                CallStack cs = dbg.getCurrentCallStack();
                if (cs != null) {
                    return cs.getCallFrames();
                }
            } else {
                return new Object[] { DBG_RUNNING_NODE };
            }
            return EMPTY_CHILDREN;
        }
        throw new UnknownTypeException(parent);
    }

    @Override
    public boolean isLeaf(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return false;
        }
        if (node instanceof CallStack) {
            return true;
        }
        if (node == DBG_RUNNING_NODE) {
            return true;
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public int getChildrenCount(Object node) throws UnknownTypeException {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canRename(Object node) throws UnknownTypeException {
        return false;
    }

    @Override
    public boolean canCopy(Object node) throws UnknownTypeException {
        return false;
    }

    @Override
    public boolean canCut(Object node) throws UnknownTypeException {
        return false;
    }

    @Override
    public Transferable clipboardCopy(Object node) throws IOException, UnknownTypeException {
        return null;
    }

    @Override
    public Transferable clipboardCut(Object node) throws IOException, UnknownTypeException {
        return null;
    }

    @Override
    public PasteType[] getPasteTypes(Object node, Transferable t) throws UnknownTypeException {
        return null;
    }

    @Override
    public void setName(Object node, String name) throws UnknownTypeException {
    }

    @Override
    public String getIconBaseWithExtension(Object node) throws UnknownTypeException {
        if (node instanceof CallFrame) {
            return ICON_CALL_STACK;
        } else if (node == DBG_RUNNING_NODE) {
            return ICON_EMPTY;
        }
        throw new UnknownTypeException(node);
    }

    @NbBundle.Messages("CTL_DebuggerRunning=Program is Running...")
    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node instanceof CallFrame) {
            CallFrame cf = (CallFrame) node;
            V8Frame frame = cf.getFrame();
            String text = "";
            String scriptName = getScriptName(cf);
            String thisName = cf.getThisName();
            if ("Object".equals(thisName) || "global".equals(thisName)) {
                thisName = null;
            }
            String functionName = cf.getFunctionName();
            long line = frame.getLine()+1;
            long column = frame.getColumn()+1;
            
            text = ((thisName != null && !thisName.isEmpty()) ? thisName + '.' : "") +
                   functionName +
                   " (" + ((scriptName != null) ? scriptName : "?") +
                   ":"+line+":"+column+")";
            //text += ":"+line+":"+column;
            return text;
        } else if (node == DBG_RUNNING_NODE) {
            return Bundle.CTL_DebuggerRunning();
        }
        throw new UnknownTypeException(node);
    }
    
    static String getScriptName(CallFrame cf) {
        V8Script script = cf.getScript();
        if (script != null) {
            String scriptName = script.getName();
            if (scriptName == null) {
                return null;
            }
            int i = scriptName.lastIndexOf('/');
            if (i < 0) {
                i = scriptName.lastIndexOf('\\');
            }
            if (i > 0) {
                scriptName = scriptName.substring(i+1);
            }
            return scriptName;
        }
        return null;
    }
    
    @Override
    public String getIconBase(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not to be called.");
    }

    @NbBundle.Messages("CTL_DebuggerRunningDescr=No stack trace while program is running.")
    @Override
    public String getShortDescription(Object node) throws UnknownTypeException {
        if (node instanceof CallFrame) {
            CallFrame cf = (CallFrame) node;
            V8Frame frame = cf.getFrame();
            String text = frame.getText();
            if (text != null) {
                text = text.replace("\\n", "\n");
            }
            return text;
        } else if (node == DBG_RUNNING_NODE) {
            return Bundle.CTL_DebuggerRunningDescr();
        }
        throw new UnknownTypeException(node);
    }
    
    private class ChangeListener implements V8Debugger.Listener {
        
        private WeakReference<CallFrame> lastCurrentFrame = new WeakReference<CallFrame>(null);
        
        public ChangeListener() {}

        @Override
        public void notifySuspended(boolean suspended) {
            fireChangeEvent(new ModelEvent.TreeChanged(DebuggingModel.this));
        }

        @Override
        public void notifyCurrentFrame(CallFrame cf) {
            CallFrame last = lastCurrentFrame.get();
            if (last != null) {
                fireChangeEvent(new ModelEvent.NodeChanged(DebuggingModel.this, last));
            }
            if (cf != null) {
                fireChangeEvent(new ModelEvent.NodeChanged(DebuggingModel.this, cf));
            }
            lastCurrentFrame = new WeakReference<>(cf);
        }
        
        @Override
        public void notifyFinished() {
        }
        
    }
    
}
