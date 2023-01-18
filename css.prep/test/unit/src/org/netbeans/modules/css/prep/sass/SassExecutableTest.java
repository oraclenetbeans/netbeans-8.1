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
package org.netbeans.modules.css.prep.sass;

import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.css.prep.util.VersionOutputProcessorFactory;


public class SassExecutableTest extends NbTestCase {

    private static final VersionOutputProcessorFactory VERSION_OUTPUT_PROCESSOR_FACTORY
            = new VersionOutputProcessorFactory(SassExecutable.VERSION_PATTERN);

    public SassExecutableTest(String name) {
        super(name);
    }

    public void testParseValidVersions() {
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.2.9 (Media Mark)"));
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("SASS 3.2.9 (Media Mark)"));
        assertEquals("3.3.0", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.3.0 (Media Mark)"));
        assertEquals("3.3.0", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.3.0.alpha.198"));
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.2.9a (Media Mark)"));
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.2.9-upd10 (Media Mark)"));
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.2.9 patch 3 (Media Mark)"));
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.2.9"));
        assertEquals("3.2.9.1.25", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 3.2.9.1.25"));
        assertEquals("3.2.9", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass  3.2.9    (Media Mark)"));
        assertEquals("1", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 1 (Media Mark)"));
        assertEquals("1.0", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 1.0 (Media Mark)"));
        assertEquals("1.0", VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass 1.0,25 (Media Mark)"));
    }

    public void testParseInvalidVersions() {
        assertNull(VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("3.2.9 (Media Mark)"));
        assertNull(VERSION_OUTPUT_PROCESSOR_FACTORY.parseVersion("Sass-NG 3.2.9 (Media Mark)"));
    }

}
