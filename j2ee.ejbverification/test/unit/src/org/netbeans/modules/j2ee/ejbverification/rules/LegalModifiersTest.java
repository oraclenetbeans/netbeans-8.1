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
package org.netbeans.modules.j2ee.ejbverification.rules;

import static junit.framework.Assert.assertNotNull;
import org.netbeans.modules.j2ee.ejbverification.HintTestBase;
import org.netbeans.modules.j2ee.ejbverification.TestBase;

/**
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public class LegalModifiersTest extends TestBase {

    public LegalModifiersTest(String name) {
        super(name);
    }
    private static final String TEST_BEAN1 = "package test;\n"
            + "@javax.ejb.Stateless\n"
            + "class TestBean1 {\n"
            + "  public TestBean1() { }"
            + "}";
    private static final String TEST_BEAN2 = "package test;\n"
            + "@javax.ejb.Stateless\n"
            + "public abstract class TestBean2 {\n"
            + "  public TestBean2() { }"
            + "}";
    private static final String TEST_BEAN3 = "package test;\n"
            + "@javax.ejb.Stateless\n"
            + "public final class TestBean3 {\n"
            + "  public TestBean3() { }"
            + "}";
    private static final String TEST_BEAN3_MORE_CLASSES = "package test;\n"
            + "@javax.ejb.Stateless\n"
            + "public final class TestBean3 {\n"
            + "  public TestBean3() { }"
            + "}\n"
            + "@javax.ejb.Stateless\n"
            + "final class TestBean4 {\n"
            + "  public TestBean4() { }"
            + "}";

    public void testLegalModifiersNotPublic() throws Exception {
        TestModule testModule = createEjb31Module();
        assertNotNull(testModule);
        HintTestBase.create(testModule.getSources()[0])
                .input("test/TestBean1.java", TEST_BEAN1)
                .run(LegalModifiers.class)
                .assertWarnings("2:6-2:15:error:" + Bundle.LegalModifiers_BeanClassMustBePublic());
    }

    public void testLegalModifiersAbstract() throws Exception {
        TestModule testModule = createEjb31Module();
        assertNotNull(testModule);
        HintTestBase.create(testModule.getSources()[0])
                .input("test/TestBean2.java", TEST_BEAN2)
                .run(LegalModifiers.class)
                .assertWarnings("2:22-2:31:error:" + Bundle.LegalModifiers_BeanClassNotBeAbstract());
    }

    public void testLegalModifiersFinal() throws Exception {
        TestModule testModule = createEjb31Module();
        assertNotNull(testModule);
        HintTestBase.create(testModule.getSources()[0])
                .input("test/TestBean3.java", TEST_BEAN3)
                .run(LegalModifiers.class)
                .assertWarnings("2:19-2:28:error:" + Bundle.LegalModifiers_BeanClassNotBeFinal());
    }

    public void testLegalModifiersFinalMoreBeansInFile() throws Exception {
        TestModule testModule = createEjb31Module();
        assertNotNull(testModule);
        HintTestBase.create(testModule.getSources()[0])
                .input("test/TestBean3.java", TEST_BEAN3_MORE_CLASSES)
                .run(LegalModifiers.class)
                .assertWarnings("2:19-2:28:error:" + Bundle.LegalModifiers_BeanClassNotBeFinal(),
                                "5:12-5:21:error:" + Bundle.LegalModifiers_BeanClassMustBePublic(),
                                "5:12-5:21:error:" + Bundle.LegalModifiers_BeanClassNotBeFinal());
    }
}
