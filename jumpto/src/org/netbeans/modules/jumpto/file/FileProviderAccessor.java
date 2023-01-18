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

package org.netbeans.modules.jumpto.file;

import java.util.List;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.project.Project;
import org.netbeans.spi.jumpto.file.FileDescriptor;
import org.netbeans.spi.jumpto.file.FileProvider;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Tomas Zezula
 */
public abstract class FileProviderAccessor {

    private static volatile FileProviderAccessor instance;

    public static synchronized FileProviderAccessor getInstance() {
        if (instance == null) {
            try {
                Class.forName(FileProvider.Context.class.getName(), true, FileProviderAccessor.class.getClassLoader());
                assert instance != null;
            } catch (ClassNotFoundException cnf) {
                Exceptions.printStackTrace(cnf);
            }
        }
        assert instance != null;
        return instance;
    }

    public static void setInstance(final FileProviderAccessor theInstance) {
        assert theInstance != null;
        instance = theInstance;
    }

    public abstract FileProvider.Context createContext(
            @NonNull String text,
            @NonNull SearchType searchType,
            int lineNr,
            @NullAllowed Project currentProject);

    public abstract void setRoot(FileProvider.Context ctx, FileObject root);

    public abstract FileProvider.Result createResult(List<? super FileDescriptor> result, String[] message, FileProvider.Context ctx);

    public abstract int getRetry(FileProvider.Result result);

    public abstract void setFromCurrentProject(@NonNull FileDescriptor desc, boolean value);

    public abstract boolean isFromCurrentProject(@NonNull FileDescriptor desc);

    public abstract void setLineNumber(@NonNull FileDescriptor desc, int lineNo);
}
