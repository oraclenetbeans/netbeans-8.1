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
package org.netbeans.modules.nativeexecution.support.hostinfo.impl;

import com.jcraft.jsch.JSchException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.netbeans.modules.nativeexecution.ConnectionManagerAccessor;
import org.netbeans.modules.nativeexecution.JschSupport;
import org.netbeans.modules.nativeexecution.JschSupport.ChannelStreams;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.HostInfo;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.api.util.RemoteStatistics;
import org.netbeans.modules.nativeexecution.pty.NbStartUtility;
import org.netbeans.modules.nativeexecution.support.EnvReader;
import org.netbeans.modules.nativeexecution.support.InstalledFileLocatorProvider;
import org.netbeans.modules.nativeexecution.support.Logger;
import org.netbeans.modules.nativeexecution.support.MiscUtils;
import org.netbeans.modules.nativeexecution.support.hostinfo.HostInfoProvider;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = org.netbeans.modules.nativeexecution.support.hostinfo.HostInfoProvider.class, position = 100)
public class UnixHostInfoProvider implements HostInfoProvider {

    private static final String TMPBASE = System.getProperty("cnd.tmpbase", null); // NOI18N
    private static final String PATH_VAR = "PATH"; // NOI18N
    private static final String PATH_TO_PREPEND = System.getProperty("hostinfo.prepend.path", null); // NOI18N
    private static final String ERROR_MESSAGE_PREFIX = "Error: TMPDIRBASE is not writable: "; // NOI18N
    private static final java.util.logging.Logger log = Logger.getInstance();
    private static final File hostinfoScript;

    static {
        InstalledFileLocator fl = InstalledFileLocatorProvider.getDefault();
        hostinfoScript = fl.locate("bin/nativeexecution/hostinfo.sh", "org.netbeans.modules.dlight.nativeexecution", false); // NOI18N

        if (hostinfoScript == null) {
            log.severe("Unable to find hostinfo.sh script!"); // NOI18N
        }
    }

    @Override
    public HostInfo getHostInfo(ExecutionEnvironment execEnv) throws IOException, InterruptedException {
        if (hostinfoScript == null) {
            return null;
        }

        boolean isLocal = execEnv.isLocal();

        if (isLocal && Utilities.isWindows()) {
            return null;
        }

        final Properties info = execEnv.isLocal()
                ? getLocalHostInfo()
                : getRemoteHostInfo(execEnv);

        final Map<String, String> environment = new HashMap<>();

        HostInfo result = HostInfoFactory.newHostInfo(execEnv, info, environment);

        if (execEnv.isLocal()) {
            getLocalUserEnvironment(result, environment);
        } else {
            getRemoteUserEnvironment(execEnv, result, environment);
        }

        // Add /bin:/usr/bin
        String path = PATH_TO_PREPEND;

        if (path != null && !path.isEmpty()) {
            if (environment.containsKey(PATH_VAR)) {
                path += ":" + environment.get(PATH_VAR); // NOI18N
            }

            environment.put(PATH_VAR, path); // NOI18N
        }

        return result;
    }

    private Properties getLocalHostInfo() throws IOException {
        Properties hostInfo = new Properties();

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", // NOI18N
                    hostinfoScript.getAbsolutePath());

            String tmpDirBase = null;
            if (TMPBASE != null) {
                if (pathIsOK(TMPBASE, false)) {
                    tmpDirBase = TMPBASE;
                } else {
                    log.log(Level.WARNING, "Ignoring cnd.tmpbase property [{0}] as it contains illegal characters", TMPBASE); // NOI18N
                }
            }

            if (tmpDirBase == null) {
                File tmpDirFile = new File(System.getProperty("java.io.tmpdir")); // NOI18N
                tmpDirBase = tmpDirFile.getCanonicalPath();
            }


            pb.environment().put("TMPBASE", tmpDirBase); // NOI18N
            pb.environment().put("NB_KEY", HostInfoFactory.getNBKey()); // NOI18N

            Process hostinfoProcess = pb.start();

            // In case of some error goes to stderr, waitFor() will not exit
            // until error stream is read/closed.
            // So this case sould be handled.

            // We safely can do this in the same thread (in this exact case)

            List<String> errorLines = ProcessUtils.readProcessError(hostinfoProcess);
            int result = hostinfoProcess.waitFor();

            for (String errLine : errorLines) {
                log.log(Level.WARNING, "UnixHostInfoProvider: {0}", errLine); // NOI18N
                if (errLine.startsWith(ERROR_MESSAGE_PREFIX)) {
                    String title = NbBundle.getMessage(UnixHostInfoProvider.class, "TITLE_PermissionDenied");
                    String shortMsg = NbBundle.getMessage(UnixHostInfoProvider.class, "SHORTMSG_PermissionDenied", TMPBASE, "localhost");
                    String msg = NbBundle.getMessage(UnixHostInfoProvider.class, "MSG_PermissionDenied", TMPBASE, "localhost");
                    MiscUtils.showNotification(title, shortMsg, msg);
                }
            }

            if (result != 0) {
                throw new IOException(hostinfoScript + " rc == " + result); // NOI18N
            }

            fillProperties(hostInfo, hostinfoProcess.getInputStream());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("HostInfo receiving for localhost interrupted " + ex); // NOI18N
        }

        return hostInfo;
    }

    private Properties getRemoteHostInfo(ExecutionEnvironment execEnv) throws IOException, InterruptedException {
        Properties hostInfo = new Properties();
        ChannelStreams sh_channels = null;

        try {
            log.log(Level.FINEST, "Getting remote host info for {0}", execEnv); // NOI18N
            sh_channels = JschSupport.startCommand(execEnv, "/bin/sh -s", null); // NOI18N

            long localStartTime = System.currentTimeMillis();

            OutputStream out = sh_channels.in;
            InputStream err = sh_channels.err;
            InputStream in = sh_channels.out;

            // echannel.setEnv() didn't work, so writing this directly
            out.write(("NB_KEY=" + HostInfoFactory.getNBKey() + '\n').getBytes()); // NOI18N
            if (TMPBASE != null) {
                if (pathIsOK(TMPBASE, true)) {
                    out.write(("TMPBASE=" + TMPBASE + '\n').getBytes()); // NOI18N
                } else {
                    log.log(Level.WARNING, "Ignoring cnd.tmpbase property [{0}] as it contains illegal characters", TMPBASE); // NOI18N
                }
            }
            out.flush();

            BufferedReader scriptReader = new BufferedReader(new FileReader(hostinfoScript));
            String scriptLine = scriptReader.readLine();

            while (scriptLine != null) {
                out.write((scriptLine + '\n').getBytes());
                out.flush();
                scriptLine = scriptReader.readLine();
            }

            scriptReader.close();

            BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
            String errLine;
            while ((errLine = errReader.readLine()) != null) {
                log.log(Level.WARNING, "UnixHostInfoProvider: {0}", errLine); // NOI18N
                if (errLine.startsWith(ERROR_MESSAGE_PREFIX)) {
                    String title = NbBundle.getMessage(UnixHostInfoProvider.class, "TITLE_PermissionDenied");
                    String shortMsg = NbBundle.getMessage(UnixHostInfoProvider.class, "SHORTMSG_PermissionDenied", TMPBASE, execEnv);
                    String msg = NbBundle.getMessage(UnixHostInfoProvider.class, "MSG_PermissionDenied", TMPBASE, execEnv);
                    MiscUtils.showNotification(title, shortMsg, msg);
                }
            }

            fillProperties(hostInfo, in);

            long localEndTime = System.currentTimeMillis();

            hostInfo.put("LOCALTIME", Long.valueOf((localStartTime + localEndTime) / 2)); // NOI18N
        } catch (JSchException ex) {
            throw new IOException("Exception while receiving HostInfo for " + execEnv.toString() + ": " + ex); // NOI18N
        } finally {
            if (sh_channels != null) {
                if (sh_channels.channel != null) {
                    try {
                        ConnectionManagerAccessor.getDefault().closeAndReleaseChannel(execEnv, sh_channels.channel);
                    } catch (JSchException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }

        return hostInfo;
    }

    private void fillProperties(Properties hostInfo, InputStream inputStream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String s;
            while ((s = br.readLine()) != null) {
                String[] data = s.split("=", 2); // NOI18N
                if (data.length == 2) {
                    hostInfo.put(data[0], data[1]);
                }
            }
        } catch (IOException ex) {
        }
    }

    private void getRemoteUserEnvironment(ExecutionEnvironment execEnv, HostInfo hostInfo, Map<String, String> environmentToFill) throws InterruptedException {
        // If NbStartUtility is available for target host will invoke it for
        // dumping environment to a file ...
        // 
        // The only thing - we cannot use builders at this point, so
        // need to do everything here... 

        String nbstart = null;

        try {
            nbstart = NbStartUtility.getInstance().getPath(execEnv, hostInfo);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to get remote path of NbStartUtility", ex); // NOI18N
            Exceptions.printStackTrace(ex);
        }

        String envPath = hostInfo.getEnvironmentFile();

        ChannelStreams login_shell_channels = null;

        RemoteStatistics.ActivityID activityID = null;
        try {
            login_shell_channels = JschSupport.startLoginShellSession(execEnv);
            activityID = RemoteStatistics.startChannelActivity("UnixHostInfoProvider", execEnv.getDisplayName()); // NOI18N
            if (nbstart != null && envPath != null) {
                // dumping environment to file, later we'll restore it for each newly created remote process
                login_shell_channels.in.write((nbstart + " --dumpenv " + envPath + "\n").getBytes()); // NOI18N
            }
            // printing evnironment to stdout to fill host info map
            login_shell_channels.in.write(("/usr/bin/env || /bin/env\n").getBytes()); // NOI18N
            login_shell_channels.in.flush();
            login_shell_channels.in.close();

            EnvReader reader = new EnvReader(login_shell_channels.out, true);
            environmentToFill.putAll(reader.call());
        } catch (Exception ex) {
            InterruptedException iex = toInterruptedException(ex);
            if (iex != null) {
                throw iex;
            }
            log.log(Level.WARNING, "Failed to get getRemoteUserEnvironment for " + execEnv.getDisplayName(), ex); // NOI18N
        } finally {
            RemoteStatistics.stopChannelActivity(activityID);
            if (login_shell_channels != null) {
                if (login_shell_channels.channel != null) {
                    try {
                        ConnectionManagerAccessor.getDefault().closeAndReleaseChannel(execEnv, login_shell_channels.channel);
                    } catch (JSchException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }
    }

    InterruptedException toInterruptedException(Exception ex) {
        if (ex instanceof InterruptedException) {
            return (InterruptedException) ex;
        } else if (ex.getCause() instanceof InterruptedException) {
            return (InterruptedException) ex.getCause();
        }
        InterruptedIOException iioe = null;
        if (ex instanceof InterruptedIOException) {
            iioe = (InterruptedIOException) ex;
        } else if (ex.getCause() instanceof InterruptedIOException) {
            iioe = (InterruptedIOException) ex.getCause();
        }
        if (iioe != null) {
            InterruptedException wrapper = new InterruptedException(ex.getMessage());
            wrapper.initCause(iioe);
            return wrapper;
        }
        return null;
    }
    
    private void getLocalUserEnvironment(HostInfo hostInfo, Map<String, String> environmentToFill) {
        environmentToFill.putAll(System.getenv());
    }

    private boolean pathIsOK(String path, boolean remote) {
        for (char c : path.toCharArray()) {
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                continue;
            }
            if (c == '_' || c == '=') {
                continue;
            }
            if (c == '\\' || c == '/') {
                continue;
            }
            if (c == ':' && !remote && Utilities.isWindows()) {
                continue;
            }

            return false;
        }

        return true;
    }
}
