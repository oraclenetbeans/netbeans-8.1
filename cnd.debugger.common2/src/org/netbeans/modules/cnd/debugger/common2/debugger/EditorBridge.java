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

package org.netbeans.modules.cnd.debugger.common2.debugger;

import java.util.Date;
import java.awt.Color;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import java.io.File;
import org.netbeans.modules.cnd.makeproject.api.configurations.Configuration;

import org.openide.text.Line;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.NbDocument;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import org.openide.filesystems.*;
import org.openide.loaders.*;

import org.netbeans.editor.Utilities;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.Coloring;

import org.netbeans.modules.cnd.utils.cache.CndFileUtils;

import org.netbeans.modules.cnd.debugger.common2.utils.IpeUtils;
import org.netbeans.modules.cnd.debugger.common2.debugger.options.DebuggerOption;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.remote.spi.FileSystemProvider;

/**
 * A bridge to the NB editor.
 *
 * For editor connectivity details see:
 *	debuggerjpda/ant/src/org/netbeans/modules/debugger/projects/
 *		EditorContextImpl.java
 */

public final class EditorBridge {

    /**
     * From org.netbeans.modules.tasklist.suggestions.SuggestionsBroker
     */
    private static TopComponent getActiveEditor() {
	// assert SwingUtilities.isEventDispatchThread();

	// NOTE:
	// an editor pane may have been pulled out so that it's not
	// longer in the editor mode!
	// JEsse G. suggested:
	/*
	True. If you wanted to be obsessive, I guess you could search all open 
	TopComponent's for those which

	1. Are the frontmost in their mode.

	2. Have EditorCookie (in activatedNodes).

	3. Have DataObject (cookie).

	Or check the currently selected TC for #2 and #3.
	*/

//	Mode mode = WindowManager.getDefault().
//	    findMode(CloneableEditorSupport.EDITOR_MODE);
//	if (mode == null)
//	    return null;
//
//	TopComponent tc = mode.getSelectedTopComponent();
//	if (tc instanceof CloneableEditorSupport.Pane) {
//	    if (tc.isShowing())
//		return tc;
//	}

        // See IZ 191251
        // LATER: EditorBridge to be replaced by EditorContextBridge
        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated instanceof CloneableEditorSupport.Pane) {
            if (activated.isShowing())
            return activated;
        }
	return null;
    }

    private static TopComponent getActiveEditorSafe() {
	if (SwingUtilities.isEventDispatchThread()) {
	    return getActiveEditor();
	} else {
	    final TopComponent[] tc = new TopComponent[1];
	    try {
		SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
		    public void run() {
			tc[0] = getActiveEditor();
		    }
		} );
	    } catch (Exception e) {
		return null;
	    }
	    return tc[0];
	}
    }

    private static DataObject getDataObject() {
	TopComponent tc = getActiveEditorSafe();
	if (tc == null)
	    return null;
	DataObject dobj = tc.getLookup().lookup(DataObject.class);
	if (dobj != null && dobj.isValid()) {
	    return dobj;
	} else {
	    return null;
	}
    }

    private static EditorCookie getCurrentEditorCookie() {
	DataObject dao = getDataObject();
	if (dao == null) {
	    return null;
	}
	EditorCookie ec = dao.getCookie(EditorCookie.class);
	return ec;
    }

    private static JEditorPane getCurrentEditor(EditorCookie e) {
	if (e == null)
	    e = getCurrentEditorCookie();
	if (e == null)
	    return null;
	JEditorPane[] op = e.getOpenedPanes();
	if (op == null || op.length < 1)
	    return null;
	return op[0];
    }

    public static DataObject dataObjectForLine(Line l) {
	// 6502318
	if (l == null)
	    return null;

	org.openide.util.Lookup lineLookup = l.getLookup();
	DataObject dao = lineLookup.lookup(DataObject.class);
	if (dao == null)
	    return null;

	if (dao instanceof DataShadow)
	    dao = ((DataShadow) dao).getOriginal ();
	return dao;
    }

    public static String filenameFor(Line l) {
	DataObject dao = dataObjectForLine(l);
	if (dao == null)
	    return null;
        FileObject fo = dao.getPrimaryFile();
        File file = FileUtil.toFile(fo);
        // it would be s better to leave just fo.getPath(), 
        // but I'm not quite sure about '\\' vs '/' issue
        if (file == null) {
            return fo.getPath();
        } else {
            return file.getPath();
        }
    }

    public static Line getCurrentLine() {
	EditorCookie e = getCurrentEditorCookie();
	if (e == null)
	    return null;

	JEditorPane ep = getCurrentEditor(e);
	if (ep == null)
	    return null;
	StyledDocument d = e.getDocument();
	if (d == null)
	    return null;
	int lineNo = NbDocument.findLineNumber(d, ep.getCaret().getDot());

	// Editor numbers lines from 0!

	Line l = null;
	try {
	    l = e.getLineSet().getCurrent(lineNo + 1);
	} catch (IndexOutOfBoundsException x) {
	    // 6494346
	}
	return l;
    } 

    public static String getCurrentSelection() {
	JEditorPane ep = getCurrentEditor(null);
	if (ep == null)
	    return null;
	String s = ep.getSelectedText();
	if (s == null)
	    return null;
	return s;
    }

    private static DataObject dataObjectFor(FileObject fo) {
	if (fo == null)
	    return null;
	DataObject dao = null;
	try {
	    dao = DataObject.find(fo);
	} catch (DataObjectNotFoundException e) {
	}
	return dao;
    }

    /**
     * For a given data object, look up a line number and return its
     * corresponding Line object.
     * @param dao The Data object containing the line
     * @param lineNumber The line number of the line to be looked up
     * @return The Line object corresponding to the file, or null if
     *         the data object has no LineCookie, if the LineCookie
     *         has no LineSet, or if the LineSet fails to locate
     *         the given line.
     */
    public static Line lineNumberToLine(DataObject dao, int lineNumber)
	throws Exception {
	LineCookie lc = dao.getCookie(LineCookie.class);
	if (lc == null) {
	    // DEBUG System.out.println("No LineCookie found for the data object.");
	} else {
	    Line.Set ls = lc.getLineSet();
	    if (ls == null) {
		// DEBUG System.out.println("No associated LineSet!");
	    } else {
		// XXX HACK
		// I'm subtracting 1 because empirically I've discovered
		// that the editor highlights whatever line I ask for plus 1
		// OLD Line l = ls.getOriginal(lineNumber-1);
		Line l = ls.getCurrent(lineNumber-1);
		if (l != null) {
		    return l;
		} else {
		    // DEBUG System.out.println("No Line object for line number " + lineNumber);
		}
	    }
	}
	throw new Exception();
    }
    
    public static FileObject findFileObject(String fileName, NativeDebugger debugger) {
        return findFileObject(fileName, getSourceFileSystem(debugger));
    }
    
    private static FileObject findFileObject(String fileName, FileSystem fs) {
        CndUtils.assertAbsolutePathInConsole(fileName);
        String normPath = FileSystemProvider.normalizeAbsolutePath(fileName, fs);
        return CndFileUtils.toFileObject(fs, normPath);
    }
    
    public static FileSystem getSourceFileSystem(NativeDebugger debugger) {
        if (debugger != null) {
            NativeDebuggerInfo ndi = debugger.getNDI();
            if (ndi != null) {
                Configuration conf = ndi.getConfiguration();
                if (conf instanceof MakeConfiguration) {
                    return ((MakeConfiguration)conf).getSourceFileSystem();
                }
            }
        }
        return CndFileUtils.getLocalFileSystem();
    }

    /**
     * Find the Line object for the given file:line pair
     */

    public static Line getLine(String fileName, int lineNumber, NativeDebugger debugger) {
	return getLine(findFileObject(fileName, debugger), lineNumber);
    }
    
    public static Line getLine(String fileName, int lineNumber, FileSystem fs) {
	return getLine(findFileObject(fileName, fs), lineNumber);
    }

    private static Line getLine(FileObject fo, int lineNumber) {

	if (Log.Editor.debug)
	    System.out.printf("getline(\"%s\", %d)\n", fo.getPath(), lineNumber); // NOI18N

	DataObject dao = dataObjectFor(fo);
	if (dao == null) {
	    if (Log.Editor.debug)
		System.out.printf("\tno DAO\n"); // NOI18N
	    return null;
	}


	try {
	    return lineNumberToLine(dao, lineNumber);
	} catch (Exception e) {
	}
	return null;
    }

    /**
     * Shows given line in editor.
     * was:
     * Based on Utils.showInEditor(Line l), but modified so that
     * we don't always do unconditional fronting of the window.
     */
    
    public static void showInEditor(Line line) {
        showInEditor(line, false);
    }

    public static void showInEditor(Line line, boolean focus) {
	if (line == null) 
	    return;
	try {
	    // 6522537 urges us to use SHOW_REUSE but that steals
	    // the focus away per IZ 132671. (Just like SHOW_SHOW
	    // used to steal it per IZ 108834. 132671 was closed
	    // in favor of 138146 which introduced Line.ShowOpenType and
	    // Line.ShowVisibilityType.
	    //
	    // However, after all of this REUSE|FRONT _still_ causes the
	    // focus to get grabbed away.

	    // OLD line.show(line.SHOW_SHOW);
	    // line.show(Line.ShowOpenType.REUSE, Line.ShowVisibilityType.FRONT);
	    line.show(Line.ShowOpenType.OPEN, focus ? Line.ShowVisibilityType.FOCUS : Line.ShowVisibilityType.FRONT);
	    if (DebuggerOption.FRONT_IDE.isEnabled(NativeDebuggerManager.get().globalOptions()))
		WindowManager.getDefault().getMainWindow().toFront();

	} catch (Exception e) {
	}
    }
    
    /**
     * Force the editor to save the given filename.
     */
    public static boolean saveFile(String fileName, NativeDebugger debugger) {
        FileObject fo = findFileObject(fileName, debugger);
        DataObject dao = dataObjectFor(fo);
        if (dao == null)
            return false;

        EditorCookie ec = dao.getCookie(EditorCookie.class);
        if (ec == null)
            return false;

        try {
	    ec.saveDocument();
	} catch (java.io.IOException ex) {
	    return false;
	}
	return true;
    }

    public static Date lastModified(Line line) {
	DataObject dao = dataObjectForLine(line);
	if (dao == null)
	    return new Date();
        FileObject fo = dao.getPrimaryFile();
	if (fo == null)
	    return new Date();
	else
	    return fo.lastModified();
    }

    private static JEditorPane lastEditorPane = null;

    public static void setStatus(String msg) {
	// DEBUG System.out.printf("EditorBridge.setStatus():\n\t%s\n", msg);
	JEditorPane ep = getCurrentEditor(null);

	if (lastEditorPane != null && lastEditorPane != ep) {
	    // protect against stale 'lastEditorPane' (IZ 106720)
	    EditorUI ui = Utilities.getEditorUI(lastEditorPane);
	    if (ui != null)
		Utilities.clearStatusText(lastEditorPane);
	}
	lastEditorPane = ep;
	    
	if (ep == null)
	    return;
	if (msg != null) {
	    Coloring coloring = new Coloring();
	    coloring = Coloring.changeForeColor(coloring,
						new Color(1.0f, 0.0f, 0.0f));
	    Utilities.setStatusText(ep, msg, coloring);
	} else {
	    Utilities.clearStatusText(ep);
	}
    }

    /**
     * Return a source code Document corresponding to pathname w/o necessarily
     * opening the document in the editor.
     * @param pathname
     * @return
     */
    public static StyledDocument documentFor(String pathname, NativeDebugger debugger) {
	if (IpeUtils.isEmpty(pathname))
	    return null;
	FileObject fo = findFileObject(pathname, debugger);
	if (fo == null || !fo.isValid())
	    return null;
	DataObject dob = null;
	try {
	    dob = DataObject.find(fo);
	} catch (DataObjectNotFoundException ex) {
	    Exceptions.printStackTrace(ex);
	}
	if (dob == null || !dob.isValid())
	    return null;
	EditorCookie ec = dob.getCookie(EditorCookie.class);
	if (ec == null)
	    return null;
	StyledDocument document = ec.getDocument();
	return document;
    }
}
