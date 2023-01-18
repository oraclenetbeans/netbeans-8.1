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

package org.netbeans.modules.xml.tools.java.actions;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.xml.actions.CollectDTDAction;
import org.netbeans.modules.xml.tools.generator.XMLGenerateAction;
import org.netbeans.modules.xml.tools.java.generator.SAXGeneratorSupport;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author sonali
 */
public class GenerateDocumentHandlerAction extends XMLGenerateAction implements CollectDTDAction.DTDAction {
        /** generated Serialized Version UID */
        private static final long serialVersionUID = 1342753912956042368L;

/**********
    public static synchronized GenerateDocumentHandlerAction getInstance() {
        GenerateDocumentHandlerAction actionInstance = null;
        String thisClassName = GenerateDocumentHandlerAction.class.getName();
        try {
            Class actionInstanceClass = Class.forName(thisClassName);
            actionInstance = (GenerateDocumentHandlerAction) actionInstanceClass.newInstance();
        } catch(Exception e) {
            Logger.getLogger(thisClassName).log(Level.SEVERE, "", e);
        }
        return actionInstance;
    }
**********/

        /* Human presentable name of the action. This should be
         * presented as an item in a menu.
         * @return the name of the action
         */
        public String getName () {
            return NbBundle.getMessage(XMLGenerateAction.class, "PROP_GenerateSAXHandler");
        }

        /* Help context where to find more about the action.
         * @return the help context for this action
         */
        public HelpCtx getHelpCtx () {
            return new HelpCtx (GenerateDocumentHandlerAction.class);
        }

        protected Class getOwnCookieClass () {
            return SAXGeneratorSupport.class;
        }
        
        protected boolean enable(Node[] node) {
        if (node.length == 0) {
            return false;
        }
        DataObject dobj = (DataObject) node[0].getLookup().lookup(DataObject.class);
        if (dobj == null) {
            return false;
        }
        FileObject fo = dobj.getPrimaryFile();
        Project project = FileOwnerQuery.getOwner(fo);
        if(project == null)
            return false;
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] srcGrps = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if (srcGrps == null || srcGrps.length == 0) {
            return false;
        } else {
            return true;
        }
    } 
} 

