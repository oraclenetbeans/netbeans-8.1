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
package org.netbeans.modules.javascript2.editor;

/**
 *
 * @author Petr Pisl
 */
public class JsSemanticAnalyzerTest extends JsTestBase {

    public JsSemanticAnalyzerTest(String testName) {
        super(testName);
    }
    
    public void testObjectAsParam() throws Exception {
        // TODO arguments can not be handled as global 
        checkSemantic("testfiles/model/objectAsParameter.js");
    }
    
    public void testjQueryFragment01() throws Exception {
        checkSemantic("testfiles/model/jQueryFragment01.js");
    }
    
    public void testCzechChars() throws Exception {
        checkSemantic("testfiles/coloring/czechChars.js");
    }
    
    public void testGetterSetterInObjectLiteral() throws Exception {
        checkSemantic("testfiles/model/getterSettterInObjectLiteral.js");
    }
    
    public void testIssue209717_01() throws Exception {
        checkSemantic("testfiles/coloring/issue209717_01.js");
    }
    
    public void testIssue209717_02() throws Exception {
        checkSemantic("testfiles/coloring/issue209717_02.js");
    }
    
    public void testIssue209717_03() throws Exception {
        checkSemantic("testfiles/coloring/issue209717_03.js");
    }
    
    public void testIssue209717_04() throws Exception {
        checkSemantic("testfiles/coloring/issue209717_04.js");
    }
    
    public void testFormatter() throws Exception {
        checkSemantic("testfiles/coloring/Formatter.js"); 
    }
    
    public void testAssignments01() throws Exception {
        checkSemantic("testfiles/coloring/assignments01.js"); 
    }
    
    public void testIssue213968() throws Exception {
        checkSemantic("testfiles/coloring/issue213968.js"); 
    }
    
    public void testIssue215354() throws Exception {
        checkSemantic("testfiles/coloring/issue215354.js"); 
    }
    
    public void testIssue214982() throws Exception {
        checkSemantic("testfiles/coloring/issue214982.js"); 
    }
    
    public void testIssue215554() throws Exception {
        checkSemantic("testfiles/coloring/issue215554.js"); 
    }
    
    public void testIssue215755() throws Exception {
        checkSemantic("testfiles/coloring/issue215755.js"); 
    }
    
    public void testUnusedVariables01() throws Exception {
        checkSemantic("testfiles/hints/weirdAssignment.js"); 
    }
    
    public void testUnusedVariables02() throws Exception {
        checkSemantic("testfiles/coloring/unusedVariables.js"); 
    }
    
    public void testUnusedVariables03() throws Exception {
        checkSemantic("testfiles/coloring/unusedVariable02.js"); 
    }
    
    public void testUnusedVariables04() throws Exception {
        checkSemantic("testfiles/coloring/unusedVariable03.js"); 
    }
    
    public void testIssue217443() throws Exception {
        checkSemantic("testfiles/coloring/issue217443.js"); 
    }

    public void testIssue218230_01() throws Exception {
        checkSemantic("testfiles/coloring/issue218230.js");
    }

    public void testIssue218230_02() throws Exception {
        checkSemantic("testfiles/markoccurences/testDocumentation/testDocumentation.js");
    }

    public void testIssue218231() throws Exception {
        checkSemantic("testfiles/coloring/issue218231.js");
    }

    public void testIssue215839() throws Exception {
        checkSemantic("testfiles/coloring/issue215839.js");
    }

    public void testIssue137317_01() throws Exception {
        checkSemantic("testfiles/markoccurences/issue137317.js");
    }

    public void testIssue180919() throws Exception {
        checkSemantic("testfiles/coloring/issue180919.js");
    }

    public void testIssue188431() throws Exception {
        checkSemantic("testfiles/coloring/issue198431.js");
    }

    public void testIssue218561() throws Exception {
        checkSemantic("testfiles/coloring/issue218561.js");
    }

    public void testIssue219044() throws Exception {
        checkSemantic("testfiles/coloring/issue219044.js");
    }

    public void testIssue219634() throws Exception {
        checkSemantic("testfiles/coloring/issue219634.js");
    }

    public void testIssue220102() throws Exception {
        checkSemantic("testfiles/coloring/issue220102.js");
    }

    public void testIssue220735() throws Exception {
        checkSemantic("testfiles/coloring/issue220735.js");
    }

    public void testIssue220891() throws Exception {
        checkSemantic("testfiles/coloring/issue220891.js");
    }

    public void testIssue221464() throws Exception {
        checkSemantic("testfiles/coloring/issue221464.js");
    }
    
    public void testIssue222498() throws Exception {
        checkSemantic("testfiles/markoccurences/issue222498.js");
    }
    
    public void testIssue218191() throws Exception {
        checkSemantic("testfiles/markoccurences/issue218191.js");
    }
    
    public void testIssue218136() throws Exception {
        // this basically reflects also issue 222880
        checkSemantic("testfiles/markoccurences/issue218136.js");
    }
    
    public void testIssue218090() throws Exception {
        checkSemantic("testfiles/coloring/issue218090.js");
    }
    
    public void testIssue218041() throws Exception {
        checkSemantic("testfiles/coloring/issue218041.js");
    }
    
    public void testIssue223037() throws Exception {
        checkSemantic("testfiles/completion/general/issue223037.js");
    }
    
    public void testIssue218100() throws Exception {
        checkSemantic("testfiles/coloring/issue218100.js");
    }

    public void testIssue223109() throws Exception {
        checkSemantic("testfiles/coloring/issue223109.js");
    }

    public void testIssue218467() throws Exception {
        checkSemantic("testfiles/coloring/issue218467.js");
    }
    
    public void testIssue223465() throws Exception {
        checkSemantic("testfiles/markoccurences/issue223465.js");
    }
    
    public void testIssue223699() throws Exception {
        checkSemantic("testfiles/coloring/issue223699.js");
    }
    
    public void testIssue223823() throws Exception {
        checkSemantic("testfiles/markoccurences/issue223823.js");
    }
    
    public void testIssue216262() throws Exception {
        checkSemantic("testfiles/coloring/issue216262.js");
    }
    
    public void testIssue224036() throws Exception {
        checkSemantic("testfiles/coloring/issue224036.js"); 
    }
    
    public void testIssue224215() throws Exception {
        checkSemantic("testfiles/markoccurences/issue224215.js");
    }
    
    public void testIssue225399() throws Exception {
        checkSemantic("testfiles/markoccurences/issue225399.js");
    }
    
    public void testIssue224520() throws Exception {
        checkSemantic("testfiles/markoccurences/issue224520.js");
    }
    
    public void testIssue229838() throws Exception {
        checkSemantic("testfiles/coloring/issue229838.js"); 
    }
    
    public void testIssue225098() throws Exception {
        checkSemantic("testfiles/coloring/issue225098.js"); 
    }
    
    public void testArrayLiteral() throws Exception {
        checkSemantic("testfiles/completion/arrays/arrayliteral.js");
    }
    
    public void testIssue231430() throws Exception {
        checkSemantic("testfiles/coloring/issue231430.js"); 
    }
    
    public void testIssue231848() throws Exception {
        checkSemantic("testfiles/coloring/issue231848.js"); 
    }
    
    public void testIssue231752() throws Exception {
        checkSemantic("testfiles/coloring/issue231752.js"); 
    }
    
    public void testIssue231921() throws Exception {
        checkSemantic("testfiles/coloring/issue231921.js"); 
    }
    
    public void testIssue232595() throws Exception {
        checkSemantic("testfiles/markoccurences/issue232595.js"); 
    }
    
    public void testIssue212319() throws Exception {
        checkSemantic("testfiles/coloring/issue212319.js"); 
    }
    
    public void testIssue215757() throws Exception {
        checkSemantic("testfiles/coloring/issue215757.js"); 
    }
    
    public void testIssue217769() throws Exception {
        checkSemantic("testfiles/markoccurences/issue217769.js"); 
    }
    
    public void testIssue233057() throws Exception {
        checkSemantic("testfiles/structure/issue219508.js"); 
    }
    
    public void testIssue233567() throws Exception {
        checkSemantic("testfiles/coloring/issue233567.js"); 
    }
    
    public void testIssue233719() throws Exception {
        checkSemantic("testfiles/structure/issue233719.js"); 
    }
    
    public void testIssue233787() throws Exception {
        checkSemantic("testfiles/markoccurences/issue233787.js"); 
    }
    
    public void testIssue233720() throws Exception {
        checkSemantic("testfiles/markoccurences/issue233720.js"); 
    }
    
    public void testIssue222964() throws Exception {
        checkSemantic("testfiles/markoccurences/issue222964/issue222964.js"); 
    }
    
    public void testIssue234359() throws Exception {
        checkSemantic("testfiles/structure/issue234359.js"); 
    }
    
    public void testIssue235793() throws Exception {
        checkSemantic("testfiles/coloring/issue235793.js"); 
    }
    
    public void testIssue238465() throws Exception {
        checkSemantic("testfiles/coloring/issue238465.js"); 
    }
    
    public void testIssue238499() throws Exception {
        checkSemantic("testfiles/markoccurences/issue238499.js");
    }
    
    public void testIssue242408() throws Exception {
        checkSemantic("testfiles/model/issue242408.js");
    }
    
    public void testIssue242454() throws Exception {
        checkSemantic("testfiles/model/issue242454.js");
    }
    
    public void testIssue242421() throws Exception {
        checkSemantic("testfiles/markoccurences/issue242421.js");
    }
    
    public void testIssue243449() throws Exception {
        checkSemantic("testfiles/model/issue243449.js");
    }
    
    public void testIssue244973A() throws Exception {
        checkSemantic("testfiles/markoccurences/issue244973A.js"); 
    }
    
    public void testIssue244973B() throws Exception {
        checkSemantic("testfiles/markoccurences/issue244973B.js"); 
    }
    
    public void testIssue244989() throws Exception {
        checkSemantic("testfiles/coloring/issue244989.js"); 
    }
    
    public void testIssue244344() throws Exception {
        checkSemantic("testfiles/markoccurences/issue244344.js"); 
    }
    
    public void testIssue245445() throws Exception {
        checkSemantic("testfiles/markoccurences/issue245445.js"); 
    }
    
    public void testIssue246896() throws Exception {
        checkSemantic("testfiles/structure/issue246896.js");
    }
    
    public void testIssue246581() throws Exception {
        checkSemantic("testfiles/coloring/issue246581.js");
    }
    
    public void testIssue249006() throws Exception {
        checkSemantic("testfiles/coloring/issue249006.js");
    }
    
    public void testIssue249119() throws Exception {
        checkSemantic("testfiles/coloring/issue249119.js");
    }
    
    public void testIssue249619() throws Exception {
        checkSemantic("testfiles/markoccurences/issue249619.js");
    }
    
    public void testCallBackDeclaration1() throws Exception {
        checkSemantic("testfiles/markoccurences/callbackDeclaration1.js"); 
    }
    
    public void testCallBackDeclaration2() throws Exception {
        checkSemantic("testfiles/markoccurences/callbackDeclaration2.js"); 
    }
    
    public void testIssue248696_01() throws Exception {
        checkSemantic("testfiles/hints/issue248696_01.js");
    }

    public void testIssue250337() throws Exception {
        checkSemantic("testfiles/coloring/issue250337.js");
    }
    
    public void testIssue251778() throws Exception {
        checkSemantic("testfiles/coloring/issue251778.js");
    }
    
    public void testIssue251911() throws Exception {
        checkSemantic("testfiles/model/issue251911.js");
    }
    
    public void testIssue2511819() throws Exception {
        checkSemantic("testfiles/coloring/issue251819.js");
    }
    
    public void testIssue242454A() throws Exception {
        checkSemantic("testfiles/completion/issue242454A.js");
    }
    
    public void testIssue251984() throws Exception {
        checkSemantic("testfiles/markoccurences/issue251984.js");
    }
    
    public void testIssue252022() throws Exception {
        checkSemantic("testfiles/hints/issue252022.js");
    }
 
    public void testIssue249487() throws Exception {
        checkSemantic("testfiles/markoccurences/issue249487.js");
    }
    
    public void testIssue252375() throws Exception {
        checkSemantic("testfiles/markoccurences/issue252375.js");
    }
    
    public void testIssue226977_01() throws Exception {
        checkSemantic("testfiles/coloring/issue226977_01.js");
    }
    
    public void testIssue226977_02() throws Exception {
        checkSemantic("testfiles/coloring/issue226977_02.js");
    }
    
    public void testIssue252469() throws Exception {
        checkSemantic("testfiles/coloring/issue252469.js");
    }
    
    public void testIssue224075() throws Exception {
        checkSemantic("testfiles/coloring/issue224075.js");
    }
    
    public void testIssue252655() throws Exception {
        checkSemantic("testfiles/coloring/issue252655.js");
    }
    
    public void testIssue252656() throws Exception {
        checkSemantic("testfiles/coloring/issue252656.js");
    }
    
    public void testIssue243566() throws Exception {
        checkSemantic("testfiles/coloring/issue243566.js");
    }
    
    public void testIssue237914() throws Exception {
        checkSemantic("testfiles/markoccurences/issue237914.js");
    }
    
    public void testIssue246451() throws Exception {
        checkSemantic("testfiles/coloring/issue246451.js");
    }
    
    public void testIssue253128() throws Exception {
        checkSemantic("testfiles/structure/issue253128.js");
    }
    
    public void testIssue253129() throws Exception {
        checkSemantic("testfiles/coloring/issue253129.js");
    }
    
    public void testIssue253348() throws Exception {
        checkSemantic("testfiles/coloring/issue253348.js");
    }
    
    public void testIssue253736() throws Exception {
        checkSemantic("testfiles/markoccurences/issue253736.js");
    }
    
    public void testIssue255494() throws Exception {
        checkSemantic("testfiles/coloring/issue255494.js");
    }
}
