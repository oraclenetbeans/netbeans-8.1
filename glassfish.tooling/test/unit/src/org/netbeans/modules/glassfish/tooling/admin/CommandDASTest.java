/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://java.net/projects/gf-tooling/pages/License or LICENSE.TXT.
 * See the License for the specific language governing permissions
 * and limitations under the License.  When distributing the software,
 * include this License Header Notice in each file and include the License
 * file at LICENSE.TXT. Oracle designates this particular file as subject
 * to the "Classpath" exception as provided by Oracle in the GPL Version 2
 * section of the License file that accompanied this code. If applicable,
 * add the following below the License Header, with the fields enclosed
 * by brackets [] replaced by your own identifying information:
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
package org.netbeans.modules.glassfish.tooling.admin;

import org.netbeans.modules.glassfish.tooling.TaskState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import org.netbeans.modules.glassfish.tooling.GlassFishIdeException;
import org.netbeans.modules.glassfish.tooling.data.GlassFishServer;
import org.netbeans.modules.glassfish.tooling.data.StartupArgsEntity;
import org.netbeans.modules.glassfish.tooling.server.ServerTasks;
import org.netbeans.modules.glassfish.tooling.utils.StreamLinesList;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * GlassFish DAS commands execution test.
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 */
@Test(enabled = false, groups = {"unit-tests"})
public class CommandDASTest extends CommandHttpTest {
    
    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // Test methods                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Test GlasFissh start local DAS administration HTTP command execution.
     */
    @SuppressWarnings("SleepWhileInLoop")
    @Test
    public void testCommandStartDAS() {
        String javaArgsProperty = getGlassFishProperty(GFPROP_JAVA_ARGS);
        String glassFishArgsProperty = getGlassFishProperty(
                GFPROP_GLASSFISH_ARGS);
        List<String> javaArgs = javaArgsProperty != null
                ? Arrays.asList(javaArgsProperty.split(" +"))
                : new ArrayList<String>();
        List<String> glassFishArgs = glassFishArgsProperty != null
                ? Arrays.asList(glassFishArgsProperty.split(" +"))
                : new ArrayList<String>();
        StartupArgsEntity startupArgs = new StartupArgsEntity(
                glassFishArgs,
                javaArgs,
                new HashMap<String, String>(),
                getJdkProperty(JDKPROP_HOME));
        GlassFishServer server = glassFishServer();
        ResultProcess result = ServerTasks.startServer(server, startupArgs);

        ValueProcess process = result.getValue();
        StreamLinesList stdOut = new StreamLinesList(
                process.getProcess().getInputStream());
        boolean exit = false;
        boolean started = false;
        boolean shutdownOnError = false;
        int emptyCycles = 0;
        while (!exit && emptyCycles++ < 10) {
            String line;                    
            while ((line = stdOut.getNext()) != null) {
                Matcher startedMessageMatcher
                        = STARTED_MESSAGE_PATTERN.matcher(line);
                Matcher shutdownMessageMatcher
                        = SHUTDOWN_MESSAGE_PATTERN.matcher(line);
                if (startedMessageMatcher.matches()) {
                    started = true;
                    exit = true;
                }
                if (shutdownMessageMatcher.matches()) {
                    shutdownOnError = true;
                }
                System.out.println("STDOUT: "+line);
                emptyCycles = 0;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                fail("FetchLogData command execution failed: "
                        + ie.getMessage());
            }
        }
//                stdOut.close();
//                stdErr.close();
        assertNotNull(result.getValue());
        assertTrue(started);
        if (shutdownOnError) {
            System.out.println("GlassFish exited on error!");
        }
    }

    /**
     * Test GlasFissh stop DAS administration HTTP command execution.
     */
    @Test//(dependsOnMethods = {"testCommandStartDAS"})
    public void testCommandStopDAS() {
        GlassFishServer server = glassFishServer();
        Command command = new CommandStopDAS();
        try {
            Future<ResultString> future = 
                    ServerAdmin.<ResultString>exec(server, command);
            try {
                ResultString result = future.get();
                assertNotNull(result.getValue());
                assertTrue(result.getState() == TaskState.COMPLETED);
            } catch (InterruptedException | ExecutionException ie) {
                fail("Version command execution failed: " + ie.getMessage());
            }
        } catch (GlassFishIdeException gfie) {
            fail("Version command execution failed: " + gfie.getMessage());
        }
    }

}
