/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Oracle
 */
package org.netbeans.modules.netbinox;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Test;
import org.netbeans.ModuleManager;
import org.netbeans.NetigsoFramework;
import org.netbeans.SetupHid;
import org.netbeans.core.netigso.Netigso;
import org.netbeans.core.netigso.NetigsoUtil;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.junit.RandomlyFails;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

/**
 * Read access test
 * see details on http://wiki.netbeans.org/FitnessViaWhiteAndBlackList
 */
public class CachingAndExternalPathsTest extends NbTestCase {
    static {
        System.setProperty("java.awt.headless", "true");
    }
    private static final Logger LOG = Logger.getLogger(CachingAndExternalPathsTest.class.getName());

    private static void initCheckReadAccess() throws IOException {
        Set<String> allowedFiles = new HashSet<String>();
        CountingSecurityManager.initialize(null, CountingSecurityManager.Mode.CHECK_READ, allowedFiles);
    }
    
    public CachingAndExternalPathsTest(String name) {
        super(name);
    }
    
    public static Test suite() throws IOException {
        return create(new SetExtDirProperty() {
            public void setExtDirProperty(File value) {
                System.setProperty("ext.dir", value.getPath());
            }
        });
    }
    static Test create(SetExtDirProperty setter) throws IOException {
        Locale.setDefault(Locale.US);
        CountingSecurityManager.initialize("none", CountingSecurityManager.Mode.CHECK_READ, null);
        System.setProperty("org.netbeans.Stamps.level", "ALL");
        System.setProperty(NbModuleSuite.class.getName() + ".level", "FINE");
        System.setProperty("org.netbeans.modules.netbinox.level", "FINE");

        NbTestSuite suite = new NbTestSuite();
        Compile compile = new Compile("testCompile", setter);
        suite.addTest(compile);
        NbModuleSuite.Configuration common = NbModuleSuite.emptyConfiguration().clusters(".*").enableClasspathModules(false)
                .gui(false).honorAutoloadEager(true);
        {
            NbModuleSuite.Configuration conf = common.reuseUserDir(false).addTest(CachingAndExternalPathsTest.class, "testInitUserDir");
            suite.addTest(NbModuleSuite.create(conf));
        }
        {
            NbModuleSuite.Configuration conf = common.reuseUserDir(true).addTest(CachingAndExternalPathsTest.class, "testStartAgain");
            suite.addTest(NbModuleSuite.create(conf));
        }
        {
            NbModuleSuite.Configuration conf = common.reuseUserDir(true).addTest(
                CachingAndExternalPathsTest.class, 
                "testStartOnceMore",
                "testTeaseTheSystemWithFileLocatorBundleFile"
            );
            suite.addTest(NbModuleSuite.create(conf));
        }

        suite.addTest(new CachingAndExternalPathsTest("testInMiddle"));

        {
            NbModuleSuite.Configuration conf = common.reuseUserDir(true).addTest(CachingAndExternalPathsTest.class, 
                "testTeaseTheSystemWithFileLocatorBundleFile", 
                "testReadAccess", "testVerifyActivatorExecuted"
            );
            suite.addTest(NbModuleSuite.create(conf));
        }

        return suite;
    }

    @RandomlyFails
    public void testInitUserDir() throws Exception {
        File simpleModule = new File(System.getProperty("external.jar"));

        File newModule = new File(new File(new File(System.getProperty("netbeans.user")), "modules"), "org-external.jar");
        newModule.getParentFile().mkdirs();
        simpleModule.renameTo(newModule);
        assertTrue("New module correctly created", newModule.exists());

        class Activate implements FileSystem.AtomicAction {
            FileObject fo;

            public Activate() throws IOException {
            }
            
            public void run() throws IOException {
                fo = FileUtil.getConfigFile("Modules").createData("org-external.xml");
                OutputStream os = fo.getOutputStream();
                os.write((
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE module PUBLIC \"-//NetBeans//DTD Module Status 1.0//EN\"\n" +
        "                        \"http://www.netbeans.org/dtds/module-status-1_0.dtd\">\n" +
        "<module name=\"org.external\">\n" +
        "    <param name=\"autoload\">false</param>\n" +
        "    <param name=\"eager\">false</param>\n" +
        "    <param name=\"enabled\">true</param>\n" +
        "    <param name=\"jar\">modules/org-external.jar</param>\n" +
        "    <param name=\"reloadable\">false</param>\n" +
        "</module>\n" +
        "").getBytes());
                os.close();
            }
        }
        Activate a = new Activate();
        System.getProperties().remove("activated.ok");

        LOG.log(Level.INFO, "Creating config file");
        FileUtil.runAtomicAction(a);
        LOG.log(Level.INFO, "Done creating {0}", a.fo);


        for (int i = 0; i < 360 && System.getProperty("activated.ok") == null; i++) {
            LOG.log(Level.INFO, "Not found, but activated.ok: {0}", System.getProperty("activated.ok"));
            Thread.sleep(500);
        }
        LOG.log(Level.INFO, "activated.ok: {0}", System.getProperty("activated.ok"));
        assertEquals("true", System.getProperty("activated.ok"));
        doNecessarySetup();
        
        LOG.info("testInitUserDir - finished");
    }

    public void testStartAgain() throws Exception {
        doNecessarySetup();
        // will be reset next time the system starts
        System.getProperties().remove("netbeans.dirs");
    }
    public void testStartOnceMore() throws Exception {
        doNecessarySetup();
        // will be reset next time the system starts
        System.getProperties().remove("netbeans.dirs");
        // initializes counting, but waits till netbeans.dirs are provided
        // by NbModuleSuite
        LOG.info("testStartAgain - enabling initCheckReadAccess");
        initCheckReadAccess();
        LOG.info("testStartAgain - finished");
    }

    static void doNecessarySetup() throws Exception {
        ClassLoader l = Lookup.getDefault().lookup(ClassLoader.class);
        try {
            Class<?> c = Class.forName("javax.help.HelpSet", true, l);
        } catch (ClassNotFoundException ok) {
        }
        try {
            Class<?> d = Class.forName("org.openide.DialogDescriptor", true, l);
        } catch (ClassNotFoundException ok) {
        }
        FileObject fo = FileUtil.getConfigFile("Services/Browsers");
        if (fo != null) {
            fo.delete();
        }
        waitWarmUpFinished();
    }
    private static void waitWarmUpFinished() throws Exception {
        ClassLoader global = Thread.currentThread().getContextClassLoader();
        Class<?> warmTask = global.loadClass("org.netbeans.core.startup.MainLookup"); // NOI18N
        Method warmUp = warmTask.getMethod("warmUp", long.class); // NOI18N
        Object wait = warmUp.invoke(null, 0L);
        Method waitUp = wait.getClass().getDeclaredMethod("waitFinished"); // NOI18N
        waitUp.invoke(wait);
    }

    public void testInMiddle() {
        LOG.info("Previous run finished, starting another one");
        System.setProperty("activated.count", "0");
    }
    
    public void testTeaseTheSystemWithFileLocatorBundleFile() throws Exception {
        Framework f = findFramework();
        Method getBundleFile;
        try {
            getBundleFile = Class.forName("org.eclipse.core.runtime.FileLocator").getMethod("getBundleFile", Bundle.class);
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Skipping the " + getName() + " test", ex);
            return;
        }
        for (Bundle b : f.getBundleContext().getBundles()) {
            File file = (File) getBundleFile.invoke(null, b);
            assertNotNull("Some file is found", file);
        }
    }

    @RandomlyFails // NB-Core-Build #9918: Unstable, NB-Core-Build #9919 on the same sources passed
    public void testReadAccess() throws Exception {
        LOG.info("Inside testReadAccess");
        ClassLoader l = Lookup.getDefault().lookup(ClassLoader.class);
        try {
            Class<?> c = Class.forName("javax.help.HelpSet", true, l);
        } catch (ClassNotFoundException ex) {
            // never mind
        }
        try {
            if (CountingSecurityManager.isEnabled()) {
                CountingSecurityManager.assertCounts("No reads during startup", 0);
            } else {
               LOG.warning("Initialization mode, counting is disabled");
            }
        } catch (Error e) {
            e.printStackTrace(getLog("file-reads-report.txt"));
            throw e;
        }
    }

    @RandomlyFails
    public void testVerifyActivatorExecuted() {
        assertEquals("1", System.getProperty("activated.count"));
    }

    public static class Compile extends NbTestCase {
        private File simpleModule;
        private SetExtDirProperty set;

        public Compile(String name, SetExtDirProperty p) {
            super(name);
            set = p;
        }

        public void testCompile() throws Exception {
            File data = new File(getDataDir(), "jars");
            File jars = new File(getWorkDir(), "jars");
            simpleModule = SetupHid.createTestJAR(data, jars, "external", null);

            File ext = new File(getWorkDir(), "extjars");
            ext.mkdirs();
            File extJAR = SetupHid.createTestJAR(data, ext, "ext", "activate");

            System.setProperty("external.jar", simpleModule.getPath());
            set.setExtDirProperty(ext);
        }
    }
    
    protected interface SetExtDirProperty {
        public void setExtDirProperty(File value);
    }
    static Framework findFramework() throws Exception {
        return NetigsoUtil.framework(Main.getModuleSystem().getManager());
    }
}
