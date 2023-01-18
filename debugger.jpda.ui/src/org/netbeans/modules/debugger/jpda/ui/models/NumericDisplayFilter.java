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

package org.netbeans.modules.debugger.jpda.ui.models;

import java.lang.reflect.InvocationTargetException;
import org.netbeans.spi.debugger.ui.Constants;
import org.netbeans.spi.viewmodel.*;
import org.netbeans.api.debugger.jpda.*;
import org.openide.util.Exceptions;
import org.openide.util.actions.Presenter;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.util.*;
import java.awt.event.ActionEvent;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.DebuggerServiceRegistrations;

/**
 * Implements the "Display As Decimal/Hexadecimal/Octal/Binary/Char"
 * option for numeric variables.
 * Provides the popup action and filters displayed values.
 *
 * @author Maros Sandor, Jan Jancura, Martin Entlicher
 */
@DebuggerServiceRegistrations({
    @DebuggerServiceRegistration(path="WatchesView", types=TableModelFilter.class),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/LocalsView",
                                 types={ NodeActionsProviderFilter.class, TableModelFilter.class },
                                 position=800),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/ResultsView",
                                 types={ NodeActionsProviderFilter.class, TableModelFilter.class },
                                 position=800),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/ToolTipView",
                                 types={ NodeActionsProviderFilter.class, TableModelFilter.class },
                                 position=800),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/WatchesView",
                                 types={ NodeActionsProviderFilter.class, TableModelFilter.class },
                                 position=800)
})
public class NumericDisplayFilter implements TableModelFilter, 
NodeActionsProviderFilter, Constants {

    enum NumericDisplaySettings { DECIMAL, HEXADECIMAL, OCTAL, BINARY, CHAR, TIME }

    private final Map<Variable, NumericDisplaySettings>   variableToDisplaySettings = new HashMap<Variable, NumericDisplaySettings>();
    private HashSet     listeners;

    
    // TableModelFilter ........................................................

    public Object getValueAt (
        TableModel original, 
        Object node, 
        String columnID
    ) throws UnknownTypeException {
        if ( (columnID == Constants.WATCH_VALUE_COLUMN_ID ||
              columnID == Constants.WATCH_TO_STRING_COLUMN_ID ||
              columnID == Constants.LOCALS_VALUE_COLUMN_ID ||
              columnID == Constants.LOCALS_TO_STRING_COLUMN_ID) && 
            node instanceof Variable && 
            isIntegralType ((Variable) node)
        ) {
            if (node instanceof JPDAWatch) {
                JPDAWatch w = (JPDAWatch) node;
                String e = w.getExceptionDescription ();
                if (e == null) {
                    if (columnID == Constants.WATCH_VALUE_COLUMN_ID ||
                        columnID == Constants.LOCALS_VALUE_COLUMN_ID) {
                        VariablesTableModel.setErrorValueMsg(w, null);
                    } else {
                        VariablesTableModel.setErrorToStringMsg(w, null);
                    }
                }
            }
            Variable var = (Variable) node;
            NumericDisplaySettings nds = variableToDisplaySettings.get (var);
            if (nds == null && var instanceof Field) {
                Variable parent = null;
                try {
                    java.lang.reflect.Method pvm = var.getClass().getMethod("getParentVariable");
                    pvm.setAccessible(true);
                    parent = (Variable) pvm.invoke(var);
                } catch (IllegalAccessException ex) {
                } catch (IllegalArgumentException ex) {
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (NoSuchMethodException ex) {
                } catch (SecurityException ex) {
                }
                nds = variableToDisplaySettings.get(parent);
            }
            return getValue(var, nds);
        }
        return original.getValueAt (node, columnID);
    }

    public boolean isReadOnly (
        TableModel original, 
        Object node, 
        String columnID
    ) throws UnknownTypeException {
        return original.isReadOnly(node, columnID);
    }

    public void setValueAt (
        TableModel original, 
        Object node, 
        String columnID, 
        Object value
    ) throws UnknownTypeException {
        if ( (columnID == Constants.WATCH_VALUE_COLUMN_ID ||
              columnID == Constants.WATCH_TO_STRING_COLUMN_ID ||
              columnID == Constants.LOCALS_VALUE_COLUMN_ID ||
              columnID == Constants.LOCALS_TO_STRING_COLUMN_ID) && 
            node instanceof Variable && 
            isIntegralType ((Variable) node) &&
            value instanceof String
        ) {
            Variable var = (Variable) node;
            value = setValue (
                var, 
                (NumericDisplaySettings) variableToDisplaySettings.get (var),
                (String) value
            );
        }
        original.setValueAt(node, columnID, value);
    }

    public void addModelListener (ModelListener l) {
        HashSet newListeners = (listeners == null) ? 
            new HashSet () : (HashSet) listeners.clone ();
        newListeners.add (l);
        listeners = newListeners;
    }

    public void removeModelListener (ModelListener l) {
        if (listeners == null) return;
        HashSet newListeners = (HashSet) listeners.clone();
        newListeners.remove (l);
        listeners = newListeners;
    }

    
    // NodeActionsProviderFilter ...............................................

    public void performDefaultAction (
        NodeActionsProvider original, 
        Object node
    ) throws UnknownTypeException {
        original.performDefaultAction (node);
    }

    public Action[] getActions (
        NodeActionsProvider original, 
        Object node
    ) throws UnknownTypeException {
        if (!(node instanceof Variable)) return original.getActions(node);
        Action [] actions;
        try {
            actions = original.getActions(node);
        } catch (UnknownTypeException e) {
            actions = new Action[0];
        }
        List myActions = new ArrayList();
        if (node instanceof Variable) {
            Variable var = (Variable) node;
            if (isIntegralTypeOrArray(var)) {
                myActions.add(new DisplayAsAction((Variable) node));
            }
        }
        myActions.addAll(Arrays.asList(actions));
        return (Action[]) myActions.toArray(new Action[myActions.size()]);
    }

    
    // other methods ...........................................................
    
    private static int getChar(String toString) {
        // Remove the surrounding apostrophes first:
        toString = toString.substring(1, toString.length() - 1);
        char c = toString.charAt(0);
        return c & 0xFFFF;
    }
    
    private Object getValue (Variable var, NumericDisplaySettings settings) {
        if (settings == null) return var.getValue ();
        String type = var.getType ();
        try {
            switch (settings) {
            case DECIMAL:
                if ("char".equals(type)) {
                    int c = getChar(var.getValue());
                    return Integer.toString(c);
                } else {
                    return var.getValue ();
                }
            case HEXADECIMAL:
                if ("int".equals (type))
                    return "0x" + Integer.toHexString (
                        Integer.parseInt (var.getValue ())
                    );
                else
                if ("short".equals (type)) {
                    String rv = Integer.toHexString(Short.parseShort(var.getValue()));
                    if (rv.length() > 4) rv = rv.substring(rv.length() - 4, rv.length());
                    return "0x" + rv;
                } else if ("byte".equals(type)) {
                    String rv = Integer.toHexString(Byte.parseByte(var.getValue()));
                    if (rv.length() > 2) rv = rv.substring(rv.length() - 2, rv.length());
                    return "0x" + rv;
                } else if ("char".equals(type)) {
                    int c = getChar(var.getValue());
                    return "0x" + Integer.toHexString(c);
                } else {//if ("long".equals(type)) {
                    return "0x" + Long.toHexString (
                        Long.parseLong (var.getValue ())
                    );
                }
            case OCTAL:
                if ("int".equals (type))
                    return "0" + Integer.toOctalString (
                        Integer.parseInt (var.getValue ())
                    );
                else
                if ("short".equals(type)) {
                    String rv = Integer.toOctalString(Short.parseShort(var.getValue()));
                    if (rv.length() > 5) rv = rv.substring(rv.length() - 5, rv.length());
                    return "0" + (rv.charAt(0) == '0' ? "1" : "") + rv;
                } else
                if ("byte".equals(type)) {
                    String rv = Integer.toOctalString(Byte.parseByte(var.getValue()));
                    if (rv.length() > 3) rv = "1" + rv.substring(rv.length() - 2, rv.length());
                    return "0" + rv;
                } else if ("char".equals(type)) {
                    int c = getChar(var.getValue());
                    return "0" + Integer.toOctalString(c);
                } else {//if ("long".equals(type)) {
                    return "0" + Long.toOctalString (
                        Long.parseLong (var.getValue ())
                    );
                }
            case BINARY:
                if ("int".equals(type))
                    return Integer.toBinaryString(Integer.parseInt(var.getValue()));
                else if ("short".equals(type)) {
                    String rv = Integer.toBinaryString(Short.parseShort(var.getValue()));
                    if (rv.length() > 16) rv = rv.substring(rv.length() - 16, rv.length());
                    return rv;
                } else if ("byte".equals(type)) {
                    String rv = Integer.toBinaryString(Byte.parseByte(var.getValue()));
                    if (rv.length() > 8) rv = rv.substring(rv.length() - 8, rv.length());
                    return rv;
                } else if ("char".equals(type)) {
                    int c = getChar(var.getValue());
                    return Integer.toBinaryString(c);
                } else {//if ("long".equals(type)) {
                    return Long.toBinaryString (Long.parseLong (var.getValue ()));
                }
            case CHAR:
                if ("char".equals(type)) {
                    return var.getValue ();
                }
                return "'" + new Character (
                    (char) Integer.parseInt (var.getValue ())
                ) + "'";
            case TIME:
                if ("long".equals(type)) {
                    return new Date(Long.parseLong(var.getValue ())).toString();
                }
            default:
                return var.getValue ();
            }
        } catch (NumberFormatException nfex) {
            return nfex.getLocalizedMessage();
        }
    }

    private Object setValue (Variable var, NumericDisplaySettings settings, String origValue) {
        if (settings == null) return origValue;
        String type = var.getType ();
        try {
            switch (settings) {
            case BINARY:
                if ("int".equals(type))
                    return Integer.toString(Integer.parseInt(origValue, 2));
                else if ("short".equals(type)) {
                    return Short.toString(Short.parseShort(origValue, 2));
                } else if ("byte".equals(type)) {
                    return Byte.toString(Byte.parseByte(origValue, 2));
                } else if ("char".equals(type)) {
                    return "'"+Character.toString((char) Integer.parseInt(origValue, 2))+"'";
                } else {//if ("long".equals(type)) {
                    return Long.toString(Long.parseLong(origValue, 2))+"l";
                }
            default:
                return origValue;
            }
        } catch (NumberFormatException nfex) {
            return nfex.getLocalizedMessage();
        }
    }
    
    private boolean isIntegralType (Variable v) {
        if (!VariablesTreeModelFilter.isEvaluated(v)) {
            return false;
        }
        
        String type = v.getType ();
        return "int".equals (type) || 
            "char".equals (type) || 
            "byte".equals (type) || 
            "long".equals (type) || 
            "short".equals (type);
    }

    private boolean isIntegralTypeOrArray(Variable v) {
        if (!VariablesTreeModelFilter.isEvaluated(v)) {
            return false;
        }

        String type = removeArray(v.getType());
        return "int".equals (type) ||
            "char".equals (type) ||
            "byte".equals (type) ||
            "long".equals (type) ||
            "short".equals (type);
    }

    private static String removeArray(String type) {
        if (type.length() > 0 && type.endsWith("[]")) { // NOI18N
            return type.substring(0, type.length() - 2);
        } else {
            return type;
        }
    }

    private class DisplayAsAction extends AbstractAction 
    implements Presenter.Popup {

        private Variable variable;
        private String type;

        public DisplayAsAction(Variable variable) {
            this.variable = variable;
            this.type = removeArray(variable.getType());
        }

        public void actionPerformed(ActionEvent e) {
        }

        public JMenuItem getPopupPresenter() {
            JMenu displayAsPopup = new JMenu 
                (NbBundle.getMessage(NumericDisplayFilter.class, "CTL_Variable_DisplayAs_Popup"));

            JRadioButtonMenuItem decimalItem = new DisplayAsMenuItem (
                    "CTL_Variable_DisplayAs_Decimal",       // NOI18N
                    NumericDisplaySettings.DECIMAL
            );
            JRadioButtonMenuItem hexadecimalItem = new DisplayAsMenuItem (
                    "CTL_Variable_DisplayAs_Hexadecimal",   // NOI18N
                    NumericDisplaySettings.HEXADECIMAL
            );
            JRadioButtonMenuItem octalItem = new DisplayAsMenuItem (
                    "CTL_Variable_DisplayAs_Octal",         // NOI18N
                    NumericDisplaySettings.OCTAL
            );
            JRadioButtonMenuItem binaryItem = new DisplayAsMenuItem (
                    "CTL_Variable_DisplayAs_Binary",        // NOI18N
                    NumericDisplaySettings.BINARY
            );
            JRadioButtonMenuItem charItem = new DisplayAsMenuItem (
                    "CTL_Variable_DisplayAs_Character",     // NOI18N
                    NumericDisplaySettings.CHAR
            );
            JRadioButtonMenuItem timeItem = new DisplayAsMenuItem (
                    "CTL_Variable_DisplayAs_Time",          // NOI18N
                    NumericDisplaySettings.TIME
            );

            NumericDisplaySettings lds = (NumericDisplaySettings) 
                variableToDisplaySettings.get (variable);
            if (lds != null) {
                switch (lds) {
                case DECIMAL:
                    decimalItem.setSelected (true);
                    break;
                case HEXADECIMAL:
                    hexadecimalItem.setSelected (true);
                    break;
                case OCTAL:
                    octalItem.setSelected (true);
                    break;
                case BINARY:
                    binaryItem.setSelected (true);
                    break;
                case CHAR:
                    charItem.setSelected (true);
                    break;
                case TIME:
                    timeItem.setSelected (true);
                    break;
                }
            } else {
                if ("char".equals(type)) {
                    charItem.setSelected(true);
                } else {
                    decimalItem.setSelected (true);
                }
            }

            displayAsPopup.add (decimalItem);
            displayAsPopup.add (hexadecimalItem);
            displayAsPopup.add (octalItem);
            displayAsPopup.add (binaryItem);
            displayAsPopup.add (charItem);
            if ("long".equals(type)) {
                displayAsPopup.add (timeItem);
            }
            return displayAsPopup;
        }

        private void onDisplayAs (NumericDisplaySettings how) {
            NumericDisplaySettings lds = (NumericDisplaySettings) 
                variableToDisplaySettings.get (variable);
            if (lds == null) {
                if ("char".equals(type)) {
                    lds = NumericDisplaySettings.CHAR;
                } else {
                    lds = NumericDisplaySettings.DECIMAL;
                }
            }
            if (lds == how) return;
            variableToDisplaySettings.put (variable, how);
            fireModelChanged ();
        }
        
        private void fireModelChanged () {
            if (listeners == null) return;
            ModelEvent evt = new ModelEvent.TableValueChanged(this, variable, null);
            for (Iterator i = listeners.iterator (); i.hasNext ();) {
                ModelListener listener = (ModelListener) i.next ();
                listener.modelChanged (evt);
            }
        }

        private class DisplayAsMenuItem extends JRadioButtonMenuItem {

            public DisplayAsMenuItem(final String message, final NumericDisplaySettings as) {
                super(new AbstractAction(NbBundle.getMessage(NumericDisplayFilter.class, message)) {
                        public void actionPerformed (ActionEvent e) {
                            onDisplayAs (as);
                        }
                    });
            }

        }

    }

    
}
