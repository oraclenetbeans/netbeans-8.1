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
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.debug.DebugUtils;
import org.netbeans.modules.cnd.makeproject.api.configurations.LibraryItem.OptionItem;
import org.netbeans.modules.cnd.makeproject.api.configurations.ui.BooleanNodeProp;
import org.netbeans.modules.cnd.makeproject.configurations.CppUtils;
import org.netbeans.modules.cnd.makeproject.configurations.ui.StringListNodeProp;
import org.netbeans.modules.cnd.makeproject.configurations.ui.VectorNodeProp;
import org.netbeans.modules.cnd.makeproject.ui.utils.TokenizerFactory;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public abstract class CCCCompilerConfiguration extends BasicCompilerConfiguration {

    public static final boolean STANDARDS_SUPPORT = DebugUtils.getBoolean("cnd.support.standards", true); //NOI18N

    public static final int LIBRARY_LEVEL_NONE = 0;
    public static final int LIBRARY_LEVEL_RUNTIME = 1;
    public static final int LIBRARY_LEVEL_CLASSIC = 2;
    public static final int LIBRARY_LEVEL_BINARY = 3;
    public static final int LIBRARY_LEVEL_CONFORMING = 4;
    private static final String[] LIBRARY_LEVEL_NAMES = {
        getString("NoneTxt"),
        getString("RuntimeOnlyTxt"),
        getString("ClassicIostreamsTxt"),
        getString("BinaryStandardTxt"),
        getString("ConformingStandardTxt"),};
    private static final String[] LIBRARY_LEVEL_OPTIONS = null;
    private IntConfiguration libraryLevel;
    public static final int STANDARDS_OLD = 0;
    public static final int STANDARDS_LEGACY = 1;
    public static final int STANDARDS_DEFAULT = 2;
    public static final int STANDARDS_MODERN = 3;
    private static final String[] STANDARDS_NAMES = {
        getString("OldTxt"),
        getString("LegacyTxt"),
        getString("DefaultTxt"),
        getString("ModernTxt"),};
    private static final String[] STANDARD_OPTIONS = null;
    private IntConfiguration standardsEvolution;
    public static final int LANGUAGE_EXT_NONE = 0;
    public static final int LANGUAGE_EXT_DEFAULT = 1;
    public static final int LANGUAGE_EXT_ALL = 2;
    private static final String[] LANGUAGE_EXT_NAMES = {
        getString("NoneTxt"),
        getString("DefaultTxt"),
        getString("AllTxt"),};
    private static final String[] LANGUAGE_EXT_OPTIONS = null;
    private IntConfiguration languageExt;
    private VectorConfiguration<String> includeDirectories;
    private BooleanConfiguration inheritIncludes;
    private VectorConfiguration<String> includeFiles;
    private BooleanConfiguration inheritFiles;
    private VectorConfiguration<String> preprocessorConfiguration;
    private VectorConfiguration<String> preprocessorUndefinedConfiguration;
    private BooleanConfiguration inheritPreprocessor;
    private BooleanConfiguration inheritUndefinedPreprocessor;
    private BooleanConfiguration useLinkerPkgConfigLibraries;
    private StringConfiguration importantFlags;
    private MakeConfiguration owner;

    // Constructors
    protected CCCCompilerConfiguration(String baseDir, CCCCompilerConfiguration master, MakeConfiguration owner) {
        super(baseDir, master);
        assert owner != null;
        this.owner = owner;
        libraryLevel = new IntConfiguration(master != null ? master.getLibraryLevel() : null, LIBRARY_LEVEL_BINARY, LIBRARY_LEVEL_NAMES, getLibraryLevelOptions());
        standardsEvolution = new IntConfiguration(master != null ? master.getStandardsEvolution() : null, STANDARDS_DEFAULT, STANDARDS_NAMES, getStandardsEvolutionOptions());
        languageExt = new IntConfiguration(master != null ? master.getLanguageExt() : null, LANGUAGE_EXT_DEFAULT, LANGUAGE_EXT_NAMES, getLanguageExtOptions());
        includeDirectories = new VectorConfiguration<>(master != null ? master.getIncludeDirectories() : null);
        inheritIncludes = new BooleanConfiguration(true);
        includeFiles = new VectorConfiguration<>(master != null ? master.getIncludeFiles() : null);
        inheritFiles = new BooleanConfiguration(true);
        preprocessorConfiguration = new VectorConfiguration<>(master != null ? master.getPreprocessorConfiguration() : null);
        preprocessorUndefinedConfiguration = new VectorConfiguration<>(master != null ? master.getUndefinedPreprocessorConfiguration() : null);
        inheritPreprocessor = new BooleanConfiguration(true);
        inheritUndefinedPreprocessor = new BooleanConfiguration(true);
        useLinkerPkgConfigLibraries = new BooleanConfiguration(true);
        importantFlags = new StringConfiguration(null, "");
    }

    public void fixupMasterLinks(CCCCompilerConfiguration compilerConfiguration) {
        super.fixupMasterLinks(compilerConfiguration);
        getMTLevel().setMaster(compilerConfiguration.getMTLevel());
        getLibraryLevel().setMaster(compilerConfiguration.getLibraryLevel());
        getStandardsEvolution().setMaster(compilerConfiguration.getStandardsEvolution());
        getLanguageExt().setMaster(compilerConfiguration.getLanguageExt());
    }

    @Override
    public boolean getModified() {
        boolean modifiedFlags = importantFlags.getValue() != null && !importantFlags.getValue().isEmpty();
        return super.getModified() ||
                libraryLevel.getModified() ||
                standardsEvolution.getModified() ||
                languageExt.getModified() ||
                includeDirectories.getModified() ||
                inheritIncludes.getModified() ||
                includeFiles.getModified() ||
                inheritFiles.getModified() ||
                preprocessorConfiguration.getModified() ||
                inheritPreprocessor.getModified() ||
                preprocessorUndefinedConfiguration.getModified() ||
                inheritUndefinedPreprocessor.getModified() ||
                useLinkerPkgConfigLibraries.getModified() ||
                modifiedFlags;
    }

    protected final String[] getLibraryLevelOptions() {
        return LIBRARY_LEVEL_OPTIONS;
    }

    protected final String[] getStandardsEvolutionOptions() {
        return STANDARD_OPTIONS;
    }

    protected final String[] getLanguageExtOptions() {
        return LANGUAGE_EXT_OPTIONS;
    }

    // Library Level
    public void setLibraryLevel(IntConfiguration libraryLevel) {
        this.libraryLevel = libraryLevel;
    }

    public IntConfiguration getLibraryLevel() {
        return libraryLevel;
    }

    // Standards Evolution
    public void setStandardsEvolution(IntConfiguration standardsEvolution) {
        this.standardsEvolution = standardsEvolution;
    }

    public IntConfiguration getStandardsEvolution() {
        return standardsEvolution;
    }

    // languageExt
    public void setLanguageExt(IntConfiguration languageExt) {
        this.languageExt = languageExt;
    }

    public IntConfiguration getLanguageExt() {
        return languageExt;
    }

    // Include Directories
    public VectorConfiguration<String> getIncludeDirectories() {
        return includeDirectories;
    }

    public void setIncludeDirectories(VectorConfiguration<String> includeDirectories) {
        this.includeDirectories = includeDirectories;
    }

    // Inherit Include Directories
    public BooleanConfiguration getInheritIncludes() {
        return inheritIncludes;
    }

    public void setInheritIncludes(BooleanConfiguration inheritIncludes) {
        this.inheritIncludes = inheritIncludes;
    }

    // Include Files
    public VectorConfiguration<String> getIncludeFiles() {
        return includeFiles;
    }

    public void setIncludeFiles(VectorConfiguration<String> includeFiles) {
        this.includeFiles = includeFiles;
    }

    // Inherit Include Files
    public BooleanConfiguration getInheritFiles() {
        return inheritFiles;
    }

    public void setInheritFiles(BooleanConfiguration inheritFiles) {
        this.inheritFiles = inheritFiles;
    }

    // Preprocessor
    public VectorConfiguration<String> getPreprocessorConfiguration() {
        return preprocessorConfiguration;
    }

    public void setPreprocessorConfiguration(VectorConfiguration<String> preprocessorConfiguration) {
        this.preprocessorConfiguration = preprocessorConfiguration;
    }
    
    // Preprocessor
    public VectorConfiguration<String> getUndefinedPreprocessorConfiguration() {
        return preprocessorUndefinedConfiguration;
    }

    public void setUndefinedPreprocessorConfiguration(VectorConfiguration<String> preprocessorUndefinedConfiguration) {
        this.preprocessorUndefinedConfiguration = preprocessorUndefinedConfiguration;
    }

    // Inherit Include Directories
    public BooleanConfiguration getInheritPreprocessor() {
        return inheritPreprocessor;
    }

    public void setInheritPreprocessor(BooleanConfiguration inheritPreprocessor) {
        this.inheritPreprocessor = inheritPreprocessor;
    }

    public BooleanConfiguration getInheritUndefinedPreprocessor() {
        return inheritUndefinedPreprocessor;
    }

    public void setInheritUndefinedPreprocessor(BooleanConfiguration inheritUndefinedPreprocessor) {
        this.inheritUndefinedPreprocessor = inheritUndefinedPreprocessor;
    }

    // Linker libraries
    public BooleanConfiguration getUseLinkerLibraries() {
        return useLinkerPkgConfigLibraries;
    }

    public void setUseLinkerLibraries(BooleanConfiguration useLinkerPkgConfigLibraries) {
        this.useLinkerPkgConfigLibraries = useLinkerPkgConfigLibraries;
    }

    public StringConfiguration getImportantFlags() {
        return importantFlags;
    }
    
    public void setImportantFlags(StringConfiguration importantFlags) {
        this.importantFlags = importantFlags;
    }
    
    /**
     * @return the owner
     */
    public MakeConfiguration getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(MakeConfiguration owner) {
        this.owner = owner;
    }

    // Clone and assign
    protected void assign(CCCCompilerConfiguration conf) {
        // BasicCompilerConfiguration
        super.assign(conf);
        // XCompilerConfiguration
        getLibraryLevel().assign(conf.getLibraryLevel());
        getStandardsEvolution().assign(conf.getStandardsEvolution());
        getLanguageExt().assign(conf.getLanguageExt());
        getIncludeDirectories().assign(conf.getIncludeDirectories());
        getInheritIncludes().assign(conf.getInheritIncludes());
        getIncludeFiles().assign(conf.getIncludeFiles());
        getInheritFiles().assign(conf.getInheritFiles());
        getPreprocessorConfiguration().assign(conf.getPreprocessorConfiguration());
        getInheritPreprocessor().assign(conf.getInheritPreprocessor());
        getUndefinedPreprocessorConfiguration().assign(conf.getUndefinedPreprocessorConfiguration());
        getInheritUndefinedPreprocessor().assign(conf.getInheritUndefinedPreprocessor());
        getUseLinkerLibraries().assign(conf.getUseLinkerLibraries());
        getImportantFlags().assign(conf.getImportantFlags());
    }

    // Sheet
    protected Sheet.Set getSet(final Project project, final Folder folder, final Item item) {
        OptionToString visitor = new OptionToString(null, null);

        Sheet.Set set1 = new Sheet.Set();
        set1.setName("General"); // NOI18N
        set1.setDisplayName(getString("GeneralTxt"));
        set1.setShortDescription(getString("GeneralHint"));
        StringBuilder inheritedValues;
        {
            // Include Dirctories
            inheritedValues = new StringBuilder();
            List<CCCCompilerConfiguration> list = new ArrayList<>();
            for(BasicCompilerConfiguration master : getMasters(false)) {
                list.add((CCCCompilerConfiguration)master);
                if (!((CCCCompilerConfiguration)master).getInheritIncludes().getValue()) {
                    break;
                }
            }
            for(int i = list.size() - 1; i >= 0; i--) {
                inheritedValues.append(list.get(i).getIncludeDirectories().toString(visitor, "\n")); //NOI18N
            }
            set1.put(new VectorNodeProp(getIncludeDirectories(), getMaster() != null ? getInheritIncludes() : null, owner.getBaseFSPath(), 
                    new String[]{"IncludeDirectories", getString("IncludeDirectoriesTxt"), getString("IncludeDirectoriesHint"), inheritedValues.toString()},
                    true, false, new HelpCtx("AddtlIncludeDirectories")){// NOI18N
                private final TokenizerFactory.Converter converter = TokenizerFactory.getPathConverter(project, folder, item, "-I"); //NOI18N
                @Override
                protected List<String> convertToList(String text) {
                    return converter.convertToList(text);
                }
                @Override
                protected String convertToString(List<String> list) {
                    return converter.convertToString(list);
                }
            });
        } 
        {
            // Include Dirctories
            inheritedValues = new StringBuilder();
            List<CCCCompilerConfiguration> list = new ArrayList<>();
            for(BasicCompilerConfiguration master : getMasters(false)) {
                list.add((CCCCompilerConfiguration)master);
                if (!((CCCCompilerConfiguration)master).getInheritFiles().getValue()) {
                    break;
                }
            }
            for(int i = list.size() - 1; i >= 0; i--) {
                inheritedValues.append(list.get(i).getIncludeFiles().toString(visitor, "\n")); //NOI18N
            }
            set1.put(new VectorNodeProp(getIncludeFiles(), getMaster() != null ? getInheritFiles() : null, owner.getBaseFSPath(), 
                    new String[]{"IncludeFiles", getString("IncludeFilesTxt"), getString("IncludeFilesHint"), inheritedValues.toString()},
                    true, false, new HelpCtx("AddtlIncludeFiles")){// NOI18N
                private final TokenizerFactory.Converter converter = TokenizerFactory.getPathConverter(project, folder, item, "-include"); //NOI18N
                @Override
                protected List<String> convertToList(String text) {
                    return converter.convertToList(text);
                }
                @Override
                protected String convertToString(List<String> list) {
                    return converter.convertToString(list);
                }
            });
        } 
        {
            // Preprocessor Macros
            inheritedValues = new StringBuilder();
            for(BasicCompilerConfiguration master : getMasters(false)) {
                inheritedValues.append(((CCCCompilerConfiguration)master).getPreprocessorConfiguration().toString(visitor, "\n")); //NOI18N
                if (!((CCCCompilerConfiguration)master).getInheritPreprocessor().getValue()) {
                    break;
                }
            }
            set1.put(new StringListNodeProp(getPreprocessorConfiguration(), getMaster() != null ? getInheritPreprocessor() : null, new String[]{"preprocessor-definitions", getString("PreprocessorDefinitionsTxt"), getString("PreprocessorDefinitionsHint"), getString("PreprocessorDefinitionsLbl"), inheritedValues.toString()}, true, new HelpCtx("preprocessor-definitions")){  // NOI18N
                @Override
                protected List<String> convertToList(String text) {
                    return TokenizerFactory.MACRO_CONVERTER.convertToList(text);
                }

                @Override
                protected String convertToString(List<String> list) {
                    return TokenizerFactory.MACRO_CONVERTER.convertToString(list);
                }
            });
        }
        if (owner.getConfigurationType().getValue() == MakeConfiguration.TYPE_MAKEFILE) {
            // Undefined Macros
            inheritedValues = new StringBuilder();
            for(BasicCompilerConfiguration master : getMasters(false)) {
                inheritedValues.append(((CCCCompilerConfiguration)master).getUndefinedPreprocessorConfiguration().toString(visitor));
                if (!((CCCCompilerConfiguration)master).getInheritUndefinedPreprocessor().getValue()) {
                    break;
                }
            }
            set1.put(new StringListNodeProp(getUndefinedPreprocessorConfiguration(),
                    getMaster() != null ? getInheritUndefinedPreprocessor() : null,
                    new String[]{"preprocessor-undefined", getString("PreprocessorUndefinedTxt"), getString("PreprocessorUndefinedHint"), getString("PreprocessorUndefinedLbl"), inheritedValues.toString()}, // NOI18N
                    true, new HelpCtx("preprocessor-undefined")) { // NOI18N

                        @Override
                        protected List<String> convertToList(String text) {
                            return TokenizerFactory.UNDEF_CONVERTER.convertToList(text);
                        }

                        @Override
                        protected String convertToString(List<String> list) {
                            return TokenizerFactory.UNDEF_CONVERTER.convertToString(list);
                        }
                    });
        }
        if (this.getMaster() == null) {            
            final IntConfiguration configurationType = this.getOwner() == null ? null : this.getOwner().getConfigurationType();
            if (configurationType == null || (configurationType.getValue() != MakeConfiguration.TYPE_MAKEFILE)) {
                set1.put(new BooleanNodeProp(getUseLinkerLibraries(), true,
                        "use-linker-libraries", getString("UseLinkerLibrariesTxt"), getString("UseLinkerLibrariesHint"))); // NOI18N
            }
        }
        return set1;
    }

    private CCCCompilerConfiguration getTopMaster() {
        List<BasicCompilerConfiguration> masters = getMasters(true);
        return (CCCCompilerConfiguration)masters.get(masters.size()-1);
    }

    // Sheet
    protected Sheet getSheet(Project project) {
        Sheet sheet = new Sheet();
        sheet.put(getSet(project, null, null));
        return sheet;
    }

    /** Look up i18n strings here */
    private static String getString(String s) {
        return NbBundle.getMessage(CCCCompilerConfiguration.class, s);
    }

    private static final String LIB_FLAG = " --libs "; // NOI18N
    private static final String FLAGS_FLAG = " --cflags "; // NOI18N

    protected String getLibrariesFlags() {
        CCCCompilerConfiguration topMaster = getTopMaster();
        if (!topMaster.getUseLinkerLibraries().getValue() || topMaster.getOwner() == null) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        for (LibraryItem lib : topMaster.getOwner().getLinkerConfiguration().getLibrariesConfiguration().getValue()) {
            if (lib.getType() == LibraryItem.OPTION_ITEM) {
                OptionItem option = (OptionItem) lib;
                String task = option.getLibraryOption();
                if (task.length() > 2  && task.charAt(0) == '`' && task.charAt(task.length()-1) == '`') { // NOI18N
                    int i = task.indexOf(LIB_FLAG);
                    if (i > 0) {
                        if (buf.length() > 0) {
                            buf.append(' ');
                        }
                        buf.append(task.substring(0, i));
                        buf.append(FLAGS_FLAG);
                        buf.append( task.substring(i+LIB_FLAG.length()));
                    }
                }
            }
        }
        if (buf.length() > 0) {
            buf.append(' ');
        }
        return buf.toString();
    }

    protected abstract String getUserIncludeFlag(CompilerSet cs);

    protected abstract String getUserFileFlag(CompilerSet cs);

    protected abstract String getUserMacroFlag(CompilerSet cs);

    public static class OptionToString implements VectorConfiguration.ToString<String> {

        private final CompilerSet compilerSet;
        private final String prepend;

        public OptionToString(CompilerSet compilerSet, String prepend) {
            this.compilerSet = compilerSet;
            this.prepend = prepend;
        }

        @Override
        public String toString(String item) {
            if (0 < item.length()) {
                if (compilerSet != null) {
                    item = CppUtils.normalizeDriveLetter(compilerSet, item);
                }
                item = CndPathUtilities.escapeOddCharacters(item);
                return prepend == null ? item : prepend + item;
            } else {
                return ""; // NOI18N
            }
        }
    }
    
    protected static class StringRONodeProp extends PropertySupport<String> {

        private final String value;

        public StringRONodeProp(String name, String description, String value) {
            super(name, String.class, name, name, true, false);
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String v) {
        }
    }
}
