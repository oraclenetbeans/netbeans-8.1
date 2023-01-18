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
package org.netbeans.modules.php.dbgp.breakpoints;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.StyledDocument;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.php.dbgp.DebugSession;
import org.netbeans.modules.php.dbgp.SessionId;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.netbeans.modules.php.editor.parser.astnodes.Statement;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.DataEditorSupport;
import org.openide.text.Line;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author ads
 */
public class LineBreakpoint extends AbstractBreakpoint {
    private static final Logger LOGGER = Logger.getLogger(LineBreakpoint.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(LineBreakpoint.class);
    private final Line myLine;
    private final FileRemoveListener myListener;
    private FileChangeListener myWeakListener;
    private final String myFileUrl;
    private final Future<Boolean> isValidFuture;

    public LineBreakpoint(Line line) {
        myLine = line;
        myListener = new FileRemoveListener();
        FileObject fileObject = line.getLookup().lookup(FileObject.class);
        if (fileObject != null) {
            myWeakListener = WeakListeners.create(FileChangeListener.class, myListener, fileObject);
            fileObject.addFileChangeListener(myWeakListener);
            myFileUrl = fileObject.toURL().toString();
        } else {
            myFileUrl = ""; //NOI18N
        }
        isValidFuture = RP.submit(new Callable<Boolean>() {

            @Override
            public Boolean call() {
                final Boolean[] result = new Boolean[1];
                DataObject dataObject = DataEditorSupport.findDataObject(myLine);
                EditorCookie editorCookie = (EditorCookie) dataObject.getLookup().lookup(EditorCookie.class);
                final StyledDocument styledDocument = editorCookie.getDocument();
                if (styledDocument != null && styledDocument instanceof BaseDocument) {
                    try {
                        final BaseDocument baseDocument = (BaseDocument) styledDocument;
                        Source source = Source.create(baseDocument);
                        ParserManager.parse(Collections.singleton(source), new UserTask() {

                            @Override
                            public void run(ResultIterator resultIterator) throws Exception {
                                Parser.Result parserResult = resultIterator.getParserResult();
                                if (parserResult != null && parserResult instanceof PHPParseResult) {
                                    PHPParseResult phpParserResult = (PHPParseResult) parserResult;
                                    int rowStart = LineDocumentUtils.getLineStartFromIndex(baseDocument, myLine.getLineNumber());
                                    int contentStart = Utilities.getRowFirstNonWhite(baseDocument, rowStart);
                                    int contentEnd = LineDocumentUtils.getLineLastNonWhitespace(baseDocument, rowStart) + 1;
                                    StatementVisitor statementVisitor = new StatementVisitor(contentStart, contentEnd);
                                    statementVisitor.scan(phpParserResult.getProgram().getStatements());
                                    int properStatementOffset = statementVisitor.getProperStatementOffset();
                                    result[0] = properStatementOffset == contentStart;
                                }
                            }
                        });
                    } catch (ParseException ex) {
                        LOGGER.log(Level.FINE, null, ex);
                    }
                }
                return result[0];
            }
        });
    }

    public final void refreshValidity() {
        setValidity(isValid() ? VALIDITY.VALID : VALIDITY.INVALID, null);
    }

    private boolean isValid() {
        boolean result = false;
        try {
            Boolean semiResult = isValidFuture.get(2, TimeUnit.SECONDS);
            if (semiResult != null) {
                result = semiResult;
            }
        } catch (InterruptedException ex) {
            Thread.interrupted();
        } catch (ExecutionException | TimeoutException ex) {
            result = true;
            isValidFuture.cancel(true);
            LOGGER.log(Level.FINE, null, ex);
        } catch (CancellationException ex) {
            //noop
        }
        return result;
    }

    public Line getLine() {
        return myLine;
    }

    public String getFileUrl() {
        return myFileUrl;
    }

    @Override
    public int isTemp() {
        return 0;
    }

    @Override
    public boolean isSessionRelated(DebugSession session) {
        SessionId id = session != null ? session.getSessionId() : null;
        if (id == null) {
            return false;
        }
        return id.getProject() != null;
    }

    @Override
    public void removed() {
        FileObject fileObject = getLine().getLookup().lookup(FileObject.class);
        if (fileObject != null) {
            fileObject.removeFileChangeListener(myWeakListener);
        }
    }

    private class FileRemoveListener extends FileChangeAdapter {

        @Override
        public void fileDeleted(FileEvent arg0) {
            DebuggerManager.getDebuggerManager().removeBreakpoint(
                    LineBreakpoint.this);
        }

    }

    private static final class StatementVisitor extends DefaultVisitor {
        private int properStatementOffset;
        private final int contentStart;
        private final int contentEnd;

        private StatementVisitor(int contentStart, int contentEnd) {
            this.contentStart = contentStart;
            this.contentEnd = contentEnd;
        }

        @Override
        public void scan(ASTNode node) {
            if (node != null) {
                OffsetRange nodeRange = new OffsetRange(node.getStartOffset(), node.getEndOffset());
                if (node instanceof Statement && nodeRange.containsInclusive(contentStart) && nodeRange.containsInclusive(contentEnd)) {
                    properStatementOffset = node.getStartOffset();
                }
                super.scan(node);
            }
        }

        public int getProperStatementOffset() {
            return properStatementOffset;
        }

    }

}
