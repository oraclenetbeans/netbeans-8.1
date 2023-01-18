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

public class ALT_Bug203563Test extends LayoutTestCase {

    public ALT_Bug203563Test(String name) {
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
     * Resize jScrollPane2 (with jTextArea1) down to bottom-align with the
     * tabbed pane.
     */
    public void doChanges0() {
        ld.externalSizeChangeHappened();
// > UPDATE CURRENT STATE
        compBounds.put("jPanel1", new Rectangle(25, 50, 137, 220));
        contInterior.put("jPanel1", new Rectangle(25, 50, 137, 220));
        compMinSize.put("jPanel1", new Dimension(0, 0));
        compBounds.put("jPanel1", new Rectangle(25, 50, 137, 220));
        compBounds.put("Form", new Rectangle(0, 0, 522, 339));
        contInterior.put("Form", new Rectangle(0, 0, 522, 339));
        compBounds.put("jPanel2", new Rectangle(10, 11, 502, 317));
        baselinePosition.put("jPanel2-502-317", new Integer(0));
        contInterior.put("jPanel2", new Rectangle(13, 14, 504, 311));
        compBounds.put("jLabel1", new Rectangle(23, 300, 484, 14));
        baselinePosition.put("jLabel1-484-14", new Integer(11));
        compBounds.put("jTabbedPane1", new Rectangle(23, 25, 142, 248));
        baselinePosition.put("jTabbedPane1-142-248", new Integer(0));
        compBounds.put("jScrollPane2", new Rectangle(284, 25, 195, 153));
        baselinePosition.put("jScrollPane2-195-153", new Integer(0));
        compBounds.put("jButton2", new Rectangle(195, 191, 73, 23));
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compBounds.put("jButton1", new Rectangle(169, 232, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compMinSize.put("jPanel2", new Dimension(510, 317));
        compBounds.put("jPanel2", new Rectangle(10, 11, 502, 317));
        compPrefSize.put("jPanel2", new Dimension(510, 317));
        prefPaddingInParent.put("jPanel2-jLabel1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        hasExplicitPrefSize.put("jPanel2", new Boolean(false));
        prefPaddingInParent.put("jPanel2-jLabel1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("jPanel2-jTabbedPane1-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("jPanel2-jScrollPane2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton2", new Dimension(73, 23));
        prefPadding.put("jButton2-jButton1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        compPrefSize.put("jButton1", new Dimension(73, 23));
        compPrefSize.put("jLabel1", new Dimension(34, 14));
        prefPaddingInParent.put("jPanel2-jLabel1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        hasExplicitPrefSize.put("jPanel2", new Boolean(false));
        compMinSize.put("Form", new Dimension(522, 339));
        compBounds.put("Form", new Rectangle(0, 0, 522, 339));
        prefPaddingInParent.put("Form-jPanel2-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jPanel2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jPanel2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jPanel2", new Dimension(510, 317));
        prefPaddingInParent.put("Form-jPanel2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compBounds.put("jPanel3", new Rectangle(25, 50, 137, 220));
        contInterior.put("jPanel3", new Rectangle(25, 50, 137, 220));
        compMinSize.put("jPanel3", new Dimension(0, 0));
        compBounds.put("jPanel3", new Rectangle(25, 50, 137, 220));
        ld.updateCurrentState();
// < UPDATE CURRENT STATE
// > START RESIZING
        baselinePosition.put("jScrollPane2-195-153", new Integer(0));
        compPrefSize.put("jScrollPane2", new Dimension(166, 96));
        {
            String[] compIds = new String[]{
                "jScrollPane2"
            };
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(284, 25, 195, 153)
            };
            Point hotspot = new Point(374, 176);
            int[] resizeEdges = new int[]{
                -1,
                1
            };
            boolean inLayout = true;
            ld.startResizing(compIds, bounds, hotspot, resizeEdges, inLayout);
        }
// < START RESIZING
        prefPadding.put("jScrollPane2-jLabel1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jScrollPane2-jLabel1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jScrollPane2-jLabel1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jScrollPane2-jLabel1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
// > MOVE
        {
            Point p = new Point(370, 266);
            String containerId = "jPanel2";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(284, 25, 195, 248)
            };
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
// < MOVE
        prefPadding.put("jScrollPane2-jLabel1-1-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jScrollPane2-jLabel1-1-0-1", new Integer(11)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jScrollPane2-jLabel1-1-0-2", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jScrollPane2-jLabel1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
// > MOVE
        {
            Point p = new Point(370, 267);
            String containerId = "jPanel2";
            boolean autoPositioning = true;
            boolean lockDimension = false;
            Rectangle[] bounds = new Rectangle[]{
                new Rectangle(284, 25, 195, 248)
            };
            ld.move(p, containerId, autoPositioning, lockDimension, bounds);
        }
// < MOVE
// > END MOVING
        prefPaddingInParent.put("jPanel2-jLabel1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("jPanel2-jLabel1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compMinSize.put("jTabbedPane1", new Dimension(38, 44));
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("jPanel2-jScrollPane2-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jButton2-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("jPanel2-jScrollPane2-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jButton1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("jPanel2-jScrollPane2-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("jPanel2-jScrollPane2-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        ld.endMoving(true);
// < END MOVING
        ld.externalSizeChangeHappened();
// > UPDATE CURRENT STATE
        compBounds.put("jPanel1", new Rectangle(25, 50, 137, 220));
        contInterior.put("jPanel1", new Rectangle(25, 50, 137, 220));
        compMinSize.put("jPanel1", new Dimension(0, 0));
        compBounds.put("jPanel1", new Rectangle(25, 50, 137, 220));
        compBounds.put("Form", new Rectangle(0, 0, 522, 339));
        contInterior.put("Form", new Rectangle(0, 0, 522, 339));
        compBounds.put("jPanel2", new Rectangle(10, 11, 502, 317));
        baselinePosition.put("jPanel2-502-317", new Integer(0));
        contInterior.put("jPanel2", new Rectangle(13, 14, 504, 311));
        compBounds.put("jLabel1", new Rectangle(23, 300, 484, 14));
        baselinePosition.put("jLabel1-484-14", new Integer(11));
        compBounds.put("jTabbedPane1", new Rectangle(23, 25, 142, 248));
        baselinePosition.put("jTabbedPane1-142-248", new Integer(0));
        compBounds.put("jScrollPane2", new Rectangle(284, 25, 195, 248));
        baselinePosition.put("jScrollPane2-195-248", new Integer(0));
        compBounds.put("jButton2", new Rectangle(195, 191, 73, 23));
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compBounds.put("jButton1", new Rectangle(169, 232, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compMinSize.put("jPanel2", new Dimension(510, 317));
        compBounds.put("jPanel2", new Rectangle(10, 11, 502, 317));
        compPrefSize.put("jPanel2", new Dimension(510, 317));
        prefPaddingInParent.put("jPanel2-jLabel1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        hasExplicitPrefSize.put("jPanel2", new Boolean(false));
        prefPaddingInParent.put("jPanel2-jLabel1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("jPanel2-jTabbedPane1-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("jPanel2-jScrollPane2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jButton2", new Dimension(73, 23));
        prefPadding.put("jButton2-jButton1-1-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        compPrefSize.put("jButton1", new Dimension(73, 23));
        compPrefSize.put("jLabel1", new Dimension(34, 14));
        prefPaddingInParent.put("jPanel2-jLabel1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jTabbedPane1", new Dimension(142, 248));
        compPrefSize.put("jScrollPane2", new Dimension(166, 96));
        hasExplicitPrefSize.put("jPanel2", new Boolean(false));
        compMinSize.put("Form", new Dimension(522, 339));
        compBounds.put("Form", new Rectangle(0, 0, 522, 339));
        prefPaddingInParent.put("Form-jPanel2-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jPanel2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jPanel2-1-0", new Integer(11)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jPanel2", new Dimension(510, 317));
        prefPaddingInParent.put("Form-jPanel2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compBounds.put("jPanel3", new Rectangle(25, 50, 137, 220));
        contInterior.put("jPanel3", new Rectangle(25, 50, 137, 220));
        compMinSize.put("jPanel3", new Dimension(0, 0));
        compBounds.put("jPanel3", new Rectangle(25, 50, 137, 220));
        compBounds.put("jPanel1", new Rectangle(25, 50, 137, 220));
        contInterior.put("jPanel1", new Rectangle(25, 50, 137, 220));
        compMinSize.put("jPanel1", new Dimension(0, 0));
        compBounds.put("jPanel1", new Rectangle(25, 50, 137, 220));
        compBounds.put("Form", new Rectangle(0, 0, 522, 339));
        contInterior.put("Form", new Rectangle(0, 0, 522, 339));
        compBounds.put("jPanel2", new Rectangle(10, 11, 502, 317));
        baselinePosition.put("jPanel2-502-317", new Integer(0));
        contInterior.put("jPanel2", new Rectangle(13, 14, 504, 311));
        compBounds.put("jLabel1", new Rectangle(23, 300, 484, 14));
        baselinePosition.put("jLabel1-484-14", new Integer(11));
        compBounds.put("jTabbedPane1", new Rectangle(23, 25, 142, 248));
        baselinePosition.put("jTabbedPane1-142-248", new Integer(0));
        compBounds.put("jScrollPane2", new Rectangle(284, 25, 195, 248));
        baselinePosition.put("jScrollPane2-195-248", new Integer(0));
        compBounds.put("jButton2", new Rectangle(195, 191, 73, 23));
        baselinePosition.put("jButton2-73-23", new Integer(15));
        compBounds.put("jButton1", new Rectangle(169, 232, 73, 23));
        baselinePosition.put("jButton1-73-23", new Integer(15));
        compMinSize.put("jPanel2", new Dimension(510, 317));
        compBounds.put("jPanel2", new Rectangle(10, 11, 502, 317));
        prefPaddingInParent.put("jPanel2-jLabel1-0-1", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jTabbedPane1-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton2-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-0", new Integer(6)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-1", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-2", new Integer(10)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPadding.put("jButton1-jScrollPane2-0-0-3", new Integer(18)); // comp1Id-comp2Id-dimension-comp2Alignment-paddingType
        prefPaddingInParent.put("jPanel2-jLabel1-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compPrefSize.put("jTabbedPane1", new Dimension(142, 248));
        compPrefSize.put("jScrollPane2", new Dimension(166, 96));
        compMinSize.put("Form", new Dimension(522, 339));
        compBounds.put("Form", new Rectangle(0, 0, 522, 339));
        prefPaddingInParent.put("Form-jPanel2-0-0", new Integer(10)); // parentId-compId-dimension-compAlignment
        prefPaddingInParent.put("Form-jPanel2-1-1", new Integer(11)); // parentId-compId-dimension-compAlignment
        compBounds.put("jPanel3", new Rectangle(25, 50, 137, 220));
        contInterior.put("jPanel3", new Rectangle(25, 50, 137, 220));
        compMinSize.put("jPanel3", new Dimension(0, 0));
        compBounds.put("jPanel3", new Rectangle(25, 50, 137, 220));
        ld.updateCurrentState();
// < UPDATE CURRENT STATE
    }
}
