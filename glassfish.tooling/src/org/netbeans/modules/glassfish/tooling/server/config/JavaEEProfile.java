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
package org.netbeans.modules.glassfish.tooling.server.config;

import java.util.HashMap;
import java.util.Map;

/**
 * JavaEE profiles supported by Glassfish.
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 */
public enum JavaEEProfile {

    ////////////////////////////////////////////////////////////////////////////
    // Enum values                                                            //
    ////////////////////////////////////////////////////////////////////////////

    /** JavaEE 1.2. */
    v1_2(Version.v1_2, Type.FULL, "1.2"),

    /** JavaEE 1.3. */
    v1_3(Version.v1_3, Type.FULL, "1.3"),

    /** JavaEE 1.4. */
    v1_4(Version.v1_4, Type.FULL, "1.4"),

    /** JavaEE 1.5. */
    v1_5(Version.v1_5, Type.FULL, "1.5"),

    /** JavaEE 1.6 web profile. */
    v1_6_web(Version.v1_6, Type.WEB, "1.6-web"),

    /** JavaEE 1.6 full profile. */
    v1_6(Version.v1_6, Type.FULL, "1.6"),

    /** JavaEE 1.7 web profile. */
    v1_7_web(Version.v1_7, Type.WEB, "1.7-web"),

    /** JavaEE 1.7 full profile. */
    v1_7(Version.v1_7, Type.FULL, "1.7");

    ////////////////////////////////////////////////////////////////////////////
    // Inner enums                                                            //
    ////////////////////////////////////////////////////////////////////////////

    /** JavaEE profile type. */
    public enum Type {
        /** Web profile. */
        WEB("web"),
        /** Full profile. */
        FULL("full");

        /** JavaEE profile type name. */
        private final String name;

        /**
         * Creates an instance of JavaEE profile type.
         * <p/>
         * @param name JavaEE profile type name.
         */
        private Type(final String name) {
            this.name = name;
        }

        /**
         * Converts JavaEE profile type value to <code>String</code>.
         * <p/>
         * @return A <code>String</code> representation of the value
         *         of this object.
         */
        @Override
        public String toString() {
            return this.name;
        }
    }

    /** JavaEE version. */
    public enum Version {
        /** JavaEE 1.2. */
        v1_2("1.2"),
        /** JavaEE 1.3. */
        v1_3("1.3"),
        /** JavaEE 1.4. */
        v1_4("1.4"),
        /** JavaEE 1.5. */
        v1_5("1.5"),
        /** JavaEE 1.6. */
        v1_6("1.6"),
        /** JavaEE 1.7. */
        v1_7("1.7");

        /** JavaEE profile type name. */
        private final String name;

        /**
         * Creates an instance of JavaEE profile type.
         * <p/>
         * @param name JavaEE profile type name.
         */
        private Version(final String name) {
            this.name = name;
        }

        /**
         * Converts JavaEE profile type value to <code>String</code>.
         * <p/>
         * @return A <code>String</code> representation of the value
         *         of this object.
         */
        @Override
        public String toString() {
            return this.name;
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** GlassFish JavaEE profile enumeration length. */
    public static final int length = JavaEEProfile.values().length;
    
    /** JavaEE profile type element separator character. */
    public static final char TYPE_SEPARATOR = '-';

    /** 
     * Stored <code>String</code> values for backward <code>String</code>
     * conversion.
     */
    private static final Map<String, JavaEEProfile> stringValuesMap
            = new HashMap<>(values().length);

    // Initialize backward String conversion Map.
    static {
        for (JavaEEProfile profile : JavaEEProfile.values()) {
            String[] names = createNames(profile);
            for (String name : names) {
                stringValuesMap.put(name.toUpperCase(), profile);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Static methods                                                         //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create JavaEE profile names to be recognized.
     * <p/>
     * @param profile JavaEE profile.
     * @return Array of names.
     * @throws ServerConfigException when JavaEE profile type is not recognized.
     */
    private static String[] createNames(final JavaEEProfile profile)
            throws ServerConfigException {
        String[] names;
        StringBuilder sb = new StringBuilder(profile.version.toString().length()
                + 1 + profile.type.toString().length());
        sb.append(profile.version.toString());
        sb.append(TYPE_SEPARATOR);
        sb.append(profile.type.toString());
        switch(profile.type) {
            // Full profile should recognize version base name and full name.
            case FULL:
                names = new String[2];
                names[0] = profile.version.toString();
                names[1] = sb.toString();
                break;
            // Web profile should regognize full name only .
            case WEB:
                names = new String[1];
                names[0] = sb.toString();
                break;
            // This is unrechable in regular conditions.
            default:
                throw new ServerConfigException(
                        ServerConfigException.INVALID_EE_PLATFORM_TYPE);
        }
        return names;
    } 

    /**
     * Returns a <code>JavaEEProfile</code> with a value represented by the
     * specified <code>String</code>. The <code>JavaEEProfile</code> returned
     * represents existing value only if specified <code>String</code>
     * matches any <code>String</code> returned by <code>toString</code> method.
     * Otherwise <code>null</code> value is returned.
     * <p>
     * @param name Value containing <code>JavaEEProfile</code> 
     *                    <code>toString</code> representation.
     * @return <code>JavaEEProfile</code> value represented
     *         by <code>String</code> or <code>null</code> if value
     *         was not recognized.
     */
    public static JavaEEProfile toValue(final String name) {
        if (name != null) {
            return (stringValuesMap.get(name.toUpperCase()));
        } else {
            return null;
        }
    }

    /**
     * Returns a <code>JavaEEProfile</code> with a value represented by the
     * specified <code>version</code> and <code>type</code>
     * <code>String</code>s. The <code>JavaEEProfile</code> returned
     * represents existing value only if specified <code>String</code>
     * matches any <code>String</code> returned by <code>toString</code> method.
     * Otherwise <code>null</code> value is returned.
     * <p>
     * @param version Value containing <code>JavaEEProfile</code> version
     *                <code>toString</code> representation.
     * @param type    Value containing <code>JavaEEProfile</code> type
     *                <code>toString</code> representation.
     * @return <code>JavaEEProfile</code> value represented
     *         by code>version</code> and <code>type</code> <code>String</code>
     *         or <code>null</code> if value was not recognized.
     */
    public static JavaEEProfile toValue(
            final String version, final String type) {
        if (version != null && type != null) {
            StringBuilder sb
                    = new StringBuilder(version.length() + 1 + type.length());
            sb.append(version);
            sb.append(TYPE_SEPARATOR);
            sb.append(type);
            return (stringValuesMap.get(sb.toString().toUpperCase()));
        } else {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** JavaEE profile version. */
    private final Version version;

    /** JavaEE profile type. */
    private final Type type;

    /** Name of JavaEE profile value. */
    private final String name;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an instance of JavaEE profiles supported by Glassfish.
     * <p/>
     * @param version JavaEE profile version.
     * @param type    JavaEE profile type.
     * @param name    Name of JavaEE profile value.
     */
    private JavaEEProfile(
            final Version version, final Type type, final String name) {
        this.version = version;
        this.type = type;
        this.name = name;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods                                                                //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Converts JavaEE profile version value to <code>String</code>.
     * <p/>
     * @return A <code>String</code> representation of the value of this object.
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Get profile type.
     * <p/>
     * @return Profile type.
     */
    public Type getType() {
        return type;
    }

}
