/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.testng.ui.actions;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.testng.api.TestNGSupport;
import org.netbeans.modules.testng.api.TestNGSupport.Action;
import org.netbeans.modules.testng.api.TestNGUtils;
import org.netbeans.modules.testng.spi.TestConfig;
import org.netbeans.modules.testng.spi.TestNGSupportImplementation.TestExecutor;
import org.netbeans.spi.project.SingleMethod;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

/**
 *
 * @author lukas
 */
@NbBundle.Messages("CTL_DebugTestMethodAction=Debug")
public class DebugTestMethodAction extends NodeAction {

    private static final Logger LOGGER = Logger.getLogger(RunTestMethodAction.class.getName());

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length != 1) {
            return false;
        }
        Lookup l = activatedNodes[0].getLookup();
        FileObject fileObject = l.lookup(FileObject.class);
        if (fileObject != null) {
            Project p = FileOwnerQuery.getOwner(fileObject);
            return TestNGSupport.isActionSupported(Action.DEBUG_TESTMETHOD, p);
        }
        SingleMethod sm = l.lookup(SingleMethod.class);
        if (sm != null) {
            Project p = FileOwnerQuery.getOwner(sm.getFile());
            return TestNGSupport.isActionSupported(Action.DEBUG_TESTMETHOD, p);
        }
        return false;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        Lookup l = activatedNodes[0].getLookup();
        EditorCookie ec = l.lookup(EditorCookie.class);
        SingleMethod sm = l.lookup(SingleMethod.class);
        if (ec == null && sm == null) {
            //should not happen
            throw new UnsupportedOperationException();
        }
        FileObject fo = null;
        String testMethod = null;
        TestClassInfoTask task = new TestClassInfoTask(0);
        if (ec != null) {
            JEditorPane[] panes = ec.getOpenedPanes();
            if (panes.length > 0) {
                final int cursor = panes[0].getCaret().getDot();
                JavaSource js = JavaSource.forDocument(panes[0].getDocument());
                task = new TestClassInfoTask(cursor);
                try {
                    js.runUserActionTask(task, true);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
                if (task.getMethodName() == null) {
                    //TODO - cursor is outside of a method or a given method is not a test
                    //so let allow user to choose any available method within given class
                    //using some UI
                }
                fo = l.lookup(FileObject.class);
                testMethod = task.getMethodName();
            }
        }
        if (sm != null) {
            fo = sm.getFile();
            testMethod = sm.getMethodName();
            JavaSource js = JavaSource.forFileObject(fo);
            if(js != null) {
                try {
                    js.runUserActionTask(task, true);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }
        }
        assert fo != null;
        Project p = FileOwnerQuery.getOwner(fo);
        TestExecutor exec = TestNGSupport.findTestNGSupport(p).createExecutor(p);
        TestConfig conf = TestNGUtils.getTestConfig(fo, false, task.getPackageName(), task.getClassName(), testMethod);
        try {
            exec.execute(Action.DEBUG_TESTMETHOD, conf);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public String getName() {
        return Bundle.CTL_DebugTestMethodAction();
    }

    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
