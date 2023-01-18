/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include the
 * License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by Oracle
 * in the GPL Version 2 section of the License file that accompanied this code.
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL or only
 * the GPL Version 2, indicate your decision by adding "[Contributor] elects to
 * include this software in this distribution under the [CDDL or GPL Version 2]
 * license." If you do not indicate a single choice of license, a recipient has
 * the option to distribute your version of this file under either the CDDL, the
 * GPL Version 2 or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 */
package org.netbeans.modules.form.layoutdesign;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.openide.filesystems.FileUtil;

public class ALT_SeqResizing09Test extends LayoutTestCase {

    private Object changeMark;

    public ALT_SeqResizing09Test(String name) {
        super(name);
        try {
            className = this.getClass().getName();
            className = className.substring(className.lastIndexOf('.') + 1, className.length());
            startingFormFile = FileUtil.toFileObject(new File(url.getFile() + goldenFilesPath + className + "-StartingForm.form").getCanonicalFile());
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    /**
     * Resize jButton2 to snap to top edge of the container (no gap).
     */
    public void doChanges0() {
        lm.setChangeRecording(true);
        changeMark = lm.getChangeMark();
        ld.externalSizeChangeHappened();
// > UPDATE CURRENT STATE
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        contInterior.put("Form", new Rectangle(0, 0, 400, 329));
        compBounds.put("jButton1", new Rectangle(10, 40, 73, 289));
        baselinePosition.put("jButton1-73-289", new Integer(148));
        compBounds.put("jButton2", new Rectangle(89, 40, 73, 23));
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compBounds.put("jButton3", new Rectangle(168, 40, 73, 23));
        baselinePosition.put("jButton3-73-23", new Integer(15));
        compBounds.put("jButton4", new Rectangle(247, 40, 73, 23));
        baselinePosition.put("jButton4-73-23", new Integer(15));
        compMinSize.put("Form", new Dimension(330, 74));
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        ld.updateCurrentState();
// < UPDATE CURRENT STATE
// > START RESIZING
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compPrefSize.put("jButton2", new Dimension(73, 23));
        {
            String[] compIds = new String[]{
                "jButton2"
            };
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(89, 40, 73, 23)
            };
            Point hotspot = new Point(116, 38);
            int[] resizeEdges = new int[]{
                -1,
                0
            };
            boolean inLayout = true;
            ld.startResizing(compIds, bounds, hotspot, resizeEdges, inLayout);
        }
// < START RESIZING
        prefPaddingInParent.put("Form-jButton2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
// > MOVE
        {
            Point p = new Point(123, -3);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(89, 0, 73, 63)
            };
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
// < MOVE
        prefPaddingInParent.put("Form-jButton2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
// > MOVE
        {
            Point p = new Point(123, -4);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(89, 0, 73, 63)
            };
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
// < MOVE
// > END MOVING
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        ld.endMoving(true);
// < END MOVING
        ld.externalSizeChangeHappened();
// > UPDATE CURRENT STATE
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        contInterior.put("Form", new Rectangle(0, 0, 400, 329));
        compBounds.put("jButton1", new Rectangle(10, 40, 73, 289));
        baselinePosition.put("jButton1-73-289", new Integer(148));
        compBounds.put("jButton2", new Rectangle(89, 0, 73, 63));
        baselinePosition.put("jButton2-73-63", new Integer(35));
        compBounds.put("jButton3", new Rectangle(168, 40, 73, 23));
        baselinePosition.put("jButton3-73-23", new Integer(15));
        compBounds.put("jButton4", new Rectangle(247, 40, 73, 23));
        baselinePosition.put("jButton4-73-23", new Integer(15));
        compMinSize.put("Form", new Dimension(330, 74));
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        contInterior.put("Form", new Rectangle(0, 0, 400, 329));
        compBounds.put("jButton1", new Rectangle(10, 40, 73, 289));
        baselinePosition.put("jButton1-73-289", new Integer(148));
        compBounds.put("jButton2", new Rectangle(89, 0, 73, 63));
        baselinePosition.put("jButton2-73-63", new Integer(35));
        compBounds.put("jButton3", new Rectangle(168, 40, 73, 23));
        baselinePosition.put("jButton3-73-23", new Integer(15));
        compBounds.put("jButton4", new Rectangle(247, 40, 73, 23));
        baselinePosition.put("jButton4-73-23", new Integer(15));
        compMinSize.put("Form", new Dimension(330, 74));
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        ld.updateCurrentState();
// < UPDATE CURRENT STATE
    }

    /**
     * Undo previous change. Then resize jButton2 to snap to container top edge
     * with default gap.
     */
    public void doChanges1() {
        lm.undo(changeMark, lm.getChangeMark());
// > UPDATE CURRENT STATE
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        contInterior.put("Form", new Rectangle(0, 0, 400, 329));
        compBounds.put("jButton1", new Rectangle(10, 40, 73, 289));
        baselinePosition.put("jButton1-73-289", new Integer(148));
        compBounds.put("jButton2", new Rectangle(89, 40, 73, 23));
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compBounds.put("jButton3", new Rectangle(168, 40, 73, 23));
        baselinePosition.put("jButton3-73-23", new Integer(15));
        compBounds.put("jButton4", new Rectangle(247, 40, 73, 23));
        baselinePosition.put("jButton4-73-23", new Integer(15));
        compMinSize.put("Form", new Dimension(330, 74));
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        ld.updateCurrentState();
// < UPDATE CURRENT STATE
// > START RESIZING
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compPrefSize.put("jButton2", new Dimension(73, 23));
        {
            String[] compIds = new String[]{
                "jButton2"
            };
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(89, 40, 73, 23)
            };
            Point hotspot = new Point(122, 39);
            int[] resizeEdges = new int[]{
                -1,
                0
            };
            boolean inLayout = true;
            ld.startResizing(compIds, bounds, hotspot, resizeEdges, inLayout);
        }
// < START RESIZING
        prefPaddingInParent.put("Form-jButton2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
// > MOVE
        {
            Point p = new Point(124, 11);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(89, 11, 73, 52)
            };
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
// < MOVE
        prefPaddingInParent.put("Form-jButton2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
// > MOVE
        {
            Point p = new Point(124, 10);
            String containerId = "Form";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(89, 11, 73, 52)
            };
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
// < MOVE
// > END MOVING
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        ld.endMoving(true);
// < END MOVING
        ld.externalSizeChangeHappened();
// > UPDATE CURRENT STATE
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        contInterior.put("Form", new Rectangle(0, 0, 400, 329));
        compBounds.put("jButton1", new Rectangle(10, 40, 73, 289));
        baselinePosition.put("jButton1-73-289", new Integer(148));
        compBounds.put("jButton2", new Rectangle(89, 11, 73, 52));
        baselinePosition.put("jButton2-73-52", new Integer(30));
        compBounds.put("jButton3", new Rectangle(168, 40, 73, 23));
        baselinePosition.put("jButton3-73-23", new Integer(15));
        compBounds.put("jButton4", new Rectangle(247, 40, 73, 23));
        baselinePosition.put("jButton4-73-23", new Integer(15));
        compMinSize.put("Form", new Dimension(330, 74));
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        contInterior.put("Form", new Rectangle(0, 0, 400, 329));
        compBounds.put("jButton1", new Rectangle(10, 40, 73, 289));
        baselinePosition.put("jButton1-73-289", new Integer(148));
        compBounds.put("jButton2", new Rectangle(89, 11, 73, 52));
        baselinePosition.put("jButton2-73-52", new Integer(30));
        compBounds.put("jButton3", new Rectangle(168, 40, 73, 23));
        baselinePosition.put("jButton3-73-23", new Integer(15));
        compBounds.put("jButton4", new Rectangle(247, 40, 73, 23));
        baselinePosition.put("jButton4-73-23", new Integer(15));
        compMinSize.put("Form", new Dimension(330, 74));
        compBounds.put("Form", new Rectangle(0, 0, 400, 329));
        prefPaddingInParent.put("Form-jButton4-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton1", new Dimension(73, 23));
        prefPaddingInParent.put("Form-jButton3-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jButton4-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        ld.updateCurrentState();
// < UPDATE CURRENT STATE
    }
}
