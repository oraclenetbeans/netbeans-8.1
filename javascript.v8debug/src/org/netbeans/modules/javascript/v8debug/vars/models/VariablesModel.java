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

package org.netbeans.modules.javascript.v8debug.vars.models;

import java.awt.Color;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.lib.v8debug.PropertyLong;
import org.netbeans.lib.v8debug.V8Command;
import org.netbeans.lib.v8debug.V8Frame;
import org.netbeans.lib.v8debug.V8Request;
import org.netbeans.lib.v8debug.V8Response;
import org.netbeans.lib.v8debug.V8Scope;
import org.netbeans.lib.v8debug.commands.Scope;
import org.netbeans.lib.v8debug.commands.SetVariableValue;
import org.netbeans.lib.v8debug.vars.NewValue;
import org.netbeans.lib.v8debug.vars.ReferencedValue;
import org.netbeans.lib.v8debug.vars.V8Object;
import org.netbeans.lib.v8debug.vars.V8Value;
import org.netbeans.modules.javascript.v8debug.ReferencedValues;
import org.netbeans.modules.javascript.v8debug.V8Debugger;
import org.netbeans.modules.javascript.v8debug.V8DebuggerEngineProvider;
import org.netbeans.modules.javascript.v8debug.frames.CallFrame;
import org.netbeans.modules.javascript.v8debug.vars.EvaluationError;
import org.netbeans.modules.javascript.v8debug.vars.ScopeValue;
import org.netbeans.modules.javascript.v8debug.vars.V8Evaluator;
import org.netbeans.modules.javascript.v8debug.vars.VarValuesLoader;
import org.netbeans.modules.javascript.v8debug.vars.Variable;
import org.netbeans.modules.javascript2.debug.models.ViewModelSupport;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.Constants;
import org.netbeans.spi.viewmodel.ExtendedNodeModel;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.WeakSet;
import org.openide.util.datatransfer.PasteType;

/**
 *
 * @author Martin Entlicher
 */
@DebuggerServiceRegistration(path=V8DebuggerEngineProvider.ENGINE_NAME+"/LocalsView",
                             types={ TreeModel.class, ExtendedNodeModel.class, TableModel.class })
public class VariablesModel extends ViewModelSupport implements TreeModel,
                                                                ExtendedNodeModel,
                                                                TableModel,
                                                                V8Debugger.Listener {
    
    //@StaticResource(searchClasspath = true)
    private static final String ICON_LOCAL = "org/netbeans/modules/debugger/resources/localsView/local_variable_16.png"; // NOI18N
    @StaticResource(searchClasspath = true)
    private static final String ICON_SCOPE = "org/netbeans/modules/javascript2/debug/resources/global_variable_16.png"; // NOI18N
    
    protected final V8Debugger dbg;
    private final VarValuesLoader vvl;
    private volatile boolean topFrameRefreshed;
    private final Set<Variable> refreshWhenLoaded = Collections.synchronizedSet(new WeakSet<Variable>());

    public VariablesModel(ContextProvider contextProvider) {
        dbg = contextProvider.lookupFirst(null, V8Debugger.class);
        dbg.addListener(this);
        vvl = contextProvider.lookupFirst(null, VarValuesLoader.class);
    }

    @Override
    public Object getRoot() {
        return ROOT;
    }

    @NbBundle.Messages({"# {0} - argument number", "CTL_Argument=Argument {0}"})
    @Override
    public Object[] getChildren(Object parent, int from, int to) throws UnknownTypeException {
        if (parent == ROOT) {
            CallFrame cf = dbg.getCurrentFrame();
            if (cf == null) {
                return EMPTY_CHILDREN;
            }
            V8Frame frame = cf.getFrame();
            Map<String, ReferencedValue> argumentRefs = frame.getArgumentRefs();
            Map<String, ReferencedValue> localRefs = frame.getLocalRefs();
            V8Scope[] scopes = frame.getScopes();
            int numscopes = 0;
            for (V8Scope scope : scopes) {
                if (!V8Scope.Type.Local.equals(scope.getType())) {
                    numscopes++;
                }
            }
            int n = argumentRefs.size() + localRefs.size() + numscopes;
            Object[] ch = new Object[n];
            int i = 0;
            ReferencedValues rvals = cf.getRvals();
            for (Map.Entry<String, ReferencedValue> namerv : argumentRefs.entrySet()) {
                String name = namerv.getKey();
                if (name == null) {
                    name = Bundle.CTL_Argument((i+1));
                }
                ReferencedValue rv = namerv.getValue();
                long ref = rv.getReference();
                V8Value v = rvals.getReferencedValue(ref);
                boolean incompleteValue = false;
                if (v == null) {
                    v = rv.getValue();
                    incompleteValue = true;
                }
                ch[i++] = new Variable(Variable.Kind.ARGUMENT, name, ref, v, incompleteValue);
            }
            for (String name : localRefs.keySet()) {
                ReferencedValue rv = localRefs.get(name);
                long ref = rv.getReference();
                V8Value v = rvals.getReferencedValue(ref);
                boolean incompleteValue = false;
                if (v == null) {
                    v = rv.getValue();
                    incompleteValue = true;
                }
                ch[i++] = new Variable(Variable.Kind.LOCAL, name, ref, v, incompleteValue);
            }
            for (V8Scope scope : scopes) {
                if (V8Scope.Type.Local.equals(scope.getType())) {
                    // Vars from local scope are provided automatically
                    continue;
                }
                ch[i++] = new ScopeValue(scope);
            }
            return ch;
        } else if (parent instanceof Variable) {
            Variable vl = (Variable) parent;
            V8Value value;
            try {
                value = vvl.getValue(vl);
            } catch (EvaluationError ee) {
                value = null;
            }
            if (value instanceof V8Object) {
                V8Object obj = (V8Object) value;
                return getObjectChildren(obj);
            }
            return EMPTY_CHILDREN;
        } else if (parent instanceof ScopeValue) {
            ScopeValue sv = (ScopeValue) parent;
            V8Scope scope = sv.getScope();
            V8Object sobj = sv.getValue();
            if (sobj == null) {
                ReferencedValue<V8Object> sobjr = scope.getObject();
                if (sobjr != null) {
                    sobj = sobjr.getValue();
                    if (sobj == null) {
                        CallFrame cf = dbg.getCurrentFrame();
                        if (cf != null) {
                            sobj = (V8Object) cf.getRvals().getReferencedValue(sobjr.getReference());
                        }
                    }
                }
                if (sobj == null) {
                    sv = loadScope(sv);
                    sobj = sv.getValue();
                }
            }
            if (sobj == null) {
                return EMPTY_CHILDREN;
            } else {
                return getObjectChildren(sobj, sv.getScope());
            }
        } else {
            return EMPTY_CHILDREN;
        }
    }
    
    protected final Object[] getObjectChildren(V8Object obj) {
        return getObjectChildren(obj, null);
    }
    
    protected final Object[] getObjectChildren(V8Object obj, V8Scope scope) {
        V8Object.Array array = obj.getArray();
        List<Object> children = null;
        if (array != null) {
            children = new ArrayList<>();
            V8Object.IndexIterator indexIterator = array.getIndexIterator();
            while (indexIterator.hasNextIndex()) {
                long index = indexIterator.nextIndex();
                children.add(new Variable(Variable.Kind.ARRAY_ELEMENT,
                                          Long.toString(index),
                                          array.getReferenceAt(index), null, true, scope));
            }
        }
        Map<String, V8Object.Property> properties = obj.getProperties();
        Object[] childrenRet;
        if (children == null) {
            childrenRet = new Object[properties.size()];
        } else {
            childrenRet = null;
        }
        int chi = 0;
        for (String name : properties.keySet()) {
            V8Object.Property property = properties.get(name);
            Variable var = new Variable(Variable.Kind.PROPERTY, name, property.getReference(), null, true, scope);
            if (children != null) {
                children.add(var);
            } else {
                childrenRet[chi++] = var;
            }
        }
        if (children != null) {
            childrenRet = children.toArray();
        }
        return childrenRet;
    }

    @Override
    public boolean isLeaf(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return false;
        }
        if (node instanceof Variable) {
            Variable var = (Variable) node;
            try {
                V8Value value = var.getValue();
                if ((value == null || (value instanceof V8Object)) && var.hasIncompleteValue()) {
                    return false; // Allow to load children
                }
                return !hasChildren(value);
            } catch (EvaluationError ex) {
            }
        }
        if (node instanceof ScopeValue) {
            return false;
        }
        return true;
    }
    
    public static boolean hasChildren(V8Value value) {
        if (value instanceof V8Object) {
            V8Object obj = (V8Object) value;
            V8Object.Array array = obj.getArray();
            Map<String, V8Object.Property> properties = obj.getProperties();
            return (array != null && array.getLength() > 0 || properties != null && !properties.isEmpty());
        } else {
            return false;
        }
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getIconBaseWithExtension(Object node) throws UnknownTypeException {
        if (node instanceof Variable) {
            Variable.Kind varKind = ((Variable) node).getKind();
            switch (varKind) {
                case ARGUMENT:
                case LOCAL:
                case PROPERTY:
                case ARRAY_ELEMENT:
                default:
                    return ICON_LOCAL;
            }
        }
        if (node instanceof ScopeValue) {
            return ICON_SCOPE;
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node instanceof Variable) {
            return ((Variable) node).getName();
        }
        if (node instanceof ScopeValue) {
            V8Scope scope = ((ScopeValue) node).getScope();
            String text = scope.getText();
            if (text == null) {
                text = scope.getType().toString();
            }
            return text + " Scope";
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public String getIconBase(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String getShortDescription(Object node) throws UnknownTypeException {
        if (node instanceof Variable) {
            Variable var = (Variable) node;
            String strVal;
            try {
                V8Value value = var.getValue();
                if (value == null) {
                    return null;
                }
                strVal = V8Evaluator.getStringValue(value);
            } catch (EvaluationError ee) {
                strVal = ">" + ee.getLocalizedMessage() + "<";
            }
            return var.getName() + " = " + strVal;
        }
        return null;
    }

    @Override
    public Object getValueAt(Object node, String columnID) throws UnknownTypeException {
        if (node == ROOT) {
            return "";
        } else if (node instanceof Variable) {
            Variable var = (Variable) node;
            V8Value value = null;
            EvaluationError ee = null;
            try {
                boolean wasIncomplete = var.hasIncompleteValue();
                value = vvl.getValue(var);
                if (wasIncomplete) {
                    if (refreshWhenLoaded.remove(var)) {
                        // Refresh isReadOnly():
                        fireChangeEvent(new ModelEvent.TableValueChanged(VariablesModel.this, node, null));
                    }
                    if (!hasChildren(value)) {
                        fireChangeEvent(new ModelEvent.NodeChanged(VariablesModel.this, node, ModelEvent.NodeChanged.CHILDREN_MASK));
                    }
                }
            } catch (EvaluationError ex) {
                ee = ex;
            }
            if (Constants.LOCALS_VALUE_COLUMN_ID.equals(columnID) ||
                Constants.LOCALS_TO_STRING_COLUMN_ID.equals(columnID)) {
                if (value != null) {
                    return toHTML(V8Evaluator.getStringValue(value));
                } else {
                    return toHTML(ee.getLocalizedMessage(), true, false, Color.red);
                }
            } else if (Constants.LOCALS_TYPE_COLUMN_ID.equals(columnID)) {
                if (value != null) {
                    return toHTML(V8Evaluator.getStringType(value));
                } else {
                    return "";
                }
            }
        } else if (node instanceof ScopeValue) {
            return "";
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public boolean isReadOnly(Object node, String columnID) throws UnknownTypeException {
        if (Constants.LOCALS_VALUE_COLUMN_ID.equals(columnID) && node instanceof Variable) {
            Variable var = (Variable) node;
            switch (var.getKind()) {
                case ARGUMENT:
                case LOCAL:
                    return isReadOnlyType(var);
                case ARRAY_ELEMENT:
                    return true;
                case PROPERTY:
                    if (var.getScope() != null) {
                        return isReadOnlyType(var);
                    } else {
                        return true;
                    }
                default:
                    return isReadOnlyType(var);
            }
        } else {
            return true;
        }
    }
    
    public boolean isReadOnlyType(Variable var) {
        try {
            V8Value value = var.getValue();
            if (value == null) {
                refreshWhenLoaded.add(var);
                return true;
            }
            switch (value.getType()) {
                case Context:
                case Error:
                case Frame:
                case Function:
                case Promise:
                    return true;
                default:
                    return false;
            }
        } catch (EvaluationError ee) {
            return true;
        }
    }

    @Override
    public void setValueAt(final Object node, String columnID, Object value) throws UnknownTypeException {
        if (!(value instanceof String)) {
            throw new UnknownTypeException("Accepting String values only. Not "+value);
        }
        if (Constants.LOCALS_VALUE_COLUMN_ID.equals(columnID) && node instanceof Variable) {
            final Variable var = (Variable) node;
            CallFrame cf = dbg.getCurrentFrame();
            if (cf == null) {
                return ;
            }
            V8Scope scope = var.getScope();
            long scopeNumber;
            long frameNumber;
            if (scope != null) {
                scopeNumber = scope.getIndex();
                if (scope.getFrameIndex().hasValue()) {
                    frameNumber = scope.getFrameIndex().getValue();
                } else {
                    frameNumber = cf.getFrame().getIndex().getValue();
                }
            } else {
                scopeNumber = 0;
                frameNumber = cf.getFrame().getIndex().getValue();
            }
            final V8Value evalVal;
            try {
                evalVal = V8Evaluator.evaluate(dbg, (String) value);
            } catch (EvaluationError ee) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(ee.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE));
                return ;
            }
            long newHandle = evalVal.getHandle();
            dbg.sendCommandRequest(V8Command.SetVariableValue,
                                   new SetVariableValue.Arguments(var.getName(),
                                                                  new NewValue(newHandle),
                                                                  scopeNumber,
                                                                  frameNumber),
                                   new V8Debugger.CommandResponseCallback() {
                @Override
                public void notifyResponse(V8Request request, V8Response response) {
                    if (response != null) {
                        String errorMessage = response.getErrorMessage();
                        if (errorMessage != null) {
                            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(errorMessage, NotifyDescriptor.ERROR_MESSAGE));
                        } else {
                            vvl.updateValue(var, evalVal);
                            fireChangeEvent(new ModelEvent.TableValueChanged(VariablesModel.this, node, null, ModelEvent.TableValueChanged.VALUE_MASK));
                        }
                    }
                }
            });
        }
    }

    @Override
    public void notifySuspended(boolean suspended) {
        refresh();
        topFrameRefreshed = suspended;
    }

    @Override
    public void notifyCurrentFrame(CallFrame cf) {
        if (cf == null) {
            return ;
        }
        if (topFrameRefreshed && cf.isTopFrame()) {
            return ;
        }
        topFrameRefreshed = false;
        refresh();
    }
    
    @Override
    public void notifyFinished() {
        
    }

    private ScopeValue loadScope(ScopeValue scopeValue) {
        Scope.Arguments sa;
        V8Scope scope = scopeValue.getScope();
        PropertyLong frameIndex = scope.getFrameIndex();
        if (frameIndex.hasValue()) {
            sa = new Scope.Arguments(scope.getIndex(), frameIndex.getValue());
        } else {
            sa = new Scope.Arguments(scope.getIndex());
        }
        final ScopeValue[] newScopeRef = new ScopeValue[] { null };
        final boolean[] isSet = new boolean[] { false };
        V8Request cmdRequest = dbg.sendCommandRequest(V8Command.Scope, sa, new V8Debugger.CommandResponseCallback() {
            @Override
            public void notifyResponse(V8Request request, V8Response response) {
                if (response != null && response.isSuccess()) {
                    Scope.ResponseBody srb = (Scope.ResponseBody) response.getBody();
                    synchronized (newScopeRef) {
                        V8Scope scope = srb.getScope();
                        V8Object obj;
                        if (!scope.getObject().hasValue()) {
                            long ref = scope.getObject().getReference();
                            obj = (V8Object) response.getReferencedValue(ref);
                        } else {
                            obj = scope.getObject().getValue();
                        }
                        newScopeRef[0] = new ScopeValue(scope, obj);
                        isSet[0] = true;
                        newScopeRef.notifyAll();
                    }
                } else {
                    synchronized (newScopeRef) {
                        isSet[0] = true;
                        newScopeRef.notifyAll();
                    }
                }
            }
        });
        if (cmdRequest != null) {
            synchronized (newScopeRef) {
                if (!isSet[0]) {
                    try {
                        newScopeRef.wait();
                    } catch (InterruptedException ex) {}
                }
                if (newScopeRef[0] != null) {
                    scopeValue.setValue(newScopeRef[0].getValue());
                }
            }
        }
        return scopeValue;
    }
    
}
