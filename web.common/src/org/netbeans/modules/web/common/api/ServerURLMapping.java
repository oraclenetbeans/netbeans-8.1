/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package org.netbeans.modules.web.common.api;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URL;
import org.netbeans.api.project.Project;
import org.netbeans.modules.web.common.spi.ServerURLMappingImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;
import org.openide.util.Utilities;

/**
 * Provides mapping between project's source file and its location on server
 * and vice versa. If project has not deployed files to a server the file:///
 * URL will be returned instead.
 * 
 * A single source file can have different URL when project is Run or Tested.
 * For example JS-Test-Driver used for tests execution has its own server and 
 * deploys sources and tests into this server. Resulting server URL is therefore
 * different based on context in which the source is being used. Clients
 * of this API should use appropriate CONTEXT_PROJECT_* constants.
 */
public final class ServerURLMapping {

    /**
     * Constant to indicate that project's source file to server URL mapping should be
     * evaluated in the context of "Run Project" action.
     */
    public static final int CONTEXT_PROJECT_SOURCES = 1;
    
    /**
     * Constant to indicate that project's source file to server URL mapping should be
     * evaluated in the context of "Test Project" action.
     */
    public static final int CONTEXT_PROJECT_TESTS = 2;
    
    private ServerURLMapping() {
    }
    
    /**
     * Convert given project's file into server URL.
     * @return could return null if file is not deployed to server and therefore
     *   not accessible
     */
    public static URL toServer(Project p, FileObject projectFile) {
        return toServer(p, CONTEXT_PROJECT_SOURCES, projectFile);
    }
    
    /**
     * Convert given project's file into server URL.
     * @param projectContext see {@link #CONTEXT_PROJECT_SOURCES} and {@link #CONTEXT_PROJECT_TESTS}
     * @return could return null if file is not deployed to server and therefore
     *   not accessible
     */
    public static URL toServer(Project p, int projectContext, FileObject projectFile) {
        Parameters.notNull("project", p); //NOI18N
        Parameters.notNull("projectFile", projectFile); //NOI18N
        
        ServerURLMappingImplementation impl = p.getLookup().lookup(ServerURLMappingImplementation.class);
        if (impl != null) {
            URL u = impl.toServer(projectContext, projectFile);
            if (u != null) {
                return u;
            }
        }
        try {
            URL url = projectFile.toURL();
            String urlString = url.toURI().toString();
            String urlString2 = urlString.replaceAll("file:/", "file:///"); //NOI18N
            if (!urlString.equals(urlString2)) {
                url = new URL(urlString2);
            }
            return url;
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
    /**
     * Convert given server URL into project's file.
     * @return returns null if nothing is known about this server URL
     */
    public static FileObject fromServer(Project p, URL serverURL) {
        return fromServer(p, CONTEXT_PROJECT_SOURCES, serverURL);
    }
    
    /**
     * Convert given server URL into project's file.
     * @param projectContext see {@link #CONTEXT_PROJECT_SOURCES} and {@link #CONTEXT_PROJECT_TESTS};
     *   it is very unlikely that server URL could be translated into two different sources
     *   but for API parity with toServer the context param is available here as well
     * @return returns null if nothing is known about this server URL
     */
    public static FileObject fromServer(Project p, int projectContext, URL serverURL) {
        Parameters.notNull("project", p); //NOI18N
        Parameters.notNull("serverURL", serverURL); //NOI18N
        ServerURLMappingImplementation impl = p.getLookup().lookup(ServerURLMappingImplementation.class);
        if (impl != null) {
            FileObject fo = impl.fromServer(projectContext, serverURL);
            if (fo != null) {
                return fo;
            }
        }
        if ("file".equals(serverURL.getProtocol())) { //NOI18N
            try {
                URI serverURI = serverURL.toURI();
                if (serverURI.getQuery() != null || serverURI.getFragment() != null) {
                    // #236532 - strip down query part from the URL:
                    serverURI = WebUtils.stringToUrl(WebUtils.urlToString(serverURL, true)).toURI();
                }
                File f = FileUtil.normalizeFile(Utilities.toFile(serverURI));
                return FileUtil.toFileObject(f);
                //FileObject fo = URLMapper.findFileObject(serverURL);
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }
}
