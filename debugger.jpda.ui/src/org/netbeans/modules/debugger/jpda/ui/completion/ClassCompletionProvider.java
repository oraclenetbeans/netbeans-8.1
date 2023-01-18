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
package org.netbeans.modules.debugger.jpda.ui.completion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClassIndex.SearchScopeType;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.util.Exceptions;

/**
 *
 * @author Martin Entlicher
 */
@MimeRegistration(mimeType = JavaClassNbDebugEditorKit.MIME_TYPE, service = CompletionProvider.class)
public class ClassCompletionProvider implements CompletionProvider {
    
    //private final Set<? extends SearchScopeType> scope = Collections.singleton(new ClassSearchScopeType());

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            
            @Override
            protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
                if (caretOffset < 0) caretOffset = 0;
                String text;
                try {
                    text = doc.getText(0, caretOffset);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                    text = "";
                }
                Set<? extends SearchScopeType> scope = Collections.singleton(new ClassSearchScopeType(text));
                int n = text.length();
                ClasspathInfo cpi = getClassPathInfo();
                ClassIndex classIndex = cpi.getClassIndex();
                Set<String> packageNames = classIndex.getPackageNames(text, false, scope);
                Set<String> resultPackages = new HashSet<String>();
                int lastTextDot = text.lastIndexOf('.');
                for (String pn : packageNames) {
                    int dot = pn.indexOf('.', n);
                    if (dot > 0) pn = pn.substring(0, dot);
                    if (lastTextDot > 0) pn = pn.substring(lastTextDot + 1);
                    if (!resultPackages.contains(pn)) {
                        resultSet.addItem(new ElementCompletionItem(pn, ElementKind.PACKAGE, caretOffset));
                        resultPackages.add(pn);
                    }
                }
                
                String classFilter;
                if (lastTextDot > 0) {
                    classFilter = text.substring(lastTextDot + 1);
                } else {
                    classFilter = text;
                }
                String classFilterLC = classFilter.toLowerCase();
                Set<ElementHandle<TypeElement>> declaredTypes = classIndex.getDeclaredTypes(classFilter, ClassIndex.NameKind.CASE_INSENSITIVE_PREFIX, scope);
                Set<String> resultClasses = new HashSet<String>();
                for (ElementHandle<TypeElement> type : declaredTypes) {
                    String className = type.getQualifiedName();
                    int packageDotIndex = -1;
                    if (lastTextDot > 0) {
                        className = className.substring(lastTextDot + 1);
                        if (!className.toLowerCase().startsWith(classFilterLC)) {
                            continue;
                        }
                    } else {
                        packageDotIndex = type.getBinaryName().lastIndexOf('.');
                        if (packageDotIndex > 0) {
                            className = className.substring(packageDotIndex + 1);
                        }
                    }
                    int dot = className.indexOf('.');
                    if (dot > 0) className = className.substring(0, dot);
                    if (!resultClasses.contains(className)) {
                        ElementCompletionItem eci = new ElementCompletionItem(className, type.getKind(), caretOffset);
                        if (packageDotIndex > 0 && lastTextDot < 0) {
                            eci.setInsertPrefix(type.getQualifiedName().substring(0, packageDotIndex + 1));
                        }
                        resultSet.addItem(eci);
                        resultClasses.add(className);
                    }
                }
                resultSet.finish();
            }
        }, component);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return COMPLETION_QUERY_TYPE;
    }
    
    static ClasspathInfo getClassPathInfo() {
        DebuggerEngine engine = DebuggerManager.getDebuggerManager().getCurrentEngine();
        // TODO
        //if (engine != null)
        // Grab the class path from the engine
        
        Set<ClassPath> bootPaths = GlobalPathRegistry.getDefault().getPaths(ClassPath.BOOT);
        Set<ClassPath> classPaths = GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE);
        ClassPath cp = ClassPathSupport.createProxyClassPath(classPaths.toArray(new ClassPath[0]));
        return ClasspathInfo.create(
                ClassPathSupport.createProxyClassPath(bootPaths.toArray(new ClassPath[0])),
                cp, cp);
    }
    
}
