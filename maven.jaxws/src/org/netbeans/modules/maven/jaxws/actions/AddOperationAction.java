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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.maven.jaxws.actions;

import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.modules.maven.jaxws._RetoucheUtil;
import org.netbeans.modules.websvc.api.support.AddOperationCookie;
import org.netbeans.modules.websvc.api.support.java.SourceUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.nodes.Node;
import org.openide.util.actions.NodeAction;

public class AddOperationAction extends NodeAction  {

    @Override
    public String getName() {
        return NbBundle.getMessage(AddOperationAction.class, "LBL_OperationAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length != 1) return false;
        FileObject implClassFo = activatedNodes[0].getLookup().lookup(FileObject.class);
        return implClassFo != null;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        FileObject implClassFo = activatedNodes[0].getLookup().lookup(FileObject.class);
        if (isFromWsdl(implClassFo)) {
            DialogDisplayer.getDefault().notify(new DialogDescriptor.Message(
                    NbBundle.getMessage(AddOperationAction.class, "LBL_CannotRunOnWsdl")));
            return;
        }
        AddOperationCookie addOperationCookie = new JaxWsAddOperation(implClassFo);
        addOperationCookie.addOperation();
    }

    @Override
    protected boolean asynchronous() {
        return true;
    }

    private boolean isFromWsdl(FileObject inplClass) {
        final boolean[] fromWsdl = new boolean[1];
        JavaSource javaSource = JavaSource.forFileObject(inplClass);
        if (javaSource != null) {

            CancellableTask<CompilationController> task = new CancellableTask<CompilationController>() {

                @Override
                public void run(CompilationController controller) throws java.io.IOException {
                    controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    TypeElement typeElement = SourceUtils.getPublicTopLevelElement(controller);
                    if ( typeElement == null ){
                        return;
                    }
                    AnnotationMirror annMirror = _RetoucheUtil.getAnnotation(
                            controller, typeElement, "javax.jws.WebService");   //NOI18N
                    if ( annMirror == null ){
                        return;
                    }
                    Map<? extends ExecutableElement, ? extends AnnotationValue> 
                        expressions = annMirror.getElementValues();
                    for (Map.Entry<? extends ExecutableElement, 
                            ? extends AnnotationValue> entry : expressions.entrySet()) 
                    {
                        if (entry.getKey().getSimpleName().contentEquals("wsdlLocation")) { //NOI18N
                            fromWsdl[0] = true;
                            return;
                        }
                    }
                }

                @Override
                public void cancel() {
                }

            };

            try {
                javaSource.runUserActionTask(task, true);
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        }

        return fromWsdl[0];
    }
}

