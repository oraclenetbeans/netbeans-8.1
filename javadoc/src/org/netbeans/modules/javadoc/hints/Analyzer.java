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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.javadoc.hints;

import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.Type;
import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SerialDataTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.DocTreePathHandle;
import org.netbeans.api.java.source.ElementUtilities;
import static org.netbeans.modules.javadoc.hints.Bundle.*;
import static org.netbeans.modules.javadoc.hints.JavadocUtilities.resolveSourceVersion;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 * Checks:
 *      - missing javadoc
 *      - @param duplicate, missing, unknown
 *      - @throws duplicate, missing, unknown
 *      - @return duplicate, missing, void
 *      - if @Deprecated annotation, check for @deprecated tag
 *      - if inheritance in place check for superclass javadoc;
 *          - javadoc and its parts may be inherited
 * @see <a href="http://java.sun.com/javase/6/docs/technotes/tools/solaris/javadoc.html#inheritingcomments">Automatic Copying of Method Comments</a>
 * @see <a href="http://java.sun.com/javase/6/docs/technotes/tools/solaris/javadoc.html#javadoctags">Javadoc Tags</a>
 * @see <a href="http://java.sun.com/javase/6/docs/technotes/tools/solaris/javadoc.html#wheretags">Where Tags Can Be Used</a>
 * @see <a href="http://java.sun.com/javase/6/docs/technotes/guides/javadoc/deprecation/index.html">Deprecation of APIs</a>
 *
 * @author Jan Pokorsky
 * @author Ralph Benjamin Ruijs
 */
    @NbBundle.Messages({"MISSING_RETURN_DESC=Missing @return tag.",
                        "# {0} - @param name", "MISSING_PARAM_DESC=Missing @param tag for {0}"})
final class Analyzer extends DocTreePathScanner<Void, List<ErrorDescription>> {

    private final CompilationInfo javac;
    private final FileObject file;
    private final TreePath currentPath;
    private final SourceVersion sourceVersion;
    private final Access access;

    private Deque<StartElementTree> tagStack = new LinkedList<>();
    private Set<Element> foundParams = new HashSet<>();
    private Set<TypeMirror> foundThrows = new HashSet<>();
    private TypeMirror returnType = null;
    private boolean returnTypeFound = false;
    private boolean foundInheritDoc = false;
    private Element currentElement;
    private final HintContext ctx;

    Analyzer(CompilationInfo javac, TreePath currentPath, Access access, HintContext ctx) {
        this.javac = javac;
        this.file = javac.getFileObject();
        this.currentPath = currentPath;
        this.sourceVersion = resolveSourceVersion(javac.getFileObject());
        this.access = access;
        this.ctx = ctx;
    }

    public List<ErrorDescription> analyze() {
        if(ctx.isCanceled()) { return Collections.<ErrorDescription>emptyList(); }
        List<ErrorDescription> errors = Collections.<ErrorDescription>emptyList();
        Tree node = currentPath.getLeaf();

        if (javac.getTreeUtilities().isSynthetic(currentPath) || /*!isValid(javac, currentPath, severity, access, -1)*/
                !access.isAccessible(javac, currentPath, false)) {
            return errors;
        }

        currentElement = javac.getTrees().getElement(currentPath);
        if (currentElement == null) {
            Logger.getLogger(Analyzer.class.getName()).log(
                    Level.INFO, "Cannot resolve element for {0} in {1}", new Object[]{node, file}); // NOI18N
            return errors;
        }
        if(ctx.isCanceled()) { return Collections.<ErrorDescription>emptyList(); }
        // check javadoc
        DocCommentTree docCommentTree = javac.getDocTrees().getDocCommentTree(currentPath);
        if (docCommentTree != null) {
            errors = new ArrayList<>();
            if (node.getKind() == Tree.Kind.METHOD) {
                ExecutableElement methodElm = (ExecutableElement) currentElement;
                returnType = methodElm.getReturnType();
            }
            DocTreePath docTreePath = new DocTreePath(currentPath, docCommentTree);
            scan(docTreePath, errors);
            if(ctx.isCanceled()) { return Collections.<ErrorDescription>emptyList(); }
            Set<String> inheritedParams = new HashSet<>();
            Set<String> inheritedTypeParams = new HashSet<>();
            Set<String> inheritedThrows = new HashSet<>();
            switch (currentElement.getKind()) {
                case METHOD: {
                    ExecutableElement method = (ExecutableElement) currentElement;
                    ElementUtilities elUtils = javac.getElementUtilities();
                    ExecutableElement overridden = method;
                    do {
                        MethodDoc methodDoc = (MethodDoc) elUtils.javaDocFor(overridden);
                        if(methodDoc != null) {
                            for (ParamTag paramTag : methodDoc.paramTags()) {
                                inheritedParams.add(paramTag.parameterName());
                            }
                            for (ParamTag paramTag : methodDoc.typeParamTags()) {
                                inheritedTypeParams.add(paramTag.parameterName());
                            }
                        }

                    } while((overridden = elUtils.getOverriddenMethod(overridden)) != null);
                    if(ctx.isCanceled()) { break; }
                    TypeElement typeElement = elUtils.enclosingTypeElement(currentElement);
                    findInheritedParams(method, typeElement, inheritedParams, inheritedTypeParams, inheritedThrows);
                    if(ctx.isCanceled()) { break; }
                }
                case CONSTRUCTOR: {
                    MethodTree methodTree = (MethodTree)currentPath.getLeaf();
                    ExecutableElement ee = (ExecutableElement) currentElement;
                    if(ctx.isCanceled()) { break; }
                    checkParamsDocumented(ee.getTypeParameters(), methodTree.getTypeParameters(), docTreePath, inheritedTypeParams, errors);
                    if(ctx.isCanceled()) { break; }
                    checkParamsDocumented(ee.getParameters(), methodTree.getParameters(), docTreePath, inheritedParams, errors);
                    if(ctx.isCanceled()) { break; }
                    checkThrowsDocumented(ee.getThrownTypes(), methodTree.getThrows(), docTreePath, inheritedThrows, errors);
                    if(ctx.isCanceled()) { break; }
                    switch (ee.getReturnType().getKind()) {
                        case VOID:
                        case NONE:
                            break;
                        default:
                            if (!returnTypeFound
                                    && !foundInheritDoc
                                    && !javac.getTypes().isSameType(ee.getReturnType(), javac.getElements().getTypeElement("java.lang.Void").asType())) {
                            Tree returnTree = methodTree.getReturnType();
                            DocTreePathHandle dtph = DocTreePathHandle.create(docTreePath, javac);
                            if(dtph != null) {
                                errors.add(ErrorDescriptionFactory.forTree(ctx, returnTree, MISSING_RETURN_DESC(), AddTagFix.createAddReturnTagFix(dtph).toEditorFix()));
                            }
                        }
                    }
//                    checkThrowsDocumented(ee.getThrownTypes());
                    break;
                }
                case CLASS:
                case ENUM:
                case INTERFACE:
                case ANNOTATION_TYPE: {
                    ClassTree classTree = (ClassTree) currentPath.getLeaf();
                    TypeElement typeElement = (TypeElement) currentElement;
                    if(ctx.isCanceled()) { break; }
                    checkParamsDocumented(typeElement.getTypeParameters(), classTree.getTypeParameters(), docTreePath, inheritedParams, errors);
                    break;
                }
            }
        }
        if(ctx.isCanceled()) { return Collections.<ErrorDescription>emptyList(); }
        return errors;
    }

    @Override
    public Void visitAttribute(AttributeTree node, List<ErrorDescription> errors) {
        return super.visitAttribute(node, errors);
    }

    @Override
    public Void visitAuthor(AuthorTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitAuthor(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitComment(CommentTree node, List<ErrorDescription> errors) {
        return super.visitComment(node, errors);
    }

    @Override
    public Void visitDeprecated(DeprecatedTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitDeprecated(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitDocComment(DocCommentTree node, List<ErrorDescription> errors) {
        DocTreePath currentDocPath = getCurrentPath();
        Void value = super.visitDocComment(node, errors);
        DocSourcePositions sp = (DocSourcePositions) javac.getTrees().getSourcePositions();

        while (!tagStack.isEmpty()) {
            StartElementTree startTree = tagStack.pop();
            Name tagName = startTree.getName();
            HtmlTag tag = HtmlTag.get(tagName);
            if (tag.endKind == HtmlTag.EndKind.REQUIRED) {
                int s = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), startTree);
                int e = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), startTree);
                errors.add(ErrorDescriptionFactory.forSpan(ctx, s, e, TAG_START_UNMATCHED(tagName)));
            }
        }
        return value;
    }

    @Override
    public Void visitDocRoot(DocRootTree node, List<ErrorDescription> errors) {
        return super.visitDocRoot(node, errors);
    }

    @Override
    @Messages({"# {0} - Tag Name", "TAG_END_NOT_PERMITTED=Invalid End Tag: </{0}>",
               "# {0} - Tag Name", "TAG_END_UNEXPECTED=Unexpected End Tag: </{0}>",
               "# {0} - Tag Name", "TAG_START_UNMATCHED=End Tag Missing: </{0}>"})
    public Void visitEndElement(EndElementTree node, List<ErrorDescription> errors) {
        DocTreePath currentDocPath = getCurrentPath();
        DocTreePathHandle dtph = DocTreePathHandle.create(currentDocPath, javac);
        if(dtph == null) {
            return null;
        }
        DocSourcePositions sp = (DocSourcePositions) javac.getTrees().getSourcePositions();
        int start = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), node);
        int end = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), node);

        final Name treeName = node.getName();
        final HtmlTag t = HtmlTag.get(treeName);
        if (t == null) {
             errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, TAG_END_UNKNOWN(treeName)));
        } else if (t.endKind == HtmlTag.EndKind.NONE) {
//            env.messages.error(HTML, node, "dc.tag.end.not.permitted", treeName);
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, TAG_END_NOT_PERMITTED(treeName)));
        } else {
            boolean done = false;
            while (!tagStack.isEmpty()) {
                StartElementTree startTree = tagStack.peek();
                Name tagName = startTree.getName();
                HtmlTag tag = HtmlTag.get(tagName);
                if (t == tag) {
                    tagStack.pop();
                    done = true;
                    break;
                } else if (tag.endKind != HtmlTag.EndKind.REQUIRED) {
                    tagStack.pop();
                } else {
                    boolean found = false;
                    for (StartElementTree set : tagStack) {
                        HtmlTag si = HtmlTag.get(set.getName());
                        if (si == t) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        int s = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), startTree);
                        int e = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), startTree);
                        errors.add(ErrorDescriptionFactory.forSpan(ctx, s, e, TAG_START_UNMATCHED(tagName)));
                        tagStack.pop();
                    } else {
                        errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, TAG_END_UNEXPECTED(treeName)));
                        done = true;
                        break;
                    }
                }
            }
            if (!done && tagStack.isEmpty()) {
                errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, TAG_END_UNEXPECTED(treeName)));
            }
        }
        return super.visitEndElement(node, errors);
    }

    @Override
    public Void visitEntity(EntityTree node, List<ErrorDescription> errors) {
        return super.visitEntity(node, errors);
    }

    @Override
    public Void visitErroneous(ErroneousTree node, List<ErrorDescription> errors) {
        return super.visitErroneous(node, errors);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, List<ErrorDescription> errors) {
        return super.visitIdentifier(node, errors);
    }

    @Override
    public Void visitInheritDoc(InheritDocTree node, List<ErrorDescription> errors) {
        foundInheritDoc = true;
        return super.visitInheritDoc(node, errors);
    }

    @Override
    public Void visitLink(LinkTree node, List<ErrorDescription> errors) {
        return super.visitLink(node, errors);
    }

    @Override
    public Void visitLiteral(LiteralTree node, List<ErrorDescription> errors) {
        return super.visitLiteral(node, errors);
    }

    @Override
    @NbBundle.Messages({"# {0} - tag name", "# {1} - element type", "INVALID_TAG_DESC={0} tag cannot be used on {1}."})
    public Void visitParam(ParamTree tree, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        DocTreePath currentDocPath = getCurrentPath();
        DocTreePathHandle dtph = DocTreePathHandle.create(currentDocPath, javac);
        if(dtph == null) {
            return null;
        }
        DocSourcePositions sp = (DocSourcePositions) javac.getTrees().getSourcePositions();
        int start = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), tree);
        int end = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), tree);
        if(ctx.isCanceled()) { return null; }
        boolean typaram = tree.isTypeParameter();
        switch (currentElement.getKind()) {
            case METHOD:
            case CONSTRUCTOR: {
                ExecutableElement ee = (ExecutableElement) currentElement;
                checkParamDeclared(tree, typaram ? ee.getTypeParameters() : ee.getParameters(), dtph, start, end, errors);
                break;
            }
            case CLASS:
            case INTERFACE: {
                TypeElement te = (TypeElement) currentElement;
                if (typaram) {
                    checkParamDeclared(tree, te.getTypeParameters(), dtph, start, end, errors);
                } else {
                    errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, INVALID_TAG_DESC("@param", currentElement.getKind()), new RemoveTagFix(dtph, "@param").toEditorFix())); //NOI18N
//                    env.messages.error(REFERENCE, tree, "dc.invalid.param");
                }
                break;
            }
            default:
                errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, INVALID_TAG_DESC("@param", currentElement.getKind()), new RemoveTagFix(dtph, "@param").toEditorFix())); //NOI18N
//                env.messages.error(REFERENCE, tree, "dc.invalid.param");
                break;
        }
        warnIfEmpty(tree, tree.getDescription());
        super.visitParam(tree, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @NbBundle.Messages({"# {0} - @param name", "UNKNOWN_TYPEPARAM_DESC=Unknown @param: {0}",
                        "# {0} - @param name", "DUPLICATE_PARAM_DESC=Duplicate @param name: {0}"})
    private void checkParamDeclared(ParamTree tree, List<? extends Element> list,
            DocTreePathHandle dtph, int start, int end, List<ErrorDescription> errors) {
        Name name = tree.getName().getName();
        boolean found = false;
        for (Element e: list) {
            if(ctx.isCanceled()) { return; }
            if (name.equals(e.getSimpleName())) {
                if(!foundParams.add(e)) {
                    errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, DUPLICATE_PARAM_DESC(name), new RemoveTagFix(dtph, "@param").toEditorFix())); // NOI18N
                }
                found = true;
            }
        }
        if (!found) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, UNKNOWN_TYPEPARAM_DESC(name), new RemoveTagFix(dtph, "@param").toEditorFix())); //NOI18N
        }
    }

    private void checkParamsDocumented(List<? extends Element> list, List<? extends Tree> trees, DocTreePath docTreePath, Set<String> inheritedParams, List<ErrorDescription> errors) {
        if(foundInheritDoc) return;
        for (int i = 0; i < list.size() && i < trees.size(); i++) {
            if(ctx.isCanceled()) { return; }
            Element e = list.get(i);
            Tree t = trees.get(i);
            if (!foundParams.contains(e) && !inheritedParams.contains(e.getSimpleName().toString())) {
                boolean isTypeParam = e.getKind() == ElementKind.TYPE_PARAMETER;
                CharSequence paramName = (isTypeParam)
                        ? "<" + e.getSimpleName() + ">"
                        : e.getSimpleName();
                DocTreePathHandle dtph = DocTreePathHandle.create(docTreePath, javac);
                if (dtph != null) {
                    errors.add(ErrorDescriptionFactory.forTree(ctx, t, MISSING_PARAM_DESC(paramName), AddTagFix.createAddParamTagFix(dtph, e.getSimpleName().toString(), isTypeParam, i).toEditorFix()));
                }
            }
        }
    }

    @NbBundle.Messages({"# {0} - [@throws|@exception]", "# {1} - @throws name",
                        "DUPLICATE_THROWS_DESC=Duplicate @{0} tag: {1}",
                        "# {0} - [@throws|@exception]", "# {1} - @throws name",
                        "UNKNOWN_THROWABLE_DESC=Unknown throwable: @{0} {1}"})
    private void checkThrowsDeclared(ThrowsTree tree, Element ex, String fqn, List<? extends TypeMirror> list, DocTreePathHandle dtph, int start, int end, List<ErrorDescription> errors) {
        boolean found = false;
        final TypeMirror type;
        if(ex != null) {
            type = ex.asType();
        } else {
            TypeElement typeElement = javac.getElements().getTypeElement(fqn);
            if(typeElement != null) {
                type = typeElement.asType();
            } else {
                type = null;
            }
        }
        for (TypeMirror t: list) {
            if(ctx.isCanceled()) { return; }
            if(type != null && javac.getTypes().isAssignable(type, t)) {
                if(!foundThrows.add(type)) {
                    errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, DUPLICATE_THROWS_DESC(tree.getTagName(), fqn), new RemoveTagFix(dtph, "@" + tree.getTagName()).toEditorFix()));
                }
                found = true;
                break;
            }
            if (type == null && fqn.equals(t.toString())) {
                if(!foundThrows.add(t)) {
                    errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, DUPLICATE_THROWS_DESC(tree.getTagName(), fqn), new RemoveTagFix(dtph, "@" + tree.getTagName()).toEditorFix()));
                }
                found = true;
                break;
            }
        }
        if (!found) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, UNKNOWN_THROWABLE_DESC(tree.getTagName(), fqn), new RemoveTagFix(dtph, "@" + tree.getTagName()).toEditorFix()));
        }
    }

    private void checkThrowsDocumented(List<? extends TypeMirror> list, List<? extends ExpressionTree> trees, DocTreePath docTreePath, Set<String> inheritedThrows, List<ErrorDescription> errors) {
        if(foundInheritDoc) return;
        for (int i = 0; i < list.size(); i++) {
            if(ctx.isCanceled()) { return; }
            TypeMirror e = list.get(i);
            Tree t = trees.get(i);
            Types types = javac.getTypes();
            if (!foundThrows.contains(e) && !inheritedThrows.contains(e.toString())
                    && (!(types.isAssignable(e, javac.getElements().getTypeElement("java.lang.Error").asType())
                || types.isAssignable(e, javac.getElements().getTypeElement("java.lang.RuntimeException").asType())))) {
                boolean found = false;
                for (TypeMirror typeMirror : foundThrows) {
                    if(types.isAssignable(typeMirror, e)) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    DocTreePathHandle dtph = DocTreePathHandle.create(docTreePath, javac);
                    if(dtph != null) {
                        errors.add(ErrorDescriptionFactory.forTree(ctx, t, NbBundle.getMessage(Analyzer.class, "MISSING_THROWS_DESC", e.toString()), AddTagFix.createAddThrowsTagFix(dtph, e.toString(), i).toEditorFix()));
                    }
                }
            }
        }
    }

    void warnIfEmpty(DocTree tree, List<? extends DocTree> list) {
//        for (DocTree d: list) {
//            switch (d.getKind()) {
//                case TEXT:
//                    if (hasNonWhitespace((TextTree) d))
//                        return;
//                    break;
//                default:
//                    return;
//            }
//        }
//        env.messages.warning(SYNTAX, tree, "dc.empty", tree.getKind().tagName);
    }

    @Override
    public Void visitReference(ReferenceTree node, List<ErrorDescription> errors) {
        return super.visitReference(node, errors);
    }

    @Override
    @NbBundle.Messages({"WRONG_RETURN_DESC=@return tag cannot be used in method with void return type.",
                        "WRONG_CONSTRUCTOR_RETURN_DESC=Illegal @return tag.",
                        "DUPLICATE_RETURN_DESC=Duplicate @return tag."})
    public Void visitReturn(ReturnTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        DocTreePath currentDocPath = getCurrentPath();
        DocTreePathHandle dtph = DocTreePathHandle.create(currentDocPath, javac);
        if(dtph == null) {
            return null;
        }
        DocSourcePositions sp = (DocSourcePositions) javac.getTrees().getSourcePositions();
        int start = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), node);
        int end = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), node);
        if(returnType == null) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, WRONG_CONSTRUCTOR_RETURN_DESC(),
                    new RemoveTagFix(dtph, "@return").toEditorFix()));
        } else if (returnType.getKind() == TypeKind.VOID) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, WRONG_RETURN_DESC(),
                    new RemoveTagFix(dtph, "@return").toEditorFix()));
        } else if(returnTypeFound) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, DUPLICATE_RETURN_DESC(),
                    new RemoveTagFix(dtph, "@return").toEditorFix()));
        } else {
            returnTypeFound = true;
        }
        super.visitReturn(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitSee(SeeTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitSee(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitSerial(SerialTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitSerial(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitSerialData(SerialDataTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitSerialData(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitSerialField(SerialFieldTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitSerialField(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitSince(SinceTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitSince(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    @Messages({"# {0} - Tag name",
               "TAG_UNKNOWN=Unknown HTML Tag: <{0}>",
               "# {0} - Tag name",
               "TAG_END_UNKNOWN=Unknown HTML End Tag: </{0}>",
               "TAG_SELF_CLOSING=Self-closing element not allowed"})
    public Void visitStartElement(StartElementTree node, List<ErrorDescription> errors) {
        DocTreePath currentDocPath = getCurrentPath();
        DocTreePathHandle dtph = DocTreePathHandle.create(currentDocPath, javac);
        if(dtph == null) {
            return null;
        }
        DocSourcePositions sp = (DocSourcePositions) javac.getTrees().getSourcePositions();
        int start = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), node);
        int end = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), node);


        final Name treeName = node.getName();
        final HtmlTag t = HtmlTag.get(treeName);
        if (t == null) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, TAG_UNKNOWN(treeName)));
        } else {
            if(t.endKind != HtmlTag.EndKind.NONE) {
                tagStack.push(node);
            }
        }
        
        if (node.isSelfClosing()) {
            errors.add(ErrorDescriptionFactory.forSpan(ctx, start, end, TAG_SELF_CLOSING()));
        }
        
        return super.visitStartElement(node, errors);
    }

    @Override
    public Void visitText(TextTree node, List<ErrorDescription> errors) {
        return super.visitText(node, errors);
    }

    @Override
    public Void visitThrows(ThrowsTree tree, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        ReferenceTree exName = tree.getExceptionName();
        DocTreePath refPath = new DocTreePath(getCurrentPath(), tree.getExceptionName());
        Element ex = javac.getDocTrees().getElement(refPath);
        Types types = javac.getTypes();
        Elements elements = javac.getElements();
        final TypeElement throwableEl = elements.getTypeElement("java.lang.Throwable");
        final TypeElement errorEl = elements.getTypeElement("java.lang.Error");
        final TypeElement runtimeEl = elements.getTypeElement("java.lang.RuntimeException");
        if(throwableEl == null || errorEl == null || runtimeEl == null) {
            LOG.warning("Broken java-platform, cannot resolve " + throwableEl == null? "java.lang.Throwable" : errorEl == null? "java.lang.Error" : "java.lang.RuntimeException"); //NOI18N
            return null;
        }
        TypeMirror throwable = throwableEl.asType();
        TypeMirror error = errorEl.asType();
        TypeMirror runtime = runtimeEl.asType();
        DocTreePath currentDocPath = getCurrentPath();
        DocTreePathHandle dtph = DocTreePathHandle.create(currentDocPath, javac);
        if(dtph == null) {
            return null;
        }
        DocSourcePositions sp = (DocSourcePositions) javac.getTrees().getSourcePositions();
        int start = (int) sp.getStartPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), tree);
        int end = (int) sp.getEndPosition(javac.getCompilationUnit(), currentDocPath.getDocComment(), tree);
        if (ex == null || (ex.asType().getKind() == TypeKind.DECLARED
                && types.isAssignable(ex.asType(), throwable))) {
            switch (currentElement.getKind()) {
                case CONSTRUCTOR:
                case METHOD:
                    if (ex == null || !(types.isAssignable(ex.asType(), error)
                            || types.isAssignable(ex.asType(), runtime))) {
                        ExecutableElement ee = (ExecutableElement) currentElement;
                        String fqn;
                        if (ex != null) {
                            fqn = ((TypeElement) ex).getQualifiedName().toString();
                        } else {
                            ExpressionTree referenceClass = javac.getTreeUtilities().getReferenceClass(new DocTreePath(currentDocPath, exName));
                            if(referenceClass == null) break;
                            fqn = referenceClass.toString();
                        }
                        checkThrowsDeclared(tree, ex, fqn, ee.getThrownTypes(), dtph, start, end, errors);
                    }
                    break;
                default:
//                        env.messages.error(REFERENCE, tree, "dc.invalid.throws");
            }
        } else {
//                env.messages.error(REFERENCE, tree, "dc.invalid.throws");
        }
        warnIfEmpty(tree, tree.getDescription());
        super.visitThrows(tree, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }
    private static final Logger LOG = Logger.getLogger(Analyzer.class.getName());

    @Override
    public Void visitUnknownBlockTag(UnknownBlockTagTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitUnknownBlockTag(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitUnknownInlineTag(UnknownInlineTagTree node, List<ErrorDescription> errors) {
        return super.visitUnknownInlineTag(node, errors);
    }

    @Override
    public Void visitValue(ValueTree node, List<ErrorDescription> errors) {
        return super.visitValue(node, errors);
    }

    @Override
    public Void visitVersion(VersionTree node, List<ErrorDescription> errors) {
        boolean oldInheritDoc = foundInheritDoc;
        super.visitVersion(node, errors);
        foundInheritDoc = oldInheritDoc;
        return null;
    }

    @Override
    public Void visitOther(DocTree node, List<ErrorDescription> errors) {
        return super.visitOther(node, errors);
    }

    @Override
    public Void scan(DocTree tree, List<ErrorDescription> p) {
        if(ctx.isCanceled()) { return null; }
        return super.scan(tree, p);
    }

    private void findInheritedParams(ExecutableElement method, TypeElement typeElement, Set<String> inheritedParams, Set<String> inheritedTypeParams, Set<String> inheritedThrows) {
        if(typeElement == null) return;

        for (TypeMirror typeMirror : typeElement.getInterfaces()) {
            for (Element el : javac.getElementUtilities().getMembers(typeMirror, new ElementUtilities.ElementAcceptor() {

                @Override
                public boolean accept(Element e, TypeMirror type) {
                    return e.getKind() == ElementKind.METHOD;
                }
            })) {
                if(ctx.isCanceled()) { return; }
                if(javac.getElements().overrides(method, (ExecutableElement) el, typeElement)) {
                    MethodDoc methodDoc = (MethodDoc) javac.getElementUtilities().javaDocFor(el);
                    if(methodDoc != null) {
                        for (ParamTag paramTag : methodDoc.paramTags()) {
                            inheritedParams.add(paramTag.parameterName());
                        }
                        for (ParamTag paramTag : methodDoc.typeParamTags()) {
                            inheritedTypeParams.add(paramTag.parameterName());
                        }
                        for (ThrowsTag throwsTag : methodDoc.throwsTags()) {
                            Type exceptionType = throwsTag.exceptionType();
                            if(exceptionType != null) {
                                inheritedThrows.add(exceptionType.qualifiedTypeName());
                            }
                        }
                        returnTypeFound |= methodDoc.tags("return").length > 0;
                    }
                }
            }
        }
    }
}
