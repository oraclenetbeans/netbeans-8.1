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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.cnd.makeproject.configurations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerFlavor;
import org.netbeans.modules.cnd.api.toolchain.PlatformTypes;
import org.netbeans.modules.cnd.makeproject.api.MakeArtifact;
import org.netbeans.modules.cnd.makeproject.api.configurations.DevelopmentHostConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.Item;
import org.netbeans.modules.cnd.makeproject.api.configurations.LibraryItem;
import org.netbeans.modules.cnd.makeproject.api.configurations.LinkerConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfigurationDescriptor;
import org.netbeans.modules.cnd.test.CndBaseTestCase;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.toolchain.execution.impl.ToolchainSPIAccessor;
import org.netbeans.modules.cnd.spi.toolchain.CompilerSetFactory;
import org.netbeans.modules.cnd.utils.FSPath;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.util.HostInfoUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

/**
 * Create a sample web project by unzipping a template into some directory
 */
public class ConfigurationMakefileWriterTest extends CndBaseTestCase {

    private static final boolean TRACE = false;

    public ConfigurationMakefileWriterTest(String name) {
        super(name);
        cleanUserDir();
    }

    public static File getBaseFolder(){
        String dataPath = System.getProperty("java.io.tmpdir");
        if (!dataPath.endsWith(File.separator)) {
            dataPath += File.separator;
        }
        dataPath += "Xxx";
        File fileDataPath = new File(dataPath);
        fileDataPath = FileUtil.normalizeFile(fileDataPath);
        if (!fileDataPath.exists()) {
            fileDataPath.mkdirs();
        }
        return fileDataPath;
    }

    private void testAppWithLibraries(String testName, String flavorName, int platform, String golden) {
        if (TRACE) {
            System.out.println("-----------------------------------------------------" + testName);
        }
        System.setProperty("org.netbeans.modules.cnd.makeproject.api.runprofiles", "true"); // NOI18N
        System.setProperty("cnd.remote.fs", "false");
        Logger logger = Logger.getLogger(NbPreferences.class.getName());
        logger.setLevel(Level.SEVERE);
        logger = Logger.getLogger("org.netbeans.modules.masterfs.filebasedfs.fileobjects.FileObjectFactory"); // NOI18N
        logger.setLevel(Level.SEVERE);
        logger = Logger.getLogger("org.netbeans.ui.metrics.cnd"); // NOI18N
        logger.setLevel(Level.SEVERE);

        // Setup project
        String libsuffix = "so";
        if (platform == PlatformTypes.PLATFORM_MACOSX) {
            libsuffix = "dylib";
        } else if (platform == PlatformTypes.PLATFORM_WINDOWS) {
            libsuffix = "dll";
        }
        File folderBase = getBaseFolder();
        final FileObject folderBaseFO = CndFileUtils.toFileObject(folderBase);
        MakeConfigurationDescriptor makeConfigurationDescriptor = new MakeConfigurationDescriptor(folderBaseFO);
        MakeConfiguration conf = MakeConfiguration.createConfiguration(FSPath.toFSPath(folderBaseFO), "Default", MakeConfiguration.TYPE_APPLICATION, null, HostInfoUtils.LOCALHOST);  // NOI18N
        makeConfigurationDescriptor.init(conf);
        makeConfigurationDescriptor.getLogicalFolders().addItem(Item.createInFileSystem(makeConfigurationDescriptor.getBaseDirFileSystem(), "test.cc"));
        LibraryItem.ProjectItem projectItem;
        projectItem = new LibraryItem.ProjectItem(new MakeArtifact(
                "../hello1lib",
                3,
                "Debug",
                true,
                true,
                "../hello1lib",
                "${MAKE} -f Makefile CONF=Debug",
                "${MAKE} -f Makefile CONF=Debug clean",
                "dist/Debug/.../libhello1lib.a",
                conf));
        conf.getLinkerConfiguration().getLibrariesConfiguration().add(projectItem);
        projectItem = new LibraryItem.ProjectItem(new MakeArtifact(
                "../hello3lib",
                2,
                "Debug",
                true,
                true,
                "../hello3lib",
                "${MAKE} -f Makefile CONF=Debug",
                "${MAKE} -f Makefile CONF=Debug clean",
                "dist/Debug/.../libhello3lib." + libsuffix,
                conf));
        conf.getLinkerConfiguration().getLibrariesConfiguration().add(projectItem);

        CompilerFlavor flavor = CompilerFlavor.toFlavor(flavorName, platform);
        CompilerSet compilerSetold = CompilerSetManager.get(ExecutionEnvironmentFactory.getLocal()).getCompilerSet("MyCompilerSet");
        if (compilerSetold != null) {
            ToolchainSPIAccessor.remove(ExecutionEnvironmentFactory.getLocal(), compilerSetold);
        }
        CompilerSet compilerSet = CompilerSetFactory.getCustomCompilerSet(folderBase.getAbsolutePath(), flavor, "MyCompilerSet", ExecutionEnvironmentFactory.getLocal());
        ToolchainSPIAccessor.add(ExecutionEnvironmentFactory.getLocal(), compilerSet);
        conf.getCompilerSet().restore("MyCompilerSet|" + flavorName, 51);
        conf.getDevelopmentHost().setBuildPlatform(platform);
        
        // Dump state
        ExecutionEnvironment env = ExecutionEnvironmentFactory.getLocal();
        System.err.println("ExecutionEnvironment " + env);
        System.err.println("host: " + env.getHost());
        System.err.println("host address: " + env.getHostAddress());
        System.err.println("user: " + env.getUser());
        System.err.println("local: " + env.isLocal());
        System.err.println("remote: " + env.isRemote());
        
        DevelopmentHostConfiguration host = conf.getDevelopmentHost();        
        System.err.println("DevelopmentHost " + host);
        System.err.println("platform: " + host.getBuildPlatformName());
        System.err.println("name: " + host.getHostDisplayName(true));
        System.err.println("key: " + host.getHostKey());

        System.err.println("DefaultDevelopmentHostID " + CppUtils.getDefaultDevelopmentHost(folderBaseFO));
        
        env = ExecutionEnvironmentFactory.fromUniqueID(CppUtils.getDefaultDevelopmentHost(folderBaseFO));
        System.err.println("DefaultExecutionEnvironment " + env);
        System.err.println("host: " + env.getHost());
        System.err.println("host address: " + env.getHostAddress());
        System.err.println("user: " + env.getUser());
        System.err.println("local: " + env.isLocal());
        System.err.println("remote: " + env.isRemote());
        
        host = new DevelopmentHostConfiguration(env);        
        System.err.println("DefaultDevelopmentHost " + host);
        System.err.println("platform: " + host.getBuildPlatformName());
        System.err.println("name: " + host.getHostDisplayName(true));
        System.err.println("key: " + host.getHostKey());
        
        System.err.println("CompilerSets"); // NOI18N        
        for (CompilerSet cs : conf.getCompilerSet().getCompilerSetManager().getCompilerSets()) {
            System.err.println(cs);
            System.err.println("flavor: " + cs.getCompilerFlavor());
            System.err.println("dir: " + cs.getDirectory());
        }
        
        // Check conf
        System.err.println("Check conf"); // NOI18N
        assertNotNull(conf);
        LinkerConfiguration lc = conf.getLinkerConfiguration();
        assertNotNull(lc);
        CompilerSet cs = lc.getMakeConfiguration().getCompilerSet().getCompilerSet();
        assertNotNull(cs);
        
        // Setup streams
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        try {
            pipedOutputStream.connect(pipedInputStream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(pipedOutputStream));
        BufferedReader rw = new BufferedReader(new InputStreamReader(pipedInputStream));

        // Generate (parts of) makefile
        try {
            ConfigurationMakefileWriter configurationMakefileWriter = new ConfigurationMakefileWriter(makeConfigurationDescriptor);
            bw.write("LDLIBSOPTIONS=" + conf.getLinkerConfiguration().getLibraryItems() + "\n"); // NOI18N
            ConfigurationMakefileWriter.writeLinkTarget(makeConfigurationDescriptor, conf, bw);
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Read and test result
        StringBuilder result = new StringBuilder();
        try {
            while (true) {
                String line = rw.readLine();
                if (line == null) {
                    rw.close();
                    break;
                }
                result.append(line).append("\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (TRACE) {
            System.out.println(result);
            System.out.println(golden);
        }
        assertEqualsText(result.toString(),golden);
    }

    private void testDynamicLibrary(String testName, String flavorName, int platform, String golden) {
        if (TRACE) {
            System.out.println("-----------------------------------------------------" + testName);
        }
        System.setProperty("org.netbeans.modules.cnd.makeproject.api.runprofiles", "true"); // NOI18N
        // Setup project
        File folderBase = getBaseFolder();
        final FileObject folderBaseFO = CndFileUtils.toFileObject(folderBase);
        MakeConfigurationDescriptor makeConfigurationDescriptor = new MakeConfigurationDescriptor(folderBaseFO);
        MakeConfiguration conf =MakeConfiguration.createConfiguration(FSPath.toFSPath(folderBaseFO), "Default", MakeConfiguration.TYPE_DYNAMIC_LIB, null, HostInfoUtils.LOCALHOST);  // NOI18N
        makeConfigurationDescriptor.init(conf);
        makeConfigurationDescriptor.getLogicalFolders().addItem(Item.createInFileSystem(makeConfigurationDescriptor.getBaseDirFileSystem(), "test.cc"));

        CompilerFlavor flavor = CompilerFlavor.toFlavor(flavorName, platform);
        CompilerSet compilerSetold = CompilerSetManager.get(ExecutionEnvironmentFactory.getLocal()).getCompilerSet("MyCompilerSet");
        if (compilerSetold != null) {
            ToolchainSPIAccessor.remove(ExecutionEnvironmentFactory.getLocal(), compilerSetold);
        }
        CompilerSet compilerSet = CompilerSetFactory.getCustomCompilerSet(folderBase.getAbsolutePath(), flavor, "MyCompilerSet", ExecutionEnvironmentFactory.getLocal());
        ToolchainSPIAccessor.add(ExecutionEnvironmentFactory.getLocal(), compilerSet);
        conf.getCompilerSet().restore("MyCompilerSet|" + flavorName, 51);
        conf.getDevelopmentHost().setBuildPlatform(platform);

        // Setup streams
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        try {
            pipedOutputStream.connect(pipedInputStream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(pipedOutputStream));
        BufferedReader rw = new BufferedReader(new InputStreamReader(pipedInputStream));

        // Generate (parts of) makefile
        try {
            ConfigurationMakefileWriter configurationMakefileWriter = new ConfigurationMakefileWriter(makeConfigurationDescriptor);
            ConfigurationMakefileWriter.writeCompileTargets(makeConfigurationDescriptor, conf, bw);
            ConfigurationMakefileWriter.writeLinkTarget(makeConfigurationDescriptor, conf, bw);
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Read and test result
        StringBuilder result = new StringBuilder();
        try {
            while (true) {
                String line = rw.readLine();
                if (line == null) {
                    rw.close();
                    break;
                }
                result.append(line).append("\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (TRACE) {
            System.out.println(result);
        }
        assertEqualsText(result.toString(),golden);
    }

    protected void assertEqualsText(String docText, String expectedText) {
        if (!docText.equals(expectedText)) {
            StringBuffer sb = new StringBuffer();
            sb.append("\n----- expected text: -----\n");
            appendDebugText(sb, expectedText);
            sb.append("\n----- document text: -----\n");
            appendDebugText(sb, docText);
            sb.append("\n-----\n");
            int startLine = 1;
            for (int i = 0; i < docText.length() && i < expectedText.length(); i++) {
                if (expectedText.charAt(i) == '\n') {
                    startLine++;
                }
                if (expectedText.charAt(i) != docText.charAt(i)) {
                    sb.append("Diff starts in line ").append(startLine).append("\n");
                    String context = expectedText.substring(i);
                    if (context.length() > 40) {
                        context = context.substring(0, 40);
                    }
                    sb.append("Expected:").append(context).append("\n");
                    context = docText.substring(i);
                    if (context.length() > 40) {
                        context = context.substring(0, 40);
                    }
                    sb.append("   Found:").append(context).append("\n");
                    break;
                }
            }
            fail(sb.toString());
        }
    }

    protected final void appendDebugChar(StringBuffer sb, char ch) {
        switch (ch) {
            case '\n':
                sb.append("\\n\n");
                break;
            case '\t':
                sb.append("\\t");
                break;

            default:
                sb.append(ch);
                break;
        }
    }

    protected final void appendDebugText(StringBuffer sb, String text) {
        for (int i = 0; i < text.length(); i++) {
            appendDebugChar(sb, text.charAt(i));
        }
    }

    @Test
    public void testMain_GNU_MacOSX() {
        StringBuilder golden = new StringBuilder();
        golden.append("LDLIBSOPTIONS=../hello1lib/dist/Debug/.../libhello1lib.a -L../hello3lib/dist/Debug/... -lhello3lib\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ../hello1lib/dist/Debug/.../libhello1lib.a\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ../hello3lib/dist/Debug/.../libhello3lib.dylib\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/xxx ${OBJECTFILES} ${LDLIBSOPTIONS}\n");
        testAppWithLibraries("Main_GNU_MacOSX", "GNU", PlatformTypes.PLATFORM_MACOSX, golden.toString());
    }

    @Test
    public void testMain_SunStudio_Solaris_Intel() {
        StringBuilder golden = new StringBuilder();
        golden.append("LDLIBSOPTIONS=../hello1lib/dist/Debug/.../libhello1lib.a -R../hello3lib/dist/Debug/... -L../hello3lib/dist/Debug/... -lhello3lib\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ../hello1lib/dist/Debug/.../libhello1lib.a\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ../hello3lib/dist/Debug/.../libhello3lib.so\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/xxx ${OBJECTFILES} ${LDLIBSOPTIONS}\n");
        testAppWithLibraries("Main_SunStudio_Solaris_Intel", "SunStudio", PlatformTypes.PLATFORM_SOLARIS_INTEL, golden.toString());
    }

    @Test
    public void testMain_GNU_Solaris_Intel() {
        StringBuilder golden = new StringBuilder();
        golden.append("LDLIBSOPTIONS=../hello1lib/dist/Debug/.../libhello1lib.a -R../hello3lib/dist/Debug/... -L../hello3lib/dist/Debug/... -lhello3lib\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ../hello1lib/dist/Debug/.../libhello1lib.a\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ../hello3lib/dist/Debug/.../libhello3lib.so\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx: ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/xxx ${OBJECTFILES} ${LDLIBSOPTIONS}\n");
        testAppWithLibraries("Main_GNU_Solaris_Intel", "GNU", PlatformTypes.PLATFORM_SOLARIS_INTEL, golden.toString());
    }

    @Test
    public void testMain_MinGW_Windows() {
        StringBuilder golden = new StringBuilder();
        golden.append("LDLIBSOPTIONS=../hello1lib/dist/Debug/.../libhello1lib.a -L../hello3lib/dist/Debug/... -lhello3lib\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx.exe: ../hello1lib/dist/Debug/.../libhello1lib.a\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx.exe: ../hello3lib/dist/Debug/.../libhello3lib.dll\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx.exe: ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/xxx ${OBJECTFILES} ${LDLIBSOPTIONS}\n");
        testAppWithLibraries("Main_MinGW_Windows", "MinGW", PlatformTypes.PLATFORM_WINDOWS, golden.toString());
    }

    @Test
    public void testMain_Cygwin_Windows() {
        StringBuilder golden = new StringBuilder();
        golden.append("LDLIBSOPTIONS=../hello1lib/dist/Debug/.../libhello1lib.a -L../hello3lib/dist/Debug/... -lhello3lib\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx.exe: ../hello1lib/dist/Debug/.../libhello1lib.a\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx.exe: ../hello3lib/dist/Debug/.../libhello3lib.dll\n");
        golden.append("\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/xxx.exe: ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/xxx ${OBJECTFILES} ${LDLIBSOPTIONS}\n");
        testAppWithLibraries("Main_Cygwin_Windows", "Cygwin", PlatformTypes.PLATFORM_WINDOWS, golden.toString());
    }

    @Test
    public void testDynLib_GNU_MacOSX() {
        StringBuilder golden = new StringBuilder();
        golden.append("\n");
        golden.append(MakeConfiguration.OBJECTDIR_MACRO+"/test.o: test.cc \n");
        golden.append("\t${MKDIR} -p "+MakeConfiguration.OBJECTDIR_MACRO+"\n");
        golden.append("\t${RM} \"$@.d\"\n");
        golden.append("\t$(COMPILE.cc) -g -fPIC  -MMD -MP -MF \"$@.d\" -o "+MakeConfiguration.OBJECTDIR_MACRO+"/test.o test.cc\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libXxx."+MakeConfiguration.CND_DLIB_EXT_MACRO+": ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/libXxx." + MakeConfiguration.CND_DLIB_EXT_MACRO + " ${OBJECTFILES} ${LDLIBSOPTIONS} -dynamiclib -install_name libXxx." + MakeConfiguration.CND_DLIB_EXT_MACRO + " -fPIC\n");
        testDynamicLibrary("DynLib_GNU_MacOSX", "GNU", PlatformTypes.PLATFORM_MACOSX, golden.toString());
    }

    @Test
    public void testDynLib_SunStudio_Solaris_Intel() {
        StringBuilder golden = new StringBuilder();
        golden.append("\n");
        golden.append(MakeConfiguration.OBJECTDIR_MACRO+"/test.o: test.cc \n");
        golden.append("\t${MKDIR} -p "+MakeConfiguration.OBJECTDIR_MACRO+"\n");
        golden.append("\t$(COMPILE.cc) -g -KPIC  -o "+MakeConfiguration.OBJECTDIR_MACRO+"/test.o test.cc\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libXxx."+MakeConfiguration.CND_DLIB_EXT_MACRO+": ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/libXxx." + MakeConfiguration.CND_DLIB_EXT_MACRO + " ${OBJECTFILES} ${LDLIBSOPTIONS} -G -KPIC -norunpath -h libXxx."+MakeConfiguration.CND_DLIB_EXT_MACRO+"\n");
        testDynamicLibrary("DynLib_SunStudio_Solaris_Intel", "SunStudio", PlatformTypes.PLATFORM_SOLARIS_INTEL, golden.toString());
    }

    @Test
    public void testDynLib_GNU_Solaris_Intel() {
        StringBuilder golden = new StringBuilder();
        golden.append("\n");
        golden.append(MakeConfiguration.OBJECTDIR_MACRO+"/test.o: test.cc \n");
        golden.append("\t${MKDIR} -p "+MakeConfiguration.OBJECTDIR_MACRO+"\n");
        golden.append("\t${RM} \"$@.d\"\n");
        golden.append("\t$(COMPILE.cc) -g -fPIC  -MMD -MP -MF \"$@.d\" -o "+MakeConfiguration.OBJECTDIR_MACRO+"/test.o test.cc\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libXxx."+MakeConfiguration.CND_DLIB_EXT_MACRO+": ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/libXxx." + MakeConfiguration.CND_DLIB_EXT_MACRO + " ${OBJECTFILES} ${LDLIBSOPTIONS} -G -fPIC\n");
        testDynamicLibrary("DynLib_GNU_Solaris_Intel", "GNU", PlatformTypes.PLATFORM_SOLARIS_INTEL, golden.toString());
    }

    @Test
    public void testDynLib_MinGW_Windows() {
        StringBuilder golden = new StringBuilder();
        golden.append("\n");
        golden.append(MakeConfiguration.OBJECTDIR_MACRO+"/test.o: test.cc \n");
        golden.append("\t${MKDIR} -p "+MakeConfiguration.OBJECTDIR_MACRO+"\n");
        golden.append("\t${RM} \"$@.d\"\n");
        golden.append("\t$(COMPILE.cc) -g  -MMD -MP -MF \"$@.d\" -o "+MakeConfiguration.OBJECTDIR_MACRO+"/test.o test.cc\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libXxx."+MakeConfiguration.CND_DLIB_EXT_MACRO+": ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/libXxx." + MakeConfiguration.CND_DLIB_EXT_MACRO + " ${OBJECTFILES} ${LDLIBSOPTIONS} -shared\n");
        testDynamicLibrary("DynLib_MinGW_Windows", "MinGW", PlatformTypes.PLATFORM_WINDOWS, golden.toString());
    }

    @Test
    public void testDynLib_Cygwin_Windows() {
        StringBuilder golden = new StringBuilder();
        golden.append("\n");
        golden.append(MakeConfiguration.OBJECTDIR_MACRO+"/test.o: test.cc \n");
        golden.append("\t${MKDIR} -p "+MakeConfiguration.OBJECTDIR_MACRO+"\n");
        golden.append("\t${RM} \"$@.d\"\n");
        golden.append("\t$(COMPILE.cc) -g  -MMD -MP -MF \"$@.d\" -o "+MakeConfiguration.OBJECTDIR_MACRO+"/test.o test.cc\n");
        golden.append("${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libXxx."+MakeConfiguration.CND_DLIB_EXT_MACRO+": ${OBJECTFILES}\n");
        golden.append("\t${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}\n");
        golden.append("\t${LINK.cc} -o "+MakeConfiguration.CND_DISTDIR_MACRO+"/"+MakeConfiguration.CND_CONF_MACRO+"/"+MakeConfiguration.CND_PLATFORM_MACRO+"/libXxx." + MakeConfiguration.CND_DLIB_EXT_MACRO + " ${OBJECTFILES} ${LDLIBSOPTIONS} -shared\n");
        testDynamicLibrary("DynLib_Cygwin_Windows", "Cygwin", PlatformTypes.PLATFORM_WINDOWS, golden.toString());
    }
}
