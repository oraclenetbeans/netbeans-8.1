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
package org.netbeans.modules.nativeexecution.api.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils.ExitStatus;
import org.netbeans.modules.nativeexecution.support.ShellSession;
import org.openide.util.Exceptions;

/**
 * Contains some information from stat structure
 *
 * @author Egor Ushakov
 */
@Deprecated
public final class Stat {

    private static final Logger LOG = org.netbeans.modules.nativeexecution.support.Logger.getInstance();
    private final long inode;
    private final long ctime;
    private static final HelperUtility statHelperUtility =
            new HelperUtility("bin/nativeexecution/$osname-${platform}$_isa/stat"); // NOI18N

    /**
     * Returns Stat structure
     *
     * @param filename - name of file
     * @param exEnv - environment where file is located
     * @return Stat structure, null if stat failed
     */
    public static Stat get(String filename, ExecutionEnvironment exEnv) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append(statHelperUtility.getPath(exEnv)).append('"'); // NOI18N
            sb.append(' ').append('"').append(filename).append('"'); // NOI18N
            ExitStatus res = ShellSession.execute(exEnv, sb.toString()); // NOI18N
            if (res.isOK()) {
                String[] data = res.output.split("\n"); // NOI18N
                if (data.length > 1) {
                    return new Stat(Long.parseLong(data[0].split(": ")[1].trim()), //NOI18N
                            Long.parseLong(data[1].split(": ")[1].trim())); //NOI18N
                }
            }

            LOG.log(Level.WARNING, "stat result for file {0} is incorrect: {1}", new Object[]{filename, res.toString()}); // NOI18N
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

        return null;
    }

    private Stat(long inode, long ctime) {
        this.inode = inode;
        this.ctime = ctime;
    }

    public long getCtime() {
        return ctime;
    }

    public long getInode() {
        return inode;
    }

    @Override
    public String toString() {
        return "inode=" + inode + ", " + "ctime=" + ctime; // NOI18N
    }
}
