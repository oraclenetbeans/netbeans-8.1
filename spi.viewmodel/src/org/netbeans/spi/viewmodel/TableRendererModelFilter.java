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

package org.netbeans.spi.viewmodel;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Model filter that can override custom cell renderer and cell editor for table cells.
 * 
 * @author Martin Entlicher
 * @since 1.28
 */
public interface TableRendererModelFilter extends Model {

    /**
     * Test whether this renderer can render the given cell.
     * @param original The original table cell renderer implementation
     * @param node Tree node representing the row
     * @param columnID The column name
     * @return <code>true</code> if the implementation can render the given cell, <code>false</code> otherwise
     * @throws UnknownTypeException If the implementation can not decide whether to render the given cell.
     */
    public boolean canRenderCell(TableRendererModel original, Object node, String columnID)
            throws UnknownTypeException;

    /**
     * Get the renderer of the given cell
     * @param original The original table cell renderer implementation
     * @param node Tree node representing the row
     * @param columnID The column name
     * @return The cell renderer
     * @throws UnknownTypeException If the implementation can not render the given cell.
     */
    public TableCellRenderer getCellRenderer(TableRendererModel original, Object node, String columnID)
            throws UnknownTypeException;

    /**
     * Test whether this renderer can edit the given cell.
     * @param original The original table cell renderer implementation
     * @param node Tree node representing the row
     * @param columnID The column name
     * @return <code>true</code> if the implementation can edit the given cell, <code>false</code> otherwise
     * @throws UnknownTypeException If the implementation can not decide whether to edit the given cell.
     */
    public boolean canEditCell(TableRendererModel original, Object node, String columnID)
            throws UnknownTypeException;

    /**
     * Get the editor of the given cell
     * @param original The original table cell renderer implementation
     * @param node Tree node representing the row
     * @param columnID The column name
     * @return The cell editor
     * @throws UnknownTypeException If the implementation can not edit the given cell.
     */
    public TableCellEditor getCellEditor(TableRendererModel original, Object node, String columnID)
            throws UnknownTypeException;

    /**
     * Registers given listener.
     *
     * @param l the listener to add
     */
    public abstract void addModelListener (ModelListener l);

    /**
     * Unregisters given listener.
     *
     * @param l the listener to remove
     */
    public abstract void removeModelListener (ModelListener l);
}
