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

package org.netbeans.modules.cnd.makeproject.platform;

import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.makeproject.api.configurations.LibraryItem;

public abstract class Platform {
    
    private final String name;
    private final String displayName;
    private final int id;
    
    public Platform(String name, String displayName, int id) {
        this.name = name;
        this.displayName = displayName;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getId() {
        return id;
    }
    
    public abstract LibraryItem.StdLibItem[] getStandardLibraries();
    
    public String getLibraryName(String baseName) {
        return getLibraryNameWithoutExtension(baseName) + "." + getLibraryExtension(); // NOI18N
    }    
    
    public abstract String getLibraryNameWithoutExtension(String baseName);
    
    public abstract String getLibraryExtension();
       
    /**
     * File name that qmake would generate on current platform
     * given <code>TARGET=baseName</code> and <code>VERSION=version</code>.
     *
     * @param baseName
     * @param version
     * @return
     */
    public String getQtLibraryName(String baseName, String version) {
        return getLibraryName(baseName) + "." + version; // NOI18N
    }

    public abstract String getLibraryLinkOption(String libName, String libDir, String libPath, CompilerSet compilerSet);
    
    public LibraryItem.StdLibItem getStandardLibrarie(String name) {
        for (int i = 0; i < getStandardLibraries().length; i++) {
            if (getStandardLibraries()[i].getName().equals(name)) {
                return getStandardLibraries()[i];
            }
        }
        return null;
    }
}
