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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.modules.profiler.ppoints.ui.TriggeredTakeSnapshotCustomizer;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;
import java.text.MessageFormat;
import java.util.Properties;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.ppoints.ui.ProfilingPointsIcons;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TriggeredTakeSnapshotProfilingPointFactory_PpType=Triggered Take Snapshot",
    "TriggeredTakeSnapshotProfilingPointFactory_PpDescr=Takes snapshot of currently collected profiling results similarly to Take Snapshot action in Profiler UI. This Profiling Point is defined globally for the profiling session and is invoked when defined condition is met during profiling.",
    "TriggeredTakeSnapshotProfilingPointFactory_PpHint=", // #207680 Do not remove, custom brandings may provide wizard hint here!!!
    "TriggeredTakeSnapshotProfilingPointFactory_PpDefaultName={0} in {1}"
})
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.ppoints.ProfilingPointFactory.class)
public class TriggeredTakeSnapshotProfilingPointFactory extends CodeProfilingPointFactory {    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getDescription() {
        return Bundle.TriggeredTakeSnapshotProfilingPointFactory_PpDescr();
    }
    
    public String getHint() {
        return Bundle.TriggeredTakeSnapshotProfilingPointFactory_PpHint();
    }

    public Icon getIcon() {
        return Icons.getIcon(ProfilingPointsIcons.TAKE_SNAPSHOT_TRIGGERED);
    }
    
    public Icon getDisabledIcon() {
        Icon icon = UIManager.getLookAndFeel().getDisabledIcon(null, 
                Icons.getIcon(ProfilingPointsIcons.TAKE_SNAPSHOT_TRIGGERED));
        if (icon == null)
            icon = new ImageIcon(GrayFilter.createDisabledImage(
                    Icons.getImage(ProfilingPointsIcons.TAKE_SNAPSHOT_TRIGGERED)));
        return icon;
    }

    public int getScope() {
        return SCOPE_GLOBAL;
    }

    public String getType() {
        return Bundle.TriggeredTakeSnapshotProfilingPointFactory_PpType();
    }

    public TriggeredTakeSnapshotProfilingPoint create(Lookup.Provider project) {
        if (project == null) {
            project = Utils.getCurrentProject(); // project not defined, will be detected from most active Editor or Main Project will be used
        }

        String name = Utils.getUniqueName(getType(),
                                          Bundle.TriggeredTakeSnapshotProfilingPointFactory_PpDefaultName(
                                                "", ProjectUtilities.getDisplayName(project)), // NOI18N
                                          project);

        return new TriggeredTakeSnapshotProfilingPoint(name, project, this);
    }

    public boolean supportsCPU() {
        return false;
    }

    public boolean supportsMemory() {
        return true;
    }

    public boolean supportsMonitor() {
        return false;
    }

    protected Class getProfilingPointsClass() {
        return TriggeredTakeSnapshotProfilingPoint.class;
    }

    protected String getServerHandlerClassName() {
        throw new UnsupportedOperationException();
    }

    protected TriggeredTakeSnapshotCustomizer createCustomizer() {
        return new TriggeredTakeSnapshotCustomizer(getType(), getIcon());
    }

    protected ProfilingPoint loadProfilingPoint(Lookup.Provider project, Properties properties, int index) {
        String name = properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_NAME, null); // NOI18N
        String enabledStr = properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_ENABLED, null); // NOI18N
        String type = properties.getProperty(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_TYPE, null); // NOI18N
        String target = properties.getProperty(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_TARGET, null); // NOI18N
        String file = properties.getProperty(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_CUSTOM_FILE, null); // NOI18N
        String resetResultsStr = properties.getProperty(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_RESET_RESULTS,
                                                        null); // NOI18N
        TriggeredGlobalProfilingPoint.TriggerCondition condition = TriggeredGlobalProfilingPoint.TriggerCondition.load(project,
                                                                                                                       index,
                                                                                                                       properties);

        if ((name == null) || (enabledStr == null) || (condition == null) || (type == null) || (target == null) || (file == null)
                || (resetResultsStr == null)) {
            return null;
        }

        TriggeredTakeSnapshotProfilingPoint profilingPoint = null;

        try {
            profilingPoint = new TriggeredTakeSnapshotProfilingPoint(name, project, this);
            profilingPoint.setEnabled(Boolean.parseBoolean(enabledStr));
            profilingPoint.setSnapshotType(type);
            profilingPoint.setSnapshotTarget(target);
            profilingPoint.setSnapshotFile(file);
            profilingPoint.setResetResults(Boolean.parseBoolean(resetResultsStr));
            profilingPoint.setCondition(condition);
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
        }

        return profilingPoint;
    }

    protected void storeProfilingPoint(ProfilingPoint profilingPoint, int index, Properties properties) {
        TriggeredTakeSnapshotProfilingPoint takeSnapshot = (TriggeredTakeSnapshotProfilingPoint) profilingPoint;
        properties.put(index + "_" + ProfilingPoint.PROPERTY_NAME, takeSnapshot.getName()); // NOI18N
        properties.put(index + "_" + ProfilingPoint.PROPERTY_ENABLED, Boolean.toString(takeSnapshot.isEnabled())); // NOI18N
        properties.put(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_TYPE, takeSnapshot.getSnapshotType()); // NOI18N
        properties.put(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_TARGET, takeSnapshot.getSnapshotTarget()); // NOI18N
        properties.put(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_CUSTOM_FILE,
                       (takeSnapshot.getSnapshotFile() == null) ? "" : takeSnapshot.getSnapshotFile()); // NOI18N
        properties.put(index + "_" + TriggeredTakeSnapshotProfilingPoint.PROPERTY_RESET_RESULTS,
                       Boolean.toString(takeSnapshot.getResetResults())); // NOI18N
        takeSnapshot.getCondition().store(takeSnapshot.getProject(), index, properties);
    }
}
