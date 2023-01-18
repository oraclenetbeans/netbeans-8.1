/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.declarative.idebinding;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.java.hints.declarative.Condition;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintTokenId;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintsParser;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintsParser.FixTextDescription;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.java.hints.spiimpl.Hacks;
import org.netbeans.modules.java.hints.spiimpl.Utilities;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
public class HintsTask extends ParserResultTask<Result> {

    @Override
    public void run(final Result result, SchedulerEvent event) {
        final DeclarativeHintsParser.Result res = ParserImpl.getResult(result);
        final List<ErrorDescription> errors;

        if (res != null) {
            errors = computeErrors(res, result.getSnapshot().getText(), result.getSnapshot().getSource().getFileObject());
        } else {
            errors = Collections.emptyList();
        }

        HintsController.setErrors(result.getSnapshot().getSource().getFileObject(),
                                  HintsTask.class.getName(),
                                  errors);
    }

    static List<ErrorDescription> computeErrors(@NonNull final DeclarativeHintsParser.Result res, @NonNull final CharSequence hintCode, @NonNull final FileObject file) {
        final List<ErrorDescription> errors = new LinkedList<ErrorDescription>();

        errors.addAll(res.errors);

        ClasspathInfo cpInfo = ClasspathInfo.create(file);
        
        try {
            FileObject scratch = FileUtil.createMemoryFileSystem().getRoot().createData("Scratch.java");
            
            JavaSource.create(cpInfo, scratch).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController parameter) throws Exception {
                    parameter.toPhase(Phase.RESOLVED);
                    String[] importsArray = res.importsBlock != null ? new String[] {hintCode.subSequence(res.importsBlock[0], res.importsBlock[1]).toString()} : new String[0];
                    for (HintTextDescription hd : res.hints) {
                        String code = hintCode.subSequence(hd.textStart, hd.textEnd).toString();
                        Collection<Diagnostic<? extends JavaFileObject>> parsedErrors = new LinkedList<Diagnostic<? extends JavaFileObject>>();
                        Scope s = Utilities.constructScope(parameter, conditions2Constraints(parameter, hd.conditions), Arrays.asList(importsArray));
                        Tree parsed = Utilities.parseAndAttribute(parameter, code, s, parsedErrors);

                        for (Diagnostic<? extends JavaFileObject> d : parsedErrors) {
                            errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.ERROR/*XXX*/, d.getMessage(null), file, (int) (hd.textStart + d.getStartPosition()), (int) (hd.textStart + d.getEndPosition())));
                        }
                        
                        if (parsed != null && ExpressionTree.class.isAssignableFrom(parsed.getKind().asInterface())) {
                            TypeMirror type = parameter.getTrees().getTypeMirror(new TreePath(new TreePath(parameter.getCompilationUnit()), parsed));
                            
                            if (type != null && !VOID_LIKE.contains(type.getKind())) {
                                for (FixTextDescription df : hd.fixes) {
                                    String fixCode = hintCode.subSequence(df.fixSpan[0], df.fixSpan[1]).toString().trim();

                                    if (fixCode.isEmpty()) {
                                        errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING,
                                                                                                  NbBundle.getMessage(HintsTask.class, "ERR_RemoveExpression"),
                                                                                                  file,
                                                                                                  hd.textStart,
                                                                                                  hd.textEnd));
                                    }
                                }
                            }
                        }
                    }
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return errors;
    }
    
    private static Map<String, TypeMirror> conditions2Constraints(CompilationInfo info, List<Condition> conditions) {
        Map<String, TypeMirror> constraints = new HashMap<String, TypeMirror>();

        for (Entry<String, String> e : org.netbeans.modules.java.hints.declarative.Utilities.conditions2Constraints(conditions).entrySet()) {
            TypeMirror designedType = Hacks.parseFQNType(info, e.getValue());

            if (designedType == null || designedType.getKind() == TypeKind.ERROR) {
                continue ;
            }

            constraints.put(e.getKey(), designedType);
        }
        
        return constraints;
    }
    
    private static final Set<TypeKind> VOID_LIKE = EnumSet.of(TypeKind.VOID, TypeKind.ERROR, TypeKind.OTHER);
    
    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }

    @MimeRegistration(mimeType=DeclarativeHintTokenId.MIME_TYPE, service=TaskFactory.class)
    public static final class FactoryImpl extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new HintsTask());
        }
        
    }

}
