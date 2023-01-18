/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.api.java.source;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;
import org.netbeans.api.annotations.common.CheckReturnValue;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

/**Various utilities related to the {@link TypeMirror}s.
 *
 * @see javax.lang.model.util.Types
 *
 * @since 0.6
 *
 * @author Jan Lahoda
 */
public final class TypeUtilities {

    private final CompilationInfo info;

    /** Creates a new instance of CommentUtilities */
    TypeUtilities(final CompilationInfo info) {
        assert info != null;
        this.info = info;
    }

    /**Check if type t1 can be cast to t2.
     * 
     * @param t1 cast from type
     * @param t2 cast to type
     * @return true if and only if type t1 can be cast to t2 without a compile time error
     * @throws IllegalArgumentException if the 't1' is of {@link TypeKind#EXECUTABLE EXACUTABLE},
     *         {@link TypeKind#PACKAGE PACKAGE}, {@link TypeKind#NONE NONE}, or {@link TypeKind#OTHER OTHER} kind
     * 
     * @since 0.6
     */
    public boolean isCastable(TypeMirror t1, TypeMirror t2) {
        switch(t1.getKind()) {
            case EXECUTABLE:
            case PACKAGE:
            case NONE:
            case OTHER:
                throw new IllegalArgumentException();
            default:
                return Types.instance(info.impl.getJavacTask().getContext()).isCastable((Type) t1, (Type) t2);
        }
    }
    
    /**
     * Substitute all occurrences of a type in 'from' with the corresponding type
     * in 'to' in 'type'. 'from' and 'to' lists have to be of the same length.
     * 
     * @param type in which the types should be substituted
     * @param from types to substitute
     * @param to   substitute to types
     * @return type corresponding to input 'type' with all references to any type from 'from'
     *         replaced with a corresponding type from 'to'
     * @throws IllegalArgumentException if the 'from' and 'to' lists are not of the same length
     * @since 0.36
     */
    public TypeMirror substitute(TypeMirror type, List<? extends TypeMirror> from, List<? extends TypeMirror> to) {
        if (from.size() != to.size()) {
            throw new IllegalArgumentException();
        }
        com.sun.tools.javac.util.List<Type> l1 = com.sun.tools.javac.util.List.nil();
        for (TypeMirror typeMirror : from)
            l1 = l1.prepend((Type)typeMirror);
        com.sun.tools.javac.util.List<Type> l2 = com.sun.tools.javac.util.List.nil();
        for (TypeMirror typeMirror : to)
            l2 = l2.prepend((Type)typeMirror);
        return Types.instance(info.impl.getJavacTask().getContext()).subst((Type)type, l1, l2);
    }

    /**
     * Find the type of the method descriptor associated to the functional interface.
     * 
     * @param origin functional interface type
     * @return associated method descriptor type or <code>null</code> if the <code>origin</code> is not a functional interface.
     * @since 0.112
     */
    public ExecutableType getDescriptorType(DeclaredType origin) {
        Types types = Types.instance(info.impl.getJavacTask().getContext());
        if (types.isFunctionalInterface(((Type)origin).tsym)) {
            Type dt = types.findDescriptorType((Type)origin);
            if (dt != null && dt.getKind() == TypeKind.EXECUTABLE) {
                return (ExecutableType)dt;
            }
        }
        return null;
    }
    
   /**Get textual representation of the given type.
     *
     * @param type to print
     * @param options allows to specify various adjustments to the output text
     * @return textual representation of the given type
     * @since 0.62
     */
    public @NonNull @CheckReturnValue CharSequence getTypeName(@NullAllowed TypeMirror type, @NonNull TypeNameOptions... options) {
	if (type == null)
            return ""; //NOI18N
        Set<TypeNameOptions> opt = EnumSet.noneOf(TypeNameOptions.class);
        opt.addAll(Arrays.asList(options));
        return new TypeNameVisitor(opt.contains(TypeNameOptions.PRINT_AS_VARARG)).visit(type, opt.contains(TypeNameOptions.PRINT_FQN)).toString();
    }

    /**Options for the {@link #getTypeName(javax.lang.model.type.TypeMirror, org.netbeans.api.java.source.TypeUtilities.TypeNameOptions[]) } method.
     * @since 0.62
     */
    public enum TypeNameOptions {
        /**
         * Print declared types as fully qualified names.
         */
        PRINT_FQN,
        /**
         * Print "..." instead of "[]".
         */
        PRINT_AS_VARARG;
    }

    private static final String UNKNOWN = "<unknown>"; //NOI18N
    private static final String CAPTURED_WILDCARD = "<captured wildcard>"; //NOI18N
    private static class TypeNameVisitor extends SimpleTypeVisitor6<StringBuilder,Boolean> {

        private boolean varArg;
        private boolean insideCapturedWildcard = false;

        private TypeNameVisitor(boolean varArg) {
            super(new StringBuilder());
            this.varArg = varArg;
        }

        @Override
        public StringBuilder defaultAction(TypeMirror t, Boolean p) {
            return DEFAULT_VALUE.append(t);
        }

        @Override
        public StringBuilder visitDeclared(DeclaredType t, Boolean p) {
            Element e = t.asElement();
            if (e instanceof TypeElement) {
                TypeElement te = (TypeElement)e;
                DEFAULT_VALUE.append((p ? te.getQualifiedName() : te.getSimpleName()).toString());
                Iterator<? extends TypeMirror> it = t.getTypeArguments().iterator();
                if (it.hasNext()) {
                    DEFAULT_VALUE.append("<"); //NOI18N
                    while(it.hasNext()) {
                        visit(it.next(), p);
                        if (it.hasNext())
                            DEFAULT_VALUE.append(", "); //NOI18N
                    }
                    DEFAULT_VALUE.append(">"); //NOI18N
                }
                return DEFAULT_VALUE;
            } else {
                return DEFAULT_VALUE.append(UNKNOWN); //NOI18N
            }
        }

        @Override
        public StringBuilder visitArray(ArrayType t, Boolean p) {
            boolean isVarArg = varArg;
            varArg = false;
            visit(t.getComponentType(), p);
            return DEFAULT_VALUE.append(isVarArg ? "..." : "[]"); //NOI18N
        }

        @Override
        public StringBuilder visitTypeVariable(TypeVariable t, Boolean p) {
            Element e = t.asElement();
            if (e != null) {
                String name = e.getSimpleName().toString();
                if (!CAPTURED_WILDCARD.equals(name))
                    return DEFAULT_VALUE.append(name);
            }
            DEFAULT_VALUE.append("?"); //NOI18N
            if (!insideCapturedWildcard) {
                insideCapturedWildcard = true;
                TypeMirror bound = t.getLowerBound();
                if (bound != null && bound.getKind() != TypeKind.NULL) {
                    DEFAULT_VALUE.append(" super "); //NOI18N
                    visit(bound, p);
                } else {
                    bound = t.getUpperBound();
                    if (bound != null && bound.getKind() != TypeKind.NULL) {
                        DEFAULT_VALUE.append(" extends "); //NOI18N
                        if (bound.getKind() == TypeKind.TYPEVAR)
                            bound = ((TypeVariable)bound).getLowerBound();
                        visit(bound, p);
                    }
                }
                insideCapturedWildcard = false;
            }
            return DEFAULT_VALUE;
        }

        @Override
        public StringBuilder visitWildcard(WildcardType t, Boolean p) {
            int len = DEFAULT_VALUE.length();
            DEFAULT_VALUE.append("?"); //NOI18N
            TypeMirror bound = t.getSuperBound();
            if (bound == null) {
                bound = t.getExtendsBound();
                if (bound != null) {
                    DEFAULT_VALUE.append(" extends "); //NOI18N
                    if (bound.getKind() == TypeKind.WILDCARD)
                        bound = ((WildcardType)bound).getSuperBound();
                    visit(bound, p);
                } else if (len == 0) {
                    bound = SourceUtils.getBound(t);
                    if (bound != null && (bound.getKind() != TypeKind.DECLARED || !((TypeElement)((DeclaredType)bound).asElement()).getQualifiedName().contentEquals("java.lang.Object"))) { //NOI18N
                        DEFAULT_VALUE.append(" extends "); //NOI18N
                        visit(bound, p);
                    }
                }
            } else {
                DEFAULT_VALUE.append(" super "); //NOI18N
                visit(bound, p);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public StringBuilder visitError(ErrorType t, Boolean p) {
            Element e = t.asElement();
            if (e instanceof TypeElement) {
                TypeElement te = (TypeElement)e;
                return DEFAULT_VALUE.append((p ? te.getQualifiedName() : te.getSimpleName()).toString());
            }
            return DEFAULT_VALUE;
        }

        @Override
        public StringBuilder visitUnknown(TypeMirror t, Boolean p) {
            return DEFAULT_VALUE.append("<unknown>");
        }

    }
}
