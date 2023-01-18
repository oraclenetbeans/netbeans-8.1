/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.editor;

import java.io.IOException;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Petr Pisl
 */
public class JsStructureScannerTest extends JsTestBase {
    
    public JsStructureScannerTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void assertDescriptionMatches(FileObject fileObject,
            String description, boolean includeTestName, String ext, boolean goldenFileInTestFileDir) throws IOException {
        super.assertDescriptionMatches(fileObject, description, includeTestName, ext, true);
    }
    
    public void testFolds01() throws Exception {
        checkFolds("testfiles/simple.js");
    }
    
    public void testFolds02() throws Exception {
        checkFolds("testfiles/coloring/czechChars.js");
    }
    
    public void testIssue226142() throws Exception {
        checkFolds("testfiles/structure/issue226142.js");
    }

    public void testIssue228186() throws Exception {
        checkFolds("testfiles/structure/issue228186.js");
    }

    public void testSimpleMethodChain() throws Exception {
        checkStructure("testfiles/completion/simpleMethodChain/methodChainSimple.js");
    }
    
    public void testTypeInferenceNew() throws Exception {
        checkStructure("testfiles/completion/typeInferenceNew.js");
    }
    
    public void testGetterSettterInObjectLiteral() throws Exception {
        checkStructure("testfiles/model/getterSettterInObjectLiteral.js");
    }
    
    public void testPerson() throws Exception {
        checkStructure("testfiles/model/person.js");
    }
    
    public void testAnonymousFunction() throws Exception {
        checkStructure("testfiles/model/jQueryFragment01.js");
    }
    
    public void testIssue198032() throws Exception {
        checkStructure("testfiles/coloring/issue198032.js");
    }
    
    public void testFormatter() throws Exception {
        checkStructure("testfiles/coloring/Formatter.js");
    }
    
    public void testAssignmnets01() throws Exception {
        checkStructure("testfiles/coloring/assignments01.js");
    }
    
    public void testArrays() throws Exception {
        checkStructure("testfiles/completion/arrays/arrays1.js");
    }
    
    public void testLiteralObject01() throws Exception {
        checkStructure("testfiles/completion/resolvingThis.js");
    }
    
    public void testDisplayPrototypeProperties01() throws Exception {
        checkStructure("testfiles/coloring/issue215354.js"); 
    }
    
    public void testIssue217031() throws Exception {
        checkStructure("testfiles/completion/issue217031.js"); 
    }
    
    public void testIssue216851() throws Exception {
        checkStructure("testfiles/coloring/issue216851.js"); 
    }
    
    public void testIssue216640() throws Exception {
        checkStructure("testfiles/coloring/issue216640.js"); 
    }
    
    public void testIssue218070() throws Exception {
        checkStructure("testfiles/coloring/issue218070_01.js"); 
    }

    public void testIssue149408() throws Exception {
        checkStructure("testfiles/coloring/issue149408.js");
    }
    
    public void testIssue215764() throws Exception {
        checkStructure("testfiles/completion/general/issue215764.js");
    }
    
    public void testIssue22601() throws Exception {
        checkStructure("testfiles/completion/general/issue222601.js");
    }
    
    public void testIssue222691() throws Exception {
        checkStructure("testfiles/coloring/issue222691.js");
    }
    
    public void testIssue222852() throws Exception {
        checkStructure("testfiles/coloring/issue222852.js");
    }
    
    public void testIssue222893() throws Exception {
        checkStructure("testfiles/coloring/issue222893.js");
    }
    
    public void testIssue222910() throws Exception {
        checkStructure("testfiles/coloring/issue222910.js");
    }
    
    public void testIssue222954() throws Exception {
        checkStructure("testfiles/coloring/issue222954.js");
    }
    
    public void testIssue222977() throws Exception {
        checkStructure("testfiles/coloring/issue222977.js");
    }
    
    public void testIssue223037() throws Exception {
        checkStructure("testfiles/completion/general/issue223037.js");
    }
    
    public void testIssue223121() throws Exception {
        checkStructure("testfiles/coloring/issue223121.js");
    }
    
    public void testIssue223313() throws Exception {
        checkStructure("testfiles/coloring/issue223313.js");
    }
    
    public void testIssue223306() throws Exception {
        checkStructure("testfiles/coloring/issue223306.js");
    }
    
    public void testIssue223423() throws Exception {
        checkStructure("testfiles/coloring/issue223423.js");
    }
    
    public void testIssue223264() throws Exception {
        checkStructure("testfiles/coloring/issue223264.js");
    }
    
    public void testIssue223304() throws Exception {
        checkStructure("testfiles/coloring/issue223304.js");
    }
    
    public void testIssue217029() throws Exception {
        checkStructure("testfiles/completion/issue217029.js");
    }
    
    public void testIssue215756() throws Exception {
        checkStructure("testfiles/coloring/issue215756.js");
    }
    
    public void testIssue223699() throws Exception {
        checkStructure("testfiles/coloring/issue223699.js");
    }
    
    public void testIssue217938() throws Exception {
        checkStructure("testfiles/structure/issue217938.js");
    }
    
    public void testIssue205098() throws Exception {
        checkStructure("testfiles/structure/issue205098.js");
    }
    
    public void testIssue223814() throws Exception {
        checkStructure("testfiles/coloring/issue223814.js");
    }
    
    public void testIssue216855() throws Exception {
        checkStructure("testfiles/structure/issue216855.js");
    }
    
    public void testIssue217011() throws Exception {
        checkStructure("testfiles/structure/issue217011.js");
    }

    public void testIssue224090() throws Exception {
        checkStructure("testfiles/structure/issue224090.js");
    }
    
    public void testIssue224562() throws Exception {
        checkStructure("testfiles/coloring/issue224562.js");
    }
    
    public void testIssue225755() throws Exception {
        checkStructure("testfiles/structure/issue225755.js");
    }
    
    public void testIssue225399() throws Exception {
        checkStructure("testfiles/markoccurences/issue225399.js");
    }
    
    public void testIssue224520() throws Exception {
        checkStructure("testfiles/markoccurences/issue224520.js");
    }
    
    public void testIssue226480() throws Exception {
        checkStructure("testfiles/structure/issue226480.js");
    }
    
    public void testIssue226559() throws Exception {
        checkStructure("testfiles/structure/issue226559.js");
    }
    
    public void testIssue226930() throws Exception {
        checkStructure("testfiles/structure/issue226930.js");
    }
    
    public void testIssue227163() throws Exception {
        checkStructure("testfiles/structure/issue227153.js");
    }
    
    public void testIssue222177() throws Exception {
        checkStructure("testfiles/structure/issue222177.js");
    }
    
    public void testIssue226976() throws Exception {
        checkStructure("testfiles/structure/issue226976.js");
    }
    
    public void testIssue228564() throws Exception {
        checkStructure("testfiles/completion/issue228564.js");
    }
    
    public void testIssue222952() throws Exception {
        checkStructure("testfiles/structure/issue222952.js");
    }
    
    public void testIssue226627() throws Exception {
        checkStructure("testfiles/structure/issue226627.js");
    }
    
    public void testIssue226521() throws Exception {
        checkStructure("testfiles/completion/general/issue226521.js");
    }
    
    public void testIssue226490() throws Exception {
        checkStructure("testfiles/structure/issue226490.js");
    }
    
    public void testIssue223967() throws Exception {
        checkStructure("testfiles/completion/general/issue223967.js");
    }
    
    public void testIssue223933() throws Exception {
        checkStructure("testfiles/completion/issue223933.js");
    }
    
    public void testIssue230578() throws Exception {
        checkStructure("testfiles/structure/issue230578.js");
    }
    
    public void testIssue230709() throws Exception {
        checkStructure("testfiles/structure/issue230709.js");
    }
    
    public void testIssue230736() throws Exception {
        checkStructure("testfiles/completion/general/issue230736.js");
    }
    
    public void testIssue230784() throws Exception {
        checkStructure("testfiles/completion/general/issue230784.js");
    }
    
    public void testIssue229717() throws Exception {
        checkStructure("testfiles/model/issue229717.js");
    }
    
    public void testIssue231026() throws Exception {
        checkStructure("testfiles/structure/issue231026.js");
    }
    
    public void testIssue231048() throws Exception {
        checkStructure("testfiles/structure/issue231048.js");
    }
    
    public void testIssue231059() throws Exception {
        checkStructure("testfiles/structure/issue231059.js");
    }
    
    public void testIssue231025() throws Exception {
        checkStructure("testfiles/structure/issue231025.js");
    }
    
    public void testIssue231333() throws Exception {
        checkStructure("testfiles/structure/issue231333.js");
    }
    
    public void testIssue231292() throws Exception {
        checkStructure("testfiles/structure/issue231292.js");
    }
    
    public void testIssue231688() throws Exception {
        checkStructure("testfiles/structure/issue231688.js");
    }
    
    public void testIssue231262() throws Exception {
        checkStructure("testfiles/structure/issue231262.js");
    }
    
    public void testResolvingThis() throws Exception {
        checkStructure("testfiles/structure/resolvingThis.js");
    }
    
    public void testIssue231751() throws Exception {
        checkStructure("testfiles/structure/issue231751.js");
    }
    
    public void testIssue231841() throws Exception {
        checkStructure("testfiles/structure/issue231841.js");
    }
    
    public void testIssue231752() throws Exception {
        checkStructure("testfiles/coloring/issue231752.js");
    }
    
    public void testIssue231908() throws Exception {
        checkStructure("testfiles/structure/issue231908.js");
    }
    
    public void testIssue232549() throws Exception {
        checkStructure("testfiles/structure/issue232549.js");
    }
    
    public void testIssue232570() throws Exception {
        checkStructure("testfiles/completion/issue232570.js");
    }
    
    public void testIssue232792() throws Exception {
        checkStructure("testfiles/markoccurences/issue232792.js");
    }
    
    public void testIssue232783() throws Exception {
        checkStructure("testfiles/markoccurences/issue232804.js");
    }
    
    public void testIssue231815() throws Exception {
        checkStructure("testfiles/structure/issue231815.js");
    }
    
    public void testIssue232910() throws Exception {
        checkStructure("testfiles/structure/issue232910.js");
    }
    
    public void testIssue232920() throws Exception {
        checkStructure("testfiles/structure/issue232920.js");
    }
    
    public void testIssue232942() throws Exception {
        checkStructure("testfiles/structure/issue232942.js");
    }
    
    public void testIssue219508() throws Exception {
        checkStructure("testfiles/structure/issue219508.js");
    }
    
    public void testIssue219508_01() throws Exception {
        checkStructure("testfiles/structure/issue219508_01.js");
    }
    
    public void testIssue222179() throws Exception {
        checkStructure("testfiles/structure/issue222179.js");
    }
    
    public void testIssue233062() throws Exception {
        checkStructure("testfiles/structure/issue233062.js");
    }
    
    public void testIssue223593() throws Exception {
        checkStructure("testfiles/completion/issue223593.js");
    }
    
    public void testIssue231744() throws Exception {
        checkStructure("testfiles/structure/issue231744.js");
    }
    
    public void testIssue233173() throws Exception {
        checkStructure("testfiles/structure/issue233173.js");
    }
    
    public void testIssue228556() throws Exception {
        checkStructure("testfiles/structure/issue228556.js");
    }
    
    public void testIssue228289() throws Exception {
        checkStructure("testfiles/structure/issue228289.js");
    }
    
    public void testIssue233237() throws Exception {
        checkStructure("testfiles/structure/issue233237.js");
    }
    
    public void testIssue231697() throws Exception {
        checkStructure("testfiles/structure/issue231697.js");
    }
    
    public void testIssue233719() throws Exception {
        checkStructure("testfiles/structure/issue233719.js");
    }
    
    public void testIssue233738() throws Exception {
        checkStructure("testfiles/structure/issue233738.js");
    }
    
    public void testIssue222964() throws Exception {
        checkStructure("testfiles/markoccurences/issue222964/issue222964.js"); 
    }
    
    public void testIssue234430() throws Exception {
        checkStructure("testfiles/structure/issue234430.js");
    }
    
    public void testIssue234453() throws Exception {
        checkStructure("testfiles/structure/issue234453.js");
    }
    
    public void testIssue234371() throws Exception {
        checkStructure("testfiles/structure/issue234371.js");
    }
    
    public void testIssue233630A() throws Exception {
        checkStructure("testfiles/structure/issue233630A.js");
    }
    
    public void testIssue233630B() throws Exception {
        checkStructure("testfiles/structure/issue233630B.js");
    }
    
    public void testIssue234359() throws Exception {
        checkStructure("testfiles/structure/issue234359.js");
    }
    
    public void testIssue242408() throws Exception {
        checkStructure("testfiles/model/issue242408.js");
    }
    
    public void testIssue242454() throws Exception {
        checkStructure("testfiles/model/issue242454.js");
    }
    
    public void testIssue243449() throws Exception {
        checkStructure("testfiles/model/issue243449.js");
    }
    
    public void testIssue244973A() throws Exception {
        checkStructure("testfiles/markoccurences/issue244973A.js"); 
    }
    
    public void testIssue244973B() throws Exception {
        checkStructure("testfiles/markoccurences/issue244973B.js"); 
    }
    
    public void testIssue244344() throws Exception {
        checkStructure("testfiles/markoccurences/issue244344.js"); 
    }
    
    public void testIssue245488() throws Exception {
        checkStructure("testfiles/markoccurences/issue245488.js"); 
    }
    
    public void testIssue245519() throws Exception {
        checkStructure("testfiles/structure/issue245519.js");
    }
    
    public void testIssue241963() throws Exception {
        checkStructure("testfiles/structure/issue241963.js");
    }
    
    public void testIssue243140_01() throws Exception {
        checkStructure("testfiles/structure/issue243140_01.js");
    }
    
    public void testIssue243140_02() throws Exception {
        checkStructure("testfiles/structure/issue243140_02.js");
    }
    
    public void testIssue246896() throws Exception {
        checkStructure("testfiles/structure/issue246896.js");
    }
    
    public void testIssue247365() throws Exception {
        checkStructure("testfiles/structure/issue247365.js");
    }
    
    public void testIssue247564() throws Exception {
        checkStructure("testfiles/structure/issue247564.js");
    }
    
    public void testIssue237878() throws Exception {
        checkStructure("testfiles/completion/issue237878.js");
    }
    
    public void testIssue190645() throws Exception {
        checkStructure("testfiles/markoccurences/issue190645.js"); 
    }
    
    public void testIssue249006() throws Exception {
        checkStructure("testfiles/coloring/issue249006.js");
    }
    
    public void testIssue249119() throws Exception {
        checkStructure("testfiles/coloring/issue249119.js");
    }
    
    public void testIssue250112() throws Exception {
        checkStructure("testfiles/markoccurences/issue250112.js"); 
    }
    
    public void testIssue250110() throws Exception {
        checkStructure("testfiles/markoccurences/issue250110.js"); 
    }
    
    public void testCallBackDeclaration1() throws Exception {
        checkStructure("testfiles/markoccurences/callbackDeclaration1.js"); 
    }
    
    public void testCallBackDeclaration2() throws Exception {
        checkStructure("testfiles/markoccurences/callbackDeclaration2.js"); 
    }
    
    public void testIssue250392() throws Exception {
        checkStructure("testfiles/structure/issue250392.js");
    }
    
    public void testIssue251758() throws Exception {
        checkStructure("testfiles/structure/issue251758.js");
    }
    
    public void testIssue245528() throws Exception {
        checkStructure("testfiles/structure/issue245528.js");
    }
    
    public void testIssue238685_01() throws Exception {
        checkStructure("testfiles/model/issue238685_01.js");
    }
    
    public void testIssue252022() throws Exception {
        checkStructure("testfiles/hints/issue252022.js");
    }
    
    public void testIssue249487() throws Exception {
        checkStructure("testfiles/markoccurences/issue249487.js");
    }
    
    public void testIssue252375() throws Exception {
        checkStructure("testfiles/markoccurences/issue252375.js");
    }
    
    public void testIssue252028() throws Exception {
        checkStructure("testfiles/structure/issue252028.js");
    }
    
    public void testIssue234480() throws Exception {
        checkStructure("testfiles/structure/issue234480.js");
    }
    
    public void testIssue224796() throws Exception {
        checkStructure("testfiles/structure/issue224796.js");
    }
    
    public void testIssue243566() throws Exception {
        checkStructure("testfiles/coloring/issue243566.js");
    }
    
    public void testIssue246451() throws Exception {
        checkStructure("testfiles/coloring/issue246451.js");
    }
    
    public void testIssue245916() throws Exception {
        checkStructure("testfiles/structure/issue245916.js");
    }
    
    public void testIssue253128() throws Exception {
        checkStructure("testfiles/structure/issue253128.js");
    }
    
    public void testIssue253147() throws Exception {
        checkStructure("testfiles/structure/issue253147.js");
    }
    
    public void testIssue253129() throws Exception {
        checkStructure("testfiles/coloring/issue253129.js");
    }
    
    public void testIssue224463() throws Exception {
        checkStructure("testfiles/structure/issue224463.js");
    }
    
    public void testIssue233155() throws Exception {
        checkStructure("testfiles/structure/issue233155.js");
    }
}
