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

import java.util.Map;

/**
 * Command that creates connector resource with the specified JNDI name and
 * the interface definition for a resource adapter on server.
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 */
@RunnerHttpClass(runner=RunnerHttpCreateConnector.class)
@RunnerRestClass(runner = RunnerRestCreateConnector.class)
public class CommandCreateConnector extends CommandTarget {

    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Command string for create connector resource command. */
    private static final String COMMAND = "create-connector-resource";
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** The JNDI name of this connector resource. */
    final String jndiName;

    /** Connection pool unique name (and ID). */
    final String poolName;

    /** Optional properties for configuring the resource. */
    final Map<String, String> properties;

    /** If this object is enabled. */
    final boolean enabled;


    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an instance of GlassFish server create connector resource
     * command entity.
     * <p/>
     * @param jndiName   The JNDI name of this connector resource.
     * @param poolName   Connection pool unique name (and ID).
     * @param properties Optional properties for configuring the pool.
     * @param enabled    If this object is enabled.
     * @param target     Name of the cluster or instance where the resource
     *                   will be created.
     */
    public CommandCreateConnector(final String jndiName, final String poolName,
            final Map<String, String> properties, final boolean enabled, final
                    String target) {
        super(COMMAND, target);
        this.jndiName = jndiName;
        this.poolName = poolName;
        this.properties = properties;
        this.enabled = enabled;
    }
    
    /**
     * Constructs an instance of GlassFish server create connector resource
     * command entity.
     * <p/>
     * @param jndiName   The JNDI name of this connector resource.
     * @param poolName   Connection pool unique name (and ID).
     * @param properties Optional properties for configuring the pool.
     * @param enabled    If this object is enabled.
     */
    public CommandCreateConnector(final String jndiName, final String poolName,
            final Map<String, String> properties, final boolean enabled) {
        this(jndiName, poolName, properties, enabled, null);
    }
    
    

    /**
     * Constructs an instance of GlassFish server create connector resource
     * command entity.
     * <p/>
     * This object will be enabled on server by default.
     * <p/>
     * @param jndiName   The JNDI name of this connector resource.
     * @param poolName   Connection pool unique name (and ID).
     * @param properties Optional properties for configuring the pool.
     */
    public CommandCreateConnector(final String jndiName, final String poolName,
            final Map<String, String> properties) {
        this(jndiName, poolName, properties, true);
    }

}
