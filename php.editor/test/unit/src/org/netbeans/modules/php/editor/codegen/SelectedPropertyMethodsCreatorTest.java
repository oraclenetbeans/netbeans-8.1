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
package org.netbeans.modules.php.editor.codegen;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.php.editor.PHPTestBase;
import org.netbeans.modules.php.project.api.PhpSourcePath;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class SelectedPropertyMethodsCreatorTest extends PHPTestBase {

    public SelectedPropertyMethodsCreatorTest(String testName) {
        super(testName);
    }

    @Override
    protected Map<String, ClassPath> createClassPathsForTest() {
        return Collections.singletonMap(
            PhpSourcePath.SOURCE_CP,
            ClassPathSupport.createClassPath(new FileObject[] {
                FileUtil.toFileObject(new File(getDataDir(), getTestFolderPath()))
            })
        );
    }

    private CGSInfo getCgsInfo(String caretLine) {
        assert caretLine != null;
        FileObject testFile = getTestFile(getTestPath());
        Source testSource = getTestSource(testFile);
        JTextArea ta = new JTextArea(testSource.getDocument(false));
        int caretOffset = getCaretOffset(testSource.createSnapshot().getText().toString(), caretLine);
        ta.setCaretPosition(caretOffset);
        return CGSInfo.getCGSInfo(ta);
    }

    private List<Property> selectAllProperties(List<Property> properties) {
        for (Property property : properties) {
            property.setSelected(true);
        }
        return properties;
    }

    private String getTestFolderPath() {
        return "testfiles/codegen/" + getName();
    }

    private String getTestPath() {
        return getTestFolderPath() + "/" + getName() + ".php";
    }

    private void checkResult(String result) throws Exception {
        assertDescriptionMatches(getTestPath(), result, false, ".codegen");
    }

    public void testInstancePropertyGetter() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setPublicModifier(true);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleGetters())).create(new SinglePropertyMethodCreator.SingleGetterCreator(cgsInfo)));
    }

    public void testInstancePropertySetter() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setPublicModifier(true);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleSetters())).create(new SinglePropertyMethodCreator.SingleSetterCreator(cgsInfo)));
    }

    public void testInstancePropertySetterWithFluentInterface() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setFluentSetter(true);
        cgsInfo.setPublicModifier(true);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleSetters())).create(new SinglePropertyMethodCreator.SingleSetterCreator(cgsInfo)));
    }

    public void testClassPropertyGetter() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setPublicModifier(true);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleGetters())).create(new SinglePropertyMethodCreator.SingleGetterCreator(cgsInfo)));
    }

    public void testClassPropertySetter() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setPublicModifier(true);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleSetters())).create(new SinglePropertyMethodCreator.SingleSetterCreator(cgsInfo)));
    }

    public void testClassPropertySetterWithFluentInterface() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setFluentSetter(true);
        cgsInfo.setPublicModifier(true);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleSetters())).create(new SinglePropertyMethodCreator.SingleSetterCreator(cgsInfo)));
    }

    public void testClassPropertyGetterWithoutPublic() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setPublicModifier(false);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleGetters())).create(new SinglePropertyMethodCreator.SingleGetterCreator(cgsInfo)));
    }

    public void testClassPropertySetterWithoutPublic() throws Exception {
        CGSInfo cgsInfo = getCgsInfo("^}");
        cgsInfo.setPublicModifier(false);
        checkResult(new SelectedPropertyMethodsCreator(selectAllProperties(cgsInfo.getPossibleSetters())).create(new SinglePropertyMethodCreator.SingleSetterCreator(cgsInfo)));
    }

}
