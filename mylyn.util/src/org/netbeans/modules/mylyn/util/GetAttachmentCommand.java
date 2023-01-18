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

package org.netbeans.modules.mylyn.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 *
 * @author Tomas Stupka
 */
public class GetAttachmentCommand extends BugtrackingCommand {

    private final TaskAttribute ta;
    private final TaskRepository taskRepository;
    private final AbstractRepositoryConnector repositoryConnector;
    private final OutputStream os;
    private String stringValue;
    private final ITask task;

    public GetAttachmentCommand(AbstractRepositoryConnector repositoryConnector, 
            TaskRepository taskRepository,
            ITask task,
            TaskAttribute ta,
            OutputStream os) {
        this.ta = ta;
        this.task = task;
        this.taskRepository = taskRepository;
        this.repositoryConnector = repositoryConnector;
        this.os = os;
    }

    @Override
    public void execute() throws CoreException, IOException, MalformedURLException {
        Logger log = Logger.getLogger(this.getClass().getName());
        if(log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "executing GetTaskDataCommand for attachment id: {0}", ta.getValue()); // NOI18N
        }
        AbstractTaskAttachmentHandler taskAttachmentHandler = repositoryConnector.getTaskAttachmentHandler();
        if (!taskAttachmentHandler.canGetContent(taskRepository, task)) {
            throw new IOException("Cannot get content for attachment with id: " + ta.getValue());
        }
        InputStream is = taskAttachmentHandler.getContent(taskRepository, task, ta, new NullProgressMonitor());
        try {
            byte [] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                os.write(buffer, 0, n);
            }
        } finally {
            is.close();
        }
    }

    @Override
    public String toString() {
        if (stringValue == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("GetAttachmentCommand [repository=");                     // NOI18N
            sb.append(taskRepository.getUrl());
            sb.append(",attachmentID=");                                        // NOI18N
            sb.append(ta.getValue());
            sb.append("]");                                                     // NOI18N
            stringValue = sb.toString();
        }
        return stringValue;
        
    }

}
