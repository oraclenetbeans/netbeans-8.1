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

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import static org.netbeans.modules.glassfish.tooling.CommonTest.JDKPROP_HOME;
import org.netbeans.modules.glassfish.tooling.GlassFishIdeException;
import org.netbeans.modules.glassfish.tooling.TaskState;
import org.netbeans.modules.glassfish.tooling.data.GlassFishServer;
import org.netbeans.modules.glassfish.tooling.logging.Logger;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;

/**
 * <p/>
 * @author Tomas Kraus
 */
public class CommandAsadminTest extends CommandTest {
    
    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Logger instance for this class. */
    private static final Logger LOGGER = new Logger(CommandAsadminTest.class);

    ////////////////////////////////////////////////////////////////////////////
    // Test methods                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Test change administrator's password command execution using
     * local asadmin interface on GlassFish v3.
     */
    @Test
    public void testCommandChangeAdminPasswordGFv3() {
        final String METHOD = "testCommandChangeAdminPasswordGFv3";
        Properties properties = jdkProperties();
        GlassFishServer server = CommandHttpTest.glassFishServer();
        Command command = new CommandChangeAdminPassword(
                properties.getProperty(JDKPROP_HOME), "admin123");
        try {
            Future<ResultString> future =
                    ServerAdmin.<ResultString>exec(server, command);
            try {
                ResultString result = future.get();
                String output = result.getValue();
                LOGGER.log(Level.INFO, METHOD, "output", output);
                assertNotNull(result.getValue());
                assertTrue(result.getState() == TaskState.COMPLETED);
            } catch (InterruptedException | ExecutionException ie) {
                fail("Change administrator's password command execution failed: "
                        + ie.getMessage());
            }
        } catch (GlassFishIdeException gfie) {
            fail("Change administrator's password command execution failed: "
                    + gfie.getMessage());
        }
    }

    /**
     * Test change administrator's password command execution using
     * local asadmin interface on GlassFish v4.
     */
    @Test
    public void testCommandChangeAdminPasswordGFv4() {
        final String METHOD = "testCommandChangeAdminPasswordGFv4";
        Properties properties = jdkProperties();
        GlassFishServer server = CommandRestTest.glassFishServer();
        Command command = new CommandChangeAdminPassword(
                properties.getProperty(JDKPROP_HOME), "admin123");
        try {
            Future<ResultString> future =
                    ServerAdmin.<ResultString>exec(server, command);
            try {
                ResultString result = future.get();
                String output = result.getValue();
                LOGGER.log(Level.INFO, METHOD, "output", output);
                assertNotNull(result.getValue());
                assertTrue(result.getState() == TaskState.COMPLETED);
            } catch (InterruptedException | ExecutionException ie) {
                fail("Change administrator's password command execution failed: "
                        + ie.getMessage());
            }
        } catch (GlassFishIdeException gfie) {
            fail("Change administrator's password command execution failed: "
                    + gfie.getMessage());
        }
    }

}
