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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle;

/**
 *
 * @author vita
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.MultipleLoggers", description = "#DESC_org.netbeans.modules.java.hints.MultipleLoggers", category="logging", suppressWarnings={"ClassWithMultipleLoggers"}, options=Options.QUERY) //NOI18N
public final class MultipleLoggers {

    public MultipleLoggers() {
    }

    @TriggerTreeKind({Tree.Kind.ANNOTATION_TYPE, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE})
    public static Iterable<ErrorDescription> checkMultipleLoggers(HintContext ctx) {
        Element cls = ctx.getInfo().getTrees().getElement(ctx.getPath());
        if (cls == null || cls.getKind() != ElementKind.CLASS || cls.getModifiers().contains(Modifier.ABSTRACT) ||
            (cls.getEnclosingElement() != null && cls.getEnclosingElement().getKind() != ElementKind.PACKAGE)
        ) {
            return null;
        }

        TypeElement loggerTypeElement = ctx.getInfo().getElements().getTypeElement("java.util.logging.Logger"); // NOI18N
        if (loggerTypeElement == null) {
            return null;
        }
        TypeMirror loggerTypeElementAsType = loggerTypeElement.asType();
        if (loggerTypeElementAsType == null || loggerTypeElementAsType.getKind() != TypeKind.DECLARED) {
            return null;
        }

        List<VariableElement> loggerFields = new LinkedList<VariableElement>();
        List<VariableElement> fields = ElementFilter.fieldsIn(cls.getEnclosedElements());
        for(VariableElement f : fields) {
            if (f.getKind() != ElementKind.FIELD) {
                continue;
            }

            if (f.asType().equals(loggerTypeElementAsType)) {
                loggerFields.add(f);
            }
        }

        if (loggerFields.size() > 1) {
            StringBuilder loggers = new StringBuilder();
            for(VariableElement f : loggerFields) {
                Tree path = ctx.getInfo().getTrees().getTree(f);
                if (path instanceof VariableTree) {
                    int [] span = ctx.getInfo().getTreeUtilities().findNameSpan((VariableTree)path);
                    if (span != null) {
                        if (loggers.length() > 0) {
                            loggers.append(", "); //NOI18N
                        }
                        loggers.append(f.getSimpleName().toString());
                    }
                }
            }

            List<ErrorDescription> errors = new LinkedList<ErrorDescription>();
            for(VariableElement f : loggerFields) {
                Tree path = ctx.getInfo().getTrees().getTree(f);
                ErrorDescription ed = ErrorDescriptionFactory.forName(ctx, path,
                    NbBundle.getMessage(MultipleLoggers.class, "MSG_MultipleLoggers_checkMultipleLoggers", loggers, cls)); //NOI18N
                errors.add(ed);
            }
            return errors;
        } else {
            return null;
        }
    }

}
