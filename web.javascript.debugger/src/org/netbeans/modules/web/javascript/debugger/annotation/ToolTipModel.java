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
package org.netbeans.modules.web.javascript.debugger.annotation;

import org.netbeans.modules.javascript2.debug.tooltip.AbstractJSToolTipAnnotation;
import org.netbeans.modules.web.javascript.debugger.locals.VariablesModel;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.viewmodel.ExtendedNodeModel;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TreeExpansionModel;
import org.netbeans.spi.viewmodel.TreeExpansionModelFilter;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;

/**
 *
 * @author Martin
 */
@DebuggerServiceRegistration(path="javascript-debuggerengine/ToolTipView", types={ TreeModel.class, ExtendedNodeModel.class, TableModel.class, TreeExpansionModelFilter.class })
public class ToolTipModel extends VariablesModel implements TreeExpansionModelFilter {
    
    public ToolTipModel(ContextProvider contextProvider) {
        super(contextProvider);
    }

    @Override
    public int getChildrenCount(Object parent) throws UnknownTypeException {
        if (parent == ROOT) {
            return 1;
        } else {
            return super.getChildrenCount(parent);
        }
    }

    @Override
    public Object[] getChildren(Object parent, int from, int to) throws UnknownTypeException {
        if (parent == ROOT) {
            Object ttv = AbstractJSToolTipAnnotation.getTooltipVariable();
            if (ttv != null) {
                return new Object[] { ttv };
            } else {
                return new Object[] { };
            }
        } else {
            return super.getChildren(parent, from, to);
        }
    }

    @Override
    public boolean isExpanded(TreeExpansionModel original, Object node) throws UnknownTypeException {
        if (node == AbstractJSToolTipAnnotation.getTooltipVariable()) {
            return true;
        } else {
            return original.isExpanded(node);
        }
    }

    @Override
    public void nodeExpanded(Object node) {}

    @Override
    public void nodeCollapsed(Object node) {}
    
}
