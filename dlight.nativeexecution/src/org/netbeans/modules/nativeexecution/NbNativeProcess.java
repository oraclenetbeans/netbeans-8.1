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
package org.netbeans.modules.nativeexecution;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.netbeans.modules.nativeexecution.api.HostInfo;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.nativeexecution.api.ProcessStatusEx;
import org.netbeans.modules.nativeexecution.api.pty.Pty;
import org.netbeans.modules.nativeexecution.api.util.MacroMap;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils.ExitStatus;
import org.netbeans.modules.nativeexecution.api.util.Signal;
import org.netbeans.modules.nativeexecution.api.util.UnbufferSupport;
import org.netbeans.modules.nativeexecution.api.util.WindowsSupport;
import org.netbeans.modules.nativeexecution.pty.NbStartUtility;
import org.netbeans.modules.nativeexecution.signals.SignalSupport;
import org.netbeans.modules.nativeexecution.support.Logger;

/**
 *
 * An implementation of NativeProcess that uses NbStartUtility as a trampoline.
 *
 * Supported platforms: MacOSX
 *
 * @author Andrew
 */
public abstract class NbNativeProcess extends AbstractNativeProcess {

    private final String nbStartPath;
    private volatile ProcessStatusEx statusEx;

    public NbNativeProcess(final NativeProcessInfo info) {
        super(new NativeProcessInfo(info, true));
        String _nbStartPath = null;
        try {
            _nbStartPath = NbStartUtility.getInstance().getPath(getExecutionEnvironment());
        } catch (IOException ex) {
        } finally {
            nbStartPath = _nbStartPath;
        }
    }

    @Override
    protected final void create() throws Throwable {
        createProcessImpl(getCommand());
        readProcessInfo(getInputStream());
    }

    private List<String> getCommand() {
        List<String> command = new ArrayList<>();

        command.add(nbStartPath);

        String wdir = info.getWorkingDirectory(true);
        if (wdir != null && !wdir.isEmpty()) {
            command.add("--dir"); // NOI18N
            command.add(fixForWindows(wdir));
        }

        if (!info.isPtyMode()) {
            command.add("--no-pty"); // NOI18N
        } else {
            Pty pty = info.getPty();
            if (FIX_ERASE_KEY_IN_TERMINAL) {
                command.add("--set-erase-key"); // NOI18N;
            }
            if (pty != null) {
                command.add("-p"); // NOI18N
                command.add(pty.getSlaveName());
            }
        }

        if (!info.isPtyMode() && info.isUnbuffer()) {
            try {
                UnbufferSupport.initUnbuffer(info.getExecutionEnvironment(), info.getEnvironment());
            } catch (IOException ex) {
                Logger.getInstance().log(Level.FINE, "initUnbuffer failed", ex); // NOI18N
            }
        }

        if (info.getInitialSuspend()) {
            command.add("-w"); // NOI18N
        }

        boolean getStatus = info.isStatusEx();
        if (getStatus) {
            command.add("--report"); // NOI18N
            // hostInfo.getTempDir() is already in 'shell' format for Windows
            command.add(hostInfo.getTempDir() + "/status"); // NOI18N
        }

        String envFile = hostInfo.getEnvironmentFile();
        if (envFile != null) {
            // envFile is already in 'shell' format for Windows
            command.add("--readenv"); // NOI18N
            command.add(envFile);
        }

        if (info.isRedirectError()) {
            command.add("--redirect-error"); // NOI18N
        }

        MacroMap userEnv = info.getEnvironment();
        if (userEnv != null) {
            Map<String, String> userDefinedMap = userEnv.getUserDefinedMap();

            for (Map.Entry<String, String> entry : userDefinedMap.entrySet()) {
                if (isWindows() && entry.getKey().equalsIgnoreCase("PATH")) { // NOI18N
                    command.add("--env"); // NOI18N
                    command.add(entry.getKey() + "=" + WindowsSupport.getInstance().convertToAllShellPaths(entry.getValue())); // NOI18N
                    continue;
                }
                command.add("--env"); // NOI18N
                command.add(entry.getKey() + "=" + entry.getValue()); // NOI18N
            }
        }

        if (info.isCommandLineDefined()) {
            command.add(hostInfo.getShell());
            command.add("-c"); // NOI18N
            final String origCommand = info.getCommandLineForShell();
            command.add("exec " + origCommand); // NOI18N
        } else {
            command.add(fixForWindows(info.getExecutable()));
            command.addAll(info.getArguments());
        }
        return command;
    }

    private void readProcessInfo(InputStream fromProcessStream) throws IOException {
        String line;

        while (!(line = readLine(fromProcessStream).trim()).isEmpty()) {
            addProcessInfo(line);
        }

        String pidProperty = getProcessInfo("PID"); // NOI18N

        if (pidProperty == null) {
            InputStream error = getErrorStream();
            while (!(line = readLine(error).trim()).isEmpty()) {
                LOG.info(line);
            }
            throw new InternalError("Failed to get process PID"); // NOI18N
        }

        setPID(Integer.parseInt(pidProperty)); // NOI18N    
    }

    @Override
    protected final int waitResult() throws InterruptedException {
        int result = waitResultImpl();

        String reportFile = getProcessInfo("REPORT"); // NOI18N

        if (reportFile != null) {
            // NbNativeProcess works in either *nix or cygwin environment;
            // So it is safe to call /bin/sh here in any case
            NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(info.getExecutionEnvironment());
            npb.setExecutable("/bin/sh"); // NOI18N
            npb.setArguments("-c", "cat " + reportFile + " && rm " + reportFile); // NOI18N
            ExitStatus st = ProcessUtils.execute(npb);
            if (st.isOK()) {
                statusEx = ProcessStatusAccessor.getDefault().create(st.output.split("\n")); // NOI18N
                result = statusEx.getExitCode();
            }
        }

        return result;
    }

    @Override
    public ProcessStatusEx getExitStatusEx() {
        // Ensure that process is finished
        exitValue();
        return statusEx;
    }

    private String readLine(final InputStream is) throws IOException {
        int c;
        StringBuilder sb = new StringBuilder(20);

        while (!isInterrupted()) {
            c = is.read();

            if (c < 0 || c == '\n') {
                break;
            }

            sb.append((char) c);
        }

        return sb.toString().trim();
    }

    protected abstract int waitResultImpl() throws InterruptedException;

    protected abstract void createProcessImpl(List<String> command) throws Throwable;

    @Override
    protected int destroyImpl() {
        if (destroyed()) {
            return 0;
        }

        // signal using env
        String env = getProcessInfo("NBMAGIC"); // NOI18N
        if (env != null) {
            String magicEnv = "NBMAGIC=" + env; // NOI18N
            SignalSupport.signalProcessesByEnv(info.getExecutionEnvironment(), magicEnv, Signal.SIGTERM);
        }

        return 0;
    }

    protected boolean isWindows() {
        return HostInfo.OSFamily.WINDOWS.equals(hostInfo.getOSFamily());
    }

    protected String fixForWindows(String path) {
        return isWindows() ? WindowsSupport.getInstance().convertToCygwinPath(path) : path;
    }

    private boolean destroyed() {
        try {
            exitValue();
            return true;
        } catch (IllegalThreadStateException ex) {
            return false;
        }
    }
}
