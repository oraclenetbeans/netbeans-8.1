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

package org.netbeans.modules.profiler.heapwalk.ui;

import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapFragmentWalkerUI_NavigateBackName=Navigate back",
    "HeapFragmentWalkerUI_NavigateBackDescr=Navigate back",
    "HeapFragmentWalkerUI_NavigateForwardName=Navigate forward",
    "HeapFragmentWalkerUI_NavigateForwardDescr=Navigate forward"
})
public class HeapFragmentWalkerUI extends JPanel {
    // --- UI definition ---------------------------------------------------------
    private static Icon ICON_BACK = Icons.getIcon(GeneralIcons.BACK);
    private static Icon ICON_FORWARD = Icons.getIcon(GeneralIcons.FORWARD);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AbstractAction backAction;
    private AbstractAction forwardAction;
    private AbstractButton oqlControllerPresenter;
    private AbstractButton analysisControllerPresenter;
    private AbstractButton classesControllerPresenter;
    private AbstractButton instancesControllerPresenter;
    private AbstractButton summaryControllerPresenter;
    private CardLayout controllerUIsLayout;
    private HeapFragmentWalker heapFragmentWalker;
    private JPanel oqlControllerPanel;
    private JPanel analysisControllerPanel;
    private JPanel classesControllerPanel;
    private JPanel controllerUIsPanel;
    private JPanel instancesControllerPanel;
    private JPanel summaryControllerPanel;
    private ProfilerToolbar toolBar;
    private boolean analysisEnabled;
    private boolean oqlEnabled;
    private int subControllersIndex;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public HeapFragmentWalkerUI(HeapFragmentWalker heapFragmentWalker) {
        this.heapFragmentWalker = heapFragmentWalker;

        ProfilerIDESettings pis = ProfilerIDESettings.getInstance();
        analysisEnabled = pis.getHeapWalkerAnalysisEnabled();
        oqlEnabled = OQLEngine.isOQLSupported();

        initComponents();
        updateNavigationActions();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isOQLViewActive() {
        if (oqlControllerPanel == null) return false;
        return oqlControllerPanel.isShowing();
    }

    public boolean isAnalysisViewActive() {
        if (analysisControllerPanel == null) return false;
        return analysisControllerPanel.isShowing();
    }

    public boolean isClassesViewActive() {
        return classesControllerPanel.isShowing();
    }

    public boolean isInstancesViewActive() {
        return instancesControllerPanel.isShowing();
    }

    // --- Public interface ------------------------------------------------------
    public boolean isSummaryViewActive() {
        return summaryControllerPanel.isShowing();
    }

    public void showOQLView() {
        showOQLView(true);
    }

    public void showAnalysisView() {
        showAnalysisView(true);
    }

    public void showClassesView() {
        showClassesView(true);
    }

    public void showHistoryOQLView() {
        showOQLView(false);
    }

    public void showHistoryAnalysisView() {
        showAnalysisView(false);
    }

    public void showHistoryClassesView() {
        showClassesView(false);
    }

    public void showHistoryInstancesView() {
        showInstancesView(false);
    }

    public void showHistorySummaryView() {
        showSummaryView(false);
    }

    public void showInstancesView() {
        showInstancesView(true);
    }

    public void showSummaryView() {
        showSummaryView(true);
    }

    // --- Internal interface ----------------------------------------------------
    public void updateNavigationActions() {
        backAction.setEnabled(heapFragmentWalker.isNavigationBackAvailable());
        forwardAction.setEnabled(heapFragmentWalker.isNavigationForwardAvailable());
    }

    private void initComponents() {
        summaryControllerPanel = heapFragmentWalker.getSummaryController().getPanel();
        classesControllerPanel = heapFragmentWalker.getClassesController().getPanel();
        instancesControllerPanel = heapFragmentWalker.getInstancesController().getPanel();
        if (oqlEnabled) {
            oqlControllerPanel = heapFragmentWalker.getOQLController().getPanel();
        }

        if (analysisEnabled) {
            analysisControllerPanel = heapFragmentWalker.getAnalysisController().getPanel();
        }

        summaryControllerPresenter = heapFragmentWalker.getSummaryController().getPresenter();
        classesControllerPresenter = heapFragmentWalker.getClassesController().getPresenter();
        instancesControllerPresenter = heapFragmentWalker.getInstancesController().getPresenter();

        if (oqlEnabled) {
            oqlControllerPresenter = heapFragmentWalker.getOQLController().getPresenter();
        }

        if (analysisEnabled) {
            analysisControllerPresenter = heapFragmentWalker.getAnalysisController().getPresenter();
        }

        backAction = new AbstractAction(Bundle.HeapFragmentWalkerUI_NavigateBackName(), ICON_BACK) {
                public void actionPerformed(ActionEvent e) {
                    heapFragmentWalker.navigateBack();
                }
            };
        backAction.putValue(Action.SHORT_DESCRIPTION, Bundle.HeapFragmentWalkerUI_NavigateBackDescr());

        forwardAction = new AbstractAction(Bundle.HeapFragmentWalkerUI_NavigateForwardName(), ICON_FORWARD) {
                public void actionPerformed(ActionEvent e) {
                    heapFragmentWalker.navigateForward();
                }
            };
        forwardAction.putValue(Action.SHORT_DESCRIPTION, Bundle.HeapFragmentWalkerUI_NavigateForwardDescr());

        setOpaque(false);
        setLayout(new BorderLayout());

        //unifyComponentsSize(classesControllerPresenter, instancesControllerPresenter);
        toolBar = ProfilerToolbar.create(false);
        toolBar.add(backAction);
        toolBar.add(forwardAction);
        toolBar.addSeparator();
        toolBar.add(summaryControllerPresenter);
        toolBar.add(classesControllerPresenter);
        toolBar.add(instancesControllerPresenter);

        if (analysisEnabled) {
            toolBar.add(analysisControllerPresenter);
        }

        if (oqlEnabled) {
            toolBar.add(oqlControllerPresenter);
        }
        
        toolBar.addFiller();
        subControllersIndex = toolBar.getComponentCount();

        controllerUIsLayout = new CardLayout();

        controllerUIsPanel = new JPanel(controllerUIsLayout);
        controllerUIsPanel.setOpaque(false);
        controllerUIsPanel.add(summaryControllerPanel, summaryControllerPresenter.getText());
        controllerUIsPanel.add(classesControllerPanel, classesControllerPresenter.getText());
        controllerUIsPanel.add(instancesControllerPanel, instancesControllerPresenter.getText());

        if (analysisEnabled) {
            controllerUIsPanel.add(analysisControllerPanel, analysisControllerPresenter.getText());
        }

        if (oqlEnabled) {
            controllerUIsPanel.add(oqlControllerPanel, oqlControllerPresenter.getText());
        }

        add(toolBar.getComponent(), BorderLayout.NORTH);
        add(controllerUIsPanel, BorderLayout.CENTER);

        summaryControllerPresenter.setSelected(true);
        classesControllerPresenter.setSelected(false);
        instancesControllerPresenter.setSelected(false);

        if (analysisEnabled) {
            analysisControllerPresenter.setSelected(false);
        }

        if (oqlEnabled) {
            oqlControllerPresenter.setSelected(false);
        }

        summaryControllerPresenter.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showSummaryView();
                }
            });
        classesControllerPresenter.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showClassesView();
                }
            });
        instancesControllerPresenter.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showInstancesView();
                }
            });

        if (analysisEnabled) {
            analysisControllerPresenter.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showAnalysisView();
                    }
                });
        }

        if (oqlEnabled) {
            oqlControllerPresenter.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showOQLView();
                    }
                });
        }

        // Classes view shown by default
        updateClientPresenters(heapFragmentWalker.getSummaryController().getClientPresenters());
        summaryControllerPresenter.setSelected(true);

        // support for traversing subtabs using Ctrl-Alt-PgDn/PgUp
        getActionMap().put("PreviousViewAction", // NOI18N
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    moveToPreviousView();
                    updatePresentersFocus();
                }
            });
        getActionMap().put("NextViewAction", // NOI18N
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    moveToNextView();
                    updatePresentersFocus();
                }
            });
    }

    private void moveToNextView() {
        if (isSummaryViewActive()) {
            showClassesView();
        } else if (isClassesViewActive()) {
            showInstancesView();
        } else if (isInstancesViewActive()) {
            if (analysisEnabled) {
                showAnalysisView();
            } else if (oqlEnabled) {
                showOQLView();
            } else {
                showSummaryView();
            }
        } else if (isAnalysisViewActive()) {
            if (oqlEnabled) {
                showOQLView();
            } else {
                showSummaryView();
            }
        } else if (isOQLViewActive()) {
            showSummaryView();
        }
    }

    private void moveToPreviousView() {
        if (isSummaryViewActive()) {
            if (oqlEnabled) {
                showOQLView();
            } else if (analysisEnabled) {
                showAnalysisView();
            } else {
                showInstancesView();
            }
        } else if (isOQLViewActive()) {
            if (analysisEnabled) {
                showAnalysisView();
            } else {
                showInstancesView();
            }
        } else if (isClassesViewActive()) {
            showSummaryView();
        } else if (isInstancesViewActive()) {
            showClassesView();
        } else if (isAnalysisViewActive()) {
            showInstancesView();
        }
    }

    private void showOQLView(boolean addToHistory) {
        if (!isOQLViewActive()) {
            if (addToHistory) {
                heapFragmentWalker.createNavigationHistoryPoint();
            }

            controllerUIsLayout.show(controllerUIsPanel, oqlControllerPresenter.getText());
            updateClientPresenters(heapFragmentWalker.getOQLController().getClientPresenters());
        }

        updatePresenters();
    }

    private void showAnalysisView(boolean addToHistory) {
        if (!isAnalysisViewActive()) {
            if (addToHistory) {
                heapFragmentWalker.createNavigationHistoryPoint();
            }

            controllerUIsLayout.show(controllerUIsPanel, analysisControllerPresenter.getText());
            updateClientPresenters(heapFragmentWalker.getAnalysisController().getClientPresenters());
        }

        updatePresenters();
    }

    private void showClassesView(boolean addToHistory) {
        if (!isClassesViewActive()) {
            if (addToHistory) {
                heapFragmentWalker.createNavigationHistoryPoint();
            }

            controllerUIsLayout.show(controllerUIsPanel, classesControllerPresenter.getText());
            updateClientPresenters(heapFragmentWalker.getClassesController().getClientPresenters());
        }

        updatePresenters();
    }

    private void showInstancesView(boolean addToHistory) {
        if (!isInstancesViewActive()) {
            if (addToHistory) {
                heapFragmentWalker.createNavigationHistoryPoint();
            }

            controllerUIsLayout.show(controllerUIsPanel, instancesControllerPresenter.getText());
            updateClientPresenters(heapFragmentWalker.getInstancesController().getClientPresenters());
        }

        updatePresenters();
    }

    // --- Private implementation ------------------------------------------------
    private void showSummaryView(boolean addToHistory) {
        if (!isSummaryViewActive()) {
            if (addToHistory) {
                heapFragmentWalker.createNavigationHistoryPoint();
            }

            controllerUIsLayout.show(controllerUIsPanel, summaryControllerPresenter.getText());
            updateClientPresenters(heapFragmentWalker.getSummaryController().getClientPresenters());
        }

        updatePresenters();
    }

    private void unifyComponentsSize(Component c1, Component c2) {
        Dimension preferredSize = new Dimension(Math.max(c1.getPreferredSize().width, c2.getPreferredSize().width),
                                                Math.max(c1.getPreferredSize().height, c2.getPreferredSize().height));
        c1.setPreferredSize(preferredSize);
        c2.setPreferredSize(preferredSize);
    }

    private void updateClientPresenters(AbstractButton[] clientPresenters) {
        while (toolBar.getComponentCount() > subControllersIndex) {
            toolBar.remove(subControllersIndex);
        }

        for (int i = 0; i < clientPresenters.length; i++) {
            toolBar.add(clientPresenters[i]);
        }
    }

    private void updatePresenters() {
        summaryControllerPresenter.setSelected(summaryControllerPanel.isShowing());
        classesControllerPresenter.setSelected(classesControllerPanel.isShowing());
        instancesControllerPresenter.setSelected(instancesControllerPanel.isShowing());

        if (analysisEnabled) {
            analysisControllerPresenter.setSelected(analysisControllerPanel.isShowing());
        }
        if (oqlEnabled) {
            oqlControllerPresenter.setSelected(oqlControllerPanel.isShowing());
        }
    }

    private void updatePresentersFocus() {
        if (summaryControllerPresenter.isSelected())
            summaryControllerPresenter.requestFocus();
        else if (classesControllerPresenter.isSelected())
            classesControllerPresenter.requestFocus();
        else if (instancesControllerPresenter.isSelected())
            instancesControllerPresenter.requestFocus();
        else if (analysisEnabled && analysisControllerPresenter.isSelected())
            analysisControllerPresenter.requestFocus();
        else if (oqlEnabled && oqlControllerPresenter.isSelected())
            oqlControllerPresenter.requestFocus();
    }
}
