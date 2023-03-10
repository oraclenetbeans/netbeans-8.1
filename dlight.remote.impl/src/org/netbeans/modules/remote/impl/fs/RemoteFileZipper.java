/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */

package org.netbeans.modules.remote.impl.fs;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.util.CommonTasksSupport;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.remote.impl.RemoteLogger;
import org.openide.util.RequestProcessor;

/**
 * @author vkvashin
 */
public class RemoteFileZipper {

    private final ExecutionEnvironment execEnv;
    private final RequestProcessor rp;
    
    private final Map<String, Worker> workers = new HashMap<>();
    private final Object workersLock = new Object();

    public RemoteFileZipper(ExecutionEnvironment execEnv) {
        this.execEnv = execEnv;
        // throughput is set to 1 to prevent making all channels busy for a long time
        rp = new RequestProcessor(getClass().getSimpleName() + ' ' + execEnv, 1); //NOI18N
    }

    public void schedule(File zipFile, File zipPartFile, String path, Collection<String> extensions) {
        synchronized (workersLock) {
            Worker worker = workers.get(path);
            if (worker == null) {
                worker = new Worker(zipFile, zipPartFile, path, extensions);
                workers.put(path, worker);
                rp.post(worker);
            } else {
                // TODO: consider whatto do
            }
        }
    }

    private class Worker implements Runnable {

        private final File zipFile;
        private final File zipPartFile;
        private final String path;
        private final Collection<String> extensions;

        public Worker(File zipFile, File zipPartFile, String path, Collection<String> extensions) {
            this.zipFile = zipFile;
            this.zipPartFile = zipPartFile;
            this.path = path;
            this.extensions = extensions;
        }

        @Override
        public void run() {
            if (!ConnectionManager.getInstance().isConnectedTo(execEnv)) {
                return;
            }
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName(RemoteFileZipper.class.getSimpleName() + 
                    ' ' + execEnv + ": zipping " + path); //NOI18N
            try {                
                zip();
            } finally {
                Thread.currentThread().setName(oldName);
                synchronized (workersLock) {
                    workers.remove(path);
                }
            }            
        }

        private void zip() {

            long time;
            
            //
            // Zip directory on remote host
            //
            time = System.currentTimeMillis();
            StringBuilder script = new StringBuilder("TZ=UTC; export TZ; F=`mktemp`; if [ $? -eq 0 ]; then echo ZIP=$F; rm -rf $F; "); //NOI18N
            boolean all;
            if (extensions == null || extensions.isEmpty()) {
                all = true;
            } else {
                String next = extensions.iterator().next();
                all = (next == null) || next.equals("*"); // NOI18N
            }
            
            if (all) {
                script.append("zip -rq $F ").append(path); // NOI18N
            } else {
                script.append("find ").append(path); // NOI18N
                boolean first = true;
                for (String ext : extensions) {
                    if (first) {
                        first = false;
                    } else {
                        script. append(" -o "); // NOI18N
                    }
                    script. append(" -name \"*.").append(ext).append("\""); // NOI18N
                }
                script.append(" | xargs zip -rq $F "); // NOI18N
            }            
            script.append("; echo RC=$?; fi"); //NOI18N
            ProcessUtils.ExitStatus res = ProcessUtils.executeInDir("/", execEnv, "sh", "-c", script.toString()); //NOI18N
            if (!res.isOK()) {
                RemoteLogger.info("Warmup: error zipping {0} at {1}: {2}", //NOI18N
                        path, execEnv, res.error);
                return;
            }
            RemoteLogger.fine("zipping {0} at {1} took {2}", //NOI18N
                    path, execEnv, System.currentTimeMillis() - time);

            // Output should be like the following:
            // ZIP=/tmp/tmp.xLDawcYe5M
            // RC=0
            String[] lines = res.output.split("\n"); // NOI18N
            if (lines.length < 2 || !lines[0].startsWith("ZIP=") || !lines[1].startsWith("RC=")) { // NOI18N
                RemoteLogger.info("Warmup: error zipping {0} at {1}: unexpected output: {2}",  //NOI18N
                        path, execEnv, res.output); 
                return;                
            }
            int rc;
            try {
                rc = Integer.parseInt(lines[1].substring(3));
            } catch (NumberFormatException ex) {
                RemoteLogger.info("Warmup: error zipping {0} at {1}: unexpected output: {2}", //NOI18N
                        path, execEnv, res.output);
                return;                                
            }
            
            if (rc != 0) {
                RemoteLogger.info("Warmup: error zipping {0} at {1}: {2}", path, execEnv, res.error); //NOI18N
                return;                
            }
            
            String remoteZipPath = lines[0].substring(4);
            
            try {
                //
                // Download zip from remote host
                //
                time = System.currentTimeMillis();
                zipPartFile.getParentFile().mkdirs();
                Future<Integer> task = CommonTasksSupport.downloadFile(remoteZipPath, execEnv,
                        zipPartFile.getAbsolutePath(), new PrintWriter(System.err));

                rc = -1;
                try {
                    rc = task.get();
                } catch (InterruptedException ex) {
                    // nothing
                } catch (ExecutionException ex) {
                    ex.printStackTrace(System.err);
                }

                if (rc != 0) {
                    if (rc != 0) {
                        RemoteLogger.info("Warmup: error downloading {0} at {1} to {2}, rc={3}", //NOI18N
                                remoteZipPath, execEnv, zipPartFile.getAbsolutePath(), rc);
                        return;
                    }
                }
                RemoteLogger.fine("uploading {0} at {1} to {2} took {3}", //NOI18N
                        path, execEnv, zipPartFile.getAbsolutePath(), System.currentTimeMillis() - time);
                
                if (!zipPartFile.renameTo(zipFile)) {
                    RemoteLogger.info("Warmup: error renaming {0} at {1}",  //NOI18N
                            zipPartFile.getAbsolutePath(), zipFile.getAbsolutePath());
                }
            } finally {
                // Remove temp. zip file from remote host
                time = System.currentTimeMillis();
                Future<Integer> task = CommonTasksSupport.rmFile(execEnv, remoteZipPath, new PrintWriter(System.err));
                rc = -1;
                try {
                    rc = task.get();
                } catch (InterruptedException ex) {
                    // nothing
                } catch (ExecutionException ex) {
                    ex.printStackTrace(System.err);
                }
                RemoteLogger.fine("removing {0} at {1} finished with rc={2} and took {3} ms", //NOI18N
                        path, execEnv, zipPartFile.getAbsolutePath(), rc, System.currentTimeMillis() - time);
            }
        }
    }
}
