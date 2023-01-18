/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.graph;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.action.TwoStateHoverProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.animator.AnimatorEvent;
import org.netbeans.api.visual.animator.AnimatorListener;
import org.netbeans.api.visual.export.SceneExporter;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.graph.layout.GraphLayout;
import org.netbeans.api.visual.graph.layout.GraphLayoutFactory;
import org.netbeans.api.visual.graph.layout.GraphLayoutSupport;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.modules.maven.api.CommonArtifactActions;
import org.netbeans.modules.maven.api.ModelUtils;
import org.netbeans.modules.maven.api.NbMavenProject;
import static org.netbeans.modules.maven.graph.Bundle.*;
import org.netbeans.modules.maven.graph.FixVersionConflictPanel.FixDescription;
import org.netbeans.modules.maven.indexer.api.ui.ArtifactViewer;
import org.netbeans.modules.maven.model.pom.Exclusion;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Profile;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Milos Kleint 
 */
public class DependencyGraphScene extends GraphScene<ArtifactGraphNode, ArtifactGraphEdge> implements Runnable {
    
    private static final RequestProcessor RP = new RequestProcessor(DependencyGraphScene.class);
    private static final Logger LOG = Logger.getLogger(DependencyGraphScene.class.getName());
    
    private LayerWidget mainLayer;
    private final LayerWidget connectionLayer;
    private ArtifactGraphNode rootNode;
    private final AllActionsProvider allActionsP = new AllActionsProvider();
    
//    private GraphLayout layout;
    private final WidgetAction moveAction = ActionFactory.createMoveAction(null, allActionsP);
    private final WidgetAction popupMenuAction = ActionFactory.createPopupMenuAction(allActionsP);
    private final WidgetAction zoomAction = ActionFactory.createMouseCenteredZoomAction(1.1);
    private final WidgetAction panAction = ActionFactory.createPanAction();
    private final WidgetAction editAction = ActionFactory.createEditAction(allActionsP);
    WidgetAction hoverAction = ActionFactory.createHoverAction(new HoverController());

    Action sceneZoomToFitAction = new SceneZoomToFitAction();
    Action highlitedZoomToFitAction = new HighlightedZoomToFitAction();

    private FruchtermanReingoldLayout layout;
    private int maxDepth = 0;
    private final MavenProject project;
    private final Project nbProject;
    private final DependencyGraphTopComponent tc;
    private FitToViewLayout fitViewL;

    private static final Set<ArtifactGraphNode> EMPTY_SELECTION = new HashSet<ArtifactGraphNode>();
    private final POMModel model;
    private final Map<Artifact, Icon> projectIcons;
    private JScrollPane pane;
    private AtomicBoolean animated = new AtomicBoolean(false);
    
    /** Creates a new instance ofla DependencyGraphScene */
    DependencyGraphScene(MavenProject prj, Project nbProj, DependencyGraphTopComponent tc,
            POMModel model) {
        project = prj;
        nbProject = nbProj;
        this.tc = tc;
        this.model = model;
        mainLayer = new LayerWidget(this);
        addChild(mainLayer);
        connectionLayer = new LayerWidget(this);
        addChild(connectionLayer);
        
        //this glasspane thing is only here to get rid of connection lines across widgets
        Widget glasspane = new LayerWidget(this) {

            @Override
            protected void paintChildren() {
                if (isCheckClipping()) {
                    Rectangle clipBounds = DependencyGraphScene.this.getGraphics().getClipBounds();
                    for (Widget child : mainLayer.getChildren()) {
                        Point location = child.getLocation();
                        Rectangle bounds = child.getBounds();
                        bounds.translate(location.x, location.y);
                        if (clipBounds == null || bounds.intersects(clipBounds)) {
                            child.paint();
                        }
                    }
                } else {
                    for (Widget child : mainLayer.getChildren()) {
                        child.paint();
                    }
                }
            }

        };
        addChild(glasspane);
        //getActions().addAction(this.createObjectHoverAction());
        getActions().addAction(hoverAction);
        getActions().addAction(ActionFactory.createSelectAction(allActionsP));
        getActions().addAction(zoomAction);
        getActions().addAction(panAction);
        getActions().addAction(editAction);
        getActions().addAction(popupMenuAction);
        this.projectIcons = getIconsForOpenProjects();
    }

    /**
     * @return map of maven artifact mapped to project icon
     */
    private Map<Artifact, Icon> getIconsForOpenProjects() {
        Map<Artifact, Icon> result = new HashMap<Artifact, Icon>();
        //NOTE: surely not the best way to get the project icon
        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
        for (Project project : openProjects) {
            NbMavenProject mavenProject = project.getLookup().lookup(NbMavenProject.class);
            if (null != mavenProject) {
                Artifact artifact = mavenProject.getMavenProject().getArtifact();
                //get icon from opened project
                Icon icon = ProjectUtils.getInformation(project).getIcon();
                if (null != icon) {
                    result.put(artifact, icon);
                }
            }
        }
        return result;
    }

    void setMyZoomFactor(double zoom) {
        setZoomFactor(zoom);
        ArrayList<Widget> arr = new ArrayList<Widget>();
        arr.addAll(mainLayer.getChildren());
        arr.addAll(connectionLayer.getChildren());
        for (Widget wid : arr) {
            if (wid instanceof ArtifactWidget) {
                ((ArtifactWidget)wid).updateReadableZoom();
            }
            if (wid instanceof EdgeWidget) {
                ((EdgeWidget)wid).updateReadableZoom();
            }
        }
    }

    void initialLayout() {
        //start using default layout
        layout =  new FruchtermanReingoldLayout(this, pane);
        layout.invokeLayout();
        addSceneListener(new SceneListener() {

            @Override
            public void sceneRepaint() {
               
            }

            @Override
            public void sceneValidating() {
                
            }

            @Override
            public void sceneValidated() {
                //the first layout has not be non animated, then we can fit to zoom easily
                if (animated.compareAndSet(false, true)) {
                    new SceneZoomToFitAction().actionPerformed(null);
                }
                    
            }
        });
    }
    
    void setSurroundingScrollPane(JScrollPane pane) {
        this.pane=pane;
    }
    
    ArtifactGraphNode getRootGraphNode() {
        return rootNode;
    }

    int getMaxNodeDepth() {
        return maxDepth;
    }

    Project getNbProject () {
        return nbProject;
    }

    MavenProject getMavenProject () {
        return project;
    }

    boolean isAnimated () {
        return animated.get();
    }

    @CheckForNull ArtifactGraphNode getGraphNodeRepresentant(DependencyNode node) {
        for (ArtifactGraphNode grnode : getNodes()) {
            if (grnode.represents(node)) {
                return grnode;
            }
        }
        return null;
    }
    
    @Override protected Widget attachNodeWidget(ArtifactGraphNode node) {
        if (rootNode == null) {
            rootNode = node;
        }
        if (node.getPrimaryLevel() > maxDepth) {
            maxDepth = node.getPrimaryLevel();
        }
        Artifact artifact = node.getArtifact().getArtifact();
        Icon icon = projectIcons.get(artifact);
        ArtifactWidget root = new ArtifactWidget(this, node, icon);
        mainLayer.addChild(root);
        node.setWidget(root);
        root.setOpaque(true);
        
        root.getActions().addAction(this.createObjectHoverAction());
        root.getActions().addAction(this.createSelectAction());
        root.getActions().addAction(moveAction);
        root.getActions().addAction(editAction);
        root.getActions().addAction(popupMenuAction);
        
        return root;
    }
    
    @Override protected Widget attachEdgeWidget(ArtifactGraphEdge edge) {
        EdgeWidget connectionWidget = new EdgeWidget(this, edge);
        connectionLayer.addChild(connectionWidget);
        return connectionWidget;
    }
    
    @Override protected void attachEdgeSourceAnchor(ArtifactGraphEdge edge,
            ArtifactGraphNode oldsource,
            ArtifactGraphNode source) {
        ((ConnectionWidget) findWidget(edge)).setSourceAnchor(AnchorFactory.createRectangularAnchor(findWidget(source)));
        
    }
    
    @Override protected void attachEdgeTargetAnchor(ArtifactGraphEdge edge,
            ArtifactGraphNode oldtarget,
            ArtifactGraphNode target) {
        ArtifactWidget wid = (ArtifactWidget)findWidget(target);
        ((ConnectionWidget) findWidget(edge)).setTargetAnchor(AnchorFactory.createRectangularAnchor(wid));
    }

    void highlightRelated (ArtifactGraphNode node) {
        List<ArtifactGraphNode> importantNodes = new ArrayList<ArtifactGraphNode>();
        List<ArtifactGraphEdge> otherPathsEdges = new ArrayList<ArtifactGraphEdge>();
        List<ArtifactGraphEdge> primaryPathEdges = new ArrayList<ArtifactGraphEdge>();
        List<ArtifactGraphNode> childrenNodes = new ArrayList<ArtifactGraphNode>();
        List<ArtifactGraphEdge> childrenEdges = new ArrayList<ArtifactGraphEdge>();

        importantNodes.add(node);

        @SuppressWarnings("unchecked")
        List<DependencyNode> children = (List<DependencyNode>)node.getArtifact().getChildren();
        for (DependencyNode n : children) {
            ArtifactGraphNode child = getGraphNodeRepresentant(n);
            if (child != null) {
                childrenNodes.add(child);
            }
        }

        childrenEdges.addAll(findNodeEdges(node, true, false));

        // primary path
        addPathToRoot(node, primaryPathEdges, importantNodes);

        // other important paths
        List<DependencyNode> representants = new ArrayList<DependencyNode>(node.getDuplicatesOrConflicts());
        for (DependencyNode curRep : representants) {
            addPathToRoot(curRep, curRep.getParent(), otherPathsEdges, importantNodes);
        }

        EdgeWidget ew;
        for (ArtifactGraphEdge curE : getEdges()) {
            ew = (EdgeWidget) findWidget(curE);
            if (primaryPathEdges.contains(curE)) {
                ew.setState(EdgeWidget.HIGHLIGHTED_PRIMARY);
            } else if (otherPathsEdges.contains(curE)) {
                ew.setState(EdgeWidget.HIGHLIGHTED);
            } else if (childrenEdges.contains(curE)) {
                ew.setState(EdgeWidget.GRAYED);
            } else {
                ew.setState(EdgeWidget.DISABLED);
            }
        }

        ArtifactWidget aw;
        for (ArtifactGraphNode curN : getNodes()) {
            aw = (ArtifactWidget) findWidget(curN);
            if (importantNodes.contains(curN)) {
                aw.setPaintState(EdgeWidget.REGULAR);
                aw.setReadable(true);
            } else if (childrenNodes.contains(curN)) {
                aw.setPaintState(EdgeWidget.REGULAR);
                aw.setReadable(true);
            } else {
                aw.setPaintState(EdgeWidget.DISABLED);
                aw.setReadable(false);
            }
        }

    }

    private void addPathToRoot(ArtifactGraphNode node, List<ArtifactGraphEdge> edges, List<ArtifactGraphNode> nodes) {
        DependencyNode parentDepN = node.getArtifactParent();
        addPathToRoot(node.getArtifact(), parentDepN, edges, nodes);
    }


    private void addPathToRoot(DependencyNode depN, DependencyNode parentDepN, List<ArtifactGraphEdge> edges, List<ArtifactGraphNode> nodes) {
        ArtifactGraphNode grNode;
        while (parentDepN != null) {
            grNode = getGraphNodeRepresentant(parentDepN);
            if (grNode == null) {
                return;
            }
            ArtifactGraphNode targetNode = getGraphNodeRepresentant(depN);
            if (targetNode == null) {
                return;
            }
            edges.addAll(findEdgesBetween(grNode, targetNode));
            nodes.add(grNode);
            depN = parentDepN;
            parentDepN = grNode.getArtifactParent();
        }
    }



    private class AllActionsProvider implements PopupMenuProvider, 
            MoveProvider, EditProvider, SelectProvider {

        private Point moveStart;

/*        public void select(Widget wid, Point arg1, boolean arg2) {
            System.out.println("select called...");
            Widget w = wid;
            while (w != null) {
                ArtifactGraphNode node = (ArtifactGraphNode)findObject(w);
                if (node != null) {
                    setSelectedObjects(Collections.singleton(node));
                    System.out.println("selected object: " + node.getArtifact().getArtifact().getArtifactId());
                    highlightRelated(node);
                    ((ArtifactWidget)w).setSelected(true);
                    return;
                }
                w = w.getParentWidget();
            }
        }*/

        @Messages({
            "ACT_Show_Graph=Show Dependency Graph", 
            "ACT_Export_As_Image=Export As Image",
            "ACT_LayoutSubMenu=Layout",
            "ACT_Export_As_Image_Title=Export Dependency Graph As PNG"
        })
        @Override public JPopupMenu getPopupMenu(Widget widget, Point localLocation) {
            JPopupMenu popupMenu = new JPopupMenu();
            if (widget == DependencyGraphScene.this) {
                popupMenu.add(sceneZoomToFitAction);
                final JMenu layoutMenu = new JMenu(Bundle.ACT_LayoutSubMenu());
                popupMenu.add(layoutMenu);
                layoutMenu.add(new FruchtermanReingoldLayoutAction());
                layoutMenu.add(new JSeparator());
                layoutMenu.add(new HierarchicalGraphLayoutAction());
                layoutMenu.add(new TreeGraphLayoutVerticalAction());
                layoutMenu.add(new TreeGraphLayoutHorizontalAction());
                popupMenu.add(new AbstractAction(Bundle.ACT_Export_As_Image()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        File file = new FileChooserBuilder("DependencyGraphScene-ExportDir").setTitle(Bundle.ACT_Export_As_Image_Title())
                                .setAcceptAllFileFilterUsed(false).addFileFilter(new FileNameExtensionFilter("PNG file", "png")).showSaveDialog();
                        if (file != null) {
                            try {
                                DependencyGraphScene theScene = DependencyGraphScene.this;
                                SceneExporter.createImage(theScene, file, SceneExporter.ImageType.PNG, SceneExporter.ZoomType.CURRENT_ZOOM_LEVEL, false, false, -1, -1, -1);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    }
                });
            } else {
                ArtifactGraphNode node = (ArtifactGraphNode)findObject(widget);
                if (isEditable()) {
                    boolean addSeparator = false;
                    if (isFixCandidate(node)) {
                        popupMenu.add(new FixVersionConflictAction(node));
                        addSeparator = true;
                    }
                    if (node.getPrimaryLevel() > 1) {
                        popupMenu.add(new ExcludeDepAction(node));
                        addSeparator = true;
                    }
                    if (addSeparator) {
                        popupMenu.add(new JSeparator());
                    }
                }
                popupMenu.add(highlitedZoomToFitAction);
                if (!node.isRoot()) {
                    Action a = CommonArtifactActions.createViewArtifactDetails(node.getArtifact().getArtifact(), project.getRemoteArtifactRepositories());
                    a.putValue("PANEL_HINT", ArtifactViewer.HINT_GRAPH); //NOI18N
                    a.putValue(Action.NAME, ACT_Show_Graph());
                    popupMenu.add(a);
                }
            }
            return popupMenu;
        }

        @Override public void movementStarted(Widget widget) {
            widget.bringToFront();
            moveStart = widget.getLocation();
        }
        @Override public void movementFinished(Widget widget) {
            // little hack to call highlightRelated on mouse click while leaving
            // normal move behaviour on real dragging
            Point moveEnd = widget.getLocation();
            if (moveStart.distance(moveEnd) < 5) {
                Object obj = DependencyGraphScene.this.findObject(widget);
                if (obj instanceof ArtifactGraphNode) {
                    DependencyGraphScene.this.highlightRelated((ArtifactGraphNode)obj);
                }
            }
        }
        @Override public Point getOriginalLocation(Widget widget) {
            return widget.getPreferredLocation ();
        }
        @Override public void setNewLocation(Widget widget, Point location) {
            widget.setPreferredLocation (location);
        }

        @Override public void edit(Widget widget) {
            if (DependencyGraphScene.this == widget) {
                sceneZoomToFitAction.actionPerformed(null);
            } else {
                highlitedZoomToFitAction.actionPerformed(null);
            }
        }

        @Override public boolean isAimingAllowed(Widget widget, Point localLocation, boolean invertSelection) {
            return false;
        }

        @Override public boolean isSelectionAllowed(Widget widget, Point localLocation, boolean invertSelection) {
            return true;
        }

        @Override public void select(Widget widget, Point localLocation, boolean invertSelection) {
            setSelectedObjects(EMPTY_SELECTION);
            DependencyGraphScene.this.tc.depthHighlight();
        }
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        super.notifyStateChanged(previousState, state);

        if (!previousState.isSelected() && state.isSelected()) {
            tc.depthHighlight();
        }
    }

    private FitToViewLayout getFitToViewLayout () {
        if (fitViewL == null) {
            fitViewL = new FitToViewLayout(this);
        }
        return fitViewL;
    }

    private static class FitToViewLayout extends SceneLayout {

        private final DependencyGraphScene depScene;
        private List<? extends Widget> widgets;

        FitToViewLayout(DependencyGraphScene scene) {
            super(scene);
            this.depScene = scene;
        }

        
        protected void fitToView(@NullAllowed List<? extends Widget> widgets) {
            this.widgets = widgets;
            this.invokeLayout(); 
        }

        @Override
        protected void performLayout() {
            Rectangle rectangle = null;
            List<? extends Widget> toFit = widgets != null ? widgets : depScene.getChildren();
            if (toFit == null) {
                return;
            }

            for (Widget widget : toFit) {
                Rectangle bounds = widget.getBounds();
                if (bounds == null) {
                    continue;
                }
                if (rectangle == null) {
                    rectangle = widget.convertLocalToScene(bounds);
                } else {
                    rectangle = rectangle.union(widget.convertLocalToScene(bounds));
                }
            }
            // margin around
            if (widgets == null) {
                rectangle.grow(5, 5);
            } else {
                rectangle.grow(25, 25);
            }
            Dimension dim = rectangle.getSize();
            Dimension viewDim = depScene.tc.getScrollPane().
                    getViewportBorderBounds ().getSize ();
            double zf = Math.min ((double) viewDim.width / dim.width, (double) viewDim.height / dim.height);
            if (depScene.isAnimated()) {
                if (widgets == null) {
                    depScene.getSceneAnimator().animateZoomFactor(zf);
                } else {
                    CenteredZoomAnimator cza = new CenteredZoomAnimator(depScene.getSceneAnimator());
                    cza.setZoomFactor(zf,
                            new Point((int)rectangle.getCenterX(), (int)rectangle.getCenterY()));
                }
            } else {
                depScene.setMyZoomFactor (zf);
            }
        }
    }

    private class SceneZoomToFitAction extends AbstractAction {

        @Messages("ACT_ZoomToFit=Zoom To Fit")
        SceneZoomToFitAction() {
            putValue(NAME, ACT_ZoomToFit());
        }

        @Override public void actionPerformed(ActionEvent e) {
            DependencyGraphScene.this.getFitToViewLayout().fitToView(null);
        }
    };

    private class HighlightedZoomToFitAction extends AbstractAction {

        HighlightedZoomToFitAction() {
            putValue(NAME, ACT_ZoomToFit());
        }

        @Override public void actionPerformed(ActionEvent e) {
            Collection<ArtifactGraphNode> grNodes = DependencyGraphScene.this.getNodes();
            List<ArtifactWidget> aws = new ArrayList<ArtifactWidget>();
            ArtifactWidget aw = null;
            int paintState;
            for (ArtifactGraphNode grNode : grNodes) {
                aw = grNode.getWidget();
                paintState = aw.getPaintState();
                if (paintState != EdgeWidget.DISABLED && paintState != EdgeWidget.GRAYED) {
                    aws.add(aw);
                }
            }
            DependencyGraphScene.this.getFitToViewLayout().fitToView(aws);
        }
    };

    boolean isEditable () {
        return model != null;
    }

    static boolean isFixCandidate (ArtifactGraphNode node) {
        Set<DependencyNode> conf = node.getDuplicatesOrConflicts();
        ArtifactVersion nodeV = new DefaultArtifactVersion(node.getArtifact().getArtifact().getVersion());
        for (DependencyNode dn : conf) {
            if (dn.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
                if (nodeV.compareTo(new DefaultArtifactVersion(dn.getArtifact().getVersion())) < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    static ArtifactVersion findNewest (ArtifactGraphNode node, boolean all) {
        Set<DependencyNode> conf = node.getDuplicatesOrConflicts();
        ArtifactVersion result = new DefaultArtifactVersion(node.getArtifact().getArtifact().getVersion());
        ArtifactVersion curV = null;
        for (DependencyNode dn : conf) {
            if (all || dn.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
                curV = new DefaultArtifactVersion(dn.getArtifact().getVersion());
                if (result.compareTo(curV) < 0) {
                    result = curV;
                }
            }
        }
        return result;
    }

    void invokeFixConflict (ArtifactGraphNode node) {
        new FixVersionConflictAction(node).actionPerformed(null);
    }

    /** Note, must be called inside model transaction */
    private void excludeDepFromModel (ArtifactGraphNode node, Set<Artifact> exclTargets) {
        assert model.isIntransaction() : "Must be called inside transaction"; //NOI18N

        Artifact nodeArtif = node.getArtifact().getArtifact();

        for (Artifact eTarget : exclTargets) {
            org.netbeans.modules.maven.model.pom.Dependency dep =
                    model.getProject().findDependencyById(
                    eTarget.getGroupId(), eTarget.getArtifactId(), null);
            if (dep == null) {
                // now check the active profiles for the dependency..
                List<String> profileNames = new ArrayList<String>();
                NbMavenProject nbMavproject = nbProject.getLookup().lookup(NbMavenProject.class);
                for (org.apache.maven.model.Profile prof : nbMavproject.getMavenProject().getActiveProfiles()) {
                    profileNames.add(prof.getId());
                }
                for (String profileId : profileNames) {
                    Profile modProf = model.getProject().findProfileById(profileId);
                    if (modProf != null) {
                        dep = modProf.findDependencyById(eTarget.getGroupId(), eTarget.getArtifactId(), null);
                        if (dep != null) {
                            break;
                        }
                    }
                }
            }
            if (dep == null) {
                // must create dependency if not found locally, so that
                // there is a place where to add dep exclusion
                dep = model.getFactory().createDependency();
                dep.setGroupId(eTarget.getGroupId());
                dep.setArtifactId(eTarget.getArtifactId());
                dep.setType(eTarget.getType());
                dep.setVersion(eTarget.getVersion());
                model.getProject().addDependency(dep);
            }
            Exclusion ex = dep.findExclusionById(
                    nodeArtif.getGroupId(), nodeArtif.getArtifactId());
            if (ex == null) {
                ex = model.getFactory().createExclusion();
                ex.setGroupId(nodeArtif.getGroupId());
                ex.setArtifactId(nodeArtif.getArtifactId());
                dep.addExclusion(ex);
            }
        }
    }

    private void updateGraphAfterExclusion (ArtifactGraphNode node, Set<Artifact> exclTargets,
            Set<DependencyNode> exclParents) {
        boolean shouldValidate = false;

        Set<DependencyNode> toExclude = new HashSet<DependencyNode>();
        DependencyNode curDn;
        for (DependencyNode dn : node.getDuplicatesOrConflicts()) {
            if (dn.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
                curDn = dn.getParent();
                while (curDn != null) {
                    if (exclTargets.contains(curDn.getArtifact())) {
                        toExclude.add(dn);
                        break;
                    }
                    curDn = curDn.getParent();
                }
            }
        }
        List<ArtifactGraphEdge> edges2Exclude = new ArrayList<ArtifactGraphEdge>();
        Collection<ArtifactGraphEdge> incoming = findNodeEdges(node, false, true);
        ArtifactGraphNode sourceNode = null;
        boolean primaryExcluded = false;
        for (ArtifactGraphEdge age : incoming) {
            sourceNode = getEdgeSource(age);
            if (sourceNode != null) {
                for (DependencyNode dn : exclParents) {
                    if (sourceNode.getArtifact().equals(dn)) {
                        primaryExcluded = true;
                    }
                    if (sourceNode.represents(dn)) {
                        edges2Exclude.add(age);
                        break;
                    }
                }
            }
        }
        // note, must be called before node removing edges to work correctly
        node.getDuplicatesOrConflicts().removeAll(toExclude);
        for (ArtifactGraphEdge age : edges2Exclude) {
            removeEdge(age);
            age.getSource().removeChild(age.getTarget());
            shouldValidate = true;
        }
        incoming = findNodeEdges(node, false, true);
        if (primaryExcluded) {
            ArtifactVersion newVersion = findNewest(node, true);
            node.getArtifact().getArtifact().setVersion(newVersion.toString());
            for (ArtifactGraphEdge age : incoming) {
                EdgeWidget curEw = (EdgeWidget) findWidget(age);
                if (curEw != null) {
                    curEw.modelChanged();
                }
            }
        }
        if (incoming.isEmpty()) {
            removeSubGraph(node);
            shouldValidate = true;
        } else {
            node.getWidget().modelChanged();
        }

        if (shouldValidate) {
            validate();
        }
    }

    private void removeSubGraph (ArtifactGraphNode node) {
        if (!isNode(node)) {
            // already visited and removed
            return;
        }

        Collection<ArtifactGraphEdge> incoming = findNodeEdges(node, false, true);
        if (!incoming.isEmpty()) {
            return;
        }
        Collection<ArtifactGraphEdge> outgoing = findNodeEdges(node, true, false);

        List<ArtifactGraphNode> children = new ArrayList<ArtifactGraphNode>();

        DependencyNode dn = null;
        ArtifactGraphNode childNode = null;
        // remove edges to children
        for (ArtifactGraphEdge age : outgoing) {
            dn = age.getTarget();
            childNode = getGraphNodeRepresentant(dn);
            if (childNode == null) {
                continue;
            }
            children.add(childNode);
            removeEdge(age);
            age.getSource().removeChild(dn);
            childNode.getDuplicatesOrConflicts().remove(dn);
        }
        // recurse to children
        for (ArtifactGraphNode age : children) {
            removeSubGraph(age);
        }

        // remove itself finally
        removeNode(node);
    }

    private class ExcludeDepAction extends AbstractAction {
        private ArtifactGraphNode node;

        @Messages({
            "ACT_ExcludeDep=Exclude",
            "TIP_ExcludeDep=Adds dependency exclusion of this artifact to relevant direct dependencies"
        })
        ExcludeDepAction(ArtifactGraphNode node) {
            this.node = node;
            putValue(NAME, ACT_ExcludeDep());
            putValue(SHORT_DESCRIPTION, TIP_ExcludeDep());
        }

        @Override public void actionPerformed(ActionEvent e) {
            FixVersionConflictPanel.ExclusionTargets et =
                    new FixVersionConflictPanel.ExclusionTargets(node, findNewest(node, true));
            Set<Artifact> exclTargets = et.getAll();

            if (!model.startTransaction()) {
                return;
            }
            try {
                excludeDepFromModel(node, exclTargets);
            } finally {
                try {
                    model.endTransaction();
                } catch (IllegalStateException ex) {
                    StatusDisplayer.getDefault().setStatusText(
                            NbBundle.getMessage(DependencyGraphScene.class, "ERR_UpdateModel", Exceptions.findLocalizedMessage(ex)), 
                            StatusDisplayer.IMPORTANCE_ERROR_HIGHLIGHT);
                    return;
                }
            }

            HashSet<DependencyNode> conflictParents = new HashSet<DependencyNode>();
            for (Artifact artif : exclTargets) {
                conflictParents.addAll(et.getConflictParents(artif));
            }
            updateGraphAfterExclusion(node, exclTargets, conflictParents);

            // save changes
            RP.post(DependencyGraphScene.this);
        }

    }

    /** Saves fix changes to the pom file, posted to RequestProcessor */
    @Override public void run() {
        try {
            tc.saveChanges(model);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            //TODO error reporting on wrong model save
        }
    }

    private class FixVersionConflictAction extends AbstractAction {

        private final ArtifactGraphNode node;
        private final Artifact nodeArtif;

        FixVersionConflictAction(ArtifactGraphNode node) {
            this.node = node;
            this.nodeArtif = node.getArtifact().getArtifact();
            putValue(NAME, ACT_FixVersionConflict());
        }

        @Messages("TIT_FixConflict=Fix Version Conflict")
        @Override public void actionPerformed(ActionEvent e) {
            FixVersionConflictPanel fixPanel = new FixVersionConflictPanel(DependencyGraphScene.this, node);
            DialogDescriptor dd = new DialogDescriptor(fixPanel, TIT_FixConflict());
            //pnl.setStatusDisplayer(dd.createNotificationLineSupport());
            Object ret = DialogDisplayer.getDefault().notify(dd);
            if (ret == DialogDescriptor.OK_OPTION) {
                FixVersionConflictPanel.FixDescription res = fixPanel.getResult();
                fixDependency(res);
                updateGraph(res);
                // save changes
                RP.post(DependencyGraphScene.this);
            }
        }

        private void fixDependency (FixVersionConflictPanel.FixDescription fixContent) {
            if (!model.startTransaction()) {
                return;
            }
            if (model.getProject() == null) {
                LOG.warning("#238748 we got a graph, but the model turned invalid for some reason. if you have steps to reproduce, please reopen the issue.");
                return; //#238748 not clear under which circumstances we get a graph but invalid model
            }
            try {
                if (fixContent.isSet && fixContent.version2Set != null) {
                    org.netbeans.modules.maven.model.pom.Dependency dep =
                            ModelUtils.checkModelDependency(model,
                            nodeArtif.getGroupId(), nodeArtif.getArtifactId(), true);
                    dep.setVersion(fixContent.version2Set.toString());
                }

                if (fixContent.isExclude) {
                    excludeDepFromModel(node, fixContent.exclusionTargets);
                }
            } finally {
                model.endTransaction();
            }
        }

        private void updateGraph(FixDescription fixContent) {
            if (fixContent.isSet) {
                node.getArtifact().getArtifact().setVersion(fixContent.version2Set.toString());
                Collection<ArtifactGraphEdge> incoming = findNodeEdges(node, false, true);
                for (ArtifactGraphEdge age : incoming) {
                    EdgeWidget curEw = (EdgeWidget) findWidget(age);
                    if (curEw != null) {
                        curEw.modelChanged();
                    }
                }
                node.getWidget().modelChanged();

                // add edge representing direct dependency if not exist yet
                if (findEdgesBetween(rootNode, node).isEmpty()) {
                    ArtifactGraphEdge ed = new ArtifactGraphEdge(rootNode.getArtifact(), node.getArtifact());
                    ed.setLevel(1);
                    ed.setPrimaryPath(true);
                    addEdge(ed);
                    setEdgeTarget(ed, node);
                    setEdgeSource(ed, rootNode);

                    node.setPrimaryLevel(1);
                    node.setArtifactParent(rootNode.getArtifact());
                    rootNode.getArtifact().addChild(node.getArtifact());

                    validate();
                }
            }

            if (fixContent.isExclude) {
                updateGraphAfterExclusion(node, fixContent.exclusionTargets, fixContent.conflictParents);
            }
        }

    } // FixVersionConflictAction


    private static class HoverController implements TwoStateHoverProvider {

        @Override public void unsetHovering(Widget widget) {
            ArtifactWidget aw = findArtifactW(widget);
            if (widget != null) {
                aw.bulbUnhovered();
            }
        }

        @Override public void setHovering(Widget widget) {
            ArtifactWidget aw = findArtifactW(widget);
            if (aw != null) {
                aw.bulbHovered();
            }
        }

        private ArtifactWidget findArtifactW (Widget w) {
            while (w != null && !(w instanceof ArtifactWidget)) {
                w = w.getParentWidget();
            }
            return (ArtifactWidget)w;
        }

    }
    
   private class HierarchicalGraphLayoutAction extends AbstractAction {

       @Messages("ACT_Layout_HierarchicalGraphLayout=Hierarchical")
       HierarchicalGraphLayoutAction() {
           putValue(NAME, ACT_Layout_HierarchicalGraphLayout());
       }

       @Override public void actionPerformed(ActionEvent e) {
           final GraphLayout layout = GraphLayoutFactory.createHierarchicalGraphLayout(DependencyGraphScene.this, DependencyGraphScene.this.isAnimated(), false);
           layout.layoutGraph(DependencyGraphScene.this);
            fitToZoomAfterLayout();
        }
   };
   private class TreeGraphLayoutVerticalAction extends AbstractAction {

       @Messages("ACT_Layout_TreeGraphLayoutVertical=Vertical Tree")
       TreeGraphLayoutVerticalAction() {
           putValue(NAME, ACT_Layout_TreeGraphLayoutVertical());
       }

       @Override public void actionPerformed(ActionEvent e) {
           final GraphLayout layout = GraphLayoutFactory.createTreeGraphLayout(10, 10, 50, 50, true);
           GraphLayoutSupport.setTreeGraphLayoutRootNode(layout, DependencyGraphScene.this.rootNode);
           
           layout.layoutGraph(DependencyGraphScene.this);
           fitToZoomAfterLayout();
       }
   };


   private class TreeGraphLayoutHorizontalAction extends AbstractAction {

       @Messages("ACT_Layout_TreeGraphLayoutHorizontal=Horizontal Tree")
       TreeGraphLayoutHorizontalAction() {
           putValue(NAME, ACT_Layout_TreeGraphLayoutHorizontal());
       }

       @Override public void actionPerformed(ActionEvent e) {
           final GraphLayout layout = GraphLayoutFactory.createTreeGraphLayout(10, 10, 50, 50, false);
           GraphLayoutSupport.setTreeGraphLayoutRootNode(layout, DependencyGraphScene.this.rootNode);
           
           layout.layoutGraph(DependencyGraphScene.this);
           fitToZoomAfterLayout();
           
       }
   };
   private class FruchtermanReingoldLayoutAction extends AbstractAction {

       @Messages("ACT_Layout_FruchtermanReingoldLayout=Default Layout")
       FruchtermanReingoldLayoutAction() {
           putValue(NAME, ACT_Layout_FruchtermanReingoldLayout());
       }

       @Override public void actionPerformed(ActionEvent e) {
           //default layout in 7.3 
           layout.invokeLayout();
           fitToZoomAfterLayout();
       }
   };

    void fitToZoomAfterLayout() {
            DependencyGraphScene.this.getSceneAnimator().getPreferredLocationAnimator().addAnimatorListener(new AnimatorListener() {
                
                @Override
                public void animatorStarted(AnimatorEvent event) {
                    
                }
                
                @Override
                public void animatorReset(AnimatorEvent event) {
                    
                }
                
                @Override
                public void animatorFinished(AnimatorEvent event) {
                    DependencyGraphScene.this.getSceneAnimator().getPreferredLocationAnimator().removeAnimatorListener(this);
                    new SceneZoomToFitAction().actionPerformed(null);
                }
                
                @Override
                public void animatorPreTick(AnimatorEvent event) {
                    
                }
                
                @Override
                public void animatorPostTick(AnimatorEvent event) {
                    
                }
            });
    }
}
