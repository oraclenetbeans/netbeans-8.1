/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.apt.impl.support.clank;

import java.util.Collection;
import org.clang.basic.vfs.FileSystem;
import org.clang.tools.services.ClankCompilationDataBase;
import org.clang.tools.services.support.ClangFileSystemProvider;
import org.llvm.adt.IntrusiveRefCntPtr;
import org.netbeans.modules.cnd.apt.debug.APTTraceFlags;
import org.netbeans.modules.cnd.utils.cache.CharSequenceUtils;
import org.openide.util.Utilities;

/**
 *
 * @author vkvashin
 */
public class ClankFileSystemProviderImpl extends ClangFileSystemProvider {
    
    public static final String RFS_PREFIX = "rfs:"; //NOI18N

    
    @Override
    public IntrusiveRefCntPtr<FileSystem> getFileSystemImpl(ClankCompilationDataBase.Entry entry) {
        boolean useFS;
        if (APTTraceFlags.ALWAYS_USE_NB_FS || Utilities.isWindows()) {
            useFS = true;
        } else {
            if (isFullRemote(entry)) {
                useFS = true;
            } else if (isMixedOrSimpleRemote(entry)) {
                useFS = true;
            } else {
                useFS = false;
            }
        }
        return useFS ? new IntrusiveRefCntPtr<FileSystem>(ClankFileObjectBasedFileSystem.getInstance()) : null;
    }

    private boolean isMixedOrSimpleRemote(ClankCompilationDataBase.Entry entry) {
        for (CharSequence sysIncludePath : entry.getPredefinedSystemIncludePaths()) {
            if (CharSequenceUtils.startsWith(sysIncludePath, RFS_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFullRemote(ClankCompilationDataBase.Entry entry) {
        Collection<CharSequence> compiledFiles = entry.getCompiledFiles();
        if (compiledFiles != null && !compiledFiles.isEmpty()) {
            if (CharSequenceUtils.startsWith(compiledFiles.iterator().next(), RFS_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    public static String getPathFromUrl(String path) {
        if (CharSequenceUtils.startsWith(path, RFS_PREFIX)) {
            // examples:
            // rfs://user@host:22/usr/include
            // rfs:user@host:22/usr/include
            // rfs://user@host:22
            // rfs:user@host:22
            int pos = CharSequenceUtils.indexOf(path, ":", RFS_PREFIX.length()); //NOI18N
            if (pos > 0) {
                pos++;
                while (pos < path.length() && Character.isDigit(path.charAt(pos))) {
                    pos++;
                }
                return path.substring(pos, path.length());
            } else {
                throw new IllegalArgumentException("The path " + path + " starts with " + RFS_PREFIX + //NOI18N
                        " but does not contain a colon after it"); //NOI18N                
            }
        } else {
            return path;
        }
    }
}
