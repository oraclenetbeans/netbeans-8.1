/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */

package org.netbeans.libs.git.jgit.commands;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.netbeans.libs.git.GitException;
import org.netbeans.libs.git.GitSubmoduleStatus;
import org.netbeans.libs.git.jgit.GitClassFactory;
import org.netbeans.libs.git.jgit.Utils;
import org.netbeans.libs.git.progress.ProgressMonitor;

/**
 *
 * @author Ondrej Vrabec
 */
public class SubmoduleStatusCommand extends GitCommand {
    
    private final File[] roots;
    private final LinkedHashMap<File, GitSubmoduleStatus> statuses;

    public SubmoduleStatusCommand (Repository repository, GitClassFactory classFactory,
            File[] roots, ProgressMonitor monitor) {
        super(repository, classFactory, monitor);
        this.roots = roots;
        statuses = new LinkedHashMap<>();
    }

    @Override
    protected void run () throws GitException {
        Repository repository = getRepository();
        File workTree = repository.getWorkTree();
        org.eclipse.jgit.api.SubmoduleStatusCommand cmd = new Git(repository).submoduleStatus();
        for (String path : Utils.getRelativePaths(workTree, roots)) {
            cmd.addPath(path);
        }
        try {
            Map<String, SubmoduleStatus> result = cmd.call();
            GitClassFactory fac = getClassFactory();
            for (Map.Entry<String, SubmoduleStatus> e : result.entrySet()) {
                File root = new File(workTree, e.getKey());
                statuses.put(root, fac.createSubmoduleStatus(e.getValue(), root));
            }
        } catch (GitAPIException | JGitInternalException ex) {
            throw new GitException(ex);
        }
    }

    @Override
    protected String getCommandDescription () {
        StringBuilder sb = new StringBuilder("git submodule status"); //NOI18N
        for (File root : roots) {
            sb.append(" ").append(root.getAbsolutePath());
        }
        return sb.toString();
    }

    public Map<File, GitSubmoduleStatus> getStatuses () {
        return statuses;
    }
    
}
