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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.versioning.util;

import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

/**
 * Holds registered scans which can be delayed and thus not interfere with openning projects or indexing
 * @author ondra
 */
public final class DelayScanRegistry {
    private final WeakHashMap<RequestProcessor.Task, DelayedScan> registry;
    private static DelayScanRegistry instance;
    private static int MAX_WAITING_TIME = 180000; // wait max 3 mins
    private static int WAITING_PERIOD = 10000;
    private static final boolean BLOCK_INDEFINITELY = "true".equals(System.getProperty("versioning.delayscan.nolimit", "false")); //NOI18N

    public static synchronized DelayScanRegistry getInstance() {
        if (instance == null) {
            instance = new DelayScanRegistry();
        }
        return instance;
    }

    private DelayScanRegistry () {
        registry = new WeakHashMap<Task, DelayedScan>(5);
    }

    /**
     * Delays given task if neccessary - e.g. projects are currently openning - and reschedules the task if indexing is running
     * This method waits for projects to open and thus blocks the current thread.
     * @param task task to be delayed
     * @param logger 
     * @param logMessagePrefix
     * @return true if the task was rescheduled
     */
    public boolean isDelayed (RequestProcessor.Task task, Logger logger, String logMessagePrefix) {
        boolean rescheduled = false;
        DelayedScan scan = getRegisteredScan(task);
        Future<Project[]> projectOpenTask = OpenProjects.getDefault().openProjects();
        if (!projectOpenTask.isDone()) {
            try {
                projectOpenTask.get();
            } catch (Exception ex) {
                // not interested
            }
        }
        if (IndexingBridge.getInstance().isIndexingInProgress()
                && (BLOCK_INDEFINITELY || scan.waitingLoops * WAITING_PERIOD < MAX_WAITING_TIME)) {
            // do not steal disk from openning projects and indexing tasks
            Level level = ++scan.waitingLoops < 10 ? Level.FINE : Level.INFO;
            logger.log(level, "{0}: Scanning in progress, trying again in {1}ms", new Object[]{logMessagePrefix, WAITING_PERIOD}); //NOI18N
            task.schedule(WAITING_PERIOD); // try again later
            rescheduled = true;
        } else {
            scan.waitingLoops = 0;
        }
        return rescheduled;
    }

    private DelayedScan getRegisteredScan(Task task) {
        synchronized (registry) {
            DelayedScan scan = registry.get(task);
            if (scan == null) {
                registry.put(task, scan = new DelayedScan());
            }
            return scan;
        }
    }

    private static class DelayedScan {
        private int waitingLoops;
    }
}
