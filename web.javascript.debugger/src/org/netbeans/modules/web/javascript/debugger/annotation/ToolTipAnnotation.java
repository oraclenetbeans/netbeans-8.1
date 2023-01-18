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
package org.netbeans.modules.web.javascript.debugger.annotation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.PopupManager;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.ext.ToolTipSupport;
import org.netbeans.modules.javascript2.debug.tooltip.AbstractJSToolTipAnnotation;
import org.netbeans.modules.web.javascript.debugger.eval.Evaluator;
import org.netbeans.modules.web.javascript.debugger.locals.VariablesModel;
import org.netbeans.modules.web.javascript.debugger.locals.VariablesModel.ScopedRemoteObject;
import org.netbeans.modules.web.javascript.debugger.watches.WatchesModel;
import org.netbeans.modules.web.webkit.debugging.api.Debugger;
import org.netbeans.modules.web.webkit.debugging.api.debugger.CallFrame;
import org.netbeans.modules.web.webkit.debugging.api.debugger.RemoteObject;
import org.netbeans.modules.web.webkit.debugging.api.debugger.RemoteObject.Type;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.text.Annotation;
import org.openide.text.DataEditorSupport;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.Pair;
import org.openide.util.RequestProcessor;


/**
 * @author ads
 *
 */
@NbBundle.Messages({
    "# {0} - variable name",
    "var.undefined={0} is not defined"
})
public class ToolTipAnnotation extends AbstractJSToolTipAnnotation<WebJSDebuggerTooltipSupport>
{
    
    @Override
    protected WebJSDebuggerTooltipSupport getEngineDebugger(Session session, DebuggerEngine engine) {
        Debugger d = engine.lookupFirst(null, Debugger.class);
        if (d == null || !d.isSuspended()) {
            return null;
        }
        CallFrame currentCallFrame = d.getCurrentCallFrame();
        return new WebJSDebuggerTooltipSupport(d, currentCallFrame);
    }

    @Override
    protected Pair<String, Object> evaluate(String expression, DebuggerEngine engine, WebJSDebuggerTooltipSupport dbg) throws CancellationException {
        VariablesModel.ScopedRemoteObject sv = Evaluator.evaluateExpression(dbg.getFrame(), expression, true);
        Object tooltipVariable = null;
        String tooltipText;
        if (sv != null) {
            RemoteObject var = sv.getRemoteObject();
            String value = var.getValueAsString();
            Type type = var.getType();
            switch (type) {
                case STRING:
                    value = "\"" + value + "\"";
                    break;
                case FUNCTION:
                    value = var.getDescription();
                    break;
                case OBJECT:
                    String clazz = var.getClassName();
                    if (clazz == null) {
                        clazz = type.getName();
                    }
                    if (value.isEmpty()) {
                        value = var.getDescription();
                    }
                    value = "("+clazz+") "+value;
                    tooltipVariable = sv;
                    // TODO: add obj ID
            }
            if (type != Type.UNDEFINED) {
                tooltipText = expression + " = " + value;
            } else {
                tooltipText = var.getDescription();
                if (tooltipText == null) {
                    tooltipText = Bundle.var_undefined(expression);
                }
            }
        } else {
            throw new CancellationException();
        }
        return Pair.of(tooltipText, tooltipVariable);
    }

}
