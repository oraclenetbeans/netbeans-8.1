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

package org.netbeans.modules.weblogic.common.api;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.extexecution.base.BaseExecutionDescriptor;
import org.netbeans.api.extexecution.base.BaseExecutionService;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.netbeans.api.extexecution.base.input.LineProcessors;
import org.netbeans.modules.weblogic.common.ProxyUtils;
import org.netbeans.modules.weblogic.common.spi.WebLogicTrustHandler;
import org.openide.util.BaseUtilities;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Petr Hejl
 */
public final class WebLogicDeployer {

    private static final Logger LOGGER = Logger.getLogger(WebLogicDeployer.class.getName());

    private static final RequestProcessor DEPLOYMENT_RP = new RequestProcessor(WebLogicDeployer.class);

    private static final int TIMEOUT = 300000;

    private static final Version VERSION_10 = Version.fromJsr277NotationWithFallback("10"); // NOI18N

    private final WebLogicConfiguration config;

    private final File javaBinary;

    private final Callable<String> nonProxy;

    private WebLogicDeployer(WebLogicConfiguration config, File javaBinary, @NullAllowed Callable<String> nonProxy) {
        this.config = config;
        this.javaBinary = javaBinary;
        this.nonProxy = nonProxy;
    }

    @NonNull
    public static WebLogicDeployer getInstance(@NonNull WebLogicConfiguration config,
            @NullAllowed File javaBinary, @NullAllowed Callable<String> nonProxy) {
        return new WebLogicDeployer(config, javaBinary, nonProxy);
    }

    @NonNull
    public Future<Collection<Application>> list(@NullAllowed final InetAddress publicAddress) {
        return DEPLOYMENT_RP.submit(new Callable<Collection<Application>>() {

            @Override
            public Collection<Application> call() throws Exception {
                return config.getRemote().executeAction(new WebLogicRemote.JmxAction<Collection<Application>>() {

                    @Override
                    public Collection<Application> execute(MBeanServerConnection connection) throws Exception {
                        List<Application> result = new ArrayList<>();
                        ObjectName service = new ObjectName("com.bea:Name=DomainRuntimeService," // NOI18N
                                    + "Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean"); // NOI18N
                        ObjectName domainConfig = (ObjectName) connection.getAttribute(service,
                                    "DomainConfiguration"); // NOI18N
                        ObjectName beans[] = (ObjectName[]) connection.getAttribute(domainConfig, "AppDeployments"); // NOI18N
                        for (ObjectName bean : beans) {
                            String name = (String) connection.getAttribute(bean, "Name"); // NOI18N
                            String type = (String) connection.getAttribute(bean, "Type"); // NOI18N
                            if ("AppDeployment".equals(type)) { // NOI18N
                                String moduleType = (String) connection.getAttribute(bean, "ModuleType"); // NOI18N
                                String contextRoot = null;
                                ObjectName[] targets = (ObjectName[]) connection.getAttribute(bean, "Targets"); // NOI18N
                                if (targets != null && targets.length > 0) {
                                    String server = (String) connection.getAttribute(targets[0], "Name"); // NOI18N
                                    ObjectName serverRuntime = (ObjectName) connection.invoke(
                                            service, "lookupServerRuntime", new Object[]{server}, new String[] {"java.lang.String"}); // NOI18N
                                    if (serverRuntime != null) {
                                        ObjectName appRuntime = (ObjectName) connection.invoke(
                                                serverRuntime, "lookupApplicationRuntime", new Object[]{name}, new String[] {"java.lang.String"}); // NOI18N
                                        if (appRuntime != null) {
                                            ObjectName[] runtimes = (ObjectName[]) connection.getAttribute(appRuntime, "ComponentRuntimes"); // NOI18N
                                            if (runtimes != null) {
                                                for (ObjectName runtime : runtimes) {
                                                    String runtimeType = (String) connection.getAttribute(runtime, "Type"); // NOI18N
                                                    if ("WebAppComponentRuntime".equals(runtimeType)) { // NOI18N
                                                        contextRoot = (String) connection.getAttribute(runtime, "ContextRoot"); // NOI18N
                                                        if (contextRoot != null) {
                                                            // XXX may there be multiple web apps in ear?
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (contextRoot != null) {
                                    URL url;
                                    if (publicAddress != null) {
                                        url = new URL("http://" + publicAddress.getHostAddress() + ":" + config.getPort() + contextRoot); // NOI18N
                                    } else {
                                        url = new URL("http://" + config.getHost() + ":" + config.getPort() + contextRoot); // NOI18N
                                    }
                                    result.add(new Application(name, moduleType, url, contextRoot));
                                } else {
                                    result.add(new Application(name, moduleType, null, null));
                                }
                            }
                        }
                        return result;
                    }
                }, nonProxy);
            }
        });
    }

    @NonNull
    public Future<String> deploy(@NonNull File file, @NullAllowed DeployListener listener,
            @NullAllowed String name) {

        return performDeploy(file, listener, name);
    }

    @NonNull
    public Future<Void> redeploy(@NonNull String name, @NonNull File file,
            @NullAllowed BatchDeployListener listener) {
        return performRedeploy(Collections.singletonList(name), Collections.singletonList(file), listener);
    }

    @NonNull
    public Future<Void> redeploy(@NonNull List<String> names, @NullAllowed BatchDeployListener listener) {
        return performRedeploy(names, null, listener);
    }

    @NonNull
    public Future<Void> undeploy(@NonNull final Collection<String> names,
            @NullAllowed final BatchDeployListener listener) {

        if (listener != null) {
            listener.onStart();
        }

        return DEPLOYMENT_RP.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                LastLineProcessor lineProcessor = new LastLineProcessor();
                for (String name : names) {
                    BaseExecutionService service = createService("-undeploy", lineProcessor, "-name", name);
                    if (listener != null) {
                        listener.onStepStart(name);
                    }

                    Future<Integer> result = service.run();
                    try {
                        Integer value = result.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        if (value != 0) {
                            if (listener != null) {
                                listener.onFail(lineProcessor.getLastLine());
                            }
                            throw new IOException("Command failed: " + lineProcessor.getLastLine());
                        } else {
                            if (listener != null) {
                                listener.onStepFinish(name);
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (listener != null) {
                            listener.onInterrupted();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (TimeoutException ex) {
                        if (listener != null) {
                            listener.onTimeout();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (ExecutionException ex) {
                        if (listener != null) {
                            Throwable cause = ex.getCause();
                            if (cause instanceof Exception) {
                                listener.onException((Exception) cause);
                                throw (Exception) cause;
                            } else {
                                listener.onException(ex);
                                throw ex;
                            }
                        }
                    }
                }
                if (listener != null) {
                    listener.onFinish();
                }
                return null;
            }
        });
    }

    @NonNull
    public Future<Void> start(@NonNull final Collection<String> names,
            @NullAllowed final BatchDeployListener listener) {

        if (listener != null) {
            listener.onStart();
        }

        return DEPLOYMENT_RP.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                LastLineProcessor lineProcessor = new LastLineProcessor();
                for (String name : names) {
                    BaseExecutionService service = createService("-start", lineProcessor, "-name", name);
                    if (listener != null) {
                        listener.onStepStart(name);
                    }

                    Future<Integer> result = service.run();
                    try {
                        Integer value = result.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        if (value != 0) {
                            if (listener != null) {
                                listener.onFail(lineProcessor.getLastLine());
                            }
                            throw new IOException("Command failed: " + lineProcessor.getLastLine());
                        } else {
                            if (listener != null) {
                                listener.onStepFinish(name);
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (listener != null) {
                            listener.onInterrupted();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (TimeoutException ex) {
                        if (listener != null) {
                            listener.onTimeout();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (ExecutionException ex) {                        
                        Throwable cause = ex.getCause();
                        if (cause instanceof Exception) {
                            if (listener != null) {
                            listener.onException((Exception) cause);
                            }
                            throw (Exception) cause;
                        } else {
                            if (listener != null) {
                            listener.onException(ex);
                            }
                            throw ex;
                        }
                    }
                }
                if (listener != null) {
                    listener.onFinish();
                }
                return null;
            }
        });
    }

    @NonNull
    public Future<Void> stop(@NonNull final Collection<String> names,
            @NullAllowed final BatchDeployListener listener) {

        if (listener != null) {
            listener.onStart();
        }

        return DEPLOYMENT_RP.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                LastLineProcessor lineProcessor = new LastLineProcessor();
                for (String name : names) {
                    BaseExecutionService service = createService("-stop", lineProcessor, "-name", name);
                    if (listener != null) {
                        listener.onStepStart(name);
                    }

                    Future<Integer> result = service.run();
                    try {
                        Integer value = result.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        if (value != 0) {
                            if (listener != null) {
                                listener.onFail(lineProcessor.getLastLine());
                            }
                            throw new IOException("Command failed: " + lineProcessor.getLastLine());
                        } else {
                            if (listener != null) {
                                listener.onStepFinish(name);
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (listener != null) {
                            listener.onInterrupted();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (TimeoutException ex) {
                        if (listener != null) {
                            listener.onTimeout();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof Exception) {
                            if (listener != null) {
                                listener.onException((Exception) cause);
                            }
                            throw (Exception) cause;
                        } else {
                            if (listener != null) {
                                listener.onException(ex);
                            }
                            throw ex;
                        }
                    }
                }
                if (listener != null) {
                    listener.onFinish();
                }
                return null;
            }
        });
    }

    private Future<String> performDeploy(@NonNull final File file, @NullAllowed final DeployListener listener,
            @NullAllowed final String name) {

        if (listener != null) {
            listener.onStart();
        }

        return DEPLOYMENT_RP.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                List<String> parameters = new ArrayList<>();
                if (config.isRemote()) {
                    parameters.add("-upload"); // NOI18N
                    parameters.add("-remote"); // NOI18N
                }

                parameters.add("-name"); // NOI18N
                parameters.add(getName(file, name));

                if (file.isDirectory()) {
                    parameters.add("-nostage"); // NOI18N
                }
                parameters.add("-source"); // NOI18N
                parameters.add(file.getAbsolutePath());

                LastLineProcessor lineProcessor = new LastLineProcessor();
                BaseExecutionService service = createService("-deploy", // NOI18N
                        lineProcessor, parameters.toArray(new String[parameters.size()]));
                Future<Integer> result = service.run();
                try {
                    Integer value = result.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (value != 0) {
                        if (listener != null) {
                            listener.onFail(lineProcessor.getLastLine());
                        }
                        throw new IOException("Command failed: " + lineProcessor.getLastLine());
                    } else {
                        if (listener != null) {
                            listener.onFinish();
                        }
                        if (name != null) {
                            return name;
                        }
                        String name = file.getName();
                        int index = name.lastIndexOf('.');
                        if (file.isFile() && index > 0) {
                            name = name.substring(0, index);
                        }
                        return name;
                    }
                } catch (InterruptedException ex) {
                    if (listener != null) {
                        listener.onInterrupted();
                    }
                    result.cancel(true);
                    throw ex;
                } catch (TimeoutException ex) {
                    if (listener != null) {
                        listener.onTimeout();
                    }
                    result.cancel(true);
                    throw ex;
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof Exception) {
                        if (listener != null) {
                            listener.onException((Exception) cause);
                        }
                        throw (Exception) cause;
                    } else {
                        if (listener != null) {
                            listener.onException(ex);
                        }
                        throw ex;
                    }
                }
            }
        });
    }

    private Future<Void> performRedeploy(@NonNull final List<String> names, @NullAllowed final List<File> files,
            @NullAllowed final BatchDeployListener listener) {

        assert files == null || files.size() == names.size();

        if (listener != null) {
            listener.onStart();
        }

        return DEPLOYMENT_RP.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                LastLineProcessor lineProcessor = new LastLineProcessor();
                int i = 0;
                for (String name : names) {
                    List<String> parameters = new ArrayList<>();
                    if (config.isRemote()) {
                        parameters.add("-upload"); // NOI18N
                        parameters.add("-remote"); // NOI18N
                    }
                    parameters.add("-name"); // NOI18N
                    parameters.add(name);
                    if (files != null) {
                        parameters.add("-source"); // NOI18N
                        parameters.add(files.get(i++).getAbsolutePath());

                    }
                    BaseExecutionService service = createService("-redeploy", // NOI18N
                            lineProcessor, parameters.toArray(new String[parameters.size()]));
                    if (listener != null) {
                        listener.onStepStart(name);
                    }

                    Future<Integer> result = service.run();
                    try {
                        Integer value = result.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        if (value != 0) {
                            if (listener != null) {
                                listener.onFail(lineProcessor.getLastLine());
                            }
                            throw new IOException("Command failed: " + lineProcessor.getLastLine());
                        } else {
                            if (listener != null) {
                                listener.onStepFinish(name);
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (listener != null) {
                            listener.onInterrupted();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (TimeoutException ex) {
                        if (listener != null) {
                            listener.onTimeout();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (ExecutionException ex) {                        
                        Throwable cause = ex.getCause();
                        if (cause instanceof Exception) {
                            if (listener != null) {
                                listener.onException((Exception) cause);
                            }
                            throw (Exception) cause;
                        } else {
                            if (listener != null) {
                                listener.onException(ex);
                            }
                            throw ex;
                        }
                    }
                }
                if (listener != null) {
                    listener.onFinish();
                }
                return null;
            }
        });
    }

    public Future<Void> deploy(@NonNull final List<Artifact> artifacts,
            @NullAllowed final BatchDeployListener listener) {

        if (listener != null) {
            listener.onStart();
        }

        return DEPLOYMENT_RP.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                LastLineProcessor lineProcessor = new LastLineProcessor();
                for (Artifact artifact : artifacts) {
                    List<String> parameters = new ArrayList<>();
                    parameters.add("-upload"); // NOI18N
                    if (config.isRemote()) {
                        parameters.add("-remote"); // NOI18N
                    }
                    String name = getName(artifact.getFile(), artifact.getName());
                    parameters.add("-name"); // NOI18N
                    parameters.add(name);
                    if (artifact.isLibrary()) {
                        parameters.add("-library"); // NOI18N
                    }
                    parameters.add(artifact.getFile().getAbsolutePath());

                    BaseExecutionService service = createService("-deploy", // NOI18N
                            lineProcessor, parameters.toArray(new String[parameters.size()]));
                    if (listener != null) {
                        listener.onStepStart(name);
                    }

                    Future<Integer> result = service.run();
                    try {
                        Integer value = result.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        if (value != 0) {
                            if (listener != null) {
                                listener.onFail(lineProcessor.getLastLine());
                            }
                            throw new IOException("Command failed: " + lineProcessor.getLastLine());
                        } else {
                            if (listener != null) {
                                listener.onStepFinish(name);
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (listener != null) {
                            listener.onInterrupted();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (TimeoutException ex) {
                        if (listener != null) {
                            listener.onTimeout();
                        }
                        result.cancel(true);
                        throw ex;
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof Exception) {
                            if (listener != null) {
                                listener.onException((Exception) cause);
                            }
                            throw (Exception) cause;
                        } else {
                            if (listener != null) {
                                listener.onException(ex);
                            }
                            throw ex;
                        }
                    }
                }
                if (listener != null) {
                    listener.onFinish();
                }
                return null;
            }
        });
    }

    private BaseExecutionService createService(final String command,
            final LineProcessor processor, String... parameters) {

        org.netbeans.api.extexecution.base.ProcessBuilder builder =
                org.netbeans.api.extexecution.base.ProcessBuilder.getLocal();
        builder.setExecutable(getJavaBinary());
        builder.setRedirectErrorStream(true);
        List<String> arguments = new ArrayList<String>();
        // NB supports only JDK6+ while WL 9, only JDK 5
        Version version = config.getDomainVersion();
        if (version == null
                || !version.isAboveOrEqual(VERSION_10)) {
            arguments.add("-Dsun.lang.ClassLoader.allowArraySyntax=true"); // NOI18N
        }

        if (config.isRemote()) {
            try {
                // XXX authentication
                // t3 and t3s is afaik sits on top of http and https (source ?)
                List<Proxy> proxies = ProxySelector.getDefault().select(
                        new URI((config.isSecured() ? "https://" : "http://")
                                + config.getHost() + ":" + config.getPort())); // NOI18N
                if (!proxies.isEmpty()) {
                    Proxy first = proxies.get(0);
                    if (first.type() != Proxy.Type.DIRECT) {
                        SocketAddress addr = first.address();
                        if (addr instanceof InetSocketAddress) {
                            InetSocketAddress inet = (InetSocketAddress) addr;
                            if (first.type() == Proxy.Type.HTTP) {
                                arguments.add("-Dhttp.proxyHost=" + inet.getHostString()); // NOI18N
                                arguments.add("-Dhttp.proxyPort=" + inet.getPort()); // NOI18N
                                arguments.add("-Dhttps.proxyHost=" + inet.getHostString()); // NOI18N
                                arguments.add("-Dhttps.proxyPort=" + inet.getPort()); // NOI18N
                            } else if (first.type() == Proxy.Type.SOCKS) {
                                arguments.add("-DsocksProxyHost=" + inet.getHostString()); // NOI18N
                                arguments.add("-DsocksProxyPort=" + inet.getPort()); // NOI18N
                            }
                        }
                    }
                }
                String nonProxyHosts = ProxyUtils.getNonProxyHosts(nonProxy);
                if (nonProxyHosts != null) {
                    arguments.add("-Dhttp.nonProxyHosts=\"" + nonProxyHosts + "\""); // NOI18N
                }
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.INFO, null, ex);
            }
        }

        if (config.isSecured()) {
            WebLogicTrustHandler handler = Lookup.getDefault().lookup(WebLogicTrustHandler.class);
            if (handler != null) {
                for (Map.Entry<String, String> e : handler.getTrustProperties(config).entrySet()) {
                    arguments.add("-D" + e.getKey() + "=" + e.getValue()); // NOI18N
                }
            }
        }

        arguments.add("-cp"); // NOI18N
        arguments.add(getClassPath());
        arguments.add("weblogic.Deployer"); // NOI18N
        arguments.add("-adminurl"); // NOI18N
        arguments.add(config.getAdminURL());
        arguments.add("-username"); // NOI18N
        arguments.add(config.getUsername());
        //arguments.add("-password"); // NOI18N
        //arguments.add(config.getPassword());
        arguments.add(command);

        arguments.addAll(Arrays.asList(parameters));

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Java CL deployer arguments");
            for (String arg : arguments) {
                LOGGER.log(Level.FINE, arg);
            }
        }

        builder.setArguments(arguments);

        final LineProcessor realProcessor;
        if (processor != null || LOGGER.isLoggable(Level.FINEST)) {
            if (processor == null) {
                realProcessor = new LoggingLineProcessor(Level.FINEST);
            } else if (!LOGGER.isLoggable(Level.FINEST)) {
                realProcessor = processor;
            } else {
                realProcessor = LineProcessors.proxy(processor, new LoggingLineProcessor(Level.FINEST));
            }
        } else {
            realProcessor = null;
        }
        BaseExecutionDescriptor descriptor = new BaseExecutionDescriptor().outProcessorFactory(new BaseExecutionDescriptor.InputProcessorFactory() {

            @Override
            public InputProcessor newInputProcessor() {
                return InputProcessors.bridge(realProcessor);
            }
        }).inReaderFactory(new BaseExecutionDescriptor.ReaderFactory() {

            @Override
            public Reader newReader() {
                return new StringReader(config.getPassword() + "\n"); // NOI18N
            }
        });
        return BaseExecutionService.newService(builder, descriptor);
    }

    private String getClassPath() {
        File[] files = config.getLayout().getClassPath();
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.append(file.getAbsolutePath()).append(File.pathSeparatorChar);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private String getJavaBinary() {
        if (javaBinary != null) {
            return javaBinary.getAbsolutePath();
        }
        return BaseUtilities.isWindows() ? "java.exe" : "java"; // NOI18N
    }

    private static String getName(File file, String name) {
        // #249066
        // during the remote deployment from windows to linux file is
        // properly uploaded but the name of the app is set to the whole
        // path using windows separator (WLS bug?)
        // this causes java.lang.IllegalArgumentException: Unexpected character
        // when the server is starting the app
        // to prevent that we always compute a default name - should be
        // the same way WLS is using
        String realName = name;
        if (realName == null) {
            realName = file.getName();
            if (!file.isDirectory()) {
                int dot = realName.lastIndexOf('.'); // NOI18N
                if (dot > 0) {
                    realName = realName.substring(0, dot);
                }
            }
        }
        return realName;
    }

    public static final class Artifact {

        private final File file;

        private final String name;

        private final boolean library;

        public Artifact(File file, String name, boolean library) {
            this.file = file;
            this.name = name;
            this.library = library;
        }

        public File getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        public boolean isLibrary() {
            return library;
        }
    }

    public static final class Application {

        private final String name;

        private final String type;

        private final URL url;

        private final String webContext;

        private Application(String id, String type, URL url, String webContext) {
            this.name = id;
            this.type = type;
            this.url = url;
            this.webContext = webContext;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public URL getUrl() {
            return url;
        }

        public String getWebContext() {
            return webContext;
        }
    }

    private static class LastLineProcessor implements LineProcessor {

        private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("^\\s+((at)|(\\.\\.\\.)).*$"); // NOI18N

        private String last = "";

        private LastLineProcessor() {
            super();
        }


        @Override
        public synchronized void processLine(String line) {
            if (line.length() != 0 && !STACK_TRACE_PATTERN.matcher(line).matches()) {
                last = line;
            }
        }

        public synchronized String getLastLine() {
            return last;
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }
    }

    private static class LoggingLineProcessor implements LineProcessor {

        private final Level level;

        private LoggingLineProcessor(Level level) {
            this.level = level;
        }

        @Override
        public void processLine(String line) {
            LOGGER.log(level, line);
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }
    }
}
