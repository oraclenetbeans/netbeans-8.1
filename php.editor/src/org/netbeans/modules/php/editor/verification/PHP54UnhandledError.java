/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.AnonymousObjectVariable;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayCreation;
import org.netbeans.modules.php.editor.parser.astnodes.DereferencedArrayAccess;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.FieldAccess;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.LambdaFunctionDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.MethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.ReflectionVariable;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.StaticMethodInvocation;
import org.netbeans.modules.php.editor.parser.astnodes.TraitDeclaration;
import org.netbeans.modules.php.editor.parser.astnodes.UseTraitStatement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class PHP54UnhandledError extends UnhandledErrorRule {

    @Override
    public void invoke(PHPRuleContext context, List<org.netbeans.modules.csl.api.Error> errors) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() == null) {
            return;
        }
        FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
        if (fileObject != null && appliesTo(fileObject)) {
            PHP54UnhandledError.CheckVisitor checkVisitor = new PHP54UnhandledError.CheckVisitor(fileObject);
            phpParseResult.getProgram().accept(checkVisitor);
            errors.addAll(checkVisitor.getErrors());
        }
    }

    public static  boolean appliesTo(FileObject fobj) {
        return !CodeUtils.isPhp54(fobj) && !CodeUtils.isPhp55(fobj) && !CodeUtils.isPhp56(fobj);
    }

    private static class CheckVisitor extends DefaultVisitor {
        private static final String BINARY_PREFIX = "0b"; //NOI18N
        private final List<VerificationError> errors = new ArrayList<>();
        private final FileObject fileObject;
        private boolean checkAnonymousObjectVariable;

        public CheckVisitor(FileObject fileObject) {
            this.fileObject = fileObject;
        }

        public Collection<VerificationError> getErrors() {
            return Collections.unmodifiableCollection(errors);
        }

        @Override
        public void visit(TraitDeclaration node) {
            Identifier name = node.getName();
            if (name != null) {
                createError(name);
            } else {
                createError(node);
            }
        }

        @Override
        public void visit(UseTraitStatement node) {
            createError(node);
        }

        @Override
        public void visit(MethodInvocation node) {
            checkAnonymousObjectVariable = true;
            super.visit(node);
            checkAnonymousObjectVariable = false;
        }

        @Override
        public void visit(FieldAccess node) {
            checkAnonymousObjectVariable = true;
            super.visit(node);
            checkAnonymousObjectVariable = false;
        }

        @Override
        public void visit(AnonymousObjectVariable node) {
            if (checkAnonymousObjectVariable) {
                createError(node);
            }
        }

        @Override
        public void visit(DereferencedArrayAccess node) {
            createError(node);
        }

        @Override
        public void visit(Scalar node) {
            if (node.getScalarType().equals(Scalar.Type.REAL) && node.getStringValue().startsWith(BINARY_PREFIX)) {
                createError(node);
            }
        }

        @Override
        public void visit(StaticMethodInvocation node) {
            Expression name = node.getMethod().getFunctionName().getName();
            if (name instanceof ReflectionVariable) {
                createError(name);
            }
        }

        @Override
        public void visit(LambdaFunctionDeclaration node) {
            if (node.isStatic()) {
                createError(node);
            }
        }

        @Override
        public void visit(ArrayCreation node) {
            ArrayCreation.Type type = node.getType();
            if (type == ArrayCreation.Type.NEW) {
                createError(node);
            } else {
                super.visit(node);
            }
        }

        private  void createError(int startOffset, int endOffset) {
            VerificationError error = new PHP54VersionError(fileObject, startOffset, endOffset);
            errors.add(error);
        }

        private void createError(ASTNode node) {
            createError(node.getStartOffset(), node.getEndOffset());
            super.visit(node);
        }

    }

    private static final class PHP54VersionError extends VerificationError {

        private static final String KEY = "Php.Version.54"; //NOI18N

        private PHP54VersionError(FileObject fileObject, int startOffset, int endOffset) {
            super(fileObject, startOffset, endOffset);
        }

        @Override
        @Messages("CheckPHP54VerDisp=Language feature not compatible with PHP version indicated in project settings")
        public String getDisplayName() {
            return Bundle.CheckPHP54VerDisp();
        }

        @Override
        @Messages("CheckPHP54VerDesc=Detect language features not compatible with PHP version indicated in project settings")
        public String getDescription() {
            return Bundle.CheckPHP54VerDesc();
        }

        @Override
        public String getKey() {
            return KEY;
        }

    }

    @Override
    @Messages("PHP54VersionErrorHintDispName=Language feature not compatible with PHP version indicated in project settings")
    public String getDisplayName() {
        return Bundle.PHP54VersionErrorHintDispName();
    }

}
