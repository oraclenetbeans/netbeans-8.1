/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.php.dbgp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.modules.php.dbgp.breakpoints.BreakpointModel;
import org.netbeans.modules.php.dbgp.models.AbstractIDEBridge;
import org.netbeans.modules.php.dbgp.models.CallStackModel;
import org.netbeans.modules.php.dbgp.models.ThreadsModel;
import org.netbeans.modules.php.dbgp.models.VariablesModel;
import org.netbeans.modules.php.dbgp.models.WatchesModel;
import org.netbeans.modules.php.dbgp.packets.DbgpCommand;
import org.netbeans.modules.php.dbgp.packets.DbgpMessage;
import org.netbeans.modules.php.dbgp.packets.DbgpResponse;
import org.netbeans.modules.php.dbgp.packets.Error;
import org.netbeans.modules.php.dbgp.packets.InitMessage;
import org.netbeans.modules.php.dbgp.packets.Reason;
import org.netbeans.modules.php.dbgp.packets.StackGetCommand;
import org.netbeans.modules.php.dbgp.packets.Status;
import org.netbeans.modules.php.dbgp.packets.StopCommand;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * @author Radek Matous
 */
public class DebugSession extends SingleThread {
    private static final Logger LOGGER = Logger.getLogger(DebugSession.class.getName());
    private static final int SLEEP_TIME = 100;
    private static final AtomicInteger TRANSACTION_ID = new AtomicInteger(0);
    private final DebuggerOptions options;
    private final BackendLauncher backendLauncher;
    private final AtomicReference<Status> status;
    private Session session;
    private Socket sessionSocket;
    private final AtomicBoolean detachRequest;
    private final AtomicBoolean stopRequest;
    private Thread sessionThread;
    private final List<DbgpCommand> commands;
    private AtomicReference<SessionId> sessionId;
    private AtomicReference<DebuggerEngine> engine;
    private IDESessionBridge myBridge;
    private AtomicReference<String> myFileName;

    DebugSession(DebuggerOptions options, BackendLauncher backendLauncher) {
        commands = new LinkedList<>();
        this.detachRequest = new AtomicBoolean(false);
        this.stopRequest = new AtomicBoolean(false);
        this.sessionId = new AtomicReference<>();
        this.backendLauncher = backendLauncher;
        this.status = new AtomicReference<>();
        this.options = options;
    }

    public void startProcessing(Socket socket) {
        synchronized (getSync()) {
            try {
                Status stat = getStatus();
                detachRequest.set(true);
                if (stat != null) {
                    waitFinished();
                }
                this.sessionSocket = socket;
                FutureTask invokeLater = invokeLater();
                invokeLater.get();
            } catch (InterruptedException | ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void run() {
        preprocess();
        try {
            while (!detachRequest.get()) {
                try {
                    sendCommands();
                    receiveData();
                    sleepTillNewCommand();
                } catch (SocketException exc) {
                    log(exc);
                    detachRequest.set(true);
                    stop();
                } catch (IOException e) {
                    log(e);
                } catch (Throwable e) {
                    log(e, Level.SEVERE);
                }
            }
        } finally {
            postprocess();
        }
    }

    private void preprocess() {
        detachRequest.set(false);
        stopRequest.set(false);
        commands.clear();
        sessionId.set(null);
        myBridge = new IDESessionBridge();
        myFileName = new AtomicReference<>();
        engine = new AtomicReference<>();
        setSessionThread(Thread.currentThread());
    }

    private void postprocess() {
        try {
            getSocket().close();
        } catch (IOException e) {
            log(e);
        } finally {
            setSessionThread(null);
            IDESessionBridge bridge = getBridge();
            if (bridge != null) {
                bridge.destroy();
            }
        }
    }

    public void initConnection(InitMessage message) {
        setSessionFile(message.getFileUri());
        DebuggerEngine[] engines = DebuggerManager.getDebuggerManager().getDebuggerEngines();
        for (DebuggerEngine nextEngine : engines) {
            SessionId id = (SessionId) nextEngine.lookupFirst(null, SessionId.class);
            if (id != null && id.getId().equals(message.getSessionId())) {
                sessionId.set(id);
                id.initialize(message.getFileUri(), options.getPathMapping());
                engine.set(nextEngine);
            }
        }
        IDESessionBridge bridge = getBridge();
        if (bridge != null) {
            bridge.init();
        }
    }

    private void sendCommands() throws IOException {
        List<DbgpCommand> list;
        synchronized (commands) {
            list = new ArrayList<>(commands);
            commands.clear();
        }
        for (DbgpCommand command : list) {
            if (!detachRequest.get()) {
                command.send(getSocket().getOutputStream());
                if (command.wantAcknowledgment()) {
                    receiveData(command);
                }
            }
        }
    }

    public void sendCommandLater(DbgpCommand command) {
        synchronized (this) {
            /*
             *  Do not collect command before session is not initialized.
             *  So any command before Init message will not be sent.
             *  ( F.e. commands for getting watch values will be just ignored
             *  if they was requested before Init message ).
             */
            if (getSessionId() == null) {
                return;
            }
            if (getSessionThread() == null) {
                return;
            }
            addCommand(command);
        }
    }

    public DbgpResponse sendSynchronCommand(DbgpCommand command) {
        DbgpResponse retval = null;
        if (canSendSynchronCommand()) {
            try {
                command.send(getSocket().getOutputStream());
                if (command.wantAcknowledgment()) {
                    DbgpMessage message = receiveData(command);
                    if (message instanceof DbgpResponse) {
                        retval = (DbgpResponse) message;
                    }
                }
            } catch (SocketException e) {
                log(e);
            } catch (IOException e) {
                log(e);
            }
        }
        return retval;
    }

    private void receiveData() throws IOException {
        receiveData(null);
    }

    private DbgpMessage receiveData(DbgpCommand command) throws IOException {
        if (command != null && command.getCommand().equals(StopCommand.COMMAND)) {
            detachRequest.set(true);
        }
        if (command != null || getSocket().getInputStream().available() > 0) {
            DbgpMessage message;
            try {
                message = DbgpMessage.create(getSocket().getInputStream(), options.getProjectEncoding());
            } catch (SocketException ex) {
                LOGGER.log(Level.INFO, "COMMAND: " + command.toString() + "; TRANS_ID: " + command.getTransactionId() + "; WANT_ACK: " + command.wantAcknowledgment(), ex);
                throw ex;
            }
            handleMessage(command, message);
            return message;
        }
        return null;
    }

    @NbBundle.Messages({
        "# {0} - Error code",
        "# {1} - Error message",
        "XdebugError=Response from XDebug contains errors:\n\n Code: {0}\n Message: {1}"
    })
    private void handleMessage(DbgpCommand command, DbgpMessage message)
            throws IOException {
        if (message == null) {
            return;
        }

        if (command == null) {
            // this is case when we don't need achnowl-t
            message.process(this, null);
            return;
        }

        boolean awaited = false;
        if (message instanceof DbgpResponse) {
            DbgpResponse response = (DbgpResponse) message;
            Error error = response.getError();
            if (error != null) {
                String errorMessage = Bundle.XdebugError(error.getErrorCode(), error.getMessage());
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(errorMessage, NotifyDescriptor.WARNING_MESSAGE));
                LOGGER.log(Level.INFO, "PHP_XDEBUG_ERROR - code: {0}, message: {1}", new Object[]{error.getErrorCode(), error.getMessage()});
            }
            String id = response.getTransactionId();
            if (id.equals(command.getTransactionId())) {
                awaited = true;
                message.process(this, command);
            }
        }
        if (!awaited) {
            message.process(this, null);
            receiveData(command);
        }
    }

    private boolean canSendSynchronCommand() {
        Thread currentSessionThread = getSessionThread();
        if (currentSessionThread == null) {
            return false;
        }
        if (currentSessionThread != Thread.currentThread()) {
            printing146558(Thread.currentThread());
        }
        return true;
    }

    private void printing146558(Thread currentThread) {
        IllegalStateException illegalStateException = new IllegalStateException(
                "Method incorrect usage. It should be called in handler thread only. " + //NOI18N
                "Called from thread: " + currentThread.getName() // NOI18N
                );
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            illegalStateException.printStackTrace(new PrintStream(bos, false, Charset.defaultCharset().name()));
            LOGGER.log(Level.WARNING, bos.toString(Charset.defaultCharset().name()));
        } catch (UnsupportedEncodingException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }

    public void processStatus(Status status, Reason reason, DbgpCommand command) {
        setStatus(status);
        if (status.isBreak() && reason.isOK()) {
            processBreakStatus();
        } else if (status.isStopping()) {
            processStoppingStatus();
        } else if (status.isStopped()) {
            processStoppedStatus();
        }
    }

    public void stopSession() {
        sendStopCommand();
        stop();
    }

    private void stop() {
        if (!stopRequest.get()) {
            stopRequest.set(true);
            stopEngines();
            stopBackend();
        }
    }

    private void processBreakStatus() {
        sendCommandLater(new StackGetCommand(getTransactionId()));
        IDESessionBridge bridge = getBridge();
        if (bridge != null) {
            bridge.setSuspended(true);
            ThreadsModel threadsModel = bridge.getThreadsModel();
            if (threadsModel != null) {
                threadsModel.updateSession(this);
            }
        }
    }

    private void processStoppingStatus() {
        detachRequest.set(true);
        processStoppedStatus();
    }

    private void processStoppedStatus() {
        if (getOptions().isDebugForFirstPageOnly()) {
            stop();
        }
    }

    private void sendStopCommand() {
        final boolean isDetached = detachRequest.get();
        if (!isDetached) {
            Thread currentThread = Thread.currentThread();
            final StopCommand stopCommand = new StopCommand(getTransactionId());
            if (currentThread == getSessionThread()) {
                sendSynchronCommand(stopCommand);
            } else {
                sendCommandLater(stopCommand);
            }
        }
    }

    private void stopEngines() {
        SessionManager.stopEngines(session);
    }

    public String getTransactionId() {
        return TRANSACTION_ID.getAndIncrement() + "";
    }

    public SessionId getSessionId() {
        return sessionId.get();
    }

    public IDESessionBridge getBridge() {
        return myBridge;
    }

    public String getFileName() {
        return myFileName.get();
    }

    private void setSessionFile(String fileName) {
        myFileName.set(fileName);
    }

    private void sleepTillNewCommand() {
        try {
            // Wake up every 100 milliseconds and see if the debuggee has something to say.
            // The IDE side can interrupt the sleep to send new packets to the
            // debugger.
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException ie) {
            // OK, run the look again.
        }
    }

    private synchronized void setSessionThread(Thread thread) {
        sessionThread = thread;
    }

    private void warnUserInCaseOfSocketException() {
        NotifyDescriptor descriptor = new NotifyDescriptor(
                NbBundle.getMessage(DebugSession.class, "MSG_SocketError"),
                NbBundle.getMessage(DebugSession.class, "MSG_SocketErrorTitle"),
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.ERROR_MESSAGE,
                new Object[]{NotifyDescriptor.OK_OPTION},
                NotifyDescriptor.OK_OPTION);
        DialogDisplayer.getDefault().notifyLater(descriptor);
    }

    private void addCommand(DbgpCommand command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    private synchronized Thread getSessionThread() {
        return sessionThread;
    }

    private Socket getSocket() {
        return sessionSocket;
    }

    private void log(IOException e) {
        log(e, Level.SEVERE);
    }

    private void log(Throwable e, Level level) {
        LOGGER.log(level, null, e);
    }

    private void log(SocketException e) {
        log(e, Level.INFO);
        warnUserInCaseOfSocketException();
    }

    public DebuggerOptions getOptions() {
        return options;
    }

    void startBackend() {
        if (backendLauncher != null) {
            backendLauncher.launch();
        }
    }

    void stopBackend() {
        if (backendLauncher != null) {
            backendLauncher.stop();
        }
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status.get();
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Status status) {
        assert status != null;
        if (status == Status.BREAK) {
            assert getSession() != null;
            DebuggerManager.getDebuggerManager().setCurrentSession(getSession());
        }

        this.status.set(status);
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /*
     * This class is associated  with DebugSession but is intended for
     * cooperation with IDE UI.
     */
    public class IDESessionBridge extends AbstractIDEBridge {

        @Override
        protected DebuggerEngine getEngine() {
            return engine.get();
        }

        @Override
        protected DebugSession getDebugSession() {
            return DebugSession.this;
        }

        private void init() {
            hideAnnotations();
            setSuspended(false);
            ThreadsModel threadsModel = getThreadsModel();
            if (threadsModel != null) {
                threadsModel.update();
            }
        }

        private void destroy() {
            setSuspended(false);
            hideAnnotations();
            BreakpointModel breakpointModel = getBreakpointModel();
            if (breakpointModel != null) {
                breakpointModel.setCurrentStack(null, DebugSession.this);
            }
            CallStackModel callStackModel = getCallStackModel();
            if (callStackModel != null) {
                callStackModel.clearModel();
            }
            ThreadsModel threadsModel = getThreadsModel();
            if (threadsModel != null) {
                threadsModel.update();
            }
            VariablesModel variablesModel = getVariablesModel();
            if (variablesModel != null) {
                variablesModel.clearModel();
            }
            WatchesModel watchesModel = getWatchesModel();
            if (watchesModel != null) {
                watchesModel.clearModel();
            }
        }

    }

}
