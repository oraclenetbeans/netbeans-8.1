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
package org.netbeans.modules.javascript2.editor.jsdoc;

import org.netbeans.modules.javascript2.editor.*;

/**
 *
 * @author Martin Fousek <marfous@oracle.com>
 */
public class JsDocCodeCompletionTest extends JsCodeCompletionBase {

    public JsDocCodeCompletionTest(String testName) {
        super(testName);
    }
  
    public void testAllCompletion() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @^param {int} One The first number to add", false);
    }

    public void testNoCompletion() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * ^@param {int} One The first number to add", false);
    }

    public void testParamCompletion2() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @p^aram {int} One The first number to add", false);
    }

    public void testParamCompletion3() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @pa^ram {int} One The first number to add", false);
    }

    public void testParamCompletion4() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @par^am {int} One The first number to add", false);
    }

    public void testParamCompletion5() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @para^m {int} One The first number to add", false);
    }

    public void testParamCompletion6() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @param^ {int} One The first number to add", false);
    }

    public void testMethodCompletion1() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @m^e", false);
    }

    public void testMethodCompletion2() throws Exception {
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", " * @me^", false);
    }

    public void testReturnCompletion() throws Exception {
        // @return tag should still offer @return and @returns entries
        checkCompletion("testfiles/jsdoc/classWithJsDoc.js", "* @return^ {Shape|Coordinate} A new shape.", false);
    }

}
