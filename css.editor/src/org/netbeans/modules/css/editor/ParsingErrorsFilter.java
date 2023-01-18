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
package org.netbeans.modules.css.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.css.lib.api.FilterableError;
import org.netbeans.modules.css.lib.api.FilterableError.SetFilterAction;
import org.netbeans.modules.css.lib.api.ProblemDescription;
import org.netbeans.modules.editor.NbEditorDocument;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.spi.lexer.MutableTextInput;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Serves as a filter for filtering default lexing/parsing problems.
 *
 * User may disable particular lexing/parsing problem (the error description is
 * the key), or can disable errorchecking for the whole file or for a folder.
 *
 * @author marek
 */
public class ParsingErrorsFilter {

    private static final RequestProcessor RP = new RequestProcessor(ParsingErrorsFilter.class);

    public static final String DISABLE_ERROR_CHECKS_KEY = "disable_error_checking_CSS"; //NOI18N

    public static Collection<SetFilterAction> getEnableFilterAction(@NonNull FileObject file, @NonNull String key) {
        Collection<SetFilterAction> actions = new ArrayList<>();
        actions.add(new SetFilterForKeyAction(file, key, true));
        actions.addAll(getEnableFilterAction(file));
        return actions;
    }
    
    private static Collection<SetFilterAction> getEnableFilterAction(@NonNull FileObject file) {
        FileObject source = file;
        Collection<SetFilterAction> actions = new ArrayList<>();
        for (; file != null && FileOwnerQuery.getOwner(file) != null; file = file.getParent()) {
            actions.add(new SetFileFilterAction(source, file, true));
        }
        return actions;
    }
    
    /**
     * Checks if the {@link ProblemDescription} in the particular file is
     * filtered or not.
     *
     * @param file
     * @param desc
     * @return
     */
    public static SetFilterAction getDisableFilterAction(@NonNull FileObject file, @NonNull String key) {
        SetFilterAction disableFilterAction = getDisableFilterAction(file);
        if (disableFilterAction == null) {
            //else check by parsing error
            //note: since neither ExtCss3Lexer nor NbParseTreeBuilder provide reasonble classes 
            //of errors which we could use for the filtering, we need to use the error description itself.
            if (CssPreferences.isErrorCheckingDisabledForCssErrorKey(key)) {
                return new SetFilterForKeyAction(file, key, false);
            }
        }

        return disableFilterAction;
    }

    /**
     * Checks if the parsing errors are filtered for this file or any of its
     * parent folders.
     *
     * @param file
     * @return
     */
    private static SetFilterAction getDisableFilterAction(@NonNull FileObject file) {
        FileObject source = file;
        for (; file != null && FileOwnerQuery.getOwner(file) != null; file = file.getParent()) {
            if (file.getAttribute(DISABLE_ERROR_CHECKS_KEY) != null) {
                return new SetFileFilterAction(source, file, false);
            }
        }
        return null;
    }

    @NbBundle.Messages({
        "# {0} - file name",
        "disableFilterForFile=Disable filtering of CSS errors in \"{0}\"",
        "# {0} - file name",
        "enableFilterForFile=Filter out CSS parsing errors in \"{0}\""
    })
    private static class SetFileFilterAction implements FilterableError.SetFilterAction {

        private final FileObject file, source;
        private final boolean enable;

        public SetFileFilterAction(FileObject source, FileObject file, boolean enable) {
            this.source = source;
            this.file = file;
            this.enable = enable;
        }

        @Override
        public void run() {
            try {
                file.setAttribute(DISABLE_ERROR_CHECKS_KEY, enable ? Boolean.TRUE.toString() : null);
                refresh(source);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public String getDisplayName() {
            String path = file.getPath();
            return enable ? Bundle.enableFilterForFile(path) : Bundle.disableFilterForFile(path);
        }

    }

    @NbBundle.Messages({
        "# {0} - filtered error key",
        "disableFilterForKey=Disable filtering of \"{0}\" CSS parsing error",
        "# {0} - filtered error key",
        "enableFilterForKey=Filter out the \"{0}\" CSS parsing error"
    })
    private static class SetFilterForKeyAction implements FilterableError.SetFilterAction {

        private final String key;
        private final boolean enable;
        private final FileObject file;

        public SetFilterForKeyAction(FileObject file, String key, boolean enable) {
            this.file = file;
            this.key = key;
            this.enable = enable;
        }

        @Override
        public void run() {
            CssPreferences.setCssErrorChecking(key, !enable);
            refresh(file);
        }

        @Override
        public String getDisplayName() {
            return enable ? Bundle.enableFilterForKey(key) : Bundle.disableFilterForKey(key);
        }

    }

    private static void refresh(FileObject file) {
        try {
            reindexActionItems();
            reindexFile(file);
            refreshDocument(file);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static void reindexFile(final FileObject fo) {
        RP.post(new Runnable() {
            @Override
            public void run() {
                //refresh Action Items for this file
                ClassPath cp = ClassPath.getClassPath(fo, "classpath/html5"); //NOI18N
                if (cp != null) {
                    FileObject root = cp.findOwnerRoot(fo);
                    IndexingManager.getDefault().refreshIndexAndWait(root.toURL(),
                        Collections.singleton(fo.toURL()), true, false);
                }   
            }
        });
    }

    private static void reindexActionItems() {
        RP.post(new Runnable() {
            @Override
            public void run() {
                //refresh all Action Items 
                IndexingManager.getDefault().refreshAllIndices("TLIndexer"); //NOI18N
            }
        });

    }

    private static void refreshDocument(final FileObject fo) throws IOException {
        RP.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DataObject dobj = DataObject.find(fo);
                    EditorCookie editorCookie = dobj.getLookup().lookup(EditorCookie.class);
                    StyledDocument document = editorCookie.openDocument();
                    forceReparse(document);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });

    }

    //force reparse of *THIS document only* => hints update
    private static void forceReparse(final Document doc) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                NbEditorDocument nbdoc = (NbEditorDocument) doc;
                nbdoc.runAtomic(new Runnable() {

                    @Override
                    public void run() {
                        MutableTextInput mti = (MutableTextInput) doc.getProperty(MutableTextInput.class);
                        if (mti != null) {
                            mti.tokenHierarchyControl().rebuild();
                        }
                    }
                });
            }
        });
    }

}
