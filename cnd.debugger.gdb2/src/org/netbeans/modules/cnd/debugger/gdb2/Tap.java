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
package org.netbeans.modules.cnd.debugger.gdb2;

import java.util.LinkedList;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import org.netbeans.modules.cnd.debugger.common2.debugger.NativeDebuggerManager;
import org.netbeans.modules.cnd.debugger.gdb2.mi.MICommandInjector;
import org.netbeans.modules.cnd.debugger.gdb2.mi.MIProxy;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 * Tap into the io between gdb and Term. - It echoes stuff it gets from gdb to
 * the Term while accumulating lines and sending them on to the MI processor via
 * MIProxy.processLine(). In this sense it works a bit like the unix 'tee(1)'
 * command. - It passes on stuff typed by the user on to gdb. - It allows
 * commands to be sent to gdb programmatically via MICommandInjector.inject() -
 * It allows informative message to be printed via MICommandInjector.log().
 *
 * It also colorizes lines. - Injected commands destined to gdb is in bold
 * black. - Stuff typed by user destined to gdb is in bold blue. - Error and
 * informative message from the ide (MIInjector.log) are printed in blue but not
 * forwarded to gdb. - gdb console stream ouptut (~) is echoed in green. - gdb
 * errors are echoed in red.
 *
 * Modelled after org.netbeans.lib.terminalemulator.LineDiscipline "put" is from
 * process to console. "send" is from "keyboard" to process.
 */
/*package*/ class Tap extends org.netbeans.lib.terminalemulator.TermStream implements MICommandInjector {

    // characters from gdb accumulate here and are forwarded to the tap
    private final StringBuilder interceptBuffer = new StringBuilder();
    private final LinkedList<String> interceptedLines = new LinkedList<String>();

    private final StringBuilder toTermBuf = new StringBuilder();
    private MIProxy miProxy;
    private GdbDebuggerImpl debugger;
    private boolean prompted = false;

    /*package*/ Tap() {
    }

    @Override
    public void flush() {
        toDTE.flush();
    }

    /**
     * Put character from gdb to console.
     *
     * @param c
     */
    @Override
    public void putChar(char c) {
        processCharFromGdb(c);
        dispatchInterceptedLines();
    }

    /**
     * Put characters from gdb to console.
     *
     * @param buf
     * @param offset
     * @param count
     */
    @Override
    public void putChars(char[] buf, int offset, int count) {
        CndUtils.assertUiThread();
        for (int bx = 0; bx < count; bx++) {
            processCharFromGdb(buf[offset + bx]);
        }
        dispatchInterceptedLines();
    }

    /**
     * Send character typed into console to gdb
     *
     * @param c
     */
    @Override
    public void sendChar(char c) {
        CndUtils.assertTrueInConsole(false, "should not be used; KeyProcessingStream should send only lines");
        toDCE.sendChar(c);
    }

    private boolean showMessage = true;
    /**
     * Send character typed into console to gdb
     *
     * @param c
     * @param offset
     * @param count
     */
    @Override
    public void sendChars(char c[], int offset, int count) {
        if (showMessage && !debugger.getGdbVersionPeculiarity().supports(GdbVersionPeculiarity.Feature.BREAKPOINT_NOTIFICATIONS)) {
            // IDE is unable to detect that something should be updated without a notification
            NativeDebuggerManager.warning(Catalog.get("MSG_OldGdbVersionConsole")); // NOI18N
            showMessage = false;
        }
        prompted = false;

        final String line = String.valueOf(c, offset, count);
        CndUtils.assertTrueInConsole(line.length() == 0 || line.endsWith("\n"), "KeyProcessingStream should send only lines");
        String cmd = line.trim();
        if (debugger != null) {
            debugger.sendTerminalTypedCommand(cmd);
        } else {
            // as fallback while debugger is not yet initialized
            toDCE.sendChars(c, offset, count);
            toDCE.flush();
        }
    }

    /*package*/ void setMiProxy(MIProxy miProxy) {
        this.miProxy = miProxy;
    }

    /*package*/ void setDebugger(GdbDebuggerImpl debugger) {
        this.debugger = debugger;
    }

    private final RequestProcessor sendQueue = new RequestProcessor("GDB send queue", 1); // NOI18N
    private static final boolean TRACING_IN_CONSOLE = CndUtils.getBoolean("cnd.gdb.trace.console", false); // NOI18N

    // interface MICommandInjector
    @Override
    public void inject(String cmd) {
        final char[] cmda = cmd.toCharArray();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // echo
                if (TRACING_IN_CONSOLE) {
                    toDTE.putChars(KeyProcessing.ESCAPES.BOLD_SEQUENCE, 0, KeyProcessing.ESCAPES.BOLD_SEQUENCE.length);
                    toDTE.putChars(cmda, 0, cmda.length);
                    toDTE.putChar(KeyProcessing.ESCAPES.CHAR_CR);			// tack on a CR
                    toDTE.putChars(KeyProcessing.ESCAPES.RESET_SEQUENCE, 0, KeyProcessing.ESCAPES.RESET_SEQUENCE.length);
                    toDTE.flush();
                }

                // send to gdb
                sendQueue.post(new Runnable() {
                    @Override
                    public void run() {
                        toDCE.sendChars(cmda, 0, cmda.length);
                    }
                });
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /*package*/ void printError(String errMsg) {
        final char[] cmda = errMsg.toCharArray();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // echo
                toDTE.putChars(KeyProcessing.ESCAPES.RED_SEQUENCE, 0, KeyProcessing.ESCAPES.RED_SEQUENCE.length);
                toDTE.putChars(cmda, 0, cmda.length);
                toDTE.putChar(KeyProcessing.ESCAPES.CHAR_CR);			// tack on a CR
                toDTE.putChar(KeyProcessing.ESCAPES.CHAR_LF);			// tack on a CR
                toDTE.putChars(KeyProcessing.ESCAPES.RESET_SEQUENCE, 0, KeyProcessing.ESCAPES.RESET_SEQUENCE.length);
                toDTE.flush();
            }
        });
    }

    // interface MICommandInjector
    @Override
    public void log(String cmd) {
        final char[] cmda = cmd.toCharArray();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // echo
                toDTE.putChars(KeyProcessing.ESCAPES.LOG_SEQUENCE, 0, KeyProcessing.ESCAPES.LOG_SEQUENCE.length);
                toDTE.putChars(cmda, 0, cmda.length);
                // toDTE.putChar(char_CR);			// tack on a CR
                toDTE.putChars(KeyProcessing.ESCAPES.RESET_SEQUENCE, 0, KeyProcessing.ESCAPES.RESET_SEQUENCE.length);
                if (prompted) {
                    toDTE.putChars(PROMPT.toCharArray(), 0, PROMPT.length());
                    toDTE.putChar(KeyProcessing.ESCAPES.CHAR_CR);			// tack on a CR
                    toDTE.putChar(KeyProcessing.ESCAPES.CHAR_LF);
                }
                toDTE.flush();
            }
        });

        // don't send to gdb
    }

    /**
     * Process character from gdb to console.
     */
    private void processCharFromGdb(char c) {
        toTermBuf.append(c);

        interceptBuffer.append(c);

        // detected EOL
        if (c == KeyProcessing.ESCAPES.CHAR_LF) {

            String line = interceptBuffer.toString();
            synchronized (interceptedLines) {
                interceptedLines.addLast(line);
            }
            interceptBuffer.delete(0, interceptBuffer.length());

            // Map NL to NLCR
            toTermBuf.append(KeyProcessing.ESCAPES.CHAR_CR);

            // do some pattern recognition and alternative colored output.
            if (line.startsWith("~")) { // NOI18N
                if (TRACING_IN_CONSOLE) {
                    // comment line
                    toTermBuf.insert(0, KeyProcessing.ESCAPES.GREEN_SEQUENCE);
                    toTermBuf.append(KeyProcessing.ESCAPES.RESET_SEQUENCE);
                } else {
                    toTermBuf.delete(0, toTermBuf.length());
                }
            } else if (line.startsWith("&") || line.startsWith("*") || line.startsWith("=")) { // NOI18N
                if (TRACING_IN_CONSOLE) {
                    // output
                    toTermBuf.insert(0, KeyProcessing.ESCAPES.BROWN_SEQUENCE);
                    toTermBuf.append(KeyProcessing.ESCAPES.RESET_SEQUENCE);
                } else {
                    toTermBuf.delete(0, toTermBuf.length());
                }
            } else {
                int caretx = line.indexOf('^');
                if (caretx != -1) {
                    if (TRACING_IN_CONSOLE) {
                        if (line.startsWith("^error,", caretx)) { // NOI18N
                            // error
                            toTermBuf.insert(0, KeyProcessing.ESCAPES.RED_SEQUENCE);
                            toTermBuf.append(KeyProcessing.ESCAPES.RESET_SEQUENCE);
                        }
                    } else {
                        toTermBuf.delete(0, toTermBuf.length());
                    }
                }
            }

            if (toTermBuf.length() > 0) {
                boolean sendFlag = true;
                if (toTermBuf.toString().trim().equals(PROMPT)) {
                    if (prompted) {
                        sendFlag = false;
                    } else {
                        prompted = true;
                    }
                } else {
                    prompted = false;
                }
                if (sendFlag) {
                    char chars[] = new char[toTermBuf.length()];
                    toTermBuf.getChars(0, toTermBuf.length(), chars, 0);
                    toDTE.putChars(chars, 0, toTermBuf.length());
                    toTermBuf.delete(0, toTermBuf.length());
                }
            }
        }
    }
    private static final String PROMPT = "(gdb)"; // NOI18N

    private final RequestProcessor processingQueue = new RequestProcessor("GDB output processing", 1); // NOI18N

    private void dispatchInterceptedLines() {
        synchronized (interceptedLines) {
            while (!interceptedLines.isEmpty()) {
                final String line = interceptedLines.removeFirst();

                processingQueue.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            miProxy.processLine(line);
                        } catch (Exception e) {
                            Exceptions.printStackTrace(new Exception("when processing line: " + line, e)); //NOI18N
                        }
                    }
                });
            }
        }
    }
}
