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
package org.netbeans.modules.php.project.ui.actions.support;

import javax.swing.JEditorPane;
import javax.swing.text.Document;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.php.api.editor.EditorSupport;
import org.netbeans.modules.php.api.editor.PhpBaseElement;
import org.netbeans.modules.php.api.editor.PhpClass;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.project.PhpProject;
import org.netbeans.modules.php.project.util.PhpProjectUtils;
import org.netbeans.modules.php.spi.testing.PhpTestingProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.util.Mutex;

public final class TestSingleMethodSupport {

    private TestSingleMethodSupport() {
    }

    public static boolean isTestClass(Node activatedNode) {
        FileObject fileObject = CommandUtils.getFileObject(activatedNode);
        if (fileObject == null) {
            return false;
        }
        PhpProject project = PhpProjectUtils.getPhpProject(fileObject);
        if (project == null) {
            return false;
        }
        if(CommandUtils.isUnderTests(project, fileObject, false)) {
            return true;
        }
        return false;
    }

    public static boolean canHandle(Node activatedNode) {
        FileObject fileObject = CommandUtils.getFileObject(activatedNode);
        if (fileObject == null) {
            return false;
        }
        PhpProject project = PhpProjectUtils.getPhpProject(fileObject);
        if (project == null) {
            return false;
        }
        final EditorCookie editorCookie = activatedNode.getLookup().lookup(EditorCookie.class);
        if (editorCookie == null) {
            return false;
        }
        JEditorPane pane = Mutex.EVENT.readAccess(new Mutex.Action<JEditorPane>() {
            @Override
            public JEditorPane run() {
                return NbDocument.findRecentEditorPane(editorCookie);
            }

        });
        if (pane == null) {
            return false;
        }
        return getTestMethod(pane.getDocument(), pane.getCaret().getDot()) != null;
    }

    public static SingleMethod getTestMethod(Document doc, int caret) {
        FileObject fileObject = NbEditorUtilities.getFileObject(doc);
        assert fileObject != null;
        EditorSupport editorSupport = Lookup.getDefault().lookup(EditorSupport.class);
        assert editorSupport != null;
        PhpBaseElement element = editorSupport.getElement(fileObject, caret);
        if (!(element instanceof PhpClass.Method)) {
            return null;
        }
        PhpClass.Method method = (PhpClass.Method) element;
        PhpProject project = PhpProjectUtils.getPhpProject(fileObject);
        assert project != null;
        PhpModule phpModule = project.getPhpModule();
        for (PhpTestingProvider testingProvider : project.getTestingProviders()) {
            if (testingProvider.isTestFile(phpModule, fileObject)
                    && testingProvider.isTestCase(phpModule, method)) {
                return new SingleMethod(fileObject, CommandUtils.encodeMethod(method.getPhpClass().getFullyQualifiedName(), method.getName()));
            }
        }
        return null;
    }

}
