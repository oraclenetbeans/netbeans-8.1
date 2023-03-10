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
package org.netbeans.modules.javaee.wildfly.ide;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.input.InputProcessor;
import org.netbeans.api.extexecution.input.InputProcessors;
import org.netbeans.api.extexecution.input.InputReader;
import org.netbeans.api.extexecution.input.InputReaderTask;
import org.netbeans.api.extexecution.input.InputReaders;
import org.netbeans.api.extexecution.input.LineProcessor;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.netbeans.modules.j2ee.deployment.profiler.api.ProfilerSupport;
import org.openide.windows.InputOutput;

/**
 *
 * @author Petr Hejl
 */
public final class WildflyOutputSupport {

    private static final Logger LOGGER = Logger.getLogger(WildflyOutputSupport.class.getName());

    private static final ExecutionDescriptor DESCRIPTOR = new ExecutionDescriptor().frontWindow(true).inputVisible(true);

    // TODO what will happen on server remove actually
    private static final Map<InstanceProperties, WildflyOutputSupport> INSTANCE_CACHE
            = new HashMap<InstanceProperties, WildflyOutputSupport>();

    private static final ExecutorService PROFILER_SERVICE = Executors.newCachedThreadPool();

    private static final ExecutorService LOG_FILE_SERVICE = Executors.newCachedThreadPool();

    private static final Pattern JBOSS_7_STARTED_ML = Pattern.compile(".*JBoss AS 7(\\..*)* \\d+ms .*");
    private static final Pattern WILDFLY_8_STARTED_ML = Pattern.compile(".*JBAS015874: WildFly 8(\\..*)* .* started in \\d+ms .*");
    private static final Pattern WILDFLY_8_STARTING_ML = Pattern.compile(".*JBAS015899: WildFly 8(\\..*)* .* starting");
    private static final Pattern WILDFLY_9_STARTED_ML = Pattern.compile(".*WFLYSRV0050: WildFly Full \\d+(\\..*)* .* started in \\d+ms .*");
    private static final Pattern WILDFLY_STARTING_ML = Pattern.compile(".*WFLYSRV0049: WildFly .* \\d+(\\..*)* .* starting");
    private static final Pattern WILDFLY_10_STARTED_ML = Pattern.compile(".*WFLYSRV0025: WildFly .* \\d+(\\..*)* .* started in \\d+ms .*");

    private static final Pattern EAP6_STARTED_ML = Pattern.compile(".*JBAS015874: JBoss EAP 6\\.[0-9]?.[0-9]?\\.GA .* \\d+ms .*");
    private static final Pattern EAP6_STARTING_ML = Pattern.compile(".*JBAS015899: JBoss EAP 6\\.[0-9]?.[0-9]?\\.GA .*");

    private final InstanceProperties props;

    /**
     * GuardedBy("this")
     */
    private boolean started;

    /**
     * GuardedBy("this")
     */
    private boolean failed;

    /**
     * GuardedBy("this")
     */
    private Future<Integer> processTask;

    /**
     * GuardedBy("this")
     */
    private Future<?> profileCheckTask;

    /**
     * GuardedBy("this")
     */
    private InputReaderTask fileTask;

    private WildflyOutputSupport(InstanceProperties props) {
        this.props = props;
    }

    public synchronized static WildflyOutputSupport getInstance(InstanceProperties props, boolean create) {
        WildflyOutputSupport instance = INSTANCE_CACHE.get(props);
        if (instance == null && create) {
            instance = new WildflyOutputSupport(props);
            INSTANCE_CACHE.put(props, instance);
        }
        return instance;
    }

    public void start(InputOutput io, final Process serverProcess, final boolean profiler) {
        reset();

        ExecutionDescriptor descriptor = DESCRIPTOR.inputOutput(io);
        descriptor = descriptor.outProcessorFactory(new ExecutionDescriptor.InputProcessorFactory() {

            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                return InputProcessors.proxy(defaultProcessor, InputProcessors.bridge(new StartLineProcessor(profiler)));
            }
        });
        descriptor = descriptor.errProcessorFactory(new ExecutionDescriptor.InputProcessorFactory() {

            @Override
            public InputProcessor newInputProcessor(InputProcessor defaultProcessor) {
                return InputProcessors.proxy(defaultProcessor, InputProcessors.bridge(new StartLineProcessor(profiler)));
            }
        });

        descriptor = descriptor.postExecution(new Runnable() {

            @Override
            public void run() {
                synchronized (WildflyOutputSupport.class) {
                    INSTANCE_CACHE.remove(WildflyOutputSupport.this.props);
                }

            }
        });

        ExecutionService service = ExecutionService.newService(new Callable<Process>() {

            @Override
            public Process call() throws Exception {
                return serverProcess;
            }
        }, descriptor, props.getProperty(InstanceProperties.DISPLAY_NAME_ATTR));
        Future<Integer> localProcessTask = service.run();

        synchronized (this) {
            if (profiler) {
                profileCheckTask = PROFILER_SERVICE.submit(new ProfilerCheckTask());
            }

            processTask = localProcessTask;
        }
        failed = !isAlive(serverProcess);
    }

    private boolean isAlive(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public void start(InputOutput io, final File file) {
        reset();

        InputReader reader = InputReaders.forFile(file, Charset.defaultCharset());
        InputReaderTask localFileTask = InputReaderTask.newTask(reader, InputProcessors.printing(io.getOut(), false));
        LOG_FILE_SERVICE.submit(localFileTask);
        synchronized (this) {
            fileTask = localFileTask;
        }

    }

    public void stop() {
        try {
            synchronized (this) {
                if (processTask != null) {
                    processTask.cancel(true);
                } else if (fileTask != null) {
                    fileTask.cancel();
                }
                if (profileCheckTask != null) {
                    profileCheckTask.cancel(true);
                }

                started = false;
                failed = false;
                processTask = null;
                profileCheckTask = null;
                fileTask = null;
            }
        } finally {
            synchronized (WildflyOutputSupport.class) {
                INSTANCE_CACHE.remove(WildflyOutputSupport.this.props);
            }
        }
    }

    public boolean waitForStart(long timeout) throws TimeoutException, InterruptedException {
        synchronized (this) {
            if (processTask == null) {
                // just defensive
                if (fileTask != null) {
                    return true;
                }
                return false;
            }

            while (!started && !failed) {
                wait(timeout);
            }
            if (started) {
                return true;
            } else if (failed) {
                return false;
            }

            // timeouted block
            if (profileCheckTask != null) {
                profileCheckTask.cancel(true);
            }
            throw new TimeoutException("Expired timeout " + timeout + " ms"); // NOI18N
        }
    }

    public void waitForStop(long timeout) throws TimeoutException, InterruptedException,
            ExecutionException {

        Future<Integer> localProcessTask;
        synchronized (this) {
            localProcessTask = processTask;
        }
        if (localProcessTask == null) {
            return;
        }
        localProcessTask.get(timeout, TimeUnit.MILLISECONDS);
    }

    private void reset() {
        synchronized (this) {
            if (fileTask != null) {
                fileTask.cancel();
            }

            if (started) {
                LOGGER.log(Level.INFO, "Instance {0} started again without proper stop",
                        props.getProperty(InstanceProperties.DISPLAY_NAME_ATTR));
            }
            started = false;
            failed = false;
            processTask = null;
            profileCheckTask = null;
            fileTask = null;
        }
    }

    private static boolean isProfilerReady() {
        int state = ProfilerSupport.getState();
        return state == ProfilerSupport.STATE_BLOCKING || state == ProfilerSupport.STATE_RUNNING
                || state == ProfilerSupport.STATE_PROFILING;
    }

    private static boolean isProfilerInactive() {
        return ProfilerSupport.getState() == ProfilerSupport.STATE_INACTIVE;
    }

    private class StartLineProcessor implements LineProcessor {

        private final boolean profiler;

        private boolean check = true;

        public StartLineProcessor(boolean profiler) {
            this.profiler = profiler;
        }

        @Override
        public void processLine(String line) {
            if (!check) {
                return;
            }
            synchronized (WildflyOutputSupport.this) {
                if (started) {
                    check = false;
                    return;
                }
            }

            if (profiler) {
                if (isProfilerReady()) {
                    synchronized (WildflyOutputSupport.this) {
                        started = true;
                        WildflyOutputSupport.this.notifyAll();
                    }
                    check = false;
                } else if (isProfilerInactive()) {
                    synchronized (WildflyOutputSupport.this) {
                        failed = true;
                        WildflyOutputSupport.this.notifyAll();
                    }
                    check = false;
                }
            }

            if (line.indexOf("Starting JBoss (MX MicroKernel)") > -1 // JBoss 4.x message // NOI18N
                    || line.indexOf("Starting JBoss (Microcontainer)") > -1 // JBoss 5.0 message // NOI18N
                    || line.indexOf("Starting JBossAS") > -1
                    || WILDFLY_8_STARTING_ML.matcher(line).matches()
                    || WILDFLY_STARTING_ML.matcher(line).matches()
                    || EAP6_STARTING_ML.matcher(line).matches()) { // JBoss 6.0 message // NOI18N
                LOGGER.log(Level.FINER, "STARTING message fired"); // NOI18N
                //fireStartProgressEvent(StateType.RUNNING, createProgressMessage("MSG_START_SERVER_IN_PROGRESS")); // NOI18N
            } else if (((line.indexOf("JBoss (MX MicroKernel)") > -1 // JBoss 4.x message // NOI18N
                    || line.indexOf("JBoss (Microcontainer)") > -1 // JBoss 5.0 message // NOI18N
                    || line.indexOf("JBossAS") > -1 // JBoss 6.0 message // NOI18N
                    || line.indexOf("JBoss AS") > -1)// JBoss 7.0 message // NOI18N
                    && (line.indexOf("Started in") > -1) // NOI18N
                    || line.indexOf("started in") > -1 // NOI18N
                    || line.indexOf("started (with errors) in") > -1) // JBoss 7 with some errors (include wrong deployments) // NOI18N
                    || JBOSS_7_STARTED_ML.matcher(line).matches()
                    || WILDFLY_8_STARTED_ML.matcher(line).matches()
                    || WILDFLY_9_STARTED_ML.matcher(line).matches()                    
                    || WILDFLY_10_STARTED_ML.matcher(line).matches()
                    || EAP6_STARTED_ML.matcher(line).matches()) {
                LOGGER.log(Level.FINER, "STARTED message fired"); // NOI18N

                synchronized (WildflyOutputSupport.this) {
                    started = true;
                    WildflyOutputSupport.this.notifyAll();
                }
                check = false;
            } else if (line.indexOf("Shutdown complete") > -1) { // NOI18N
                synchronized (WildflyOutputSupport.this) {
                    failed = true;
                    WildflyOutputSupport.this.notifyAll();
                }
                check = false;
            }
        }

        @Override
        public void reset() {
            // noop
        }

        @Override
        public void close() {
            // noop
        }
    }

    private class ProfilerCheckTask implements Runnable {

        @Override
        public void run() {
            for (;;) {
                if (isProfilerReady()) {
                    synchronized (WildflyOutputSupport.this) {
                        started = true;
                        WildflyOutputSupport.this.notifyAll();
                    }
                    break;
                } else if (isProfilerInactive()) {
                    synchronized (WildflyOutputSupport.this) {
                        failed = true;
                        WildflyOutputSupport.this.notifyAll();
                    }
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.INFO, null, ex);
                    break;
                }
            }
        }

    }
}
