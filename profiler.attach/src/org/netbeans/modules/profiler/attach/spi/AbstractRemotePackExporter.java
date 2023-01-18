/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.attach.spi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.netbeans.modules.profiler.attach.providers.TargetPlatformEnum;

/**
 *
 * @author Jaroslav Bachorik
 */
abstract public class AbstractRemotePackExporter {
    private static final Map<String, String> scriptMapper = new HashMap<String, String>() {
        {
            put(IntegrationUtils.PLATFORM_LINUX_AMD64_OS, "linuxamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_OS, "linux"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_ARM_OS, "linuxarm"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_ARM_VFP_HFLT_OS, "linuxarmvfphflt"); //NOI18N
            put(IntegrationUtils.PLATFORM_MAC_OS, "mac"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_AMD64_OS, "solamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_INTEL_OS, "solx86"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_SPARC_OS, "solsparc"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_SPARC64_OS, "solsparcv9"); //NOI18N
            put(IntegrationUtils.PLATFORM_WINDOWS_AMD64_OS, "winamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_WINDOWS_OS, "win"); //NOI18N
        }
    };
    private static final Map<String, String> jdkMapper = new HashMap<String, String>() {
        {
            // NOTE: 15 is used to only generate Ant task name which always ends with '-15'
            put(TargetPlatformEnum.JDK5.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK6.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK7.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK8.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK9.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK_CVM.toString(), "cvm"); //NOI18N
        }
    };
    
    final protected String getPlatformShort(String hostOS) {
        return scriptMapper.get(hostOS);
    }
    
    final protected String getJVMShort(String jvm) {
        return jdkMapper.get(jvm);
    }
    
    abstract public String export(String exportPath, String hostOS, String jvm) throws IOException;
    abstract public String getRemotePackPath(String exportPath, String hostOS);
}
