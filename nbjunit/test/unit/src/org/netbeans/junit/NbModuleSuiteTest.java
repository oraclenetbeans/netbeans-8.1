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

package org.netbeans.junit;


import java.net.URISyntaxException;
import test.pkg.not.in.junit.NbModuleSuiteIns;
import test.pkg.not.in.junit.NbModuleSuiteT;
import test.pkg.not.in.junit.NbModuleSuiteS;
import java.io.File;
import org.netbeans.testjunit.AskForOrgOpenideUtilEnumClass;
import java.util.Properties;
import java.util.Set;
import junit.framework.Test;
import test.pkg.not.in.junit.NbModuleSuiteClusters;
import test.pkg.not.in.junit.NbModuleSuiteTUserDir;

/**
 *
 * @author Jaroslav Tulach <jaroslav.tulach@netbeans.org>
 */
public class NbModuleSuiteTest extends NbTestCase {

    public NbModuleSuiteTest(String testName) {
        super(testName);
    }

    public void testUserDir() {
        Test instance = NbModuleSuite.createConfiguration(NbModuleSuiteTUserDir.class).gui(false).suite();
        junit.textui.TestRunner.run(instance);

        assertEquals("Doesn't exist", System.getProperty("t.userdir"));

        instance = NbModuleSuite.createConfiguration(NbModuleSuiteTUserDir.class).gui(false).reuseUserDir(true).suite();
        junit.textui.TestRunner.run(instance);

        assertEquals("Exists", System.getProperty("t.userdir"));

        instance = NbModuleSuite.createConfiguration(NbModuleSuiteTUserDir.class).gui(false).reuseUserDir(false).suite();
        junit.textui.TestRunner.run(instance);

        assertEquals("Doesn't exist", System.getProperty("t.userdir"));
        assertProperty("netbeans.full.hack", "true");
    }
    
    public void testPreparePatches() throws URISyntaxException {
        Properties p = new Properties();

        String prop = File.separator + "x" + File.separator + "c:org-openide-util.jar" + File.pathSeparator +
            File.separator + "x" + File.separator + "org-openide-nodes.jar" + File.pathSeparator +
            File.separator + "x" + File.separator + "org-openide-util" + File.separator  + "tests.jar" + File.pathSeparator +
            File.separator + "x" + File.separator + "org-openide-filesystems.jar";
        Class<?>[] classes = {
            this.getClass(),
            this.getClass()
        };
        NbModuleSuite.S.preparePatches(prop, p, classes);
        assertNull(
            p.getProperty("netbeans.patches.org.openide.util")
        );
        assertEquals(
                File.separator + "x" + File.separator + "org-openide-util" + File.separator + "tests.jar"
                + File.pathSeparator + new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(),
                p.getProperty("netbeans.systemclassloader.patches"));
    }

    public void testAccessToInsaneAndFS() {
        System.setProperty("ins.one", "no");
        System.setProperty("ins.fs", "no");

        Test instance = NbModuleSuite.createConfiguration(NbModuleSuiteIns.class).gui(false).enableModules(".*").suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("ins.one", "OK");
        assertProperty("ins.fs", "OK");
    }

    public void testAccessToInsaneAndFSWithAllModules() {
        System.setProperty("ins.one", "no");
        System.setProperty("ins.fs", "no");

        Test instance = NbModuleSuite.createConfiguration(NbModuleSuiteIns.class).
                gui(false).clusters(".*").enableModules(".*").suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("ins.one", "OK");
        assertProperty("ins.fs", "OK");
    }

    public void testAccessToInsaneAndFSWithAllModulesEnumerated() {
        System.setProperty("ins.one", "no");
        System.setProperty("ins.fs", "no");

        Test instance = NbModuleSuite.createConfiguration(NbModuleSuiteIns.class).
                gui(false).clusters(".*").enableModules(".*").addTest("testFS").suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("ins.one", "no");
        assertProperty("ins.fs", "OK");
    }

    public void testOneCanEnumerateMethodsFromTheSuite() {
        System.setProperty("ins.one", "No");
        System.setProperty("ins.two", "No");
        System.setProperty("ins.three", "No");




        Test instance =
            NbModuleSuite.createConfiguration(NbModuleSuiteIns.class).addTest("testOne").
            addTest("testThree").gui(false)
            .suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("ins.one", "OK");
        assertProperty("ins.two", "No");
        assertProperty("ins.three", "OK");
    }

    public void testOneCanEnumerateMethodsFromTheSuiteWithANewMethod() {
        System.setProperty("ins.one", "No");
        System.setProperty("ins.two", "No");
        System.setProperty("ins.three", "No");




        Test instance = NbModuleSuite.createConfiguration(NbModuleSuiteIns.class).gui(false).addTest("testOne", "testThree").suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("ins.one", "OK");
        assertProperty("ins.two", "No");
        assertProperty("ins.three", "OK");
    }

    /* Cannot meaningfully rewrite while passing gui(false):
    public void testEmptyArrayMeansAll() {
        System.setProperty("ins.one", "No");
        System.setProperty("ins.two", "No");
        System.setProperty("ins.three", "No");

        Test instance = NbModuleSuite.create(NbModuleSuiteIns.class, null, null, new String[0]);
        junit.textui.TestRunner.run(instance);

        assertProperty("ins.one", "OK");
        assertProperty("ins.two", "OK");
        assertProperty("ins.three", "OK");
    }
     */

    static void assertProperty(String name, String value) {
        String v = System.getProperty(name);
        assertEquals("Property " + name, value, v);
    }

    public void testTwoClassesAtOnce() throws Exception {
        System.setProperty("ins.one", "No");
        System.setProperty("ins.two", "No");
        System.setProperty("ins.three", "No");
        System.setProperty("en.one", "No");

        NbModuleSuite.Configuration config = NbModuleSuite.Configuration.create(
            AskForOrgOpenideUtilEnumClass.class
        ).enableModules("org.openide.util.enumerations").gui(false)
        .addTest(NbModuleSuiteIns.class, "testSecond");
        Test instance = config.suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("en.one", "OK");
        assertProperty("ins.one", "No");
        assertProperty("ins.two", "OK");
        assertProperty("ins.three", "No");
    }
    public void testCumulativeUseOfModules() throws Exception {
        System.setProperty("ins.one", "No");
        System.setProperty("ins.two", "No");
        System.setProperty("ins.three", "No");
        System.setProperty("ins.java", "No");
        System.setProperty("en.one", "No");

        NbModuleSuite.Configuration config = NbModuleSuite.Configuration.create(
            AskForOrgOpenideUtilEnumClass.class
        )
        .enableModules("ide", "org.netbeans.modules.java.platform.*")
        .enableModules("platform", "org.openide.util.enumerations")
        .enableModules("ide", "org.openide.loaders.*")
        .gui(false)
        .addTest(NbModuleSuiteIns.class);
        Test instance = config.suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("en.one", "OK");
        assertProperty("ins.java", "No"); // no Windows as it is not in ide cluster
        assertProperty("ins.two", "OK");
    }

    public void testAccessExtraDefinedAutoload() {
        System.setProperty("en.one", "No");

        NbModuleSuite.Configuration config = NbModuleSuite.Configuration.create(AskForOrgOpenideUtilEnumClass.class);
        NbModuleSuite.Configuration addEnum = config.enableModules("org.openide.util.enumerations");
        Test instance = addEnum.gui(false).suite();
        junit.textui.TestRunner.run(instance);

        assertEquals("OK", System.getProperty("en.one"));
    }

    public void testAutoloadNotUsedIfAutoloadsAreSupposedToBeIgnored() {
        System.setProperty("en.one", "No");

        NbModuleSuite.Configuration config = NbModuleSuite.Configuration.create(AskForOrgOpenideUtilEnumClass.class);
        NbModuleSuite.Configuration addEnum = config.enableModules("org.openide.util.enumerations");
        Test instance = addEnum.gui(false).honorAutoloadEager(true).suite();
        junit.textui.TestRunner.run(instance);

        assertEquals("No", System.getProperty("en.one"));
    }

    public void testClustersCanBeCumulated() {
        System.setProperty("clusters", "No");

        Test instance =
            NbModuleSuite.emptyConfiguration().
            gui(false).
            clusters("ide").
            clusters("extide").
            addTest(NbModuleSuiteClusters.class)
        .suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("clusters", "ide:extide");
    }

    public void testClustersCanBeCumulatedInReverseOrder() {
        System.setProperty("clusters", "No");

        Test instance =
            NbModuleSuite.emptyConfiguration().
            gui(false).
            clusters("extide").
            clusters("ide").
            addTest(NbModuleSuiteClusters.class)
        .suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("clusters", "extide:ide");
    }

    /*
    public void testAccessClassPathDefinedAutoload() {

        NbModuleSuite.Configuration config = NbModuleSuite.Configuration.create(En.class);
        String manifest =
"Manifest-Version: 1.0\n" +
"OpenIDE-Module-Module-Dependencies: org.openide.util.enumerations>1.5\n" +
"OpenIDE-Module: org.netbeans.modules.test.nbjunit\n" +
"OpenIDE-Module-Specification-Version: 1.0\n";

        ClassLoader loader = new ManifestClassLoader(config.parentClassLoader, manifest);
        NbModuleSuite.Configuration load = config.classLoader(loader);
        Test instance = NbModuleSuite.create(load);
        junit.textui.TestRunner.run(instance);

        assertEquals("OK", System.getProperty("en.one"));
    }
     */

    public void testModulesForCL() throws Exception {
        Set<String> s = NbModuleSuite.S.findEnabledModules(ClassLoader.getSystemClassLoader());
        s.remove("org.netbeans.modules.nbjunit");
        assertEquals("Four modules left: " + s, 5, s.size());

        assertTrue("Util: " + s, s.contains("org.openide.util.ui"));
        assertTrue("Util: " + s, s.contains("org.openide.util"));
        assertTrue("Lookup: " + s, s.contains("org.openide.util.lookup"));
        assertTrue("junit: " + s, s.contains("org.netbeans.libs.junit4"));
        assertTrue("insane: " + s, s.contains("org.netbeans.insane"));
    }

    public void testModulesForMe() throws Exception {
        Set<String> s = NbModuleSuite.S.findEnabledModules(getClass().getClassLoader());
        s.remove("org.netbeans.modules.nbjunit");
        assertEquals("Four modules left: " + s, 5, s.size());

        assertTrue("Util: " + s, s.contains("org.openide.util.ui"));
        assertTrue("Util: " + s, s.contains("org.openide.util"));
        assertTrue("Lookup: " + s, s.contains("org.openide.util.lookup"));
        assertTrue("JUnit: " + s, s.contains("org.netbeans.libs.junit4"));
        assertTrue("insane: " + s, s.contains("org.netbeans.insane"));
    }

    public void testAddSuite() throws Exception{
        System.setProperty("t.one", "No");
        NbModuleSuite.Configuration conf = NbModuleSuite.emptyConfiguration();
        conf = conf.addTest(TS.class).gui(false);
        junit.textui.TestRunner.run(conf.suite());
        assertProperty("t.one", "OK");
    }

    public static class TS extends NbTestSuite{

        public TS() {
            super(NbModuleSuiteT.class);
        }
    }

    public void testRunSuiteNoSimpleTests() throws Exception{
        System.setProperty("s.one", "No");
        System.setProperty("s.two", "No");
        System.setProperty("nosuit", "OK");
        NbModuleSuite.Configuration conf = NbModuleSuite.emptyConfiguration().gui(false);
        junit.textui.TestRunner.run(conf.addTest(NbModuleSuiteS.class).suite());
        assertProperty("s.one", "OK");
        assertProperty("s.two", "OK");
        assertProperty("nosuit", "OK");
    }

    public void testRunEmptyConfiguration() throws Exception{
        junit.textui.TestRunner.run(NbModuleSuite.emptyConfiguration().gui(false).suite());
    }

    public void testAddTestCase()throws Exception{
        System.setProperty("t.one", "No");
        Test instance =
            NbModuleSuite.emptyConfiguration().addTest(NbModuleSuiteT.class).gui(false)
                .suite();
        junit.textui.TestRunner.run(instance);

        assertProperty("t.one", "OK");
    }
    
    public void testAddStartupArgument()throws Exception{
        System.setProperty("t.arg", "No");

        Test instance =
            NbModuleSuite.createConfiguration(NbModuleSuiteT.class)
                .gui(false)
                .addStartupArgument("--branding", "sample")
                .suite();

        junit.textui.TestRunner.run(instance);

        assertProperty("t.arg", "OK");
    }
}
