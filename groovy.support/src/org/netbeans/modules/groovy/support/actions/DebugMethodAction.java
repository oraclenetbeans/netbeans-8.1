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

package org.netbeans.modules.groovy.support.actions;

import java.util.Collection;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodDebuggerProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

/**
 *
 * @author Martin Janicek
 */
//@ActionID(id = "org.netbeans.modules.groovy.support.actions.DebugMethodAction", category = "CommonTestRunner")
//@ActionRegistration(displayName = "#LBL_Action_DebugTestMethod")
//@ActionReference(path = "Editors/text/x-groovy/Popup", position=860)
//@NbBundle.Messages({"LBL_Action_DebugTestMethod=Debug Focused Test Method"})
public class DebugMethodAction extends NodeAction {

    public DebugMethodAction() {
        putValue("noIconInMenu", Boolean.TRUE);                         //NOI18N
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(DebugMethodAction.class, "LBL_Action_DebugTestMethod");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public boolean asynchronous() {
        return false;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        Collection<? extends TestMethodDebuggerProvider> providers = Lookup.getDefault().lookupAll(TestMethodDebuggerProvider.class);
        for (TestMethodDebuggerProvider provider : providers) {
            if(provider.canHandle(activatedNodes[0])) {
                provider.debugTestMethod(activatedNodes[0]);
                break;
            }
        }
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        return false;
//        if (activatedNodes.length == 0) {
//            return false;
//        }
//        Collection<? extends TestMethodDebuggerProvider> providers = Lookup.getDefault().lookupAll(TestMethodDebuggerProvider.class);
//        for (TestMethodDebuggerProvider provider : providers) {
//            if(provider.canHandle(activatedNodes[0])) {
//                return true;
//            }
//        }
//        return false;
    }

}
