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
package org.netbeans.spi.project.ui.support;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.project.ui.convertor.ProjectConvertorFactory;
import org.netbeans.spi.project.ui.ProjectConvertor;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * Support for {@link ProjectConvertor}s.
 * @author Tomas Zezula
 * @since 1.80
 */
public final class ProjectConvertors {
    private ProjectConvertors() {
        throw new IllegalStateException("No instance allowed"); //NOI18N
    }

    /**
     * Checks if the given {@link Project} is a artificial one created by a {@link ProjectConvertor}.
     * @param project the {@link Project} to be tested
     * @return true if the {@link Project} was created by a {@link ProjectConvertor}
     */
    public static boolean isConvertorProject(@NonNull final Project project) {
        return ProjectConvertorFactory.isConvertorProject(project);
    }

    /**
     * Unregisters the artifical convertor {@link Project} from {@link ProjectManager}.
     * Unregisters the artifical convertor {@link Project} from {@link ProjectManager} to
     * allow another {@link Project} to take folder ownership. The method should be called
     * only by {@link Project} generators when creating a new {@link Project}.
     * Requires {@link ProjectManager#mutex()} write access.
     * @param project the project to be unregistered.
     * @since 1.81
     */
    public static void unregisterConvertorProject(@NonNull final Project project) {
        ProjectConvertorFactory.unregisterConvertorProject(project);
    }

    /**
     * Finds the owning non convertor project.
     * Finds nearest enclosing non convertor project.
     * @param file the {@link FileObject} to find owner for
     * @return the owning {@link Project} or null if there is no such a project.
     * @since 1.82
     */
    @CheckForNull
    @SuppressWarnings("NestedAssignment")
    public static Project getNonConvertorOwner(@NonNull final FileObject file) {
        for (FileObject parent = file.getParent(); parent != null; parent = parent.getParent()) {
            final Project prj = FileOwnerQuery.getOwner(parent);
            if (prj != null && !isConvertorProject(prj)) {
                return prj;
            }
        }
        return null;
    }

    /**
     * Creates {@link FileEncodingQueryImplementation} delegating to the nearest non convertor project.
     * @return the {@link FileEncodingQueryImplementation}
     * @since 1.82
     */
    @NonNull
    public static FileEncodingQueryImplementation createFileEncodingQuery() {
        return new ConvertorFileEncodingQuery();
    }

    /**
     * Creates a {@link Lookup} with given instances.
     * The returned {@link Lookup} implements {@link Closeable}, calling {@link Closeable#close}
     * on it removes all the instances.
     * <p class="nonnormative">
     * Typical usage would be to pass the {@link Lookup} to {@link ProjectConvertor.Result#Result}
     * and call {@link Closeable#close} on it in the convertor's project factory before the real
     * project is created.
     * </p>
     * @param instances the {@link Lookup} content
     * @return the {@link Lookup} implementing {@link Closeable}
     * @since 1.82
     */
    @NonNull
    public static Lookup createProjectConvertorLookup(@NonNull final Object... instances) {
        return new CloseableLookup(instances);
    }

    private static final class ConvertorFileEncodingQuery extends FileEncodingQueryImplementation {

        ConvertorFileEncodingQuery() {}

        @Override
        @CheckForNull
        public Charset getEncoding(@NonNull final FileObject file) {
            final Project p = getNonConvertorOwner(file);
            return p != null ?
                p.getLookup().lookup(FileEncodingQueryImplementation.class).getEncoding(file) :
                null;
        }
    }

    private static final class CloseableLookup extends ProxyLookup implements Closeable {

        CloseableLookup(Object... instances) {
            setLookups(Lookups.fixed(instances));
        }

        @Override
        public void close() throws IOException {
            setLookups(Lookup.EMPTY);
        }
    }

}
