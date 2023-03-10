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
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package org.netbeans.lib.uihandler;

import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

/** Represents a gesture that initiated the given LogRecord.
 *
 * @author Jaroslav Tulach
 */
public enum InputGesture {
    KEYBOARD, MENU, TOOLBAR;


    private static final XMLFormatter F = new XMLFormatter();
    
    /** Finds the right InputGesture for given LogRecord.
     * @param rec the record
     * @return the gesture that initiated the record or <code>null</code> if unknown
     */
    public static InputGesture valueOf(LogRecord rec) {
        if ("UI_ACTION_BUTTON_PRESS".equals(rec.getMessage())) {
            String fullMsg = F.format(rec);
            if (fullMsg.indexOf("Actions$Menu") >= 0) {
                return MENU;
            }
            if (fullMsg.indexOf("Actions$Toolbar") >= 0) {
                return TOOLBAR;
            }
        }
        if ("UI_ACTION_KEY_PRESS".equals(rec.getMessage())) {
            return KEYBOARD;
        }
        return null;
    }
}
