/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.common.sourcemap;

/**
 * Mapping of a source map.
 *
 * @author Jan Stola
 */
public class Mapping {
    /** Mapping representing a new line. For internal purposes only. */
    static final Mapping NEW_LINE = new Mapping();

    /** Column in the compiled source. */
    private int column;
    /** Index of the source file. */
    private int sourceIndex = -1;
    /** Line in the source file. */
    private int originalLine = -1;
    /** Column in the source file. */
    private int originalColumn = -1;

    /**
     * Sets the column in the compiled source.
     * 
     * @param column column in the compiled source.
     */
    void setColumn(int column) {
        this.column = column;
    }

    /**
     * Sets the index of the source file.
     * 
     * @param sourceIndex index of the source file.
     */
    void setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    /**
     * Sets the line in the source file.
     * 
     * @param originalLine line in the source file.
     */
    void setOriginalLine(int originalLine) {
        this.originalLine = originalLine;
    }

    /**
     * Sets the column in the source file.
     * 
     * @param originalColumn column in the source file.
     */
    void setOriginalColumn(int originalColumn) {
        this.originalColumn = originalColumn;
    }

    /**
     * Returns the column in the compiled source.
     * 
     * @return column in the compiled source.
     */
    int getColumn() {
        return column;
    }

    /**
     * Returns the index of the source file.
     * 
     * @return index of the source file.
     */
    public int getSourceIndex() {
        return sourceIndex;
    }

    /**
     * Returns the line in the source file.
     * 
     * @return line in the source file.
     */
    public int getOriginalLine() {
        return originalLine;
    }

    /**
     * Returns the column in the source file.
     * 
     * @return column in the source file.
     */
    public int getOriginalColumn() {
        return originalColumn;
    }

}
