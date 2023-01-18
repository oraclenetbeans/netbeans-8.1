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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.autoupdate.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.spi.autoupdate.UpdateItem;
import org.netbeans.spi.autoupdate.UpdateProvider;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Rechtacek
 */
public class UpdateUnitWithOsFactoryTest extends NbmAdvancedTestCase {
    
    public UpdateUnitWithOsFactoryTest (String testName) {
        super (testName);
    }
    
    private UpdateProvider p = null;
    private String testModuleName = "org.netbeans.modules.applemenu";
    private String testModuleVersion = "1.111";
    
    @Override
    protected void setUp () throws IOException {
        System.setProperty("netbeans.user", getWorkDirPath());
        Lookup.getDefault ().lookup (ModuleInfo.class);
        clearWorkDir ();
    }
    
    public void testUpdateItemsDoesntContainAlien () throws IOException {
        String os = org.openide.util.Utilities.isUnix () ? "Windows" : "Unix";
        Lookup.getDefault ().lookup (ModuleInfo.class);
        String catalog = generateCatalog (
                generateModuleElementWithRequires ("com.sun.collablet", "1.3", null,
                    "org.openide.filesystems > 6.2",
                    "org.openide.util > 6.2",
                    "org.openide.modules > 6.2",
                    "org.openide.nodes > 6.2",
                    "org.openide.loaders",
                    "org.openide.io"),
                generateModuleElementWithRequires (testModuleName, testModuleVersion, "org.openide.modules.os." + os,
                    "org.netbeans.core.windows/2",
                    "org.netbeans.modules.editor/3",
                    "org.netbeans.modules.java.editor/1 > 1.3",
                    "org.openide.filesystems > 6.2",
                    "org.openide.loaders",
                    "org.openide.modules > 6.2",
                    "org.openide.nodes > 6.2",
                    "org.openide.util > 6.2")
                
                );
        p = createUpdateProvider (catalog);
        p.refresh (true);
        Map<String, UpdateUnit> unitImpls = new HashMap<String, UpdateUnit> ();
        Map<String, UpdateItem> updates = p.getUpdateItems ();
        assertNotNull ("Some modules are installed.", updates);
        assertFalse ("Some modules are installed.", updates.isEmpty ());
        assertTrue (testModuleName + " found in parsed items.", updates.keySet ().contains (testModuleName + "_" + testModuleVersion));
        
        Map<String, UpdateUnit> newImpls = UpdateUnitFactory.getDefault ().appendUpdateItems (unitImpls, p);
        assertNotNull ("Some units found.", newImpls);
        assertFalse ("Some units found.", newImpls.isEmpty ());
        
        assertFalse (testModuleName + " doesn't found in generated UpdateUnits.", newImpls.keySet ().contains (testModuleName));
    }
    
    public void testUpdateItemsContainsMyModule () throws IOException {
        String os = ! org.openide.util.Utilities.isUnix () ? "Windows" : "Unix";
        String catalog = generateCatalog (
                generateModuleElementWithRequires ("com.sun.collablet", "1.3", null,
                    "org.openide.filesystems > 6.2",
                    "org.openide.util > 6.2",
                    "org.openide.modules > 6.2",
                    "org.openide.nodes > 6.2",
                    "org.openide.loaders",
                    "org.openide.io"),
                generateModuleElementWithRequires (testModuleName, testModuleVersion, "org.openide.modules.os." + os,
                    "org.netbeans.core.windows/2",
                    "org.netbeans.modules.editor/3",
                    "org.netbeans.modules.java.editor/1 > 1.3",
                    "org.openide.filesystems > 6.2",
                    "org.openide.loaders",
                    "org.openide.modules > 6.2",
                    "org.openide.nodes > 6.2",
                    "org.openide.util > 6.2")
                
                );
        p = createUpdateProvider (catalog);
        p.refresh (true);
        Map<String, UpdateUnit> unitImpls = new HashMap<String, UpdateUnit> ();
        Map<String, UpdateItem> updates = p.getUpdateItems ();
        assertNotNull ("Some modules are installed.", updates);
        assertFalse ("Some modules are installed.", updates.isEmpty ());
        assertTrue (testModuleName + " found in parsed items.", updates.keySet ().contains (testModuleName + "_" + testModuleVersion));
        
        Map<String, UpdateUnit> newImpls = UpdateUnitFactory.getDefault ().appendUpdateItems (unitImpls, p);
        assertNotNull ("Some units found.", newImpls);
        assertFalse ("Some units found.", newImpls.isEmpty ());
        
        assertTrue (testModuleName + " must found in generated UpdateUnits.", newImpls.keySet ().contains (testModuleName));
    }
    
}
