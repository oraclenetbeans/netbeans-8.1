/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
package org.netbeans.modules.testng.ant;

import org.netbeans.modules.gsf.testrunner.api.TestSession.SessionType;

/**
 * Holds information about an <code>AntSession</code>.
 *
 * @author  Marian Petras
 * @see  TestNGAntLogger
 */
final class AntSessionInfo {

    TestNGOutputReader outputReader = null;
    /** */
    private long timeOfTestTaskStart;
    /** */
    private SessionType currentSessionType;
    /**
     * type of the session - one of the <code>SESSION_TYPE_xxx</code> constants
     */
    private SessionType sessionType;

    /** Suite name, defaults to "Ant suite" */
    private String sessionName = "Ant suite";

    /**
     */
    AntSessionInfo() {
    }

    /**
     */
    long getTimeOfTestTaskStart() {
        return timeOfTestTaskStart;
    }

    void setTimeOfTestTaskStart(long time) {
        timeOfTestTaskStart = time;
    }

    SessionType getCurrentSessionType() {
        return currentSessionType;
    }

    void setCurrentSessionType(SessionType currentTaskType) {
        this.currentSessionType = currentTaskType;
    }

    SessionType getSessionType() {
        return sessionType;
    }

    void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    String getSessionName() {
        return sessionName;
    }

    void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

}
