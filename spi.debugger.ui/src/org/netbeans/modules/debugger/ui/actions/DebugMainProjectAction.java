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

package org.netbeans.modules.debugger.ui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Properties;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.debugger.ui.AttachType;
import org.netbeans.spi.debugger.ui.Controller;
import org.netbeans.spi.debugger.ui.PersistentController;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.BuildExecutionSupport;
import org.netbeans.spi.project.ui.support.MainProjectSensitiveActions;
import org.openide.awt.Actions;
import org.openide.awt.DropDownButtonFactory;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.util.WeakSet;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Martin Entlicher
 */
public class DebugMainProjectAction implements Action, Presenter.Toolbar, PopupMenuListener {

    private static WeakSet<AttachHistorySupport> ahs = null;
    
    private final Action delegate;
    private final DebugHistorySupport debugHistorySupport;
    private final AttachHistorySupport attachHistorySupport;
    
    /** Creates a new instance of DebugMainProjectAction */
    public DebugMainProjectAction() {
        delegate = MainProjectSensitiveActions.mainProjectCommandAction(
                ActionProvider.COMMAND_DEBUG,
                NbBundle.getMessage(DebugMainProjectAction.class, "LBL_DebugMainProjectAction_Name" ),ImageUtilities.loadImageIcon("org/netbeans/modules/debugger/resources/debugProject.png", false)); // NOI18N
        delegate.putValue("iconBase","org/netbeans/modules/debugger/resources/debugProject.png"); //NOI18N
        debugHistorySupport = new DebugHistorySupport();
        attachHistorySupport = new AttachHistorySupport();
    }
    
    @Override public Object getValue(String arg0) {
        return delegate.getValue(arg0);
    }

    @Override public void putValue(String arg0, Object arg1) {
        delegate.putValue(arg0, arg1);
    }

    @Override public void setEnabled(boolean arg0) {
        delegate.setEnabled(arg0);
    }

    @Override public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override public void addPropertyChangeListener(PropertyChangeListener arg0) {
        delegate.addPropertyChangeListener(arg0);
    }

    @Override public void removePropertyChangeListener(PropertyChangeListener arg0) {
        delegate.removePropertyChangeListener(arg0);
    }

    @Override public void actionPerformed(ActionEvent arg0) {
        Project p = OpenProjects.getDefault().getMainProject();
        GestureSubmitter.logDebugProject(p);
        delegate.actionPerformed(arg0);
    }

    @Override public Component getToolbarPresenter() {
        JPopupMenu menu = new JPopupMenu();
        JButton button = DropDownButtonFactory.createDropDownButton(
                new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)), menu);
        final JMenuItem item = new JMenuItem(Actions.cutAmpersand((String) delegate.getValue("menuText")));
        item.setEnabled(delegate.isEnabled());

        delegate.addPropertyChangeListener(new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                String propName = evt.getPropertyName();
                if ("enabled".equals(propName)) {
                    item.setEnabled((Boolean)evt.getNewValue());
                } else if ("menuText".equals(propName)) {
                    item.setText(Actions.cutAmpersand((String) evt.getNewValue()));
                }
            }
        });

        menu.add(item);
        item.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                DebugMainProjectAction.this.actionPerformed(e);
            }
        });
        try {
            Action ca = Actions.forID("Debug", "org.netbeans.modules.debugger.ui.actions.ConnectAction");
            JMenuItem item2 = new JMenuItem(Actions.cutAmpersand((String) ca.getValue(NAME)));
            Actions.connect(item2, ca);
            menu.add(item2);
        } catch (Exception nsee) {
            Exceptions.printStackTrace(nsee);
        }

        menu.addPopupMenuListener(this);

        Actions.connect(button, this);
        return button;
    }

    static synchronized void attachHistoryChanged() {
        if (ahs == null) { return; }
        for (AttachHistorySupport support : ahs) {
            support.computeItems();
        }
    }

    private static synchronized void addAttachHistorySupport(AttachHistorySupport support) {
        if (ahs == null) {
            ahs = new WeakSet<AttachHistorySupport>();
        }
        ahs.add(support);
    }

    // PopupMenuListener ........................................................

    @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JPopupMenu menu = (JPopupMenu)e.getSource();
        debugHistorySupport.init(menu);
        attachHistorySupport.init(menu);
        menu.removePopupMenuListener(this);
    }

    @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    @Override public void popupMenuCanceled(PopupMenuEvent e) {
    }
    
    private static class DebugHistorySupport implements ActionListener, ChangeListener {

        private JPopupMenu menu;
        private JMenuItem[] items = new JMenuItem[0];
        private final JSeparator separator1 = new JPopupMenu.Separator();
        private final JSeparator separator2 = new JPopupMenu.Separator();
        private final BuildExecutionSupportChangeSupport besc;
        private final LinkedList<BuildExecutionSupport.ActionItem> debugItems = new LinkedList<BuildExecutionSupport.ActionItem>();
        
        private static final int MAX_ITEMS_COUNT = 7;
        private static final String DEBUG_ACTION_ITEM_PROP_NAME = "debug action item";
        private static final RequestProcessor RP = new RequestProcessor(DebugHistorySupport.class.getName());
        
        public DebugHistorySupport() {
            besc = new BuildExecutionSupportChangeSupport();
            besc.addChangeListener(WeakListeners.change(this, besc));
        }
        
        void init(JPopupMenu menu) {
            this.menu = menu;
            computeItems();
        }
        
        private void computeItems() {
            if (menu == null) {
                return ;
            }
            boolean wasSeparator = items.length > 0;
            for (int i = 0; i < items.length; i++) {
                menu.remove(items[i]);
            }
            synchronized (debugItems) {
                if (debugItems.isEmpty()) {
                    items = new JMenuItem[0];
                } else {
                    int n = debugItems.size();
                    items = new JMenuItem[n];
                    int i = 0;
                    for (BuildExecutionSupport.ActionItem ai : debugItems) {
                        String dispName = ai.getDisplayName();
                        items[i] = new JMenuItem(dispName);
                        items[i].putClientProperty(DEBUG_ACTION_ITEM_PROP_NAME, ai);
                        items[i].addActionListener(this);
                        i++;
                    }
                }
            }
            if (items.length == 0) {
                if (wasSeparator) {
                    menu.remove(separator1);
                    menu.remove(separator2);
                }
            } else {
                if (!wasSeparator) {
                    menu.insert(separator1, 1);
                }
                int i;
                for (i = 0; i < items.length; i++) {
                    menu.insert(items[i], i + 2);
                }
                menu.insert(separator2, i + 2);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem)e.getSource();
            final BuildExecutionSupport.ActionItem ai =
                    (BuildExecutionSupport.ActionItem) item.getClientProperty(DEBUG_ACTION_ITEM_PROP_NAME);
            RP.post(new Runnable() {
                @Override
                public void run() {
                    ai.repeatExecution();
                }
            });
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            BuildExecutionSupport.Item lastItem = BuildExecutionSupport.getLastFinishedItem();
            if (lastItem instanceof BuildExecutionSupport.ActionItem) {
                BuildExecutionSupport.ActionItem ai = (BuildExecutionSupport.ActionItem) lastItem;
                String action = ai.getAction();
                if (ActionProvider.COMMAND_DEBUG.equals(action)) { // Track debug items only
                    boolean changed = false;
                    synchronized (debugItems) {
                        if (debugItems.isEmpty() || ai != debugItems.getFirst()) {
                            debugItems.remove(ai); // Remove it if it's there
                            debugItems.addFirst(ai);
                            if (debugItems.size() > MAX_ITEMS_COUNT) {
                                debugItems.removeLast();
                            }
                            changed = true;
                        }
                    }
                    if (changed) {
                        computeItems();
                    }
                }
            }
        }
    }
    
    private static class BuildExecutionSupportChangeSupport {
        
        public void addChangeListener(ChangeListener listener) {
            BuildExecutionSupport.addChangeListener(listener);
        }
        
        public void removeChangeListener(ChangeListener listener) {
            BuildExecutionSupport.removeChangeListener(listener);
        }
    }

    // AttachHistorySupport .....................................................

    static class AttachHistorySupport implements ActionListener {

        private JPopupMenu menu;
        private JMenuItem[] items = new JMenuItem[0];
        private JSeparator separator = new JPopupMenu.Separator();
        private static final RequestProcessor RP = new RequestProcessor(AttachHistorySupport.class.getName());

        public void init(JPopupMenu menu) {
            this.menu = menu;
            addAttachHistorySupport(this);
            computeItems();
        }

        public void computeItems() {
            menu.remove(separator);
            for (int x = 0; x < items.length; x++) {
                menu.remove(items[x]);
            } // for
            Properties props = Properties.getDefault().getProperties("debugger").getProperties("last_attaches");
            Integer[] usedSlots = (Integer[]) props.getArray("used_slots", new Integer[0]);
            if (usedSlots.length > 0) {
                menu.add(separator);
            }
            items = new JMenuItem[usedSlots.length];
            for (int x = 0; x < usedSlots.length; x++) {
                String dispName = props.getProperties("slot_" + usedSlots[x]).getString("display_name", "<???>"); // NOI18N
                items[x] = new JMenuItem(dispName);
                items[x].addActionListener(this);
                menu.add(items[x]);
            } // for
        }

        @Override public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem)e.getSource();
            int index = -1;
            for (int x = 0; x < items.length; x++) {
                if (items[x] == item) {
                    index = x;
                    break;
                }
            }
            if (index == -1) { return; } // should not occure
            final int findex = index;
            RP.post(new Runnable() {
                @Override
                public void run() {
                    perform(findex);
                }
            });
        }
        
        private void perform(int index) {
            Properties props = Properties.getDefault().getProperties("debugger").getProperties("last_attaches");
            Integer[] usedSlots = (Integer[]) props.getArray("used_slots", new Integer[0]);
            String attachTypeName = props.getProperties("slot_" + usedSlots[index]).getString("attach_type", "???");
            List types = DebuggerManager.getDebuggerManager().lookup (null, AttachType.class);
            AttachType att = null;
            for (Object t : types) {
                AttachType at = (AttachType)t;
                if (attachTypeName.equals(at.getTypeDisplayName())) {
                    att = at;
                    break;
                }
            } // for
            if (att != null) {
                final AttachType attachType = att;
                final PersistentController[] controllerPtr = new PersistentController[] { null };
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            JComponent customizer = attachType.getCustomizer ();
                            Controller controller = attachType.getController();
                            if (controller == null && (customizer instanceof Controller)) {
                                Exceptions.printStackTrace(new IllegalStateException("FIXME: JComponent "+customizer+" must not implement Controller interface!"));
                                controller = (Controller) customizer;
                            }
                            if (controller instanceof PersistentController) {
                                controllerPtr[0] = (PersistentController) controller;
                            }
                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                    return ;
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                    return ;
                }
                final PersistentController controller = controllerPtr[0];
                if (controller == null) {
                    return ;
                }
                boolean result = controller.load(props.getProperties("slot_" + usedSlots[index]).getProperties("values"));
                if (!result) {
                    return; // [TODO] not loaded, cannot be used to attach
                }
                final boolean[] passedPtr = new boolean[] { false };
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            passedPtr[0] = controller.ok();
                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                    return ;
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                    return ;
                }
                if (passedPtr[0]) {
                    makeFirst(index);
                    GestureSubmitter.logAttach(attachTypeName);
                }
                //return;
            } else {
                // report failure - attach type not found
                StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(DebugMainProjectAction.class, "CTL_Attach_Type_Not_Found"));
            }
        }

        private void makeFirst(int index) {
            if (index == 0) {
                return;  // nothing to do
            }
            Properties props = Properties.getDefault().getProperties("debugger").getProperties("last_attaches");
            Integer[] usedSlots = (Integer[]) props.getArray("used_slots", new Integer[0]);
            int temp = usedSlots[index];
            for (int x = index; x > 0; x--) {
                usedSlots[x] = usedSlots[x - 1];
            }
            usedSlots[0] = temp;
            props.setArray("used_slots", usedSlots);
            attachHistoryChanged();
        }

    } // AttachHistorySupport


}
