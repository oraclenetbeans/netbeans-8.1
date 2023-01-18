/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.terminal.actions;

import java.awt.Dialog;
import java.awt.Dimension;
import javax.swing.Action;
import org.netbeans.lib.terminalemulator.Term;
import org.netbeans.lib.terminalemulator.TermListener;
import org.netbeans.modules.terminal.PinPanel;
import org.netbeans.modules.terminal.ioprovider.Terminal;
import org.netbeans.modules.terminal.support.TerminalPinSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author igromov
 */
@ActionID(id = ActionFactory.PIN_TAB_ACTION_ID, category = ActionFactory.CATEGORY)
@ActionRegistration(displayName = "#CTL_PinTab", lazy = true) //NOI18N
@ActionReferences({
    @ActionReference(path = ActionFactory.ACTIONS_PATH, name = "PinTabAction") //NOI18N
})
public class PinTabAction extends TerminalAction {

    public static final String pinMessage = getMessage("CTL_PinTab");
    public static final String unpinMessage = getMessage("CTL_UnpinTab");

    private final TerminalPinSupport support = TerminalPinSupport.getDefault();

    public PinTabAction(Terminal context) {
	super(context);

	final Terminal terminal = getTerminal();

	putValue(NAME, getMessage(terminal.isPinned()));
	putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
    }

    @Override
    public void performAction() {
	final Terminal terminal = getTerminal();
	final Term term = terminal.term();

	TerminalPinSupport.TerminalPinningDetails pinningDetails = support.findPinningDetails(term);
	boolean oldState = pinningDetails == null ? false : pinningDetails.isPinned();
	boolean newState = !oldState;
	
	putValue(NAME, getMessage(!terminal.isPinned()));

	if (newState) {
	    boolean customTitle = terminal.isCustomTitle();
	    String title = terminal.getTitle();
	    String name = terminal.name();
	    TerminalPinSupport.TerminalPinningDetails defaultValues = TerminalPinSupport.TerminalPinningDetails.create(customTitle, customTitle ? title : name, terminal.getCwd(), enabled);
	    PinPanel pinPanel = new PinPanel(new TerminalPinSupport.TerminalDetails(support.findCreationDetails(term), defaultValues));

	    DialogDescriptor dd = new DialogDescriptor(
		    pinPanel,
		    NbBundle.getMessage(Terminal.class, "LBL_PinTab"),
		    true,
		    DialogDescriptor.OK_CANCEL_OPTION,
		    DialogDescriptor.OK_OPTION,
		    null
	    );

	    Dialog cfgDialog = DialogDisplayer.getDefault().createDialog(dd);

	    try {
		cfgDialog.setVisible(true);
	    } catch (Throwable th) {
		if (!(th.getCause() instanceof InterruptedException)) {
		    throw new RuntimeException(th);
		}
		dd.setValue(DialogDescriptor.CANCEL_OPTION);
	    } finally {
		cfgDialog.dispose();
	    }

	    if (dd.getValue() != DialogDescriptor.OK_OPTION) {
		return;
	    }

	    String chosenTitle = pinPanel.getTitle();
	    boolean chosenIsCustom = pinPanel.isCustomTitle();
	    String chosenDirectory = pinPanel.getDirectory();

	    if (chosenDirectory.isEmpty()) {
		chosenDirectory = null;
	    }

	    support.tabWasPinned(
		    term,
		    TerminalPinSupport.TerminalPinningDetails.create(
			    chosenIsCustom,
			    chosenIsCustom ? chosenTitle : name,
			    chosenDirectory,
			    enabled
		    )
	    );

	    if (chosenIsCustom && !title.equals(chosenTitle)) {
		customTitle = true;
		terminal.updateName(chosenTitle);
	    }
	} else {
	    support.tabWasUnpinned(term);
	}

	terminal.pin(newState);
    }

    public static String getMessage(boolean isPinned) {
	return isPinned ? unpinMessage : pinMessage;
    }

    // --------------------------------------------- 
    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
	return new PinTabAction(actionContext.lookup(Terminal.class));
    }
}
