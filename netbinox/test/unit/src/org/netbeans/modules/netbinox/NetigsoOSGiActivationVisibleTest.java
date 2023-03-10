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
 * Portions Copyrighted 2011 Oracle
 */
package org.netbeans.modules.netbinox;

import java.lang.reflect.Method;
import org.netbeans.core.startup.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.netbeans.Module;
import org.netbeans.ModuleManager;
import org.netbeans.SetupHid;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Are modules turned on by OSGi framework directly marked as enabled?
 * Can we load classes from them?
 *
 * @author Jaroslav Tulach
 */
public class NetigsoOSGiActivationVisibleTest extends SetupHid {
    private static Module m1;
    private Module m2;
    private static ModuleManager mgr;
    private int cnt;
    private File simpleBundle;
    private File activate;

    public NetigsoOSGiActivationVisibleTest(String name) {
        super(name);
    }
    
    protected boolean autoload() {
        return false;
    }

    protected @Override void setUp() throws Exception {
        Locale.setDefault(Locale.US);
        clearWorkDir();
        File ud = new File(getWorkDir(), "ud");
        ud.mkdirs();
        System.setProperty("netbeans.user", ud.getPath());
        
        data = new File(getDataDir(), "jars");
        jars = new File(getWorkDir(), "space in path");
        jars.mkdirs();
        activate = createTestJAR("activate", null);
        File simpleModule = createTestJAR("simple-module", null);
        
        String mf = "Bundle-SymbolicName: org.snd.module\n" +
            "Bundle-Version: 33.0.3\n" +
            "Bundle-ManifestVersion: 2\n";
        simpleBundle = NetigsoHid.changeManifest(getWorkDir(), simpleModule, mf);
    }

    public void testActivation() throws Exception {
        ModuleSystem ms = Main.getModuleSystem();
        mgr = ms.getManager();
        mgr.mutexPrivileged().enterWriteAccess();
        BundleContext bc;
        try {
            m1 = mgr.create(activate, null, false, false, false);
            m2 = mgr.create(simpleBundle, null, false, autoload(), false);
            
            mgr.enable(m1);

            Class<?> main = m1.getClassLoader().loadClass("org.activate.Main");
            Object s = main.getField("start").get(null);
            assertNotNull("Bundle started, its context provided", s);
            bc = (BundleContext) s;
        } finally {
            mgr.mutexPrivileged().exitWriteAccess();
        }
        Bundle toEnable = null;
        for (Bundle b : bc.getBundles()) {
            if (b.getSymbolicName().equals("org.snd.module")) {
                toEnable = b;
                break;
            }
        }
        assertNotNull("org.snd.module bundle found", toEnable);
            
        assertFalse("not started yet", m2.isEnabled());
        toEnable.start();

        Class<?> directBundle = toEnable.loadClass("org.foo.Something");
        assertNotNull("Bundle knows how to load the class", directBundle);
        
        Class<?> someModule = m2.getClassLoader().loadClass("org.foo.Something");
        assertNotNull("Something loaded from module CL", someModule);
        
        Method loadClass = directBundle.getMethod("loadClass", String.class, ClassLoader.class);
        Class<?> directly = (Class<?>) loadClass.invoke(null, "org.foo.Something", null);
        assertNotNull("Bundle knows how to load the class from itself without problems", directly);
        
        Class<?> someFromBundle = (Class<?>) loadClass.invoke(null, "org.foo.Something", someModule.getClassLoader());
        assertNotNull("Bundle knows how to load the class from its using own classloader", someFromBundle);

        Class<?> some = (Class<?>) loadClass.invoke(null, "org.foo.Something", Thread.currentThread().getContextClassLoader());
        assertNotNull("Context class loader loads from disabled module bundles too", some);

        assertFalse("still disabled from NetBeans view point", m2.isEnabled());
    }
    private File createTestJAR(String name, String srcdir, File... classpath) throws IOException {
        return createTestJAR(data, jars, name, srcdir, classpath);
    }
}
