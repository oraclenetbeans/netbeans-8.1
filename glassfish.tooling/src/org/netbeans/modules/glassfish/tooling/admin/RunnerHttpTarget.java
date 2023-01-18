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

import org.netbeans.modules.glassfish.tooling.data.GlassFishServer;
import org.netbeans.modules.glassfish.tooling.utils.Utils;

/**
 * GlassFish instance and cluster administration command with 
 * <code>DEFAULT=&lt;target&gt;</code> query execution using HTTP interface.
 * <p/>
 * Contains common code for commands that are called with
 * <code>DEFAULT=&lt;target&gt;</code> query string. Individual child classes
 * are not needed at this stage.
 * Class implements GlassFish server administration functionality trough HTTP
 * interface.
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 */
public class RunnerHttpTarget extends RunnerHttp {

    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Start/Stop command <code>DEFAULT</code> parameter's name. */
    private static final String DEFAULT_PARAM = "DEFAULT";

    ////////////////////////////////////////////////////////////////////////////
    // Static methods                                                         //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Builds enable/disable query string for given command.
     * <p/>
     * <code>QUERY :: "DEFAULT" '=' &lt;target&gt;</code>
     * <p/>
     * @param command GlassFish Server Administration Command Entity.
     *                <code>CommandDisable</code> instance is expected.
     * @return Enable/Disable query string for given command.
     */
    static String query(Command command) {
        String target;
        if (command instanceof CommandTarget) {
            target = Utils.sanitizeName(((CommandTarget)command).target);
        }
        else {
            throw new CommandException(
                    CommandException.ILLEGAL_COMAND_INSTANCE);
        }
        if (target == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder(
                    DEFAULT_PARAM.length() + 1 + target.length());
            sb.append(DEFAULT_PARAM);
            sb.append(PARAM_ASSIGN_VALUE);
            sb.append(target);
            return sb.toString();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an instance of administration command executor using
     * HTTP interface.
     * <p/>
     * @param server  GlassFish server entity object.
     * @param command GlassFish server administration command entity.
     */
    public RunnerHttpTarget(final GlassFishServer server,
            final Command command) {
        super(server, command, query(command));
    }

}
