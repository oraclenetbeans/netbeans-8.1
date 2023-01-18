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

package org.netbeans.modules.editor.impl.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.BaseKit;
import org.netbeans.api.editor.NavigationHistory;
import org.openide.awt.DropDownButtonFactory;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Vita Stejskal
 */
public final class NavigationHistoryBackAction extends TextAction implements ContextAwareAction, Presenter.Toolbar, PropertyChangeListener {
    
    private static final Logger LOG = Logger.getLogger(NavigationHistoryBackAction.class.getName());
    
    private final Reference<JTextComponent> componentRef;
    private final NavigationHistory.Waypoint waypoint;
    private final JPopupMenu popupMenu;
    private boolean updatePopupMenu = false;
   
    public NavigationHistoryBackAction() {
        this(null, null, null);
    }

    private NavigationHistoryBackAction(JTextComponent component, NavigationHistory.Waypoint waypoint, String actionName) {
        super(BaseKit.jumpListPrevAction);
        
        this.componentRef = new WeakReference<>(component);
        this.waypoint = waypoint;

        putValue("menuText", NbBundle.getMessage(NavigationHistoryBackAction.class,
                "NavigationHistoryBackAction_Tooltip_simple")); //NOI18N

        if (waypoint != null) {
            putValue(NAME, actionName);
            putValue(SHORT_DESCRIPTION, NbBundle.getMessage(NavigationHistoryBackAction.class, 
                "NavigationHistoryBackAction_Tooltip", actionName)); //NOI18N
            this.popupMenu = null;
        } else if (component != null) {
            putValue(SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/editor/resources/navigate_back_16.png", false)); //NOI18N
            this.popupMenu = new JPopupMenu() {

                @Override
                public int getComponentCount() {
                    if (updatePopupMenu) {
                        updatePopupMenu = false;    
                        List<NavigationHistory.Waypoint> waypoints = NavigationHistory.getNavigations().getPreviousWaypoints();
                        removeAll();

                        int count = 0;
                        String lastFileName = null;
                        NavigationHistory.Waypoint lastWpt = null;

                        for (int i = waypoints.size() - 1; i >= 0; i--) {
                            NavigationHistory.Waypoint wpt = waypoints.get(i);
                            String fileName = getWaypointName(wpt);

                            if (fileName == null) {
                                continue;
                            }

                            if (lastFileName == null || !fileName.equals(lastFileName)) {
                                JTextComponent c = componentRef.get();
                                if (lastFileName != null && c != null) {
                                    popupMenu.add(new NavigationHistoryBackAction(c, lastWpt,
                                            count > 1 ? lastFileName + ":" + count : lastFileName)); //NOI18N
                                }
                                lastFileName = fileName;
                                lastWpt = wpt;
                                count = 1;
                            } else {
                                count++;
                            }
                        }

                        JTextComponent c = componentRef.get();
                        if (lastFileName != null && c != null) {
                            add(new NavigationHistoryBackAction(c, lastWpt,
                                    count > 1 ? lastFileName + ":" + count : lastFileName)); //NOI18N
                        }
                    }
                    return super.getComponentCount(); //To change body of generated methods, choose Tools | Templates.
                }  
            };
            update();
            NavigationHistory nav = NavigationHistory.getNavigations();
            nav.addPropertyChangeListener(WeakListeners.propertyChange(this, nav));
        } else {
            this.popupMenu = null;
            putValue(SHORT_DESCRIPTION, NbBundle.getMessage(NavigationHistoryBackAction.class, 
                "NavigationHistoryBackAction_Tooltip_simple")); //NOI18N
        }
    }
    
    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        JTextComponent c = findComponent(actionContext);
        return new NavigationHistoryBackAction(c, null, null);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        NavigationHistory history = NavigationHistory.getNavigations();
        if (null == history.getCurrentWaypoint()) {
            // Haven't navigated back yet
            JTextComponent c = componentRef.get();
            JTextComponent target = c != null ? c : getTextComponent(evt);
            if (target != null) {
                try {
                    history.markWaypoint(target, target.getCaret().getDot(), true, false);
                } catch (BadLocationException ble) {
                    LOG.log(Level.WARNING, "Can't mark current position", ble); //NOI18N
                }
            }
        }
        
        NavigationHistory.Waypoint wpt = waypoint != null ? 
            history.navigateTo(waypoint) : history.navigateBack();
        
        if (wpt != null) {
            show(wpt);
        }
    }

    @Override
    public Component getToolbarPresenter() {
        if (popupMenu != null) {
            JButton button = DropDownButtonFactory.createDropDownButton(
                (ImageIcon) getValue(SMALL_ICON), 
                popupMenu
            );
            button.putClientProperty("hideActionText", Boolean.TRUE); //NOI18N
            button.setAction(this);
            return button;
        } else {
            return new JButton(this);
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        update();
    }
    
    private void update() {
        List<NavigationHistory.Waypoint> waypoints = NavigationHistory.getNavigations().getPreviousWaypoints();

        // Update popup menu
        if (popupMenu != null) {
            updatePopupMenu = true;
        }
        
        // Set the short description
        if (!waypoints.isEmpty()) {
            NavigationHistory.Waypoint wpt = waypoints.get(waypoints.size() - 1);
            String fileName = getWaypointName(wpt);
            if (fileName != null) {
                putValue(SHORT_DESCRIPTION, NbBundle.getMessage(NavigationHistoryBackAction.class, 
                    "NavigationHistoryBackAction_Tooltip", fileName)); //NOI18N
            } else {
                putValue(SHORT_DESCRIPTION, NbBundle.getMessage(NavigationHistoryBackAction.class, 
                    "NavigationHistoryBackAction_Tooltip_simple")); //NOI18N
            }
            
            setEnabled(true);
        } else {
            putValue(SHORT_DESCRIPTION, NbBundle.getMessage(NavigationHistoryBackAction.class, 
                "NavigationHistoryBackAction_Tooltip_simple")); //NOI18N
            setEnabled(false);
        }
    }

    /* package */ static void show(NavigationHistory.Waypoint wpt) {
        final int offset = wpt.getOffset();
        if (offset < 0) {
            return;
        }
        
        Lookup lookup = findLookupFor(wpt);
        if (lookup != null) {
            final EditorCookie editorCookie = lookup.lookup(EditorCookie.class);
            final LineCookie lineCookie = lookup.lookup(LineCookie.class);
            Document doc = null;

            if (editorCookie != null && lineCookie != null) {
                try {
                    doc = editorCookie.openDocument();
                } catch (IOException ioe) {
                    LOG.log(Level.WARNING, "Can't open document", ioe); //NOI18N
                }
            }

            if (doc instanceof BaseDocument) {
                final BaseDocument baseDoc = (BaseDocument) doc;
                final Line[] line = new Line[1];
                final int column[] = new int[1];
                baseDoc.render(new Runnable() {
                    @Override
                    public void run() {
                        Element lineRoot = baseDoc.getParagraphElement(0).getParentElement();
                        int lineIndex = lineRoot.getElementIndex(offset);

                        if (lineIndex != -1) {
                            Element lineElement = lineRoot.getElement(lineIndex);
                            column[0] = offset - lineElement.getStartOffset();
                            line[0] = lineCookie.getLineSet().getCurrent(lineIndex);
                        }
                    }
                });
                // Line.show() must NOT be called under doc.writeLock().
                // By possible thread's waiting in CloneableEditor.getEditorPane()
                // an asynchronous editor pane opening would be blocked
                // by the write-lock.
                // In case the current unlocked Line.show() solution would be found
                // unsatisfactory then issue #232175 should be reopened.
                if (line[0] != null) {
                    line[0].show(ShowOpenType.REUSE, ShowVisibilityType.FOCUS, column[0]);
                    return;
                }
            }
        }
        
        // lookup didn't work try simple navigation in the text component
        JTextComponent component = wpt.getComponent();
        if (component != null && component.getCaret() != null) {
            component.setCaretPosition(offset);
            component.requestFocusInWindow();
        }
    }
    
    private static Lookup findLookupFor(NavigationHistory.Waypoint wpt) {
        // try component first
        JTextComponent component = wpt.getComponent();
        if (component != null) {
            for (java.awt.Component c = component; c != null; c = c.getParent()) {
                if (c instanceof Lookup.Provider) {
                    Lookup lookup = ((Lookup.Provider)c).getLookup ();
                    if (lookup != null) {
                        return lookup;
                    }
                }
            }
        }

        // now try DataObject
        URL url = wpt.getUrl();
        FileObject f = url == null ? null : URLMapper.findFileObject(url);
        
        if (f != null) {
            try {
                return DataObject.find(f).getLookup();
            } catch (DataObjectNotFoundException e) {
                LOG.log(Level.WARNING, "Can't get DataObject for " + f, e); //NOI18N
            }
        }
        
        return null;
    }
    
    /* package */ static String getWaypointName(NavigationHistory.Waypoint wpt) {
        URL url = wpt.getUrl();
        if (url != null) {
            String path = url.getPath();
            int idx = path.lastIndexOf('/'); //NOI18N
            if (idx != -1) {
                return path.substring(idx + 1);
            } else {
                return path;
            }
        } else {
            return null;
        }
    }
    
    /* package */ static JTextComponent findComponent(Lookup lookup) {
        EditorCookie ec = (EditorCookie) lookup.lookup(EditorCookie.class);
        if (ec != null) {
            JEditorPane panes[] = ec.getOpenedPanes();
            if (panes != null && panes.length > 0) {
                return panes[0];
            }
        }
        return lookup.lookup(JTextComponent.class);
    }
}
