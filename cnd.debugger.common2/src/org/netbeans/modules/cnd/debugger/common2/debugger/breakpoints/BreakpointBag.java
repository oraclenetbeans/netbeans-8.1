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

package org.netbeans.modules.cnd.debugger.common2.debugger.breakpoints;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.openide.ErrorManager;

import org.netbeans.modules.cnd.debugger.common2.utils.masterdetail.RecordListEvent;
import org.netbeans.modules.cnd.debugger.common2.utils.masterdetail.RecordListListener;

import org.netbeans.modules.cnd.debugger.common2.utils.options.OptionSet;

import org.netbeans.modules.cnd.debugger.common2.utils.UserdirFile;


import org.netbeans.modules.cnd.debugger.common2.debugger.ModelChangeDelegator;
import org.netbeans.modules.cnd.debugger.common2.debugger.NativeDebugger;
import org.netbeans.modules.cnd.debugger.common2.debugger.NativeDebuggerManager;

import org.netbeans.modules.cnd.debugger.common2.debugger.options.DebuggerOption;

import org.netbeans.modules.cnd.debugger.common2.debugger.debugtarget.DebugTarget;
import org.netbeans.modules.cnd.debugger.common2.debugger.debugtarget.DebugTargetList;

/**
 * "database" of top-level NativeBreakpoints.
 */

public final class BreakpointBag {

    // shadow of the breakpoint list kept by debuggercore.
    private final ArrayList<NativeBreakpoint> breakpoints =
	new ArrayList<NativeBreakpoint>();

    private final DebugTargetList debugTargets;

    private boolean dirty;


    private class DebugTargetListener implements RecordListListener {

	/**
	 * Gets called when the contents of the DebugTarget list changes.
	 *
	 * It gets called too many times!
	 * a) In addRecentDebugTarget() there's the sequence
	 *	// 6785977
	 *	debugtargetlist.replaceRecordAt(dt, foundAt);
	 *	debugtargetlist.moveToFront(foundAt);
	 *    Each one causing a call to us.
	 * b) addRecentDebugTarget() gets called on progUnloaded()!
	 */
        @Override
	public void contentsChanged(RecordListEvent e) {
	    cleanupBpts();
	}
    }

    private class OptionListener implements PropertyChangeListener {
        @Override
	public void propertyChange(PropertyChangeEvent e) {
	    if (DebuggerOption.SAVE_BREAKPOINTS.caused(e))
		cleanupBpts();
	}
    }

    private NativeDebuggerManager manager() {
	return NativeDebuggerManager.get();
    }

    public BreakpointBag() {
	debugTargets = DebugTargetList.getInstance();
	RecordListListener debugTargetListener = new DebugTargetListener();
	debugTargets.addRecordListListener(debugTargetListener);

	PropertyChangeListener optionListener = new OptionListener();
	DebuggerOption.SAVE_BREAKPOINTS.addPropertyChangeListener(optionListener);
    }


    /**
     * Remove any bpts for which there are no debug targets.
     * This is called ...
     * - When the list of debug targets is changed.
     *   This includes changes in a DT's executable name as all changes get
     *   committed when the dialog OK or Apply is pressed.
     * - When we turn "Save And Restore Bpts" off.
     * - When a session is exited (NativeDebuggerImpl.preKill()).
     * - When we restore bpts from XML. This serves to cull any global-style
     *   bpts when one switches from non-pertarget to pertarget bpts.
     */
    public void cleanupBpts() {

	OptionSet globalOptions = NativeDebuggerManager.get().globalOptions();
	boolean saveBreakpoints =
	    DebuggerOption.SAVE_BREAKPOINTS.isEnabled(globalOptions);

	// create a Set of Contexts from the current debug target list
	Set<Context> contexts = null;

	if (saveBreakpoints) {
	    contexts = new HashSet<Context>();
	    for (DebugTarget dt : debugTargets) {
		Context c = new Context(dt.getExecutable(),
					dt.getHostName());
		contexts.add(c);
	    }
	} else {
	    // null 'contexts' means we don't save any bpts
	}

	if (Log.Bpt.pertarget) {
	    System.out.printf("BB.contentsChanged()::::::::::::::::::::::::\n"); // NOI18N
	    if (contexts == null) {
	    } else {
		for (Context c : contexts)
		    System.out.printf("\tCtx: %s\n", c); // NOI18N
	    }
	    System.out.printf("\t---------------------------------\n"); // NOI18N
	}

	// eliminate bpts which aren't in that set
	for (NativeBreakpoint b : getBreakpoints())
	    b.discardUnused(contexts);
    }

    /**
     * Return true if at least one breakpoint is enabled
     */

    public boolean anyEnabled() {
	for (NativeBreakpoint b : sessionBreakpoints(debugger())) {
	    if (b.isEnabled())
		return true;
	}
	return false;
    }

    /**
     * Return true if at least one breakpoint is disabled
     */

    public boolean anyDisabled() {
	for (NativeBreakpoint b : sessionBreakpoints(debugger())) {
	    if (! b.isEnabled())
		return true;
	}
	return false;
    }

    private NativeDebugger debugger() {
	return manager().currentDebugger();
    }

    ModelChangeDelegator breakpointUpdater() {
	return manager().breakpointUpdater();
    }


    public NativeBreakpoint[] getBreakpoints() {
	NativeBreakpoint [] ba = new NativeBreakpoint[breakpoints.size()];
	return breakpoints.toArray(ba);
    }


    /**
     * Locate a NativeBreakpoint at the given line.
     * There might be more than one so SHOULD return an array?
     * For now return the first one found or null.
     *
     * Match sub-bpts if we have a session, otherwise use toplevel ones.
     */

    public NativeBreakpoint locateBreakpointAt(String src,
					       int line,
					       NativeDebugger debugger) {

	for (NativeBreakpoint b : breakpoints) {
	    assert b.isToplevel();

	    if (debugger != null) {
		for (NativeBreakpoint m : b.getChildren()) {
		    for (NativeBreakpoint c : m.getChildren()) {
			if (c.matchesLineIn(src, line, debugger))
			    return c;
		    }
		}
	    } else {
		if (b.matchesLine(src, line))
		    return b;
	    }

	}
	return null;
    }


    /**
     * Called back when we restore a bag from XML.
     * At that point there may be no debugger or updater.
     * All such restored bpts get re-add'ed later on so only need to put
     * them on the list.
     */

    void restore(NativeBreakpoint newBpt) {
	newBpt.restoredChild();
	assert !breakpoints.contains(newBpt) :
	       "BB.restore(): bpt added redundantly"; // NOI18N
	breakpoints.add(newBpt);
	manager().addBreakpoint(newBpt);
	newBpt.setUpdater(breakpointUpdater());	// case of globalBreakpoints
    }


    void add(NativeBreakpoint newBpt) {
	assert newBpt.isToplevel();
	assert !newBpt.isEditable();

	assert !breakpoints.contains(newBpt) :
	       "BB.add(): bpt added redundantly"; // NOI18N
	breakpoints.add(newBpt);
	manager().addBreakpoint(newBpt);

	newBpt.setUpdater(breakpointUpdater());

	// propagate updater to all children
	for (NativeBreakpoint m : newBpt.getChildren()) {
	    m.setUpdater(breakpointUpdater());
	    for (NativeBreakpoint c : m.getChildren()) {
		c.setUpdater(breakpointUpdater());
	    }
	}

	breakpointUpdater().treeChanged();	// causes a pull

	dirty = true;
    }

    void remove(NativeBreakpoint oldBpt) {
	assert oldBpt.isToplevel();
	assert oldBpt.nChildren() == 0;

	oldBpt.cleanup();
	oldBpt.setDisposed(true);
	boolean removed = breakpoints.remove(oldBpt);
	assert removed :
	       "BB.remove(): bpt to be removed not in bag"; // NOI18N
	assert !breakpoints.contains(oldBpt) :
	       "BB.remove(): bpt still there after removal"; // NOI18N
	manager().removeBreakpoint(oldBpt);
	breakpointUpdater().treeChanged();	// causes a pull

	dirty = true;
    }

    /**
     * Return a list of midlevel bpts belonging to the session associated
     * with 'debugger'.
     */
    private List<NativeBreakpoint> sessionBreakpoints(NativeDebugger debugger) {
	assert NativeDebuggerManager.isPerTargetBpts();

	List<NativeBreakpoint> bpts = new ArrayList<NativeBreakpoint>();

	// for each top-level
	for (NativeBreakpoint b : breakpoints) {

	    // for each mid-level
	    for (NativeBreakpoint c : b.getChildren()) {
		if (c.getDebugger() == debugger ) {
		    bpts.add(c);
		    break;
		}
	    }
	}

	return bpts;
    }

    /*
     * routers for actions from BreakpointModel
     */

    public void postEnableAllHandlers(boolean v) {
	if (debugger() != null)
	    debugger().bm().postEnableAllHandlers(v);
    }

    public void postDeleteAllHandlers() {
	if (debugger() != null)
	    debugger().bm().postDeleteAllHandlers();
    }

    /*package*/ static final String moduleFolderName = "DbxGui";	// NOI18N
    /*package*/ static final String folderName = "DbxDebugBreakpoints";// NOI18N
    private static final String filename = "Breakpoints";	// NOI18N

    private static final UserdirFile userdirFile =
	new UserdirFile(moduleFolderName, folderName, filename);

    public void restore() {
        doRestore(userdirFile, this);
    }

    static void doRestore(UserdirFile userdirFile, BreakpointBag bb) {
	// DEBUG System.out.println("Restoring breakpoints...");
	BreakpointXMLReader xr = new BreakpointXMLReader(userdirFile, bb);
	try {
	    xr.read();
	} catch(Exception e) {
	    ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, e);
	}

	if (NativeDebuggerManager.isPerTargetBpts()) {
	    // In case we had an old Breakpoints.xml with global bpts.
	    bb.cleanupBpts();
	}

	// DEBUG System.out.println("Done.");
	bb.breakpointUpdater().treeChanged();	// causes a pull
    }

    private boolean isDirty() {
	if (dirty)
	    return true;

	for (NativeBreakpoint b : breakpoints) {
	    if (b.isDirty())
		return true;
	}
	return false;
    }

    private void clearDirty() {
	dirty = false;
        for (NativeBreakpoint b : breakpoints) {
	    b.clearDirty();
	}
    }

    public void save() {
        doSave(userdirFile, this);
    }

    static void doSave(UserdirFile userdirFile, BreakpointBag bb) {
	// DEBUG System.out.println("Saving breakpoints...");

	if (!bb.isDirty())
	    return;

	BreakpointXMLWriter xw = new BreakpointXMLWriter(userdirFile, bb);
	try {
	    xw.write();
	    bb.clearDirty();
	} catch(Exception e) {
	    ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, e);
	}
	// DEBUG System.out.println("Done.");
    }
}

