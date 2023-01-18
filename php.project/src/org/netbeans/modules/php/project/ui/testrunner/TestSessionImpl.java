/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.project.ui.testrunner;

import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.gsf.testrunner.ui.api.Manager;
import org.netbeans.modules.php.spi.testing.PhpTestingProvider;
import org.netbeans.modules.php.spi.testing.coverage.Coverage;
import org.netbeans.modules.php.spi.testing.run.OutputLineHandler;
import org.netbeans.modules.php.spi.testing.run.TestSession;
import org.netbeans.modules.php.spi.testing.run.TestSuite;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.windows.OutputWriter;

public class TestSessionImpl implements TestSession {

    private final Manager manager;
    private final org.netbeans.modules.gsf.testrunner.api.TestSession testSession;
    private final PhpTestingProvider testingProvider;

    private volatile Coverage coverage;
    private volatile boolean coverageSet = false;
    private volatile boolean frozen = false;
    private volatile boolean testException = false;


    TestSessionImpl(Manager manager, org.netbeans.modules.gsf.testrunner.api.TestSession testSession, PhpTestingProvider testingProvider) {
        assert manager != null;
        assert testSession != null;
        assert testingProvider != null;
        this.manager = manager;
        this.testSession = testSession;
        this.testingProvider = testingProvider;
    }

    @NbBundle.Messages({
        "# {0} - provider name",
        "# {1} - suite name",
        "TestSessionImpl.suite.name=[{0}] {1}",
    })
    @Override
    public TestSuite addTestSuite(String name, FileObject location) {
        Parameters.notWhitespace("name", name); // NOI18N
        checkFrozen();
        String suiteName = Bundle.TestSessionImpl_suite_name(testingProvider.getDisplayName(), name);
        org.netbeans.modules.gsf.testrunner.api.TestSuite testSuite = new org.netbeans.modules.gsf.testrunner.api.TestSuite(suiteName);
        manager.displaySuiteRunning(testSession, suiteName);
        testSession.addSuite(testSuite);
        return new TestSuiteImpl(this, testSuite, location);
    }

    @Override
    public void setOutputLineHandler(OutputLineHandler outputLineHandler) {
        Parameters.notNull("outputLineHandler", outputLineHandler); // NOI18N
        Manager.getInstance().setOutputLineHandler(map(outputLineHandler));
    }

    @Override
    public void printMessage(String message, boolean error) {
        Parameters.notNull("message", message); // NOI18N
        manager.displayOutput(testSession, message, error);
    }

    public PhpTestingProvider getTestingProvider() {
        return testingProvider;
    }

    @Override
    public void setCoverage(Coverage coverage) {
        coverageSet = true;
        this.coverage = coverage;
    }

    @CheckForNull
    public Coverage getCoverage() {
        return coverage;
    }

    public boolean isCoverageSet() {
        return coverageSet;
    }

    public Manager getManager() {
        return manager;
    }

    public org.netbeans.modules.gsf.testrunner.api.TestSession getTestSession() {
        return testSession;
    }

    public boolean isTestException() {
        return testException;
    }

    public void setTestException(boolean testException) {
        this.testException = testException;
    }

    void freeze() {
        frozen = true;
    }

    void checkFrozen() {
        if (frozen) {
            throw new IllegalStateException("Test session is already frozen (PhpTestingProvider.runTests() already finished)");
        }
    }

    //~ Mappers

    private org.netbeans.modules.gsf.testrunner.ui.api.OutputLineHandler map(final OutputLineHandler outputLineHandler) {
        return new org.netbeans.modules.gsf.testrunner.ui.api.OutputLineHandler() {
            @Override
            public void handleLine(OutputWriter out, String text) {
                outputLineHandler.handleLine(out, text);
            }
        };
    }

}
