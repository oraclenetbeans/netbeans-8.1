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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.api.remote.ServerRecord;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.makeproject.configurations.ui.CompilerSetNodeProp;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

public class CompilerSet2Configuration implements PropertyChangeListener, Cloneable {
    public static final String DEFAULT_CS = "default"; // NOI18N
    private DevelopmentHostConfiguration dhconf;
    private StringConfiguration compilerSetName;
    private CompilerSetNodeProp compilerSetNodeProp;
    private String flavor;
    private boolean dirty = false;
    private final Map<String, String> oldNameMap = new HashMap<>();
    
    private static final RequestProcessor RP = new RequestProcessor("CompilerSet2Configuration", 1); // NOI18N

    private CompilerSet2Configuration(CompilerSet2Configuration other) {
        this.dhconf = other.dhconf.clone();
        this.compilerSetName = other.compilerSetName.clone();
        this.flavor = other.flavor;
        this.compilerSetNodeProp = null;        
    }
    
    // Constructor for default tool collection
    public CompilerSet2Configuration(DevelopmentHostConfiguration dhconf) {
        this.dhconf = dhconf;
        compilerSetName = new StringConfiguration(null, DEFAULT_CS);
        flavor = null;
        compilerSetNodeProp = null;
    }

    // Constructor for tool collection
    public CompilerSet2Configuration(DevelopmentHostConfiguration dhconf, CompilerSet cs) {
        this.dhconf = dhconf;
        String csName = (cs == null) ? null : cs.getName();
        if (csName == null || csName.length() == 0) {
            if (getCompilerSetManager().getCompilerSets().size() > 0) {
                csName = getCompilerSetManager().getCompilerSets().get(0).getName();
            } else {
                if (Utilities.getOperatingSystem() == Utilities.OS_SOLARIS) {
                    csName = "Sun"; // NOI18N
                } else {
                    csName = "GNU"; // NOI18N
                }
            }
        }
        compilerSetName = new StringConfiguration(null, csName);
        flavor = null;
        compilerSetNodeProp = null;
    }

    // we can't store CSM because it's dependent on devHostConfig name which is not persistent
    public final CompilerSetManager getCompilerSetManager() {
        return CompilerSetManager.get(dhconf.getExecutionEnvironment());
    }

    // compilerSetName
    public StringConfiguration getCompilerSetName() {
        return compilerSetName;
    }

    public void setCompilerSetName(StringConfiguration compilerSetName) {
        this.compilerSetName = compilerSetName;
    }

    public void setCompilerSetNodeProp(CompilerSetNodeProp compilerSetNodeProp) {
        this.compilerSetNodeProp = compilerSetNodeProp;
    }

    // ----------------------------------------------------------------------------------------------------

    public void setValue(String name) {
        if (!getOption().equals(name)) {
            setValue(name, null);
        }
    }

    public void restore(String name, int version) {
        String nm;
        String fl;
        int index = name.indexOf('|'); // NOI18N
        if (index > 0) {
            nm = name.substring(0, index);
            fl = name.substring(index+1);
        } else {
            nm = name;
            if (DEFAULT_CS.equals(nm)) {
                fl = null;
            } else {
                fl = name;
            }
        }
        setValue(CompilerSet2Configuration.mapOldToNew(nm, version), CompilerSet2Configuration.mapOldToNew(fl, version));
    }

    private void setValue(String name, String flavor) {
        if (name == null || name.startsWith(CompilerSetNodeProp.DEFAULT_CS+" (")) { // NOI18N
            name = DEFAULT_CS;
        }
        getCompilerSetName().setValue(name);
        setFlavor(flavor);
    }

    /*
     * TODO: spread it out (Sergey)
     * Should this return csm.getCurrentCompilerSet()? (GRP)
     */
    public CompilerSet getCompilerSet() {
        String value = getCompilerSetName().getValue();
        if (DEFAULT_CS.equals(value)) {
            return getCompilerSetManager().getDefaultCompilerSet();
        }
        return getCompilerSetManager().getCompilerSet(value);
    }

    public boolean isDefaultCompilerSet() {
        return DEFAULT_CS.equals(getCompilerSetName().getValue());
    }

    public String getName() {
        return getDisplayName(false);
    }

    public String getDisplayName(boolean createIfNotFound) {
        CompilerSet compilerSet;
        String value = getCompilerSetName().getValue();
        boolean isDefault;
        if (DEFAULT_CS.equals(value)) {
            isDefault = true;
            compilerSet = getCompilerSetManager().getDefaultCompilerSet();
        } else {
            isDefault = false;
            compilerSet = getCompilerSetManager().getCompilerSet(value);
        }
        String displayName = null;

        if (compilerSet != null) {
            if (isDefault) {
                displayName = CompilerSetNodeProp.DEFAULT_CS+" ("+compilerSet.getName()+")"; //NOI18N
            } else {
                displayName = compilerSet.getName();
            }
        }
        if (displayName != null && dhconf.isConfigured()) {
            return displayName;
        } else {
            if (createIfNotFound) {
                return createNotFoundName(value);
            } else {
                return ""; // NOI18N
            }
        }
    }

    private String createNotFoundName(String name) {
        if (!dhconf.isConfigured()) {
            return "";
        } else {
            return name.equals(CompilerSet.None) ? name : NbBundle.getMessage(CompilerSet2Configuration.class,  "NOT_FOUND", name); // NOI18N
        }
    }

    public boolean isDevHostSetUp() {
        return dhconf.isConfigured();
    }

    // Clone and assign
    public void assign(CompilerSet2Configuration conf) {
        String oldName = getCompilerSetName().getValue();
        String newName = conf.getCompilerSetName().getValue();
        setDirty(newName != null && !newName.equals(oldName));
//        setMakeConfiguration(conf.getMakeConfiguration());
        setValue(conf.getCompilerSetName().getValue());
    }

    @Override
    public CompilerSet2Configuration clone() {
        CompilerSet2Configuration clone = new CompilerSet2Configuration(this);
        return clone;
    }

    public void setDevelopmentHostConfiguration(DevelopmentHostConfiguration dhconf) {
        this.dhconf = dhconf;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean getDirty() {
        return dirty;
    }

    /*
     * Backward compatibility with old CompilerSetConfiguration (for now)
     */
    public boolean isValid() {
        return getCompilerSet() != null;
    }

    public String getOption() {
        return getCompilerSetName().getValue();
    }

    public String getNameAndFlavor() {
        StringBuilder ret = new StringBuilder();
        ret.append(getOption());
        if (!DEFAULT_CS.equals(getOption())) {
            if (getFlavor() != null) {
                ret.append("|"); // NOI18N
                ret.append(getFlavor());
            }
        }
        return ret.toString();
    }

    public String getFlavor() {
        if (flavor == null) {
            CompilerSet cs = getCompilerSet();
            if (cs != null) {
                this.flavor = cs.getCompilerFlavor().getToolchainDescriptor().getName();
            }
        }
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        CompilerSet ocs;
        String hkey = ((DevelopmentHostConfiguration) evt.getNewValue()).getHostKey();
        final ExecutionEnvironment env = ExecutionEnvironmentFactory.fromUniqueID(hkey);
        final String oldName = oldNameMap.get(hkey);
        if (oldName != null) {
            ocs = CompilerSetManager.get(env).getCompilerSet(oldName);
        } else {
            ocs = CompilerSetManager.get(env).getDefaultCompilerSet();
        }
        if (ocs == null && !CompilerSetManager.get(env).getCompilerSets().isEmpty()) {
            ocs = CompilerSetManager.get(env).getCompilerSets().get(0);
        }
        if (ocs == null) {
            return;
        }

        String okey = (String) evt.getOldValue();
        oldNameMap.put(okey, getName());
        if (env.isLocal()) {
            setValue(ocs.getName());
        } else {
            setValue(ocs.getName());
            final CompilerSet focs = ocs;
            RP.post(new Runnable() {
                @Override
                public void run() {
                    ServerRecord record = ServerList.get(env);
                    if (record != null) {
                        // Not sure why we do this in an RP, but don't want to remove it this late in the release
                        setValue(focs.getName());
                        if (compilerSetNodeProp != null) {
                            compilerSetNodeProp.repaint();
                        }
                    }
                }
            });
        }
    }

    private static String mapOldToNew(String flavor, int version) {
        if (version <= 43) {
            if ("Sun".equals(flavor)) { // NOI18N
                return "SunStudio"; // NOI18N
            } else if ("SunExpress".equals(flavor)) { // NOI18N
                return "SunStudioExpress"; // NOI18N
            } else if ("Sun12".equals(flavor)) { // NOI18N
                return "SunStudio_12"; // NOI18N
            } else if ("Sun11".equals(flavor)) { // NOI18N
                return "SunStudio_11"; // NOI18N
            } else if ("Sun10".equals(flavor)) { // NOI18N
                return "SunStudio_10"; // NOI18N
            } else if ("Sun9".equals(flavor)) { // NOI18N
                return "SunStudio_9"; // NOI18N
            } else if ("Sun8".equals(flavor)) { // NOI18N
                return "SunStudio_8"; // NOI18N
            } else if ("DJGPP".equals(flavor)) { // NOI18N
                return "GNU"; // NOI18N
            } else if ("Interix".equals(flavor)) { // NOI18N
                return "GNU"; // NOI18N
            } else if (CompilerSet.UNKNOWN.equals(flavor)) {
                return "GNU"; // NOI18N
            }
        }
        return flavor;
    }
}
