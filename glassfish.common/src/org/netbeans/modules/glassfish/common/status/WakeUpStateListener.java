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
package org.netbeans.modules.glassfish.common.status;

import org.netbeans.modules.glassfish.tooling.GlassFishStatus;
import org.netbeans.modules.glassfish.tooling.data.GlassFishServer;
import org.netbeans.modules.glassfish.tooling.data.GlassFishStatusTask;

/**
 * Notification about server state check results that will wake up waiting
 * thread to pass notification.
 * <p/>
 * @author Tomas Kraus
 */
public abstract class WakeUpStateListener extends BasicStateListener {
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** Requested wake up of checking thread. */
    private volatile boolean wakeUp;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an instance of state check results notification.
     */
    public WakeUpStateListener() {
        super();
        wakeUp = false;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods                                                                //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Wake up checking thread.
     */
    protected void wakeUp() {
        if (!wakeUp) synchronized(this) {
            wakeUp = true;
            this.notify();
        }
    }

    /**
     * Get status of wake up request of checking thread.
     * <p/>
     * @return Status of wake up request of checking thread.
     */
    public boolean isWakeUp() {
        return wakeUp;
    }

    /**
     * Wake up waiting thread immediately on registered state changes.
     * <p/>
     * @param server GlassFish server instance being monitored.
     * @param status Current server status.
     * @param task   Last GlassFish server status check task details.
     */    
    @Override
    public void newState(final GlassFishServer server,
            final GlassFishStatus status, final GlassFishStatusTask task) {
        wakeUp();
    }

    /**
     * Do nothing.
     * <p/>
     * Error callback is expected to be unused.
     * <p/>
     * @param server GlassFish server instance being monitored.
     * @param task   GlassFish server status check task details.
     */
    @Override
    public void error(final GlassFishServer server,
            final GlassFishStatusTask task) {
        // Not used yet.
    }

}
