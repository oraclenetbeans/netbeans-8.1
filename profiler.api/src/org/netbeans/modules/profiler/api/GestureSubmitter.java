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

package org.netbeans.modules.profiler.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;


/**
 * A utility class for submitting UI Gestures Collector records
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
public class GestureSubmitter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger USG_LOGGER = Logger.getLogger("org.netbeans.ui.metrics.profiler"); // NOI18N
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

//    public static void logConfig(ProfilingSettings settings, InstrumentationFilter filter) {
//        List<Object> paramList = new ArrayList<Object>();
//
//        fillParamsForProfiling(settings, paramList);
//
//        logUsage("CONFIG", paramList); // NOI18N
//    }

    public static void logProfileApp(Lookup.Provider p, ProfilingSettings ps) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(p, paramList);
        fillParamsForProfiling(ps, paramList);

        logUsage("PROFILE_APP", paramList); // NOI18N
    }
    
    public static void logAttachApp(Lookup.Provider p, ProfilingSettings ps, AttachSettings as) {
        List<Object> paramList = new ArrayList<Object>();

        fillProjectParam(p, paramList);
        fillParamsForProfiling(ps, paramList);
        fillParamsForAttach(as, paramList);

        logUsage("ATTACH_APP", paramList); // NOI18N
    }
    
    public static void logAttachExternal(ProfilingSettings ps, AttachSettings as) {
        List<Object> paramList = new ArrayList<Object>();

        fillParamsForProfiling(ps, paramList);
        fillParamsForAttach(as, paramList);

        logUsage("ATTACH_EXT", paramList); // NOI18N
    }

//    public static void logProfileClass(Lookup.Provider profiledProject, SessionSettings session) {
//        List<Object> paramList = new ArrayList<Object>();
//
//        fillProjectParam(profiledProject, paramList);
//        fillParamsForSession(session, paramList);
//
//        logUsage("PROFILE_CLASS", paramList); // NOI18N
//    }

//    public static void logAttach(Lookup.Provider profiledProject, AttachSettings attach) {
//        List<Object> paramList = new ArrayList<Object>();
//
//        fillProjectParam(profiledProject, paramList);
//        fillParamsForAttach(attach, paramList);
//
//        logUsage("ATTACH", paramList); // NOI18N
//    }
    
//    public static void logRMSSearch(String pattern) {
//        logUsage("RMS_SEARCH", Arrays.asList(new Object[]{pattern}));
//    }

    private static void fillProjectParam(Lookup.Provider profiledProject, List<Object> paramList) {
        String param = ""; // NOI18N
        if (profiledProject != null) {
            param = profiledProject.getClass().getName();
        }
        paramList.add(0, param);
    }
    
    private static void fillParamsForAttach(AttachSettings as, List<Object> paramList) {
//        paramList.add("OS_" + as.getHostOS());
        paramList.add(as.isDirect() ? "ATTACH_DIRECT" : "ATTACH_DYNAMIC"); // NOI18N
        paramList.add(as.isRemote() ? "ATTACH_REMOTE" : "ATTACH_LOCAL"); // NOI18N
    }
    
    private static void fillParamsForProfiling(ProfilingSettings ps, List<Object> paramList) {
        switch (ps.getProfilingType()) {
            case ProfilingSettings.PROFILE_CPU_ENTIRE:
                paramList.add("TYPE_CPU_ENTIRE"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_PART:
                paramList.add("TYPE_CPU_PART"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_SAMPLING:
                paramList.add("TYPE_CPU_SAMPLING"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                paramList.add("TYPE_CPU_STOPWATCH"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                paramList.add("TYPE_MEM_ALLOC"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                paramList.add("TYPE_MEM_LIVENESS"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
                paramList.add("TYPE_MEM_SAMPLING"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_MONITOR:
                paramList.add("TYPE_MONITOR"); // NOI18N

                break;
        }
        
        if (ps.getThreadsMonitoringEnabled()) paramList.add("TYPE_THREADS"); // NOI18N
        
        if (ps.getLockContentionMonitoringEnabled()) paramList.add("TYPE_LOCKS"); // NOI18N
    }

//    private static void fillParamsForProfiling(ProfilingSettings ps, List<Object> paramList) {
//        switch (ps.getProfilingType()) {
//            case ProfilingSettings.PROFILE_CPU_ENTIRE:
//                paramList.add("TYPE_CPU_ENTIRE"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_CPU_PART:
//                paramList.add("TYPE_CPU_PART"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_CPU_SAMPLING:
//                paramList.add("TYPE_CPU_SAMPLING"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
//                paramList.add("TYPE_CPU_STOPWATCH"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
//                paramList.add("TYPE_MEM_ALLOC"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
//                paramList.add("TYPE_MEM_LIVENESS"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
//                paramList.add("TYPE_MEM_SAMPLING"); // NOI18N
//
//                break;
//            case ProfilingSettings.PROFILE_MONITOR:
//                paramList.add("TYPE_MONITOR"); // NOI18N
//
//                break;
//        }
//
//        switch (ps.getInstrScheme()) {
//            case CommonConstants.INSTRSCHEME_EAGER:
//                paramList.add("INSTR_EAGER"); // NOI18N
//
//                break;
//            case CommonConstants.INSTRSCHEME_LAZY:
//                paramList.add("INSTR_LAZY"); // NOI18N
//
//                break;
//            case CommonConstants.INSTRSCHEME_TOTAL:
//                paramList.add("INSTR_TOTAL"); // NOI18N
//
//                break;
//        }
//
//        paramList.add(ps.getProfileUnderlyingFramework() ? "FRAMEWORK_YES" : "FRAMEWORK_NO");
//        paramList.add(ps.getExcludeWaitTime() ? "WAIT_EXCLUDE" : "WAIT_INCLUDE"); // NOI18N
//        paramList.add(ps.getInstrumentMethodInvoke() ? "REFL_INVOKE_YES" : "REFL_INVOKE_NO"); // NOI18N
//        paramList.add(ps.getInstrumentSpawnedThreads() ? "SPAWNED_THREADS_YES" : "SPAWNED_THREADS_NO"); // NOI18N
//        paramList.add(ps.getThreadCPUTimerOn() ? "THREAD_CPU_YES" : "THREAD_CPU_NO"); // NOI18N
//        paramList.add(ps.useProfilingPoints() ? "PPOINTS_YES" : "PPOINTS_NO"); //NOI18N
//    }

//    private static void fillParamsForSession(SessionSettings ss, List<Object> paramList) {
//        paramList.add("JAVA_" + ss.getJavaVersionString()); // NOI18N
//    }

    private static void logUsage(String startType, List<Object> params) {
        LogRecord record = new LogRecord(Level.INFO, "USG_PROFILER_" + startType); // NOI18N
        record.setResourceBundle(NbBundle.getBundle(GestureSubmitter.class));
        record.setResourceBundleName(GestureSubmitter.class.getPackage().getName() + ".Bundle"); // NOI18N
        record.setLoggerName(USG_LOGGER.getName());
        record.setParameters(params.toArray(new Object[params.size()]));

        USG_LOGGER.log(record);
    }
}
