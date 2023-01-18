/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009-2012 Sun Microsystems, Inc.
 */

package org.netbeans.lib.nbjavac.services;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javadoc.JavadocClassReader;
import com.sun.tools.javadoc.Messager;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author lahvac
 */
public class AnonymousNumberingTest extends TestCase {

    public AnonymousNumberingTest(String name) {
        super(name);
    }

    public void testCorrectAnonymousIndicesForMethodInvocations() throws IOException {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public Test main(Object o) {\n" +
                      "        return new Test().main(new Runnable() {\n" +
                      "            public void run() {\n" +
                      "                throw new UnsupportedOperationException();\n" +
                      "            }\n" +
                      "        }).main(new Iterable() {\n" +
                      "            public java.util.Iterator iterator() {\n" +
                      "                throw new UnsupportedOperationException();\n" +
                      "            }\n" +
                      "        });\n" +
                      "    }\n" +
                      "}";

        JavacTaskImpl ct = Utilities.createJavac(null, Utilities.fileObjectFor(code));
        
        ct.analyze();
        
        Symtab symTab = Symtab.instance(ct.getContext());
        TypeElement first = symTab.classes.get(ct.getElements().getName("test.Test$1"));
        TypeElement second = symTab.classes.get(ct.getElements().getName("test.Test$2"));

        assertEquals("java.lang.Iterable", ((TypeElement) ((DeclaredType) first.getInterfaces().get(0)).asElement()).getQualifiedName().toString());
        assertEquals("java.lang.Runnable", ((TypeElement) ((DeclaredType) second.getInterfaces().get(0)).asElement()).getQualifiedName().toString());
    }

    public void testCorrectAnonymousIndicesForMultipleMethods() throws IOException {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public Test main1(Object o) {\n" +
                      "        new Runnable() {\n" +
                      "            public void run() {\n" +
                      "                throw new UnsupportedOperationException();\n" +
                      "            }\n" +
                      "        };" +
                      "    }" +
                      "    public Test main2(Object o) {\n" +
                      "        new Iterable() {\n" +
                      "            public java.util.Iterator iterator() {\n" +
                      "                throw new UnsupportedOperationException();\n" +
                      "            }\n" +
                      "        };\n" +
                      "    }\n" +
                      "    public Test main3(Object o) {\n" +
                      "        new java.util.ArrayList() {};\n" +
                      "    }\n" +
                      "}";

        JavacTaskImpl ct = Utilities.createJavac(null, Utilities.fileObjectFor(code));

        ct.analyze();

        Symtab symTab = Symtab.instance(ct.getContext());
        TypeElement first = symTab.classes.get(ct.getElements().getName("test.Test$1"));
        TypeElement second = symTab.classes.get(ct.getElements().getName("test.Test$2"));
        TypeElement third = symTab.classes.get(ct.getElements().getName("test.Test$3"));

        assertEquals("java.lang.Runnable", ((TypeElement) ((DeclaredType) first.getInterfaces().get(0)).asElement()).getQualifiedName().toString());
        assertEquals("java.lang.Iterable", ((TypeElement) ((DeclaredType) second.getInterfaces().get(0)).asElement()).getQualifiedName().toString());
        assertEquals("java.util.ArrayList", ((TypeElement) ((DeclaredType) third.getSuperclass()).asElement()).getQualifiedName().toString());
    }

    public void testCorrectNameForAnonymous() throws IOException {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public Test main1(Object o) {\n" +
                      "        new Runnable() {\n" +
                      "            public void run() {\n" +
                      "                throw new UnsupportedOperationException();\n" +
                      "            }\n" +
                      "        };" +
                      "        new Iterable() {\n" +
                      "            public java.util.Iterator iterator() {\n" +
                      "                new java.util.ArrayList() {};\n" +
                      "            }\n" +
                      "        };\n" +
                      "    }\n" +
                      "}";

        JavacTaskImpl ct = Utilities.createJavac(null, Utilities.fileObjectFor(code));
        
        ct.analyze();

        Symtab symTab = Symtab.instance(ct.getContext());
        TypeElement first = symTab.classes.get(ct.getElements().getName("test.Test$1"));
        TypeElement second = symTab.classes.get(ct.getElements().getName("test.Test$2"));
        TypeElement third = symTab.classes.get(ct.getElements().getName("test.Test$2$1"));

        assertEquals("java.lang.Runnable", ((TypeElement) ((DeclaredType) first.getInterfaces().get(0)).asElement()).getQualifiedName().toString());
        assertEquals("java.lang.Iterable", ((TypeElement) ((DeclaredType) second.getInterfaces().get(0)).asElement()).getQualifiedName().toString());
        assertEquals("java.util.ArrayList", ((TypeElement) ((DeclaredType) third.getSuperclass()).asElement()).getQualifiedName().toString());
    }

}
