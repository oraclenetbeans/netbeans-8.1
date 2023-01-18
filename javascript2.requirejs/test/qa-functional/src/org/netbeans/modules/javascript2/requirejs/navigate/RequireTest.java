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
package org.netbeans.modules.javascript2.requirejs.navigate;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import junit.framework.Test;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.modules.javascript2.requirejs.GeneralRequire;

/**
 *
 * @author Vladimir Riha
 */
public class RequireTest extends GeneralRequire {

    public RequireTest(String arg0) {
        super(arg0);
    }

    public static Test suite() {
        return NbModuleSuite.create(
                NbModuleSuite.createConfiguration(RequireTest.class).addTest(
                        "openProject",
                        "testStdModule",
                        "testFncModule",
                        "testNewFncModule",
                        "testLiteralModule",
                        "testLibrary",
                        "testStdModuleParam",
                        "testFncModuleParam",
                        "testNewFncModuleParam",
                        "testLiteralModuleParam",
                        "testLibraryParam"
                ).enableModules(".*").clusters(".*").honorAutoloadEager(true));
    }

    public void openProject() throws Exception {
        startTest();
        RequireTest.currentProject = "SimpleRequire";
        JemmyProperties.setCurrentTimeout("ActionProducer.MaxActionTime", 180000);
        openDataProjects("SimpleRequire");
        openFile("js|main.js", "SimpleRequire");
        endTest();
    }

    public void testLibrary() {
        startTest();
        navigate("main.js", "piwik.js", 19, 25, 1, 1);
        endTest();
    }

    public void testStdModule() {
        startTest();
        navigate("main.js", "stdModule.js", 19, 96, 1, 1);
        endTest();
    }

    public void testFncModule() {
        startTest();
        navigate("main.js", "function.js", 19, 39, 1, 1);
        endTest();
    }

    public void testNewFncModule() {
        startTest();
        navigate("main.js", "newFunction.js", 19, 56, 1, 1);
        endTest();
    }

    public void testLiteralModule() {
        startTest();
        navigate("main.js", "objectLiteral.js", 19, 78, 1, 1);
        endTest();
    }

    public void testLibraryParam() {
        startTest();
        navigate("main.js", "piwik.js", 20, 22, 1, 1);
        endTest();
    }

    public void testStdModuleParam() {
        startTest();
        navigate("main.js", "stdModule.js", 20, 50, 1, 1);
        endTest();
    }

    public void testFncModuleParam() {
        startTest();
        navigate("main.js", "function.js", 20, 27, 1, 1);
        endTest();
    }

    public void testNewFncModuleParam() {
        startTest();
        navigate("main.js", "newFunction.js", 20, 33, 1, 1);
        endTest();
    }

    public void testLiteralModuleParam() {
        startTest();
        navigate("main.js", "objectLiteral.js", 20, 42, 1, 1);
        endTest();
    }

    private void navigate(String fromFile, String toFile, int fromLine, int fromColumn, int toLine, int toColumn) {
        EditorOperator eo = new EditorOperator(fromFile);
        eo.setCaretPosition(fromLine, fromColumn);
        evt.waitNoEvent(200);
        new org.netbeans.jellytools.actions.Action(null, null, KeyStroke.getKeyStroke(KeyEvent.VK_B, 2)).performShortcut(eo);
        evt.waitNoEvent(500);
        long defaultTimeout = JemmyProperties.getCurrentTimeout("ComponentOperator.WaitComponentTimeout");
        try {
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", 3000);
            EditorOperator ed = new EditorOperator(toFile);
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", defaultTimeout);
            int position = ed.txtEditorPane().getCaretPosition();
            ed.setCaretPosition(toLine, toColumn);
            int expectedPosition = ed.txtEditorPane().getCaretPosition();
            assertTrue("Incorrect caret position. Expected position " + expectedPosition + " but was " + position, position == expectedPosition);
            if (!fromFile.equals(toFile)) {
                ed.close(false);
            }
        } catch (Exception e) {
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", defaultTimeout);
            fail(e.getMessage());
        }

    }

}
