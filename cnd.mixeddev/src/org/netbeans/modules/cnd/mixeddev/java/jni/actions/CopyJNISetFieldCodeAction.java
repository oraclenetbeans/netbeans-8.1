/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.mixeddev.java.jni.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.text.Document;
import org.netbeans.modules.cnd.mixeddev.MixedDevUtils;
import org.netbeans.modules.cnd.mixeddev.Triple;
import org.netbeans.modules.cnd.mixeddev.java.model.JavaEntityInfo;
import org.netbeans.modules.cnd.mixeddev.java.model.JavaFieldInfo;
import org.netbeans.modules.cnd.mixeddev.java.model.JavaMethodInfo;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Kudryavtsev <petrk@netbeans.org>
 */
@ActionID(id = "org.netbeans.modules.cnd.mixeddev.java.jni.actions.CopyJNISetFieldCodeAction", category = "MixedDevelopment")
@ActionRegistration(displayName = "#LBL_Action_CopyJNISetFieldCodeAction", lazy = false)
@ActionReferences(value = {@ActionReference(path = "Editors/text/x-java/Popup/MixedDevelopment", position=100)})
@NbBundle.Messages({"LBL_Action_CopyJNISetFieldCodeAction=Set field code"})
public class CopyJNISetFieldCodeAction extends AbstractCopyJNIAccessCodeAction {
    
    @Override
    protected boolean isEnabledAtPosition(Document doc, int caret) {
        JavaEntityInfo entity = resolveJavaEntity(doc, caret);
        return entity instanceof JavaFieldInfo;
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(MixedDevUtils.class, "cnd.mixeddev.copy_set_field_code"); // NOI18N
    }
    
    @Override
    protected void performAction(Node[] activatedNodes) {
        Triple<DataObject, Document, Integer> context = extractContext(activatedNodes);
        if (context != null) {
            final Document doc = context.second;
            final int caret = context.third;
            JavaEntityInfo entity = resolveJavaEntity(doc, caret);
            String code = generateEntitySetter(entity);
            if (code != null) {
                StatusDisplayer.getDefault().setStatusText(code);
                StringSelection ss = new StringSelection(code);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(ss, null);
            }
        }
    }
}
