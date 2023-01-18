/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.editor.lib2.view;

import java.awt.Shape;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import org.netbeans.api.annotations.common.NonNull;

/**
 * Locked view hierarchy as result of {@link ViewHierarchy#lock() }.
 * <br/>
 * Underlying document of the view hierarchy's text component must be
 * read-locked to guarantee stability of offsets passed to methods of this class.
 * <br/>
 * If editor view hierarchy is not installed into text component
 * (text component's root view is not an instance of DocumentView)
 * the methods return default values as described in their documentation.
 * 
 * @author Miloslav Metelka
 */
public final class LockedViewHierarchy {
    
    private ViewHierarchyImpl impl;
    
    private final DocumentView docView;
    
    LockedViewHierarchy(ViewHierarchyImpl impl) {
        assert (impl != null);
        this.impl = impl;
        docView = impl.getDocumentView();
        if (docView != null) {
            docView.lock();
        }
    }

    /**
     * Unlock this view hierarchy which causes all methods in it to no longer work.
     */
    public void unlock() {
        checkValid();
        if (docView != null) {
            docView.unlock();
        }
        impl = null;
    }

    /**
     * Get text component that this view hierarchy is associated with.
     * <br/>
     * 
     * @return non-null text component.
     */
    public @NonNull JTextComponent getTextComponent() {
        checkValid();
        return impl.textComponent();
    }
    
    /**
     * Get y coordinate of a visual row that corresponds to given offset.
     * <br/>
     * Underlying document of the view hierarchy's text component should be read-locked
     * to guarantee stability of passed offset.
     * <br/>
     * If editor view hierarchy is not installed into text component this method
     * delegates to {@link JTextComponent#modelToView(int) }.
     *
     * @param offset
     * @return y
     */
    public double modelToY(int offset) {
        checkValid();
        return impl.modelToY(docView, offset);
    }
    
    /**
     * Multi-offset variant of {@link #modelToY(int)} with improved efficiency for sorted offsets.
     *
     * @param offsets array of offsets to be translated to y coordinates. More efficiency
     *  is achieved if the offsets are sorted from lowest to highest (at least partially).
     * @return array of y-coordinates (with the same cardinality as offsets array).
     */
    public double[] modelToY(int[] offsets) {
        checkValid();
        return impl.modelToY(docView, offsets);
    }
    
    /**
     * Return visual mapping of character at offset.
     *
     * @param offset
     * @param bias
     * @return shape corresponding to given offset.
     */
    public Shape modelToView(int offset, Position.Bias bias) {
        checkValid();
        return impl.modelToView(docView, offset, bias);
    }

    /**
     * Map visual point to an offset.
     *
     * @param x
     * @param y
     * @param biasReturn single-item array or null to ignore return bias.
     * @return offset corresponding to given visual point.
     */
    public int viewToModel(double x, double y, Position.Bias[] biasReturn) {
        checkValid();
        return impl.viewToModel(docView, x, y, biasReturn);
    }
    
    /**
     * Get index of a paragraph view corresponding to given offset.
     *
     * @param offset
     * @return index of a paragraph view containing the given offset
     *  or -1 if the view hierarchy contains no paragraph views
     *  or when the view hierarchy is not active.
     */
    public int modelToParagraphViewIndex(int offset) {
        checkValid();
        return impl.modelToParagraphViewIndex(docView, offset);
    }
    
    /**
     * Map y to index of paragraph view.
     *
     * @param y
     * @return offset corresponding to given visual point.
     */
    public int yToParagraphViewIndex(double y) {
        checkValid();
        return impl.yToParagraphViewIndex(docView, y);
    }
    
    /**
     * Get descriptor of a paragraph views contained in view hierarchy
     * at the given index.
     *
     * @param paragraphViewIndex index obtained by {@link #modelToParagraphViewIndex(int)}
     *  or {@link #yToParagraphViewIndex(double)}.
     * @return descriptor of paragraph view or null if the view hierarchy is not active.
     */
    public ParagraphViewDescriptor getParagraphViewDescriptor(int paragraphViewIndex) {
        checkValid();
        return (impl.verifyParagraphViewIndexValid(docView, paragraphViewIndex))
                ? new ParagraphViewDescriptor(docView, paragraphViewIndex)
                : null;
    }

    /**
     * Get total count of paragraph views contained in view hierarchy.
     *
     * @return count of paragraph view contained in view hierarchy
     *  or -1 if the view hierarchy is not active.
     */
    public int getParagraphViewCount() {
        checkValid();
        return impl.getParagraphViewCount(docView);
    }

    /**
     * Get height of a visual row of text.
     * <br/>
     * For wrapped lines (containing multiple visual rows) this is height of a single visual row.
     * <br/>
     * Current editor view hierarchy implementation uses uniform row height for all the rows.
     * 
     * @return height of a visual row.
     */
    public float getDefaultRowHeight() {
        checkValid();
        return impl.getDefaultRowHeight(docView);
    }
    
    /**
     * Get width of a typical character of a default font used by view hierarchy.
     * <br/>
     * In case mixed fonts (non-monospaced) are used this gives a little value
     * but certain tools such as rectangular selection may use this value.
     */
    public float getDefaultCharWidth() {
        checkValid();
        return impl.getDefaultCharWidth(docView);
    }
    
    /**
     * Return true if the view hierarchy is actively managing its contained views.
     * <br/>
     * Infrastructure may turn the view hierarchy inactive in case there are many
     * edits performed in the document (mainly during code reformatting).
     * <br/>
     * Also the view hierarchy is not active when a document modification was performed
     * but the view hierarchy did not update itself accordingly yet (its DocumentListener
     * was not called yet).
     *
     * @return true if the editor view hierarchy is active or false otherwise
     *  (when Editor's view hierarchy is not installed in the component's TextUI this method returns false).
     */
    public boolean isActive() {
        checkValid();
        return impl.isActive(docView);
    }

    private void checkValid() {
        if (impl == null) {
            throw new IllegalStateException("Inactive LockedViewHierarchy: unlock() already called.");
        }
    }
}
