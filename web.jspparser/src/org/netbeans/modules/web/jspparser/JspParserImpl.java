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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

package org.netbeans.modules.web.jspparser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.web.api.webmodule.WebModule;
import org.netbeans.modules.web.jsps.parserapi.TldChangeListener;
import org.netbeans.modules.web.jsps.parserapi.JspParserAPI;

import org.openide.filesystems.FileObject;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbBundle;

// PENDING - need to call reinitOptions when something changes (taglib, jar, web.xml)
// PENDING - separate to two classes, have a per-application instance of one of them

/**
 * @author Petr Jiricka
 */
public class JspParserImpl implements JspParserAPI {

    // @GuardedBy(this)
    final Map<WebModule, WebAppParseProxy> parseSupports = new WeakHashMap<WebModule, WebAppParseProxy>();
    private final TldChangeSupport tldChangeSupport;

    private static final Logger LOGGER = Logger.getLogger(JspParserImpl.class.getName());
    private static Method webAppParserImplFactoryMethod;

    private static final JspParserAPI.JspOpenInfo DEFAULT_OPENINFO =
            new JspParserAPI.JspOpenInfo(false, "ISO-8859-1"); // NOI18N

    /** Constructs a new Parser API implementation.
     */
    public JspParserImpl() {
        // PENDING - we are preventing the garbage collection of
        // Project-s and FileObject-s (wmRoots)
        tldChangeSupport = new TldChangeSupport(this);
    }

    private static URL[] urls;

    private static final String[] JAR_FILE_NAMES = new String[]{
        "ant/lib/ant.jar",  //NOI18N
        "modules/ext/glassfish-jspparser-4.0.jar", //NOI18N
//        "modules/ext/glassfish-logging.jar" //NOI18N
        "modules/ext/jsp-parser-ext.jar", //NOI18N
        "modules/ext/jstl-api.jar", //NOI18N
        "modules/ext/jstl-impl.jar", //NOI18N
        "modules/ext/servlet3.1-jsp2.3-api.jar", //NOI18N
        "ant/lib/ant-launcher.jar" //Glassfish V2 //NOI18N
    };

    private static void initURLs() throws MalformedURLException, IOException {
        if (urls == null) {
            File[] files = new File[JAR_FILE_NAMES.length];
            List<String> missing = new ArrayList<String>();
            for(int i = 0; i < JAR_FILE_NAMES.length; i++) {
                files[i] = InstalledFileLocator.getDefault().locate(JAR_FILE_NAMES[i], null, false);
                if(files[i] == null) {
                    missing.add(JAR_FILE_NAMES[i]);
                }
            }

            if(!missing.isEmpty()) {
                //something wasn't found, report error and cancel the initialization
                StringBuffer msg = new StringBuffer();
                msg.append("Cannot initialize JSP parser, following JAR files couldn't be localted: "); //NOI18N
                for(String fname : missing) {
                    msg.append(fname);
                    msg.append(','); //NOI18N
                }
                msg.setCharAt(msg.length() - 1, '.'); //replace last comma

                throw new IOException(msg.toString());
            }

            URL[] urls2 = new URL[files.length];
            for (int i = 0; i < files.length; i++) {
                urls2[i] = files[i].toURI().toURL();
            }
            urls = urls2;
        }
    }

    /**
     * This method is designed to be called only from unit tests to initialize
     * parser JARs.
     */
    public static void setParserJARs(URL[] urls) {
        JspParserImpl.urls = urls;
    }

    private static void initReflection() throws IOException {
        if (webAppParserImplFactoryMethod == null) {
            try {
                initURLs();
                ExtClassLoader urlCL = new ExtClassLoader(urls, JspParserImpl.class.getClassLoader());
                Class<?> cl = urlCL.loadClass("org.netbeans.modules.web.jspparser_ext.WebAppParseSupport"); // NOI18N
                webAppParserImplFactoryMethod = cl.getDeclaredMethod("create", JspParserImpl.class, WebModule.class); // NOI18N
            } catch (NoSuchMethodException e) {
                LOGGER.log(Level.INFO, null, e);
            } catch (MalformedURLException e) {
                LOGGER.log(Level.INFO, null, e);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.INFO, null, e);
            }
        }
    }

    public JspParserAPI.JspOpenInfo getJspOpenInfo(FileObject jspFile, WebModule wm, boolean useEditor) {
        //try to fast create openinfo

        //detects encoding even if there is not webmodule (null) or deployment descriptor doesn't exist
        FastOpenInfoParser fastOIP = FastOpenInfoParser.get(wm);
        if (fastOIP != null) {
            JspParserAPI.JspOpenInfo jspOI = fastOIP.getJspOpenInfo(jspFile, useEditor);
            if (jspOI != null) {
                return jspOI;
            }
        }

        //no encoding found in the file or the deployment descriptor contains encoding declarations
        if (wm != null) {
            FileObject wmRoot = wm.getDocumentBase();
            if (wmRoot != null) {
                WebAppParseProxy pp = getParseProxy(wm);
                if (pp != null) {
                    return pp.getJspOpenInfo(jspFile, useEditor);
                }
            }
        }
        return DEFAULT_OPENINFO;
    }

    public JspParserAPI.ParseResult analyzePage(FileObject jspFile, WebModule wm, int errorReportingMode) {
        if (wm == null) {
            return getNoWebModuleResult(jspFile, null);
        }
        FileObject wmRoot = wm.getDocumentBase();
        if (wmRoot == null) {
            return getNoWebModuleResult(jspFile, wm);
        }
        WebAppParseProxy pp = getParseProxy(wm);
        if (pp == null) {
            return getNoWebModuleResult(jspFile, wm);
        }
        return pp.analyzePage(jspFile, errorReportingMode);
    }

    /**
     * Returns the mapping of the 'global' tag library URI to the location (resource
     * path) of the TLD associated with that tag library.
     * @param wmRoot the web module for which to return the map
     * @return Map which maps global tag library URI to the location
     * (resource path) of its tld. The location is
     * returned as a String array:
     *    [0] The location
     *    [1] If the location is a jar file, this is the location of the tld.
     */
    public Map<String, String[]> getTaglibMap(WebModule wm) throws IOException {
        FileObject wmRoot = wm.getDocumentBase();
        if (wmRoot == null) {
            throw new IOException();
        }
        WebAppParseProxy pp = getParseProxy(wm);
        return pp.getTaglibMap(true);
    }

    private synchronized WebAppParseProxy getParseProxy(WebModule wm) {
        WebAppParseProxy pp = parseSupports.get(wm);
        if (pp == null || !pp.isValid()) {
            pp = createParseProxy(wm);
            parseSupports.put(wm, pp);
        }
        return pp;
    }

    private WebAppParseProxy createParseProxy(WebModule wm) {
        // PENDING - do caching for individual JSPs
        try {
            initReflection();
            return (WebAppParseProxy) webAppParserImplFactoryMethod.invoke(null, this, wm);
        } catch (IOException ise) {
            LOGGER.log(Level.WARNING, null, ise);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.INFO, null, e);
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.INFO, null, e);
        }
        return null;
    }

    public URLClassLoader getModuleClassLoader(WebModule wm) {
        WebAppParseProxy pp = getParseProxy(wm);
        return pp.getWAClassLoader();
    }

    private JspParserAPI.ParseResult getNoWebModuleResult(FileObject jspFile, WebModule wm) {
        JspParserAPI.ErrorDescriptor error = new JspParserAPI.ErrorDescriptor(null, jspFile, -1, -1,
                NbBundle.getMessage(JspParserImpl.class, "MSG_webModuleNotFound", jspFile.getNameExt()), ""); // NOI18N
        return new JspParserAPI.ParseResult(new JspParserAPI.ErrorDescriptor[] {error});
    }

    public void addTldChangeListener(TldChangeListener listener) {
        tldChangeSupport.addTldChangeListener(listener);
    }

    public void removeTldChangeListener(TldChangeListener listener) {
        tldChangeSupport.removeTldChangeListener(listener);
    }

    public void fireChange(WebModule webModule) {
        tldChangeSupport.fireChange(webModule);
    }

    private static class ExtClassLoader extends URLClassLoader {

        private static final AllPermission ALL_PERM = new AllPermission();

        public ExtClassLoader(URL[] classLoadingURLs, ClassLoader parent) {
            super(classLoadingURLs, parent);
        }

        @Override
        protected PermissionCollection getPermissions(CodeSource codesource) {
            PermissionCollection perms = super.getPermissions(codesource);
            perms.add(ALL_PERM);
            return perms;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // this is because debugger adds ant on cp but this classloader needs ant as well
            //  - so let this class loader find the class
            if (name.startsWith("org.apache.tools.ant.")) { // NOI18N
                Class<?> clazz = findClass(name);
                return clazz;
            } else if (name.startsWith("org.apache.jasper.")) { // NOI18N
                Class<?> clazz = findClass(name);
                return clazz;
            }
            return super.loadClass(name, resolve);
        }
    }
}
