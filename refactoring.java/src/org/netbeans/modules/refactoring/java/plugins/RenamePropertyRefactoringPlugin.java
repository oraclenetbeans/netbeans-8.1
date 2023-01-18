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
package org.netbeans.modules.refactoring.java.plugins;

import com.sun.source.util.TreePath;
import java.io.IOException;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CodeStyle;
import org.netbeans.api.java.source.CodeStyleUtils;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.java.RefactoringUtils;
import org.netbeans.modules.refactoring.java.spi.JavaRefactoringPlugin;
import org.netbeans.modules.refactoring.java.ui.JavaRenameProperties;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jan Becicka
 */
public class RenamePropertyRefactoringPlugin extends JavaRefactoringPlugin {

    private RenameRefactoring refactoring;
    private TreePathHandle property;
    private CodeStyle codeStyle;
    private boolean isStatic;
    private boolean isBoolean;
    private RenameRefactoring getterDelegate;
    private RenameRefactoring setterDelegate;
    private RenameRefactoring parameterDelegate;

    /** Creates a new instance of RenamePropertyRefactoringPlugin */
    public RenamePropertyRefactoringPlugin(RenameRefactoring rename) {
        this.refactoring = rename;
        property = rename.getRefactoringSource().lookup(TreePathHandle.class);
    }

    @Override
    protected JavaSource getJavaSource(Phase phase) {
        return JavaSource.forFileObject(property.getFileObject());
    }

    @Override
    public Problem checkParameters() {
        if (!isRenameProperty()) {
            return null;
        }

        initDelegates();

        Problem p = null;
        if (getterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, getterDelegate.checkParameters());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        if (setterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, setterDelegate.checkParameters());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        if (parameterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, parameterDelegate.checkParameters());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        return p = JavaPluginUtils.chainProblems(p, super.checkParameters());
    }

    @Override
    public Problem fastCheckParameters() {
        if (!isRenameProperty()) {
            return null;
        }
        initDelegates();

        Problem p = null;
        if (getterDelegate != null) {
            String gettername = CodeStyleUtils.computeGetterName(
                                    refactoring.getNewName(), isBoolean, isStatic, codeStyle);
            getterDelegate.setNewName(gettername);
            p = JavaPluginUtils.chainProblems(p, getterDelegate.fastCheckParameters());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        if (setterDelegate != null) {
            String settername = CodeStyleUtils.computeSetterName(
                                    refactoring.getNewName(), isStatic, codeStyle);
            setterDelegate.setNewName(settername);
            p = JavaPluginUtils.chainProblems(p, setterDelegate.fastCheckParameters());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        if (parameterDelegate != null) {
            String newParam = RefactoringUtils.addParamPrefixSuffix(
                            CodeStyleUtils.removePrefixSuffix(
                            refactoring.getNewName(),
                            isStatic ? codeStyle.getStaticFieldNamePrefix() : codeStyle.getFieldNamePrefix(),
                            isStatic ? codeStyle.getStaticFieldNameSuffix() : codeStyle.getFieldNameSuffix()), codeStyle);
            parameterDelegate.setNewName(newParam);
            p = JavaPluginUtils.chainProblems(p, parameterDelegate.fastCheckParameters());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        return p = JavaPluginUtils.chainProblems(p, super.fastCheckParameters());
    }

    @Override
    protected Problem preCheck(CompilationController javac) throws IOException {
        if (!isRenameProperty()) {
            return null;
        }
        initDelegates();
        Problem p = null;
        if (getterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, getterDelegate.preCheck());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        if (setterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, setterDelegate.preCheck());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        if (parameterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, parameterDelegate.preCheck());
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        return p = JavaPluginUtils.chainProblems(p, super.preCheck(javac));
    }

    @Override
    public Problem prepare(RefactoringElementsBag reb) {
        if (!isRenameProperty()) {
            return null;
        }
        initDelegates();
        fireProgressListenerStart(AbstractRefactoring.PREPARE, 3);
        Problem p = null;
        if (getterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, getterDelegate.prepare(reb.getSession()));
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        fireProgressListenerStep();
        if (setterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, setterDelegate.prepare(reb.getSession()));
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        fireProgressListenerStep();
        if (parameterDelegate != null) {
            p = JavaPluginUtils.chainProblems(p, parameterDelegate.prepare(reb.getSession()));
            if (p != null && p.isFatal()) {
                return p;
            }
        }
        fireProgressListenerStep();

        fireProgressListenerStop();

        return p;
    }

    private boolean isRenameProperty() {
        JavaRenameProperties renameProps = refactoring.getContext().lookup(JavaRenameProperties.class);
        if (renameProps != null && renameProps.isIsRenameGettersSetters()) {
            return true;
        }
        return false;
    }

    private boolean inited = false;

    private void initDelegates() {
        if (inited) {
            return;
        }
        try {
            getJavaSource(Phase.PREPARE).runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run(CompilationController p) throws Exception {
                    p.toPhase(JavaSource.Phase.RESOLVED);
                    codeStyle = RefactoringUtils.getCodeStyle(p);
                    Element propertyElement = property.resolveElement(p);
                    isStatic = propertyElement.getModifiers().contains(Modifier.STATIC);
                    isBoolean = propertyElement.asType().getKind() == TypeKind.BOOLEAN;
                    String propName = RefactoringUtils.removeFieldPrefixSuffix(propertyElement, codeStyle);
                    String paramName = RefactoringUtils.addParamPrefixSuffix(propName, codeStyle);
                    String newParam = RefactoringUtils.addParamPrefixSuffix(
                            CodeStyleUtils.removePrefixSuffix(
                            refactoring.getNewName(),
                            isStatic ? codeStyle.getStaticFieldNamePrefix() : codeStyle.getFieldNamePrefix(),
                            isStatic ? codeStyle.getStaticFieldNameSuffix() : codeStyle.getFieldNameSuffix()), codeStyle);
                    for (ExecutableElement el : ElementFilter.methodsIn(propertyElement.getEnclosingElement().getEnclosedElements())) {
                        TreePath elPath = p.getTrees().getPath(el);
                        if(elPath == null || p.getTreeUtilities().isSynthetic(elPath)) {
                            continue;
                        } else if (RefactoringUtils.isGetter(p, el, propertyElement)) {
                            getterDelegate = new RenameRefactoring(Lookups.singleton(TreePathHandle.create(el, p)));
                            String gettername = CodeStyleUtils.computeGetterName(
                                    refactoring.getNewName(), isBoolean, isStatic, codeStyle);
                            getterDelegate.setNewName(gettername);
                            getterDelegate.setSearchInComments(refactoring.isSearchInComments());
                        } else if (RefactoringUtils.isSetter(p, el, propertyElement)) {
                            setterDelegate = new RenameRefactoring(Lookups.singleton(TreePathHandle.create(el, p)));
                            String settername = CodeStyleUtils.computeSetterName(
                                    refactoring.getNewName(), isStatic, codeStyle);
                            setterDelegate.setNewName(settername);
                            setterDelegate.setSearchInComments(refactoring.isSearchInComments());
                            VariableElement par = el.getParameters().iterator().next();
                            if (par.getSimpleName().contentEquals(paramName)) {
                                parameterDelegate = new RenameRefactoring(Lookups.singleton(TreePathHandle.create(p.getTrees().getPath(par), p)));
                                parameterDelegate.getContext().add(RenamePropertyRefactoringPlugin.this);
                                parameterDelegate.setNewName(newParam);
                                parameterDelegate.setSearchInComments(refactoring.isSearchInComments());
                            }
                        }
                    }
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        inited = true;
    }
}