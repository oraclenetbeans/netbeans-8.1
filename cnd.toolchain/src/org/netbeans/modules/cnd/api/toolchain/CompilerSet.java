/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.api.toolchain;

import java.nio.charset.Charset;
import java.util.List;
import org.netbeans.modules.cnd.toolchain.compilerset.CompilerSetImpl;

/**
 *
 * @author Alexander Simon
 */
public abstract class CompilerSet {
    public static final String None = "None"; // NOI18N
    public static final String UNKNOWN = "Unknown"; // NOI18N

    /**
     * Get the first tool of its kind.
     *
     * @param kind The tool kind to get
     * @return The Tool or null
     */
    public abstract Tool findTool(ToolKind kind);

    /**
     *
     * @return The compiler flavor
     */
    public abstract CompilerFlavor getCompilerFlavor();

    /**
     *
     * @return The path to binaries of compilers
     */
    public abstract String getDirectory();

    /**
     *
     * @return The path to folder with shell commands of MinGW tool collections.
     */
    public abstract String getCommandFolder();

    /**
     *
     * @return The full name of compiler set
     */
    public abstract String getDisplayName();

    /**
     *
     * @return The name of compiler set
     */
    public abstract String getName();

    /**
     * Get the first tool of its kind.
     *
     * @param kind The tool kind to get
     * @return The Tool or null
     */
    public abstract Tool getTool(ToolKind kind);

    /**
     *
     * @return collection of tools
     */
    public abstract List<Tool> getTools();

    /**
     *
     * @return false if tool collection explicitly created by user in the Build Tools Options
     */
    public abstract boolean isAutoGenerated();

    /**
     *
     * @return true if tool collection is represented as reference on update center and need to be installed
     */
    public abstract boolean isUrlPointer();

    /**
     *
     * @return tool collection encoding
     */
    public abstract Charset getEncoding();

    protected CompilerSet() {
        if (!getClass().equals(CompilerSetImpl.class)) {
            throw new UnsupportedOperationException("this class can not be overriden by clients"); // NOI18N
        }
    }
}
