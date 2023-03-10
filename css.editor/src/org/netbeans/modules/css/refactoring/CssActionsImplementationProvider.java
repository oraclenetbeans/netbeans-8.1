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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.css.refactoring;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.css.editor.csl.CssLanguage;
import org.netbeans.modules.css.lib.api.CssParserResult;
import org.netbeans.modules.css.lib.api.CssTokenId;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.refactoring.spi.ui.ActionsImplementationProvider;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.netbeans.modules.web.common.api.LexerUtils;
import org.netbeans.modules.web.common.api.WebUtils;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

/**
 * Folder rename refactoring is enabled by the
 * org.netbeans.modules.web.common.refactoring.FolderActionsImplementationProvider
 *
 * The css refactoring just provides the rename plugin which handles css links
 * possibly affected by the folder rename.
 *
 * @author mfukala@netbeans.org
 */
@ServiceProvider(service = ActionsImplementationProvider.class, position = 1050)
public class CssActionsImplementationProvider extends ActionsImplementationProvider {

    private static final RequestProcessor RP = new RequestProcessor(CssActionsImplementationProvider.class);
    private static final Logger LOG = Logger.getLogger(CssActionsImplementationProvider.class.getName());

    @Override
    public boolean canRename(Lookup lookup) {
        Collection<? extends Node> nodes = lookup.lookupAll(Node.class);
        //we are able to rename only one node selection [at least for now ;-) ]
        if (nodes.size() != 1) {
            return false;
        }

        //check if the file is a file with .css extension or represents
        //an opened file which code embeds a css content on the caret position
        Node node = nodes.iterator().next();
        return isCssFile(node) || isRefactorableEditorElement(node); 

    }

    @Override
    public void doRename(Lookup selectedNodes) {
        EditorCookie ec = selectedNodes.lookup(EditorCookie.class);
        Runnable task;
        if (isFromEditor(ec)) {
            //editor refactoring
            task = new TextComponentTask(ec) {
                @Override
                protected RefactoringUI createRefactoringUI(CssElementContext context) {
                    return new CssRenameRefactoringUI(context);
                }
            };
        } else {
            //file or folder refactoring
            Collection<? extends Node> nodes = selectedNodes.lookupAll(Node.class);
            assert nodes.size() == 1;
            Node currentNode = nodes.iterator().next();
            task = new NodeToFileTask(currentNode) {
                @Override
                protected RefactoringUI createRefactoringUI(CssElementContext context) {
                    return new CssRenameRefactoringUI(context);
                }
            };
        }
        RP.post(task);
    }

    @Override
    public boolean canFindUsages(Lookup lookup) {
        Collection<? extends Node> nodes = lookup.lookupAll(Node.class);
        //we are able to rename only one node selection [at least for now ;-) ]
        if (nodes.size() != 1) {
            return false;
        }

        //check if the file is a file with .css extension or represents
        //an opened file which code embeds a css content on the caret position
        Node node = nodes.iterator().next();
        return isCssFile(node) || isRefactorableEditorElement(node);
    }

    @Override
    public void doFindUsages(Lookup lookup) {
        EditorCookie ec = lookup.lookup(EditorCookie.class);
        Runnable task;
        if (isFromEditor(ec)) {
            task = new TextComponentTask(ec) {
                //editor element context
                @Override
                protected RefactoringUI createRefactoringUI(CssElementContext context) {
                    return new WhereUsedUI(context);
                }
            };
        } else {
            //file context
            Collection<? extends Node> nodes = lookup.lookupAll(Node.class);
            assert nodes.size() == 1;
            Node currentNode = nodes.iterator().next();
            task = new NodeToFileTask(currentNode) {
                @Override
                protected RefactoringUI createRefactoringUI(CssElementContext context) {
                    return new WhereUsedUI(context);
                }
            };
        }

        RP.post(task);
    }

    private static boolean isCssFile(Node node) {
        //for the one thing check if the node represents a css file itself
        FileObject fo = getFileObjectFromNode(node);
        if (fo == null) {
            return false;
        }
        return CssLanguage.CSS_MIME_TYPE.equals(fo.getMIMEType());
    }

    /*
    * We can't access the parser here as we may (or always are?) be called
     * in the EDT.
     */
    private static boolean isRefactorableEditorElement(final Node node) {
        final AtomicBoolean result = new AtomicBoolean(false);
        Mutex.EVENT.readAccess(new Runnable() {

            @Override
            public void run() {
                EditorCookie ec = getEditorCookie(node);
                if (isFromEditor(ec)) {
                    //check if there's css code at the offset
                    final StyledDocument document = ec.getDocument();
                    JEditorPane pane = ec.getOpenedPanes()[0];
                    final int caret = pane.getCaretPosition();
                    document.render(new Runnable() {

                        @Override
                        public void run() {
                            result.set(null != LexerUtils.getTokenSequence(document, caret, CssTokenId.language(), true));
                        }
                    });
                }
            }
            
        });

        return result.get();
    }

    private static FileObject getFileObjectFromNode(Node node) {
        DataObject dobj = node.getLookup().lookup(DataObject.class);
        return dobj != null ? dobj.getPrimaryFile() : null;
    }

    private static boolean isFromEditor(EditorCookie ec) {
        if (ec != null && ec.getOpenedPanes() != null) {
            TopComponent activetc = TopComponent.getRegistry().getActivated();
            if (activetc instanceof CloneableEditorSupport.Pane) {
                return true;
            }
        }
        return false;
    }

    private static EditorCookie getEditorCookie(Node node) {
        return node.getLookup().lookup(EditorCookie.class);
    }

    private static abstract class NodeToFileTask extends UserTask implements Runnable {

        private final Node node;
        private CssElementContext context;
        private FileObject fileObject;

        public NodeToFileTask(Node node) {
            this.node = node;
        }

        @Override
        public void run(ResultIterator resultIterator) throws Exception {
            Collection<CssParserResult> results = new ArrayList<>();
            Snapshot snapshot = resultIterator.getSnapshot();
            try {
                if ("text/css".equals(snapshot.getMimeType())) { //NOI18N
                    results.add((CssParserResult) resultIterator.getParserResult());
                    return;
                }
                for (Embedding e : resultIterator.getEmbeddings()) {
                    run(resultIterator.getResultIterator(e));
                }
            } finally {
                context = new CssElementContext.File(fileObject, results);
            }
        }

        //runs in RequestProcessor
        @Override
        public void run() {
            DataObject dobj = node.getLookup().lookup(DataObject.class);
            if (dobj != null) {
                Runnable task;
                fileObject = dobj.getPrimaryFile();

                if (fileObject.isFolder()) {
                    //folder
                    task = new Runnable() {
                        @Override
                        public void run() {
                            UI.openRefactoringUI(createRefactoringUI(new CssElementContext.Folder(fileObject)));
                        }
                    };
                } else {
                    //css file
                    Source source = Source.create(fileObject);
                    try {
                        ParserManager.parse(Collections.singletonList(source), this);
                        task = new Runnable() {
                            @Override
                            public void run() {
                                UI.openRefactoringUI(createRefactoringUI(context));
                            }
                        };
                    } catch (ParseException ex) {
                        Exceptions.printStackTrace(ex);
                        return ;
                    }
                }
                
                //switch to EDT
                EventQueue.invokeLater(task);
            }

        }

        protected abstract RefactoringUI createRefactoringUI(CssElementContext context);
    }

    private static abstract class TextComponentTask extends UserTask implements Runnable {

        private final Document document;
        private final int caretOffset;
        private final int selectionStart;
        private final int selectionEnd;
        private RefactoringUI ui;

        public TextComponentTask(EditorCookie ec) {
            JTextComponent textC = ec.getOpenedPanes()[0];
            this.document = textC.getDocument();
            this.caretOffset = textC.getCaretPosition();
            this.selectionStart = textC.getSelectionStart();
            this.selectionEnd = textC.getSelectionEnd();
        }

        @Override
        public void run(ResultIterator ri) throws ParseException {
            Snapshot topLevelSnapshot = ri.getSnapshot();
            ResultIterator cssri = WebUtils.getResultIterator(ri, CssLanguage.CSS_MIME_TYPE);

            if (cssri != null) {
                CssParserResult result = (CssParserResult) cssri.getParserResult();
                if (result.getParseTree() != null) {
                    //the parser result seems to be quite ok,
                    //in case of serious parse issue the parse root is null
                    CssElementContext context = new CssElementContext.Editor(result, topLevelSnapshot, caretOffset, selectionStart, selectionEnd);
                    ui = context.isRefactoringAllowed() ? createRefactoringUI(context) : null;
                }
            }
        }

        //runs in RequestProcessor
        @Override
        public final void run() {
            try {
                Source source = Source.create(document);
                ParserManager.parse(Collections.singleton(source), this);
            } catch (ParseException e) {
                LOG.log(Level.WARNING, null, e);
                return;
            }

            //switch to EDT
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    TopComponent activetc = TopComponent.getRegistry().getActivated();

                    if (ui != null) {
                        UI.openRefactoringUI(ui, activetc);
                    } else {
                        JOptionPane.showMessageDialog(null, NbBundle.getMessage(CssActionsImplementationProvider.class, "ERR_CannotRefactorLoc"));//NOI18N
                    }
                }
            });
        }

        protected abstract RefactoringUI createRefactoringUI(CssElementContext context);
    }
}
