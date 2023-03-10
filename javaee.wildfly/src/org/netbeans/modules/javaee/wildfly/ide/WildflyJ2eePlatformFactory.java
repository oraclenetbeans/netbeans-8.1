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
package org.netbeans.modules.javaee.wildfly.ide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.deploy.spi.DeploymentManager;
import org.netbeans.api.j2ee.core.Profile;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.modules.j2ee.deployment.common.api.J2eeLibraryTypeProvider;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule.Type;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eePlatform;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.netbeans.modules.j2ee.deployment.plugins.spi.J2eePlatformFactory;
import org.netbeans.modules.j2ee.deployment.plugins.spi.J2eePlatformImpl;
import org.netbeans.modules.j2ee.deployment.plugins.spi.J2eePlatformImpl2;
import org.netbeans.modules.j2ee.deployment.plugins.spi.support.LookupProviderSupport;
import org.netbeans.modules.javaee.specs.support.api.JaxWs;
import org.netbeans.modules.javaee.specs.support.spi.JaxRsStackSupportImplementation;
import org.netbeans.modules.javaee.wildfly.WildflyDeploymentManager;
import org.netbeans.modules.javaee.wildfly.util.WildFlyProperties;
import org.netbeans.modules.websvc.wsstack.api.WSStack;
import org.netbeans.modules.websvc.wsstack.spi.WSStackFactory;
import org.netbeans.spi.project.libraries.LibraryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Kirill Sorokin <Kirill.Sorokin@Sun.COM>
 */
public class WildflyJ2eePlatformFactory extends J2eePlatformFactory {

    static final String HIBERNATE_JPA_PROVIDER = "org.hibernate.ejb.HibernatePersistence";

    static final String TOPLINK_JPA_PROVIDER = "oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider";

    static final String KODO_JPA_PROVIDER = "kodo.persistence.PersistenceProviderImpl";

    private static final WeakHashMap<InstanceProperties, J2eePlatformImplImpl> instanceCache = new WeakHashMap<InstanceProperties, J2eePlatformImplImpl>();

    @Override
    public synchronized J2eePlatformImpl getJ2eePlatformImpl(DeploymentManager dm) {
        WildflyDeploymentManager manager = (WildflyDeploymentManager) dm;
        InstanceProperties ip = manager.getInstanceProperties();
        if (ip == null) {
            throw new RuntimeException("Cannot create J2eePlatformImpl instance for " + manager.getUrl()); // NOI18N
        }
        J2eePlatformImplImpl platform = instanceCache.get(ip);
        if (platform == null) {
            platform = new J2eePlatformImplImpl(manager.getProperties());
            instanceCache.put(ip, platform);
        }
        return platform;
    }

    public static class J2eePlatformImplImpl extends J2eePlatformImpl2 {

        private static final Set<Type> MODULE_TYPES = new HashSet<Type>();

        static {
            MODULE_TYPES.add(Type.EAR);
            MODULE_TYPES.add(Type.WAR);
            MODULE_TYPES.add(Type.EJB);
            MODULE_TYPES.add(Type.RAR);
            MODULE_TYPES.add(Type.CAR);
        }

        private static final Set<Profile> WILDFLY_PROFILES = new HashSet<Profile>();

        static {
            WILDFLY_PROFILES.add(Profile.JAVA_EE_6_WEB);
            WILDFLY_PROFILES.add(Profile.JAVA_EE_6_FULL);
            WILDFLY_PROFILES.add(Profile.JAVA_EE_7_WEB);
            WILDFLY_PROFILES.add(Profile.JAVA_EE_7_FULL);
        }

        private static final Set<Profile> EAP6_PROFILES = new HashSet<Profile>();

        static {
            EAP6_PROFILES.add(Profile.JAVA_EE_6_WEB);
            EAP6_PROFILES.add(Profile.JAVA_EE_6_FULL);
        }
        
        private static final Set<Profile> WILDFLY_WEB_PROFILES = new HashSet<Profile>();

        static {
            WILDFLY_WEB_PROFILES.add(Profile.JAVA_EE_6_WEB);
            WILDFLY_WEB_PROFILES.add(Profile.JAVA_EE_7_WEB);
        }
        private LibraryImplementation[] libraries;

        private final WildFlyProperties properties;

        public J2eePlatformImplImpl(WildFlyProperties properties) {
            this.properties = properties;
        }

        @Override
        public Set<org.netbeans.api.j2ee.core.Profile> getSupportedProfiles() {
            if (this.properties.isWildfly()) {
                if(this.properties.isServletOnly()) {
                    return Collections.unmodifiableSet(WILDFLY_WEB_PROFILES);
                }
                return Collections.unmodifiableSet(WILDFLY_PROFILES);
            }
            return Collections.unmodifiableSet(EAP6_PROFILES);
        }

        @Override
        public Set<org.netbeans.api.j2ee.core.Profile> getSupportedProfiles(Type moduleType) {
            if (this.properties.isWildfly()) {
                return Collections.unmodifiableSet(WILDFLY_PROFILES);
            }
            return Collections.unmodifiableSet(EAP6_PROFILES);
        }

        @Override
        public Set<Type> getSupportedTypes() {
            return Collections.unmodifiableSet(MODULE_TYPES);
        }

        @Override
        public Set/*<String>*/ getSupportedJavaPlatformVersions() {
            Set versions = new HashSet();
            versions.add("1.7"); // NOI18N
            versions.add("1.8"); // NOI18N
            return versions;
        }

        @Override
        public JavaPlatform getJavaPlatform() {
            return properties.getJavaPlatform();
        }

        @Override
        public File[] getPlatformRoots() {
            return new File[]{getServerHome(), getDomainHome()};
        }

        @Override
        public File getServerHome() {
            return properties.getRootDir();
        }

        @Override
        public File getDomainHome() {
            return properties.getServerDir();
        }

        @Override
        public File getMiddlewareHome() {
            return null;
        }

        private static class FF implements FilenameFilter {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") || new File(dir, name).isDirectory(); //NOI18N
            }
        }

        @Override
        public LibraryImplementation[] getLibraries() {
            if (libraries == null) {
                initLibraries();
            }
            return libraries.clone();
        }

        public void notifyLibrariesChanged() {
            initLibraries();
            firePropertyChange(PROP_LIBRARIES, null, libraries.clone());
        }

        @Override
        public java.awt.Image getIcon() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return NbBundle.getMessage(WildflyJ2eePlatformFactory.class, "TITLE_JBOSS_FACTORY");

        }

        @Override
        public boolean isToolSupported(String toolName) {

            if (J2eePlatform.TOOL_JSR109.equals(toolName)) {
                if (containsJaxWsLibraries()) {
                    return true;
                }
            }

            if (J2eePlatform.TOOL_WSIMPORT.equals(toolName)) {
                if (containsJaxWsLibraries()) {
                    return true;
                }
            }

            if (J2eePlatform.TOOL_WSGEN.equals(toolName)) {
                if (containsJaxWsLibraries()) {
                    return true;
                }
            }

            if ("JaxWs-in-j2ee14-supported".equals(toolName)) { //NOI18N
                if (containsJaxWsLibraries()) {
                    return true;
                }
            }

            if (!containsJaxWsLibraries()
                    && (J2eePlatform.TOOL_WSCOMPILE.equals(toolName) || J2eePlatform.TOOL_APP_CLIENT_RUNTIME.equals(toolName))) {
                return true;
            }

            if (HIBERNATE_JPA_PROVIDER.equals(toolName)
                    || TOPLINK_JPA_PROVIDER.equals(toolName)
                    || KODO_JPA_PROVIDER.equals(toolName)) {
                return containsPersistenceProvider(toolName);
            }

            if ("jpaversionverification".equals(toolName)) { // NOI18N
                return true;
            }
            if ("jpa1.0".equals(toolName)) { // NOI18N
                return true;
            }
            if ("jpa2.0".equals(toolName)) { // NOI18N
                return true;
            }
            if ("jpa2.1".equals(toolName)) { // NOI18N
                return this.properties.isWildfly();
            }

            if ("hibernatePersistenceProviderIsDefault2.0".equals(toolName)) {
                return true;
            }
            if ("defaultPersistenceProviderJavaEE5".equals(toolName)) {
                return true;
            }

            return false;
        }

        String getDefaultJpaProvider() {
            return HIBERNATE_JPA_PROVIDER;
        }

        private boolean containsJaxWsLibraries() {
            File[] jaxWsAPILib = new File(properties.getModulePath("org/jboss/ws/api/main")).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("jbossws-api") && name.endsWith("jar");
                }
            }); // NOI18N
            if (jaxWsAPILib != null && jaxWsAPILib.length == 1 && jaxWsAPILib[0].exists()) {
                return true;
            }
            jaxWsAPILib = new File(properties.getModulePath("javax/xml/ws/api/main")).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("jboss-jaxws-api") && name.endsWith("jar");
                }
            }); // NOI18N
            if (jaxWsAPILib != null && jaxWsAPILib.length == 1 && jaxWsAPILib[0].exists()) {
                return true;
            }
            return false;
        }

        boolean containsPersistenceProvider(String providerName) {
            return containsService(libraries, "javax.persistence.spi.PersistenceProvider", providerName);
        }

        private static boolean containsService(LibraryImplementation[] libraries, String serviceName, String serviceImplName) {
            for (LibraryImplementation libImpl : libraries) {
                if (containsService(libImpl, serviceName, serviceImplName)) { //NOI18N
                    return true;
                }
            }
            return false;
        }

        private static boolean containsService(LibraryImplementation library, String serviceName, String serviceImplName) {
            List roots = library.getContent(J2eeLibraryTypeProvider.VOLUME_TYPE_CLASSPATH);
            for (Iterator it = roots.iterator(); it.hasNext();) {
                URL rootUrl = (URL) it.next();
                FileObject root = URLMapper.findFileObject(rootUrl);
                if (root != null && "jar".equals(rootUrl.getProtocol())) {  //NOI18N
                    FileObject archiveRoot = FileUtil.getArchiveRoot(FileUtil.getArchiveFile(root));
                    String serviceRelativePath = "META-INF/services/" + serviceName; //NOI18N
                    FileObject serviceFO = archiveRoot.getFileObject(serviceRelativePath);
                    if (serviceFO != null && containsService(serviceFO, serviceName, serviceImplName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean containsService(FileObject serviceFO, String serviceName, String serviceImplName) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(serviceFO.getInputStream()));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int ci = line.indexOf('#');
                        if (ci >= 0) {
                            line = line.substring(0, ci);
                        }
                        if (line.trim().equals(serviceImplName)) {
                            return true;
                        }
                    }
                } finally {
                    br.close();
                }
            } catch (Exception ex) {
                Exceptions.attachLocalizedMessage(ex, serviceFO.toURL().toString());
                Logger.getLogger("global").log(Level.INFO, null, ex);
            }
            return false;
        }

        @Override
        public File[] getToolClasspathEntries(String toolName) {
            if (J2eePlatform.TOOL_WSIMPORT.equals(toolName)) {
                return getJaxWsLibraries();
            }
            if (J2eePlatform.TOOL_WSGEN.equals(toolName)) {
                return getJaxWsLibraries();
            }
            if (J2eePlatform.TOOL_WSCOMPILE.equals(toolName)) {
                File root = InstalledFileLocator.getDefault().locate("modules/ext/jaxrpc16", null, false); // NOI18N
                return new File[]{
                    new File(root, "saaj-api.jar"), // NOI18N
                    new File(root, "saaj-impl.jar"), // NOI18N
                    new File(root, "jaxrpc-api.jar"), // NOI18N
                    new File(root, "jaxrpc-impl.jar"), // NOI18N
                };
            }
            if (J2eePlatform.TOOL_APP_CLIENT_RUNTIME.equals(toolName)) {
                return new File(properties.getRootDir(), "client").listFiles(new FF()); // NOI18N
            }
            return null;
        }

        private File[] getJaxWsLibraries() {
            File root = new File(properties.getRootDir(), "client"); // NOI18N
            File jaxWsAPILib = new File(root, "jboss-jaxws.jar"); // NOI18N
            // JBoss without jbossws
            if (jaxWsAPILib.exists()) {
                return new File[]{
                    new File(root, "wstx.jar"), // NOI18N
                    new File(root, "jaxws-tools.jar"), // NOI18N
                    new File(root, "jboss-common-client.jar"), // NOI18N
                    new File(root, "jboss-logging-spi.jar"), // NOI18N
                    new File(root, "stax-api.jar"), // NOI18N

                    new File(root, "jbossws-client.jar"), // NOI18N
                    new File(root, "jboss-jaxws-ext.jar"), // NOI18N
                    new File(root, "jboss-jaxws.jar"), // NOI18N
                    new File(root, "jboss-saaj.jar") // NOI18N
                };
            }
            jaxWsAPILib = new File(root, "jbossws-native-jaxws.jar"); // NOI18N
            // JBoss+jbossws-native
            if (jaxWsAPILib.exists()) {
                return new File[]{
                    new File(root, "wstx.jar"), // NOI18N
                    new File(root, "jaxws-tools.jar"), // NOI18N
                    new File(root, "jboss-common-client.jar"), // NOI18N
                    new File(root, "jboss-logging-spi.jar"), // NOI18N
                    new File(root, "stax-api.jar"), // NOI18N

                    new File(root, "jbossws-native-client.jar"), // NOI18N
                    new File(root, "jbossws-native-jaxws-ext.jar"), // NOI18N
                    new File(root, "jbossws-native-jaxws.jar"), // NOI18N
                    new File(root, "jbossws-native-saaj.jar") // NOI18N
                };
            }
            jaxWsAPILib = new File(root, "jaxws-api.jar"); // NOI18N
            // JBoss+jbossws-metro
            if (jaxWsAPILib.exists()) {
                return new File[]{
                    new File(root, "wstx.jar"), // NOI18N
                    new File(root, "jaxws-tools.jar"), // NOI18N
                    new File(root, "jboss-common-client.jar"), // NOI18N
                    new File(root, "jboss-logging-spi.jar"), // NOI18N
                    new File(root, "stax-api.jar"), // NOI18N

                    new File(root, "jbossws-metro-client.jar"), // NOI18N
                    new File(root, "saaj-api.jar") // NOI18N
                };
            }
            return null;
        }

        @Override
        public String getToolProperty(String toolName, String propertyName) {
            if (J2eePlatform.TOOL_APP_CLIENT_RUNTIME.equals(toolName)) {
                if (J2eePlatform.TOOL_PROP_MAIN_CLASS.equals(propertyName)) {
                    return ""; // NOI18N
                }
                if (J2eePlatform.TOOL_PROP_MAIN_CLASS_ARGS.equals(propertyName)) {
                    return ""; // NOI18N
                }
                if ("j2ee.clientName".equals(propertyName)) { // NOI18N
                    return "${jar.name}"; // NOI18N
                }
                if (J2eePlatform.TOOL_PROP_JVM_OPTS.equals(propertyName)) {
                    return " -Djava.naming.factory.url.pkgs=org.jboss.ejb.client.naming"; // NOI18N
                }
            }
            return null;
        }

        // private helper methods -------------------------------------------------
        private void initLibraries() {
            // create library
            LibraryImplementation lib = new J2eeLibraryTypeProvider().createLibrary();
            lib.setName(NbBundle.getMessage(WildflyJ2eePlatformFactory.class, "TITLE_JBOSS_LIBRARY"));
            lib.setContent(J2eeLibraryTypeProvider.VOLUME_TYPE_CLASSPATH, properties.getClasses());
            lib.setContent(J2eeLibraryTypeProvider.VOLUME_TYPE_JAVADOC, properties.getJavadocs());
            lib.setContent(J2eeLibraryTypeProvider.VOLUME_TYPE_SRC, properties.getSources());
            libraries = new LibraryImplementation[]{lib};
        }

        @Override
        public Lookup getLookup() {
            WSStack<JaxWs> wsStack = WSStackFactory.createWSStack(JaxWs.class,
                    new JBossJaxWsStack(properties.getRootDir()), WSStack.Source.SERVER);
            Lookup baseLookup = Lookups.fixed(properties.getRootDir(),
                    wsStack, new JpaSupportImpl(this), new JaxRsStackSupportImpl(this));
            return LookupProviderSupport.createCompositeLookup(baseLookup,
                    "J2EE/DeploymentPlugins/Wildlfy/Lookup"); //NOI18N
        }

        private class JaxRsStackSupportImpl implements JaxRsStackSupportImplementation {

            private static final String JAX_RS_APPLICATION_CLASS = "javax.ws.rs.core.Application"; //NOI18N
            private J2eePlatformImplImpl j2eePlatform;

            JaxRsStackSupportImpl(J2eePlatformImplImpl j2eePlatform) {
                this.j2eePlatform = j2eePlatform;
            }

            @Override
            public boolean addJsr311Api(Project project) {
                return isBundled(JAX_RS_APPLICATION_CLASS);
            }

            @Override
            public boolean extendsJerseyProjectClasspath(Project project) {
                return isBundled(JAX_RS_APPLICATION_CLASS);
            }

            @Override
            public void removeJaxRsLibraries(Project project) {
            }

            @Override
            public void configureCustomJersey(Project project) {
            }

            @Override
            public boolean isBundled(String classFqn) {
                j2eePlatform.getLibraries();
                for (LibraryImplementation lib : j2eePlatform.getLibraries()) {
                    List<URL> urls = lib.getContent(J2eeLibraryTypeProvider.VOLUME_TYPE_CLASSPATH);
                    for (URL url : urls) {
                        FileObject root = URLMapper.findFileObject(url);
                        if (FileUtil.isArchiveFile(root)) {
                            root = FileUtil.getArchiveRoot(root);
                        }
                        String path = classFqn.replace('.', '/') + ".class"; //NOI18N
                        if (root.getFileObject(path) != null) {
                            return true;
                        }
                    }
                }

                return false;
            }

        }

    }
}
