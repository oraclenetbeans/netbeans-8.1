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
package org.netbeans.modules.html.angular.model;

import java.util.Arrays;
import org.netbeans.modules.html.angular.index.AngularJsIndexer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.html.angular.index.AngularJsController;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.JsArray;
import org.netbeans.modules.javascript2.editor.model.JsFunction;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.TypeUsage;
import org.netbeans.modules.javascript2.editor.spi.model.FunctionArgument;
import org.netbeans.modules.javascript2.editor.spi.model.FunctionInterceptor;
import org.netbeans.modules.javascript2.editor.spi.model.ModelElementFactory;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Petr Pisl
 */
@FunctionInterceptor.Registration(priority = 15)
public class AngularModuleInterceptor implements FunctionInterceptor{

    //private static Pattern PATTERN = Pattern.compile("angular\\.module(\\..*)*\\.controller");
    private final static Pattern PATTERN = Pattern.compile("(.)*\\.controller");
    private final static List<String> KNOWN_TYPES = Arrays.asList(
            TypeUsage.ARRAY, TypeUsage.BOOLEAN, TypeUsage.FUNCTION, TypeUsage.NULL,
            TypeUsage.NUMBER, TypeUsage.OBJECT, TypeUsage.REGEXP, TypeUsage.STRING,
            TypeUsage.UNDEFINED, TypeUsage.UNRESOLVED);
    private static final String ROUTECONFIG_PROP = "$routeConfig"; //NOI18N

    @Override
    public Pattern getNamePattern() {
        return PATTERN;
    }

    @Override
    public Collection<TypeUsage> intercept(Snapshot snapshot, String name, JsObject globalObject, DeclarationScope scope, ModelElementFactory factory, Collection<FunctionArgument> args) {
        if (!AngularJsIndexer.isScannerThread()) {
            return Collections.emptyList();
        }
        String controllerName = null;
        String functionName = null;
        int functionOffset = -1;
        int nameOffset = -1;
        String fqnOfController;
        Map<String, Integer> controllersMap = null;
        for (FunctionArgument fArgument : args) {
            switch (fArgument.getKind()) {
                case STRING :
                    if (controllerName == null) {
                        // we expect that the first string parameter is the name of the conroller
                        controllerName = (String)fArgument.getValue();
                        nameOffset = fArgument.getOffset();
                    }
                    break;
                case ARRAY:
                    // the function can be declared in ArrayLiteral like:
                    // ['$scope', 'projects', function($scope, projects) { ... }]
                    // So we go through the types of arrays, and if contains the Function type,
                    // we have the offset of the function definition
                    JsArray array = (JsArray)fArgument.getValue();
                    for (TypeUsage type : array.getTypesInArray()) {
                        if (type.getType().equals(TypeUsage.FUNCTION)) {
                            functionName = type.getType();
                            functionOffset = type.getOffset();
                            break;
                        } else if (!KNOWN_TYPES.contains(type.getType())) {
                            // e.g.: app.controller('MyCtrl', ['$scope', MyCtrl])
                            // we have to find the referenced function (MyCtrl in this case)
                            String parts[] = type.getType().split("\\."); //NOI18N
                            JsObject property = globalObject;
                            for (int i = 0; i < parts.length; i++) {
                                property = property.getProperty(parts[i]);
                                if (property == null) {
                                    break;
                                }
                            }
                            if (property != null) {
                                // we have found the referenced function
                                functionName = property.getFullyQualifiedName();
                                functionOffset = property.getOffset();
                            }
                        }
                    }
                    break;
                case REFERENCE:
                    List<String> fArgumentValue = ((List<String>) fArgument.getValue());
                    functionName = fArgumentValue.isEmpty() ? null : fArgumentValue.get(0);
                    if (functionName != null) {
                        JsObject funcObj = globalObject.getProperty(functionName);
                        JsFunction func = funcObj != null && (funcObj instanceof JsFunction) ? (JsFunction) funcObj : null;
                        if (func == null) {
                            // try to find it enclosed in IIFE
                            JsObject argumentObject = ModelUtils.findJsObject(globalObject, fArgument.getOffset());
                            if (argumentObject != null && argumentObject instanceof JsFunction) {
                                JsFunction iife = (JsFunction) argumentObject;
                                funcObj = iife.getProperty(functionName);
                                func = funcObj != null && (funcObj instanceof JsFunction) ? (JsFunction) iife.getProperty(functionName) : null;
                            }
                        }
                        if (func != null && !func.isAnonymous()) {
                            // if function isn't anonymous, use the offset of
                            // the function itself, not the argument's offset
                            functionOffset = func.getOffset();
                            break;
                        }
                    }
                    functionOffset = fArgument.getOffset();
                    break;
                case ANONYMOUS_OBJECT:
                    // controllers can be declared also like object map where
                    // the keys are the names of controllers and the values are the constructors
                    // e.g., app.controller({ MyCtrl: function($scope) { ... } });                    
                    JsObject object = (JsObject) fArgument.getValue();
                    controllersMap = new HashMap<>();
                    functionOffset = object.getOffset();
                    for (Map.Entry<String, ? extends JsObject> prop : object.getProperties().entrySet()) {
                        if (prop.getValue() instanceof JsFunction) {
                            controllersMap.put(
                                    prop.getValue().getName(),
                                    ((JsFunction) prop.getValue()).getOffset());
                        }
                    }
                    break;
                default:
            }
            if ((controllerName != null && functionName != null) || (controllerName == null && controllersMap != null)) {
                // we probably found the name of the controller and also the function definition
                // or we have found the anonymous object with the controller map
                break;
            }
        }
        if (controllerName != null && functionName != null) {
            // we need to find the function itself
            JsObject controllerDecl = ModelUtils.findJsObject(globalObject, functionOffset);
            if (controllerDecl != null
                    && !functionName.equals(TypeUsage.FUNCTION)
                    && !controllerDecl.getFullyQualifiedName().endsWith(functionName)) {
                // Probably controller function is assigned to the variable in IIFE (issue #251909)
                JsObject functProp = controllerDecl.getProperty(functionName);
                if (functProp != null && functProp.getFullyQualifiedName().endsWith(functionName)) {
                    controllerDecl = functProp;
                }
            }
            if (controllerDecl != null && controllerDecl instanceof JsFunction && controllerDecl.isDeclared()) {
                fqnOfController = controllerDecl.getFullyQualifiedName();
                FileObject fo = globalObject.getFileObject();
                if (fo != null) {
                    AngularJsIndexer.addController(fo.toURI(), new AngularJsController(controllerName, fqnOfController, fo.toURL(), nameOffset));
                }

                // index Angular "New Router" components in case of using "$routeConfig" property
                indexComponents(snapshot, fo, controllerDecl);
            }
        } else if (controllerName == null && controllersMap != null) {
            // we need to find an anonymous object, which contains the controller map
            JsObject controllerDecl = ModelUtils.findJsObject(globalObject, functionOffset);
            if (controllerDecl != null && controllerDecl instanceof JsObject && controllerDecl.isDeclared()) {
                FileObject fo = globalObject.getFileObject();
                for (Map.Entry<String, Integer> controller : controllersMap.entrySet()) {
                    fqnOfController = controllerDecl.getFullyQualifiedName() + "." + controller.getKey(); //NOI18N
                    if (fo != null) {
                        AngularJsIndexer.addController(fo.toURI(), new AngularJsController(controller.getKey(), fqnOfController, fo.toURL(), controller.getValue()));
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Indexes component if registered using $routeConfig property. E.g.:
     * AppController.$routeConfig = [{ path: '/users/posts', components: {left:
     * 'users', right: 'posts'} }];
     *
     * @param snapshot
     * @param fo
     * @param controllerDecl
     */
    private void indexComponents(Snapshot snapshot, FileObject fo, JsObject controllerDecl) {
        JsObject routerConfig = controllerDecl.getProperty(ROUTECONFIG_PROP);
        if (routerConfig != null && routerConfig instanceof JsArray) {
            Collection<? extends TypeUsage> assignments = routerConfig.getAssignments();
            if (assignments.size() == 1 && assignments.iterator().next().getType().equals(TypeUsage.ARRAY)) {
                int routerConfigOffset = routerConfig.getOffset();
                TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(snapshot, routerConfigOffset);
                if (ts != null && fo != null) {
                    AngularConfigInterceptor.saveComponentsToIndex(fo, AngularConfigInterceptor.findComponents(ts, routerConfigOffset));
                }
            }
        }
    }

}
