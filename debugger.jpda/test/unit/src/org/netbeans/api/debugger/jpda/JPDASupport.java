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

package org.netbeans.api.debugger.jpda;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.debugger.*;

import java.io.*;
import java.util.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.beans.PropertyChangeEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.sun.jdi.connect.*;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.Bootstrap;
//import org.netbeans.api.java.classpath.ClassPath;
import junit.framework.Test;
import junit.framework.TestCase;
import org.netbeans.api.java.classpath.ClassPath;
//import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbModuleSuite.Configuration;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;

/**
 * Contains support functionality for unit tests.
 *
 * @author Maros Sandor
 */
public class JPDASupport implements DebuggerManagerListener {

    private static final boolean    verbose = false;
    private static final DateFormat df = new SimpleDateFormat("kk:mm:ss.SSS");
    private static DebuggerManager  dm = DebuggerManager.getDebuggerManager ();

    private JPDADebugger            jpdaDebugger;
    private DebuggerEngine          debuggerEngine;
    

    private Object [] debuggerStartLock = new Object[1];
    private Object [] stepLock = new Object[1];

    private Object              STATE_LOCK = new Object ();
    
    
    private JPDASupport (JPDADebugger jpdaDebugger) {
        this.jpdaDebugger = jpdaDebugger;
        jpdaDebugger.addPropertyChangeListener (this);
        DebuggerEngine[] de = dm.getDebuggerEngines ();
        int i, k = de.length;
        for (i = 0; i < k; i++)
            if (de [i].lookupFirst (null, JPDADebugger.class) == jpdaDebugger) {
                debuggerEngine = de [i];
                break;
            }
    }
    
    public static Test createTestSuite(Class<? extends TestCase> clazz) {
        Configuration suiteConfiguration = NbModuleSuite.createConfiguration(clazz);
        suiteConfiguration = suiteConfiguration.gui(false);
        //suiteConfiguration = suiteConfiguration.reuseUserDir(false);
        return NbModuleSuite.create(suiteConfiguration);
    }

    
    // starting methods ........................................................

//    public static JPDASupport listen (String mainClass) 
//    throws IOException, IllegalConnectorArgumentsException, 
//    DebuggerStartException {
//        VirtualMachineManager vmm = Bootstrap.virtualMachineManager ();
//        List lconnectors = vmm.listeningConnectors ();
//        ListeningConnector connector = null;
//        for (Iterator i = lconnectors.iterator (); i.hasNext ();) {
//            ListeningConnector lc = (ListeningConnector) i.next ();
//            Transport t = lc.transport ();
//            if (t != null && t.name ().equals ("dt_socket")) {
//                connector = lc;
//                break;
//            }
//        }
//        if (connector == null) 
//            throw new RuntimeException 
//                ("No listening socket connector available");
//
//        Map args = connector.defaultArguments ();
//        String address = connector.startListening (args);
//        String localhostAddres;
//        try
//        {
//            int port = Integer.parseInt 
//                (address.substring (address.indexOf (':') + 1));
//            localhostAddres = "localhost:" + port;
//            Connector.IntegerArgument portArg = 
//                (Connector.IntegerArgument) args.get("port");
//            portArg.setValue(port);
//        } catch (Exception e) {
//            // this address format is not known, use default
//            localhostAddres = address;
//        }
//
//        JPDADebugger jpdaDebugger = JPDADebugger.listen 
//            (connector, args, createServices ());
//        if (jpdaDebugger == null) 
//            throw new DebuggerStartException ("JPDA jpdaDebugger was not started");
//        Process process = launchVM (mainClass, localhostAddres, false);
//        ProcessIO pio = new ProcessIO (process);
//        pio.go ();
//        return new JPDASupport (jpdaDebugger);
//    }

    private static void deleteUserDir() {
        String userDir = System.getProperty("netbeans.user");
        if (userDir != null) {
            delete(new File(userDir));
        }
    }
    
    private static void delete(File f) {
        if (f.isDirectory()) {
            for (File cf : f.listFiles()) {
                delete(cf);
            }
        }
        f.delete();
    }

    public static JPDASupport attach (String mainClass) throws IOException, 
    DebuggerStartException {
        return attach(mainClass, null);
    }
    
    public static JPDASupport attach (String mainClass, String[] args) throws IOException, 
    DebuggerStartException {
        Process process = launchVM (mainClass, args, "", true);
        String line = readLine (process.getInputStream ());
        int port = Integer.parseInt (line.substring (line.lastIndexOf (':') + 1).trim ());
        ProcessIO pio = new ProcessIO (process);
        pio.go ();

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        List aconnectors = vmm.attachingConnectors();
        AttachingConnector connector = null;
        for (Iterator i = aconnectors.iterator(); i.hasNext();) {
            AttachingConnector ac = (AttachingConnector) i.next();
            Transport t = ac.transport ();
            if (t != null && t.name().equals("dt_socket")) {
                connector = ac;
                break;
            }
        }
        if (connector == null) 
            throw new RuntimeException
                ("No attaching socket connector available");

        JPDADebugger jpdaDebugger = JPDADebugger.attach (
            "localhost", 
            port, 
            createServices ()
        );
        return new JPDASupport (jpdaDebugger);
    }

    
    // public interface ........................................................
    
    public void doContinue () {
        if (jpdaDebugger.getState () != JPDADebugger.STATE_STOPPED) 
            throw new IllegalStateException ();
        debuggerEngine.getActionsManager ().doAction 
            (ActionsManager.ACTION_CONTINUE);
    }

    public void stepOver () {
        step (ActionsManager.ACTION_STEP_OVER);
    }

    public void stepInto () {
        step (ActionsManager.ACTION_STEP_INTO);
    }

    public void stepOut () {
        step (ActionsManager.ACTION_STEP_OUT);
    }

    public void step (Object action) {
        if (jpdaDebugger.getState () != JPDADebugger.STATE_STOPPED)
            throw new IllegalStateException ();
        debuggerEngine.getActionsManager ().doAction (action);
        waitState (JPDADebugger.STATE_STOPPED);
    }

    public void stepAsynch (final Object actionAsynch, final ActionsManagerListener al) {
        if (jpdaDebugger.getState () != JPDADebugger.STATE_STOPPED)
            throw new IllegalStateException ();
        debuggerEngine.getActionsManager().addActionsManagerListener(
                new ActionsManagerListener() {
                    public void actionPerformed(Object action) {
                        if (action != actionAsynch) return ;
                        al.actionPerformed(action);
                        debuggerEngine.getActionsManager().removeActionsManagerListener(this);
                    }
                    public void actionStateChanged(Object action, boolean enabled) {
                    }
                }
        );
        debuggerEngine.getActionsManager ().postAction (actionAsynch);
    }

    public void doFinish () {
        if (jpdaDebugger == null) return;
        debuggerEngine.getActionsManager ().
            doAction (ActionsManager.ACTION_KILL);
        waitState (JPDADebugger.STATE_DISCONNECTED);
        deleteUserDir();
    }

    public void waitState (int state) {
        synchronized (STATE_LOCK) {
            while ( jpdaDebugger.getState () != state &&
                    jpdaDebugger.getState () != JPDADebugger.STATE_DISCONNECTED
            ) {
                try {
                    STATE_LOCK.wait ();
                } catch (InterruptedException ex) {
                    ex.printStackTrace ();
                }
            }
        }
    }

    /*public void waitState (int state) {
        synchronized (STATE_LOCK) {
            int ds = jpdaDebugger.getState ();
            System.err.println("JPDASupport.waitState("+state+"): ds = "+ds+", jpdaDebugger = "+jpdaDebugger);
            while ( ds != state &&
                    ds != JPDADebugger.STATE_DISCONNECTED
            ) {
                try {
                    STATE_LOCK.wait ();
                } catch (InterruptedException ex) {
                    ex.printStackTrace ();
                }
                ds = jpdaDebugger.getState ();
                System.err.println("JPDASupport.waitState("+state+"): new ds = "+ds+", jpdaDebugger = "+jpdaDebugger);
            }
            System.err.println("JPDASupport.waitState("+state+"): state reached.");
        }
    }*/

    public JPDADebugger getDebugger() {
        return jpdaDebugger;
    }
    
    public static void removeAllBreakpoints () {
        Breakpoint[] bs = DebuggerManager.getDebuggerManager ().
            getBreakpoints ();
        int i, k = bs.length;
        for (i = 0; i < k; i++)
            DebuggerManager.getDebuggerManager ().removeBreakpoint (bs [i]);
    }
    
    
    // other methods ...........................................................
    
    private static Object[] createServices () {
        try {
            Map map = new HashMap ();
            String sourceRoot = System.getProperty ("test.dir.src");
            URL sourceUrl = new File(sourceRoot).toURI().toURL();
            String sourceUrlStr = sourceUrl.toString() + "/";
            sourceUrl = new URL(sourceUrlStr);
            ClassPath cp = ClassPathSupport.createClassPath (new URL[] {
                sourceUrl
            });
            map.put ("sourcepath", cp);
            map.put ("baseDir", new File(sourceRoot).getParentFile());
            return new Object[] { map };
        } catch (MalformedURLException ex) {
            //System.err.println("MalformedURLException: sourceRoot = '"+sourceRoot+"'.");
            ex.printStackTrace();
            return new Object[] {};
        }
    }

    private static String readLine (InputStream in) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (;;) {
            int c = in.read();
            if (c == -1) throw new EOFException();
            if (c == 0x0D) {
                c = in.read();
                if (c != 0x0A) sb.append((char)0x0D);
            }
            if (c == 0x0A) return sb.toString();
            sb.append((char)c);
        }
    }
    
    private static Process launchVM (
        String mainClass,
        String[] args,
        String connectorAddress, 
        boolean server
    ) throws IOException {

        String cp = getClassPath();
        //System.err.println("CP = "+cp);

        String [] cmdArray = new String [] {
            System.getProperty ("java.home") + File.separatorChar + 
                "bin" + File.separatorChar + "java",
            "-Xdebug",
            "-Xnoagent",
            "-Xrunjdwp:transport=" + "dt_socket" + ",address=" + 
                connectorAddress + ",suspend=y,server=" + 
                (server ? "y" : "n"),
            "-classpath",
            cp.substring(0, cp.length() -1),
            mainClass
        };
        if (args != null && args.length > 0) {
            String[] arr = new String[cmdArray.length + args.length];
            System.arraycopy(cmdArray, 0, arr, 0, cmdArray.length);
            System.arraycopy(args, 0, arr, cmdArray.length, args.length);
            cmdArray = arr;
        }

        return Runtime.getRuntime ().exec (cmdArray);
    }
    
    private static String getClassPath() {
        StringBuilder cp = new StringBuilder (200);
        ClassLoader cl = JPDASupport.class.getClassLoader ();
        if (cl instanceof URLClassLoader) {
            URLClassLoader ucl = (URLClassLoader) cl;
            URL [] urls = ucl.getURLs ();
            
            for (int i = 0; i < urls.length; i++) {
                URL url = urls [i];
                cp.append (url.getPath ());
                cp.append (File.pathSeparatorChar);
            }
        } else if (cl.getClass().getName().indexOf("org.netbeans.ModuleManager$SystemClassLoader") >= 0) {
            Class jarClassLoaderClass = cl.getClass().getSuperclass();
            try {
                java.lang.reflect.Field sourcesField = jarClassLoaderClass.getDeclaredField("sources");
                sourcesField.setAccessible(true);
                Object[] sources = (Object[]) sourcesField.get(cl);
                for (int i = 0; i < sources.length; i++) {
                    Method getURL = sources[i].getClass().getMethod("getURL");
                    getURL.setAccessible(true);
                    URL url = (URL) getURL.invoke(sources[i]);
                    cp.append (url.getPath ());
                    cp.append (File.pathSeparatorChar);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Problem retrieving class path from class loader: "+cl, ex);
            }
        } else {
            throw new RuntimeException("Unsupported class loader: "+cl);
        }
        
        return cp.toString();
    }
    
    public String toString () {
        switch (jpdaDebugger.getState ()) {
            case JPDADebugger.STATE_DISCONNECTED:
                return "Debugger finished.";
            case JPDADebugger.STATE_RUNNING:
                return "Debugger running.";
            case JPDADebugger.STATE_STARTING:
                return "Debugger starting.";
            case JPDADebugger.STATE_STOPPED:
                CallStackFrame f = jpdaDebugger.getCurrentCallStackFrame ();
                return "Debugger stopped: " +
                    f.getClassName () + "." + 
                    f.getMethodName () + ":" + 
                    f.getLineNumber (null);
        }
        return super.toString ();
    }
    
    // DebuggerListener ........................................................

    public Breakpoint[] initBreakpoints() {
        return new Breakpoint[0];
    }

    public void breakpointAdded(Breakpoint breakpoint) {
    }

    public void breakpointRemoved(Breakpoint breakpoint) {
    }

    public void initWatches() {
    }

    public void watchAdded(Watch watch) {
    }

    public void watchRemoved(Watch watch) {
    }

    public void sessionAdded(Session session) {
    }

    public void sessionRemoved(Session session) {
    }

    public void propertyChange (PropertyChangeEvent evt) {
        if (evt.getSource() instanceof JPDADebugger) {
            JPDADebugger dbg = (JPDADebugger) evt.getSource();

            if (JPDADebugger.PROP_STATE.equals(evt.getPropertyName())) {
                synchronized (STATE_LOCK) {
                    STATE_LOCK.notifyAll ();
                }
                if (jpdaDebugger.getState () == JPDADebugger.STATE_DISCONNECTED)
                    jpdaDebugger.removePropertyChangeListener (this);
            }
        }
    }

    // TODO: Include check of these call in the test suite
    public void engineAdded (DebuggerEngine debuggerEngine) {
    }

    // TODO: Include check of these call in the test suite
    public void engineRemoved (DebuggerEngine debuggerEngine) {
    }

    
    // innerclasses ............................................................
    
    private static class ProcessIO {

        private Process p;

        public ProcessIO(Process p) {
            this.p = p;
        }

        public void go() {
            InputStream out = p.getInputStream();
            InputStream err = p.getErrorStream();

            new SimplePipe(System.out, out).start();
            new SimplePipe(System.out, err).start();
        }
    }

    private static class SimplePipe extends Thread {
        private OutputStream out;
        private InputStream in;

        public SimplePipe(OutputStream out, InputStream in) {
            this.out = out;
            this.in = in;
            setDaemon(true);
        }

        public void run() {
            byte [] buffer = new byte[1024];
            int n;
            try {
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            } catch (IOException e) {
            } finally {
                try {
                    out.close();
                    in.close();
                } catch (IOException e) {
                }
            }
            System.out.println("PIO QUIT");
        }
    }
}
