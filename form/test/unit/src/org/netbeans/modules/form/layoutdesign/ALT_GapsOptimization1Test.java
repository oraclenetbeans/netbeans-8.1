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

package org.netbeans.modules.form.layoutdesign;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.openide.filesystems.FileUtil;

/*
 * Test gap optimization (joining) in a group with a component spanning whole
 * group (i.e. with no gap).
 */
public class ALT_GapsOptimization1Test extends LayoutTestCase {

    public ALT_GapsOptimization1Test(String name) {
        super(name);
        try {
	    className = this.getClass().getName();
	    className = className.substring(className.lastIndexOf('.') + 1, className.length());
            startingFormFile = FileUtil.toFileObject(new File(url.getFile() + goldenFilesPath + className + "-StartingForm.form").getCanonicalFile());
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    /*
     * Add button under the textfield, with a default gap from container's left
     * border. Then add a toggle button just below it.
     */
    public void doChanges0() {
        ld.externalSizeChangeHappened();
        // > UPDATE CURRENT STATE
        contInterior.put("Form", new Rectangle(0, 0, 400, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 400, 20));
        baselinePosition.put("jTextField1-400-20", new Integer(14));
        compPrefSize.put("jTextField1", new Dimension(59, 20));
        contInterior.put("Form", new Rectangle(0, 0, 400, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 400, 20));
        baselinePosition.put("jTextField1-400-20", new Integer(14));
        ld.updateCurrentState();
        // < UPDATE CURRENT STATE
        lc = new LayoutComponent("jButton1", false);
        // > START ADDING
        baselinePosition.put("jButton1-73-23", new Integer(15));
        {
            LayoutComponent[] comps = new LayoutComponent[]{lc};
            Rectangle[] bounds = new Rectangle[]{new Rectangle(0, 0, 73, 23)};
            String defaultContId = null;
            Point hotspot = new Point(32, 11);
            ld.startAdding(comps, bounds, hotspot, defaultContId);
        }
        // < START ADDING
        prefPaddingInParent.put("Form-jButton1-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTextField1-jButton1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTextField1-jButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTextField1-jButton1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTextField1-jButton1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("Form-jButton1-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTextField1-jButton1-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        // > MOVE
        // > MOVE
        // > MOVE
        {
            Point p = new Point(38, 41);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{new Rectangle(10, 31, 73, 23)};
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
        // < MOVE
        prefPaddingInParent.put("Form-jButton1-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTextField1-jButton1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTextField1-jButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTextField1-jButton1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTextField1-jButton1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jTextField1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("Form-jButton1-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTextField1-jButton1-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        // > MOVE
        // > MOVE
        // > MOVE
        {
            Point p = new Point(38, 42);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{new Rectangle(10, 31, 73, 23)};
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
        // < MOVE
        // > END MOVING
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton1-1-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        // parentId-compId-dimension-compAlignment
        ld.endMoving(true);
        // < END MOVING
        ld.externalSizeChangeHappened();
        // > UPDATE CURRENT STATE
        contInterior.put("Form", new Rectangle(0, 0, 400, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 400, 20));
        baselinePosition.put("jTextField1-400-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compPrefSize.put("jTextField1", new Dimension(59, 20));
        compPrefSize.put("jButton1", new Dimension(73, 23));
        contInterior.put("Form", new Rectangle(0, 0, 400, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 400, 20));
        baselinePosition.put("jTextField1-400-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        ld.updateCurrentState();
        // < UPDATE CURRENT STATE
        lc = new LayoutComponent("jToggleButton1", false);
        // > START ADDING
        baselinePosition.put("jToggleButton1-105-23", new Integer(15));
        {
            LayoutComponent[] comps = new LayoutComponent[]{lc};
            Rectangle[] bounds = new Rectangle[]{new Rectangle(0, 0, 105, 23)};
            String defaultContId = null;
            Point hotspot = new Point(48, 11);
            ld.startAdding(comps, bounds, hotspot, defaultContId);
        }
        // < START ADDING
        prefPaddingInParent.put("Form-jToggleButton1-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTextField1-jToggleButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("Form-jToggleButton1-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jButton1-jToggleButton1-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        // > MOVE
        // > MOVE
        // > MOVE
        {
            Point p = new Point(59, 67);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{new Rectangle(10, 60, 105, 23)};
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
        // < MOVE
        prefPaddingInParent.put("Form-jToggleButton1-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTextField1-jToggleButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jTextField1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jToggleButton1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jToggleButton1-jButton1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("Form-jToggleButton1-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jButton1-jToggleButton1-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        // > MOVE
        // > MOVE
        // > MOVE
        {
            Point p = new Point(58, 67);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{new Rectangle(10, 60, 105, 23)};
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
        // < MOVE
        // > END MOVING
        compPrefSize.put("jToggleButton1", new Dimension(105, 23));
        prefPaddingInParent.put("Form-jToggleButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-1-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        // parentId-compId-dimension-compAlignment
        ld.endMoving(true);
        // < END MOVING
        ld.externalSizeChangeHappened();
        // > UPDATE CURRENT STATE
        contInterior.put("Form", new Rectangle(0, 0, 400, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 400, 20));
        baselinePosition.put("jTextField1-400-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compBounds.put("jToggleButton1", new Rectangle(10, 60, 105, 23));
        baselinePosition.put("jToggleButton1-105-23", new Integer(15));
        compPrefSize.put("jTextField1", new Dimension(59, 20));
        compPrefSize.put("jButton1", new Dimension(73, 23));
        compPrefSize.put("jToggleButton1", new Dimension(105, 23));
        contInterior.put("Form", new Rectangle(0, 0, 400, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 400, 20));
        baselinePosition.put("jTextField1-400-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compBounds.put("jToggleButton1", new Rectangle(10, 60, 105, 23));
        baselinePosition.put("jToggleButton1-105-23", new Integer(15));
        ld.updateCurrentState();
        // < UPDATE CURRENT STATE
    }

    /*
     * Resize (shrink) whole container horizontally so it snaps on minimum size
     * with a default gap on the right of the toggle button. As a result only
     * the size of the texfield is changed in the model.
     */
    public void doChanges1() {
        // > START RESIZING
        baselinePosition.put("Form-400-300", new Integer(-1));
        compMinSize.put("Form", new Dimension(125, 94));
        prefPaddingInParent.put("Form-jButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        // parentId-compId-dimension-compAlignment
        // parentId-compId-dimension-compAlignment
        {
            String[] compIds = new String[]{"Form"};
            Rectangle[] bounds = new Rectangle[]{new Rectangle(0, 0, 400, 300)};
            Point hotspot = new Point(403, 138);
            int[] resizeEdges = new int[]{1, -1};
            boolean inLayout = false;
            ld.startResizing(compIds, bounds, hotspot, resizeEdges, inLayout);
        }
        // < START RESIZING
        // > MOVE
        // < START RESIZING
        // > MOVE
        {
            Point p = new Point(125, 143);
            String containerId = null;
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{new Rectangle(0, 0, 125, 300)};
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
        // < MOVE
        // > MOVE
        // < MOVE
        // > MOVE
        {
            Point p = new Point(126, 143);
            String containerId = null;
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{new Rectangle(0, 0, 125, 300)};
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
        // < MOVE
        // > END MOVING
        contInterior.put("Form", new Rectangle(0, 0, 125, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 125, 20));
        baselinePosition.put("jTextField1-125-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compBounds.put("jToggleButton1", new Rectangle(10, 60, 105, 23));
        baselinePosition.put("jToggleButton1-105-23", new Integer(15));
        prefPaddingInParent.put("Form-jButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jTextField1", new Dimension(59, 20));
        compPrefSize.put("jButton1", new Dimension(73, 23));
        compPrefSize.put("jToggleButton1", new Dimension(105, 23));
        ld.endMoving(true);
        // < END MOVING
        ld.externalSizeChangeHappened();
        // > UPDATE CURRENT STATE
        contInterior.put("Form", new Rectangle(0, 0, 125, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 125, 20));
        baselinePosition.put("jTextField1-125-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compBounds.put("jToggleButton1", new Rectangle(10, 60, 105, 23));
        baselinePosition.put("jToggleButton1-105-23", new Integer(15));
        prefPaddingInParent.put("Form-jButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jToggleButton1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jTextField1", new Dimension(59, 20));
        compPrefSize.put("jButton1", new Dimension(73, 23));
        compPrefSize.put("jToggleButton1", new Dimension(105, 23));
        contInterior.put("Form", new Rectangle(0, 0, 125, 300));
        compBounds.put("jTextField1", new Rectangle(0, 0, 125, 20));
        baselinePosition.put("jTextField1-125-20", new Integer(14));
        compBounds.put("jButton1", new Rectangle(10, 31, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compBounds.put("jToggleButton1", new Rectangle(10, 60, 105, 23));
        baselinePosition.put("jToggleButton1-105-23", new Integer(15));
        ld.updateCurrentState();
        // < UPDATE CURRENT STATE
    }

}
