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
package org.netbeans.modules.cnd.remote.support;

import org.netbeans.modules.cnd.remote.test.RemoteTestBase;
import java.util.Map;
import junit.framework.Test;
import org.netbeans.modules.cnd.api.remote.HostInfoProvider;
import org.netbeans.modules.cnd.remote.test.RemoteDevelopmentTest;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.test.ForAllEnvironments;

/**
 * There hardly is a way to unit test remote operations.
 * This is just an entry point for manual validation.
 *
 * @author Sergey Grinev
 */
public class TransportTestCase extends RemoteTestBase {

    public TransportTestCase(String testName, ExecutionEnvironment execEnv) {
        super(testName, execEnv);
    }

    @ForAllEnvironments
    public void testRun() throws Exception {
        final String randomString = "i am just a random string, it does not matter that I mean";
        ProcessUtils.ExitStatus rcs = ProcessUtils.execute(getTestExecutionEnvironment(), "echo", randomString);
        assert rcs.exitCode == 0 : "echo command on remote server '" + getTestExecutionEnvironment() + "' returned " + rcs.exitCode;
        assert randomString.equals( rcs.output.trim()) : "echo command on remote server '" + getTestExecutionEnvironment() + "' produced unexpected output: " + rcs.output;
    }

    @ForAllEnvironments
    public void testFileExistst() throws Exception {
        assert HostInfoProvider.fileExists(getTestExecutionEnvironment(), "/etc/passwd");
        assert !HostInfoProvider.fileExists(getTestExecutionEnvironment(), "/etc/passwd/noway");
    }

    @ForAllEnvironments
    public void testGetEnv() throws Exception {
        Map<String, String> env = HostInfoProvider.getEnv(getTestExecutionEnvironment());
        System.err.println("Environment: " + env);
        assert env != null && env.size() > 0;
        assert env.containsKey("PATH") || env.containsKey("Path") || env.containsKey("path");
    }

    public static Test suite() {
        return new RemoteDevelopmentTest(TransportTestCase.class);
    }
}
