/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 * Opens the Heap Walker
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "OpenHeapWalkerAction_ActionName=Load Heap D&ump...",
    "OpenHeapWalkerAction_DialogCaption=Open Heap Dump File"
})
public final class OpenHeapWalkerAction implements ActionListener {
    private static File importDir;

    public void actionPerformed(ActionEvent e) {
        final File heapDumpFile = getHeapDumpFile();
        BrowserUtils.performTask(new Runnable() {
                public void run() {
                    if (heapDumpFile != null) {
                        HeapWalkerManager.getDefault().openHeapWalker(heapDumpFile);
                    }
                }
            });
    }
    private static File getHeapDumpFile() {
        JFileChooser chooser = new JFileChooser();

        if (importDir != null) {
            chooser.setCurrentDirectory(importDir);
        }

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());
        chooser.addChoosableFileFilter(new FileFilterImpl());
        chooser.setDialogTitle(
            Bundle.OpenHeapWalkerAction_DialogCaption()
        );

        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            importDir = chooser.getCurrentDirectory();

            return chooser.getSelectedFile();
        }

        return null;
    }

    private static class FileFilterImpl extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            return ResultsManager.checkHprofFile(f);
        }

        @NbBundle.Messages({
            "FileDescription=Heap Dumps (*.hprof, *.bin, *.*)"
        })
        @Override
        public String getDescription() {
            return Bundle.FileDescription();
        }
    }
}
