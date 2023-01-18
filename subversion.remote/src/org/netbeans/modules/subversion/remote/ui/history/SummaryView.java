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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.subversion.remote.ui.history;

import java.awt.Point;
import java.awt.event.ActionEvent;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.openide.nodes.Node;
import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;
import java.util.List;
import java.text.DateFormat;
import java.util.ArrayList;
import org.netbeans.modules.remotefs.versioning.api.VCSFileProxySupport;
import org.netbeans.modules.subversion.remote.Subversion;
import org.netbeans.modules.subversion.remote.api.SVNClientException;
import org.netbeans.modules.subversion.remote.api.SVNRevision;
import org.netbeans.modules.subversion.remote.api.SVNUrl;
import org.netbeans.modules.subversion.remote.client.SvnClientExceptionHandler;
import org.netbeans.modules.subversion.remote.client.SvnProgressSupport;
import org.netbeans.modules.subversion.remote.options.AnnotationColorProvider;
import org.netbeans.modules.subversion.remote.ui.diff.DiffSetupSource;
import org.netbeans.modules.subversion.remote.ui.diff.Setup;
import org.netbeans.modules.subversion.remote.ui.update.RevertModifications;
import org.netbeans.modules.subversion.remote.ui.update.RevertModificationsAction;
import org.netbeans.modules.subversion.remote.util.Context;
import org.netbeans.modules.subversion.remote.util.SvnUtils;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.history.AbstractSummaryView;
import org.netbeans.modules.versioning.history.AbstractSummaryView.SummaryViewMaster.SearchHighlight;
import org.openide.util.WeakListeners;

/**
 * @author Maros Sandor
 */
/**
 * Shows Search History results in a JList.
 *
 * @author Maros Sandor
 */
class SummaryView extends AbstractSummaryView implements DiffSetupSource {

    private final SearchHistoryPanel master;

    private static final DateFormat defaultFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    
    static final class SvnLogEntry extends AbstractSummaryView.LogEntry implements PropertyChangeListener {

        private final RepositoryRevision revision;
        private List events = new ArrayList<>(10);
        private List<Event> dummyEvents;
        private final SearchHistoryPanel master;
        private final PropertyChangeListener list;
    
        public SvnLogEntry (RepositoryRevision revision, SearchHistoryPanel master) {
            this.revision = revision;
            this.master = master;
            this.dummyEvents = Collections.<Event>emptyList();
            if (revision.isEventsInitialized()) {
                refreshEvents();
                list = null;
            } else {
                prepareDummyEvents();
                revision.addPropertyChangeListener(RepositoryRevision.PROP_EVENTS_CHANGED, list = WeakListeners.propertyChange(this, revision));
            }
        }

        @Override
        public Collection<AbstractSummaryView.LogEntry.Event> getEvents () {
            return events;
        }

        @Override
        public Collection<Event> getDummyEvents () {
            return dummyEvents;
        }

        @Override
        public String getAuthor () {
            return revision.getLog().getAuthor();
        }

        @Override
        public String getDate () {
            Date date = revision.getLog().getDate();
            return date != null ? defaultFormat.format(date) : null;
        }

        @Override
        public String getRevision () {
            return revision.getLog().getRevision().toString();
        }

        @Override
        protected Collection<AbstractSummaryView.LogEntry.RevisionHighlight> getRevisionHighlights () {
            return Collections.<AbstractSummaryView.LogEntry.RevisionHighlight>emptyList();
        }

        @Override
        public String getMessage () {
            return revision.getLog().getMessage();
        }

        @Override
        public Action[] getActions () {
            List<Action> actions = new ArrayList<>();
            long revisionNumber = revision.getLog().getRevision().getNumber();
            
            if (revisionNumber > 1) {
                actions.add(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_DiffToPreviousShort")) { //NOI18N
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        diffPrevious(master, revision);
                    }
                });
            }
            actions.addAll(Arrays.asList(revision.getActions()));
            return actions.toArray(new Action[actions.size()]);
        }

        @Override
        public String toString () {
            return revision.toString();
        }

        @Override
        protected void expand () {
            revision.expandEvents();
        }

        @Override
        protected void cancelExpand () {
            revision.cancelExpand();
        }

        @Override
        protected boolean isEventsInitialized () {
            return revision.isEventsInitialized();
        }

        @Override
        public boolean isVisible () {
            return master.applyFilter(revision);
        }

        @Override
        protected boolean isLessInteresting () {
            return false;
        }

        RepositoryRevision getRepositoryRevision () {
            return revision;
        }

        void prepareDummyEvents () {
            ArrayList<Event> evts = new ArrayList<>(revision.getDummyEvents().size());
            for (RepositoryRevision.Event event : revision.getDummyEvents()) {
                evts.add(new SvnLogEvent(master, event));
            }
            dummyEvents = evts;
        }

        void refreshEvents () {
            ArrayList<SvnLogEvent> evts = new ArrayList<>(revision.getEvents().size());
            for (RepositoryRevision.Event event : revision.getEvents()) {
                evts.add(new SvnLogEvent(master, event));
            }
            List<SvnLogEvent> newEvents = new ArrayList<>(evts);
            events = evts;
            dummyEvents.clear();
            eventsChanged(null, newEvents);
        }

        @Override
        public void propertyChange (PropertyChangeEvent evt) {
            if (RepositoryRevision.PROP_EVENTS_CHANGED.equals(evt.getPropertyName()) && revision == evt.getSource()) {
                refreshEvents();
            }
        }
    }
    
    static class SvnLogEvent extends AbstractSummaryView.LogEntry.Event {

        private final RepositoryRevision.Event event;
        private final SearchHistoryPanel master;

        SvnLogEvent (SearchHistoryPanel master, RepositoryRevision.Event event) {
            this.master = master;
            this.event = event;
        }

        @Override
        public String getPath () {
            String path = event.getChangedPath().getPath();
            return path;
        }

        @Override
        public String getOriginalPath () {
            String path = event.getOriginalPath();
            return path;
        }

        @Override
        public String getAction () {
            return event.getAction();
        }
        
        public RepositoryRevision.Event getEvent() {
            return event;
        }

        @Override
        public Action[] getUserActions () {
            List<Action> actions = new ArrayList<>();
            long revisionNumber = event.getLogInfoHeader().getLog().getRevision().getNumber();
            if (revisionNumber > 1) {
                actions.add(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_DiffToPreviousShort")) { //NOI18N
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        diffPrevious(master, event);
                    }
                });
            }
            actions.addAll(Arrays.asList(event.getActions()));
            return actions.toArray(new Action[actions.size()]);
        }

        @Override
        public boolean isVisibleByDefault () {
            return master.isShowInfo() || event.isUnderRoots();
        }

        @Override
        public String toString () {
            return event.toString();
        }
    }

    public SummaryView (SearchHistoryPanel master, List<? extends LogEntry> results) {
        super(createViewSummaryMaster(master), results, null);
        this.master = master;
    }
    
    @Override
    public Collection<Setup> getSetups() {
        Node [] nodes = TopComponent.getRegistry().getActivatedNodes();
        if (nodes.length == 0) {
            List<RepositoryRevision> results = master.getResults();
            return master.getSetups(results.toArray(new RepositoryRevision[results.size()]), new RepositoryRevision.Event[0]);
        }
    
        Set<RepositoryRevision.Event> events = new HashSet<>();
        Set<RepositoryRevision> revisions = new HashSet<>();

        Object [] sel = getSelection();
        for (Object revCon : sel) {
            if (revCon instanceof RepositoryRevision) {
                revisions.add((RepositoryRevision) revCon);
            } else {
                events.add((RepositoryRevision.Event) revCon);
            }
        }
        return master.getSetups(revisions.toArray(new RepositoryRevision[revisions.size()]), events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    @Override
    public String getSetupDisplayName() {
        return null;
    }
    
    private static SummaryViewMaster createViewSummaryMaster (final SearchHistoryPanel master) {
        final Map<String, String> colors = new HashMap<>();
        colors.put("A", SvnUtils.getColorString(AnnotationColorProvider.getInstance().ADDED_LOCALLY_FILE.getActualColor())); //NOI18N
        colors.put("C", SvnUtils.getColorString(AnnotationColorProvider.getInstance().COPIED_LOCALLY_FILE.getActualColor())); //NOI18N
        colors.put("R", SvnUtils.getColorString(AnnotationColorProvider.getInstance().COPIED_LOCALLY_FILE.getActualColor())); //NOI18N
        colors.put("M", SvnUtils.getColorString(AnnotationColorProvider.getInstance().MODIFIED_LOCALLY_FILE.getActualColor())); //NOI18N
        colors.put("D", SvnUtils.getColorString(AnnotationColorProvider.getInstance().REMOVED_LOCALLY_FILE.getActualColor())); //NOI18N
        colors.put("?", SvnUtils.getColorString(AnnotationColorProvider.getInstance().EXCLUDED_FILE.getActualColor())); //NOI18N

        return new SummaryViewMaster() {

            @Override
            public JComponent getComponent () {
                return master;
            }

            @Override
            public File[] getRoots(){
                List<File> files = new ArrayList<>();
                for(VCSFileProxy proxy : master.getRoots()) {
                    File file = proxy.toFile();
                    if (file != null) {
                        files.add(file);
                    }
                }
                return files.toArray(new File[files.size()]);
            }

            @Override
            public Collection<SearchHighlight> getSearchHighlights () {
                return master.getSearchHighlights();
            }

            @Override
            public Map<String, String> getActionColors () {
                return colors;
            }

            @Override
            public void getMoreResults (PropertyChangeListener callback, int count) {
                master.getMoreRevisions(callback, count);
            }

            @Override
            public boolean hasMoreResults () {
                return master.hasMoreResults();
            }
        };
    }
    
    @Override
    protected void onPopup (JComponent invoker, Point p, final Object[] selection) {
        JPopupMenu menu = new JPopupMenu();

        String previousRevision = null;
        RepositoryRevision container;
        List<RepositoryRevision.Event> drevList;
        Object revCon = selection[0];

        boolean noExDeletedExistingFiles = true;
        boolean revisionSelected;
        boolean missingFile = false;
        boolean oneRevisionMultiselected = true;
        boolean deleted = false;

        if (revCon instanceof SvnLogEntry && selection.length == 1) {
            revisionSelected = true;
            container = ((SvnLogEntry) selection[0]).revision;
            drevList = new ArrayList<>(0);
            oneRevisionMultiselected = true;
            noExDeletedExistingFiles = true;
        } else {
            revisionSelected = false;
            drevList = new ArrayList<>(selection.length);
            for(int i = 0; i < selection.length; i++) {
                if (!(selection[i] instanceof SvnLogEvent)) {
                    return;
                }

                RepositoryRevision.Event event = ((SvnLogEvent) selection[i]).getEvent();
                drevList.add(event);
                VCSFileProxy file = event.getFile();

                if(!deleted && file != null && !file.exists() && event.getChangedPath().getAction() == 'D') {
                    deleted = true;
                }
                if(!missingFile && event.getFile() == null) {
                    missingFile = true;
                }
                if(oneRevisionMultiselected && i > 0 &&
                   drevList.get(0).getLogInfoHeader().getLog().getRevision().getNumber() != drevList.get(0).getLogInfoHeader().getLog().getRevision().getNumber())
                {
                    oneRevisionMultiselected = false;
                }
                if(file != null && event.getChangedPath().getAction() == 'D' && file.exists()) {
                    noExDeletedExistingFiles = false;
                }
            }
            container = drevList.get(0).getLogInfoHeader();
        }
        final RepositoryRevision.Event[] drev = drevList.toArray(new RepositoryRevision.Event[drevList.size()]);
        long revision = container.getLog().getRevision().getNumber();

        final boolean rollbackToEnabled = !deleted && !missingFile && !revisionSelected && oneRevisionMultiselected;
        final boolean rollbackChangeEnabled = !missingFile && oneRevisionMultiselected && (drev.length == 0 || noExDeletedExistingFiles); // drev.length == 0 => the whole revision was selected
        final boolean viewEnabled = selection.length == 1 && !revisionSelected && drev[0].getFile() != null && !drev[0].getFile().isDirectory() && drev[0].getChangedPath().getAction() != 'D';
        final boolean diffToPrevEnabled = selection.length == 1;

        if (revision > 1) {
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_DiffToPrevious", previousRevision)) { // NOI18N
                {
                    setEnabled(diffToPrevEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    diffPrevious(master, selection[0]);
                }
            }));
        }

        menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_RollbackChange")) { // NOI18N
            {
                setEnabled(rollbackChangeEnabled);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                revertModifications(master, selection);
            }
        }));

        if (!revisionSelected) {
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_RollbackTo", revision)) { // NOI18N
                {
                    setEnabled(rollbackToEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    Subversion.getInstance().getParallelRequestProcessor().post(new Runnable() {
                        @Override
                        public void run() {
                            rollback(drev);
                        }
                    });
                }
            }));
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_View")) { // NOI18N
                {
                    setEnabled(viewEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    Subversion.getInstance().getParallelRequestProcessor().post(new Runnable() {
                        @Override
                        public void run() {
                            view(selection[0], false);
                        }
                    });
                }
            }));
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_ShowAnnotations")) { // NOI18N
                {
                    setEnabled(viewEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    Subversion.getInstance().getParallelRequestProcessor().post(new Runnable() {
                        @Override
                        public void run() {
                            view(selection[0], true);
                        }
                    });
                }
            }));
            menu.add(new JMenuItem(new AbstractAction(Bundle.CTL_Action_ViewCurrent_name()) {
                {
                    setEnabled(viewEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    Subversion.getInstance().getParallelRequestProcessor().post(new Runnable() {
                        @Override
                        public void run() {
                            VCSFileProxySupport.openFile(drev[0].getFile().normalizeFile());
                        }
                    });
                }
            }));
        }

        menu.show(invoker, p.x, p.y);
    }

    /**
     * Overwrites local file with this revision.
     *
     * @param event
     */
    private void rollback(final RepositoryRevision.Event[] events) {
        // TODO: confirmation
        SVNUrl repository = events[0].getLogInfoHeader().getRepositoryRootUrl();
        RequestProcessor rp = Subversion.getInstance().getRequestProcessor(repository);
        SvnProgressSupport support = new SvnProgressSupport(master.getFileSystem()) {
            @Override
            public void perform() {
                for(RepositoryRevision.Event event : events) {
                    VCSFileProxy file = event.getFile();
                    boolean wasDeleted = event.getChangedPath().getAction() == 'D';
                    SVNUrl repoUrl = event.getLogInfoHeader().getRepositoryRootUrl();
                    SVNUrl fileUrl = repoUrl.appendPath(event.getChangedPath().getPath());                    
                    SVNRevision.Number revision = event.getLogInfoHeader().getLog().getRevision();
                    SvnUtils.rollback(file, repoUrl, fileUrl, revision, wasDeleted, getLogger());
                }
            }
        };
        support.start(rp, repository, NbBundle.getMessage(SummaryView.class, "MSG_Rollback_Progress")); // NOI18N
    }

    private static void revertModifications (SearchHistoryPanel master, Object[] selection) {
        Set<RepositoryRevision.Event> events = new HashSet<>();
        Set<RepositoryRevision> revisions = new HashSet<>();
        for (Object o : selection) {
            if (o instanceof RepositoryRevision) {
                revisions.add((RepositoryRevision) o);
            } else if (o instanceof SvnLogEntry) {
                revisions.add(((SvnLogEntry) o).revision);
            } else if (o instanceof SvnLogEvent) {
                events.add(((SvnLogEvent) o).event);
            } else {
                events.add((RepositoryRevision.Event) o);
            }
        }
        revert(master, revisions.toArray(new RepositoryRevision[revisions.size()]), (RepositoryRevision.Event[]) events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    private static void revert(final SearchHistoryPanel master, final RepositoryRevision [] revisions, final RepositoryRevision.Event [] events) {
        SVNUrl url;
        try {
            url = master.getSearchRepositoryRootUrl();
        } catch (SVNClientException ex) {
            SvnClientExceptionHandler.notifyException(new Context(master.getRoots()), ex, true, true);
            return;
        }
        RequestProcessor rp = Subversion.getInstance().getRequestProcessor(url);
        SvnProgressSupport support = new SvnProgressSupport(master.getFileSystem()) {
            @Override
            public void perform() {
                revertImpl(master, revisions, events, this);
            }
        };
        support.start(rp, url, NbBundle.getMessage(SummaryView.class, "MSG_Revert_Progress")); // NOI18N
    }

    private static void revertImpl(SearchHistoryPanel master, RepositoryRevision[] revisions, RepositoryRevision.Event[] events, SvnProgressSupport progress) {
        for (RepositoryRevision revision : revisions) {
            RevertModifications.RevisionInterval revisionInterval = new RevertModifications.RevisionInterval(revision.getLog().getRevision());
            final Context ctx = new Context(master.getRoots());
            RevertModificationsAction.performRevert(revisionInterval, false, false, ctx, progress);
        }
        for (RepositoryRevision.Event event : events) {
            if (event.getFile() == null) continue;
            RevertModifications.RevisionInterval revisionInterval = new RevertModifications.RevisionInterval(event.getLogInfoHeader().getLog().getRevision());
            final Context ctx = new Context(event.getFile());
            RevertModificationsAction.performRevert(revisionInterval, false, false, ctx, progress);
        }
    }

    private static void view (Object o, boolean showAnnotations) {
        RepositoryRevision.Event drev = null;
        if (o instanceof RepositoryRevision.Event) {
            drev = (RepositoryRevision.Event) o;
        } else if (o instanceof SvnLogEvent) {
            drev = ((SvnLogEvent) o).event;
        }
        if (drev != null) {
            drev.viewFile(showAnnotations);
        }
    }

    private static void diffPrevious (SearchHistoryPanel master, Object o) {
        if (o instanceof RepositoryRevision.Event) {
            RepositoryRevision.Event drev = (RepositoryRevision.Event) o;
            master.showDiff(drev);
        } else if (o instanceof SvnLogEvent) {
            RepositoryRevision.Event drev = ((SvnLogEvent) o).event;
            master.showDiff(drev);
        } else if (o instanceof SvnLogEntry) {
            RepositoryRevision container = ((SvnLogEntry) o).revision;
            master.showDiff(container);
        } else {
            RepositoryRevision container = (RepositoryRevision) o;
            master.showDiff(container);
        }
    }
}
