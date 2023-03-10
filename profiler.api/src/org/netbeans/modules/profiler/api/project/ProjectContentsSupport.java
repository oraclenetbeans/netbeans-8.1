/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api.project;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.modules.profiler.spi.project.ProjectContentsSupportProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Support for configuring profiling roots and instrumentation filter from a project.
 *
 * @author Jiri Sedlacek
 */
public final class ProjectContentsSupport {
    
    private static final ClientUtils.SourceCodeSelection[] EMPTY_SELECTION = new ClientUtils.SourceCodeSelection[0];
    private static ProjectContentsSupport DEFAULT;
    
    private final Collection<? extends ProjectContentsSupportProvider> providers;
    
    
    /**
     * Returns array of profiling roots for the defined context.
     * 
     * @param profiledClassFile profiled file or null for profiling the entire project
     * @param profileSubprojects true if profiling also project's subprojects, false for profiling just the project
     * @return array of profiling roots for the defined context
     */
    public ClientUtils.SourceCodeSelection[] getProfilingRoots(FileObject profiledClassFile,
                                                               boolean profileSubprojects) {
        if (providers == null) {
            return EMPTY_SELECTION;
        } else {
            Set<ClientUtils.SourceCodeSelection> allRoots = new HashSet<ClientUtils.SourceCodeSelection>();
            for (ProjectContentsSupportProvider provider : providers) {
                ClientUtils.SourceCodeSelection[] roots = provider.getProfilingRoots(profiledClassFile, profileSubprojects);
                if (roots != null && roots.length > 0) allRoots.addAll(Arrays.asList(roots));
            }
            return allRoots.toArray(new ClientUtils.SourceCodeSelection[allRoots.size()]);
        }
    }
    
    /**
     * Returns instrumentation filter for the defined context.
     * 
     * @param profileSubprojects true if profiling also project's subprojects, false for profiling just the project
     * @return instrumentation filter for the defined context
     */
    public String getInstrumentationFilter(boolean profileSubprojects) {
        if (providers == null) {
            return ""; // NOI18N
        } else {
            StringBuilder buffer = new StringBuilder();
            for( ProjectContentsSupportProvider provider : providers) {
                String filter = provider.getInstrumentationFilter(profileSubprojects);
                if (filter != null && !filter.isEmpty()) {
                    buffer.append(filter).append(" "); // NOI18N
                }
            }
            return buffer.toString().trim();
        }
    }
    
    /**
     * Resets the ProjectContentsSupport instance after submitting or cancelling the Select Profiling Task dialog.
     */
    public void reset() {
        if (providers != null)
            for (ProjectContentsSupportProvider provider : providers) 
                provider.reset();
    }
    
    
    private ProjectContentsSupport(Collection<? extends ProjectContentsSupportProvider> providers) {
        this.providers = providers;
    }
    
    private static synchronized ProjectContentsSupport defaultImpl() {
        if (DEFAULT == null)
            DEFAULT = new ProjectContentsSupport(null);
        return DEFAULT;
    }
    

    /**
     * Returns ProjectContentsSupport instance for the provided project.
     * 
     * @param project project
     * @return ProjectContentsSupport instance for the provided project
     */
    public static ProjectContentsSupport get(Lookup.Provider project) {
        Collection<? extends ProjectContentsSupportProvider> providers =
                project != null ? project.getLookup().lookupAll(ProjectContentsSupportProvider.class) : null;
        if (providers == null || providers.isEmpty()) return defaultImpl();
        else return new ProjectContentsSupport(providers);
    }
    
}
