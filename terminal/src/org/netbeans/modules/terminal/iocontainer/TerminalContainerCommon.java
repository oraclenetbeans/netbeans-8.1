/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.terminal.iocontainer;



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;

import org.openide.windows.IOContainer;
import org.openide.windows.IOContainer.CallBacks;
import org.openide.windows.TopComponent;

import org.netbeans.lib.terminalemulator.support.FindBar;
import org.netbeans.lib.terminalemulator.support.FindState;

import org.netbeans.modules.terminal.api.IOVisibilityControl;
import org.netbeans.modules.terminal.api.TerminalContainer;

abstract class TerminalContainerCommon extends TerminalContainer implements IOContainer.Provider {

    private final static String PROP_ATTRIBUTES =
	    "TerminalContainerCommonImpl.ATTRIBUTES";	// NOI18N

    protected final TopComponent owner;
    protected final String originalName;

    private IOContainer ioContainer;
    private boolean activated = false;

    private JToolBar actionBar;
    private FindBar findBar;

    private JComponent lastSelection;

    protected static final class Attributes {
	public CallBacks cb;
	public String title;
	public Action[] toolbarActions;
	public String toolTipText;
	public Icon icon;

	// semi-LATER
	public FindState findState;

	// LATER
	// public boolean isClosable;
    }

    public TerminalContainerCommon(TopComponent owner, String originalName) {
        super();
        this.owner = owner;
        this.originalName = originalName;
    }

    final protected TopComponent topComponent() {
        return owner;
    }

    /**
     * Return Attributes associated with 'comp'.
     * Create and attach of none exist.
     * @param comp
     * @return
     */
    protected final Attributes attributesFor(JComponent comp) {
	Object o = comp.getClientProperty(PROP_ATTRIBUTES);
	if (o == null) {
	    Attributes a = new Attributes();
	    comp.putClientProperty(PROP_ATTRIBUTES, a);
	    return a;
	} else {
	    return (Attributes) o;
	}
    }


    //
    // Overrides of TerminalContainer
    //

    @Override
    public IOContainer ioContainer() {
	if (ioContainer == null)
	    ioContainer = IOContainer.create(this);
	return ioContainer;
    }

    /**
     * Handle delegation from containing TopComponent.
     */
    @Override
    final public void componentActivated() {
	// Up to the client of TerminalContainer:
	// owner.componentActivated();
	activated = true;
        JComponent comp = getSelected();
	if (comp != null) {
	    CallBacks cb = attributesFor(comp).cb;
	    if (cb != null)
		cb.activated();
	}
    }

    /**
     * Handle delegation from containing TopComponent.
     */
    @Override
    final public void componentDeactivated() {
	// Up to the client of TerminalContainer:
	// owner.componentDeactivated();
	activated = false;
        JComponent comp = getSelected();
	if (comp != null) {
	    CallBacks cb = attributesFor(comp).cb;
	    if (cb != null)
		cb.deactivated();
	}
    }

    //
    // Overrides of IOContainer.Provider
    //
    @Override
    final public void open() {
	if (owner != null)
	    owner.open();
    }

    @Override
    final public void requestActive() {
	if (owner != null)
	    owner.requestActive();
    }

    @Override
    final public void requestVisible() {
	if (owner != null)
	    owner.requestVisible();
    }

    @Override
    final public boolean isActivated() {
	return activated;
    }

    @Override
    final public void add(JComponent comp, CallBacks cb) {
	addTab(comp, cb);
    }

    @Override
    final public void remove(JComponent comp) {
	removeTab(comp);
    }

    @Override
    final public void select(JComponent comp) {
	selectLite(comp);
    }

    @Override
    public void setToolbarActions(JComponent comp, Action[] toolbarActions) {
	// Remember in attributes
	// They get recalled when this comp is selected
	//
	// output2 remembers the actions in a client property.
	// SHOULD consider migration of components from one type
	// of container to another?
	Attributes attrs = attributesFor(comp);
	attrs.toolbarActions = toolbarActions;

	// pass-through for currently visible component
	if (getSelected() == comp)
            setButtons(toolbarActions);
    }

    @Override
    public void setTitle(JComponent comp, String title) {
	/* OLD
	if (title == null)
	    title = originalName;
	*/

	// Remember title in attributes
	// It gets recalled when we switch from tabbed to soleComponent mode
	Attributes attrs = attributesFor(comp);
	attrs.title = title;

	// output2 uses the name property of the JComponent to
	// remember the title.
	// So do we for good measure.
	comp.setName(title);

	if (!contains(comp))
	    return;

	setTitleWork(comp, title);
    }

    @Override
    public void setToolTipText(JComponent comp, String text) {
	// Remember tip text in attributes
	// It gets recalled when this comp is re-added to the tabbedPane
	//
	// output2 remembers the tip text in te toolTipText property of
	// the JComponent itself.
	Attributes attrs = attributesFor(comp);
	attrs.toolTipText = text;

	// pass-through for currently visible component
	restoreAttrsFor(comp);
    }

    @Override
    public void setIcon(JComponent comp, Icon icon) {
	// Remember icon in attributes
	// It gets recalled when this comp is re-added to the tabbedPane
	//
	// output2 remembers the icon in a client property.
	Attributes attrs = attributesFor(comp);
	attrs.icon = icon;

	// pass-through for currently visible component
	restoreAttrsFor(comp);
    }

    @Override
    final public boolean isCloseable(JComponent comp) {
	CallBacks cb = attributesFor(comp).cb;
	if (cb != null && IOVisibilityControl.isSupported(cb)) {
	    return IOVisibilityControl.isClosable(cb);
	} else {
	    return true;
	}
    }

    //
    // Overrides of JComponent
    //

    @Override
    public void requestFocus() {
	// redirect focus into terminal
	JComponent selected = getSelected();
	if (selected != null) {
	    selected.requestFocus();
	} else {
	    super.requestFocus();
	}
    }

    @Override
    public boolean requestFocusInWindow() {
	// redirect focus into terminal
	JComponent selected = getSelected();
	if (selected != null) {
	    return selected.requestFocusInWindow();
	} else {
	    return super.requestFocusInWindow();
	}
    }


    //
    // Methods to be implemented in implementation
    //
    abstract protected void addTabWork(JComponent comp);

    abstract protected void removeTabWork(JComponent comp);

    abstract protected void setTitleWork(JComponent comp, String title);

    abstract protected boolean contains(JComponent comp);

    abstract protected void restoreAttrsFor(JComponent comp);

    abstract protected void selectLite(JComponent comp);

    @Override
    abstract public JComponent getSelected();

    //
    // Implementation support
    //
    final protected void addTab(JComponent comp, CallBacks cb) {
	Attributes attr = attributesFor(comp);
	attr.cb = cb;

	addTabWork(comp);
    }

    final protected void removeTab(JComponent comp) {
	if (comp == null)
	    return;

	// SHOULD check if callers of this function assume that it
	// always succeeds.

	CallBacks cb = attributesFor(comp).cb;

	if (cb != null && IOVisibilityControl.isSupported(cb)) {
	    // SHOULD only check closability if request comes from UI!
	    if (IOVisibilityControl.isClosable(cb)) {
		if (! IOVisibilityControl.okToClose(cb))
		    return;		// close got vetoed.
	    } else {
		// Should usually not get here because all relevant
		// actions or their peformers should've been disabled.
		// SHOULD emit a warning
		assert false;
		return;
	    }

	}
	removeTabWork(comp);

	if (cb != null)
	    cb.closed();
    }

    protected void initComponents() {
        setLayout(new BorderLayout());

        actionBar = new JToolBar();
        actionBar.setOrientation(JToolBar.VERTICAL);
        actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.Y_AXIS));
        actionBar.setFloatable(false);
        fixSize(actionBar);
        add(actionBar, BorderLayout.WEST);

	// Make actionBar initially invisible. setButtons will make it visible
	// if actions are defined.
	// This will prevent 'blinking' of the toolbar (see IZ 233206)
	actionBar.setVisible(false);

        findBar = new FindBar(new FindBar.Owner() {

	    @Override
            public void close(FindBar fb) {
                findBar.getState().setVisible(false);
                // OLD TerminalContainerImpl.super.remove(findBar);
                componentRemove(findBar);
                validate();
            }
        });

    }

    protected final void componentRemove(JComponent comp) {
	super.remove(comp);
    }

    /**
     * Update out containing TC's window name.
     * @param title
     */
    final protected void updateWindowName(String title) {
	if (owner == null)
	    return;

	if (title == null || title.trim().isEmpty()) {
	    // sole or no component
	    owner.setDisplayName(originalName);
	    owner.setToolTipText(originalName);
	    owner.setHtmlDisplayName(null);

	} else {
	    String composite  = originalName + " - ";	// NOI18N
	    if (title.contains("<html>")) {		// NOI18N
		// pull the "<html>" to the beginning of the string
		title = title.replace("<html>", "");		// NOI18N
		composite = "<html> " + composite + title;// NOI18N
		owner.setHtmlDisplayName(composite);
	    } else {
		owner.setDisplayName(composite);
		owner.setHtmlDisplayName(null);
	    }
	    owner.setToolTipText(composite);
	}
    }

    /**
     * A new component has been selected.
     * Update anything that needs to be updated.
     */

    final protected void checkSelectionChange() {
	// outptu2 calls this checkTabSelChange().
	JComponent selection = getSelected();
	if (selection != lastSelection) {
	    lastSelection = selection;
	    updateBars(selection);
	    if (selection != null) {
		// This is the case when we remove the last component
		CallBacks cb = attributesFor(selection).cb;
		if (cb != null)
		    cb.selected();
	    }
	    // LATER update findstate
	    // LATER not sure what the following does:
	    // LATER getActionMap().setParent(sel != null ? sel.getActionMap() : null);
	}
    }

    private void setFindBar(FindState findState) {
        findBar.setState(findState);
        if (findState != null && findState.isVisible()) {
            add(findBar, BorderLayout.SOUTH);
        } else {
            // OLD super.remove(findBar);
            componentRemove(findBar);
        }
        validate();
    }

    private void fixSize(JToolBar actionBar) {
        Insets ins = actionBar.getMargin();
        JButton dummy = new JButton();
        dummy.setBorderPainted(false);
        dummy.setOpaque(false);
        dummy.setText(null);
        dummy.setIcon(new Icon() {

	    @Override
            public int getIconHeight() {
                return 16;
            }

	    @Override
            public int getIconWidth() {
                return 16;
            }

            @SuppressWarnings(value = "empty-statement")
	    @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                ;
            }
        });
        actionBar.add(dummy);
        Dimension buttonPref = dummy.getPreferredSize();
        Dimension minDim = new Dimension(buttonPref.width + ins.left + ins.right, buttonPref.height + ins.top + ins.bottom);
        actionBar.setMinimumSize(minDim);
        actionBar.setPreferredSize(minDim);
        actionBar.remove(dummy);
    }

    private JButton adjustButton(JButton b) {
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setText(null);
        b.putClientProperty("hideActionText", Boolean.TRUE);	// NOI18N
        // NOI18N
        return b;
    }

    private void setButtons(Action[] actions) {
	if (actions == null)
	    actions = new Action[0];
        JButton[] buttons = new JButton[actions.length];
        for (int ax = 0; ax < actions.length; ax++) {
            Action a = actions[ax];
            JButton b = new JButton(a);
            buttons[ax] = adjustButton(b);
        }

        actionBar.removeAll();
	if (buttons.length != 0) {
	    actionBar.setVisible(true);
	    for (JButton b : buttons) {
		actionBar.add(b);
	    }
	} else {
	    actionBar.setVisible(false);
	}
        actionBar.revalidate();
        actionBar.repaint();
    }

    private void updateBars(JComponent comp) {
	if (comp != null) {
	    Attributes attrs = attributesFor(comp);
	    setButtons(attrs.toolbarActions);
	    setFindBar(attrs.findState);
	} else {
	    setButtons(null);
	    setFindBar(null);
	}
    }

    /* OLD
    void find(Terminal who) {
        FindState findState = who.getFindState();
        if (findState.isVisible()) {
            return;
        }
        findState.setVisible(true);
        findBar.setState(findState);
        add(findBar, BorderLayout.SOUTH);
        validate();
    }
     */

}
