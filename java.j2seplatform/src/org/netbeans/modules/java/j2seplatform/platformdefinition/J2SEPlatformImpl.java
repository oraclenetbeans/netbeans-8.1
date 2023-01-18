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

package org.netbeans.modules.java.j2seplatform.platformdefinition;

import java.lang.ref.Reference;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.modules.java.j2seplatform.spi.J2SEPlatformDefaultJavadoc;
import org.netbeans.modules.java.j2seplatform.spi.J2SEPlatformDefaultSources;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;

/**
 * Implementation of the JavaPlatform API class, which serves proper
 * bootstrap classpath information.
 */
public class J2SEPlatformImpl extends JavaPlatform {
    
    public static final String PROP_ANT_NAME = "antName";                   //NOI18N
    public static final String PLATFORM_J2SE = "j2se";                      //NOI18N

    protected static final String PLAT_PROP_ANT_NAME="platform.ant.name";             //NOI18N
    protected static final String PLAT_PROP_ARCH_FOLDER="platform.arch.folder";       //NOI18N
    protected static final String SYSPROP_BOOT_CLASSPATH = "sun.boot.class.path";     // NOI18N
    protected static final String SYSPROP_JAVA_CLASS_PATH = "java.class.path";        // NOI18N
    protected static final String SYSPROP_JAVA_EXT_PATH = "java.ext.dirs";            //NOI18N
    protected static final String SYSPROP_USER_DIR = "user.dir";                      //NOI18N

    private static final String PROP_NO_DEFAULT_JAVADOC = "no.default.javadoc";       //NOI18N
    private static final String DEFAULT_JAVADOC_PROVIDER_PATH =
            "org-netbeans-api-java/platform/j2seplatform/defaultJavadocProviders/";  //NOI18N
    private static final String DEFAULT_SOURCES_PROVIDER_PATH =
            "org-netbeans-api-java/platform/j2seplatform/defaultSourcesProviders/";  //NOI18N
    private static final Logger LOG = Logger.getLogger(J2SEPlatformImpl.class.getName());

    /**
     * Holds {@link J2SEPlatformDefaultSources} implementations
     */
    private static final AtomicReference<Lookup.Result<J2SEPlatformDefaultSources>> sourcesRes = new AtomicReference<>();

    /**
     * Holds {@link J2SEPlatformDefaultJavadoc} implementations
     */
    private static final AtomicReference<Lookup.Result<J2SEPlatformDefaultJavadoc>> jdocRes = new AtomicReference<>();

    /**
     * Holds the display name of the platform
     */
    private String displayName;
    /**
     * Holds the properties of the platform
     */
    private Map<String,String> properties;

    /**
     * List&lt;URL&gt;
     */
    private ClassPath sources;

    /**
     * List&lt;URL&gt;
     */
    private List<URL> javadoc;

    /**
     * List&lt;URL&gt;
     */
    private List<URL> installFolders;

    /**
     * Holds bootstrap libraries for the platform
     */
    //@GuardedBy("this")
    private Reference<ClassPath> bootstrap;
    /**
     * Holds standard libraries of the platform
     */
    //@GuardedBy("this")
    private Reference<ClassPath> standardLibs;

    /**
     * Holds the specification of the platform
     */
    private Specification spec;

    /**
     * Holds a listener on global {@link J2SEPlatformDefaultSources} instances.
     */
    //@GuardedBy("this")
    private LookupListener[] sourcesListener;

    /**
     * Holds a listener on global {@link J2SEPlatformDefaultJavadoc} instances.
     */
    //@GuardedBy("this")
    private LookupListener[] jdocListener;

    J2SEPlatformImpl (String dispName, List<URL> installFolders, Map<String,String> initialProperties, Map<String,String> sysProperties, List<URL> sources, List<URL> javadoc) {
        super();
        this.displayName = dispName;
        if (installFolders != null) {
            this.installFolders = installFolders;       //No copy needed, called from this module => safe
        }
        else {
            //Old version, repair
            String home = initialProperties.remove ("platform.home");        //NOI18N
            if (home != null) {
                this.installFolders = new ArrayList<URL> ();
                StringTokenizer tk = new StringTokenizer (home, File.pathSeparator);
                while (tk.hasMoreTokens()) {
                    File f = new File (tk.nextToken());
                    try {
                        this.installFolders.add (Utilities.toURI(f).toURL());
                    } catch (MalformedURLException mue) {
                        LOG.log(Level.INFO, null, mue);
                    }
                }
            }
            else {
                throw new IllegalArgumentException ("Invalid platform, platform must have install folder.");    //NOI18N
            }
        }
        this.properties = initialProperties;
        if (sources != null) {
            this.sources = createClassPath(sources);
        }
        if (javadoc != null) {
            this.javadoc = Collections.unmodifiableList(javadoc);   //No copy needed, called from this module => safe
        }
        setSystemProperties(filterProbe(sysProperties));
    }

    protected J2SEPlatformImpl (String dispName, String antName, List<URL> installFolders, Map<String,String> initialProperties,
        Map<String,String> sysProperties, List<URL> sources, List<URL> javadoc) {
        this (dispName,  installFolders, initialProperties, sysProperties,sources, javadoc);
        this.properties.put (PLAT_PROP_ANT_NAME,antName);
    }

    /**
     * @return  a descriptive, human-readable name of the platform
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Alters the human-readable name of the platform
     * @param name the new display name
     */
    public void setDisplayName(String name) {
        this.displayName = name;
        firePropertyChange(PROP_DISPLAY_NAME, null, null); // NOI18N
    }
    
    /**
     * Alters the human-readable name of the platform without firing
     * events. This method is an internal contract to allow lazy creation
     * of display name
     * @param name the new display name
     */
    final protected void internalSetDisplayName (String name) {
        this.displayName = name;
    }


    public String getAntName () {
        return this.properties.get(PLAT_PROP_ANT_NAME);
    }

    public void setAntName (String antName) {
        if (antName == null || antName.length()==0) {
            throw new IllegalArgumentException ();
        }
        this.properties.put(PLAT_PROP_ANT_NAME, antName);
        this.firePropertyChange (PROP_ANT_NAME,null,null);
    }
    
    public void setArchFolder (final String folder) {
        if (folder == null || folder.length() == 0) {
            throw new IllegalArgumentException ();
        }
        this.properties.put (PLAT_PROP_ARCH_FOLDER, folder);
    }


    @Override
    public ClassPath getBootstrapLibraries() {
        synchronized (this) {
            ClassPath cp = (bootstrap == null ? null : bootstrap.get());
            if (cp != null)
                return cp;
            String pathSpec = getSystemProperties().get(SYSPROP_BOOT_CLASSPATH);
            if (pathSpec == null) {
                LOG.log(Level.WARNING, "No " + SYSPROP_BOOT_CLASSPATH + " property in platform {0}, broken platform?", getDisplayName());
                pathSpec = "";  //NOI18N
            }
            String extPathSpec = Util.getExtensions(getSystemProperties().get(SYSPROP_JAVA_EXT_PATH));
            if (extPathSpec != null) {
                pathSpec = pathSpec + File.pathSeparator + extPathSpec;
            }
            cp = Util.createClassPath (pathSpec);
            bootstrap = new SoftReference<ClassPath>(cp);
            return cp;
        }
    }

    /**
     * This implementation simply reads and parses `java.class.path' property and creates a ClassPath
     * out of it.
     * @return  ClassPath that represents contents of system property java.class.path.
     */
    @Override
    public ClassPath getStandardLibraries() {
        synchronized (this) {
            ClassPath cp = (standardLibs == null ? null : standardLibs.get());
            if (cp != null)
                return cp;
            final String pathSpec = getSystemProperties().get(SYSPROP_JAVA_CLASS_PATH);
            if (pathSpec == null) {
                cp = ClassPathSupport.createClassPath(new URL[0]);
            }
            else {
                cp = Util.createClassPath (pathSpec);
            }
            standardLibs = new SoftReference<ClassPath>(cp);
            return cp;
        }
    }

    /**
     * Retrieves a collection of {@link org.openide.filesystems.FileObject}s of one or more folders
     * where the Platform is installed. Typically it returns one folder, but
     * in some cases there can be more of them.
     */
    @Override
    public final Collection<FileObject> getInstallFolders() {
        return Util.toFileObjects(installFolders);
    }


    @Override
    public final FileObject findTool(final String toolName) {
        String archFolder = getProperties().get(PLAT_PROP_ARCH_FOLDER);        
        FileObject tool = null;
        if (archFolder != null) {
            tool = Util.findTool (toolName, this.getInstallFolders(), archFolder);            
        }
        if (tool == null) {
            tool = Util.findTool (toolName, this.getInstallFolders());
        }
        return tool;
    }


    /**
     * Returns the location of the source of platform
     * @return List&lt;URL&gt;
     */
    @Override
    public final ClassPath getSourceFolders () {
        if (sources == null) {
            sources = createClassPath(defaultSources(true));
        }
        return this.sources;
    }

    public final void setSourceFolders (ClassPath c) {
        assert c != null;
        this.sources = c;
        LookupListener listener;
        synchronized(this) {
            listener = sourcesListener == null ? null : sourcesListener[1];
            sourcesListener = null;
        }
        if (listener != null) {
            getJ2SEPlatformDefaultSources().removeLookupListener(listener);
        }
        this.firePropertyChange(PROP_SOURCE_FOLDER, null, null);
    }

        /**
     * Returns the location of the Javadoc for this platform
     * @return FileObject
     */
    @Override
    public final List<URL> getJavadocFolders () {
        if (javadoc == null) {
            javadoc = shouldAddDefaultJavadoc() ? defaultJavadoc(true) : Collections.<URL>emptyList();
        }
        return this.javadoc;
    }

    public final void setJavadocFolders (List<URL> c) {
        assert c != null;
        final List<URL> safeCopy = Collections.unmodifiableList (new ArrayList<> (c));
        for (Iterator<URL> it = safeCopy.iterator(); it.hasNext();) {
            URL url = it.next ();
            if (!Util.isRemote(url) && !"jar".equals (url.getProtocol()) && FileUtil.isArchiveFile(url)) {
                throw new IllegalArgumentException ("JavadocFolder must be a folder: " + url);  //NOI18N
            }
        }
        if (c.isEmpty()) {
            if (toURIList(this.javadoc).equals(toURIList(defaultJavadoc()))) {
                //Set the PROP_NO_DEFAULT_JAVADOC
                this.properties.put(PROP_NO_DEFAULT_JAVADOC, Boolean.TRUE.toString());
            }
        } else {
            //Reset the PROP_NO_DEFAULT_JAVADOC to allow auto javadoc again
            this.properties.remove(PROP_NO_DEFAULT_JAVADOC);
        }
        this.javadoc = safeCopy;
        LookupListener listener;
        synchronized (this) {
            listener = jdocListener == null ? null : jdocListener[1];
            jdocListener = null;
        }
        if (listener != null) {
            getJ2SEPlatformDefaultJavadoc().removeLookupListener(listener);
        }
        this.firePropertyChange(PROP_JAVADOC_FOLDER, null, null);
    }

    @Override
    public String getVendor() {
        String s = getSystemProperties().get("java.vm.vendor"); // NOI18N
        return s == null ? "" : s; // NOI18N
    }

    @Override
    public Specification getSpecification() {
        if (spec == null) {
            spec = new Specification (
                PLATFORM_J2SE,
                Util.getSpecificationVersion(this),
                NbBundle.getMessage(J2SEPlatformImpl.class, "TXT_J2SEDisplayName"),
                null);
        }
        return spec;
    }

    @Override
    public Map<String,String> getProperties() {
        return Collections.unmodifiableMap (this.properties);
    }
    
    Collection getInstallFolderURLs () {
        return Collections.unmodifiableList(this.installFolders);
    }
    
    protected static String filterProbe (String v, final String probePath) {
        if (v != null) {
            final String[] pes = PropertyUtils.tokenizePath(v);
            final StringBuilder sb = new StringBuilder ();
            for (String pe : pes) {
                if (probePath != null ?  probePath.equals(pe) : (pe != null &&
                pe.endsWith("org-netbeans-modules-java-j2seplatform-probe.jar"))) { //NOI18N
                    //Skeep
                }
                else {
                    if (sb.length() > 0) {
                        sb.append(File.pathSeparatorChar);
                    }
                    sb.append(pe);
                }
            }
            v = sb.toString();
        }
        return v;
    }
    
    private static Map<String,String> filterProbe (final Map<String,String> p) {
        if (p!=null) {
            final String val = p.get(SYSPROP_JAVA_CLASS_PATH);
            if (val != null) {
                p.put(SYSPROP_JAVA_CLASS_PATH, filterProbe(val, null));
            }
        }
        return p;
    }


    private static ClassPath createClassPath (final List<? extends URL> urls) {
        List<PathResourceImplementation> resources = new ArrayList<PathResourceImplementation> ();
        if (urls != null) {
            for (URL url : urls) {
                resources.add (ClassPathSupport.createResource (url));
            }
        }
        return ClassPathSupport.createClassPath (resources);
    }
    
    /**
     * Tests if the default javadoc was already added and removed.
     * If so do not add it again.
     * @return 
     */
    private boolean shouldAddDefaultJavadoc() {
        return !Boolean.parseBoolean(getProperties().get(PROP_NO_DEFAULT_JAVADOC));
    }
    
    @NonNull
    private static List<? extends URI> toURIList(
            @NonNull final List<? extends URL> original) {
        final List<URI> result = new ArrayList<URI>(original.size());
        for (URL url : original) {
            try {
                result.add(url.toURI());
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return result;
    }
    
    /**
     * Try to find the standard Javadoc for a platform.
     * The {@code docs/} folder is used if it exists, else network Javadoc is looked up.
     * @param platform a JDK
     * @return a (possibly empty) list of URLs
     */
    public List<URL> defaultJavadoc() {
        return defaultJavadoc(false);
    }

    private List<URL> defaultJavadoc(final boolean listen) {
        final JavaPlatform safePlatform = new ForwardingJavaPlatform(this) {
            @Override
            public List<URL> getJavadocFolders() {
                return Collections.<URL>emptyList();
            }
        };
        final Set<URI> roots = new LinkedHashSet<>();
        final Lookup.Result<? extends J2SEPlatformDefaultJavadoc> res = getJ2SEPlatformDefaultJavadoc();
        if (listen) {
            synchronized (this) {
                if (jdocListener == null) {
                    jdocListener = new LookupListener[2];
                    jdocListener[0] = new LookupListener() {
                        @Override
                        public void resultChanged(LookupEvent ev) {
                            javadoc = null;
                        }
                    };
                    jdocListener[1] = WeakListeners.create(LookupListener.class, jdocListener[0], res);
                    res.addLookupListener(jdocListener[1]);
                }
            }
        }
        for (J2SEPlatformDefaultJavadoc jdoc : res.allInstances()) {
            roots.addAll(jdoc.getDefaultJavadoc(safePlatform));
        }
        final List<URL> result = new ArrayList<>(roots.size());
        for (URI root : roots) {
            try {
                result.add(root.toURL());
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<URL> defaultSources() {
        return defaultSources(false);
    }

    @NonNull
    private List<URL> defaultSources(final boolean listen) {
        final JavaPlatform safePlatform = new ForwardingJavaPlatform(this) {
            @Override
            public List<URL> getJavadocFolders() {
                return Collections.<URL>emptyList();
            }
        };
        final Set<URI> roots = new LinkedHashSet<>();
        final Lookup.Result<? extends J2SEPlatformDefaultSources> res = getJ2SEPlatformDefaultSources();
        if (listen) {
            synchronized (this) {
                if (sourcesListener == null) {
                    sourcesListener = new LookupListener[2];
                    sourcesListener[0] = new LookupListener() {
                        @Override
                        public void resultChanged(LookupEvent ev) {
                            sources = null;
                        }
                    };
                    sourcesListener[1] = WeakListeners.create(LookupListener.class, sourcesListener[0], res);
                    res.addLookupListener(sourcesListener[1]);
                }
            }
        }
        for (J2SEPlatformDefaultSources src : res.allInstances()) {
            roots.addAll(src.getDefaultSources(safePlatform));
        }
        final List<URL> result = new ArrayList<>(roots.size());
        for (URI root : roots) {
            try {
                result.add(root.toURL());
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return Collections.unmodifiableList(result);
    }

    boolean isBroken () {
        if (getInstallFolders().isEmpty()) {
            return true;
        }
        for (String tool : PlatformConvertor.IMPORTANT_TOOLS) {
            if (findTool(tool) == null) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static Lookup.Result<? extends J2SEPlatformDefaultSources> getJ2SEPlatformDefaultSources() {
        Lookup.Result<J2SEPlatformDefaultSources> res = sourcesRes.get();
        if (res == null) {
            res = Lookups.forPath(DEFAULT_SOURCES_PROVIDER_PATH).lookupResult(J2SEPlatformDefaultSources.class);
            if (!sourcesRes.compareAndSet(null, res)) {
                res = sourcesRes.get();
            }
        }
        return res;
    }

    @NonNull
    private static Lookup.Result<? extends J2SEPlatformDefaultJavadoc> getJ2SEPlatformDefaultJavadoc() {
        Lookup.Result<J2SEPlatformDefaultJavadoc> res = jdocRes.get();
        if (res == null) {
            res = Lookups.forPath(DEFAULT_JAVADOC_PROVIDER_PATH).lookupResult(J2SEPlatformDefaultJavadoc.class);
            if (!jdocRes.compareAndSet(null, res)) {
                res = jdocRes.get();
            }
        }
        return res;
    }
}
