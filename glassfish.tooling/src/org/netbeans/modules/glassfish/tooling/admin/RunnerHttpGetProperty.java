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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.logging.Level;
import org.netbeans.modules.glassfish.tooling.data.GlassFishServer;
import org.netbeans.modules.glassfish.tooling.logging.Logger;

/**
 *
 * @author Peter Benedikovic, Tomas Kraus
 */
public class RunnerHttpGetProperty extends RunnerHttp {

    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Logger instance for this class. */
    private static final Logger LOGGER
            = new Logger(RunnerHttpGetProperty.class);

    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** Returned value is map where key-value pairs returned by server
     *  are stored. */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    ResultMap<String, String> result;

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
    public RunnerHttpGetProperty(final GlassFishServer server,
            final Command command) {
        super(server, command,
                "pattern=" + ((CommandGetProperty)command).propertyPattern);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Implemented Abstract Methods                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create <code>ResultMap</code> object corresponding
     * to server get property command execution value to be returned.
     */
    @Override
    protected ResultMap<String, String> createResult() {
        return result = new ResultMap<String, String>();
    }

    @Override
    protected boolean processResponse() {
        final String METHOD = "processResponse";
        if (manifest == null) {
            LOGGER.log(Level.WARNING, METHOD, "manifestNull", query);
            return false;
        }
        result.value = new HashMap<String, String>();
        for (String encodedkey : manifest.getEntries().keySet()) {
            String key = "";
            try {
                if (null != encodedkey) {
                    key = encodedkey;
                    key = URLDecoder.decode(encodedkey, "UTF-8");
                }
            } catch (UnsupportedEncodingException uee) {
                LOGGER.log(Level.INFO, METHOD,
                        "unsupportedEncoding", encodedkey);
                LOGGER.log(Level.INFO, METHOD, "exceptionDetails", uee);
            } catch (IllegalArgumentException iae) {
                // Ignore this for now
            }
            int equalsIndex = key.indexOf('=');
            if (equalsIndex >= 0) {
                String keyPart = key.substring(0, equalsIndex);
                String valuePart = key.substring(equalsIndex + 1);
                try {
                    // Around Sept. 2008... 3.x servers were double encoding their
                    // responces.  It appears that has stopped
                    // (See http://netbeans.org/bugzilla/show_bug.cgi?id=195015)
                    // The open question is, "When did 3.x stop doing the double
                    // encode?" since we don't know... this strategy will work
                    // for us
                    //   Belt and suspenders, like
                    result.value.put(keyPart, valuePart);       // raw form
                    result.value.put(keyPart, URLDecoder.decode(valuePart,
                            "UTF-8"));                          // single decode
                    result.value.put(keyPart, URLDecoder.decode(result.value.
                            get(keyPart), "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    LOGGER.log(Level.INFO, METHOD,
                            "unsupportedEncoding", result.value.get(keyPart));
                } catch (IllegalArgumentException iae) {
                    LOGGER.log(Level.INFO, METHOD, "illegalArgument",
                            new Object[] {valuePart, result.value.get(keyPart)});
                }
            } else {
                LOGGER.log(Level.WARNING, METHOD, "emptyString", key);
                result.value.put(key, "");
            }
        }

        return true;
    }
}
