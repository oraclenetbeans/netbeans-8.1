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
package org.netbeans.modules.gsf.testrunner.ui;

import java.util.Collection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodDebuggerProvider;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodRunnerProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.TaskListener;
import org.openide.util.actions.NodeAction;

/**
 *
 * @author Theofanis Oikonomou
 */
@ActionID(id = "org.netbeans.modules.gsf.testrunner.TestMethodDebuggerAction", category = "CommonTestRunner")
@ActionRegistration(displayName = "#LBL_Action_DebugTestMethod")
@ActionReferences(value = {@ActionReference(path = "Editors/text/x-java/Popup", position=1797)})
@NbBundle.Messages({"LBL_Action_DebugTestMethod=Debug Focused Test Method"})
public class TestMethodDebuggerAction extends NodeAction {
    private RequestProcessor.Task debugMethodTask;
    private TestMethodDebuggerProvider debugMethodProvider;
    
    /** Creates a new instance of TestMethodDebuggerAction */
    public TestMethodDebuggerAction() {
        putValue("noIconInMenu", Boolean.TRUE);                         //NOI18N
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(TestMethodDebuggerAction.class, "LBL_Action_DebugTestMethod");
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
    protected void performAction(final Node[] activatedNodes) {
        final Collection<? extends TestMethodDebuggerProvider> providers = Lookup.getDefault().lookupAll(TestMethodDebuggerProvider.class);
        RequestProcessor RP = new RequestProcessor("TestMethodDebuggerAction", 1, true);   // NOI18N
	debugMethodTask = RP.create(new Runnable() {
	    @Override
	    public void run() {
		for (TestMethodDebuggerProvider provider : providers) {
		    if (provider.canHandle(activatedNodes[0])) {
			debugMethodProvider = provider;
			break;
		    }
		}
	    }
	});
	final ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.Search_For_Provider(), debugMethodTask);
	debugMethodTask.addTaskListener(new TaskListener() {
	    @Override
	    public void taskFinished(org.openide.util.Task task) {
		ph.finish();
		if(debugMethodProvider == null) {
		    StatusDisplayer.getDefault().setStatusText(Bundle.No_Provider_Found());
		} else {
		    debugMethodProvider.debugTestMethod(activatedNodes[0]);
		}
	    }
	});
	ph.start();
	debugMethodTask.schedule(0);
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length == 0) {
            return false;
        }
        if(debugMethodTask != null && !debugMethodTask.isFinished()) {
	    return false;
	}
        Collection<? extends TestMethodDebuggerProvider> providers = Lookup.getDefault().lookupAll(TestMethodDebuggerProvider.class);
        for (TestMethodDebuggerProvider provider : providers) {
            if (provider.isTestClass(activatedNodes[0])) {
                return true;
            }
        }
        return false;
    }
    
}
