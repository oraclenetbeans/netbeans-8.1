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

import java.awt.Point;
import java.awt.Shape;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.editor.util.ListenerList;
import org.openide.util.Exceptions;


/**
 * View hierarchy implementation associated with a particular text component.
 * 
 * @author Miloslav Metelka
 */

@SuppressWarnings("ClassWithMultipleLoggers") //NOI18N
public final class ViewHierarchyImpl {
    
    /**
     * Logger for core operations of the view hierarchy - resolving modelToView() and viewToModel() etc.
     * <br/>
     * FINE logs basic info about view hierarchy operations performed.
     * <br/>
     * FINER also reports query during document's modification (when the view hierarchy cannot respond
     *  appropriately to queries since it would give incorrect results).
     */
    static final Logger OP_LOG = Logger.getLogger("org.netbeans.editor.view.op"); // -J-Dorg.netbeans.editor.view.op.level=FINE
    
    /**
     * Logger tracking all view factory changes that cause either rebuild of the views
     * or offset repaints.
     * <br/>
     * FINE reports which factory reported a change and an offset range of that change.
     * <br/>
     * FINER reports additional detailed information about the change.
     * <br/>
     * FINEST reports stacktrace where a particular span change request originated.
     */
    static final Logger CHANGE_LOG = Logger.getLogger("org.netbeans.editor.view.change"); // -J-Dorg.netbeans.editor.view.change.level=FINE
    
    /**
     * Logger tracking all view rebuilds in the view hierarchy.
     */
    static final Logger BUILD_LOG = Logger.getLogger("org.netbeans.editor.view.build"); // -J-Dorg.netbeans.editor.view.build.level=FINE
    
    /**
     * Logger for paint operations (may generate lots of output).
     */
    static final Logger PAINT_LOG = Logger.getLogger("org.netbeans.editor.view.paint"); // -J-Dorg.netbeans.editor.view.paint.level=FINE
    
    /**
     * Logger for span change requests on the views and underlying text component.
     * <br/>
     * FINE reports span change descriptions
     * <br/>
     * FINEST reports stacktrace where a particular span change request originated.
     */
    static final Logger SPAN_LOG = Logger.getLogger("org.netbeans.editor.view.span"); // -J-Dorg.netbeans.editor.view.span.level=FINE
    
    /**
     * Logger for repaint requests of the underlying text component.
     * <br/>
     * FINE reports repaint request's coordinates
     * <br/>
     * FINEST reports stacktrace where a particular repaint request originated.
     */
    static final Logger REPAINT_LOG = Logger.getLogger("org.netbeans.editor.view.repaint"); // -J-Dorg.netbeans.editor.view.repaint.level=FINE
    
    /**
     * Logger for extra consistency checks inside view hierarchy (may slow down processing).
     * <br/>
     */
    static final Logger CHECK_LOG = Logger.getLogger("org.netbeans.editor.view.check"); // -J-Dorg.netbeans.editor.view.check.level=FINE
    
    /**
     * Logger related to any settings being used in view hierarchy.
     * <br/>
     */
    static final Logger SETTINGS_LOG = Logger.getLogger("org.netbeans.editor.view.settings"); // -J-Dorg.netbeans.editor.view.settings.level=FINE
    
    /**
     * Logger related to view hierarchy events generation.
     * <br/>
     */
    static final Logger EVENT_LOG = Logger.getLogger("org.netbeans.editor.view.event"); // -J-Dorg.netbeans.editor.view.event.level=FINE
    
    /**
     * Logger for tracking view hierarchy locking.
     * <br/>
     * FINER stores the stack of the lock thread of view hierarchy in lockStack variable
     * (it should help to find missing unlock).
     * <br/>
     * FINEST in addition it dumps thread dump of each locker of a view hierarchy. 
     */
    // -J-Dorg.netbeans.modules.editor.lib2.view.ViewHierarchyImpl.level=FINER
    static final Logger LOG = Logger.getLogger(ViewHierarchyImpl.class.getName());

    private final JTextComponent textComponent;
    
    private final ViewHierarchy viewHierarchy;
    
    private final ListenerList<ViewHierarchyListener> listenerList;
    
    /**
     * Lockable document view.
     */
    private DocumentView currentDocView;
    
    public static synchronized ViewHierarchyImpl get(JTextComponent component) {
        ViewHierarchyImpl ViewHierarchyImpl = (ViewHierarchyImpl) component.getClientProperty(ViewHierarchyImpl.class);
        if (ViewHierarchyImpl == null) {
            ViewHierarchyImpl = new ViewHierarchyImpl(component);
            component.putClientProperty(ViewHierarchyImpl.class, ViewHierarchyImpl);
        }
        return ViewHierarchyImpl;
    }
    
    private ViewHierarchyImpl(JTextComponent textComponent) {
        this.textComponent = textComponent;
        this.listenerList = new ListenerList<ViewHierarchyListener>();
        this.viewHierarchy = ViewApiPackageAccessor.get().createViewHierarchy(this);
    }
    
    public @NonNull JTextComponent textComponent() {
        return textComponent;
    }
    
    public @NonNull ViewHierarchy viewHierarchy() {
        return viewHierarchy;
    }

    /**
     * Set the docView that is lockable (its setParent() was called and so its mutex is set).
     *
     * @param docView 
     */
    public synchronized void setDocumentView(DocumentView docView) {
        this.currentDocView = docView;
    }

    /**
     * Get document view in synchronized section so that a possible setDocumentView() gets
     * picked by this method.
     *
     * @return current document view.
     */
    public synchronized DocumentView getDocumentView() {
        return currentDocView;
    }
    
    public LockedViewHierarchy lock() {
        LockedViewHierarchy lvh = ViewApiPackageAccessor.get().createLockedViewHierarchy(this);
        return lvh;
    }

    public double modelToY(DocumentView docView, int offset) {
        if (docView != null) {
            return docView.modelToYNeedsLock(offset);
        } else { // Fallback behavior
            return fallBackModelToY(offset);
        }
    }
    
    private double fallBackModelToY(int offset) {
        Shape s;
        try {
            s = textComponent.modelToView(offset);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
            s = null;
        }

        if (s != null) {
            return s.getBounds().y;
        } else {
            return 0d;
        }
    }
    
    public double[] modelToY(DocumentView docView, int[] offsets) {
        if (docView != null) {
            return docView.modelToYNeedsLock(offsets);
        } else { // Fallback behavior
            double[] ys = new double[offsets.length];
            for (int i = 0; i < offsets.length; i++) {
                ys[i] = fallBackModelToY(offsets[i]);
            }
            return ys;
        }
    }

    public Shape modelToView(DocumentView docView, int offset, Position.Bias bias) {
        if (docView != null) {
            return docView.modelToViewNeedsLock(offset, docView.getAllocation(), bias);
        } else {
            TextUI ui = textComponent.getUI();
            try {
                return (ui != null) ? ui.modelToView(textComponent, offset, bias) : null;
            } catch (BadLocationException ex) {
                return null;
            }
        }
    }

    public int viewToModel(DocumentView docView, double x, double y, Position.Bias[] biasReturn) {
        if (docView != null) {
            return docView.viewToModelNeedsLock(x, y, docView.getAllocation(), biasReturn);
        } else {
            TextUI ui = textComponent.getUI();
            return (ui != null) ? ui.viewToModel(textComponent, new Point((int)x, (int)y), biasReturn) : 0;
        }
    }

    public int modelToParagraphViewIndex(DocumentView docView, int offset) {
        int pViewIndex;
        if (docView != null && docView.op.isActive()) {
            pViewIndex = docView.getViewIndex(offset);
        } else {
            pViewIndex = -1;
        }
        return pViewIndex;
    }
    
    public int yToParagraphViewIndex(DocumentView docView, double y) {
        int pViewIndex;
        if (docView != null && docView.op.isActive()) {
            pViewIndex = docView.getViewIndex(y);
        } else {
            pViewIndex = -1;
        }
        return pViewIndex;
    }
    
    public boolean verifyParagraphViewIndexValid(DocumentView docView, int paragraphViewIndex) {
        return docView.op.isActive() && (paragraphViewIndex >= 0 &&
                paragraphViewIndex < docView.getViewCount());
    }

    public int getParagraphViewCount(DocumentView docView) {
        int pViewCount;
        if (docView != null && docView.op.isActive()) {
            pViewCount = docView.getViewCount();
        } else {
            pViewCount = -1; // Special value to mark non-NB (or inactive view hierarchy)
        }
        return pViewCount;
    }

    public float getDefaultRowHeight(DocumentView docView) {
        return docView.op.getDefaultRowHeight();
    }

    public float getDefaultCharWidth(DocumentView docView) {
        return docView.op.getDefaultCharWidth();
    }
    
    public boolean isActive(DocumentView docView) {
        return (docView != null && docView.op.isActive());
    }

    public void addViewHierarchyListener(ViewHierarchyListener l) {
        listenerList.add(l); // synced
    }

    public void removeViewHierarchyListener(ViewHierarchyListener l) {
        listenerList.remove(l); // synced
    }

    void fireChange(ViewHierarchyChange change) {
        ViewHierarchyEvent evt = ViewApiPackageAccessor.get().createEvent(viewHierarchy, change);
        if (ViewHierarchyImpl.EVENT_LOG.isLoggable(Level.FINE)) {
            ViewHierarchyImpl.EVENT_LOG.fine("Firing event: " + evt + "\n"); // NOI18N
        }
        for (ViewHierarchyListener l : listenerList.getListeners()) {
            l.viewHierarchyChanged(evt);
        }
    }

    @Override
    public String toString() {
        return (currentDocView != null) ? currentDocView.getDumpId() : "<NULL-docView>"; // NOI18N
    }

}
