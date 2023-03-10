/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javascript.v8debug.breakpoints;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.api.debugger.Session;
import org.netbeans.modules.javascript.v8debug.V8Debugger;
import org.netbeans.modules.javascript2.debug.JSUtils;
import org.netbeans.modules.javascript2.debug.breakpoints.JSBreakpointsInfo;
import org.netbeans.modules.javascript2.debug.sources.SourceFilesCache;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Martin Entlicher
 */
@ServiceProvider(service = JSBreakpointsInfo.class)
public class V8BreakpointsActiveService implements JSBreakpointsInfo {
    
    private volatile boolean active = true;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    public V8BreakpointsActiveService() {
        SessionActiveListener sal = new SessionActiveListener();
        DebuggerManager.getDebuggerManager().addDebuggerListener(DebuggerManager.PROP_CURRENT_SESSION, sal);
    }

    @Override
    public boolean isDefault() {
        FileObject mostRecentFile = EditorContextDispatcher.getDefault().getMostRecentFile();
        if (mostRecentFile == null) {
            return false;
        }
        String mimeType = mostRecentFile.getMIMEType();
        return JSUtils.JS_MIME_TYPE.equals(mimeType);
    }

    @Override
    public boolean isAnnotatable(FileObject fo) {
        String mimeType = fo.getMIMEType();
        return JSUtils.JS_MIME_TYPE.equals(mimeType);
    }

    @Override
    public boolean isTransientURL(URL url) {
        return SourceFilesCache.URL_PROTOCOL.equals(url.getProtocol());
    }

    @Override
    public boolean areBreakpointsActivated() {
        return active;
    }

    private void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            pcs.firePropertyChange(PROP_BREAKPOINTS_ACTIVE, !active, active);
        }
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
    
    private class SessionActiveListener extends DebuggerManagerAdapter implements BreakpointsHandler.BreakpointsActiveListener {
        
        private BreakpointsHandler bh;
        
        public SessionActiveListener() {
            bh = getCurrentBreakpointsHandler();
            if (bh != null) {
                active = bh.areBreakpointsActive();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (DebuggerManager.PROP_CURRENT_SESSION.equals(propertyName)) {
                BreakpointsHandler newBh = getCurrentBreakpointsHandler();
                synchronized (this) {
                    if (bh != null) {
                        bh.removeBreakpointsActiveListener(this);
                    }
                    bh = newBh;
                }
                if (newBh != null) {
                    setActive(newBh.areBreakpointsActive());
                } else {
                    setActive(true);
                }
            }
        }
        
        @Override
        public void breakpointsActivated(boolean activated) {
            setActive(activated);
        }
        
        private BreakpointsHandler getCurrentBreakpointsHandler() {
            Session s = DebuggerManager.getDebuggerManager().getCurrentSession();
            if (s != null) {
                V8Debugger debugger = s.lookupFirst(null, V8Debugger.class);
                if (debugger != null) {
                    BreakpointsHandler bh = debugger.getBreakpointsHandler();
                    bh.addBreakpointsActiveListener(this);
                    return bh;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

    }
}
