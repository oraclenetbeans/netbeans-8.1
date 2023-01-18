/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.bugs;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerOptions;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
public class Unbalanced {
    private static final String SEEN_KEY = Unbalanced.class.getName() + ".seen"; // NOI18N
    
    private static boolean isAcceptable(Element el) {
        return el != null && (el.getKind() == ElementKind.LOCAL_VARIABLE || (el.getKind() == ElementKind.FIELD && el.getModifiers().contains(Modifier.PRIVATE)));
    }
    
    private static void record(CompilationInfo info, VariableElement el, State... states) {
        Map<Element, Set<State>> cache = (Map<Element, Set<State>>)info.getCachedValue(SEEN_KEY);

        if (cache == null) {
            info.putCachedValue(SEEN_KEY, cache = new HashMap<>(), CompilationInfo.CacheClearPolicy.ON_CHANGE);
        }

        Set<State> state = cache.get(el);

        if (state == null) {
            cache.put(el, state = EnumSet.noneOf(State.class));
        }
        
        state.addAll(Arrays.asList(states));
    }

    private static ErrorDescription produceWarning(HintContext ctx, String keyBase) {
        Element el = ctx.getInfo().getTrees().getElement(ctx.getPath());

        if (el == null) return null;

        Map<Element, Set<State>> cache = (Map<Element, Set<State>>)ctx.getInfo().getCachedValue(SEEN_KEY);

        if (cache == null) return null;

        Set<State> state = cache.remove(el);

        if (state == null) return null;

        if (state.isEmpty() || state.size() == 2) return null;

        String warningKey = keyBase + state.iterator().next().name();
        String warning = NbBundle.getMessage(Array.class, warningKey, el.getSimpleName().toString());

        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), warning);
    }

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Unbalanced.Array", description = "#DESC_org.netbeans.modules.java.hints.bugs.Unbalanced.Array", category="bugs", options=Options.QUERY, suppressWarnings="MismatchedReadAndWriteOfArray")
    public static final class Array {
        private static final Set<Kind> ARRAY_WRITE = EnumSet.of(
            Kind.AND_ASSIGNMENT, Kind.ASSIGNMENT, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR,
            Kind.DIVIDE_ASSIGNMENT, Kind.LEFT_SHIFT_ASSIGNMENT, Kind.MINUS_ASSIGNMENT,
            Kind.MULTIPLY_ASSIGNMENT, Kind.OR_ASSIGNMENT, Kind.PLUS_ASSIGNMENT,
            Kind.POSTFIX_DECREMENT, Kind.POSTFIX_INCREMENT, Kind.PREFIX_DECREMENT,
            Kind.PREFIX_INCREMENT, Kind.REMAINDER_ASSIGNMENT, Kind.RIGHT_SHIFT_ASSIGNMENT,
            Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT, Kind.XOR_ASSIGNMENT
        );

        private static VariableElement testElement(HintContext ctx) {
            Element el = ctx.getInfo().getTrees().getElement(ctx.getPath());

            if (!isAcceptable(el) || el.asType().getKind() != TypeKind.ARRAY) return null;

            if (((ArrayType) el.asType()).getComponentType().getKind() == TypeKind.ARRAY) return null;

            return (VariableElement) el;
        }

        @TriggerTreeKind({Kind.IDENTIFIER, Kind.MEMBER_SELECT})
        @TriggerOptions(TriggerOptions.PROCESS_GUARDED)
        public static ErrorDescription before(HintContext ctx) {
            VariableElement var = testElement(ctx);

            if (var == null) return null;

            TreePath tp = ctx.getPath();
            
            if (tp.getParentPath().getLeaf().getKind() == Kind.ARRAY_ACCESS) {
                State accessType = State.READ;
                Tree access = tp.getParentPath().getLeaf();
                Tree assign = tp.getParentPath().getParentPath().getLeaf();
                
                switch (assign.getKind()) {
                    case ASSIGNMENT:
                        if (((AssignmentTree) assign).getVariable() == access) {
                            accessType = State.WRITE;
                        }
                        break;
                    case AND_ASSIGNMENT: case DIVIDE_ASSIGNMENT: case LEFT_SHIFT_ASSIGNMENT:
                    case MINUS_ASSIGNMENT: case MULTIPLY_ASSIGNMENT: case OR_ASSIGNMENT:
                    case PLUS_ASSIGNMENT: case REMAINDER_ASSIGNMENT: case RIGHT_SHIFT_ASSIGNMENT:
                    case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT: case XOR_ASSIGNMENT:
                        if (((CompoundAssignmentTree) assign).getVariable() == access) {
                            accessType = State.WRITE;
                        }
                        break;
                    case POSTFIX_DECREMENT: case POSTFIX_INCREMENT: case PREFIX_DECREMENT:
                    case PREFIX_INCREMENT:
                        accessType = State.WRITE;
                        break;
                }
                
                record(ctx.getInfo(), var, accessType);
            } else {
                record(ctx.getInfo(), var, State.WRITE, State.READ);
            }

            return null;
        }

        @TriggerPattern(value="$mods$ $type[] $name = $init$;")
        public static ErrorDescription after(HintContext ctx) {
            VariableElement var = testElement(ctx);

            if (var == null) return null;

            Tree parent = ctx.getPath().getParentPath().getLeaf();

            if (parent.getKind() == Kind.ENHANCED_FOR_LOOP
                && ((EnhancedForLoopTree) parent).getVariable() == ctx.getPath().getLeaf()) {
                return null;
            }
            
            TreePath init = ctx.getVariables().get("$init$");

            if (init != null) {
                boolean asWrite = true;
                
                if (init.getLeaf().getKind() == Kind.NEW_ARRAY) {
                    NewArrayTree nat = (NewArrayTree) init.getLeaf();

                    if (nat.getInitializers() == null || nat.getInitializers().isEmpty()) {
                        asWrite = false;
                    }
                }
                
                if (asWrite) {
                    record(ctx.getInfo(), var, State.WRITE);
                }
            }

            return produceWarning(ctx, "ERR_UnbalancedArray");
        }
    }

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Unbalanced.Collection", description = "#DESC_org.netbeans.modules.java.hints.bugs.Unbalanced.Collection", category="bugs", options=Options.QUERY, suppressWarnings="MismatchedQueryAndUpdateOfCollection")
    public static final class Collection {
        private static final Set<String> READ_METHODS = new HashSet<String>(Arrays.asList("get", "contains", "remove", "containsAll", "removeAll", "retain", "retainAll", "containsKey", "containsValue", "iterator", "isEmpty", "size", "toArray", "listIterator", "indexOf", "lastIndexOf"));
        private static final Set<String> WRITE_METHODS = new HashSet<String>(Arrays.asList("add", "addAll", "set"));

        private static boolean testType(CompilationInfo info, TypeMirror actualType, String superClass) {
            TypeElement juCollection = info.getElements().getTypeElement(superClass);

            if (juCollection == null) return false;

            Types t = info.getTypes();

            return t.isAssignable(t.erasure(actualType), t.erasure(juCollection.asType()));
        }

        private static VariableElement testElement(HintContext ctx) {
            TreePath tp = ctx.getPath();
            Element el = ctx.getInfo().getTrees().getElement(tp);

            if (!isAcceptable(el)) return null;

            TypeMirror actualType = ctx.getInfo().getTrees().getTypeMirror(tp);

            if (actualType == null || actualType.getKind() != TypeKind.DECLARED) return null;

            if (testType(ctx.getInfo(), actualType, "java.util.Collection") || testType(ctx.getInfo(), actualType, "java.util.Map")) {
                return (VariableElement) el;
            } else {
                return null;
            }
        }

        @TriggerTreeKind({Kind.IDENTIFIER, Kind.MEMBER_SELECT})
        @TriggerOptions(TriggerOptions.PROCESS_GUARDED)
        public static ErrorDescription before(HintContext ctx) {
            TreePath tp = ctx.getPath();
            VariableElement var = testElement(ctx);

            if (var == null) return null;

            if (tp.getParentPath().getLeaf().getKind() == Kind.MEMBER_SELECT && tp.getParentPath().getParentPath().getLeaf().getKind() == Kind.METHOD_INVOCATION) {
                String methodName = ((MemberSelectTree) tp.getParentPath().getLeaf()).getIdentifier().toString();
                if (READ_METHODS.contains(methodName)) {
                    if (tp.getParentPath().getParentPath().getParentPath().getLeaf().getKind() != Kind.EXPRESSION_STATEMENT) {
                        record(ctx.getInfo(), var, State.READ);
                    }
                    return null;
                } else if (WRITE_METHODS.contains(methodName)) {
                    if (tp.getParentPath().getParentPath().getParentPath().getLeaf().getKind() != Kind.EXPRESSION_STATEMENT) {
                        record(ctx.getInfo(), var, State.WRITE, State.READ);
                    } else {
                        record(ctx.getInfo(), var, State.WRITE);
                    }
                    return null;
                }
            }

            record(ctx.getInfo(), var, State.WRITE, State.READ);

            return null;
        }

        @TriggerPattern(value="$mods$ $type $name = $init$;")
        public static ErrorDescription after(HintContext ctx) {
            if (testElement(ctx) == null) return null;

            TreePath init = ctx.getVariables().get("$init$");

            if (init != null) {
                if (init.getLeaf().getKind() != Kind.NEW_CLASS) return null;

                NewClassTree nct = (NewClassTree) init.getLeaf();

                if (nct.getClassBody() != null || nct.getArguments().size() > 1) return null;

                if (nct.getArguments().size() == 1) {
                    TypeMirror tm = ctx.getInfo().getTrees().getTypeMirror(new TreePath(init, nct.getArguments().get(0)));

                    if (tm == null || tm.getKind() != TypeKind.INT) return null;
                }
            }

            if (   ctx.getPath().getParentPath().getLeaf().getKind() == Kind.ENHANCED_FOR_LOOP
                && ((EnhancedForLoopTree) ctx.getPath().getParentPath().getLeaf()).getVariable() == ctx.getPath().getLeaf()) {
                return null;
            }
            
            return produceWarning(ctx, "ERR_UnbalancedCollection");
        }
    }

    public enum State {
        READ, WRITE;
    }
}
