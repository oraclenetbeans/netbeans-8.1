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

package org.netbeans.modules.cnd.debugger.common2.debugger.breakpoints.types;

import java.io.File;
import javax.swing.JFileChooser;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.modules.cnd.debugger.common2.debugger.EditorContextBridge;
import org.netbeans.modules.cnd.debugger.common2.debugger.NativeSession;

import org.netbeans.modules.cnd.debugger.common2.utils.IpeUtils;
import org.netbeans.modules.cnd.debugger.common2.debugger.breakpoints.BreakpointPanel;
import org.netbeans.modules.cnd.debugger.common2.debugger.breakpoints.NativeBreakpoint;
import org.netbeans.modules.cnd.debugger.common2.debugger.remote.Host;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.remote.api.ui.FileChooserBuilder;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;

class LineBreakpointPanel extends BreakpointPanel {

    private LineBreakpoint lb;
    
    @Override
    public void seed(NativeBreakpoint breakpoint) {
	seedCommonComponents(breakpoint);
	lb = (LineBreakpoint) breakpoint;

	if (lb.getLineNumber() >= 1) {
	    lineField.setText(Integer.toString(lb.getLineNumber()));
	}

	String s = lb.getFileName();
	if (!IpeUtils.isEmpty(s))
	    fileText.setText(s.trim());
    }

    /*
     * Constructors
     */

    public LineBreakpointPanel() {
	this(new LineBreakpoint(NativeBreakpoint.TOPLEVEL), false);
    } 

    public LineBreakpointPanel(NativeBreakpoint b) {
	this((LineBreakpoint) b, true);
    } 

    private LineBreakpointPanel(LineBreakpoint breakpoint,
				boolean customizing) {
	super(breakpoint, customizing);
	lb = breakpoint;
	initComponents();
	addCommonComponents(2);

	if (!customizing) {
            // Seed the bpt object
            FileObject mostRecentFileObject = EditorContextBridge.getMostRecentFileObject();
            if (mostRecentFileObject != null) {
                String fileName = mostRecentFileObject.getPath();
                int lineNo = EditorContextBridge.getMostRecentLineNumber();
                FileSystem fileSystem = null;
                try {
                    fileSystem = mostRecentFileObject.getFileSystem();
                } catch (FileStateInvalidException ex) {
                }
                breakpoint.setFileAndLine(fileName, lineNo, fileSystem);
            }
	}

	seed(breakpoint);

	// Arrange to revalidate on changes
	fileText.getDocument().addDocumentListener(this);
	lineField.getDocument().addDocumentListener(this);

	lineField.selectAll();

	//fileText.requestDefaultFocus();
	fileText.requestFocus();
    }

    @Override
    public void setDescriptionEnabled(boolean enabled) {
	lineField.setEnabled(false);
	// lineLabel.setEnabled(false);
	browseButton.setEnabled(false);
	// fileLabel.setEnabled(false);
	fileText.setEnabled(false);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents

	java.awt.GridBagConstraints gridBagConstraints;

	fileLabel = new javax.swing.JLabel();
	fileText = new javax.swing.JTextField();
	browseButton = new javax.swing.JButton();
	lineLabel = new javax.swing.JLabel();
	lineField = new javax.swing.JTextField();

	panel_settings.setLayout(new java.awt.GridBagLayout());

	fileLabel.setText(Catalog.get("File"));	// NOI18N
	fileLabel.setDisplayedMnemonic(
	    Catalog.getMnemonic("MNEM_File"));	// NOI18N
	fileLabel.setLabelFor(fileText);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 0;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	panel_settings.add(fileLabel, gridBagConstraints);

	fileText.setColumns(20);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 0;
	gridBagConstraints.gridwidth = 3;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	gridBagConstraints.weightx = 1.0;
	panel_settings.add(fileText, gridBagConstraints);

	browseButton.setMnemonic(
	    Catalog.getMnemonic("MNEM_Browse"));// NOI18N
	browseButton.setText(Catalog.get("Browse")); // NOI18N
	browseButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		onBrowse(evt);
	    }
	});

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 4;
	gridBagConstraints.gridy = 0;
	gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
	panel_settings.add(browseButton, gridBagConstraints);

	lineLabel.setText(Catalog.get("Line"));	// NOI18N
	lineLabel.setDisplayedMnemonic(
	    Catalog.getMnemonic("MNEM_Line"));	// NOI18N
	lineLabel.setLabelFor(lineField);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 1;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	panel_settings.add(lineLabel, gridBagConstraints);

	lineField.setColumns(12);
	javax.swing.JPanel linePanel = new javax.swing.JPanel();
	linePanel.setLayout(new java.awt.BorderLayout());
	linePanel.add(lineField, java.awt.BorderLayout.WEST);
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 1;
	gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	panel_settings.add(linePanel, gridBagConstraints);

	// a11y
	lineField.getAccessibleContext().setAccessibleDescription(
	    Catalog.get("ACSD_Line") // NOI18N
	);
	fileText.getAccessibleContext().setAccessibleDescription(
	    Catalog.get("ACSD_File") // NOI18N
	);
	browseButton.getAccessibleContext().setAccessibleDescription(
	    browseButton.getText()
	);

    }//GEN-END:initComponents

    private void onBrowse(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onBrowse
	// Pick a seed for the filechooser
	// Generally fileText is 
	// Empty: seed will be null and chooser will go to home directory
	// Has a src file like /a/b/c/d/t.c: seed will be /a/b/c/d
	// Is a user-typed-in/pasted directory /a/b/c/d: seed will be /a/b/c/d
	// Is a directory with a bad tail: /a/b/c/X: will be /a/b/c
	// Is a bad path: chooser will go to home directory

	File seed = new File(fileText.getText());
	if (!seed.isDirectory())
	    seed = seed.getParentFile();

        FileSystem fileSystem = lb.getFileSystem();
        ExecutionEnvironment environment;
        if (fileSystem != null) {
            environment = FileSystemProvider.getExecutionEnvironment(fileSystem);
        } else {
            environment = ExecutionEnvironmentFactory.getLocal();
        }
        Session coreSession = DebuggerManager.getDebuggerManager().getCurrentSession();
        if (coreSession != null) {
            NativeSession nativeSession = NativeSession.map(coreSession);
            if (nativeSession != null) {
                environment = Host.byName(nativeSession.getSessionHost()).executionEnvironment();
            }
        }
        
        FileChooserBuilder builder = new FileChooserBuilder(environment);
        JFileChooser chooser = builder.createFileChooser();
        //	chooser.setDialogTitle("File Name");  Reasonable default???
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setMultiSelectionEnabled(false);
	// OLD chooser.setFileSystemView(new UnixFileSystemView(chooser.getFileSystemView()));
	chooser.setFileHidingEnabled(false);
	chooser.setCurrentDirectory(seed);
	int returnVal = chooser.showOpenDialog(this);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    fileText.setText(
		chooser.getSelectedFile().getParent() + "/" + // NOI18N
		chooser.getSelectedFile().getName());
	}	
    }//GEN-LAST:event_onBrowse

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField lineField;
    private javax.swing.JLabel lineLabel;
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JTextField fileText;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void assignProperties() {
	int i = 1;
	try {
	    i = Integer.parseInt (lineField.getText ());
	    if (i < 1) {
		i = 1;
	    }
	} catch (NumberFormatException e) {
	}	
	lb.setFileAndLine(fileText.getText(), i);
    }
    
    @Override
    protected boolean propertiesAreValid() {
	if (IpeUtils.isEmpty(lineField.getText()))
	    return false;

	try {
	    int i = Integer.parseInt (lineField.getText ());
	    if (i < 1) {
		return false;
	    }
	} catch (NumberFormatException e) {
	    return false;
	}	
	return true;
    }
}
