/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.editor.lib2.view;

import javax.swing.text.AttributeSet;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.modules.editor.lib2.highlighting.CompoundAttributes;
import org.netbeans.modules.editor.lib2.highlighting.HighlightItem;
import org.netbeans.modules.editor.lib2.highlighting.HighlightsList;
import org.netbeans.spi.editor.highlighting.HighlightsSequence;

/**
 * Special highlights sequence used for painting of individual views.
 * <br/>
 * It merges together highlights contained in views (as attributes) together
 * with extra painting highlights (from highlighting layers that do not change metrics).
 * <br/>
 * It "covers" even non-highlighted areas by returning null from {@link #getAttributes()}.
 * <br/>
 * The instance can only be used by a single thread.
 *
 * @author mmetelka
 */
class ViewPaintHighlights implements HighlightsSequence {
    
    private static final HighlightItem[] EMPTY = new HighlightItem[0];

    /** All paint highlights in the area being painted. */
    private final HighlightsList paintHighlights;
    
    /** Current index in paint highlights. */
    private int phIndex;
    
    /** Current paint highlight (the one pointed by phIndex) start offset. */
    private int phStartOffset;
    
    /** Current paint highlight (the one pointed by phIndex) end offset. */
    private int phEndOffset;
    
    /** Current paint highlight (the one pointed by phIndex) attributes (or null). */
    private AttributeSet phAttrs;
    
    private int viewEndOffset;
    
    /** Items of view's compound attributes. */
    private HighlightItem[] cahItems;
    
    /** Index of current compoundAttrs highlight. It's -1 for regular attrs or no attrs. */
    private int cahIndex;
    
    /** End offset of current compoundAttrs highlight. */
    private int cahEndOffset;
    
    /** Attributes (or null) of current compoundAttrs highlight. */
    private AttributeSet cahAttrs;
    
    private int offsetDiff;
    
    /** Start offset of highlight currently provided by this highlights sequence. */
    private int hiStartOffset;
    
    /** End offset of highlight currently provided by this highlights sequence. */
    private int hiEndOffset;
    
    /** Attributes (or null) of highlight currently provided by this highlights sequence. */
    private AttributeSet hiAttrs;

    ViewPaintHighlights(HighlightsList paintHighlights) {
        this.paintHighlights = paintHighlights;
        updatePH(0);
    }
    
    /**
     * Reset paint highlights for the given view.
     *
     * @param view child view for which the highlights are obtained.
     * @param shift shift inside the view where the computed highlights should start.
     */
    void reset(EditorView view, int shift) {
        assert (shift >= 0) : "shift=" + shift + " < 0"; // NOI18N
        int viewStartOffset = view.getStartOffset();
        int viewLength = view.getLength();
        assert (shift < viewLength) : "shift=" + shift + " >= viewLength=" + viewLength; // NOI18N
        viewEndOffset = viewStartOffset + viewLength;
        AttributeSet attrs = view.getAttributes();
        int startOffset = viewStartOffset + shift;
        if (ViewUtils.isCompoundAttributes(attrs)) {
            CompoundAttributes cAttrs = (CompoundAttributes) attrs;
            offsetDiff = viewStartOffset - cAttrs.startOffset();
            cahItems = cAttrs.highlightItems();
            if (shift == 0) {
                cahIndex = 0;
            } else {
                int cahOffset = startOffset - offsetDiff; // Orig offset inside cAttrs
                cahIndex = findCAHIndex(cahOffset); // Search in orig.offsets
            }
            if (cahIndex >= cahItems.length) {
                throw new IllegalStateException("offsetDiff=" + offsetDiff + // NOI18N
                        ", view=" + view + ", shift=" + shift + ", viewStartOffset+shift=" + startOffset + // NOI18N
                        "\ncAttrs:\n" + cAttrs + // NOI18N
                        "\n" + this + "docView:\n" + // NOI18N
                        ((DocumentView) view.getParent().getParent()).toStringDetail());
            }
            HighlightItem cahItem = cahItems[cahIndex];
            cahEndOffset = cahItem.getEndOffset() + offsetDiff;
//            assert (startOffset < cahEndOffset) : "startOffset=" + startOffset + // NOI18N
//                    " >= cahEndOffset=" + cahEndOffset + "\n" + this; // NOI18N
            cahAttrs = cahItem.getAttributes();

        } else { // Either regular or no attrs
            // offsetDiff will not be used
            cahItems = EMPTY;
            cahIndex = -1;
            cahEndOffset = viewEndOffset;
            if (attrs == null) {
                cahAttrs = null;
            } else { // regular attrs
                cahAttrs = attrs;
            }
        }
        // Update paint highlight if necessary
        if (startOffset < phStartOffset) { // Must go back
            updatePH(findPHIndex(startOffset));
        } else if (startOffset >= phEndOffset) { // Must fetch further
            // Should be able to fetch since it should not fetch beyond requested area size
//            if (startOffset >= paintHighlights.endOffset()) {
//                throw new IllegalStateException("startOffset=" + startOffset + // NOI18N
//                        " >= paintHighlights.endOffset()=" + paintHighlights.endOffset() + // NOI18N
//                        "\n" + this + "docView:\n" + // NOI18N
//                        ((DocumentView)view.getParent().getParent()).toStringDetail());
//            }
            fetchNextPH();
            if (startOffset >= phEndOffset) {
                updatePH(findPHIndex(startOffset));
            }
        } // Within current PH
        hiStartOffset = hiEndOffset = startOffset;
    }
    
    @Override
    public boolean moveNext() {
        if (hiEndOffset >= viewEndOffset) {
            return false;
        }
        if (hiEndOffset >= phEndOffset) {
            fetchNextPH();
        }
        if (hiEndOffset >= cahEndOffset) {
            // Fetch next CAH
            cahIndex++;
            if (cahIndex >= cahItems.length) {
                return false;
            }
            HighlightItem hItem = cahItems[cahIndex];
            cahEndOffset = hItem.getEndOffset() + offsetDiff;
            cahAttrs = hItem.getAttributes();
        }
        // There will certainly be a next highlight
        hiStartOffset = hiEndOffset;
        // Decide whether paint highlight ends lower than compound attrs' one
        if (phEndOffset < cahEndOffset) {
            hiEndOffset = Math.min(phEndOffset, viewEndOffset);
        } else {
            hiEndOffset = cahEndOffset;
        }
        // Merge (possibly null) attrs (ph over cah)
        hiAttrs = cahAttrs;
        if (phAttrs != null) {
            hiAttrs = (hiAttrs != null) ? AttributesUtilities.createComposite(phAttrs, hiAttrs) : phAttrs;
        }
        return true;
    }

    @Override
    public int getStartOffset() {
        return hiStartOffset;
    }

    @Override
    public int getEndOffset() {
        return hiEndOffset;
    }

    @Override
    public AttributeSet getAttributes() {
        return hiAttrs;
    }

    /**
     * Find index of cAttrs' item "containing" the cahOffset.
     * @param cahOffset offset in original offset space of cAttrs.
     * @return index of hItem.
     */
    private int findCAHIndex(int cahOffset) {
        int low = 0;
        int high = cahItems.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1; // mid in the binary search
            int hEndOffset = cahItems[mid].getEndOffset();
            if (hEndOffset < cahOffset) {
                low = mid + 1;
            } else if (hEndOffset > cahOffset) {
                high = mid - 1;
            } else { // hEndOffset == offset
                low = mid + 1;
                break;
            }
        }
        return low;
    }

    private void updatePH(int index) {
        phIndex = index;
        phStartOffset = (phIndex > 0)
                ? paintHighlights.get(phIndex - 1).getEndOffset()
                : paintHighlights.startOffset();
        HighlightItem phItem = paintHighlights.get(phIndex);
        phEndOffset = phItem.getEndOffset();
        phAttrs = phItem.getAttributes();
    }

    private void fetchNextPH() {
        phIndex++;
        if (phIndex >= paintHighlights.size()) {
            throw new IllegalStateException("phIndex=" + phIndex + // NOI18N
                    " >= paintHighlights.size()=" + paintHighlights.size() + // NOI18N
                    "\n" + this); // NOI18N
        }
        phStartOffset = phEndOffset;
        HighlightItem hItem = paintHighlights.get(phIndex);
        phEndOffset = hItem.getEndOffset();
        phAttrs = hItem.getAttributes();
    }
    
    private int findPHIndex(int offset) {
        int low = 0;
        int high = paintHighlights.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1; // mid in the binary search
            int hEndOffset = paintHighlights.get(mid).getEndOffset();
            if (hEndOffset < offset) {
                low = mid + 1;
            } else if (hEndOffset > offset) {
                high = mid - 1;
            } else { // hEndOffset == offset
                low = mid + 1;
                break;
            }
        }
        return low;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("ViewPaintHighlights: ph[").append(phIndex). // NOI18N
                append("]<").append(phStartOffset). // NOI18N
                append(",").append(phEndOffset). // NOI18N
                append("> attrs=").append(phAttrs).append('\n');
        sb.append("cah[").append(cahIndex).append("]#").append(cahItems.length);
        sb.append(" <?,").append(cahEndOffset).append("> attrs=").append(cahAttrs);
        sb.append(", viewEndOffset=").append(viewEndOffset).
                append(", offsetDiff=").append(offsetDiff);
        sb.append("\npaintHighlights:").append(paintHighlights);
        return sb.toString();
    }
    
}
