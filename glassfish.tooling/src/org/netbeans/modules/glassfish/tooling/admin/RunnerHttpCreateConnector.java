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

/**
 * GlassFish server create connector resource administration command execution
 * using HTTP interface.
 * <p/>
 * Contains code for create connector resource command.
 * Class implements GlassFish server administration functionality trough HTTP
 * interface.
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 */
public class RunnerHttpCreateConnector extends RunnerHttp {
    

    /** Create connector resource command <code>jndi_name</code>
     *  parameter name. */
    private static final String JNDI_NAME_PARAM = "jndi_name";

    /** Create connector resource command <code>poolname</code>
     *  parameter name. */
    private static final String POOL_NAME_PARAM = "poolname";

    /** Create connector resource command <code>property</code>
     *  parameter name. */
    private static final String PROPERTY_PARAM = "property";

    /** Create connector resource command <code>enabled</code>
     *  parameter name. */
    private static final String ENABLED_PARAM = "enabled";

    ////////////////////////////////////////////////////////////////////////////
    // Static methods                                                         //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Builds create connector resource query string for given command.
     * <p/>
     * <code>QUERY :: "jndi_name" '=' &lt;jndiName&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ['&' "poolname" '=' &lt;poolName&gt; ]<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ['&' "properties" '=' &lt;pname&gt; '=' &lt;pvalue&gt;
     * { ':' &lt;pname&gt; '=' &lt;pvalue&gt;} ]</code>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ['&' "enabled" '=' true|false ]<br/>
     * <p/>
     * @param command GlassFish server administration command entity.
     *                <code>CommandCreateConnector</code> instance
     *                is expected.
     * @return Create connector resource query string for given command.
     */
    private static String query(final Command command) {
        String jndiName;
        String poolName;
        boolean enabled;
        if (command instanceof CommandCreateConnector) {
            jndiName = ((CommandCreateConnector)command).jndiName;
            poolName = ((CommandCreateConnector)command).poolName;
            enabled = ((CommandCreateConnector)command).enabled;
        } else {
            throw new CommandException(
                    CommandException.ILLEGAL_COMAND_INSTANCE);
        }
        boolean isPoolName = poolName != null && poolName.length() > 0;
        // Calculate StringBuilder initial length to avoid resizing
        StringBuilder sb = new StringBuilder(
                JNDI_NAME_PARAM.length() + 1 + jndiName.length()
                + ENABLED_PARAM.length() + 1 + toString(enabled).length()
                + ( isPoolName
                        ? POOL_NAME_PARAM.length() + 1 + poolName.length()
                        : 0)
                + queryPropertiesLength(
                        ((CommandCreateConnector)command).properties,
                        PROPERTY_PARAM));
        // Build query string
        sb.append(JNDI_NAME_PARAM).append(PARAM_ASSIGN_VALUE);
        sb.append(jndiName);
        sb.append(PARAM_SEPARATOR).append(ENABLED_PARAM);
        sb.append(PARAM_ASSIGN_VALUE).append(toString(enabled));
        if (isPoolName) {
             sb.append(PARAM_SEPARATOR).append(POOL_NAME_PARAM);
             sb.append(PARAM_ASSIGN_VALUE).append(poolName);
        }
        queryPropertiesAppend(sb,
                ((CommandCreateConnector)command).properties,
                PROPERTY_PARAM, true);
        return sb.toString();
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
    public RunnerHttpCreateConnector(final GlassFishServer server,
            final Command command) {
        super(server, command, query(command));
    }
}
