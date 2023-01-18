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
package org.netbeans.modules.cnd.makeproject.api.configurations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.modules.cnd.api.toolchain.AbstractCompiler;
import org.netbeans.modules.cnd.makeproject.api.configurations.ui.BooleanNodeProp;
import org.netbeans.modules.cnd.makeproject.api.configurations.ui.IntNodeProp;
import org.netbeans.modules.cnd.makeproject.configurations.ConfigurationMakefileWriter;
import org.netbeans.modules.cnd.makeproject.configurations.ui.StringNodeProp;
import org.netbeans.modules.cnd.makeproject.spi.configurations.AllOptionsProvider;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;

public abstract class BasicCompilerConfiguration implements AllOptionsProvider, ConfigurationBase {

    private String baseDir;
    private BasicCompilerConfiguration master;
    public static final int DEVELOPMENT_MODE_FAST = 0;
    public static final int DEVELOPMENT_MODE_DEBUG = 1;
    public static final int DEVELOPMENT_MODE_DEBUG_PERF = 2;
    public static final int DEVELOPMENT_MODE_TEST = 3;
    public static final int DEVELOPMENT_MODE_RELEASE_DIAG = 4;
    public static final int DEVELOPMENT_MODE_RELEASE = 5;
    public static final int DEVELOPMENT_MODE_RELEASE_PERF = 6;
    private static final String[] DEVELOPMENT_MODE_NAMES = {
        getString("FastBuildTxt"),
        getString("DebugTxt"),
        getString("PerformanceDebugTxt"),
        getString("TestCoverageTxt"),
        getString("DiagnosableReleaseTxt"),
        getString("ReleaseTxt"),
        getString("PerformanceReleaseTxt"),};
    private IntConfiguration developmentMode;
    public static final int WARNING_LEVEL_NO = 0;
    public static final int WARNING_LEVEL_DEFAULT = 1;
    public static final int WARNING_LEVEL_MORE = 2;
    public static final int WARNING_LEVEL_TAGS = 3;
    public static final int WARNING_LEVEL_CONVERT = 4;
    public static final int WARNING_LEVEL_32_64 = 5;
    private static final String[] WARNING_LEVEL_NAMES = {
        getString("NoWarningsTxt"),
        getString("SomeWarningsTxt"),
        getString("MoreWarningsTxt"),
        getString("ConvertWarningsTxt"),};
    private IntConfiguration warningLevel;
    public static final int BITS_DEFAULT = 0;
    public static final int BITS_32 = 1;
    public static final int BITS_64 = 2;
    private static final String[] BITS_NAMES = {
        getString("BITS_DEFAULT"),
        getString("BITS_32"),
        getString("BITS_64"),};
    private IntConfiguration sixtyfourBits;
    private InheritedBooleanConfiguration strip;
    public static final int MT_LEVEL_NONE = 0;
    public static final int MT_LEVEL_SAFE = 1;
    public static final int MT_LEVEL_AUTOMATIC = 2;
    public static final int MT_LEVEL_OPENMP = 3;
    private static final String[] MT_LEVEL_NAMES = {
        getString("NoneTxt"),
        getString("SafeTxt"),
        getString("AutomaticTxt"),
        getString("OpenMPTxt"),};
    private static final String[] MT_LEVEL_OPTIONS = null;
    private IntConfiguration mpLevel;
    private StringConfiguration additionalDependencies;
    private StringConfiguration tool;
    private OptionsConfiguration commandLineConfiguration;

    // Constructors
    protected BasicCompilerConfiguration(String baseDir, BasicCompilerConfiguration master) {
        this.baseDir = baseDir;
        this.master = master;
        developmentMode = new IntConfiguration(master != null ? master.getDevelopmentMode() : null, DEVELOPMENT_MODE_DEBUG, DEVELOPMENT_MODE_NAMES, null);
        warningLevel = new IntConfiguration(master != null ? master.getWarningLevel() : null, WARNING_LEVEL_DEFAULT, WARNING_LEVEL_NAMES, null);
        sixtyfourBits = new IntConfiguration(master != null ? master.getSixtyfourBits() : null, BITS_DEFAULT, BITS_NAMES, null);
        strip = new InheritedBooleanConfiguration(master != null ? master.getStrip() : null, false);
        mpLevel = new IntConfiguration(master != null ? master.getMTLevel() : null, MT_LEVEL_NONE, MT_LEVEL_NAMES, null);
        additionalDependencies = new StringConfiguration(master != null ? master.getAdditionalDependencies() : null, ""); // NOI18N
        tool = new StringConfiguration(master != null ? master.getTool() : null, ""); // NOI18N
        commandLineConfiguration = new OptionsConfiguration();
    }

    public void fixupMasterLinks(BasicCompilerConfiguration compilerConfiguration) {
        getDevelopmentMode().setMaster(compilerConfiguration.getDevelopmentMode());
        getWarningLevel().setMaster(compilerConfiguration.getWarningLevel());
        getSixtyfourBits().setMaster(compilerConfiguration.getSixtyfourBits());
        getStrip().setMaster(compilerConfiguration.getStrip());
        getAdditionalDependencies().setMaster(compilerConfiguration.getAdditionalDependencies());
        getTool().setMaster(compilerConfiguration.getTool());
    }

    @Override
    public boolean getModified() {
        return developmentMode.getModified() ||
                mpLevel.getModified() ||
                warningLevel.getModified() ||
                sixtyfourBits.getModified() ||
                strip.getModified() ||
                additionalDependencies.getModified() ||
                tool.getModified() ||
                commandLineConfiguration.getModified();
    }

    // baseDir
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getBaseDir() {
        return baseDir;
    }

    // To be overridden
    public String getOptions(AbstractCompiler compiler) {
        return "OVERRIDE"; // NOI18N
    }

    // Master
    public void setMaster(BasicCompilerConfiguration master) {
        this.master = master;
    }

    public BasicCompilerConfiguration getMaster() {
        return master;
    }

    public List<BasicCompilerConfiguration> getMasters(boolean addThis) {
        List<BasicCompilerConfiguration> res = new ArrayList<>();
        if (addThis) {
            res.add(this);
        }
        BasicCompilerConfiguration current = master;
        while (current != null) {
            if (res.contains(current)) {
                assert false:"Infinite loop in configurations"; //NOI18N
                break;
            }
            res.add(current);
            current = current.getMaster();
        }
        return res;
    }
    
    // Development Mode
    public void setDevelopmentMode(IntConfiguration developmentMode) {
        this.developmentMode = developmentMode;
    }

    public IntConfiguration getDevelopmentMode() {
        return developmentMode;
    }

    // Warning Level
    public void setWarningLevel(IntConfiguration warningLevel) {
        this.warningLevel = warningLevel;
    }

    public IntConfiguration getWarningLevel() {
        return warningLevel;
    }


    // SixtyfourBits
    public void setSixtyfourBits(IntConfiguration sixtyfourBits) {
        this.sixtyfourBits = sixtyfourBits;
    }

    public IntConfiguration getSixtyfourBits() {
        return sixtyfourBits;
    }

    // MT Level
    public void setMTLevel(IntConfiguration mpLevel) {
        this.mpLevel = mpLevel;
    }

    public IntConfiguration getMTLevel() {
        return mpLevel;
    }
    
    // To be overridden
    protected String[] getMTLevelOptions() {
        return MT_LEVEL_OPTIONS;
    }

    // Strip
    public void setStrip(InheritedBooleanConfiguration strip) {
        this.strip = strip;
    }

    public InheritedBooleanConfiguration getStrip() {
        return strip;
    }

    public void setAdditionalDependencies(StringConfiguration additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    public StringConfiguration getAdditionalDependencies() {
        return additionalDependencies;
    }

    // Tool
    public void setTool(StringConfiguration tool) {
        this.tool = tool;
    }

    public StringConfiguration getTool() {
        return tool;
    }

    // CommandLine
    public OptionsConfiguration getCommandLineConfiguration() {
        return commandLineConfiguration;
    }
    
    protected String getCommandLineOptions(boolean inherit) {
        if (!inherit) {
            return getCommandLineConfiguration().getValue();
        }

        List<String> options = new LinkedList<>();
        //to fix bz#231603 - C/C++ Additional options passed to commandline twice
        //we have $(COMPILE.cc) in makefile which is extended to COMPILE.c=$(CC) $(CFLAGS) $(CPPFLAGS) -c        
        //so when user adds some additional options on the project level
        //they are recorded as CFLAGS in Makefile
        //and there is no any need to return them when compile target is written
        //that's why we check if parent.getMaster() != null
        List<BasicCompilerConfiguration> masters = getMasters(true);
        for(int i = 0; i < masters.size() -1; i++) {
            options.add(0, masters.get(i).getCommandLineConfiguration().getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (String opt : options) {
            sb.append(opt).append(' ');
        }
        return sb.toString().trim();
    }

    public void setCommandLineConfiguration(OptionsConfiguration commandLineConfiguration) {
        this.commandLineConfiguration = commandLineConfiguration;
    }
    
    public String getOutputFile(Item item, MakeConfiguration conf, boolean expanded) {
        String filePath = item.getPath(true);
        // qmake generated Makefile expects to find all object files in one directory 
        // the project directory by default. So for Qt projects we need to get rid of any paths.
        // In other case in won't be possible to compile single file, which is not located 
        // in not a project directory.
        // See Bug 189542 - compile single file (QT)
        if (conf.isQmakeConfiguration()) {            
            filePath = CndPathUtilities.getBaseName(filePath);
        }
        String fileName = filePath;
        String suffix = ".o"; // NOI18N
        boolean append = false;
        if (item.hasHeaderOrSourceExtension(false, false)) {
            suffix = ".pch"; // NOI18N
            ItemConfiguration itemConf = item.getItemConfiguration(conf);
            if (conf.getCompilerSet().getCompilerSet() != null && itemConf != null) {
                AbstractCompiler compiler = (AbstractCompiler) conf.getCompilerSet().getCompilerSet().getTool(itemConf.getTool());
                if (compiler != null && compiler.getDescriptor() != null) {
                    suffix = compiler.getDescriptor().getPrecompiledHeaderSuffix();
                    append = compiler.getDescriptor().getPrecompiledHeaderSuffixAppend();
                }
            }
        }
        int i = fileName.lastIndexOf('.'); // NOI18N
        if (i >= 0 && !append) {
            fileName = fileName.substring(0, i) + suffix;
        } else {
            fileName = fileName + suffix;
        }

        String dirName;
        if (item.getFolder() != null && item.getFolder().isTest()) {
            if (expanded) {
                dirName = ConfigurationMakefileWriter.getTestObjectDir(conf);
            } else {
                // I think it's a good idea to get rid of hardcoded paths
                // (i.e. MakeActionProvider::getOutputFile, ConfigurationMakefileWriter::writeLinkTestTarget, ...)
                // target.replace(MakeConfiguration.OBJECTDIR_MACRO, "${TESTDIR}");
                dirName = "${TESTDIR}"; // NOI18N
            }
        } else {
            if (expanded) {
                dirName = ConfigurationMakefileWriter.getObjectDir(conf);
            } else {
                dirName = MakeConfiguration.OBJECTDIR_MACRO;
            }
        }

        if (CndPathUtilities.isPathAbsolute(fileName) || filePath.startsWith("..")) { // NOI18N;
            String ofileName = CndPathUtilities.getBaseName(fileName);
            String odirName = CndPathUtilities.getDirName(fileName);
            if (odirName == null) {
                odirName = ""; // NOI18N
            }
            String absPath = dirName + '/' + MakeConfiguration.EXT_FOLDER + '/' + Integer.toHexString(odirName.hashCode()) + '/' + ofileName; // UNIX path
            absPath = CndPathUtilities.replaceOddCharacters(absPath, '_');
            return absPath;
        } else {
            fileName = CndPathUtilities.escapeOddCharacters(fileName);
            return dirName + '/' + fileName; // UNIX path
        }
    }

    // Assigning & Cloning
    protected void assign(BasicCompilerConfiguration conf) {
        setBaseDir(conf.getBaseDir());
        getDevelopmentMode().assign(conf.getDevelopmentMode());
        getWarningLevel().assign(conf.getWarningLevel());
        getSixtyfourBits().assign(conf.getSixtyfourBits());
        getStrip().assign(conf.getStrip());
        getMTLevel().assign(conf.getMTLevel());
        getAdditionalDependencies().assign(conf.getAdditionalDependencies());
        getTool().assign(conf.getTool());
        getCommandLineConfiguration().assign(conf.getCommandLineConfiguration());
    }

    // Sheets
    protected Sheet.Set getBasicSet() {
        Sheet.Set set = new Sheet.Set();
        set.setName("BasicOptions"); // NOI18N
        set.setDisplayName(getString("BasicOptionsTxt"));
        set.setShortDescription(getString("BasicOptionsHint"));
        set.put(new IntNodeProp(getDevelopmentMode(), true, "DevelopmentMode", getString("DevelopmentModeTxt"), getString("DevelopmentModeHint"))); // NOI18N
        set.put(new IntNodeProp(getWarningLevel(), true, "WarningLevel", getString("WarningLevelTxt"), getString("WarningLevelHint"))); // NOI18N
        set.put(new IntNodeProp(getSixtyfourBits(), true, "64BitArchitecture", getString("64BitArchitectureTxt"), getString("64BitArchitectureHint"))); // NOI18N
        set.put(new BooleanNodeProp(getStrip(), true, "StripSymbols", getString("StripSymbolsTxt"), getString("StripSymbolsHint"))); // NOI18N
        return set;
    }

    protected Sheet.Set getInputSet() {
        Sheet.Set set = new Sheet.Set();
        set.setName("Input"); // NOI18N
        set.setDisplayName(getString("InputTxt"));
        set.setShortDescription(getString("InputHint"));
        set.put(new StringNodeProp(getAdditionalDependencies(), "AdditionalDependencies", getString("AdditionalDependenciesTxt1"), getString("AdditionalDependenciesHint")));  // NOI18N
        return set;
    }

    /** Look up i18n strings here */
    private static String getString(String s) {
        return NbBundle.getMessage(BasicCompilerConfiguration.class, s);
    }
}
