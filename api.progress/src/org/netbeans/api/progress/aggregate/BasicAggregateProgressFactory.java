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

package org.netbeans.api.progress.aggregate;

import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Cancellable;

/**
 * Factory for creation of aggregate progress indication handles and individual contributor instances.
 * For a more simple version of progress indication, see {@link org.netbeans.api.progress.ProgressHandle}
 *
 * @author mkleint (mkleint@netbeans.org)
 */
public class BasicAggregateProgressFactory {
    /** Creates a new instance of AggregateProgressFactory */
    
    /**
     * Create an aggregating progress ui handle for a long lasting task.
     * @param contributors the initial set of progress indication contributors that are aggregated in the UI.
     * @param allowToCancel either null, if the task cannot be cancelled or 
     *          an instance of {@link org.openide.util.Cancellable} that will be called when user 
     *          triggers cancel of the task.
     * @param linkOutput an <code>Action</code> instance that links the running task in the progress bar
     *                   to an output of the task. The action is assumed to open the apropriate component with the task's output.
     * @param displayName to be shown in the progress UI
     * @return an instance of <code>ProgressHandle</code>, initialized but not started.
     *
     */
    public static AggregateProgressHandle createHandle(String displayName, ProgressContributor[] contributors, 
                                                       Cancellable allowToCancel) {
        return doCreateHandle(displayName, contributors, allowToCancel, false, 
                ProgressHandle.createHandle(displayName, allowToCancel));
    }
    
    public static ProgressContributor createProgressContributor(String trackingId) {
        return new ProgressContributor(trackingId);
    }
    
    protected static AggregateProgressHandle doCreateHandle(String displayName, ProgressContributor[] contributors, 
                                                       Cancellable allowToCancel, boolean systemHandle, ProgressHandle h) {
        return new AggregateProgressHandle(displayName, contributors, allowToCancel, systemHandle,
                h);
    }
    
    protected static ProgressHandle getProgressHandle(AggregateProgressHandle ah) {
        return ah.handle;
    }
}
