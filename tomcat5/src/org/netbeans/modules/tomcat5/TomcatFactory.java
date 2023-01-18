/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.tomcat5;

import org.netbeans.modules.tomcat5.deploy.TomcatManager;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.netbeans.modules.tomcat5.deploy.TomcatManager.TomEEType;
import org.netbeans.modules.tomcat5.deploy.TomcatManager.TomEEVersion;
import org.netbeans.modules.tomcat5.deploy.TomcatManager.TomcatVersion;
import org.openide.util.NbBundle;

/** 
 * Factory capable to create DeploymentManager that can deploy to Tomcat 5 and 6.
 *
 * Tomcat URI has following format:
 * <PRE><CODE>tomcat[55|60]:[home=&lt;home_path&gt;:[base=&lt;base_path&gt;:]]&lt;manager_app_url&gt;</CODE></PRE>
 * for example
 * <PRE><CODE>tomcat:http://localhost:8080/manager/</CODE></PRE>
 * where paths values will be used as CATALINA_HOME and CATALINA_BASE properties and manager_app_url
 * denotes URL of manager application configured on this server and has to start with <CODE>http:</CODE>.
 * @author Petr Hejl, Radim Kubacki
 */
public final class TomcatFactory implements DeploymentFactory {
    
    public static final String SERVER_ID_50 = "Tomcat";     // NOI18N
    public static final String SERVER_ID_55 = "Tomcat55";   // NOI18N
    public static final String SERVER_ID_60 = "Tomcat60";   // NOI18N
    public static final String SERVER_ID_70 = "Tomcat70";   // NOI18N
    public static final String SERVER_ID_80 = "Tomcat80";   // NOI18N
    
    public static final String TOMCAT_URI_PREFIX_50 = "tomcat:";    // NOI18N
    public static final String TOMCAT_URI_PREFIX_55 = "tomcat55:";  // NOI18N
    public static final String TOMCAT_URI_PREFIX_60 = "tomcat60:";  // NOI18N
    public static final String TOMCAT_URI_PREFIX_70 = "tomcat70:";  // NOI18N
    public static final String TOMCAT_URI_PREFIX_80 = "tomcat80:";  // NOI18N
    
    public static final String TOMCAT_URI_HOME_PREFIX = "home=";    // NOI18N
    public static final String TOMCAT_URI_BASE_PREFIX = ":base=";   // NOI18N

    private static final Pattern TOMEE_JAR_PATTERN = Pattern.compile("tomee-common-(\\d+(\\.\\d+)*).*\\.jar"); // NOI18N

    private static final Pattern TOMEE_JAXRS_JAR_PATTERN = Pattern.compile("tomee-jaxrs-(\\d+(\\.\\d+)*).*\\.jar"); // NOI18N

    private static final Pattern TOMEE_GERONIMO_JAR_PATTERN = Pattern.compile("geronimo-connector-(\\d+(\\.\\d+)*).*\\.jar"); // NOI18N

    private static final String GENERIC_DISCONNECTED_URI_PREFIX = "tomcat-any:"; // NOI18N
    private static final String GENERIC_DISCONNECTED_URI =
            GENERIC_DISCONNECTED_URI_PREFIX + "jakarta-tomcat-generic"; // NOI18N
    private static final String DISCONNECTED_URI_50 = TOMCAT_URI_PREFIX_50 + "jakarta-tomcat-5.0.x";    // NOI18N
    private static final String DISCONNECTED_URI_55 = TOMCAT_URI_PREFIX_55 + "jakarta-tomcat-5.5.x";  // NOI18N
    private static final String DISCONNECTED_URI_60 = TOMCAT_URI_PREFIX_60 + "apache-tomcat-6.0.x";   // NOI18N
    private static final String DISCONNECTED_URI_70 = TOMCAT_URI_PREFIX_70 + "apache-tomcat-7.0.x";   // NOI18N
    private static final String DISCONNECTED_URI_80 = TOMCAT_URI_PREFIX_80 + "apache-tomcat-8.0.x";   // NOI18N
    
    private static final Set<String> DISCONNECTED_URIS = new HashSet<String>();
    static {
        Collections.addAll(DISCONNECTED_URIS, DISCONNECTED_URI_50,
                DISCONNECTED_URI_55, DISCONNECTED_URI_60, DISCONNECTED_URI_70,
                DISCONNECTED_URI_80, GENERIC_DISCONNECTED_URI);
    }
    
    private static TomcatFactory instance;
    
    private static final WeakHashMap managerCache = new WeakHashMap();
    
    private static final Logger LOGGER = Logger.getLogger("org.netbeans.modules.tomcat5");  // NOI18N
    
    private TomcatFactory() {
        super();
    }

    public static TomcatFactory create50() {
        return getInstance();
    }

    public static synchronized TomcatFactory getInstance() {
        if (instance == null) {
            instance = new TomcatFactory();
            DeploymentFactoryManager.getInstance().registerDeploymentFactory(instance);
        }
        return instance;
    }
    
    /** Factory method to create DeploymentManager.
     * @param uri URL of configured manager application.
     * @param uname user with granted manager role
     * @param passwd user's password
     * @throws DeploymentManagerCreationException
     * @return {@link TomcatManager}
     */
    public DeploymentManager getDeploymentManager(String uri, String uname, String passwd)
            throws DeploymentManagerCreationException {
        if (!handlesURI (uri)) {
            throw new DeploymentManagerCreationException ("Invalid URI:" + uri); // NOI18N
        }
        // Lets reuse the same instance of TomcatManager for each server instance
        // during the IDE session, j2eeserver does not ensure this. Without it,
        // however, we could not rely on keeping data in the member variables.
        InstanceProperties ip = InstanceProperties.getInstanceProperties(uri);
        if (ip == null) {
            // null ip either means that the instance is not registered, or that this is the disconnected URL
            if (!DISCONNECTED_URIS.contains(uri)) {
                throw new DeploymentManagerCreationException("Tomcat instance: " + uri + " is not registered in the IDE."); // NOI18N
            }
        }
        synchronized (this) {
            TomcatManager tm = (TomcatManager)managerCache.get(ip);
            if (tm == null) {
                try {
                    TomcatVersion version = getTomcatVersion(uri);
                    tm = new TomcatManager(true, stripUriPrefix(uri, version), version);
                    managerCache.put(ip, tm);
                } catch (IllegalArgumentException iae) {
                    Throwable t = new DeploymentManagerCreationException("Cannot create deployment manager for Tomcat instance: " + uri + "."); // NOI18N
                    throw (DeploymentManagerCreationException)(t.initCause(iae));
                }
            }
            return tm;
        }
    }
    
    public DeploymentManager getDisconnectedDeploymentManager(String uri) 
    throws DeploymentManagerCreationException {
        // no need to distinguish beetween the connected and disconnected DM for Tomcat
        return getDeploymentManager(uri, null, null);
    }
    
    public String getDisplayName() {
        return NbBundle.getMessage(TomcatFactory.class, "LBL_TomcatFactory");
    }
    
    public String getProductVersion() {
        return NbBundle.getMessage(TomcatFactory.class, "LBL_TomcatFactoryVersion");
    }
    
    /**
     * @param str
     * @return <CODE>true</CODE> for URIs beggining with <CODE>tomcat[55|60]:</CODE> prefix
     */    
    public boolean handlesURI(String str) {
        return str != null && (str.startsWith(TOMCAT_URI_PREFIX_50)
                || str.startsWith(TOMCAT_URI_PREFIX_55)
                || str.startsWith(TOMCAT_URI_PREFIX_60)
                || str.startsWith(TOMCAT_URI_PREFIX_70)
                || str.startsWith(TOMCAT_URI_PREFIX_80));
    }
    
    /** 
     * Retrieve the tomcat version e.g. '6.0.10'
     * 
     * @throws IllegalStateException if the version information cannot be retrieved 
     */
    public static String getTomcatVersionString(File catalinaHome) throws IllegalStateException {
        File catalinaJar = new File(catalinaHome, "lib/catalina.jar"); // NOI18N
        if (!catalinaJar.exists()) {
            catalinaJar = new File(catalinaHome, "server/lib/catalina.jar"); // NOI18N
        }
        File coyoteJar = new File(catalinaHome, "lib/tomcat-coyote.jar"); // NOI18N

        try {
            URLClassLoader loader = new URLClassLoader(new URL[] {
                catalinaJar.toURI().toURL(), coyoteJar.toURI().toURL() });
            Class serverInfo = loader.loadClass("org.apache.catalina.util.ServerInfo"); // NOI18N
            try {
                Method method = serverInfo.getMethod("getServerNumber", new Class[] {}); // NOI18N
                String version = (String) method.invoke(serverInfo, new Object[] {});
                return version;
            } catch (NoSuchMethodException ex) {
                // try getServerInfo
            }

            Method method = serverInfo.getMethod("getServerInfo", new Class[] {}); // NOI18N
            String version = (String) method.invoke(serverInfo, new Object[] {});
            int idx = version.indexOf('/');
            if (idx > 0) {
                return version.substring(idx + 1);
            }
            throw new IllegalStateException("Cannot identify the version of the server."); // NOI18N
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static TomcatVersion getTomcatVersion(File catalinaHome) throws IllegalStateException {
        String version = null;
        try {
            // TODO we might use fallback as primary check - it might be faster
            // than loading jars and executing code in JVM
            version = getTomcatVersionString(catalinaHome);
        } catch (IllegalStateException ex) {
            LOGGER.log(Level.INFO, null, ex);
            return getTomcatVersionFallback(catalinaHome);
        } catch (UnsupportedClassVersionError err) {
            LOGGER.log(Level.INFO, null, err);
            return getTomcatVersionFallback(catalinaHome);
        }
        return getTomcatVersion(version, TomcatVersion.TOMCAT_50);
    }

    private static TomcatVersion getTomcatVersionFallback(File catalinaHome) throws IllegalStateException {
        File lib = new File(catalinaHome, "common" + File.separator + "lib"); // NOI18N
        if (lib.isDirectory()) {
            // 5 or 5.5
            File tasks = new File(catalinaHome, "bin" + File.separator + "catalina-tasks.xml"); // NOI18N
            if (tasks.isFile()) {
                // 5.5
                return TomcatVersion.TOMCAT_55;
            }
            return TomcatVersion.TOMCAT_50;
        } else {
            // 6 or 7 or 8
            File bootstrapJar = new File(catalinaHome, "bin" + File.separator + "bootstrap.jar"); // NOI18N
            if (!bootstrapJar.exists()) {
                return null;
            }
            try {
                JarFile jar = new JarFile(bootstrapJar);
                try {
                    Manifest manifest = jar.getManifest();
                    String specificationVersion = null;
                    if (manifest != null) {
                        specificationVersion = manifest.getMainAttributes()
                                .getValue("Specification-Version"); // NOI18N
                    }
                    if (specificationVersion != null) { // NOI18N
                        specificationVersion = specificationVersion.trim();
                        return getTomcatVersion(specificationVersion, TomcatVersion.TOMCAT_55);
                    }
                } finally {
                    try {
                        jar.close();
                    } catch (IOException ex) {
                        LOGGER.log(Level.FINEST, null, ex);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.FINE, null, e);
            }
        }
        return TomcatVersion.TOMCAT_50;
    }

    private static TomcatVersion getTomcatVersion(String version, TomcatVersion defaultVersion) throws IllegalStateException {
        if (version.startsWith("5.5.")) { // NOI18N
            return TomcatVersion.TOMCAT_55;
        } else if (version.startsWith("6.")) { // NOI18N
            return TomcatVersion.TOMCAT_60;
        } else if (version.startsWith("7.")) { // NOI18N
            return TomcatVersion.TOMCAT_70;
        } else if (version.startsWith("8.")) { // NOI18N
            return TomcatVersion.TOMCAT_80;
        }
        return defaultVersion;
    }

    private static TomcatVersion getTomcatVersion(String uri) throws IllegalStateException {
        if (uri.startsWith(TOMCAT_URI_PREFIX_80)) {
            return TomcatVersion.TOMCAT_80;
        } else if (uri.startsWith(TOMCAT_URI_PREFIX_70)) {
            return TomcatVersion.TOMCAT_70;
        } else if (uri.startsWith(TOMCAT_URI_PREFIX_60)) {
            return TomcatVersion.TOMCAT_60;
        } else if (uri.startsWith(TOMCAT_URI_PREFIX_55)) {
            return TomcatVersion.TOMCAT_55;
        }
        return TomcatVersion.TOMCAT_50;
    }

    public static TomEEVersion getTomEEVersion(File catalinaHome, File catalinaBase)
            throws IllegalStateException {
        File tomee = getTomEEJar(catalinaHome);
        return getTomEEVersion(tomee);
    }

    public static TomEEType getTomEEType(File catalinaHome, File catalinaBase)
            throws IllegalStateException {
        File tomee = getTomEEJar(catalinaHome);
        return getTomEEType(tomee.getParentFile());
    }

    @NonNull
    public static TomEEType getTomEEType(@NonNull File libFolder) {
        File[] children = libFolder.listFiles();
        TomEEType type = TomEEType.TOMEE_WEB;
        if (children != null) {
            for (File file : children) {
                if (TOMEE_JAXRS_JAR_PATTERN.matcher(file.getName()).matches()) {
                    if (type.ordinal() < TomEEType.TOMEE_JAXRS.ordinal()) {
                        type = TomEEType.TOMEE_JAXRS;
                    }
                } else if (TOMEE_GERONIMO_JAR_PATTERN.matcher(file.getName()).matches()) {
                    if (type.ordinal() < TomEEType.TOMEE_PLUS.ordinal()) {
                        type = TomEEType.TOMEE_PLUS;
                    }
                }
            }
        }
        return type;
    }

    public static TomEEVersion getTomEEVersion(File tomeeJar) throws IllegalStateException {
        if (tomeeJar != null) {
            Matcher matcher = TOMEE_JAR_PATTERN.matcher(tomeeJar.getName());
            if (matcher.matches()) {
                String versionString = matcher.group(1);
                return getTomEEVersion(versionString, TomEEVersion.TOMEE_15);
            }
        }

        return null;
    }

    @CheckForNull
    public static File getTomEEWebAppJar(File catalinaHome, File catalinaBase) {
        // XXX this is not really accurate as when using basedir
        // webapp from home may not be linked there we would have to check xml files
        File ret = getTomEEWebAppJar(catalinaHome);
        if (ret != null) {
            return ret;
        }
        if (catalinaBase != null) {
            return getTomEEWebAppJar(catalinaBase);
        }
        return null;
    }

    @CheckForNull
    public static File getTomEEWebAppJar(File parent) {
        File webApps = new File(parent, "webapps"); // NOI18N
        File[] children = webApps.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (children != null) {
            for (File child : children) {
                File jar = getTomEEJar(child);
                if (jar != null) {
                    return jar;
                }
            }
        }
        return null;
    }

    private static File getTomEEJar(File parentDir) throws IllegalStateException {
        File libDir = new File(parentDir, "lib"); // NOI18N
        String[] names = libDir.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return TOMEE_JAR_PATTERN.matcher(name).matches();
            }
        });
        if (names != null && names.length > 0) {
            // XXX based on filename we may improve it later
            return new File(libDir, names[0]);
        }
        return null;
    }

    private static TomEEVersion getTomEEVersion(String version, TomEEVersion defaultVersion) throws IllegalStateException {
        if (version.startsWith("1.5.")) { // NOI18N
            return TomcatManager.TomEEVersion.TOMEE_15;
        } else if (version.startsWith("1.6.")) { // NOI18N
            return TomcatManager.TomEEVersion.TOMEE_16;
        } else if (version.startsWith("1.7.")) { // NOI18N
            return TomcatManager.TomEEVersion.TOMEE_17;
        }
        return defaultVersion;
    }

    private static String stripUriPrefix(String uri, TomcatVersion tomcatVersion) {
        if (uri.startsWith(GENERIC_DISCONNECTED_URI_PREFIX)) {
            return uri.substring(GENERIC_DISCONNECTED_URI_PREFIX.length());
        }
        switch (tomcatVersion) {
            case TOMCAT_80:
                return uri.substring(TomcatFactory.TOMCAT_URI_PREFIX_80.length());
            case TOMCAT_70:
                return uri.substring(TomcatFactory.TOMCAT_URI_PREFIX_70.length());
            case TOMCAT_60: 
                return uri.substring(TomcatFactory.TOMCAT_URI_PREFIX_60.length());
            case TOMCAT_55: 
                return uri.substring(TomcatFactory.TOMCAT_URI_PREFIX_55.length());
            case TOMCAT_50: 
            default:
                return uri.substring(TomcatFactory.TOMCAT_URI_PREFIX_50.length());
        }        
    }
}
