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
package org.netbeans.modules.php.doctrine2.annotations.orm.parser;

import java.util.Map;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.spi.annotation.AnnotationLineParser;
import org.netbeans.modules.php.spi.annotation.AnnotationParsedLine;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class ManyToManyLineParserTest extends NbTestCase {
    private TypedParametersAnnotationLineParser parser;

    public ManyToManyLineParserTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.parser = new TypedParametersAnnotationLineParser();
    }

    public void testIsAnnotationParser() throws Exception {
        assertTrue(parser instanceof AnnotationLineParser);
    }

    public void testReturnValueIsManyToManyParsedLine_01() throws Exception {
        assertTrue(parser.parse("ManyToMany") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsManyToManyParsedLine_02() throws Exception {
        assertTrue(parser.parse("Annotations\\ManyToMany") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsManyToManyParsedLine_03() throws Exception {
        assertTrue(parser.parse("\\Foo\\Bar\\ManyToMany") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsManyToManyParsedLine_04() throws Exception {
        assertTrue(parser.parse("Annotations\\ManyToMany(targetEntity=\"Group\", inversedBy=\"features\")") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsNull() throws Exception {
        assertNull(parser.parse("ManyToManys"));
    }

    public void testValidUseCase_01() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ManyToMany");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 10));
        assertEquals("ManyToMany", type1);
    }

    public void testValidUseCase_02() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ManyToMany   ");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 10));
        assertEquals("ManyToMany", type1);
    }

    public void testValidUseCase_03() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ManyToMany\t\t  ");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 10));
        assertEquals("ManyToMany", type1);
    }

    public void testValidUseCase_04() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ManyToMany(targetEntity=\"Group\", inversedBy=\"features\")");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("(targetEntity=\"Group\", inversedBy=\"features\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(2, types.size());
        String type1 = types.get(new OffsetRange(0, 10));
        assertEquals("ManyToMany", type1);
        String type2 = types.get(new OffsetRange(25, 30));
        assertEquals("Group", type2);
    }

    public void testValidUseCase_05() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("Annotations\\ManyToMany(targetEntity=\"Group\", inversedBy=\"features\")  \t");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("(targetEntity=\"Group\", inversedBy=\"features\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(2, types.size());
        String type1 = types.get(new OffsetRange(0, 22));
        assertEquals("Annotations\\ManyToMany", type1);
        String type2 = types.get(new OffsetRange(37, 42));
        assertEquals("Group", type2);
    }

    public void testValidUseCase_06() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("\\Foo\\Bar\\ManyToMany(targetEntity=\"Group\", inversedBy=\"features\")  \t");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("(targetEntity=\"Group\", inversedBy=\"features\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(2, types.size());
        String type1 = types.get(new OffsetRange(0, 19));
        assertEquals("\\Foo\\Bar\\ManyToMany", type1);
        String type2 = types.get(new OffsetRange(34, 39));
        assertEquals("Group", type2);
    }

    public void testValidUseCase_07() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("manytomany");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 10));
        assertEquals("manytomany", type1);
    }

    public void testValidUseCase_08() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("\\Foo\\Bar\\manytomany(targetEntity=\"Group\", inversedBy=\"features\")  \t");
        assertEquals("ManyToMany", parsedLine.getName());
        assertEquals("(targetEntity=\"Group\", inversedBy=\"features\")", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(2, types.size());
        String type1 = types.get(new OffsetRange(0, 19));
        assertEquals("\\Foo\\Bar\\manytomany", type1);
        String type2 = types.get(new OffsetRange(34, 39));
        assertEquals("Group", type2);
    }

}
