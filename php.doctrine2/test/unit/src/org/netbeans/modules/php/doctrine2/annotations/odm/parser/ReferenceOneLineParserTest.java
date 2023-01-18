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
package org.netbeans.modules.php.doctrine2.annotations.odm.parser;

import java.util.Map;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.spi.annotation.AnnotationLineParser;
import org.netbeans.modules.php.spi.annotation.AnnotationParsedLine;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class ReferenceOneLineParserTest extends NbTestCase {
    private TypedParametersAnnotationLineParser parser;

    public ReferenceOneLineParserTest(String name) {
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

    public void testReturnValueIsReferenceOneParsedLine_01() throws Exception {
        assertTrue(parser.parse("ReferenceOne") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsReferenceOneParsedLine_02() throws Exception {
        assertTrue(parser.parse("Annotations\\ReferenceOne") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsReferenceOneParsedLine_03() throws Exception {
        assertTrue(parser.parse("\\Foo\\Bar\\ReferenceOne") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsReferenceOneParsedLine_04() throws Exception {
        assertTrue(parser.parse("Annotations\\ReferenceOne(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})") instanceof AnnotationParsedLine.ParsedLine);
    }

    public void testReturnValueIsNull() throws Exception {
        assertNull(parser.parse("ReferenceOnes"));
    }

    public void testValidUseCase_01() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ReferenceOne");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 12));
        assertEquals("ReferenceOne", type1);
    }

    public void testValidUseCase_02() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ReferenceOne   ");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 12));
        assertEquals("ReferenceOne", type1);
    }

    public void testValidUseCase_03() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ReferenceOne\t\t  ");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 12));
        assertEquals("ReferenceOne", type1);
    }

    public void testValidUseCase_04() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("ReferenceOne(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(4, types.size());
        String type1 = types.get(new OffsetRange(0, 12));
        assertEquals("ReferenceOne", type1);
        String type2 = types.get(new OffsetRange(29, 43));
        assertEquals("Documents\\Item", type2);
        String type3 = types.get(new OffsetRange(114, 132));
        assertEquals("Documents\\BookItem", type3);
        String type4 = types.get(new OffsetRange(143, 161));
        assertEquals("Documents\\SongItem", type4);
    }

    public void testValidUseCase_05() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("Annotations\\ReferenceOne(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})  \t");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(4, types.size());
        String type1 = types.get(new OffsetRange(0, 24));
        assertEquals("Annotations\\ReferenceOne", type1);
        String type2 = types.get(new OffsetRange(41, 55));
        assertEquals("Documents\\Item", type2);
        String type3 = types.get(new OffsetRange(126, 144));
        assertEquals("Documents\\BookItem", type3);
        String type4 = types.get(new OffsetRange(155, 173));
        assertEquals("Documents\\SongItem", type4);
    }

    public void testValidUseCase_06() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("\\Foo\\Bar\\ReferenceOne(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})  \t");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(4, types.size());
        String type1 = types.get(new OffsetRange(0, 21));
        assertEquals("\\Foo\\Bar\\ReferenceOne", type1);
        String type2 = types.get(new OffsetRange(38, 52));
        assertEquals("Documents\\Item", type2);
        String type3 = types.get(new OffsetRange(123, 141));
        assertEquals("Documents\\BookItem", type3);
        String type4 = types.get(new OffsetRange(152, 170));
        assertEquals("Documents\\SongItem", type4);
    }

    public void testValidUseCase_07() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("referenceone");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        String type1 = types.get(new OffsetRange(0, 12));
        assertEquals("referenceone", type1);
    }

    public void testValidUseCase_08() throws Exception {
        AnnotationParsedLine parsedLine = parser.parse("\\Foo\\Bar\\referenceone(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})  \t");
        assertEquals("ReferenceOne", parsedLine.getName());
        assertEquals("(targetDocument=\"Documents\\Item\", cascade=\"all\", discriminatorField=\"type\", discriminatorMap={\"book\"=\"Documents\\BookItem\", \"song\"=\"Documents\\SongItem\"})", parsedLine.getDescription());
        Map<OffsetRange, String> types = parsedLine.getTypes();
        assertNotNull(types);
        assertEquals(4, types.size());
        String type1 = types.get(new OffsetRange(0, 21));
        assertEquals("\\Foo\\Bar\\referenceone", type1);
        String type2 = types.get(new OffsetRange(38, 52));
        assertEquals("Documents\\Item", type2);
        String type3 = types.get(new OffsetRange(123, 141));
        assertEquals("Documents\\BookItem", type3);
        String type4 = types.get(new OffsetRange(152, 170));
        assertEquals("Documents\\SongItem", type4);
    }

}
