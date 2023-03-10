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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import javax.swing.UIManager;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.netbeans.api.visual.anchor.AnchorShape;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.LevelOfDetailsWidget;
import org.netbeans.api.visual.widget.Widget;
import static org.netbeans.modules.maven.graph.Bundle.*;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author mkleint
 */
public class EdgeWidget extends ConnectionWidget {
    public static final int DISABLED = 0;
    public static final int GRAYED = 1;
    public static final int REGULAR = 2;
    public static final int HIGHLIGHTED = 3;
    public static final int HIGHLIGHTED_PRIMARY = 4;

    private static float[] hsbVals = new float[3];

    private ArtifactGraphEdge edge;
    private int state = REGULAR;
    private boolean isConflict;
    private int edgeConflictType;

    private LabelWidget conflictVersion;
    private Widget versionW;
    private Stroke origStroke;

    public EdgeWidget(DependencyGraphScene scene, ArtifactGraphEdge edge) {
        super(scene);
        this.edge = edge;
        origStroke = getStroke();
        setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED);
        isConflict = edge.getTarget().getState() == DependencyNode.OMITTED_FOR_CONFLICT;
        edgeConflictType = getConflictType();

        updateVersionW(isConflict);
    }

    private void updateVersionW (boolean isConflict) {
        DependencyGraphScene scene = (DependencyGraphScene)getScene();
        ArtifactGraphNode targetNode = scene.getGraphNodeRepresentant(edge.getTarget());
        if (targetNode == null) {
            return;
        }
        int includedConflictType = targetNode.getConflictType();

        if (versionW == null) {
            if (isConflict || includedConflictType != ArtifactGraphNode.NO_CONFLICT) {
                versionW = new LevelOfDetailsWidget(scene, 0.5, 0.7, Double.MAX_VALUE, Double.MAX_VALUE);
                conflictVersion = new LabelWidget(scene, edge.getTarget().getArtifact().getVersion());
                if (isConflict) {
                    Color c = getConflictColor(edgeConflictType);
                    if (c != null) {
                        conflictVersion.setForeground(c);
                    }
                }
                versionW.addChild(conflictVersion);
                addChild(versionW);
                setConstraint(versionW, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER_RIGHT, 0.5f);
            }
        } else {
            if (!isConflict && includedConflictType == ArtifactGraphNode.NO_CONFLICT) {
                if (versionW.getParentWidget() == this) {
                    removeChild(versionW);
                } // else already removed??
            }
        }
    }

    public void setState (int state) {
        this.state = state;
        updateAppearance(((DependencyGraphScene)getScene()).isAnimated());
    }

    /**
     * readable widgets are calculated  based on scene zoom factor when zoom factor changes, the readable scope should too
     */
    void updateReadableZoom() {
        if (state == HIGHLIGHTED || state == HIGHLIGHTED_PRIMARY) {
            updateAppearance(false);
        }
    }

    void modelChanged () {
        edgeConflictType = getConflictType();
        isConflict = edge.getTarget().getState() == DependencyNode.OMITTED_FOR_CONFLICT;
        // correction if some graph editing(fixing) was done
        if (isConflict && edgeConflictType == ArtifactGraphNode.NO_CONFLICT) {
            isConflict = false;
        }
        
        updateVersionW(isConflict);
        updateAppearance(((DependencyGraphScene)getScene()).isAnimated());
    }

    @Messages({
        "TIP_VersionConflict=Conflict with {0} version required by {1}",
        "TIP_VersionWarning=Warning, overridden by {0} version required by {1}",
        "TIP_Primary=Primary dependency path",
        "TIP_Secondary=Secondary dependency path"
    })
    @SuppressWarnings("fallthrough")
    @org.netbeans.api.annotations.common.SuppressWarnings(value = "SF_SWITCH_FALLTHROUGH")
    private void updateAppearance (boolean animated) {
        Color inactiveC = UIManager.getColor("textInactiveText");
        if (inactiveC == null) {
            inactiveC = Color.LIGHT_GRAY;
        }
        Color activeC = UIManager.getColor("textText");
        if (activeC == null) {
            activeC = Color.BLACK;
        }
        Color conflictC = getConflictColor(edgeConflictType);
        if (conflictC == null) {
            conflictC = activeC;
        }

        Stroke stroke = origStroke;
        Color c = activeC;
        switch (state) {
            case REGULAR:
                c = edge.isPrimary() ? middleColor(activeC, inactiveC) : isConflict ? conflictC : inactiveC;
                break;
            case GRAYED:
                c = isConflict ? deriveColor(conflictC, 0.7f) : inactiveC;
                break;
            case HIGHLIGHTED_PRIMARY:
                stroke = new BasicStroke(3);
                // without break!
            case HIGHLIGHTED:
                c = isConflict ? conflictC : activeC;
                break;
        }

        if (state != DISABLED) {
            StringBuilder sb = new StringBuilder("<html>");
            if (isConflict) {
                DependencyGraphScene grScene = (DependencyGraphScene)getScene();
                DependencyNode includedDepN = grScene.getGraphNodeRepresentant(
                        edge.getTarget()).getArtifact();
                if (includedDepN == null) {
                    return;
                }
                DependencyNode parent = includedDepN.getParent();
                String version = includedDepN.getArtifact().getVersion();
                String requester = parent != null ? parent.getArtifact().getArtifactId() : "???";
                String confText = edgeConflictType == ArtifactGraphNode.CONFLICT ? TIP_VersionConflict(version, requester) : TIP_VersionWarning(version, requester);
                conflictVersion.setToolTipText(confText);
                sb.append(confText);
                sb.append("<br>");
            }
            sb.append("<i>");
            if (edge.isPrimary()) {
                sb.append(TIP_Primary());
            } else {
                sb.append(TIP_Secondary());
            }
            sb.append("</i>");
            sb.append("</html>");
            setToolTipText(sb.toString());
        } else {
            setToolTipText(null);
        }

        if (conflictVersion != null) {
            conflictVersion.setForeground(c);
            Font origF = getScene().getDefaultFont();
            conflictVersion.setFont(state == HIGHLIGHTED || state == HIGHLIGHTED_PRIMARY
                    ? ArtifactWidget.getReadable(getScene(), origF) : origF);
        }

        setVisible(state != DISABLED);

        setStroke(stroke);

        DependencyGraphScene grScene = (DependencyGraphScene)getScene();
        if (animated) {
            grScene.getSceneAnimator().animateForegroundColor(this, c);
        } else {
            setForeground(c);
        }
    }

    private int getConflictType () {
        ArtifactGraphNode included = ((DependencyGraphScene)getScene()).getGraphNodeRepresentant(edge.getTarget());
        if (included == null) {
            return ArtifactGraphNode.NO_CONFLICT;
        }
        DefaultArtifactVersion edgeV = new DefaultArtifactVersion(edge.getTarget().getArtifact().getVersion());
        DefaultArtifactVersion includedV = new DefaultArtifactVersion(included.getArtifact().getArtifact().getVersion());
        int ret = edgeV.compareTo(includedV);
        return ret > 0 ? ArtifactGraphNode.CONFLICT : ret < 0 ?
            ArtifactGraphNode.POTENTIAL_CONFLICT : ArtifactGraphNode.NO_CONFLICT;
    }

    private static Color getConflictColor (int conflictType) {
        return conflictType == ArtifactGraphNode.CONFLICT ? ArtifactWidget.CONFLICT
                : conflictType == ArtifactGraphNode.POTENTIAL_CONFLICT
                ? ArtifactWidget.WARNING : null;
    }

    /** Derives color from specified with saturation multiplied by given ratio.
     */
    static Color deriveColor (Color c, float saturationR) {
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbVals);
        hsbVals[1] = Math.min(1.0f, hsbVals[1] * saturationR);
        return Color.getHSBColor(hsbVals[0], hsbVals[1], hsbVals[2]);
    }

    private static Color middleColor (Color c1, Color c2) {
        return new Color(
                (c1.getRed() + c2.getRed()) / 2,
                (c1.getGreen() + c2.getGreen()) / 2,
                (c1.getBlue() + c2.getBlue()) / 2);
    }

}
