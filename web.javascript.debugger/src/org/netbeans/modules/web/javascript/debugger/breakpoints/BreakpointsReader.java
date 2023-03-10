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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.web.javascript.debugger.breakpoints;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.Properties;
import org.netbeans.modules.javascript2.debug.breakpoints.JSLineBreakpoint;
import org.netbeans.modules.web.javascript.debugger.MiscEditorUtil;
import org.netbeans.modules.web.webkit.debugging.api.Debugger;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.openide.text.Line;

/**
 *
 * @author ads
 */
@DebuggerServiceRegistration(path="", types=Properties.Reader.class)
public class BreakpointsReader implements Properties.Reader {

    private static final String LINE_NUMBER     = "lineNumber";                 // NOI18N
    private static final String URL             = "url";                        // NOI18N
    
    private static final String DOM_NODE_PATH   = "domNodePath";                // NOI18N
    
    private static final String EVENTS          = "events";                     // NOI18N
    
    private static final String URL_SUBSTRING   = "urlSubstring";               // NOI18N
    
    private static final String ENABED          = "enabled";                    // NOI18N
    private static final String GROUP_NAME      = "groupName";                  // NOI18N

    private static final String OLD_LINE_BP_CLASS_NAME = "org.netbeans.modules.web.javascript.debugger.breakpoints.LineBreakpoint";

    @Override
    public String [] getSupportedClassNames() {
        return new String[] {
            OLD_LINE_BP_CLASS_NAME,
            DOMBreakpoint.class.getName(),
            EventsBreakpoint.class.getName(),
            XHRBreakpoint.class.getName(),
        };
    }

    @Override
    public Object read( String typeID, Properties properties ) {
        if (typeID.equals(OLD_LINE_BP_CLASS_NAME)) {
            Line line = MiscEditorUtil.getLine(properties.getString(URL, null), properties
                    .getInt(LINE_NUMBER, 1));

            if (line == null) {
                return null;
            }
            JSLineBreakpoint breakpoint = new JSLineBreakpoint(line);
            readGeneralProperties(properties, breakpoint);
            return breakpoint;
        }
        else if (typeID.equals(DOMBreakpoint.class.getName())) {
            String nodePathDefinition = properties.getString(DOM_NODE_PATH, null);
            if (nodePathDefinition == null) {
                return null;
            }
            String urlStr = properties.getString(URL, null);
            URL url;
            if (urlStr != null) {
                try {
                    url = new URL(urlStr);
                } catch (MalformedURLException ex) {
                    url = null;
                }
            } else {
                url = null;
            }
            if (url == null) {
                // The file is gone
                return null;
            }
            DOMNode node = DOMNode.create(nodePathDefinition);
            DOMBreakpoint db = new DOMBreakpoint(url, node);
            db.setOnSubtreeModification(properties.getBoolean(Debugger.DOM_BREAKPOINT_SUBTREE, false));
            db.setOnAttributeModification(properties.getBoolean(Debugger.DOM_BREAKPOINT_ATTRIBUTE, false));
            db.setOnNodeRemoval(properties.getBoolean(Debugger.DOM_BREAKPOINT_NODE, false));
            readGeneralProperties(properties, db);
            return db;
        }
        else if (typeID.equals(EventsBreakpoint.class.getName())) {
            EventsBreakpoint eb = new EventsBreakpoint();
            Object[] events = properties.getArray(EVENTS, null);
            if (events != null) {
                for (Object event : events) {
                    eb.addEvent((String) event);
                }
            }
            readGeneralProperties(properties, eb);
            return eb;
        }
        else if (typeID.equals(XHRBreakpoint.class.getName())) {
            String urlSubstring = properties.getString(URL_SUBSTRING, "");
            XHRBreakpoint xb = new XHRBreakpoint(urlSubstring);
            readGeneralProperties(properties, xb);
            return xb;
        }
        else {
            return null;
        }
    }
    
    @Override
    public void write(Object object, Properties properties) {
        /*if (object instanceof LineBreakpoint) {
            LineBreakpoint breakpoint = (LineBreakpoint) object;
            
            properties.setString(URL, breakpoint.getURLStringToPersist());
            properties.setInt(LINE_NUMBER, breakpoint.getLine().getLineNumber());
            writeGeneralProperties(properties, breakpoint);
        }
        else*/ if (object instanceof DOMBreakpoint) {
            DOMBreakpoint db = (DOMBreakpoint) object;
            
            String urlStr;
            URL url = db.getURL();
            if (url != null) {
                urlStr = url.toExternalForm();
            } else {
                urlStr = null;
            }
            properties.setString(URL, urlStr);
            properties.setString(DOM_NODE_PATH, db.getNode().getStringDefinition());
            properties.setBoolean(Debugger.DOM_BREAKPOINT_SUBTREE, db.isOnSubtreeModification());
            properties.setBoolean(Debugger.DOM_BREAKPOINT_ATTRIBUTE, db.isOnAttributeModification());
            properties.setBoolean(Debugger.DOM_BREAKPOINT_NODE, db.isOnNodeRemoval());
            writeGeneralProperties(properties, db);
        }
        else if (object instanceof EventsBreakpoint) {
            EventsBreakpoint eb = (EventsBreakpoint) object;
            
            Set<String> events = eb.getEvents();
            properties.setArray(EVENTS, events.toArray());
            writeGeneralProperties(properties, eb);
        }
        else if (object instanceof XHRBreakpoint) {
            XHRBreakpoint xb = (XHRBreakpoint) object;
            
            String urlSubstring = xb.getUrlSubstring();
            properties.setString(URL_SUBSTRING, urlSubstring);
            writeGeneralProperties(properties, xb);
        }
    }
    
    private void readGeneralProperties(Properties properties, Breakpoint breakpoint) {
        if (!properties.getBoolean(ENABED, true)) {
            breakpoint.disable();
        }
        breakpoint.setGroupName(properties.getString(GROUP_NAME, ""));
    }

    private void writeGeneralProperties(Properties properties, Breakpoint breakpoint) {
        properties.setBoolean(ENABED, breakpoint.isEnabled());
        properties.setString(GROUP_NAME, breakpoint.getGroupName());
    }

}
