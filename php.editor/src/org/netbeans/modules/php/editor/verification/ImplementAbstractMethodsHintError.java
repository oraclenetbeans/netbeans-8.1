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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.elements.BaseFunctionElement.PrintAs;
import org.netbeans.modules.php.editor.api.elements.ElementFilter;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.api.elements.TypeNameResolver;
import org.netbeans.modules.php.editor.elements.TypeNameResolverImpl;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.FileScope;
import org.netbeans.modules.php.editor.model.InterfaceScope;
import org.netbeans.modules.php.editor.model.MethodScope;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.TraitScope;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;

/**
 * @author Radek Matous, Ondrej Brejla
 */
public class ImplementAbstractMethodsHintError extends HintErrorRule {

    private static final String ABSTRACT_PREFIX = "abstract "; //NOI18N
    private static final Logger LOGGER = Logger.getLogger(ImplementAbstractMethodsHintError.class.getName());

    @Override
    @Messages("ImplementAbstractMethodsDispName=Implement All Abstract Methods")
    public String getDisplayName() {
        return Bundle.ImplementAbstractMethodsDispName();
    }

    @Override
    @Messages({
        "# {0} - Class name",
        "# {1} - Abstract method name",
        "# {2} - Owner (class) of abstract method",
        "ImplementAbstractMethodsHintDesc={0} is not abstract and does not override abstract method {1} in {2}"
    })
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        FileScope fileScope = context.fileScope;
        FileObject fileObject = context.parserResult.getSnapshot().getSource().getFileObject();
        if (fileScope != null && fileObject != null) {
            Collection<? extends ClassScope> allClasses = ModelUtils.getDeclaredClasses(fileScope);
            for (FixInfo fixInfo : checkHints(allClasses, context)) {
                hints.add(new Hint(
                        ImplementAbstractMethodsHintError.this,
                        Bundle.ImplementAbstractMethodsHintDesc(fixInfo.className, fixInfo.lastMethodDeclaration, fixInfo.lastMethodOwnerName),
                        fileObject,
                        fixInfo.classNameRange,
                        createHintFixes(context.doc, fixInfo),
                        500));
            }
        }
    }

    private List<HintFix> createHintFixes(BaseDocument doc, FixInfo fixInfo) {
        List<HintFix> hintFixes = new ArrayList<>();
        hintFixes.add(new ImplementAllFix(doc, fixInfo));
        hintFixes.add(new AbstractClassFix(doc, fixInfo));
        return Collections.unmodifiableList(hintFixes);
    }

    private Collection<FixInfo> checkHints(Collection<? extends ClassScope> allClasses, PHPRuleContext context) {
        List<FixInfo> retval = new ArrayList<>();
        for (ClassScope classScope : allClasses) {
            if (!classScope.isAbstract()) {
                Index index = context.getIndex();
                Set<String> allValidMethods = new HashSet<>();
                allValidMethods.addAll(toNames(getValidInheritedMethods(getInheritedMethods(classScope, index))));
                allValidMethods.addAll(toNames(index.getDeclaredMethods(classScope)));
                ElementFilter declaredMethods = ElementFilter.forExcludedNames(allValidMethods, PhpElementKind.METHOD);
                Set<MethodElement> accessibleMethods = declaredMethods.filter(index.getAccessibleMethods(classScope, classScope));
                Set<String> methodSkeletons = new LinkedHashSet<>();
                MethodElement lastMethodElement = null;
                FileObject lastFileObject = null;
                FileScope fileScope = null;
                for (MethodElement methodElement : accessibleMethods) {
                    final TypeElement type = methodElement.getType();
                    if ((type.isInterface() || methodElement.isAbstract()) && !methodElement.isFinal()) {
                        FileObject fileObject = methodElement.getFileObject();
                        if (lastFileObject != fileObject) {
                            lastFileObject = fileObject;
                            fileScope = getFileScope(fileObject);
                        }
                        if (fileScope != null) {
                            NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(fileScope, methodElement.getOffset());
                            List typeNameResolvers = new ArrayList<>();
                            if (fileObject != null && CodeUtils.isPhp52(fileObject)) {
                                typeNameResolvers.add(TypeNameResolverImpl.forUnqualifiedName());
                            } else {
                                typeNameResolvers.add(TypeNameResolverImpl.forFullyQualifiedName(namespaceScope, methodElement.getOffset()));
                                typeNameResolvers.add(TypeNameResolverImpl.forSmartName(classScope, classScope.getOffset()));
                            }
                            TypeNameResolver typeNameResolver = TypeNameResolverImpl.forChainOf(typeNameResolvers);
                            String skeleton = methodElement.asString(PrintAs.DeclarationWithEmptyBody, typeNameResolver);
                            skeleton = skeleton.replace(ABSTRACT_PREFIX, ""); //NOI18N
                            methodSkeletons.add(skeleton);
                            lastMethodElement = methodElement;
                        }
                    }
                }
                if (!methodSkeletons.isEmpty() && lastMethodElement != null) {
                    int classDeclarationOffset = getClassDeclarationOffset(context.parserResult.getSnapshot().getTokenHierarchy(), classScope.getOffset());
                    int newMethodsOffset = getNewMethodsOffset(classScope, context.doc, classDeclarationOffset);
                    if (newMethodsOffset != -1 && classDeclarationOffset != -1) {
                        retval.add(new FixInfo(classScope, methodSkeletons, lastMethodElement, newMethodsOffset, classDeclarationOffset));
                    }
                }
            }
        }
        return retval;
    }

    private FileScope getFileScope(final FileObject fileObject) {
        final FileScope[] fileScope = new FileScope[1];
        try {
            ParserManager.parse(Collections.singletonList(Source.create(fileObject)), new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result parserResult = resultIterator.getParserResult();
                    PHPParseResult phpResult = (PHPParseResult) parserResult;
                    fileScope[0] = phpResult.getModel().getFileScope();
                }
        });
        } catch (ParseException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return fileScope[0];
    }

    private Set<MethodElement> getInheritedMethods(final ClassScope classScope, final Index index) {
        Set<MethodElement> inheritedMethods = new HashSet<>();
        Set<MethodElement> declaredSuperMethods =  new HashSet<>();
        Set<MethodElement> accessibleSuperMethods =  new HashSet<>();
        Collection<? extends ClassScope> superClasses = classScope.getSuperClasses();
        for (ClassScope cls : superClasses) {
            declaredSuperMethods.addAll(index.getDeclaredMethods(cls));
            accessibleSuperMethods.addAll(index.getAccessibleMethods(cls, classScope));
        }
        Collection<? extends InterfaceScope> superInterface = classScope.getSuperInterfaceScopes();
        for (InterfaceScope interfaceScope : superInterface) {
            declaredSuperMethods.addAll(index.getDeclaredMethods(interfaceScope));
            accessibleSuperMethods.addAll(index.getAccessibleMethods(interfaceScope, classScope));
        }
        Collection<? extends TraitScope> traits = classScope.getTraits();
        for (TraitScope traitScope : traits) {
            declaredSuperMethods.addAll(index.getDeclaredMethods(traitScope));
            accessibleSuperMethods.addAll(index.getAccessibleMethods(traitScope, classScope));
        }
        inheritedMethods.addAll(declaredSuperMethods);
        inheritedMethods.addAll(accessibleSuperMethods);
        return inheritedMethods;
    }

    private Set<MethodElement> getValidInheritedMethods(Set<MethodElement> inheritedMethods) {
        Set<MethodElement> retval = new HashSet<>();
        for (MethodElement methodElement : inheritedMethods) {
            if (!methodElement.isAbstract()) {
                retval.add(methodElement);
            }
        }
        return retval;
    }

    private static Set<String> toNames(Set<? extends PhpElement> elements) {
        Set<String> names = new HashSet<>();
        for (PhpElement elem : elements) {
            names.add(elem.getName());
        }
        return names;
    }

    private static int getClassDeclarationOffset(TokenHierarchy<?> th, int classNameOffset) {
        TokenSequence<PHPTokenId> ts = LexUtilities.getPHPTokenSequence(th, classNameOffset);
        ts.move(classNameOffset);
        ts.movePrevious();
        Token<? extends PHPTokenId> previousToken = LexUtilities.findPreviousToken(ts, Collections.<PHPTokenId>singletonList(PHPTokenId.PHP_CLASS));
        return previousToken.offset(th);
    }

    private static int getNewMethodsOffset(ClassScope classScope, BaseDocument doc, int classDeclarationOffset) {
        int offset = -1;
        Collection<? extends MethodScope> declaredMethods = classScope.getDeclaredMethods();
        for (MethodScope methodScope : declaredMethods) {
            OffsetRange blockRange = methodScope.getBlockRange();
            if (blockRange != null && blockRange.getEnd() > offset) {
                offset = blockRange.getEnd();
            }
        }
        if (offset == -1 && classScope.getBlockRange() != null) {
            try {
                int rowStartOfClassEnd = Utilities.getRowStart(doc, classScope.getBlockRange().getEnd());
                int rowEndOfPreviousRow = rowStartOfClassEnd - 1;
                int newMethodPossibleOffset = Utilities.getRowStart(doc, rowEndOfPreviousRow);
                int newMethodLineOffset = Utilities.getLineOffset(doc, newMethodPossibleOffset);
                int classDeclarationLineOffset = Utilities.getLineOffset(doc, classDeclarationOffset);
                if (newMethodLineOffset == classDeclarationLineOffset) {
                    offset = rowEndOfPreviousRow;
                } else {
                    offset = newMethodPossibleOffset;
                }
            } catch (BadLocationException ex) {
                offset = -1;
            }
        }
        return offset;
    }

    private static class ImplementAllFix implements HintFix {
        private final BaseDocument doc;
        private final FixInfo fixInfo;

        ImplementAllFix(BaseDocument doc, FixInfo fixInfo) {
            this.doc = doc;
            this.fixInfo = fixInfo;
        }

        @Override
        @Messages("ImplementAbstractMethodsDesc=Implement All Abstract Methods")
        public String getDescription() {
            return Bundle.ImplementAbstractMethodsDesc();
        }

        @Override
        public void implement() throws Exception {
            getEditList().apply();
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        EditList getEditList() throws Exception {
            EditList edits = new EditList(doc);
            edits.setFormatAll(true);
            for (String methodScope : fixInfo.methodSkeletons) {
                edits.replace(fixInfo.newMethodsOffset, 0, methodScope, true, 0);
            }
            return edits;
        }
    }

    private static class AbstractClassFix implements HintFix {
        private final BaseDocument doc;
        private final FixInfo fixInfo;

        public AbstractClassFix(BaseDocument doc, FixInfo fixInfo) {
            this.doc = doc;
            this.fixInfo = fixInfo;
        }

        @Override
        @Messages("AbstractClassFixDesc=Declare Abstract Class")
        public String getDescription() {
            return Bundle.AbstractClassFixDesc();
        }

        @Override
        public void implement() throws Exception {
            EditList edits = new EditList(doc);
            edits.replace(fixInfo.classDeclarationOffset, 0, ABSTRACT_PREFIX, true, 0);
            edits.apply();
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

    }

    private static class FixInfo {
        private List<String> methodSkeletons;
        private String className;
        private int newMethodsOffset;
        private OffsetRange classNameRange;
        private final String lastMethodDeclaration;
        private final String lastMethodOwnerName;
        private final int classDeclarationOffset;

        FixInfo(ClassScope classScope, Set<String> methodSkeletons, MethodElement lastMethodElement, int newMethodsOffset, int classDeclarationOffset) {
            this.methodSkeletons = new ArrayList<>(methodSkeletons);
            className = classScope.getFullyQualifiedName().toString();
            Collections.sort(this.methodSkeletons);
            this.classNameRange = classScope.getNameRange();
            this.classDeclarationOffset = classDeclarationOffset;
            this.newMethodsOffset = newMethodsOffset;
            lastMethodDeclaration = lastMethodElement.asString(PrintAs.NameAndParamsDeclaration);
            lastMethodOwnerName = lastMethodElement.getType().getFullyQualifiedName().toString();
        }
    }
}
