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
package org.netbeans.modules.cnd.makeproject.source.bridge;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.cnd.api.lexer.CndLexerUtilities;
import org.netbeans.cnd.api.lexer.CppTokenId;
import org.netbeans.cnd.api.lexer.Filter;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.cnd.api.project.CodeAssistance;
import org.netbeans.modules.cnd.api.project.NativeFileItem;
import org.netbeans.modules.cnd.api.project.NativeFileItem.LanguageFlavor;
import org.netbeans.modules.cnd.api.project.NativeFileItemSet;
import org.netbeans.modules.cnd.api.project.NativeProject;
import org.netbeans.modules.cnd.api.project.NativeProjectItemsAdapter;
import org.netbeans.modules.cnd.source.spi.CndDocumentCodeStyleProvider;
import org.netbeans.modules.cnd.source.spi.CndSourcePropertiesProvider;
import org.netbeans.modules.cnd.utils.CndLanguageStandards;
import org.netbeans.modules.cnd.utils.MIMEExtensions;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.netbeans.spi.lexer.MutableTextInput;
import org.netbeans.spi.lexer.TokenHierarchyControl;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.Pair;
import org.openide.util.lookup.ServiceProvider;

/**
 * bridge which affects editor behavior based on options from makeproject.
 * @author Vladimir Voskresensky
 */
@ServiceProvider(path=CndSourcePropertiesProvider.REGISTRATION_PATH, service=CndSourcePropertiesProvider.class, position=1000)
public final class DocumentLanguageFlavorProvider implements CndSourcePropertiesProvider {

    @Override
    public void addProperty(DataObject dob, StyledDocument doc) {
        ListenerImpl old = (ListenerImpl) doc.getProperty(ListenerImpl.class);
        if (old != null) {
            old.unregister();
        }
        // check if it should have C++11 flavor
        Language<?> language = (Language<?>) doc.getProperty(Language.class);
        if (language != CppTokenId.languageCpp() && language != CppTokenId.languageC() && language != CppTokenId.languageHeader()) {
            return;
        }
        // fast check using NativeFileItemSet
        NativeFileItemSet nfis = dob.getLookup().lookup(NativeFileItemSet.class);
        if (nfis != null && !nfis.isEmpty()) {
            for (NativeFileItem nativeFileItem : nfis.getItems()) {
                doc.putProperty(ListenerImpl.class, new ListenerImpl(doc, dob, nativeFileItem));
                setLanguage(nativeFileItem, doc);
                rebuildTH(doc);
                return;
            }
        }
        FileObject primaryFile = dob.getPrimaryFile();
        if (primaryFile == null) {
            return;
        }
        if (setLanguage(primaryFile, doc)) {
            rebuildTH(doc);
            return;
        }
        Project owner = FileOwnerQuery.getOwner(primaryFile);
        if (owner == null) {
            return;
        }
        NativeProject np = owner.getLookup().lookup(NativeProject.class);
        if (np == null) {
            setDefaltLanguageFlavor(doc);
            return;
        }
        NativeFileItem nfi = np.findFileItem(primaryFile);
        if (nfi == null) {
            CndDocumentCodeStyleProvider cs = owner.getLookup().lookup(CndDocumentCodeStyleProvider.class);
            if (cs != null) {
                doc.putProperty(CndDocumentCodeStyleProvider.class, cs);
            }            
            setDefaltLanguageFlavor(doc);
            return;
        }
        doc.putProperty(ListenerImpl.class, new ListenerImpl(doc, dob, nfi));
        setLanguage(nfi, doc);
        rebuildTH(doc);
    }

    private void setDefaltLanguageFlavor(StyledDocument doc) {
        // stand-alone file without context
        String mime = (String) doc.getProperty("mimeType"); //NOI18N
        MIMEExtensions ee = MIMEExtensions.get(mime);
        if (ee == null) {
            return;
        }
        CndLanguageStandards.CndLanguageStandard defaultStandard = ee.getDefaultStandard();
        if (defaultStandard != null) {
            switch (defaultStandard) {
                case C89:
                    if (MIMENames.isHeader(mime)) {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C_HEADER, LanguageFlavor.C89, null, doc);
                    } else {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C, LanguageFlavor.C89, null, doc);
                    }
                    break;
                case C99:
                    if (MIMENames.isHeader(mime)) {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C_HEADER, LanguageFlavor.C99, null, doc);
                    } else {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C, LanguageFlavor.C99, null, doc);
                    }
                    break;
                case C11:
                    if (MIMENames.isHeader(mime)) {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C_HEADER, LanguageFlavor.C11, null, doc);
                    } else {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C, LanguageFlavor.C11, null, doc);
                    }
                    break;
                case CPP98:
                    if (MIMENames.isHeader(mime)) {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C_HEADER, LanguageFlavor.CPP, null, doc);
                    } else {
                        tryToSetDocumentLanguage(NativeFileItem.Language.CPP, LanguageFlavor.CPP, null, doc);
                    }
                    break;
                case CPP11:
                    if (MIMENames.isHeader(mime)) {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C_HEADER, LanguageFlavor.CPP11, null, doc);
                    } else {
                        tryToSetDocumentLanguage(NativeFileItem.Language.CPP, LanguageFlavor.CPP11, null, doc);
                    }
                    break;
                case CPP14:
                    if (MIMENames.isHeader(mime)) {
                        tryToSetDocumentLanguage(NativeFileItem.Language.C_HEADER, LanguageFlavor.CPP14, null, doc);
                    } else {
                        tryToSetDocumentLanguage(NativeFileItem.Language.CPP, LanguageFlavor.CPP14, null, doc);
                    }
                    break;
                default:
                    return;
            }
            rebuildTH(doc);
        }
        return;
    }
    
    private static boolean setLanguage(FileObject fo, StyledDocument doc) {
        CodeAssistance CAProvider = Lookup.getDefault().lookup(CodeAssistance.class);
        if (CAProvider != null) {
            Pair<NativeFileItem.Language, LanguageFlavor> pair = CAProvider.getHeaderLanguageFlavour(fo);
            return tryToSetDocumentLanguage(pair.first(), pair.second(), null, doc);
        } else {
            return false;
        }
    }
    
    private static boolean setLanguage(NativeFileItem nfi, StyledDocument doc) {
        final NativeProject nativeProject = nfi.getNativeProject();
        if (nativeProject != null) {
            final Lookup.Provider project = nativeProject.getProject();
            if (project != null) {
                Lookup lookup = project.getLookup();
                CndDocumentCodeStyleProvider cs = lookup.lookup(CndDocumentCodeStyleProvider.class);
                if (cs != null) {
                    doc.putProperty(CndDocumentCodeStyleProvider.class, cs);
                }
            }
        }
        return tryToSetDocumentLanguage(null, null, nfi, doc);
    }

    private static boolean tryToSetDocumentLanguage(NativeFileItem.Language itemLang, LanguageFlavor flavor, NativeFileItem nfi, StyledDocument doc) {
        if (itemLang == null) {
            itemLang = nfi.getLanguage();
        }
        Filter<?> filter = null;
        Language<?> language = null;
        switch (itemLang) {
            case C:
                language = CppTokenId.languageC();
                filter = CndLexerUtilities.getGccCFilter();
                break;
            case C_HEADER:
                language = CppTokenId.languageHeader();
                if (flavor == null) {
                    flavor = getLanguageFlavor(nfi);
                }
                if (flavor == NativeFileItem.LanguageFlavor.CPP11 ||
                        flavor == NativeFileItem.LanguageFlavor.CPP14) {
                    filter = CndLexerUtilities.getHeaderCpp11Filter();
                } else {
                    filter = CndLexerUtilities.getHeaderCppFilter();
                }
                break;
            case CPP:
                language = CppTokenId.languageCpp();
                if (flavor == null) {
                    flavor = getLanguageFlavor(nfi);
                }                
                if (flavor == NativeFileItem.LanguageFlavor.CPP11 ||
                        flavor == NativeFileItem.LanguageFlavor.CPP14) {
                    filter = CndLexerUtilities.getGccCpp11Filter();
                } else {
                    filter = CndLexerUtilities.getGccCppFilter();
                }
                break;
            case FORTRAN:
            case OTHER:
                return false;
        }
        assert language != null;
        assert filter != null;
        doc.putProperty(Language.class, language);
        InputAttributes lexerAttrs = (InputAttributes) doc.getProperty(InputAttributes.class);
        lexerAttrs.setValue(language, CndLexerUtilities.LEXER_FILTER, filter, true);
        return true;
    }

    private static LanguageFlavor getLanguageFlavor(NativeFileItem nfi) {
        LanguageFlavor flavor = nfi.getLanguageFlavor();
        if (flavor == LanguageFlavor.UNKNOWN) {
            // Ask flavor of first start compilation unit.
            CodeAssistance CAProvider = Lookup.getDefault().lookup(CodeAssistance.class);
            if (CAProvider != null) {
                flavor = CAProvider.getStartFileLanguageFlavour(nfi).second();
            }
        }    
        return flavor;
    }

    private static void rebuildTH(final StyledDocument doc) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BaseDocument bdoc = (BaseDocument) doc;
                try {
                    if (bdoc != null) {
                        bdoc.extWriteLock();
                        MutableTextInput mti = (MutableTextInput) bdoc.getProperty(MutableTextInput.class);
                        if (mti != null) {
                            TokenHierarchyControl thc = mti.tokenHierarchyControl();
                            if (thc != null) {
                                thc.rebuild();
                            }
                        }
                    }
                } finally {
                    if (bdoc != null) {
                        bdoc.extWriteUnlock();
                    }
                }
            }
        });
    }

    private final static class ListenerImpl extends NativeProjectItemsAdapter implements PropertyChangeListener {
        private static final boolean TRACE = false;
        private final Reference<StyledDocument> docRef;
        private final String path;
        private final FileObject fo;
        private final Reference<NativeProject> prjRef;
        private LanguageFlavor languageFlavor;

        public ListenerImpl(StyledDocument doc, DataObject dob, NativeFileItem nativeFileItem) {
            this.docRef = new WeakReference<StyledDocument>(doc);
            this.fo = dob.getPrimaryFile();
            this.path = nativeFileItem.getAbsolutePath();
            NativeProject nativeProject = nativeFileItem.getNativeProject();
            this.prjRef = new WeakReference<NativeProject>(nativeProject);
            this.languageFlavor = getLanguageFlavor(nativeFileItem);
            if (nativeProject != null) {
                nativeProject.addProjectItemsListener(ListenerImpl.this);
            } else {
                System.err.println("no native project for " + nativeFileItem); 
            }
            EditorRegistry.addPropertyChangeListener(ListenerImpl.this);
            if (TRACE) System.err.println(path + " created Listener " + System.identityHashCode(ListenerImpl.this));
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (TRACE) System.err.println(path + " propertyChange Listener " + System.identityHashCode(this));
            StyledDocument doc = docRef.get();
            NativeProject project = prjRef.get();
            if (doc == null || project == null) {
                unregister();
                return;
            }
            if ("usedByCloneableEditor".equals(evt.getPropertyName())) { // NOI18N
                if (Boolean.FALSE.equals(evt.getNewValue())) {
                    unregister();
                }
            } else if (EditorRegistry.COMPONENT_REMOVED_PROPERTY.equals(evt.getPropertyName())) {
                JTextComponent oldValue = (JTextComponent) evt.getOldValue();
                if (oldValue != null && doc.equals(oldValue.getDocument())) {
                    unregister();
                }
            }
        }

        private void unregister() {
            if (TRACE) System.err.println("unregister Listener " + System.identityHashCode(this) + " for " + path);
            EditorRegistry.removePropertyChangeListener(this);
            NativeProject nativeProject = this.prjRef.get();
            if (nativeProject != null) {
                nativeProject.removeProjectItemsListener(this);
            }
            StyledDocument doc = docRef.get();
            if (doc != null) {
                doc.putProperty(ListenerImpl.class, null);
            }
        }

        @Override
        public void filesAdded(List<NativeFileItem> fileItems) {
            filesPropertiesChanged(fileItems);
        }

        private void filePropertiesChanged(NativeFileItem fileItem) {
            if (fileItem != null && path.equals(fileItem.getAbsolutePath())) {
                final StyledDocument doc = docRef.get();
                if (doc == null) {
                    unregister();
                    return;
                }
                if (TRACE) System.err.println(path + " Item Listener " + System.identityHashCode(this));
                LanguageFlavor newFlavor = getLanguageFlavor(fileItem);
                if (!languageFlavor.equals(newFlavor)) {
                    setLanguage(fileItem, doc);
                    languageFlavor = newFlavor;
                    rebuildTH(doc);
                }
            }
        }

        @Override
        public void filesPropertiesChanged(List<NativeFileItem> fileItems) {
            for (NativeFileItem nativeFileItem : fileItems) {
                filePropertiesChanged(nativeFileItem);
            }
        }

        @Override
        public void filesPropertiesChanged() {
            NativeProject nativeProject = this.prjRef.get();
            if (nativeProject != null) {
                NativeFileItem findFileItem = nativeProject.findFileItem(fo);
                filePropertiesChanged(findFileItem);
            } else {
                unregister();
            }
        }

        @Override
        public void projectDeleted(NativeProject nativeProject) {
            unregister();
        }
    }
}
