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
package org.netbeans.modules.php.editor.verification;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.api.elements.ParameterElement;
import org.netbeans.modules.php.editor.model.ClassScope;
import org.netbeans.modules.php.editor.model.MethodScope;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.MethodDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class ParentConstructorCallHint extends HintRule {
    private static final String HINT_ID = "Parent.Constructor.Call.Hint"; //NOI18N
    private List<Hint> hints;
    private BaseDocument baseDocument;
    private FileObject fileObject;

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        this.hints = hints;
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        baseDocument = context.doc;
        if (phpParseResult.getProgram() != null) {
            fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
            if (fileObject != null) {
                checkHints(phpParseResult, fileObject);
            }
        }
    }

    private void checkHints(PHPParseResult phpParseResult, FileObject fileObject) {
        Collection<? extends ClassScope> declaredClasses = ModelUtils.getDeclaredClasses(phpParseResult.getModel().getFileScope());
        for (ClassScope classScope : declaredClasses) {
            MethodScope constructor = extractConstructor(classScope);
            MethodScope overriddenConstructor = extractOverriddenConstructor(classScope);
            if (constructor != null && overriddenConstructor != null) {
                ParametersDescriptor parametersDescriptor = new ParametersDescriptor(overriddenConstructor.getParameters());
                CheckVisitor checkVisitor = new CheckVisitor(constructor.getOffset());
                phpParseResult.getProgram().accept(checkVisitor);
                createHint(checkVisitor.getConstructorCallDescriptor(), parametersDescriptor, constructor.getNameRange());
            }
        }
    }

    private void createHint(ConstructorCallDescriptor constructorCallDescriptor, ParametersDescriptor parametersDescriptor, OffsetRange offsetRange) {
        if (constructorCallDescriptor.shouldBeHinted(parametersDescriptor) && showHint(offsetRange, baseDocument)) {
            hints.add(new Hint(
                    this,
                    constructorCallDescriptor.createMessage(parametersDescriptor),
                    fileObject,
                    offsetRange,
                    null,
                    500));
        }
    }

    private MethodScope extractOverriddenConstructor(ClassScope classScope) {
        MethodScope result = null;
        Set<ClassScope> recursionDetection = new HashSet<>();
        while (classScope != null && result == null && recursionDetection.add(classScope)) {
            ClassScope superClass = ModelUtils.getFirst(classScope.getSuperClasses());
            result = extractConstructor(superClass);
            classScope = superClass;
        }
        return result;
    }

    private MethodScope extractConstructor(ClassScope classScope) {
        MethodScope result = null;
        if (classScope != null) {
            result = ModelUtils.getFirst(classScope.getDeclaredConstructors());
        }
        return result;
    }

    private static final class CheckVisitor extends DefaultVisitor {
        private final int constructorOffset;
        private boolean inConstructor;
        private ConstructorCallDescriptor constructorCallDescriptor = new ConstructorCallDescriptor(ConstructorCallDescriptor.Description.NOT_CALLED, 0);

        public CheckVisitor(int constructorOffset) {
            this.constructorOffset = constructorOffset;
        }

        public ConstructorCallDescriptor getConstructorCallDescriptor() {
            return constructorCallDescriptor;
        }

        @Override
        public void visit(MethodDeclaration node) {
            if (node.getStartOffset() <= constructorOffset && node.getEndOffset() >= constructorOffset) {
                inConstructor = true;
                super.visit(node);
                inConstructor = false;
            }
        }

        @Override
        public void visit(StaticMethodInvocation node) {
            if (inConstructor) {
                String functionName = CodeUtils.extractFunctionName(node.getMethod());
                if (functionName.equals("__construct")) { //NOI18N
                    constructorCallDescriptor = new ConstructorCallDescriptor(ConstructorCallDescriptor.Description.IS_CALLED, node.getMethod().getParameters().size());
                }
            }
        }

    }

    private static final class ConstructorCallDescriptor {
        private final Description description;
        private final int parametersCount;

        private enum Description {
            @NbBundle.Messages({
                "# {0} - Number of used parameters",
                "# {1} - Number of mandatory parameters",
                "# {2} - Number of optional parameters",
                "ParentConstructorCallHintIsCalledText=Parent Constructor is Called"
                + "\n- with wrong number of parameters: {0}."
                + "\n- {1} mandatory and {2} optional parameters needed."
            })
            IS_CALLED {

                @Override
                protected String createMessage(int parametersCount, ParametersDescriptor parametersDescriptor) {
                    return Bundle.ParentConstructorCallHintIsCalledText(parametersCount, parametersDescriptor.getMandatoryCount(), parametersDescriptor.getOptionalCount());
                }

                @Override
                protected boolean shouldBeHinted(int parametersCount, ParametersDescriptor parametersDescriptor) {
                    return !parametersDescriptor.allows(parametersCount);
                }

            },

            @NbBundle.Messages({
                "# {0} - Number of mandatory parameters",
                "# {1} - Number of optional parameters",
                "ParentConstructorCallHintNotCalledText=Parent Constructor is Not Called"
                + "\n- {0} mandatory and {1} optional parameters needed."
                + "\n- Your objects can be wrongly initialized."
            })
            NOT_CALLED {

                @Override
                protected String createMessage(int parametersCount, ParametersDescriptor parametersDescriptor) {
                    return Bundle.ParentConstructorCallHintNotCalledText(parametersDescriptor.getMandatoryCount(), parametersDescriptor.getOptionalCount());
                }

                @Override
                protected boolean shouldBeHinted(int parametersCount, ParametersDescriptor parametersDescriptor) {
                    return true;
                }

            };

            protected abstract String createMessage(int parametersCount, ParametersDescriptor parametersDescriptor);
            protected abstract boolean shouldBeHinted(int parametersCount, ParametersDescriptor parametersDescriptor);
        }

        public ConstructorCallDescriptor(Description description, int parametersCount) {
            this.description = description;
            this.parametersCount = parametersCount;
        }

        public boolean shouldBeHinted(ParametersDescriptor parametersDescriptor) {
            return description.shouldBeHinted(parametersCount, parametersDescriptor);
        }

        public String createMessage(ParametersDescriptor parametersDescriptor) {
            return description.createMessage(parametersCount, parametersDescriptor);
        }

    }

    private static final class ParametersDescriptor {
        private final List<? extends ParameterElement> parameters;
        private boolean processed = false;
        private int mandatoryCount = 0;
        private int optionalCount = 0;

        public ParametersDescriptor(List<? extends ParameterElement> parameters) {
            this.parameters = parameters;
        }

        public int getMandatoryCount() {
            if (!processed) {
                processParameters();
            }
            return mandatoryCount;
        }

        public int getOptionalCount() {
            if (!processed) {
                processParameters();
            }
            return optionalCount;
        }

        private void processParameters() {
            for (ParameterElement parameter : parameters) {
                if (parameter.isMandatory()) {
                    mandatoryCount++;
                } else {
                    optionalCount++;
                }
            }
            processed = true;
        }

        public boolean allows(int numberOfUsedParameters) {
            OffsetRange offsetRange = new OffsetRange(getMandatoryCount(), getMandatoryCount() + getOptionalCount());
            return offsetRange.containsInclusive(numberOfUsedParameters);
        }

    }

    @Override
    public String getId() {
        return HINT_ID;
    }

    @Override
    @NbBundle.Messages(
        "ParentConstructorCallHintDesc=Constructor of parent class should be called if exists (it ensures the right initialization of instantiated object)."
    )
    public String getDescription() {
        return Bundle.ParentConstructorCallHintDesc();
    }

    @Override
    @NbBundle.Messages("ParentConstructorCallHintDisp=Parent Constructor Call")
    public String getDisplayName() {
        return Bundle.ParentConstructorCallHintDisp();
    }

}
