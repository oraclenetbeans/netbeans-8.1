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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.java.hints;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.Position.Bias;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.ModificationResult.Difference;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.java.source.support.CaretAwareJavaSourceTaskFactory;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.java.editor.rename.InstantRenamePerformer;
import org.netbeans.modules.java.hints.errors.Utilities;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
public class AssignResultToVariable extends AbstractHint {
    
    public AssignResultToVariable() {
        super(true, false, AbstractHint.HintSeverity.CURRENT_LINE_WARNING);
    }
    
    public Set<Kind> getTreeKinds() {
        return EnumSet.of(Kind.METHOD_INVOCATION, Kind.NEW_CLASS, Kind.BLOCK);
    }

    public List<ErrorDescription> run(CompilationInfo info, TreePath treePath) {
        try {
            int offset = CaretAwareJavaSourceTaskFactory.getLastPosition(info.getFileObject());
            boolean verifyOffset = true;
            if (treePath.getLeaf().getKind() == Kind.BLOCK) {
                StatementTree found = findStatementForgiving(info, (BlockTree) treePath.getLeaf(), offset);

                if (found == null || found.getKind() != Kind.EXPRESSION_STATEMENT)
                    return null;

                ExpressionStatementTree est = (ExpressionStatementTree) found;
                Kind innerKind = est.getExpression().getKind();

                if (innerKind != Kind.METHOD_INVOCATION && innerKind != Kind.NEW_CLASS) {
                    return null;
                }

                treePath = new TreePath(new TreePath(treePath, found), est.getExpression());
                verifyOffset = false;
            }
            
            if (treePath.getParentPath().getLeaf().getKind() != Kind.EXPRESSION_STATEMENT)
                return null;

            Tree tree = treePath.getLeaf();
            Tree exprTree = null;
            Kind kind = tree.getKind();
            if (kind == Kind.METHOD_INVOCATION) {
                exprTree = ((MethodInvocationTree)tree).getMethodSelect();
            } else if (kind == Kind.NEW_CLASS) {
                exprTree = tree;
            }

            long start = info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), exprTree);
            long end   = info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), exprTree);

            if (verifyOffset) {
                if (start == (-1) || end == (-1) || offset < start || offset > end)
                    return null;
                if (kind == Kind.NEW_CLASS) {
                    NewClassTree nct = (NewClassTree) exprTree;
                    if (nct.getClassBody() != null) {
                        long bodyStart = info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), nct.getClassBody());
                        long bodyEnd = info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), nct.getClassBody());
                        
                        if (bodyStart != (-1) && bodyEnd != (-1) && offset > bodyStart && offset <= bodyEnd)
                            return null;
                    }
                }
            }
            
            Element elem = info.getTrees().getElement(treePath);
            
            if (elem == null || (elem.getKind() != ElementKind.METHOD && elem.getKind() != ElementKind.CONSTRUCTOR)) {
                return null;
            }
            
            TypeMirror type = info.getTrees().getTypeMirror(treePath);
            
            // could use Utilities.isValidType, but NOT_ACCEPTABLE_TYPE_KINDS does the check as well
            if (type == null || NOT_ACCEPTABLE_TYPE_KINDS.contains(type.getKind())) {
                return null;
            }
            
            List<Fix> fixes = Collections.<Fix>singletonList(new FixImpl(info.getFileObject(), info.getDocument(), TreePathHandle.create(treePath, info)));
            String description = NbBundle.getMessage(AssignResultToVariable.class, "HINT_AssignResultToVariable");
            
            return Collections.singletonList(ErrorDescriptionFactory.createErrorDescription(getSeverity().toEditorSeverity(), description, fixes, info.getFileObject(), offset, offset));
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }

    private static final Set<JavaTokenId> TO_IGNORE = EnumSet.of(JavaTokenId.BLOCK_COMMENT, JavaTokenId.JAVADOC_COMMENT, JavaTokenId.LINE_COMMENT, JavaTokenId.WHITESPACE);
    
    private int findFirstNonWhitespace(CompilationInfo info, int offset, boolean previous) {
        TokenSequence<JavaTokenId> ts = info.getTokenHierarchy().tokenSequence(JavaTokenId.language());
        ts.move(offset);

        if (!ts.moveNext()) {
            return -1;
        }

        if (!TO_IGNORE.contains(ts.token().id())) return offset;
        
        do {
            JavaTokenId id = ts.token().id();
            if (TO_IGNORE.contains(id)) {
                CharSequence text = ts.token().text();
                int start = Math.max(0, previous ? 0 : offset - ts.offset());
                int end = Math.min(text.length(), previous ? offset - ts.offset() : /*TODO: not tested*/Integer.MAX_VALUE);

                for (int c = start; c < end; c++) {
                    if (text.charAt(c) == '\n') {
                        return -1;
                    }
                }
                continue;
            }
            offset = ts.offset() + (previous ? ts.token().length() : 0);
            break;
        } while (previous ? ts.movePrevious() : ts.moveNext());

        return offset;
    }

    private StatementTree findExactStatement(CompilationInfo info, BlockTree block, int offset, boolean start) {
        if (offset == (-1)) return null;
        
        SourcePositions sp = info.getTrees().getSourcePositions();
        CompilationUnitTree cut = info.getCompilationUnit();
        
        for (StatementTree t : block.getStatements()) {
            long pos = start ? sp.getStartPosition(info.getCompilationUnit(), t) : sp.getEndPosition( cut, t);

            if (offset == pos) {
                return t;
            }
        }

        return null;
    }

    private StatementTree findMatchingMethodInvocation(CompilationInfo info, BlockTree block, int offset) {
        for (StatementTree t : block.getStatements()) {
            if (t.getKind() != Kind.EXPRESSION_STATEMENT) continue;

            long statementStart = info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), t);

            if (offset < statementStart) return null;

            ExpressionStatementTree est = (ExpressionStatementTree) t;
            long statementEnd = info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), t);
            long expressionEnd = info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), est.getExpression());

            if (expressionEnd <= offset && offset < statementEnd) {
                return t;
            }
        }

        return null;
    }

    private StatementTree findStatementForgiving(CompilationInfo info, BlockTree block, int offset) {
        //<method-call>()|;
        StatementTree found = findMatchingMethodInvocation(info, block, offset);

        if (found != null) return found;

        //<method-call>();| or |<method-call>();
        StatementTree left = findExactStatement(info, block, offset, false);
        StatementTree right = findExactStatement(info, block, offset, true);

        if (left != null && right != null) {
            //cannot decide which one, stop
            return null;
        }

        if (left != null) return left;
        if (right != null) return right;

        //<method-call>;   | or |  <method-call>();
        int leftOffset = findFirstNonWhitespace(info, offset, true);
        int rightOffset = findFirstNonWhitespace(info, offset, false);
        
        left = findExactStatement(info, block, leftOffset, false);
        right = findExactStatement(info, block, rightOffset, true);

        if (left != null && right != null) {
            //cannot decide which one, stop
            return null;
        }

        return left != null ? left : right;
    }

    public void cancel() {
        // XXX implement me
    }
    
    public String getId() {
        return AssignResultToVariable.class.getName();
    }

    public String getDisplayName() {
        return NbBundle.getMessage(AssignResultToVariable.class, "DN_AssignResultToVariable");
    }

    public String getDescription() {
        return NbBundle.getMessage(AssignResultToVariable.class, "DESC_AssignResultToVariable");
    }

    private static final String VAR_TYPE_TAG = "varType";

    static final class FixImpl implements Fix, Runnable {
        
        private FileObject file;
        private Document doc;
        private TreePathHandle tph;
        private Position pos;
        
        public FixImpl(FileObject file, Document doc, TreePathHandle tph) {
            this.file = file;
            this.doc = doc;
            this.tph = tph;
        }

        public String getText() {
            return NbBundle.getMessage(AssignResultToVariable.class, "FIX_AssignResultToVariable");
        }
        
        // invoke instant rename performer
        public void run() {
            if (pos == null) {
                return;
            }
            try {
                EditorCookie cook = DataObject.find(file).getLookup().lookup(EditorCookie.class);
                JEditorPane[] arr = cook.getOpenedPanes();
                if (arr == null) {
                    return;
                }
                arr[0].setCaretPosition(pos.getOffset());
                InstantRenamePerformer.invokeInstantRename(arr[0]);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        public ChangeInfo implement() {
           try {
                final String[] name = new String[1];
                ModificationResult result = JavaSource.forFileObject(file).runModificationTask(new Task<WorkingCopy>() {
                    public void run(WorkingCopy copy) throws Exception {
                        copy.toPhase(Phase.RESOLVED);
                        
                        TreePath tp = tph.resolve(copy);
                        
                        if (tp == null) {
                            Logger.getLogger(AssignResultToVariable.class.getName()).info("tp=null"); // NOI18N
                            return ;
                        }
                        
                        TypeMirror type = copy.getTrees().getTypeMirror(tp);
                        Element el = copy.getTrees().getElement(tp);
                        
                        if (el == null || type == null || NOT_ACCEPTABLE_TYPE_KINDS.contains(type.getKind())) {
                            return ;
                        }

                        Tree t = tp.getLeaf();
                        boolean isAnonymous = false; //handle anonymous classes #138223
                        ExpressionTree identifier = null;
                        if (t instanceof NewClassTree) {
                            NewClassTree nct = ((NewClassTree)t);
                            isAnonymous = nct.getClassBody() != null || el.getKind().isInterface() || el.getModifiers().contains(Modifier.ABSTRACT);
                            identifier = nct.getIdentifier();
                        }

                        type = Utilities.resolveCapturedType(copy, type);
                        
                        TreeMaker make = copy.getTreeMaker();
                        
                        name[0] = Utilities.guessName(copy, tp);

                        Tree varType = isAnonymous ? identifier : make.Type(type);
                        VariableTree var = make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), name[0], varType, (ExpressionTree) tp.getLeaf());
                        
                        var = Utilities.copyComments(copy, tp.getParentPath().getLeaf(), var);
                        copy.tag(varType, VAR_TYPE_TAG);
                        
                        copy.rewrite(tp.getParentPath().getLeaf(), var);
                    }
                });

                result.commit();

                final int[] varTypeSpan = result.getSpan(VAR_TYPE_TAG);

                if (varTypeSpan == null) {
                    Logger.getLogger(AssignResultToVariable.class.getName()).log(Level.INFO, "Cannot resolve variable type span."); // NOI18N
                    return null;
                }
                
                final ChangeInfo[] info = new ChangeInfo[1];
                
                doc.render(new Runnable() {
                    public void run() {
                        try {
                            CharSequence text = DocumentUtilities.getText(doc, varTypeSpan[1], doc.getLength() - varTypeSpan[1]);
                            Pattern p = Pattern.compile(Pattern.quote(name[0]));
                            Matcher m = p.matcher(text);

                            if (m.find()) {
                                int startPos = varTypeSpan[1] + m.start();
                                info[0] = new ChangeInfo(pos = doc.createPosition(startPos), doc.createPosition(startPos + name[0].length()));
                            } else {
                                Logger.getLogger(AssignResultToVariable.class.getName()).log(Level.INFO, "Cannot find the name in: {0}", text.toString()); // NOI18N
                            }
                        } catch (BadLocationException e) {
                            Exceptions.printStackTrace(e);
                        }
                    }
                });
                
                SwingUtilities.invokeLater(this);
                
                return info[0];
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
            
            return null;
        }
    }

    private static final Set<TypeKind> NOT_ACCEPTABLE_TYPE_KINDS = EnumSet.of(TypeKind.ERROR, TypeKind.EXECUTABLE, TypeKind.NONE, TypeKind.NULL, TypeKind.OTHER, TypeKind.PACKAGE, TypeKind.WILDCARD, TypeKind.VOID);
    
}
