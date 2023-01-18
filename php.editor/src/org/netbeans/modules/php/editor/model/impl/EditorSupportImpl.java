/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.php.api.editor.EditorSupport;
import org.netbeans.modules.php.api.editor.PhpBaseElement;
import org.netbeans.modules.php.api.editor.PhpClass;
import org.netbeans.modules.php.api.editor.PhpFunction;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.QuerySupportFactory;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.FieldElement;
import org.netbeans.modules.php.editor.model.FileScope;
import org.netbeans.modules.php.editor.model.FunctionScope;
import org.netbeans.modules.php.editor.model.MethodScope;
import org.netbeans.modules.php.editor.model.Model;
import org.netbeans.modules.php.editor.model.ModelFactory;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.Scope;
import org.netbeans.modules.php.editor.model.TypeScope;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Pair;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Radek Matous
 */
@ServiceProvider(service = EditorSupport.class)
public class EditorSupportImpl implements EditorSupport {

    @Override
    public Collection<PhpClass> getClasses(FileObject fo) {
        final List<PhpClass> retval = new ArrayList<>();
        Source source = Source.create(fo);
        if (source != null) {
            try {
                ParserManager.parse(Collections.singleton(source), new UserTask() {
                    @Override
                    public void run(ResultIterator resultIterator) throws Exception {
                        Parser.Result pr = resultIterator.getParserResult();
                        if (pr != null && pr instanceof PHPParseResult) {
                            Model model = ModelFactory.getModel((PHPParseResult) pr);
                            FileScope fileScope = model.getFileScope();
                            Collection<? extends ClassScope> allClasses = ModelUtils.getDeclaredClasses(fileScope);
                            for (ClassScope classScope : allClasses) {
                                retval.add((PhpClass) getPhpBaseElement(classScope));
                            }
                        }
                    }
                });
            } catch (ParseException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return retval;
    }

    @Override
    public Collection<Pair<FileObject, Integer>> filesForClass(FileObject sourceRoot, PhpClass phpClass) {
        if (sourceRoot.isData()) {
            throw new IllegalArgumentException("sourceRoot must be a folder");
        }
        final List<Pair<FileObject, Integer>> retval = new ArrayList<>();
        Index indexQuery = ElementQueryFactory.createIndexQuery(QuerySupportFactory.get(sourceRoot));
        String fullyQualifiedName = phpClass.getFullyQualifiedName();
        String unqualifiedName = phpClass.getName();
        NameKind kind = fullyQualifiedName == null ? NameKind.prefix(unqualifiedName) : NameKind.exact(fullyQualifiedName);
        Set<ClassElement> classes = indexQuery.getClasses(kind);
        for (ClassElement indexedClass : classes) {
            FileObject fo = indexedClass.getFileObject();
            if (unqualifiedName.equals(indexedClass.getName()) && fo != null && fo.isValid()) {
                retval.add(Pair.of(fo, indexedClass.getOffset()));
            }
        }
        return retval;
    }

    @Override
    public PhpBaseElement getElement(FileObject fo, final int offset) {
        Source source = Source.create(fo);
        final List<PhpBaseElement> retval = new ArrayList<>(1);
        if (source != null) {
            try {
                ParserManager.parse(Collections.singleton(source), new UserTask() {
                    @Override
                    public void run(ResultIterator resultIterator) throws Exception {
                        Parser.Result pr = resultIterator.getParserResult();
                        if (pr != null && pr instanceof PHPParseResult) {
                            Model model = ModelFactory.getModel((PHPParseResult) pr);
                            retval.add(getPhpBaseElement(model.getVariableScopeForNamedElement(offset)));
                        }
                    }
                });
            } catch (ParseException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return retval.isEmpty() ? null : retval.get(0);
    }

    private PhpBaseElement getPhpBaseElement(Scope scope) {
        PhpBaseElement phpBaseElement = null;
        if (scope instanceof MethodScope) {
            PhpClass phpClass = (PhpClass) getPhpBaseElement((TypeScope) scope.getInScope());
            for (PhpClass.Method method : phpClass.getMethods()) {
                if (method.getName().equals(scope.getName())) {
                    phpBaseElement = method;
                    break;
                }
            }
        } else if (scope instanceof ClassScope) {
            ClassScope classScope = (ClassScope) scope;
            PhpClass phpClass = new PhpClass(
                    classScope.getName(),
                    classScope.getNamespaceName().append(classScope.getName()).toFullyQualified().toString(),
                    classScope.getOffset());
            for (FieldElement fieldElement : classScope.getDeclaredFields()) {
                phpClass.addField(fieldElement.getName(), fieldElement.getName(), fieldElement.getOffset());
            }
            for (MethodScope methodScope : classScope.getDeclaredMethods()) {
                phpClass.addMethod(methodScope.getName(), methodScope.getName(), methodScope.getOffset());
            }
            phpBaseElement = phpClass;
        } else if (scope instanceof FunctionScope) {
            phpBaseElement = new PhpFunction(
                    scope.getName(),
                    scope.getNamespaceName().append(scope.getName()).toFullyQualified().toString(),
                    scope.getOffset());
        }
        return phpBaseElement;
    }
}
