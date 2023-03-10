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
package org.netbeans.modules.javascript2.knockout.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.JsFunction;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.Occurrence;
import org.netbeans.modules.javascript2.editor.model.TypeUsage;
import org.netbeans.modules.javascript2.editor.spi.model.FunctionArgument;
import org.netbeans.modules.javascript2.editor.spi.model.FunctionInterceptor;
import org.netbeans.modules.javascript2.editor.spi.model.ModelElementFactory;
import org.netbeans.modules.parsing.api.Snapshot;

/**
 *
 * @author Petr Hejl
 */
@FunctionInterceptor.Registration(priority = 200)
public class KnockoutExportInterceptor implements FunctionInterceptor {

    private static final Logger LOGGER = Logger.getLogger(KnockoutExportInterceptor.class.getName());

    private static final String GLOBAL_KO_OBJECT = "ko"; // NOI18N

    private static final Pattern NAME_PATTERN = Pattern.compile("ko\\.(exportSymbol|exportProperty)"); // NOI18N

    @Override
    public Pattern getNamePattern() {
        return NAME_PATTERN;
    }

    @Override
    public Collection<TypeUsage> intercept(Snapshot snapshot, String functionName, JsObject globalObject, DeclarationScope scope,
            ModelElementFactory factory, Collection<FunctionArgument> args) {

        if (args.size() == 3) {
            Iterator<FunctionArgument> iterator = args.iterator();
            FunctionArgument objectArgument = iterator.next();
            FunctionArgument nameArgument = iterator.next();
            FunctionArgument valueArgument = iterator.next();

            int offset = nameArgument.getOffset();

            JsObject object = null;
            if (objectArgument.getKind() == FunctionArgument.Kind.REFERENCE) {
                List<String> identifiers = (List<String>) objectArgument.getValue();
                JsObject ref = getReference(scope, identifiers, false);
                if (ref != null) {
                    JsObject found = findJsObjectByAssignment(globalObject, ref, offset);
                    if (found != null) {
                        ref = found;
                    }
                }
                object = ref;
            }
            JsObject value = null;
            if (valueArgument.getKind() == FunctionArgument.Kind.REFERENCE) {
                List<String> identifiers = (List<String>) valueArgument.getValue();
                JsObject ref = getReference(scope, identifiers, true);
                if (ref != null) {
                    JsObject found = findJsObjectByAssignment(globalObject, ref, offset);
                    if (found != null) {
                        ref = found;
                    }
                }
                value = ref;
            }
            String name = (String) nameArgument.getValue();
            OffsetRange offsetRange = new OffsetRange(offset, offset + name.length());
            if (object != null && value != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Exporting property {0} to {1} as {2}",
                            new Object[] {name, object.getFullyQualifiedName(), value.getFullyQualifiedName()});
                }
                EnumSet modifiers = EnumSet.copyOf(value.getModifiers());
                modifiers.remove(Modifier.STATIC);
                object.addProperty(name,
                        factory.newReference(object, name, offsetRange, value, true, modifiers));
            }
        } else if (args.size() == 2) {
            JsObject ko = globalObject.getProperty(GLOBAL_KO_OBJECT); // NOI18N
            if (ko == null) {
                ko = factory.newObject(globalObject, GLOBAL_KO_OBJECT, OffsetRange.NONE, true);
                globalObject.addProperty(GLOBAL_KO_OBJECT, ko);
            }

            Iterator<FunctionArgument> iterator = args.iterator();
            FunctionArgument nameArgument = iterator.next();
            FunctionArgument valueArgument = iterator.next();

            int offset = nameArgument.getOffset();
            if (nameArgument.getKind() == FunctionArgument.Kind.STRING) { // NOI18N
                JsObject parent = ko;

                String[] names = ((String) nameArgument.getValue()).split("\\."); // NOI18N
                for (int i = 0; i < names.length - 1; i++) {
                    String name = names[i];
                    if (i == 0 && GLOBAL_KO_OBJECT.equals(name)) {
                        continue;
                    }
                    JsObject child = parent.getProperty(name);
                    OffsetRange offsetRange = new OffsetRange(offset, offset + name.length());
                    if (child == null) {
                        child = factory.newObject(parent, name, offsetRange, true);
                        parent.addProperty(name, child);
                    } else if (!child.isDeclared()) {
                        JsObject newJsObject = factory.newObject(parent, name, offsetRange, true);
                        parent.addProperty(name, newJsObject);
                        for (Occurrence occurrence : child.getOccurrences()) {
                            newJsObject.addOccurrence(occurrence.getOffsetRange());
                        }
                        newJsObject.addOccurrence(child.getDeclarationName().getOffsetRange());
                        child = newJsObject;
                    }
                    parent = child;
                }

                String name = names[names.length - 1];
                if (valueArgument.getKind() == FunctionArgument.Kind.REFERENCE) {
                    List<String> identifiers = (List<String>) valueArgument.getValue();
                    JsObject value = getReference(scope, identifiers, false);
                    if (value != null) {
                        JsObject found = findJsObjectByAssignment(globalObject, value, offset);
                        if (found != null && found.isDeclared()) {
                            value = found;
                        } else {
                            int levelUp = identifiers.size() - 1;
                            JsObject foundParent = null;
                            JsObject valueParent = value.getParent();
                            while (valueParent != null) {
                                foundParent = findJsObjectByAssignment(globalObject, valueParent, offset);
                                if (foundParent != null) {
                                    break;
                                }
                                levelUp--;
                                valueParent = valueParent.getParent();
                            }
                            if (foundParent != null) {
                                boolean skip = true;
                                for (int i = levelUp; i < identifiers.size(); i++) {
                                    JsObject property = foundParent.getProperty(identifiers.get(i));
                                    if (property == null) {
                                        skip = false;
                                        break;
                                    }
                                    foundParent = property;
                                }
                                if (skip) {
                                    return Collections.emptyList();
                                }
                            }
                        }

                        OffsetRange offsetRange = new OffsetRange(offset, offset + name.length());
                        JsObject property = parent.getProperty(name);
                        if (property != null) {
                            // XXX is looks like value is artificial
                            if ((property instanceof JsFunction) && !(value instanceof JsFunction)) {
                                value = property;
                            } else {
                                Map<String, JsObject> current = new HashMap<String, JsObject>(property.getProperties());
                                current.keySet().removeAll(value.getProperties().keySet());
                                for (Map.Entry<String, JsObject> entry : current.entrySet()) {
                                    // XXX reference ?
                                    value.addProperty(entry.getKey(), entry.getValue());
                                }
                            }
                        }

                        parent.addProperty(name, factory.newReference(
                                parent, name, offsetRange, value, true, null));
                    }
                }

            }
        }
        return Collections.emptyList();
    }

    private static JsObject getReference(DeclarationScope scope,
            List<String> identifier, boolean searchPrototype) {

        if ("this".equals(identifier.get(0))) { // NOI18N
            // XXX this is not exactly right as it is evaluated at runtime
            return (JsObject) scope;
        }
        DeclarationScope currentScope = scope;
        while (currentScope != null) {
            JsObject ret = getReference((JsObject) currentScope, identifier);
            if (ret != null) {
                return ret;
            }
            currentScope = currentScope.getParentScope();
        }
        if (searchPrototype && identifier.size() > 1) {
            List<String> prototype = new ArrayList<String>(identifier);
            prototype.add(prototype.size() - 1, "prototype"); // NOI18N
            return getReference(scope, prototype, false);
        }
        return null;
    }

    private static JsObject getReference(JsObject object, List<String> identifier) {
        // XXX performance
        if (object == null) {
            return null;
        }
        if (identifier.isEmpty()) {
            return object;
        }
        return getReference(object.getProperty(identifier.get(0)),
                identifier.subList(1, identifier.size()));
    }

    private static JsObject findJsObjectByAssignment(JsObject globalObject,
            JsObject value, int offset) {

        return findJsObjectByAssignment(globalObject, value, offset, true);
    }

    private static JsObject findJsObjectByAssignment(JsObject globalObject, JsObject value,
            int offset, boolean searchPrototype) {

        if (value == null) {
            return null;
        }

        JsObject ret = null;
        Collection<? extends TypeUsage> assigments = value.getAssignmentForOffset(offset);
        if (assigments.size() == 1) {
            ret = findJsObjectByName(globalObject,
                    assigments.iterator().next().getType());
        }
        // XXX multiple assignments

        if (ret == null && searchPrototype) {
            String fqn = value.getFullyQualifiedName();
            int index = fqn.lastIndexOf('.');
            if (index > 0) {
                fqn = fqn.substring(0, index) + ".prototype" + fqn.substring(index);
                JsObject obj = findJsObjectByName(globalObject, fqn);
                if (obj != null) {
                    ret = findJsObjectByAssignment(globalObject, obj, offset, false);
                }
            }
        }
        return ret;
    }

    private static JsObject findJsObjectByName(JsObject global, String fqName) {
        JsObject result = global;
        JsObject property = result;
        for (StringTokenizer stringTokenizer = new StringTokenizer(fqName, "."); stringTokenizer.hasMoreTokens() && result != null;) {
            String token = stringTokenizer.nextToken();
            property = result.getProperty(token);
            if (property == null) {
                result = (result instanceof JsFunction)
                        ? ((JsFunction)result).getParameter(token)
                        : null;
                if (result == null) {
                    break;
                }
            } else {
                result = property;
            }
        }
        return result;
    }
}
