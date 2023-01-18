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

package org.netbeans.modules.websvc.editor.hints.fixes;

import java.io.IOException;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.websvc.editor.hints.common.Utilities;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 * @author Ajit.Bhate@Sun.COM
 */
public class AddAnnotation implements Fix {
    private FileObject fileObject;
    private Element element;
    private String annotation;
    
    /** Creates a new instance of AddAnnotation */
    public AddAnnotation(FileObject fileObject, Element element,
            String annotation) {
        this.element = element;
        this.fileObject = fileObject;
        this.annotation = annotation;
    }
    
    public ChangeInfo implement(){
        CancellableTask<WorkingCopy> task = new CancellableTask<WorkingCopy>(){
            public void cancel() {}
            
            public void run(WorkingCopy workingCopy) throws Exception {
                if (element.getKind() == ElementKind.PARAMETER){
                    Element method = element.getEnclosingElement();
                    if ( method instanceof ExecutableElement ){
                        ExecutableElement methodElement = (ExecutableElement)method;
                        List<? extends VariableElement> parameters = methodElement.getParameters();
                        int index = parameters.indexOf( element );
                        if ( index == -1 ){
                            return;
                        }
                        Utilities.addAnnotation(workingCopy, 
                                ElementHandle.create(methodElement), index,
                                annotation);
                    }
                    else {
                        return;
                    }
                }
                else {
                    Utilities.addAnnotation(workingCopy, 
                            ElementHandle.create(element), annotation);
                }
            }
        };
        
        JavaSource javaSource = JavaSource.forFileObject(fileObject);
        
        try{
            if ( javaSource!= null ){
                javaSource.runModificationTask(task).commit();
            }
        } catch (IOException e){
        }
        return null;
    }
    
    public int hashCode(){
        return 1;
    }
    
    public boolean equals(Object o){
        // TODO: implement equals properly
        return super.equals(o);
    }
    
    public String getText(){
        String annotationLabel = annotation.substring(annotation.lastIndexOf(".")+1);
        return NbBundle.getMessage(RemoveAnnotation.class, "LBL_AddAnnotation",annotationLabel);
    }
}
