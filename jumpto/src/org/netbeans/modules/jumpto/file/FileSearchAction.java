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
 * The Original Software is the Accelerators module.
 * The Initial Developer of the Original Software is Andrei Badea.
 * Portions Copyright 2005-2006 Andrei Badea.
 * All Rights Reserved.
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
 * Contributor(s): Andrei Badea
 *                 Petr Hrebejk
 *                 markiewb@netbeans.org
 */

package org.netbeans.modules.jumpto.file;

import org.netbeans.modules.jumpto.common.CurrentSearch;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.editor.JumpList;
import org.netbeans.modules.jumpto.common.AbstractModelFilter;
import org.netbeans.modules.jumpto.common.Factory;
import org.netbeans.modules.jumpto.common.ItemRenderer;
import org.netbeans.modules.jumpto.common.Models;
import org.netbeans.modules.jumpto.common.Utils;
import org.netbeans.modules.parsing.lucene.support.Queries;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.netbeans.spi.jumpto.file.FileDescriptor;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.text.NbDocument;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Pair;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
/**
 *
 * @author Andrei Badea, Petr Hrebejk, Tomas Zezula
 */
public class FileSearchAction extends AbstractAction implements FileSearchPanel.ContentProvider {

    /* package */ static final Logger LOGGER = Logger.getLogger(FileSearchAction.class.getName());
    /* package */ static final String CAMEL_CASE_SEPARATOR = "\\p{javaUpperCase}|-|_|\\.";    //NOI18N
    /* package */ static final String CAMEL_CASE_PART_CASE_SENSITIVE = "\\p{javaLowerCase}|\\p{Digit}|\\$";         //NOI18N
    /* package */ static final String CAMEL_CASE_PART_CASE_INSENSITIVE = "\\p{javaLowerCase}|\\p{Digit}|\\p{javaUpperCase}|\\$";         //NOI18N
    /* package */ static final Map<String,Object> SEARCH_OPTIONS_CASE_SENSITIVE;
    /* package */ static final Map<String,Object> SEARCH_OPTIONS_CASE_INSENSITIVE;
    static {
        Map<String,Object> m = new HashMap<>();
        m.put(Queries.OPTION_CAMEL_CASE_SEPARATOR, CAMEL_CASE_SEPARATOR);
        m.put(Queries.OPTION_CAMEL_CASE_PART, CAMEL_CASE_PART_CASE_SENSITIVE);
        SEARCH_OPTIONS_CASE_SENSITIVE = Collections.unmodifiableMap(m);
        m = new HashMap<>();
        m.put(Queries.OPTION_CAMEL_CASE_SEPARATOR, CAMEL_CASE_SEPARATOR);
        m.put(Queries.OPTION_CAMEL_CASE_PART, CAMEL_CASE_PART_CASE_INSENSITIVE);
        SEARCH_OPTIONS_CASE_INSENSITIVE = Collections.unmodifiableMap(m);
    }
    private static final char LINE_NUMBER_SEPARATOR = ':';    //NOI18N
    private static final Pattern PATTERN_WITH_LINE_NUMBER = Pattern.compile("(.*)"+LINE_NUMBER_SEPARATOR+"(\\d*)");    //NOI18N

    private static final ListModel EMPTY_LIST_MODEL = new DefaultListModel();
    //Threading: Throughput 1 required due to inherent sequential code in Work.Request.exclude
    private static final RequestProcessor rp = new RequestProcessor ("FileSearchAction-RequestProcessor",1);
    private final FilterFactory filterFactory = new FilterFactory();
    private final CurrentSearch<FileDescriptor> currentSearch = new CurrentSearch(filterFactory);
    //@GuardedBy("this")
    private FileComarator itemsComparator;
    //@GuardedBy("this")
    private Worker[] running;
    //@GuardedBy("this")
    private RequestProcessor.Task[] scheduledTasks;
    private Dialog dialog;
    private JButton openBtn;
    private FileSearchPanel panel;
    private Dimension initialDimension;

    public FileSearchAction() {
        super( NbBundle.getMessage(FileSearchAction.class, "CTL_FileSearchAction") );
        // XXX this should be in initialize()?
        putValue("PopupMenuText", NbBundle.getBundle(FileSearchAction.class).getString("editor-popup-CTL_FileSearchAction")); // NOI18N
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }

    @Override
    public boolean isEnabled() {
        return OpenProjects.getDefault().getOpenProjects().length > 0;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        FileDescriptor[] typeDescriptors = getSelectedFiles();
        if (typeDescriptors != null) {
            JumpList.checkAddEntry();
            for(FileDescriptor td: typeDescriptors){
                td.open();
            }
        }
    }

    // Implementation of content provider --------------------------------------


    @Override
    @NonNull
    public ListCellRenderer getListCellRenderer(
            @NonNull final JList list,
            @NonNull final Document nameDocument,
            @NonNull final ButtonModel caseSensitive,
            @NonNull final ButtonModel colorPrefered) {
        Parameters.notNull("list", list);   //NOI18N
        Parameters.notNull("nameDocument", nameDocument);   //NOI18N
        Parameters.notNull("caseSensitive", caseSensitive); //NOI18N
        return ItemRenderer.Builder.create(
                    list,
                    caseSensitive,
                    new FileDescriptorConvertor(nameDocument)).
                setCamelCaseSeparator(CAMEL_CASE_SEPARATOR).
                setColorPreferedProject(colorPrefered).
                build();
    }


    @Override
    public boolean setListModel(final FileSearchPanel panel, String text ) {
        enableOK(false);

        cancel();

        if ( text == null ) {
            panel.setModel(EMPTY_LIST_MODEL, true);
            currentSearch.resetFilter();
            return false;
        }
        boolean exact = text.endsWith(" "); // NOI18N
        text = text.trim();
        if ( text.length() == 0 || !Utils.isValidInput(text)) {
            panel.setModel(EMPTY_LIST_MODEL, true);
            currentSearch.resetFilter();
            return false;
        }

        //Extract linenumber from search text
        //Pattern is like 'My*Object.java:123'
        final Pair<String,Integer> nameLinePair = splitNameLine(text);
        text = nameLinePair.first();
        final int lineNr = nameLinePair.second();
        final QuerySupport.Kind nameKind = Utils.toQueryKind(Utils.getSearchType(
                text,
                exact,
                panel.isCaseSensitive(),
                CAMEL_CASE_SEPARATOR,
                CAMEL_CASE_PART_CASE_INSENSITIVE));
        if (nameKind == QuerySupport.Kind.REGEXP || nameKind == QuerySupport.Kind.CASE_INSENSITIVE_REGEXP) {
            text = Utils.removeNonNeededWildCards(text);
        }

        // Compute in other thread
        synchronized(this) {
            final SearchType searchType = Utils.toSearchType(nameKind);
            if (currentSearch.isNarrowing(searchType, text, null, true)) {
                itemsComparator.setUsePreferred(panel.isPreferedProject());
                filterFactory.setLineNumber(lineNr);
                currentSearch.filter(
                        searchType,
                        text,
                        panel.isCaseSensitive() ? SEARCH_OPTIONS_CASE_SENSITIVE : SEARCH_OPTIONS_CASE_INSENSITIVE);
                enableOK(panel.searchCompleted(true));
                return false;
            } else {
                final String searchText = text;
                itemsComparator = new FileComarator(
                        panel.isPreferedProject(),
                        panel.isCaseSensitive());
                final Models.MutableListModel baseListModel = Models.mutable(
                        itemsComparator,
                        currentSearch.resetFilter(),
                        null);
                panel.setModel(Models.refreshable(
                        baseListModel,
                        new Factory<FileDescriptor, Pair<FileDescriptor,Runnable>>() {
                            @Override
                            public FileDescriptor create(@NonNull final Pair<FileDescriptor,Runnable> param) {
                                return new AsyncFileDescriptor(param.first(), param.second());
                            }
                        }),
                        false);
                final Worker.Request request = Worker.newRequest(
                    searchText,
                    nameKind,
                    panel.getCurrentProject(),
                    lineNr);
                final Worker.Collector collector = Worker.newCollector(
                    baseListModel,
                    new Runnable () {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    panel.searchProgress();
                                    enableOK(baseListModel.getSize() > 0);
                                }
                            });
                        }
                    },
                    new Runnable () {
                        @Override
                        public void run() {
                            currentSearch.searchCompleted(searchType, searchText, null);
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    panel.searchCompleted(true);
                                }
                            });
                        }
                    },
                    panel.time);
                final Worker.Type[] wts = Worker.Type.values();
                final Worker[] workers = new Worker[wts.length];
                //Threading: All workers need to be created before they are scheduled
                for (int i = 0; i < wts.length; i++) {
                    workers[i] = Worker.newWorker(request, collector, wts[i]);
                }
                running = workers;
                final RequestProcessor.Task[] tasks = new RequestProcessor.Task[workers.length];
                for (int i = 0; i < workers.length; i++) {
                    tasks[i] = rp.post(workers[i], 220);
                }
                scheduledTasks = tasks;
                if ( panel.time != -1 ) {
                    LOGGER.log(
                        Level.FINE,
                        "Worker posted after {0} ms.",  //NOI18N
                        System.currentTimeMillis() - panel.time );
                }
                return true;
            }
        }
    }

    @Override
    public void closeDialog() {
        dialog.setVisible( false );
        cleanup();
    }

    @Override
    public boolean hasValidContent () {
        return this.openBtn != null && this.openBtn.isEnabled();
    }

    // Private methods ---------------------------------------------------------

    private void enableOK(final boolean enable) {
        if (openBtn != null) {
            openBtn.setEnabled (enable);
        }
    }

    private FileDescriptor[] getSelectedFiles() {
        FileDescriptor[] result = null;
        panel = new FileSearchPanel(this, findCurrentProject());
        dialog = createDialog(panel);

        Node[] arr = TopComponent.getRegistry ().getActivatedNodes();
        if (arr.length > 0) {
            EditorCookie ec = arr[0].getCookie (EditorCookie.class);
            if (ec != null) {
                JEditorPane recentPane = NbDocument.findRecentEditorPane(ec);
                if (recentPane != null) {
                    String initSearchText = null;
                    if (org.netbeans.editor.Utilities.isSelectionShowing(recentPane.getCaret())) {
                        initSearchText = recentPane.getSelectedText();
                    }
                    if (initSearchText != null) {
                        if (initSearchText.length() > 256) {
                            initSearchText = initSearchText.substring(0, 256);
                        }
                        panel.setInitialText(initSearchText);
                    } else {
                        FileObject fo = arr[0].getLookup().lookup(FileObject.class);
                        if (fo != null) {
                            panel.setInitialText(fo.getNameExt());
                        }
                    }
                }
            }
        }

        dialog.setVisible(true);
        result = panel.getSelectedFiles();
        return result;
    }

   private Dialog createDialog( final FileSearchPanel panel) {
        openBtn = new JButton();
        Mnemonics.setLocalizedText(openBtn, NbBundle.getMessage(FileSearchAction.class, "CTL_Open"));
        openBtn.getAccessibleContext().setAccessibleDescription(openBtn.getText());
        openBtn.setEnabled( false );

        final Object[] buttons = new Object[] { openBtn, DialogDescriptor.CANCEL_OPTION };

        String title = NbBundle.getMessage(FileSearchAction.class, "MSG_FileSearchDlgTitle");
        DialogDescriptor dialogDescriptor = new DialogDescriptor(
                panel,
                title,
                true,
                buttons,
                openBtn,
                DialogDescriptor.DEFAULT_ALIGN,
                HelpCtx.DEFAULT_HELP,
                new DialogButtonListener(panel));
        dialogDescriptor.setClosingOptions(buttons);

        Dialog d = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        d.getAccessibleContext().setAccessibleName(NbBundle.getMessage(FileSearchAction.class, "AN_FileSearchDialog"));
        d.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(FileSearchAction.class, "AD_FileSearchDialog"));

        // Set size
        d.setPreferredSize( new Dimension(  FileSearchOptions.getWidth(),
                                                 FileSearchOptions.getHeight() ) );

        // Center the dialog after the size changed.
        Rectangle r = Utilities.getUsableScreenBounds();
        int maxW = (r.width * 9) / 10;
        int maxH = (r.height * 9) / 10;
        Dimension dim = d.getPreferredSize();
        dim.width = Math.min(dim.width, maxW);
        dim.height = Math.min(dim.height, maxH);
        initialDimension = dim;
        d.setBounds(Utilities.findCenterBounds(dim));
        d.addWindowListener(new WindowAdapter() {
            public @Override void windowClosed(WindowEvent e) {
                cleanup();
            }
        });

        return d;
    }

    /** For original of this code look at:
     *  org.netbeans.modules.project.ui.actions.ActionsUtil
     */
    private static Project findCurrentProject( ) {
        Lookup lookup = Utilities.actionsGlobalContext();

        // Maybe the project is in the lookup
        for (Project p : lookup.lookupAll(Project.class)) {
            return p;
        }
        // Now try to guess the project from dataobjects
        for (DataObject dObj : lookup.lookupAll(DataObject.class)) {
            FileObject fObj = dObj.getPrimaryFile();
            Project p = FileOwnerQuery.getOwner(fObj);
            if ( p != null ) {
                return p;
            }
        }

       return OpenProjects.getDefault().getMainProject();
    }

    private void cleanup() {
        cancel();
        if ( dialog != null ) { // Closing event for some reson sent twice
            // Save dialog size only when changed
            final int currentWidth = dialog.getWidth();
            final int currentHeight = dialog.getHeight();
            if (initialDimension != null && (initialDimension.width != currentWidth || initialDimension.height != currentHeight)) {
                FileSearchOptions.setHeight(currentHeight);
                FileSearchOptions.setWidth(currentWidth);
            }
            initialDimension = null;
            // Clean caches
            dialog.dispose();
            this.dialog = null;
            FileSearchOptions.flush();
        }
    }

    private void cancel() {
        synchronized (this) {
            if ( running != null ) {
                for (Worker w : running) {
                    w.cancel();
                }
                for (RequestProcessor.Task t : scheduledTasks) {
                    t.cancel();
                }
                running = null;
                scheduledTasks = null;
                panel.searchCompleted(false);
            }
        }
    }

    @NonNull
    private static Pair<String,Integer> splitNameLine(@NonNull String text) {
        final Matcher matcher = PATTERN_WITH_LINE_NUMBER.matcher(text);
        int lineNr = -1;
        if (matcher.matches()) {
            text = matcher.group(1);
            try {
                lineNr = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
               //pass
            }
        }
        return Pair.of(text,lineNr);
    }

    // Private classes ---------------------------------------------------------
    private class DialogButtonListener implements ActionListener {

        private FileSearchPanel panel;

        public DialogButtonListener(FileSearchPanel panel) {
            this.panel = panel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if ( e.getSource() == openBtn) {
                panel.setSelectedFile();
            }
        }
    }

    private static final class AsyncFileDescriptor extends FileDescriptor implements Runnable {

        @StaticResource
        private static final String UNKNOWN_ICON_PATH = "org/netbeans/modules/jumpto/resources/unknown.gif";    //NOI18N
        private static final Icon UNKNOWN_ICON = ImageUtilities.loadImageIcon(UNKNOWN_ICON_PATH, false);
        private static final RequestProcessor LOAD_ICON_RP = new RequestProcessor(AsyncFileDescriptor.class.getName(), 1, false, false);

        private final FileDescriptor delegate;
        private final Runnable refreshCallback;
        private volatile Icon icon;

        AsyncFileDescriptor(
            @NonNull final FileDescriptor delegate,
            @NonNull final Runnable refreshCallback) {
            Parameters.notNull("delegate", delegate);   //NOI18N
            Parameters.notNull("refreshCallback", refreshCallback); //NOI18N
            this.delegate = delegate;
            this.refreshCallback = refreshCallback;
            FileProviderAccessor.getInstance().setFromCurrentProject(
                this,
                FileProviderAccessor.getInstance().isFromCurrentProject(delegate));
        }

        @Override
        public String getFileName() {
           return delegate.getFileName();
        }

        @Override
        public String getOwnerPath() {
            return delegate.getOwnerPath();
        }

        @Override
        public Icon getIcon() {
            if (icon != null) {
                return icon;
            }
            LOAD_ICON_RP.execute(this);
            return UNKNOWN_ICON;
        }

        @Override
        public String getProjectName() {
            return delegate.getProjectName();
        }

        @Override
        public Icon getProjectIcon() {
            return delegate.getProjectIcon();
        }

        @Override
        public void open() {
            delegate.open();
        }

        @Override
        public FileObject getFileObject() {
            final FileObject res = delegate.getFileObject();
            if (res == null) {
                LOGGER.log(
                    Level.FINE,
                    "FileDescriptor: {0} : {1} returned null from getFile", //NOI18N
                    new Object[]{
                        delegate,
                        delegate.getClass()
                    });
            }
            return res;
        }

        @Override
        public String getFileDisplayPath() {
            return delegate.getFileDisplayPath();
        }

        @Override
        public void run() {
            icon = delegate.getIcon();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    refreshCallback.run();
                }
            });
        }

    }

    private static final class FileDescriptorConvertor implements ItemRenderer.Convertor<FileDescriptor>, DocumentListener {
        private String textToFind = "";   //NOI18N

        FileDescriptorConvertor(@NonNull final Document doc) {
            doc.addDocumentListener(this);
        }

        @Override
        public String getName(@NonNull final FileDescriptor item) {
            return item.getFileName();
        }

        @Override
        public String getHighlightText(@NonNull final FileDescriptor item) {
            return textToFind;
        }

        @Override
        public String getOwnerName(@NonNull final FileDescriptor item) {
            return item.getOwnerPath().length() > 0 ? " (" + item.getOwnerPath() + ")" : "";    //NOI18N
        }

        @Override
        public String getProjectName(@NonNull final FileDescriptor item) {
            return item.getProjectName();
        }

        @Override
        public String getFilePath(@NonNull final FileDescriptor item) {
            return item.getFileDisplayPath();
        }

        @Override
        public Icon getItemIcon(@NonNull final FileDescriptor item) {
            return item.getIcon();
        }

        @Override
        public Icon getProjectIcon(@NonNull final FileDescriptor item) {
            return item.getProjectIcon();
        }

        @Override
        public boolean isFromCurrentProject(@NonNull final FileDescriptor item) {
            return FileProviderAccessor.getInstance().isFromCurrentProject(item);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            try {
                textToFind = splitNameLine(e.getDocument().getText(0, e.getDocument().getLength())).first();
            } catch (BadLocationException ex) {
                textToFind = "";    //NOI18N
            }
        }

    }

    //@NotThreadSafe
    private static final class FilterFactory implements Callable<Models.Filter<FileDescriptor>> {
        private int currentLineNo;

        void setLineNumber(final int lineNo) {
            this.currentLineNo = lineNo;
        }

        @Override
        public Models.Filter<FileDescriptor> call() throws Exception {
            return new AbstractModelFilter<FileDescriptor>() {
                @NonNull
                @Override
                protected String getItemValue(@NonNull final FileDescriptor item) {
                    return item.getFileName();
                }
                @Override
                protected void update(@NonNull final FileDescriptor item) {
                    FileProviderAccessor.getInstance().setLineNumber(item, currentLineNo);
                }
            };
        }
    }
}
