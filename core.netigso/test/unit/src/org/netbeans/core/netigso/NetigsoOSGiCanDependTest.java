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

package org.netbeans.core.netigso;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.netbeans.MockEvents;
import org.netbeans.MockModuleInstaller;
import org.netbeans.Module;
import org.netbeans.ModuleManager;
import org.openide.modules.Dependency;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class NetigsoOSGiCanDependTest extends NetigsoHid {
    public NetigsoOSGiCanDependTest(String name) {
        super(name);
    }

    public void testOSGiCanDependOnNetBeans() throws Exception {
        MockModuleInstaller installer = new MockModuleInstaller();
        MockEvents ev = new MockEvents();
        ModuleManager mgr = new ModuleManager(installer, ev);
        mgr.mutexPrivileged().enterWriteAccess();
        HashSet<Module> both = null;
        try {
            String mfBar = "Bundle-SymbolicName: org.bar\n" +
                "Bundle-Version: 1.1.0\n" +
                "Bundle-ManifestVersion: 2\n" +
                "Export-Package: org.bar\n" +
                "Import-Package: org.foo\n" +
                "\n\n";

            File j1 = new File(jars, "simple-module.jar");
            File j2 = changeManifest(new File(jars, "depends-on-simple-module.jar"), mfBar);
            Module m1 = mgr.create(j1, null, false, false, false);
            Module m2 = mgr.create(j2, null, false, false, false);
            assertProvidesRequires(m2, "org.bar", "org.foo");
            HashSet<Module> b = new HashSet<Module>(Arrays.asList(m1, m2));
            mgr.enable(b);
            both = b;

            Class<?> clazz = m2.getClassLoader().loadClass("org.bar.SomethingElse");
            Class<?> sprclass = m2.getClassLoader().loadClass("org.foo.Something");

            assertEquals("Correct parent is used", sprclass, clazz.getSuperclass());
        } finally {
            if (both != null) {
                mgr.disable(both);
            }
            mgr.mutexPrivileged().exitWriteAccess();
        }
    }

    private static void assertProvidesRequires(Module m, String provides, String requires) {
        List<String> p = Arrays.asList(m.getProvides());
        assertTrue("Bundles provide their packages: " + p, p.contains(provides));
        
        for (Dependency d : m.getDependencies()) {
            if (d.getType() == Dependency.TYPE_RECOMMENDS) {
                if (requires.equals(d.getName())) {
                    return;
                }
            }
        }
        fail("Module " + m + " does not require " + requires);
    }

}
