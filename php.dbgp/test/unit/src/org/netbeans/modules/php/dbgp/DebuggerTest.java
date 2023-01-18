/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.dbgp;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.Callable;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.project.Project;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.php.dbgp.breakpoints.LineBreakpoint;
import org.netbeans.modules.php.dbgp.breakpoints.Utils;
import org.netbeans.modules.php.dbgp.packets.RunCommand;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.text.Line;
import org.openide.util.Cancellable;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * For running this test is necessary properly configured xdebug
 * @author Radek Matous
 */
public class DebuggerTest extends NbTestCase {

    public DebuggerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Utils.setLineFactory(new TestLineFactory());
        System.setProperty("TestRun", "On");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.setProperty("TestRun", "Off");
    }

    /**
     * Test of debug method, of class Debugger.
     */
    public void testStopAtBreakpoint()  throws Exception {
        FileObject scriptFo = createPHPTestFile("index.php");//NOI18N
        assertNotNull(scriptFo);
        Project dummyProject = DummyProject.create(scriptFo);
        assertNotNull(dummyProject);
        final SessionId sessionId = new SessionId(scriptFo, dummyProject);
        File scriptFile = FileUtil.toFile(scriptFo);
        assertNotNull(scriptFile);
        assertTrue(scriptFile.exists());
        final TestWrapper testWrapper = new TestWrapper(getTestForSuspendState(sessionId));
        addBreakpoint(scriptFo, 7, testWrapper, new RunContinuation(sessionId));
        startDebugging(sessionId, scriptFile);
        sessionId.isInitialized(true);
        testWrapper.assertTested();//sometimes, randomly fails
    }

    private static Breakpoint addBreakpoint(final FileObject fo, final int line, final TestWrapper testObj, final Continuation move) {
        Breakpoint breakpoint = new TestLineBreakpoint(createDummyLine(fo, line - 1, testObj, move));
        DebuggerManager.getDebuggerManager().addBreakpoint(breakpoint);
        return breakpoint;
    }

    private static class DummyProject implements Project {

        private final FileObject fo;

        static DummyProject create(FileObject fo) {
            return new DummyProject(fo);
        }

        private DummyProject(FileObject fo) {
            this.fo = fo;
        }

        @Override
        public FileObject getProjectDirectory() {
            return fo.getParent();
        }

        @Override
        public Lookup getLookup() {
            return Lookup.EMPTY;
        }

    }

    static Line createDummyLine(final FileObject fo, final int editorLineNum, final TestWrapper testObj, final Continuation move) {
        return new Line(Lookups.singleton(fo)) {

            @Override
            public int getLineNumber() {
                return editorLineNum;
            }

            @Override
            public void show(int kind) {
                testObj.test();
                move.goAhead();
            }

            @Override
            public void show(int kind, int column) {
                testObj.test();
                move.goAhead();
            }

            @Override
            public void setBreakpoint(boolean b) {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public boolean isBreakpoint() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public void markError() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public void unmarkError() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public void markCurrentLine() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public void unmarkCurrentLine() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

    private static class TestLineBreakpoint extends LineBreakpoint {

        public TestLineBreakpoint(Line line) {
            super(line);
        }

        @Override
        public boolean isSessionRelated(DebugSession session) {
            return true;
        }
    }

    private static class TestLineFactory extends Utils.LineFactory {

        @Override
        public Line getLine(int line, String remoteFileName, SessionId id) {
            Breakpoint[] breakpoints = DebuggerManager.getDebuggerManager().getBreakpoints();
            for (Breakpoint breakpoint : breakpoints) {
                if (breakpoint instanceof TestLineBreakpoint) {
                    TestLineBreakpoint lineBreakpoint = (TestLineBreakpoint) breakpoint;
                    Line lineObj = lineBreakpoint.getLine();
                    Lookup lkp = lineObj.getLookup();
                    FileObject fo = lkp.lookup(FileObject.class);
                    try {
                        URL remoteURL = new URL(remoteFileName);
                        FileObject remoteFo = URLMapper.findFileObject(remoteURL);
                        if (remoteFo == fo && line == lineObj.getLineNumber() + 1) {
                            return lineObj;
                        }
                    } catch (MalformedURLException ex) {
                        break;
                    }
                }
            }
            return super.getLine(line, remoteFileName, id);
        }
    }

    private static class BasedOnSession {

        private SessionId sessionId;

        BasedOnSession(SessionId sessionId) {
            this.sessionId = sessionId;
        }

        SessionId getSessionId() {
            return sessionId;
        }

        DebugSession getDebugSession() {
            return ConversionUtils.toDebugSession(getSessionId());
        }
    }

    private static class TestWrapper {
        private Runnable test;
        private boolean isTested = false;
        private static int WAIT_TIME = 3000;

        TestWrapper(Runnable test) {
            this.test = test;
        }

        synchronized void assertTested() throws InterruptedException {
            if (!isTested) {
                wait(WAIT_TIME);
            }
            assertTrue(isTested);
        }

        synchronized void setAsTested() {
            isTested = true;
            notifyAll();
        }

        void test() {
            this.test.run();
            setAsTested();
        }
    }

    private abstract static class Continuation extends BasedOnSession {
        Continuation(SessionId sessionId) {
            super(sessionId);
        }

        abstract void goAhead();
    }

    private static class RunContinuation extends Continuation {
        RunContinuation(SessionId sessionId) {
            super(sessionId);
        }

        @Override
        void goAhead() {
            DebugSession debugSession = getDebugSession();
            RunCommand command = new RunCommand(debugSession.getTransactionId());
            debugSession.sendCommandLater(command);
        }
    }

    private static FileObject createPHPTestFile(String scriptName) {
        URL urlToScript = DebuggerTest.class.getResource("resources/" + scriptName);
        FileObject scriptFo = URLMapper.findFileObject(urlToScript);
        return scriptFo;
    }

    private String gePHPInterpreter() {
        String command = DebuggerOptions.getGlobalInstance().getPhpInterpreter();
        if (command == null) {
            /*TODO: use more sophisticated code here for individual platforms
             * to find out php (such a code exists in  SystemPackageFinder.getPhpInterpreterAny());
             */
            command = "/usr/bin/php";
        }
        return command;
    }

    private Runnable getTestForSuspendState(final SessionId sessionId) {
    return  new Runnable() {
        @Override
            public void run() {
                //TODO: can be tested much more here - not ready yet
                Session session = DebuggerManager.getDebuggerManager().getCurrentSession();
                assertNotNull(session);
                DebuggerEngine engine = session.getCurrentEngine();
                assertNotNull(engine);
                ActionsManager actionManager = engine.getActionsManager();
                assertNotNull(actionManager);
                DebugSession debugSession = ConversionUtils.toDebugSession(session);
                assertNotNull(debugSession);
                assertEquals(sessionId, debugSession.getSessionId());
            }
        };
    }

    private void startDebugging(final SessionId sessionId, File scriptFile) {
        final ProcessBuilder processBuilder = new ProcessBuilder(new String[]{gePHPInterpreter(), scriptFile.getAbsolutePath()});
        processBuilder.directory(scriptFile.getParentFile());
        processBuilder.environment().put("XDEBUG_CONFIG", "idekey=" + sessionId.getId()); //NOI18N
        final DebuggerOptions options = DebuggerOptions.getGlobalInstance();
        options.pathMapping = Collections.emptyList();
        SessionManager.getInstance().startSession(sessionId, options, new Callable<Cancellable>() {
            @Override
            public Cancellable call() throws Exception {
                processBuilder.start();
                return new Cancellable() {

                    @Override
                    public boolean cancel() {
                        return true;
                    }
                };
            }
        });

    }
}
