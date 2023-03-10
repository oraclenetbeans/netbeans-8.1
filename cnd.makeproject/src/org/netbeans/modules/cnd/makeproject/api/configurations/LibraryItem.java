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

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.cnd.api.remote.RemoteFileUtil;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.makeproject.api.MakeArtifact;
import org.netbeans.modules.cnd.makeproject.configurations.CppUtils;
import org.netbeans.modules.cnd.makeproject.platform.Platform;
import org.netbeans.modules.cnd.makeproject.platform.Platforms;
import org.netbeans.modules.cnd.makeproject.platform.StdLibraries;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.netbeans.modules.cnd.utils.FSPath;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

public abstract class LibraryItem implements Cloneable {
    public static final int PROJECT_ITEM = 0;
    public static final int STD_LIB_ITEM = 1;
    public static final int LIB_ITEM = 2;
    public static final int LIB_FILE_ITEM = 3;
    public static final int OPTION_ITEM = 4;

    private int type;

    protected LibraryItem() {
    }

    public int getType() {
	return type;
    }

    public void setType(int type) {
	this.type = type;
    }

    // Should be overridden
    public String getToolTip() {
	return "Should be overridden"; // NOI18N
    }

    // Should be overridden
    public String getIconName() {
	return "org/netbeans/modules/cnd/resources/blank.gif"; // NOI18N
    }

    // Should be overridden
    public void setValue(String value) {
    }

    // Should be overridden
    public String getPath() {
        return null;
    }

    // Should be overridden
    @Override
    public String toString() {
	return "Should be overridden"; // NOI18N
    }

    // Should be overridden
//    public String getOption() {
//	return ""; // NOI18N
//    }
    
    // Must be overridden
    public abstract String getOption(MakeConfiguration conf);

    // Should be overridden
    public boolean canEdit() {
	return false;
    }

    // Should be overridden
    @Override
    public LibraryItem clone() {
	return this;
    }

    public static class ProjectItem extends LibraryItem implements Cloneable {
	private MakeArtifact makeArtifact;
	private Project project; // Just for caching

	public ProjectItem(MakeArtifact makeArtifact) {
	    this.makeArtifact = makeArtifact;
	    setType(PROJECT_ITEM);
	}

	public MakeArtifact getMakeArtifact() {
	    return makeArtifact;
	}

	public void setMakeArtifact(MakeArtifact makeArtifact) {
	    this.makeArtifact = makeArtifact;
	}

	public Project getProject(FSPath baseDir) {
	    if (project == null) {
		String location = CndPathUtilities.toAbsolutePath(baseDir.getFileObject(), getMakeArtifact().getProjectLocation());
		try {
		    FileObject fo = RemoteFileUtil.getFileObject(baseDir.getFileObject(), location);
                    if (fo != null && fo.isValid()) {
                        fo = CndFileUtils.getCanonicalFileObject(fo);
                    }
                    if (fo != null && fo.isValid()) {
                        project = ProjectManager.getDefault().findProject(fo);
                    }
		}
		catch (Exception e) {
		    System.err.println("Cannot find subproject in '"+location+"' "+e); // FIXUP // NOI18N
		}
	    }
	    return project;
	}

        @Override
	public String getToolTip() {
            String ret = NbBundle.getMessage(LibraryItem.class, "ProjectTxt", getMakeArtifact().getProjectLocation()); // NOI18N
            if (getMakeArtifact().getOutput() != null && getMakeArtifact().getOutput().length() > 0) {
                ret = ret + " (" + getMakeArtifact().getOutput() + ")"; // NOI18N
            }
            return ret;
	}

        @Override
	public String getIconName() {
	    return "org/netbeans/modules/cnd/makeproject/ui/resources/makeProject.gif"; // NOI18N
	}

        @Override
        public String toString() {
            String ret = CndPathUtilities.getBaseName(getMakeArtifact().getProjectLocation());
            if (getMakeArtifact().getOutput() != null && getMakeArtifact().getOutput().length() > 0) {
                ret = ret + " (" + getMakeArtifact().getOutput() + ")"; // NOI18N
            }
            return ret;
        }

        @Override
        public void setValue(String value) {
            // Can't do
        }

        @Override
        public String getPath() {
            String libPath = getMakeArtifact().getOutput();
            if (!CndPathUtilities.isPathAbsolute(libPath)) {
                libPath = getMakeArtifact().getProjectLocation() + '/' + libPath; // UNIX path
            }
            return libPath;
        }

        @Override
        public String getOption(MakeConfiguration conf) {
            CompilerSet compilerSet = conf.getCompilerSet().getCompilerSet();
            Platform platform = Platforms.getPlatform(conf.getDevelopmentHost().getBuildPlatform());
            String libPath = getPath();
            String libDir = CndPathUtilities.getDirName(libPath);
            String libName = CndPathUtilities.getBaseName(libPath);
            return platform.getLibraryLinkOption(libName, libDir, libPath, compilerSet);
        }

        @Override
	public boolean canEdit() {
	    return false;
	}

        @Override
	public ProjectItem clone() {
	    ProjectItem clone = new ProjectItem(getMakeArtifact());
	    return clone;
	}
    }

    public static class StdLibItem extends LibraryItem implements Cloneable {
	private final String name;
	private final String displayName;
	private final String[] libs;

	public StdLibItem(String name, String displayName, String[] libs) {
            this.name = name;
            this.displayName = displayName;
            this.libs = libs;
            setType(STD_LIB_ITEM);
        }

        public static StdLibItem getStandardItem(String id) {
            return StdLibraries.getStandardLibary(id);
        }

        public String getName() {
            return name;
        }

	public String getDisplayName() {
	    return displayName;
	}

	public String[] getLibs() {
	    return libs;
	}

        @Override
	public String getToolTip() {
            StringBuilder options = new StringBuilder();
            for (int i = 0; i < libs.length; i++) {
                if (options.length()>0) {
                    options.append(' '); // NOI18N
                }
                options.append(libs[i]); // NOI18N
            }
	    return NbBundle.getMessage(LibraryItem.class, "StandardLibraryTxt", getDisplayName(), options.toString()); // NOI18N
	}

        @Override
	public String getIconName() {
	    return "org/netbeans/modules/cnd/resources/stdLibrary.gif"; // NOI18N
	}

        @Override
	public String toString() {
	    return getDisplayName();
	}

        @Override
	public void setValue(String value) {
	    // Can't do
	}

        @Override
        public String getOption(MakeConfiguration conf) {
            StringBuilder options = new StringBuilder();
            String flag = null;
            for (int i = 0; i < libs.length; i++) {
                if (libs[i].charAt(0) != '-') {
                    if (flag == null) {
                        CompilerSet cs = conf.getCompilerSet().getCompilerSet();
                        if (cs != null) {
                            flag = cs.getCompilerFlavor().getToolchainDescriptor().getLinker().getLibraryFlag();
                        }
                    }
                    if (flag != null) {
                        options.append(flag).append(libs[i]).append(" "); // NOI18N
                    }
                } else {
                    options.append(libs[i]).append(" "); // NOI18N
                }
            }
            return options.toString();
        }

        @Override
	public boolean canEdit() {
	    return false;
	}

        @Override
	public StdLibItem clone() {
	    StdLibItem clone = new StdLibItem(getName(), getDisplayName(), getLibs());
	    return clone;
	}
    }

    public static class LibItem extends LibraryItem implements Cloneable {
	private String libName;

	public LibItem(String libName) {
	    this.libName = libName;
	    setType(LIB_ITEM);
	}

	public String getLibName() {
	    return libName;
	}

	public void setLibName(String libName) {
	    this.libName = libName;
	}

        @Override
	public String getToolTip() {
	    return NbBundle.getMessage(LibraryItem.class, "LibraryTxt", getLibName()); // NOI18N
	}

        @Override
	public String getIconName() {
	    return "org/netbeans/modules/cnd/loaders/LibraryIcon.gif"; // NOI18N
	}

        @Override
	public String toString() {
	    return getLibName();
	}

        @Override
	public void setValue(String value) {
	    setLibName(value);
	}

        @Override
	public String getOption(MakeConfiguration conf) {
            CompilerSet cs = conf.getCompilerSet().getCompilerSet();
            if (cs != null) {
                String lib = getLibName();
                lib = CndPathUtilities.quoteIfNecessary(CppUtils.normalizeDriveLetter(cs, lib));
                return cs.getCompilerFlavor().getToolchainDescriptor().getLinker().getLibraryFlag() + lib;
            }
	    return ""; // NOI18N
	}

        @Override
	public boolean canEdit() {
	    return true;
	}

        @Override
	public LibItem clone() {
	    return new LibItem(getLibName());
	}
    }

    public static class LibFileItem extends LibraryItem implements Cloneable {
	private String path;

	public LibFileItem(String path) {
	    this.path = path;
	    setType(LIB_FILE_ITEM);
	}

        @Override
	public String getPath() {
	    return path;
	}

	public void setPath(String path) {
	    this.path = path;
	}

        @Override
	public String getToolTip() {
	    return NbBundle.getMessage(LibraryItem.class, "LibraryFileTxt", getPath()); // NOI18N
	}

        @Override
        public String getIconName() {
            if (getPath().endsWith(".so") || getPath().endsWith(".dll") || getPath().endsWith(".dylib")) { // NOI18N
                return "org/netbeans/modules/cnd/loaders/DllIcon.gif"; // NOI18N
            } else if (getPath().endsWith(".a")) { // NOI18N
                return "org/netbeans/modules/cnd/loaders/static_library.gif"; // NOI18N
            } else {
                return "org/netbeans/modules/cnd/loaders/unknown.gif"; // NOI18N
            }
        }

        @Override
	public String toString() {
	    return getPath();
	}

        @Override
	public void setValue(String value) {
	    setPath(value);
	}

        @Override
	public String getOption(MakeConfiguration conf) {
            String lpath = getPath();
            if (conf != null) {
                CompilerSet cs = conf.getCompilerSet().getCompilerSet();
                lpath = CndPathUtilities.quoteIfNecessary(CppUtils.normalizeDriveLetter(cs, lpath));
            }
	    return lpath;
	}

        @Override
	public boolean canEdit() {
	    return true;
	}

        @Override
	public LibFileItem clone() {
	    return new LibFileItem(getPath());
	}
    }

    public static class OptionItem extends LibraryItem implements Cloneable {
	private String libraryOption;

	public OptionItem(String libraryOption) {
	    this.libraryOption = libraryOption;
	    setType(OPTION_ITEM);
	}

	public String getLibraryOption() {
	    return libraryOption;
	}

	public void setLibraryOption(String libraryOption) {
	    this.libraryOption = libraryOption;
	}

        @Override
	public String getToolTip() {
	    return NbBundle.getMessage(LibraryItem.class, "LibraryOptionTxt", getLibraryOption()); // NOI18N
	}

        @Override
	public String getIconName() {
	    return "org/netbeans/modules/cnd/makeproject/ui/resources/general.gif"; // NOI18N
	}

        @Override
	public String toString() {
	    return getLibraryOption();
	}

        @Override
	public void setValue(String value) {
	    setLibraryOption(value);
	}

        @Override
	public String getOption(MakeConfiguration conf) {
	    return getLibraryOption();
	}

        @Override
	public boolean canEdit() {
	    return true;
	}

        @Override
	public OptionItem clone() {
	    return new OptionItem(getLibraryOption());
	}
    }
}
