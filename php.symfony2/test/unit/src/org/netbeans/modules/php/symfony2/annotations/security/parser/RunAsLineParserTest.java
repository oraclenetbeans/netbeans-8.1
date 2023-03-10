/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.symfony2.annotations.security.parser;

import java.util.Map;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.spi.annotation.AnnotationLineParser;
import org.netbeans.modules.php.spi.annotation.AnnotationParsedLine;
import org.netbeans.modules.php.spi.annotation.AnnotationParsedLine.ParsedLine;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class RunAsLineParserTest extends NbTestCase {
    private AnnotationLineParser parser;

    public RunAsLineParserTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.parser = Symfony2SecurityAnnotationLineParser.getDefault();
    }

    public void testIsAnnotationParser() throws Exception {
        assertTrue(parser instanceof AnnotationLineParser);
    }

    public void testReturnValueIsRunAsParsedLine_01() throws Exception {
        assertTrue(parser.parse("RunAs") instanceof ParsedLine);
    }

    public void testReturnValueIsRunAsParsedLine_02() throws Exception {
        assertTrue(parser.parse("Annotations\\RunAs") instanceof ParsedLine);
    }

    public void testReturnValueIsRunAsParsedLine_03() throws Exception {
        assertTrue(parser.parse("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\RunAs") instanceof ParsedLine);
    }

    public void testReturnValueIsRunAsParsedLine_04() throws Exception {
        assertTrue(parser.parse("Annotations\\RunAs(roles=\"ROLE_PRIVATE_SERVICE\")") instanceof ParsedLine);
    }

    public void testReturnValueIsNull() throws Exception {
        assertNull(parser.parse("RunAss"));
    }

    public void testValidUseCase_01() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("RunAs");
        assertEquals("RunAs", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        for (Map.Entry<OffsetRange, String> entry : types.entrySet()) {
            OffsetRange offsetRange = entry.getKey();
            String value = entry.getValue();
            assertEquals(0, offsetRange.getStart());
            assertEquals(5, offsetRange.getEnd());
            assertEquals("RunAs", value);
        }
    }

    public void testValidUseCase_02() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("RunAs   ");
        assertEquals("RunAs", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        for (Map.Entry<OffsetRange, String> entry : types.entrySet()) {
            OffsetRange offsetRange = entry.getKey();
            String value = entry.getValue();
            assertEquals(0, offsetRange.getStart());
            assertEquals(5, offsetRange.getEnd());
            assertEquals("RunAs", value);
        }
    }

    public void testValidUseCase_03() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("RunAs\t\t  ");
        assertEquals("RunAs", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        for (Map.Entry<OffsetRange, String> entry : types.entrySet()) {
            OffsetRange offsetRange = entry.getKey();
            String value = entry.getValue();
            assertEquals(0, offsetRange.getStart());
            assertEquals(5, offsetRange.getEnd());
            assertEquals("RunAs", value);
        }
    }

    public void testValidUseCase_04() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("RunAs(roles=\"ROLE_PRIVATE_SERVICE\")");
        assertEquals("RunAs", parsedLine.getName());
        assertEquals("(roles=\"ROLE_PRIVATE_SERVICE\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        for (Map.Entry<OffsetRange, String> entry : types.entrySet()) {
            OffsetRange offsetRange = entry.getKey();
            String value = entry.getValue();
            assertEquals(0, offsetRange.getStart());
            assertEquals(5, offsetRange.getEnd());
            assertEquals("RunAs", value);
        }
    }

    public void testValidUseCase_05() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("Annotations\\RunAs(roles=\"ROLE_PRIVATE_SERVICE\")  \t");
        assertEquals("RunAs", parsedLine.getName());
        assertEquals("(roles=\"ROLE_PRIVATE_SERVICE\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        for (Map.Entry<OffsetRange, String> entry : types.entrySet()) {
            OffsetRange offsetRange = entry.getKey();
            String value = entry.getValue();
            assertEquals(0, offsetRange.getStart());
            assertEquals(17, offsetRange.getEnd());
            assertEquals("Annotations\\RunAs", value);
        }
    }

    public void testValidUseCase_06() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\RunAs(roles=\"ROLE_PRIVATE_SERVICE\")  \t");
        assertEquals("RunAs", parsedLine.getName());
        assertEquals("(roles=\"ROLE_PRIVATE_SERVICE\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        for (Map.Entry<OffsetRange, String> entry : types.entrySet()) {
            OffsetRange offsetRange = entry.getKey();
            String value = entry.getValue();
            assertEquals(0, offsetRange.getStart());
            assertEquals(55, offsetRange.getEnd());
            assertEquals("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\RunAs", value);
        }
    }

}
