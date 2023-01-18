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

package org.netbeans.modules.javascript2.nodejs.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.modules.javascript2.editor.JsTestBase;
import static org.netbeans.modules.javascript2.editor.JsTestBase.JS_SOURCE_ID;
import org.openide.filesystems.FileObject;
import org.openide.util.test.MockLookup;
import org.netbeans.modules.javascript2.nodejs.TestProjectSupport;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Petr Pisl
 */
public class NodeJsDeclarationFinderTest extends JsTestBase {
    private static boolean projectCreated = false;
    
    public NodeJsDeclarationFinderTest(String testName) {
        super(testName);
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(); 
//        if (!projectCreated) {
            projectCreated = true;
            FileObject folder = getTestFile("TestNavigation");
            Project testProject = new TestProjectSupport.TestProject(folder, null);
            List lookupAll = new ArrayList();
            lookupAll.addAll(MockLookup.getDefault().lookupAll(Object.class));
            lookupAll.add(new TestProjectSupport.FileOwnerQueryImpl(testProject));
            MockLookup.setInstances(lookupAll.toArray());
//        }
    }
    
    @Override
    protected Map<String, ClassPath> createClassPathsForTest() {
        List<FileObject> cpRoots = new LinkedList<FileObject>();

        cpRoots.add(FileUtil.toFileObject(new File(getDataDir(), "/TestNavigation/public_html/")));
//        cpRoots.add(FileUtil.toFileObject(new File(getDataDir(), "/NodeJsRuntime/")));
        return Collections.singletonMap(
                JS_SOURCE_ID,
                ClassPathSupport.createClassPath(cpRoots.toArray(new FileObject[cpRoots.size()]))
        );
    }

    @Override
    protected boolean classPathContainsBinaries() {
        return true;
    }

    @Override
    protected boolean cleanCacheDir() {
        return false;
    }
    
    @Test
    public void testNavigation01() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/foo.js", "var circle = require('./cir^cle.js');", "circle.js", 0);
    }
    
    @Test
    public void testNavigation02() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/foo.js", "var triangle = require('trian^gle.js');", "triangle.js", 0);
    }
    
    @Test
    public void testNavigation03() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/lib/testLib.js", "var bb = require ('./some_^lib');", "some-library.js", 0);
    }
    
    @Test
    public void testIssue247565_01() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o^1.obj.conf.a;", "issue247565.js", 4);
    }
    
    @Test
    public void testIssue247565_02() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.o^bj.conf.a;", "literal.js", 256);
    }

    @Test
    public void testIssue247565_03() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.obj.co^nf.a;", "literal.js", 114);
    }
    
    @Test
    public void testIssue247565_04() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.obj.conf.a^;", "literal.js", 131);
    }
    
    @Test
    public void testIssue247565_05() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.pok^us.getDay();", "literal.js", 233);
    }
    
    @Test
    public void testIssue247565_06() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.obj.do^b.getMilliseconds();", "literal.js", 49);
    }
    
    @Test
    public void testIssue247565_07() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.obj.he^llo();", "literal.js", 76);
    }
    
    @Test
    public void testIssue247565_08() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o1.obj.ni^ck;", "literal.js", 29);
    }
    
    @Test
    public void testIssue247565_09() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.ob^j.conf.a;", "literalRef.js", 415);
    }
    
    @Test
    public void testIssue247565_10() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.obj.co^nf.a;", "literalRef.js", 303);
    }
    
    @Test
    public void testIssue247565_11() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.obj.conf.a^;", "literalRef.js", 320);
    }
    
    @Test
    public void testIssue247565_12() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.po^kus.getSeconds();", "literalRef.js", 392);
    }
    
    @Test
    public void testIssue247565_13() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.obj.d^ob.getFullYear();", "literalRef.js", 238);
    }
    
    @Test
    public void testIssue247565_14() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.obj.he^llo();", "literalRef.js", 265);
    }
    
    @Test
    public void testIssue247565_15() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247565/issue247565.js", "o2.obj.ni^ck;", "literalRef.js", 218);
    }
    
    @Test
    public void testIssue249854_01() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/app/maingt.js", "p.b^ar();//gt;5;func.js;8;10", "func.js", 105);
    }
    
    @Test
    public void testIssue249854_02() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/app/maingt.js", "console.log(p.getAtt^empt().aa);//gt;19;func.js;38;22", "func.js", 601);
    }
    
    @Test
    public void testIssue247727_01() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247727/issue247727.js", "o7.ma^rs.jejda;", "test247727.js", 148);
    }
    
    @Test
    public void testIssue247727_02() throws Exception {
        checkDeclaration("TestNavigation/public_html/js/issue247727/issue247727.js", "o7.mars.jej^da;", "test247727.js", 75);
    }
}
