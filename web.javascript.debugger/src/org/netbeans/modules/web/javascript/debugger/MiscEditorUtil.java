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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

package org.netbeans.modules.web.javascript.debugger;

import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.editor.DialogBinding;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.Project;
import org.netbeans.editor.EditorUI;
import org.netbeans.modules.web.common.api.RemoteFileCache;
import org.netbeans.modules.web.common.api.ServerURLMapping;
import org.netbeans.modules.web.javascript.debugger.browser.ProjectContext;
import org.netbeans.modules.web.webkit.debugging.api.Debugger;
import org.netbeans.modules.web.webkit.debugging.api.debugger.CallFrame;
import org.netbeans.modules.web.webkit.debugging.api.debugger.Script;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.netbeans.spi.viewmodel.Models;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

public final class MiscEditorUtil {

    public static final String HTML_MIME_TYPE = "text/html";
    public static final String PHP_MIME_TYPE = "text/x-php5";
    public static final String JAVASCRIPT_MIME_TYPE = "text/javascript";
	
    
    public static final String BREAKPOINT_ANNOTATION_TYPE = "Breakpoint"; //NOI18N
    public static final String DISABLED_BREAKPOINT_ANNOTATION_TYPE =  "DisabledBreakpoint"; //NOI18N
    public static final String CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE =  "CondBreakpoint"; //NOI18N
    public static final String DISABLED_CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE =  "DisabledCondBreakpoint"; //NOI18N
    public static final String DEACTIVATED_BREAKPOINT_SUFFIX = "_stroke"; //NOI18N
    public static final String BROKEN_BREAKPOINT_SUFFIX = "_broken"; //NOI18N
    public static final String CURRENT_LINE_ANNOTATION_TYPE =  "CurrentPC"; //NOI18N
    public static final String CALL_STACK_FRAME_ANNOTATION_TYPE =  "CallSite"; //NOI18N
    public static final String PROP_LINE_NUMBER = "lineNumber"; //NOI18N
    
    private static final Logger LOG = Logger.getLogger(MiscEditorUtil.class.getName());
    
    private static final RequestProcessor RP = new RequestProcessor(MiscEditorUtil.class.getName());
    
    public static String getAnnotationTooltip(String annotationType) {
        return getMessage("TOOLTIP_"+ annotationType);
    }
    
    private static String getMessage(final String key) {
        return NbBundle.getBundle(MiscEditorUtil.class).getString(key);
    }

    public static void openFileObject(FileObject fileObject) {
        if (fileObject == null) {
            return;
        }
        
        try {
            DataObject dataObject = DataObject.find(fileObject);
            EditorCookie cookie = dataObject.getCookie(EditorCookie.class);
            cookie.open();
        } catch (DataObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Call this method only when local file is concerned (eg file://myproject/src/foo.html) 
     * and no project specific URL conversion is required. For example deserializing 
     * file:// protocol URLs or handling local file URLs directly entered by user.
     */
    public static Line getLine(final String filePath, final int lineNumber) {
        return getLineImpl(null, filePath, lineNumber);
    }
    
    /**
     * Call this method for all URLs which are coming from browser and which 
     * potentially needs to be converted from project's deployment URL 
     * (eg http://localhost/smth/foo.html) into project's local file URL 
     * (ie file://myproject/src/foo.html).
     * As this happens mainly when URL is coming from browser the Script parameter
     * is used in this method signature.
     */
    public static Line getLine(final Project project, final Script script, final int lineNumber) {
        return getLineImpl(project, script.getURL(), lineNumber);
    }
    
    public static Line getLine(final Project project, final String stringURL, final int lineNumber) {
        return getLineImpl(project, stringURL, lineNumber);
    }
    
    private static Line getLineImpl(Project project, final String filePath, final int lineNumber) {
        if (filePath == null || lineNumber < 0) {
            return null;
        }
        
        FileObject fileObject = null;
        try {
            URI uri = URI.create(filePath);
            if (uri.isAbsolute()) {
                URL url;
                try {
                    url = uri.toURL();
                } catch (MalformedURLException muex) {
                    // Issue 230657
                    LOG.log(Level.INFO, "Cannot resolve " + filePath, muex); // NOI18N
                    return null;
                }
                if (project != null) {
                    fileObject = ServerURLMapping.fromServer(project, url);
                }
                if (fileObject == null && (filePath.startsWith("http:") || filePath.startsWith("https:"))) {    // NOI18N
                    fileObject = RemoteFileCache.getRemoteFile(url);
                }
            }
            if (fileObject == null) {
                File file;
                if (filePath.startsWith("file:/")) {
                    file = Utilities.toFile(uri);
                } else {
                    file = new File(filePath);
                }
                fileObject = FileUtil.toFileObject(FileUtil.normalizeFile(file));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (fileObject == null) {
            LOG.log(Level.INFO, "Cannot resolve \"{0}\"", filePath);
            return null;
        }

        LineCookie lineCookie = getLineCookie(fileObject);
        if (lineCookie == null) {
            LOG.log(Level.INFO, "No line cookie for \"{0}\"", fileObject);
            return null;
        }
        try {
            return lineCookie.getLineSet().getCurrent(lineNumber);
        } catch (IndexOutOfBoundsException ioob) {
            List<? extends Line> lines = lineCookie.getLineSet().getLines();
            if (lines.size() > 0) {
                return lines.get(lines.size() - 1);
            } else {
                return null;
            }
        }
    }

    public static Line getLine(final FileObject fileObject, final int lineNumber) {
        if (fileObject != null) {
            LineCookie lineCookie = MiscEditorUtil.getLineCookie(fileObject);
            if (lineCookie != null) {
                Line.Set ls = lineCookie.getLineSet();
                if (ls != null) {
                    try {
                        return ls.getCurrent(lineNumber - 1);
                    } catch (IndexOutOfBoundsException ioob) {
                        List<? extends Line> lines = ls.getLines();
                        if (lines.size() > 0) {
                            return lines.get(lines.size() - 1);
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static LineCookie getLineCookie(final FileObject fo) {
        LineCookie result = null;
        try {
            DataObject dataObject = DataObject.find(fo);
            if (dataObject != null) {
                result = dataObject.getCookie(LineCookie.class);
            }
        } catch (DataObjectNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void showLine(final Line line, final boolean toFront) {
        if (line == null) {
            return;
        }

        EventQueue.invokeLater(new Runnable() {

            public void run() {
                line.show(Line.ShowOpenType.REUSE, 
                    toFront ? Line.ShowVisibilityType.FRONT : Line.ShowVisibilityType.FOCUS);
            }
        });
    }

    public static void showLine(final Line line) {
        showLine(line, false);
    }

    /**
     * Returns current editor line (the line where the caret currently is).
     * Might be <code>null</code>. Works only for
     * {@link NbJSUtil#isJavascriptSource supported mime-types}. For unsupported
     * ones returns <code>null</code>.
     */
    public static Line getCurrentLine() {
        FileObject fo = EditorContextDispatcher.getDefault().getCurrentFile();
        if (fo == null) {
            return null;
        }
        if (!MiscEditorUtil.isJavascriptSource(fo) && !MiscEditorUtil.isJSWrapperSource(fo)) {
            return null;
        }
        return EditorContextDispatcher.getDefault().getCurrentLine();
    }
    
    /**
     * Test whether the line is in JavaScript source.
     * @param line The line to test
     * @return <code>true</code> when the line is in JavaScript source, <code>false</code> otherwise.
     */
    public static boolean isInJavaScript(Line line) {
        LOG.log(Level.FINER, "\nisInJavaScript({0}):", line);
        FileObject fo = line.getLookup().lookup(FileObject.class);
        if (isJavascriptSource(fo)) {
            LOG.fine("is JavaScript source file => true");
            return true;
        }
        EditorCookie editorCookie = line.getLookup().lookup(EditorCookie.class);
        StyledDocument document = editorCookie.getDocument();
        Boolean isJS = null;
        ((AbstractDocument) document).readLock();
        try {
            TokenHierarchy<Document> th = TokenHierarchy.get((Document) document);
            int ln = line.getLineNumber();
            int offset = NbDocument.findLineOffset(document, ln);
            int maxOffset = document.getLength() - 1;
            int maxLine = NbDocument.findLineNumber(document, maxOffset);
            int offset2;
            if (ln + 1 > maxLine) {
                offset2 = maxOffset;
            } else {
                offset2 = NbDocument.findLineOffset(document, ln+1) - 1;
            }
            // The line has offsets <offset, offset2>
            Set<LanguagePath> languagePaths = th.languagePaths();
            for (LanguagePath lp : languagePaths) {
                List<TokenSequence<?>> tsl = th.tokenSequenceList(lp, offset, offset2);
                for (TokenSequence ts : tsl) {
                    if (ts.moveNext()) {
                        /*int to = ts.offset();
                        if (LOG.isLoggable(Level.FINER)) {
                            LOG.finer("Token offset = "+to+", offsets = <"+offset+", "+offset2+">, mimeType = "+ts.language().mimeType());
                        }
                        if (!(offset <= to && to < offset2)) {
                            continue;
                        }*/
                        TokenSequence ets;
                        ets = ts.embedded();
                        if (ets != null) {
                            ts = ets;
                        }
                        String mimeType = ts.language().mimeType();
                        LOG.log(Level.FINER, "Have language {0}", mimeType);
                        if (isJS == null && JAVASCRIPT_MIME_TYPE.equals(mimeType)) {
                            isJS = true;
                            if (!LOG.isLoggable(Level.FINER)) {
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            ((AbstractDocument) document).readUnlock();
        }
        LOG.log(Level.FINER, "isJS = {0}", isJS);
        return isJS != null && isJS;
    }

    /**
     * Test if the FileObject is a JavaScript wrapper source, like HTML or PHP.
     */
    public static boolean isJSWrapperSource(final FileObject fo) {
        String mimeType = fo.getMIMEType();
        return HTML_MIME_TYPE.equals(mimeType) || PHP_MIME_TYPE.equals(mimeType);
    }

    /**
     * Supported mime-types:
     *
     * <ul>
     * <li>text/javascript</li>
     * </ul>
     */
    public static boolean isJavascriptSource(final FileObject fo) {
        return JAVASCRIPT_MIME_TYPE.equals(fo.getMIMEType());
    }

    /**
     * Checks whether the MIME type is JavaScript or a JavaScript wrapper source.
     */
    public static boolean isJSOrWrapperMIMEType(String mimeType) {
        switch (mimeType) {
            case JAVASCRIPT_MIME_TYPE:
            case HTML_MIME_TYPE:
            case PHP_MIME_TYPE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Goes to editor location
     * @param fileObject
     * @param lineNumber - assumes index starts at 1 instead of 0.
     */
    public static final void goToSource(FileObject fileObject, int lineNumber) {
        Line line = MiscEditorUtil.getLine(fileObject.getPath(), lineNumber - 1);
        MiscEditorUtil.showLine(line);
    }
    
    public static Action createDebuggerGoToAction (final ProjectContext pc) {
        Models.ActionPerformer actionPerform =  new Models.ActionPerformer () {
            @Override
            public boolean isEnabled (Object object) {
                return true;
            }
            @Override
            public void perform (Object[] nodes) {
                Object node = nodes[0];
                /*if( node instanceof JSWindow ){
                    JSWindow window = (JSWindow)node;
                    String strURI = window.getURI();
                    JSSource source = JSFactory.createJSSource(strURI);
                    MiscEditorUtil.openFileObject(debugger.getFileObjectForSource(source));                    
                } else if( node instanceof JSSource ){
                    JSSource jsSource = (JSSource) node;
                    FileObject fileObject = debugger.getFileObjectForSource(jsSource);
                    MiscEditorUtil.openFileObject(fileObject);
                } else*/ if ( node instanceof CallFrame ){
                    CallFrame cf = ((CallFrame)node);
                    Project project = pc.getProject();
                    Line line = MiscEditorUtil.getLine(project, cf.getScript(), cf.getLineNumber());
                    if ( line != null ) {
                        showLine(line, true);
                    }
                }
            }
        };
        return Models.createAction(
                NbBundle.getMessage(MiscEditorUtil.class, "CTL_GoToSource"),
                actionPerform, Models.MULTISELECTION_TYPE_EXACTLY_ONE);
    }
    
    public static void setupContext(final JEditorPane editorPane, final Runnable contextSetUp) {
        //EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-java");
        //editorPane.setEditorKit(kit); - Do not set it, setupContext() will do the job.
        DebuggerEngine en = DebuggerManager.getDebuggerManager ().getCurrentEngine();
        if (EventQueue.isDispatchThread() && en != null) {
            final DebuggerEngine den = en;
            RP.post(new Runnable() {
                @Override
                public void run() {
                    final Context c = retrieveContext(den);
                    if (c != null) {
                        setupContext(editorPane, c.url, c.line, c.debugger);
                        if (contextSetUp != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    contextSetUp.run();
                                }
                            });
                        }
                    }
                }
            });
            Context c = retrieveContext(null);
            if (c != null) {
                setupContext(editorPane, c.url, c.line, c.debugger);
            } else {
                setupUI(editorPane);
            }
            return ;
        }
        Context c = retrieveContext(en);
        if (c != null) {
            setupContext(editorPane, c.url, c.line, c.debugger);
        } else {
            setupUI(editorPane);
        }
        if (contextSetUp != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    contextSetUp.run();
                }
            });
        }
    }

    private static Context retrieveContext(DebuggerEngine en) {
        CallFrame cf = null;
        Debugger d = null;
        if (en != null) {
            d = en.lookupFirst(null, Debugger.class);
            if (d != null) {
                cf = d.getCurrentCallFrame();
            }
        }
        boolean adjustContext = true;
        Context c;
        Script script;
        if (cf != null && (script = cf.getScript()) != null) {
            Session session = en.lookupFirst(null, Session.class);
            String language = session.getCurrentLanguage();
            c = new Context();
            c.url = script.getURL();
            c.line = cf.getLineNumber();
            c.debugger = d;
            if (c.line > 0) {
                adjustContext = false;
                c.line--;
            }
        } else {
            EditorContextDispatcher context = EditorContextDispatcher.getDefault();
            String url = context.getMostRecentURLAsString();
            if (url != null && url.length() > 0) {
                c = new Context();
                c.url = url;
                c.line = context.getMostRecentLineNumber();
                c.debugger = d;
            } else {
                return null;
            }
        }
        if (adjustContext && !EventQueue.isDispatchThread()) {
            // Do the adjustment only outside of AWT.
            // When in AWT, the context update in RP is spawned.
            //adjustLine(c);
        }
        return c;
    }

    public static void setupContext(final JEditorPane editorPane, String url, int line) {
        setupContext(editorPane, url, line, null);
    }

    public static void setupContext(final JEditorPane editorPane, String url, final int line, final Debugger debugger) {
        final FileObject file;
        try {
            file = URLMapper.findFileObject (new URL (url));
            if (file == null) {
                return;
            }
        } catch (MalformedURLException e) {
            // null dobj
            return;
        }
        //System.err.println("WatchPanel.setupContext("+file+", "+line+", "+offset+")");
        // Do the binding for text files only:
        if (file.getMIMEType().startsWith("text/")) { // NOI18N
            Runnable bindComponentToDocument = new Runnable() {
                @Override
                public void run() {
                    String origText = editorPane.getText();
                    DialogBinding.bindComponentToFile(file, (line >= 0) ? line : 0, 0, 0, editorPane);
                    Document editPaneDoc = editorPane.getDocument();
                    //editPaneDoc.putProperty("org.netbeans.modules.editor.java.JavaCompletionProvider.skipAccessibilityCheck", "true");
                    editorPane.setText(origText);
                }
            };
            if (EventQueue.isDispatchThread()) {
                bindComponentToDocument.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(bindComponentToDocument);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        setupUI(editorPane);
    }
    
    private static void setupUI(final JEditorPane editorPane) {
        Runnable runnable = new Runnable() {
            public void run() {
                EditorUI eui = org.netbeans.editor.Utilities.getEditorUI(editorPane);
                if (eui == null) {
                    return ;
                }
                editorPane.putClientProperty(
                    "HighlightsLayerExcludes", //NOI18N
                    "^org\\.netbeans\\.modules\\.editor\\.lib2\\.highlighting\\.CaretRowHighlighting$" //NOI18N
                );
                // Do not draw text limit line
                try {
                    java.lang.reflect.Field textLimitLineField = EditorUI.class.getDeclaredField("textLimitLineVisible"); // NOI18N
                    textLimitLineField.setAccessible(true);
                    textLimitLineField.set(eui, false);
                } catch (Exception ex) {}
                editorPane.repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static final class Context {
        public String url;
        public int line;
        public Debugger debugger;
    }

}
