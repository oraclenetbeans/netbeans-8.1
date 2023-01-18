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
package org.netbeans.modules.glassfish.tooling.data;

import java.util.Set;
import org.netbeans.modules.glassfish.tooling.server.config.JavaSEPlatform;
import org.netbeans.modules.glassfish.tooling.server.config.JavaSESet;

/**
 * Container of GlassFish JavaSE features configuration.
 * <p/>
 * @author Peter Benedikovic, Tomas Kraus
 */
public class GlassFishJavaSEConfig {
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** Platforms retrieved from XML elements. */
    private final Set<JavaSEPlatform> platforms;

    /** Highest JavaEE specification version implemented. */
    private final String version;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an instance of avaEE configuration entity using JavaEE set
     * for GlassFish features configuration as source of instance content.
     * <p/>
     * @param javaSEconfig Container of GlassFish JavaEE features configuration.
     */
    public GlassFishJavaSEConfig(final JavaSESet javaSEconfig) {
        platforms = javaSEconfig.platforms();
        version = javaSEconfig.getVersion();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Getters and setters                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Get highest JavaSE specification version implemented.
     * <p/>
     * @return Highest JavaSE specification version implemented.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get supported JavaSE platforms.
     * <p/>
     * @return Supported JavaSE platforms.
     */
    public Set<JavaSEPlatform> getPlatforms() {
        return platforms;
    }

}
