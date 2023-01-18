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
package org.netbeans.modules.glassfish.tooling.server.parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.netbeans.modules.glassfish.tooling.logging.Logger;
import org.netbeans.modules.glassfish.tooling.server.parser.TreeParser.Path;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Reads configuration of network listeners.
 * For each listener returns one {@link HttpData} object that contains
 * port number, protocol and information whether this protocol is secured.
 * <p/>
 * @author Peter Benedikovic, Tomas Kraus
 */
public class NetworkListenerReader extends TargetConfigReader implements
        XMLReader {
    
    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Logger instance for this class. */
    private static final Logger LOGGER
            = new Logger(NetworkListenerReader.class);

    public static final String DEFAULT_PATH =
            "/domain/configs/config/network-config/network-listeners/network-listener";
    
    private String path;
    
    private Map<String, HttpData> result;
    
    public NetworkListenerReader(String targetConfigName) {
        this(DEFAULT_PATH, targetConfigName);
    }
    
    public NetworkListenerReader(String path, String targetConfigName) {
        super(targetConfigName);
        this.path = path;
        this.result = new HashMap<String, HttpData>();
    }
    
    @Override
    public void readAttributes(String qname, Attributes attributes) throws
            SAXException {
        final String METHOD = "readAttributes";
        /*
         <network-listeners>
         <thread-pool max-thread-pool-size="20" min-thread-pool-size="2" thread-pool-id="http-thread-pool" max-queue-size="4096"></thread-pool>
         <network-listener port="8080" protocol="http-listener-1" transport="tcp" name="http-listener-1" thread-pool="http-thread-pool"></network-listener>
         <network-listener port="8181" enabled="false" protocol="http-listener-2" transport="tcp" name="http-listener-2" thread-pool="http-thread-pool"></network-listener>
         <network-listener port="4848" protocol="admin-listener" transport="tcp" name="admin-listener" thread-pool="http-thread-pool"></network-listener>
         </network-listeners>
         */
        if (readData) {
            try {
                String id = attributes.getValue("name");
                if (id != null && id.length() > 0) {
                    
                    if (attributes.getValue("port").startsWith("$")) {  //GlassFish v3.1 : ignore these template entries
                        return;
                    }
                    int port = Integer.parseInt(attributes.getValue("port"));
                    boolean secure = "true".equals(attributes.getValue(
                            "security-enabled"));
                    boolean enabled = !"false".equals(attributes.getValue(
                            "enabled"));
                    LOGGER.log(Level.INFO, METHOD, "port", new Object[] {
                        Integer.toString(port), Boolean.toString(enabled),
                        Boolean.toString(secure)});
                    if (enabled) {
                        HttpData data = new HttpData(id, port, secure);
                        LOGGER.log(Level.INFO, METHOD, "add", data);
                        result.put(id, data);
                    }
                } else {
                    LOGGER.log(Level.INFO, METHOD, "noName");
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.SEVERE, METHOD, "numberFormat", ex);
            }
        }
    }
    
    @Override
    public List<TreeParser.Path> getPathsToListen() {
        LinkedList<TreeParser.Path> paths = new LinkedList<TreeParser.Path>();
        paths.add(new Path(path, this));
        paths.add(new Path(CONFIG_PATH, new TargetConfigMarker()));
        return paths;
    }
    
    public Map<String, HttpData> getResult() {
        return result;
    }
}
