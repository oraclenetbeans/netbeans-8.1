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
package org.netbeans.modules.php.editor.actions;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.editor.BaseAction;
import org.netbeans.modules.editor.MainMenuAction;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser.Result;
import static org.netbeans.modules.php.api.util.FileUtils.PHP_MIME_TYPE;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.actions.ImportData.ItemVariant;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
import org.netbeans.modules.php.editor.indent.CodeStyle;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
@Messages({
    "FixUsesLabel=Fix Uses...",
    "LongName=Fix Uses in Current Namespace"
})
@EditorActionRegistration(
    name = FixUsesAction.ACTION_NAME,
    mimeType = PHP_MIME_TYPE,
    shortDescription = "Fixes use statements.",
    popupText = "#FixUsesLabel"
)
public class FixUsesAction extends BaseAction {

    static final String ACTION_NAME = "fix-uses"; //NOI18N
    private static final String PREFERENCES_NODE_KEY = FixUsesAction.class.getName();
    private static final String KEY_REMOVE_UNUSED_USES = "remove.unused.uses"; //NOI18N
    private static final boolean REMOVE_UNUSED_USES_DEFAULT = true;

    public FixUsesAction() {
        super(MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
    }

    @Override
    public void actionPerformed(ActionEvent evt, final JTextComponent target) {
        if (target != null) {
            final int caretPosition = target.getCaretPosition();
            final AtomicBoolean cancel = new AtomicBoolean();
            final AtomicReference<ImportData> importData = new AtomicReference<>();
            final UserTask task = new UserTask() {

                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result parserResult = resultIterator.getParserResult();
                    if (parserResult instanceof PHPParseResult) {
                        if (cancel.get()) {
                            return;
                        }

                        final ImportData data = computeUses((PHPParseResult) parserResult, caretPosition);

                        if (cancel.get()) {
                            return;
                        }
                        if (data.shouldShowUsesPanel) {
                            if (!cancel.get()) {
                                importData.set(data);
                            }
                        } else {
                            performFixUses((PHPParseResult) parserResult, data, data.getDefaultVariants(), isRemoveUnusedUses());
                        }
                    }
                }
            };

            ProgressUtils.runOffEventDispatchThread(new Runnable() {

                @Override
                public void run() {
                    try {
                        ParserManager.parse(Collections.singleton(Source.create(target.getDocument())), task);
                    } catch (ParseException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }, Bundle.LongName(), cancel, false);

            if (importData.get() != null && !cancel.get()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        showFixUsesDialog(target, importData.get());
                    }
                });
            }
        }
    }

    private static Preferences getPreferences() {
        return NbPreferences.forModule(FixUsesAction.class).node(PREFERENCES_NODE_KEY);
    }

    private static boolean isRemoveUnusedUses() {
        return getPreferences().getBoolean(KEY_REMOVE_UNUSED_USES, REMOVE_UNUSED_USES_DEFAULT);
    }

    private static void setRemoveUnusedUses(final boolean removeUnusedUses) {
        getPreferences().putBoolean(KEY_REMOVE_UNUSED_USES, removeUnusedUses);
    }

    private static ImportData computeUses(final PHPParseResult parserResult, final int caretPosition) {
        Map<String, List<UsedNamespaceName>> filteredExistingNames = new UsedNamesCollector(parserResult, caretPosition).collectNames();
        Index index = parserResult.getModel().getIndexScope().getIndex();
        NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(parserResult.getModel().getFileScope(), caretPosition);
        assert namespaceScope != null;
        ImportData importData = new ImportDataCreator(filteredExistingNames, index, namespaceScope.getNamespaceName(), createOptions(parserResult)).create();
        importData.caretPosition = caretPosition;
        return importData;
    }

    private static void performFixUses(
            final PHPParseResult parserResult,
            final ImportData importData,
            final List<ImportData.ItemVariant> selections,
            final boolean removeUnusedUses) {
        new FixUsesPerformer(parserResult, importData, selections, removeUnusedUses, createOptions(parserResult)).perform();
    }

    private static Options createOptions(final PHPParseResult parserResult) {
        Document document = parserResult.getSnapshot().getSource().getDocument(false);
        CodeStyle codeStyle = CodeStyle.get(document);
        return new Options(codeStyle, parserResult.getModel().getFileScope().getFileObject());
    }

    private static final RequestProcessor WORKER = new RequestProcessor(FixUsesAction.class.getName(), 1);

    @Messages({
        "LBL_Ok=Ok",
        "LBL_Cancel=Cancel"
    })
    private static void showFixUsesDialog(final JTextComponent target, final ImportData importData) {
    final FixDuplicateImportStmts panel = new FixDuplicateImportStmts();
        panel.initPanel(importData, isRemoveUnusedUses());
        final JButton ok = new JButton(Bundle.LBL_Ok());
        final JButton cancel = new JButton(Bundle.LBL_Cancel());
        final AtomicBoolean stop = new AtomicBoolean();
        DialogDescriptor dd = new DialogDescriptor(panel, Bundle.LongName(), true, new Object[] {ok, cancel}, ok, DialogDescriptor.DEFAULT_ALIGN,
                HelpCtx.DEFAULT_HELP, new ActionListener() {
                                          @Override
                                          public void actionPerformed(ActionEvent e) {
                                          }
                                      }, true);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok.setEnabled(false);
                final List<ItemVariant> selections = panel.getSelections();
                final boolean removeUnusedUses = panel.getRemoveUnusedImports();
                WORKER.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            ParserManager.parse(Collections.singleton(Source.create(target.getDocument())), new UserTask() {

                                @Override
                                public void run(ResultIterator resultIterator) throws Exception {
                                    Result parserResult = resultIterator.getParserResult();
                                    if (parserResult instanceof PHPParseResult) {
                                        SwingUtilities.invokeLater(new Runnable() {

                                            @Override
                                            public void run() {
                                                cancel.setEnabled(false);
                                                ((JDialog) d).setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                                            }
                                        });
                                        if (stop.get()) {
                                            return;
                                        }
                                        performFixUses((PHPParseResult) parserResult, importData, selections, removeUnusedUses);
                                    }
                                }
                            });
                        } catch (ParseException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        setRemoveUnusedUses(removeUnusedUses);
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                d.setVisible(false);
                            }
                        });
                    }
                });
            }
        });

        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                stop.set(true);
                d.setVisible(false);
            }
        });

        d.setVisible(true);

        d.dispose();
    }

    public static final class GlobalAction extends MainMenuAction {
        public GlobalAction() {
            super();
            setMenu();
        }

        @Override
        protected String getMenuItemText() {
            return Bundle.FixUsesLabel();
        }

        @Override
        protected String getActionName() {
            return ACTION_NAME;
        }
    }

    public static class Options {

        private final boolean preferFullyQualifiedNames;
        private final boolean preferMultipleUseStatementsCombined;
        private final boolean startUseWithNamespaceSeparator;
        private final boolean aliasesCapitalsOfNamespaces;
        private final boolean isPhp56OrGreater;

        public Options(
                boolean preferFullyQualifiedNames,
                boolean preferMultipleUseStatementsCombined,
                boolean startUseWithNamespaceSeparator,
                boolean aliasesCapitalsOfNamespaces,
                boolean isPhp56OrGreater) {
            this.preferFullyQualifiedNames = preferFullyQualifiedNames;
            this.preferMultipleUseStatementsCombined = preferMultipleUseStatementsCombined;
            this.startUseWithNamespaceSeparator = startUseWithNamespaceSeparator;
            this.aliasesCapitalsOfNamespaces = aliasesCapitalsOfNamespaces;
            this.isPhp56OrGreater = isPhp56OrGreater;
        }

        public Options(CodeStyle codeStyle, FileObject fileObject) {
            this.preferFullyQualifiedNames = codeStyle.preferFullyQualifiedNames();
            this.preferMultipleUseStatementsCombined = codeStyle.preferMultipleUseStatementsCombined();
            this.startUseWithNamespaceSeparator = codeStyle.startUseWithNamespaceSeparator();
            this.aliasesCapitalsOfNamespaces = codeStyle.aliasesFromCapitalsOfNamespaces();
            this.isPhp56OrGreater = CodeUtils.isPhp56OrGreater(fileObject);
        }

        public boolean preferFullyQualifiedNames() {
            return preferFullyQualifiedNames;
        }

        public boolean preferMultipleUseStatementsCombined() {
            return preferMultipleUseStatementsCombined;
        }

        public boolean startUseWithNamespaceSeparator() {
            return startUseWithNamespaceSeparator;
        }

        public boolean aliasesCapitalsOfNamespaces() {
            return aliasesCapitalsOfNamespaces;
        }

        public boolean isPhp56OrGreater() {
            return isPhp56OrGreater;
        }

    }
}
