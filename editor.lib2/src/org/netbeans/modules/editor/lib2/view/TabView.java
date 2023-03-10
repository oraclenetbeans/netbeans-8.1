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

package org.netbeans.modules.editor.lib2.view;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
import javax.swing.text.Position;
import javax.swing.text.Position.Bias;
import javax.swing.text.TabExpander;
import javax.swing.text.TabableView;
import javax.swing.text.View;

/**
 * View of (possibly multiple) '\t' characters.
 * <br/>
 * It needs to be measured specially - it needs to get visually aligned to multiples
 * of TAB_SIZE char width.
 * <br/>
 * Note that the view does not hold a last tab expander itself by itself so if tab expander
 * changes the view does not call a preference change.
 * <br/>
 * Due to line wrap the view cannot base its tab-stop calculations upon alloc.getX().
 *
 *
 * @author Miloslav Metelka
 */

public final class TabView extends EditorView implements TabableView {

    // -J-Dorg.netbeans.modules.editor.lib2.view.TabView.level=FINE
    private static final Logger LOG = Logger.getLogger(TabView.class.getName());

    /** Offset of start offset of this view. */
    private int rawEndOffset; // 24-super + 4 = 28 bytes

    /** Number of subsequent '\t' characters. */
    private int length; // 28 + 4 = 32 bytes

    /** Attributes for rendering */
    private final AttributeSet attributes; // 36 + 4 = 40 bytes

    /** Width corresponding to first '\t' */
    private float firstTabWidth; // 40 + 4 = 44 bytes

    /** Total span of the view */
    private float width; // 44 + 4 = 48 bytes

    public TabView(int length, AttributeSet attributes) {
        super(null);
        assert (length > 0) : "Length == 0"; // NOI18N
        this.length = length;
        this.attributes = attributes;
    }

    @Override
    public float getPreferredSpan(int axis) {
        DocumentView docView = getDocumentView();
        return (axis == View.X_AXIS)
            ? width // Return last width computed by getTabbedSpan()
            : ((docView != null) ? docView.op.getDefaultRowHeight() : 0f);
    }

    @Override
    public float getTabbedSpan(float x, TabExpander e) {
        int offset = getStartOffset();
        int endOffset = offset + getLength();
        float tabX = e.nextTabStop(x, offset++);
        firstTabWidth = tabX - x;
        while (offset < endOffset) {
            tabX = e.nextTabStop(tabX, offset++);
        }
        width = tabX - x;
        return width;
    }

    @Override
    public float getPartialSpan(int p0, int p1) {
        // No non-tab areas
        return 0f;
    }

    @Override
    public int getRawEndOffset() {
        return rawEndOffset;
    }

    @Override
    public void setRawEndOffset(int rawOffset) {
        this.rawEndOffset = rawOffset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getStartOffset() {
        return getEndOffset() - getLength();
    }

    @Override
    public int getEndOffset() {
        EditorView.Parent parent = (EditorView.Parent) getParent();
        return (parent != null) ? parent.getViewEndOffset(rawEndOffset) : rawEndOffset;
    }

    @Override
    public AttributeSet getAttributes() {
        return attributes;
    }

    ParagraphView getParagraphView() {
        return (ParagraphView) getParent();
    }

    DocumentView getDocumentView() {
        ParagraphView paragraphView = getParagraphView();
        return (paragraphView != null) ? paragraphView.getDocumentView() : null;
    }

    @Override
    public Shape modelToViewChecked(int offset, Shape alloc, Position.Bias bias) {
        int startOffset = getStartOffset();
        Rectangle2D.Double mutableBounds = ViewUtils.shape2Bounds(alloc);
        int charIndex = offset - startOffset;
        if (charIndex == 1) {
            mutableBounds.x += firstTabWidth;
        } else if (charIndex > 1) {
            int extraTabCount = getLength() - 1;
            if (extraTabCount > 0) {
                mutableBounds.x += firstTabWidth + (charIndex - 1) * ((width - firstTabWidth) / extraTabCount);
            }
        }
        mutableBounds.width = 1;
        return mutableBounds;
    }

    @Override
    public int viewToModelChecked(double x, double y, Shape alloc, Bias[] biasReturn) {
        int offset = getStartOffset();
        Rectangle2D.Double mutableBounds = ViewUtils.shape2Bounds(alloc);
        // Compare to the middle of a tab width: to left it's a previous offset; to right it's a next offset
        double cmpX = mutableBounds.x + firstTabWidth / 2;
        if (x > cmpX) {
            int endOffset = offset + getLength();
            offset++;
            if (offset < endOffset) { // At least one extra '\t'
                float tabWidth = (width - firstTabWidth) / (endOffset - offset);
                cmpX += firstTabWidth / 2 + tabWidth / 2;
                while (x > cmpX && offset < endOffset) {
                    cmpX += tabWidth;
                    offset++;
                }
            }
        }
        return offset;
    }

    @Override
    public int getNextVisualPositionFromChecked(int offset, Bias bias, Shape alloc, int direction, Bias[] biasRet) {
        int startOffset = getStartOffset();
        int endOffset = startOffset + getLength();
        int retOffset = -1;
        switch (direction) {
            case EAST:
                biasRet[0] = Bias.Forward;
                if (offset == -1) {
                    retOffset = getStartOffset();
                } else {
                    retOffset = offset + 1;
                    if (retOffset >= endOffset) {
                        retOffset = endOffset;
                        biasRet[0] = Bias.Backward;
                    }
                }
                break;

            case WEST:
                biasRet[0] = Bias.Forward;
                if (offset == -1) {
                    retOffset = endOffset - 1;
                } else {
                    retOffset = offset - 1;
                    if (retOffset < startOffset) {
                        retOffset = -1;
                    }
                }
                break;

            case View.NORTH:
            case View.SOUTH:
                break; // Return -1
            default:
                throw new IllegalArgumentException("Bad direction: " + direction);
        }
        return retOffset;
    }

    @Override
    public void paint(Graphics2D g, Shape alloc, Rectangle clipBounds) {
        int viewStartOffset = getStartOffset();
        DocumentView docView = getDocumentView();
        // TODO render only necessary parts
        HighlightsViewUtils.paintHiglighted(g, alloc, clipBounds,
                docView, this, viewStartOffset,
                false, null, viewStartOffset, 0, getLength());
    }

    @Override
    public View breakView(int axis, int offset, float pos, float len) {
        return this; // Currently unbreakable
    }

    @Override
    public View createFragment(int p0, int p1) {
        ViewUtils.checkFragmentBounds(p0, p1, getStartOffset(), getLength());
        return this;
    }

    @Override
    protected String getDumpName() {
        return "TV";
    }

    @Override
    public String toString() {
        return appendViewInfo(new StringBuilder(200), 0, "", -1).toString();
    }

}
