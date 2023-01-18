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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.UiUtils;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.api.elements.ConstantElement;
import org.netbeans.modules.php.editor.api.elements.FullyQualifiedElement;
import org.netbeans.modules.php.editor.api.elements.FunctionElement;
import org.netbeans.modules.php.editor.model.ModelElement;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.UseScope;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.NavUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.ClassName;
import org.netbeans.modules.php.editor.parser.astnodes.FormalParameter;
import org.netbeans.modules.php.editor.parser.astnodes.FunctionName;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.NamespaceName;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar.Type;
import org.netbeans.modules.php.editor.parser.astnodes.StaticConstantAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticFieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultTreePathVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Radek Matous
 */
public class AddUseImportSuggestion extends SuggestionRule {

    public AddUseImportSuggestion() {
        super();
    }

    @Override
    public String getId() {
        return "AddUse.Import.Rule"; //NOI18N
    }

    @Override
    @Messages("AddUseImportRuleDesc=Add Use Import")
    public String getDescription() {
        return Bundle.AddUseImportRuleDesc();
    }

    @Override
    @Messages("AddUseImportRuleDispName=Add Use Import")
    public String getDisplayName() {
        return Bundle.AddUseImportRuleDispName();
    }

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() == null) {
            return;
        }
        FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
        if (fileObject == null || CodeUtils.isPhp52(fileObject)) {
            return;
        }
        int caretOffset = getCaretOffset();
        final BaseDocument doc = context.doc;
        OffsetRange lineBounds = VerificationUtils.createLineBounds(caretOffset, doc);
        if (lineBounds.containsInclusive(caretOffset)) {
            CheckVisitor checkVisitor = new CheckVisitor(context, doc, lineBounds);
            phpParseResult.getProgram().accept(checkVisitor);
            hints.addAll(checkVisitor.getHints());
        }
    }

    private class CheckVisitor extends DefaultTreePathVisitor {
        private final BaseDocument doc;
        private final PHPRuleContext context;
        private final Collection<Hint> hints = new ArrayList<>();
        private final OffsetRange lineBounds;

        CheckVisitor(PHPRuleContext context, BaseDocument doc, OffsetRange lineBounds) {
            this.doc = doc;
            this.lineBounds = lineBounds;
            this.context = context;
        }

        public Collection<Hint> getHints() {
            return hints;
        }

        @Override
        public void scan(ASTNode node) {
            if (node != null && (VerificationUtils.isBefore(node.getStartOffset(), lineBounds.getEnd()))) {
                super.scan(node);
            }
        }

        @Override
        public void visit(NamespaceName node) {
            if (lineBounds.containsInclusive(node.getStartOffset())) {
                NamespaceDeclaration currenNamespace = null;
                List<ASTNode> path = getPath();
                ASTNode parentNode = path.get(0);
                for (ASTNode oneNode : path) {
                    if (oneNode instanceof NamespaceDeclaration) {
                        currenNamespace = (NamespaceDeclaration) oneNode;
                    }
                }
                if (isFunctionName(parentNode)) {
                    final QualifiedName nodeName = QualifiedName.create(node);
                    if (!nodeName.getKind().isFullyQualified()) {
                        Set<FunctionElement> functions = context.getIndex().getFunctions(NameKind.exact(nodeName));
                        for (FunctionElement indexedFunction : functions) {
                            addImportHints(indexedFunction, nodeName, currenNamespace, node);
                        }
                    }
                    super.visit(node);
                } else if (isClassName(parentNode)) {
                    final QualifiedName nodeName = QualifiedName.create(node);
                    if (!nodeName.getKind().isFullyQualified()) {
                        Set<ClassElement> classes = context.getIndex().getClasses(NameKind.exact(nodeName));
                        for (ClassElement indexedClass : classes) {
                            addImportHints(indexedClass, nodeName, currenNamespace, node);
                        }
                    }
                    super.visit(node);
                } else {
                    final QualifiedName nodeName = QualifiedName.create(node);
                    if (!nodeName.getKind().isFullyQualified()) {
                        Set<ConstantElement> constants = context.getIndex().getConstants(NameKind.exact(nodeName));
                        for (ConstantElement cnst : constants) {
                            addImportHints(cnst, nodeName, currenNamespace, node);
                        }
                    }
                    super.visit(node);
                }
            }
        }

        @Override
        public void visit(Scalar node) {
            if (lineBounds.containsInclusive(node.getStartOffset())) {
                NamespaceDeclaration currenNamespace = null;
                for (ASTNode oneNode : getPath()) {
                    if (oneNode instanceof NamespaceDeclaration) {
                        currenNamespace = (NamespaceDeclaration) oneNode;
                    }
                }
                String stringValue = node.getStringValue();
                if (stringValue != null && stringValue.trim().length() > 0 && node.getScalarType() == Type.STRING && !NavUtils.isQuoted(stringValue)) {
                    final QualifiedName nodeName = QualifiedName.create(stringValue);
                    if (!nodeName.getKind().isFullyQualified()) {
                        Set<ConstantElement> constants = context.getIndex().getConstants(NameKind.exact(nodeName));
                        for (ConstantElement cnst : constants) {
                            addImportHints(cnst, nodeName, currenNamespace, node);
                        }
                    }
                }
            }
            super.visit(node);
        }

        private void addImportHints(FullyQualifiedElement idxElement, final QualifiedName nodeName, NamespaceDeclaration currenNamespace, ASTNode node) {
            final QualifiedName indexedName = idxElement.getFullyQualifiedName(); //getQualifiedName() used before
            QualifiedName importName = QualifiedName.getPrefix(indexedName, nodeName, true);

            if (importName != null && context.fileScope != null) {
                final String retvalStr = importName.toString();
                NamespaceScope currentScope = ModelUtils.getNamespaceScope(currenNamespace, context.fileScope);

                if (currentScope != null) {
                    Collection<? extends UseScope> declaredUses = currentScope.getDeclaredUses();
                    List<? extends UseScope> suitableUses = ModelUtils.filter(declaredUses, new ModelUtils.ElementFilter<UseScope>() {

                        @Override
                        public boolean isAccepted(UseScope element) {
                            return element.getName().equalsIgnoreCase(retvalStr);
                        }
                    });
                    if (suitableUses.isEmpty()) {
                        if (idxElement instanceof ClassElement || !nodeName.getKind().isUnqualified()) {
                            AddImportFix importFix = new AddImportFix(doc, currentScope, importName);
                            hints.add(new Hint(AddUseImportSuggestion.this,
                                    importFix.getDescription(),
                                    context.parserResult.getSnapshot().getSource().getFileObject(),
                                    new OffsetRange(node.getStartOffset(), node.getEndOffset()),
                                    Collections.<HintFix>singletonList(importFix), 500));
                        }

                        QualifiedName name = VariousUtils.getPreferredName(indexedName, currentScope);
                        if (name != null) {
                            ChangeNameFix changeNameFix = new ChangeNameFix(doc, node, currentScope, name, nodeName);
                            hints.add(new Hint(AddUseImportSuggestion.this,
                                    changeNameFix.getDescription(),
                                    context.parserResult.getSnapshot().getSource().getFileObject(),
                                    new OffsetRange(node.getStartOffset(), node.getEndOffset()),
                                    Collections.<HintFix>singletonList(changeNameFix), 500));
                        }
                    }

                }
            }

        }
    }

    static class AddImportFix implements HintFix {
        private final BaseDocument doc;
        private final NamespaceScope scope;
        private final QualifiedName importName;

        public AddImportFix(BaseDocument doc, NamespaceScope scope, QualifiedName importName) {
            this.doc = doc;
            this.importName = importName;
            this.scope = scope;
        }

        OffsetRange getOffsetRange() {
            return new OffsetRange(getOffset(), getOffset() + getGeneratedCode().length());
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        @Messages({
            "# {0} - Use statement",
            "AddUseImportFix_Description=Generate \"{0}\""
        })
        public String getDescription() {
            return Bundle.AddUseImportFix_Description(getGeneratedCode());
        }

        @Override
        public void implement() throws Exception {
            int templateOffset = getOffset();
            EditList edits = new EditList(doc);
            edits.replace(templateOffset, 0, "\n" + getGeneratedCode(), true, 0); //NOI18N
            edits.apply();
            UiUtils.open(scope.getFileObject(), Utilities.getRowStart(doc, getOffsetRange().getEnd()));
        }

        private String getGeneratedCode() {
            return "use " + importName.toString() + ";"; //NOI18N
            }

        private int getOffset() {
            try {
                return Utilities.getRowEnd(doc, getReferenceElement().getOffset());
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
            return 0;
        }

        private ModelElement getReferenceElement() {
            ModelElement offsetElement = null;
            Collection<? extends UseScope> declaredUses = scope.getDeclaredUses();
            for (UseScope useElement : declaredUses) {
                if (offsetElement == null || offsetElement.getOffset() < useElement.getOffset()) {
                    offsetElement = useElement;
                }
            }
            return (offsetElement != null) ? offsetElement : scope;
        }
    }

    static class ChangeNameFix implements HintFix {
        private final BaseDocument doc;
        private final ASTNode node;
        private final NamespaceScope scope;
        private final QualifiedName newName;
        private final QualifiedName oldName;

        public ChangeNameFix(BaseDocument doc, ASTNode node, NamespaceScope scope,
                QualifiedName newName, QualifiedName oldName) {
            this.doc = doc;
            this.newName = newName;
            this.oldName = oldName;
            this.scope = scope;
            this.node = node;
        }

        OffsetRange getOffsetRange() {
            return new OffsetRange(node.getStartOffset(), node.getEndOffset());
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        @Messages({
            "# {0} - Fixed name",
            "ChangeNameFix_Description=Fix Name To \"{0}\""
        })
        public String getDescription() {
            return Bundle.ChangeNameFix_Description(getGeneratedCode());
        }

        @Override
        public void implement() throws Exception {
            int templateOffset = getOffset();
            EditList edits = new EditList(doc);
            edits.replace(templateOffset, oldName.toString().length(), getGeneratedCode(), true, 0); //NOI18N
            edits.apply();
            UiUtils.open(scope.getFileObject(), Utilities.getRowStart(doc, templateOffset));
        }

        private String getGeneratedCode() {
            return newName.toString();
        }

        private int getOffset() {
            return node.getStartOffset();
        }
    }

    private static boolean isClassName(ASTNode parentNode) {
        return parentNode instanceof ClassName || parentNode instanceof FormalParameter || parentNode instanceof StaticConstantAccess
                || parentNode instanceof StaticMethodInvocation || parentNode instanceof StaticFieldAccess || parentNode instanceof ClassDeclaration;
    }

    private static boolean isFunctionName(ASTNode parentNode) {
        return parentNode instanceof FunctionName;
    }
}
