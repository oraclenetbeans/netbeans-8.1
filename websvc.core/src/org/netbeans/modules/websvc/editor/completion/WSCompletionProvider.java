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

package org.netbeans.modules.websvc.editor.completion;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent; 
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.websvc.jaxws.api.JAXWSSupport;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Milan Kuchtiak
 */
public class WSCompletionProvider implements CompletionProvider {
    
    private static Logger LOG = Logger.getLogger( WSCompletionProvider.class.getCanonicalName().toString());
    
    private static final String[] BINDING_TYPES = {
        "SOAPBinding.SOAP11HTTP_BINDING", //NOI18N
        "SOAPBinding.SOAP11HTTP_MTOM_BINDING", //NOI18N
        "SOAPBinding.SOAP12HTTP_BINDING", //NOI18N
        "SOAPBinding.SOAP12HTTP_MTOM_BINDING", //NOI18N
        "HTTPBinding.HTTP_BINDING"}; //NOI18N
    
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return 0;
    }

    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType == CompletionProvider.COMPLETION_QUERY_TYPE) {
            return new AsyncCompletionTask(new WsCompletionQuery(component.
                    getSelectionStart()), component);
        }
        return null;
    }
    
    static final class WsCompletionQuery extends AsyncCompletionQuery implements 
        CancellableTask<CompilationController> 
    {
        private int caretOffset;
        private int anchorOffset;
        private List<CompletionItem> results;
        private JTextComponent component;
        private JAXWSSupport jaxWsSupport;
        private volatile boolean hasErrors;
        private RequestProcessor REQUEST_PROCESSOR = new RequestProcessor(WsCompletionQuery.class);
        
        private WsCompletionQuery(int caretOffset) {
            this.caretOffset = caretOffset;
        }
        
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                NbEditorUtilities.getFileObject(doc);
                final JavaSource js = JavaSource.forDocument(doc);
                if (js!=null) {
                    js.runUserActionTask(this, true);
                    if (isTaskCancelled()) {
                        return;
                    }

                    if (results != null) {
                        resultSet.addAllItems(results);
                    }
                    if (anchorOffset > -1) {
                        resultSet.setAnchorOffset(anchorOffset);
                    }
                }
            } 
            catch( CancellationException e ){
            }
            catch (IOException ex) {
                LOG.log( Level.WARNING , null , ex );
            }
            finally 
            {
                resultSet.finish();
            }
        }
        
        @Override
        public void cancel() {
        }

        @Override
        public void run(CompilationController controller) throws Exception {
            resolveCompletion(controller);
        }
         
        private void resolveCompletion(CompilationController controller) throws IOException {
            
            if (isTaskCancelled()) {
                return;
            }
            controller.toPhase(Phase.PARSED);
            results = new ArrayList<CompletionItem>();
            Env env = getCompletionEnvironment(controller, true);
            
            anchorOffset = env.getOffset();
            TreePath path = env.getPath();
            
            switch(path.getLeaf().getKind()) {
                case ANNOTATION:
                    break;
                case STRING_LITERAL:
                    createStringResults(controller,env);
                    break;
                case ASSIGNMENT:
                    createAssignmentResults(controller,env);
                    break;
                default:
            }
        }
        
        @Override
        protected void prepareQuery(JTextComponent component) {
            this.component = component;
            FileObject fo = NbEditorUtilities.getFileObject(component.getDocument());
            if (fo!=null) {
                jaxWsSupport = JAXWSSupport.getJAXWSSupport(fo);
            }
        }
        
        private Env getCompletionEnvironment(CompilationController controller, 
                boolean upToOffset) throws IOException 
        {
            int offset = caretOffset;
            String prefix = "";
            if (upToOffset && offset > 0) {
                TokenSequence<JavaTokenId> ts = controller.getTokenHierarchy().
                    tokenSequence(JavaTokenId.language());
                boolean successfullyMoved = false;
                if (ts.move(offset) == 0) // When right at the token end
                    successfullyMoved = ts.movePrevious(); // Move to previous token
                else
                    successfullyMoved = ts.moveNext(); // otherwise move to the token that "contains" the offset
                
                if (successfullyMoved && ts.offset() < offset) {
                    String token = ts.token().text().toString();
                    int length = Math.min( offset - ts.offset() , token.length());
                    prefix = token.substring(0, length);
                    offset=ts.offset();
                    if (ts.token().id() == JavaTokenId.STRING_LITERAL && prefix.startsWith("\"")) { //NOI18N
                        prefix = prefix.substring(1);
                        offset++;
                    } else if (ts.token().id() == JavaTokenId.EQ && prefix.startsWith("=")) { //NOI18N
                        prefix = prefix.substring(1);
                        offset++;
                    }
                }
            }
            controller.toPhase(Phase.PARSED);
            TreePath path = controller.getTreeUtilities().pathFor(caretOffset);
            return new Env(offset, prefix, path);
        }
        
        private boolean hasErrors(){
            return hasErrors;
        }
        
        private void createStringResults(CompilationController controller, Env env) 
            throws IOException {
            TreePath elementPath = env.getPath();
            TreePath parentPath = elementPath.getParentPath();
            Tree parent = parentPath.getLeaf();
            Tree grandParent = parentPath.getParentPath().getLeaf();
            switch (grandParent.getKind()) {                
                case ANNOTATION : {
                    switch (parent.getKind()) {
                        case ASSIGNMENT : {
                            ExpressionTree var = ((AssignmentTree)parent).getVariable();
                            if (var.getKind() == Kind.IDENTIFIER) {
                                Name name = ((IdentifierTree)var).getName();
                                if (!name.contentEquals("wsdlLocation") ||   //NOI18N 
                                        jaxWsSupport==null) 
                                {
                                    return;
                                }
                                controller.toPhase(Phase.ELEMENTS_RESOLVED);
                                TypeElement webMethodEl = controller
                                            .getElements().getTypeElement(
                                                    "javax.jws.WebService"); // NOI18N
                                if (webMethodEl == null) {
                                    hasErrors = true;
                                    return;
                                }
                                TypeMirror el = controller.getTrees().getTypeMirror(
                                                    parentPath.getParentPath());
                                if (el == null || el.getKind() == TypeKind.ERROR) {
                                    hasErrors = true;
                                    return;
                                }
                                if (controller.getTypes().isSameType(el,
                                            webMethodEl.asType()))
                                {
                                    FileObject wsdlFolder = jaxWsSupport
                                                .getWsdlFolder(false);
                                    if (wsdlFolder != null) {
                                        Enumeration<? extends FileObject> en = 
                                                wsdlFolder.getChildren(true);
                                        while (en.hasMoreElements()) {
                                            FileObject fo = en.nextElement();
                                            if (!fo.isData() || !"wsdl"   // NOI18N
                                                        .equalsIgnoreCase(fo.getExt()))
                                            {
                                                continue;
                                            }
                                            String wsdlPath = FileUtil.getRelativePath(
                                                           wsdlFolder.getParent()
                                                           .getParent(),fo);
                                            // Temporary fix for wsdl
                                            // files in EJB project
                                            if (wsdlPath.startsWith("conf/"))   // NOI18
                                            {
                                                wsdlPath = "META-INF/"+ wsdlPath// NOI18
                                                                  .substring(5); 
                                            }
                                            if (wsdlPath.startsWith(env.getPrefix()))
                                            {
                                                results.add(WSCompletionItem
                                                                .createWsdlFileItem(
                                                                        wsdlFolder,
                                                                        fo,
                                                                        env.getOffset()));
                                            }
                                        }
                                    }
                                }
                            }
                        } break; //ASSIGNMENT
                    }
                } break; // ANNOTATION
            }
        }
        
        private void createAssignmentResults(CompilationController controller, 
                Env env) throws IOException 
        {
            TreePath elementPath = env.getPath();
            TreePath parentPath = elementPath.getParentPath();
            Tree parent = parentPath.getLeaf();
            switch (parent.getKind()) {                
                case ANNOTATION : {
                    ExpressionTree var = ((AssignmentTree)elementPath.getLeaf()).
                        getVariable();
                    if (var!= null && var.getKind() == Kind.IDENTIFIER) {
                        Name name = ((IdentifierTree)var).getName();
                        if (name.contentEquals("value"))  {//NOI18N
                            controller.toPhase(Phase.ELEMENTS_RESOLVED);
                            TypeElement webParamEl = controller.getElements().
                                getTypeElement("javax.xml.ws.BindingType"); //NOI18N
                            if (webParamEl==null) {
                                hasErrors = true;
                                return;
                            }
                            TypeMirror el = controller.getTrees().getTypeMirror(parentPath);
                            if (el==null || el.getKind() == TypeKind.ERROR) {
                                hasErrors = true;
                                return;
                            }
                            if ( controller.getTypes().isSameType(el,webParamEl.asType())) 
                            {
                                for (String mode : BINDING_TYPES) {
                                    if (mode.startsWith(env.getPrefix())){ 
                                                results.add(WSCompletionItem.
                                                        createEnumItem(mode, 
                                                                "String", env.getOffset())); //NOI18N
                                    }
                                }
                            }
                        }
                    }
                } break; // ANNOTATION
            }
        }

        private static class Env {
            private int offset;
            private String prefix;
            private TreePath path;
            
            private Env(int offset, String prefix, TreePath path) {
                this.offset = offset;
                this.prefix = prefix;
                this.path = path;
            }
            
            public int getOffset() {
                return offset;
            }
            
            public String getPrefix() {
                return prefix;
            }
            
            public TreePath getPath() {
                return path;
            }
        }   
    }
}
