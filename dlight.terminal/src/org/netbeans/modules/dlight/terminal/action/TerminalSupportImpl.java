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
package org.netbeans.modules.dlight.terminal.action;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.terminalemulator.Term;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.HostInfo;
import org.netbeans.modules.nativeexecution.api.NativeProcess;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionDescriptor;
import org.netbeans.modules.nativeexecution.api.execution.NativeExecutionService;
import org.netbeans.modules.nativeexecution.api.pty.PtySupport;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager.CancellationException;
import org.netbeans.modules.nativeexecution.api.util.HostInfoUtils;
import org.netbeans.modules.terminal.api.IONotifier;
import org.netbeans.modules.terminal.api.IOTerm;
import org.netbeans.modules.terminal.api.IOVisibility;
import org.netbeans.modules.terminal.support.TerminalPinSupport;
import org.netbeans.modules.terminal.support.TerminalPinSupport.TerminalCreationDetails;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.windows.IOContainer;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 *
 * @author Vladimir Voskresensky
 */
public final class TerminalSupportImpl {

    private static final RequestProcessor RP = new RequestProcessor("Terminal Action RP", 100); // NOI18N

    private TerminalSupportImpl() {
    }

    public static Component getToolbarPresenter(Action action) {
        JButton button = new JButton(action);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setText(null);
        button.putClientProperty("hideActionText", Boolean.TRUE); // NOI18N
        Object icon = action.getValue(Action.SMALL_ICON);
        if (icon == null) {
            icon = ImageUtilities.loadImageIcon("org/netbeans/modules/dlight/terminal/action/local_term.png", false);// NOI18N
        }
        if (!(icon instanceof Icon)) {
            throw new IllegalStateException("No icon provided for " + action); // NOI18N
        }
        button.setDisabledIcon(ImageUtilities.createDisabledIcon((Icon) icon));
        return button;
    }
    
    
    /**
     * Creates new Terminal tab. Method must be called from UI Thread.
     * @param ioContainer parent tabbed container
     * @param io io will be reused if this value is not null, if null the new one will be created 
     * (<code>ioProvider.getIO(tabTitle, null, ioContainer)</code>)
     * @param tabTitle tab title
     * @param env execution environment
     * @param dir Terminal tries to cd into this dir when connected
     * @param silentMode produces output on errors if true
     * @param pwdFlag try to set title to 'user@host - ${PWD}' every time ${PWD} changes.
     */
    public static void openTerminalImpl(
            final IOContainer ioContainer,
            final String tabTitle,
            final ExecutionEnvironment env,
            final String dir,
            final boolean silentMode,
            final boolean pwdFlag,
            final long termId) {
        final IOProvider ioProvider = IOProvider.get("Terminal"); // NOI18N
        if (ioProvider != null) {
            final AtomicReference<InputOutput> ioRef = new AtomicReference<InputOutput>();
            // Create a tab in EDT right after we call the method, don't let this 
            // work to be done in RP in asynchronous manner. We need this to
            // save tab order 
            InputOutput io = ioProvider.getIO(tabTitle, null, ioContainer);
            ioRef.set(io);
            final AtomicBoolean destroyed = new AtomicBoolean(false);
            
            final Runnable runnable = new Runnable() {
                private final Runnable delegate = new Runnable() {
                    @Override
                    public void run() {
                        if (SwingUtilities.isEventDispatchThread()) {
                            ioContainer.requestActive();
                        } else {
                            doWork();
                        }
                    }
                };

                private final HyperlinkAdapter retryLink = new HyperlinkAdapter() {
                    @Override
                    public void outputLineAction(OutputEvent ev) {
                        RP.post(delegate);
                    }
                };

                @Override
                public void run() {
                    delegate.run();
                }

                private void doWork() {
                    boolean verbose = env.isRemote(); // can use silentMode instead
                    OutputWriter out = ioRef.get().getOut();
                    
                    long id = TerminalPinSupport.getDefault().createPinDetails(TerminalCreationDetails.create(IOTerm.term(ioRef.get()), termId, env.getDisplayName(), pwdFlag));
                    
                    if (!ConnectionManager.getInstance().isConnectedTo(env)) {
                        try {
                            if (verbose) {
                                out.println(NbBundle.getMessage(TerminalSupportImpl.class, "LOG_ConnectingTo", env.getDisplayName() ));
                            }
                            ConnectionManager.getInstance().connectTo(env);
                        } catch (IOException ex) {
                            if (!destroyed.get()) {
                                if (verbose) {
                                    try {
                                        out.print(NbBundle.getMessage(TerminalSupportImpl.class, "LOG_ConnectionFailed"));
                                        out.println(NbBundle.getMessage(TerminalSupportImpl.class, "LOG_Retry"), retryLink);
                                    } catch (IOException ignored) {
                                    }
                                }
                                String error = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                                String msg = NbBundle.getMessage(TerminalSupportImpl.class, "TerminalAction.FailedToStart.text", error); // NOI18N
                                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                            }
                            return;
                        } catch (CancellationException ex) {
                            if (verbose) {
                                try {
                                    out.print(NbBundle.getMessage(TerminalSupportImpl.class, "LOG_Canceled"));
                                    out.println(NbBundle.getMessage(TerminalSupportImpl.class, "LOG_Retry"), retryLink);
                                } catch (IOException ignored) {
                                }
                            }
                            return;
                        }
                    }

                    final HostInfo hostInfo;
                    try {
                        // There is still a chance of env being disconnected
                        // (exception supressed in FetchHostInfoTask.compute)
                        if (!ConnectionManager.getInstance().isConnectedTo(env)) {
                            return;
                        }

                        try {
                            if (dir != null && !HostInfoUtils.directoryExists(env, dir)) {
                                // Displaying this message always, not just for remote envs.
                                out.print(NbBundle.getMessage(TerminalSupportImpl.class, "LOG_DirNotExist", dir, env.getDisplayName()));
                                return;
                            }
                        } catch (ConnectException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (InterruptedException ex) {
                            Exceptions.printStackTrace(ex);
                        }

                        hostInfo = HostInfoUtils.getHostInfo(env);
                        boolean isSupported = PtySupport.isSupportedFor(env);
                        if (!isSupported) {
                            if (!silentMode) {
                                String message;

                                if (hostInfo.getOSFamily() == HostInfo.OSFamily.WINDOWS) {
                                    message = NbBundle.getMessage(TerminalSupportImpl.class, "LocalTerminalNotSupported.error.nocygwin"); // NOI18N
                                } else {
                                    message = NbBundle.getMessage(TerminalSupportImpl.class, "LocalTerminalNotSupported.error"); // NOI18N
                                }

                                NotifyDescriptor nd = new NotifyDescriptor.Message(message, NotifyDescriptor.INFORMATION_MESSAGE);
                                DialogDisplayer.getDefault().notify(nd);
                            }
                            return;
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        return;
                    } catch (CancellationException ex) {
                        Exceptions.printStackTrace(ex);
                        return;
                    }
                    
                    if (verbose) {
                        try {
                            // Erase "log" in case we successfully connected to host
                            out.reset();
                        } catch (IOException ex) {
                            // never thrown from TermOutputWriter
                        }
                    }

                    try {
                        final Term term = IOTerm.term(ioRef.get());
                        // TODO: this is a temporary solution.

                        // Right now xterm emulation is not fully supported. (NB7.4)
                        // Still it has a very desired functionality - is recognises
                        // \ESC]%d;%sBEL escape sequences.
                        // Although \ESC]0;%sBEL is not implemented yet and window title
                        // is not set, it, at least, can skip the whole %s.
                        // This makes command prompt look better when this sequence is used
                        // in PS1 (ex. cygwin set this by default).
                        //
                        term.setEmulation("xterm"); // NOI18N

                        NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(env);
                        // clear env modified by NB. Let it be initialized by started shell process
                        npb.getEnvironment().put("LD_LIBRARY_PATH", "");// NOI18N
                        npb.getEnvironment().put("DYLD_LIBRARY_PATH", "");// NOI18N

                        
                        
                        final TerminalPinSupport support = TerminalPinSupport.getDefault();
                        String envId = ExecutionEnvironmentFactory.toUniqueID(env);

                        npb.addNativeProcessListener(new NativeProcessListener(ioRef.get(), destroyed));

                        /*
                         * Was: echo -n \"\033]0;" + tabTitle + " `pwd`\007\""
                         * Why changed:
                         *  1. Flag "-n" is not always supported by different shells,
                         *     so use printf instead. (Actually, if "-n" is not supported,
                         *     almost always PROMPT_COMMAND won't be supported too. (ksh, for example).
                         *  2. Now we use "033]3" (op_cwd) instead of "033]0" (op_win_title) 
                         *     and let listeners decide what to do when cwd is changed.
                         *  3. Removed a useless `pwd` call because cd has already updated
                         *     $PWD and $OLDPWD variables.
                         */
                        if (pwdFlag) {
                            final String promptCommand = "printf \"\033]3;${PWD}\007\"";   // NOI18N
                            final String commandName = "PROMPT_COMMAND";                                    // NOI18N
                            String usrPrompt = npb.getEnvironment().get(commandName);
                            npb.getEnvironment().put(commandName,
                                    (usrPrompt == null)
                                            ? promptCommand
                                            : promptCommand + ';' + usrPrompt
                            );
                        }

                        String shell = hostInfo.getLoginShell();
                        if (dir != null) {
                            npb.setWorkingDirectory(dir);
                        }
//                            npb.setWorkingDirectory("${HOME}");
                        npb.setExecutable(shell);
                        if (shell.endsWith("bash") || shell.endsWith("bash.exe")) { // NOI18N
                            npb.setArguments("--login"); // NOI18N
                        }
                        
                        NativeExecutionDescriptor descr;
                        descr = new NativeExecutionDescriptor().controllable(true).frontWindow(true).inputVisible(true).inputOutput(ioRef.get());
                        descr.postExecution(new Runnable() {

                            @Override
                            public void run() {
                                ioRef.get().closeInputOutput();
                                support.close(term);
                            }
                        });
                        NativeExecutionService es = NativeExecutionService.newService(npb, descr, "Terminal Emulator"); // NOI18N
                        Future<Integer> result = es.run();
                        // ask terminal to become active
                        SwingUtilities.invokeLater(this);

                        try {
                            // if terminal can not be started then ExecutionException should be thrown
                            // wait one second to see if terminal can not be started. otherwise it's OK to exit by TimeOut

                            // IG: I've increased the timeout from 1 to 10 seconds.
                            // On slow hosts 1 sec was not enougth to get an error code from the pty
                            // No work is done after this call, so this change should be safe.
                            Integer rc = result.get(10, TimeUnit.SECONDS);
                            if (rc != 0) {
                                Logger.getLogger(TerminalSupportImpl.class.getName())
                                        .log(Level.INFO, "{0}{1}", new Object[]{NbBundle.getMessage(TerminalSupportImpl.class, "LOG_ReturnCode"), rc});
                            }
                        } catch (TimeoutException ex) {
                            // we should be there
                        } catch (InterruptedException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (ExecutionException ex) {
                            if (!destroyed.get()) {
                                String error = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                                String msg = NbBundle.getMessage(TerminalSupportImpl.class, "TerminalAction.FailedToStart.text", error); // NOI18N
                                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                            }
                        }
                    } catch (java.util.concurrent.CancellationException ex) { // VK: don't quite understand who can throw it?
                        Exceptions.printStackTrace(ex);
                        reportInIO(ioRef.get(), ex);
                    }
                }

                private void reportInIO(InputOutput io, Exception ex) {
                    if (io != null && ex != null) {
                        io.getErr().print(ex.getLocalizedMessage());
                    }
                }
            };
            RP.post(runnable);
        }
    }
    
    private final static class NativeProcessListener implements ChangeListener, PropertyChangeListener {

        private final AtomicReference<NativeProcess> processRef;
        private final AtomicBoolean destroyed;

        public NativeProcessListener(InputOutput io, AtomicBoolean destroyed) {
            assert destroyed != null;
            this.destroyed = destroyed;
            this.processRef = new AtomicReference<NativeProcess>();
            IONotifier.addPropertyChangeListener(io, WeakListeners.propertyChange(NativeProcessListener.this, io));
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            NativeProcess process = processRef.get();
            if (process == null && e.getSource() instanceof NativeProcess) {
                processRef.compareAndSet(null, (NativeProcess) e.getSource());
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (IOVisibility.PROP_VISIBILITY.equals(evt.getPropertyName()) && Boolean.FALSE.equals(evt.getNewValue())) {
                if (destroyed.compareAndSet(false, true)) {
                    // term is closing => destroy process
                    final NativeProcess proc = processRef.get();
                    if (proc != null) {
                        RP.submit(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    proc.destroy();
                                } catch (Throwable th) {
                                }
                            }
                        });
                    }
                }
            }
        }
    }
    
    private static class HyperlinkAdapter implements OutputListener{

        @Override
        public void outputLineSelected(OutputEvent ev) {
        }

        @Override
        public void outputLineAction(OutputEvent ev) {
        }

        @Override
        public void outputLineCleared(OutputEvent ev) {
        }
    }
}
