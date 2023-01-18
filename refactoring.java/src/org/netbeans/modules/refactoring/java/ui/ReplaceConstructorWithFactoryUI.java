/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.refactoring.java.ui;

import org.netbeans.modules.refactoring.java.api.ReplaceConstructorWithFactoryRefactoring;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.swing.event.ChangeListener;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.ui.JavaRefactoringUIFactory;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
public class ReplaceConstructorWithFactoryUI implements RefactoringUI, JavaRefactoringUIFactory {

    private ReplaceConstructorWithFactoryPanel panel;
    private ReplaceConstructorWithFactoryRefactoring refactoring;
    private String initialName;

    private ReplaceConstructorWithFactoryUI(TreePathHandle constructor, String name) {
        refactoring = new ReplaceConstructorWithFactoryRefactoring(constructor);
        initialName = name;
        
    }

    private ReplaceConstructorWithFactoryUI() {
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(ReplaceConstructorWithFactoryUI.class, "ReplaceConstructorName");    
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(ReplaceConstructorWithFactoryUI.class, "ReplaceConstructorDescription", initialName ,panel.getFactoryName());    
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public CustomRefactoringPanel getPanel(final ChangeListener parent) {
        if (panel == null) {
            panel = new ReplaceConstructorWithFactoryPanel(parent, "create");
        }
        return panel;
    }

    @Override
    public Problem setParameters() {
        refactoring.setFactoryName(panel.getFactoryName());
        return refactoring.checkParameters();
    }

    @Override
    public Problem checkParameters() {
        refactoring.setFactoryName(panel.getFactoryName());
        return refactoring.fastCheckParameters();
    }

    @Override
    public boolean hasParameters() {
        return true;
    }

    @Override
    public AbstractRefactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(ReplaceConstructorWithFactoryUI.class);
    }

    @Override
    public RefactoringUI create(CompilationInfo info, TreePathHandle[] handles, FileObject[] files, NonRecursiveFolder[] packages) {
        assert handles.length == 1;
        TreePath path = handles[0].resolve(info);

        Set<Tree.Kind> treeKinds = EnumSet.of(
                Tree.Kind.NEW_CLASS,
                Tree.Kind.METHOD);

        while (path != null && !treeKinds.contains(path.getLeaf().getKind())) {
            path = path.getParentPath();
        }
        if (path != null && treeKinds.contains(path.getLeaf().getKind())) {
            Element selected = info.getTrees().getElement(path);
            if (selected.getKind() == ElementKind.CONSTRUCTOR && selected.getEnclosingElement().getKind() != ElementKind.ENUM) {
                return new ReplaceConstructorWithFactoryUI(TreePathHandle.create(selected, info), selected.getEnclosingElement().getSimpleName().toString());
            }
        }
        return null;
    }
    
    public static JavaRefactoringUIFactory factory() {
        return new ReplaceConstructorWithFactoryUI();
    }

}
