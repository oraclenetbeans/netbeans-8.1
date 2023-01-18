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
 */package org.netbeans.modules.glassfish.tooling.server.config;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Container of GlassFish JavaSE features configuration.
 * <p/>
 * @author Peter Benedikovic, Tomas Kraus
 */
public class JavaSESet extends JavaSet {

    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** Platforms retrieved from XML elements. */
    private final List<String> platforms;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an instance of container of GlassFish JavaSE features
     * configuration.
     * <p/>
     * @param platforms Platforms retrieved from XML elements.
     * @param version   Highest JavaSE specification version implemented.
     */
    public JavaSESet(final List<String> platforms, final String version) {
        super(version);
        this.platforms = platforms;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Getters and setters                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Get platforms retrieved from XML elements.
     * <p/>
     * @return Platforms retrieved from XML elements.
     */
    public List<String> getPlatforms() {
        return platforms;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods                                                                //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Build {@link Set} of {@link JavaSEPlatform} for known platforms
     * retrieved from XML elements.
     * <p/>
     * @return {@link Set} of {@link JavaSEPlatform} for known platforms.
     */
    public Set<JavaSEPlatform> platforms() {
        int size = platforms != null ? platforms.size() : 0;
        EnumSet<JavaSEPlatform> platformsSet
                = EnumSet.noneOf(JavaSEPlatform.class);
        if (size > 0) {
            for (String name : platforms) {
                JavaSEPlatform type = JavaSEPlatform.toValue(name);
                if (type != null) {
                    platformsSet.add(type);
                }
            }
        }
        return platformsSet;
    }

}
