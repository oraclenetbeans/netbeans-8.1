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

package org.netbeans.modules.terminal.ioprovider;

import java.awt.Color;
import java.io.CharConversionException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.Action;
import javax.swing.Icon;

import javax.swing.SwingUtilities;
import org.netbeans.lib.terminalemulator.ActiveRegion;
import org.openide.util.Exceptions;

import org.openide.windows.IOContainer;
import org.openide.windows.IOSelect;
import org.openide.windows.OutputListener;
import org.openide.xml.XMLUtil;

import org.netbeans.lib.terminalemulator.Coord;
import org.netbeans.lib.terminalemulator.LineDiscipline;

/**
 * Perform a Task on the EDT.
 * @author ivan
 */

/* package */ abstract class Task {

    /**
     * Performs a computation on the EDT. The value can be retrieved
     * using the get() method which uses futures until the value is available.
     * @param <V>
     */
    public static abstract class ValueTask<V> extends Task implements Callable<V> {
	private final FutureTask<V> future;

	protected  ValueTask(Terminal terminal) {
	    super(terminal);
	    this.future = new FutureTask<V>(this);
	}

	@Override
	abstract public V call();

	@Override
	protected final void perform() {
	    future.run();
	}

	@Override
	protected boolean isValueTask() {
	    return true;
	}

	public V get() {
	    try {
		return future.get();
	    } catch (InterruptedException ex) {
		Exceptions.printStackTrace(ex);
		return null;
	    } catch (ExecutionException ex) {
		Exceptions.printStackTrace(ex);
		return null;
	    }
	}
    }

    private static int scheduledTasks;

    private final IOContainer container;
    private final Terminal terminal;

    /**
     * Schedule this task to be performed on the EDT, or perform it now.
     */
    public final void post() {

	scheduledTasks++;

	if (! SwingUtilities.isEventDispatchThread()) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		    dispatch();
		}
	    });
	    return;
	} else {
	    dispatch();
	}
    }

    private void dispatch() {
	try {
	    if (terminal().isDisposed() && ! isValueTask()) {
		// closeInputOutput has been called
		return;
	    } else {
		perform();
	    }
	} finally {
	    scheduledTasks--;
	}
    }

    static boolean isQuiescent() {
	return scheduledTasks == 0;
    }

    protected abstract void perform();

    protected boolean isValueTask() {
	return false;
    }

    /**
     * Common task that involves both container and Terminal.
     * @param container
     * @param terminal
     */
    protected Task(IOContainer container, Terminal terminal) {
	this.container = container;
	this.terminal = terminal;
    }

    /**
     * Common task that only involves the Terminal.
     * @param terminal
     */
    protected Task(Terminal terminal) {
	this.container = null;
	this.terminal = terminal;
    }

    protected final IOContainer container() {
	return container;
    }

    protected final Terminal terminal() {
	return terminal;
    }


    static class Add extends Task {
	private final Action[] actions;

	public Add(IOContainer container, Terminal terminal, Action[] actions) {
	    super(container, terminal);
	    this.actions = actions;
	}

	@Override
	public void perform() {
	    // It's important to add first because otherwise output2's
	    // container impl will assert.
	    container().add(terminal(), terminal().callBacks());

	    container().setToolbarActions(terminal(), actions);
	    terminal().setVisibleInContainer(true);
	    /* OLD bug #181064
	    container().open();
	    container().requestActive();
	     */
	    /* OLD
	    // output2 tacks on this " ".
	    // If anything it protects against null names.
	    terminal().setTitle(terminal().name() + " ");	// NOI18N
	     */
	    terminal().updateName(terminal().name());

	    // TMP container().add(terminal(), terminal().callBacks());
	}
    }

    static class Select extends Task {
	private final Set<IOSelect.AdditionalOperation> extraOps;

	public Select(IOContainer container, Terminal terminal,
		      Set<IOSelect.AdditionalOperation> extraOps) {
	    super(container, terminal);
	    this.extraOps = extraOps;
	}

	@Override
	public void perform() {
	    if (terminal().isDisposed())
		return;

	    terminal().setClosedUnconditionally(false);

	    if (!terminal().isVisibleInContainer()) {
		container().add(terminal(), terminal().callBacks());
		terminal().setVisibleInContainer(true);
	    }
	    if (extraOps != null) {
		if (extraOps.contains(IOSelect.AdditionalOperation.OPEN))
		    container().open();
		if (extraOps.contains(IOSelect.AdditionalOperation.REQUEST_VISIBLE))
		    container().requestVisible();
		if (extraOps.contains(IOSelect.AdditionalOperation.REQUEST_ACTIVE))
		    container().requestActive();
	    }
	    container().select(terminal());
	}
    }

    static class DeSelect extends Task {

	public DeSelect(IOContainer container, Terminal terminal) {
	    super(container, terminal);
	}

	@Override
	public void perform() {
	    container().setToolbarActions(terminal(), new Action[0]);
	    terminal().closeUnconditionally();
	}
    }

    static class StrongClose extends Task {

	public StrongClose(IOContainer container, Terminal terminal) {
	    super(container, terminal);
	}

	@Override
	public void perform() {
	    terminal().closeUnconditionally();
	    terminal().dispose();
	}
    }

    static class UpdateName extends Task {

	public UpdateName(IOContainer container, Terminal terminal) {
	    super(container, terminal);
	}

	@Override
	public void perform() {
	    if (!terminal().isVisibleInContainer()) {
		return ;
	    }
	    String newTitle = terminal().getTitle();
	    if (terminal().isConnected() && newTitle != null) {
		String escaped;
		try {
		    escaped = XMLUtil.toAttributeValue(newTitle);
		} catch (CharConversionException ex) {
		    escaped = newTitle;
		}

		newTitle = "<html><b>" + escaped + "</b></html>";	// NOI18N
	    }
	    container().setTitle(terminal(), newTitle);
	}
    }

    static class SetIcon extends Task {
	private final Icon icon;

	public SetIcon(IOContainer container, Terminal terminal, Icon icon) {
	    super(container, terminal);
	    this.icon = icon;
	}

	@Override
	public void perform() {
	    container().setIcon(terminal(), icon);
	}
    }

    static class SetToolTipText extends Task {
	private final String text;

	public SetToolTipText(IOContainer container, Terminal terminal, String text) {
	    super(container, terminal);
	    this.text = text;
	}

	@Override
	public void perform() {
	    container().setToolTipText(terminal(), text);
	}
    }

    static class Scroll extends Task {
	private final Coord coord;

	public Scroll(Terminal terminal, Coord coord) {
	    super(terminal);
	    this.coord = coord;
	}

	@Override
	public void perform() {
	    terminal().scrollTo(coord);
	}
    }

    static class SetClosable extends Task {
	private final boolean closable;

	public SetClosable(IOContainer container, Terminal terminal, boolean closable) {
	    super(container, terminal);
	    this.closable = closable;
	}

	@Override
	public void perform() {
	    terminal().setClosable(closable);
	}
    }

    static class SetDisciplined extends Task {
	private final boolean disciplined;

	public SetDisciplined(Terminal terminal, boolean disciplined) {
	    super(terminal);
	    this.disciplined = disciplined;
	}

	@Override
	public void perform() {
	    if (disciplined)
		terminal().term().pushStream(new LineDiscipline());
	}
    }

    static class SetCustomColor extends Task {
	private final int index;
	private final Color color;

	public SetCustomColor(Terminal terminal, int index, Color color) {
	    super(terminal);
	    this.index = index;
	    this.color = color;
	}

	@Override
	protected void perform() {
	    terminal().term().setCustomColor(index, color);
	}
    }

    static class ClearHistory extends Task {
	public ClearHistory(Terminal terminal) {
	    super(terminal);
	}

	@Override
	protected void perform() {
	    terminal().term().clearHistory();
	}
    }

    static class Connect extends Task {
	private final OutputStream pin;
	private final InputStream pout;
	private final InputStream perr;
	private final String charset;

	public Connect(Terminal terminal,
		       OutputStream pin, InputStream pout, InputStream perr, String charset) {
	    super(terminal);
	    this.pin = pin;
	    this.pout = pout;
	    this.perr = perr;
	    this.charset = charset;
	}

	@Override
	protected void perform() {
	    terminal().term().connect(pin, pout, perr, charset); // NOI18N
	    terminal().setExtConnected(true);
	}
    }

    static class Disconnect extends Task {
	private final Runnable continuation;

	public Disconnect(Terminal terminal, Runnable continuation) {
	    super(terminal);
	    this.continuation = continuation;
	}

	@Override
	protected void perform() {
	    // Wrap 'continuation' in another one so we can
	    // set the extConnected state at the right time.
	    terminal().term().disconnect(new Runnable() {
		@Override
		public void run() {
		    terminal().setExtConnected(false);
		    if (continuation != null)
			continuation.run();
		}
	    });
	}
    }

    static class BeginActiveRegion extends Task {
	private final OutputListener listener;

	public BeginActiveRegion(Terminal terminal,
		                 OutputListener listener) {
	    super(terminal);
	    this.listener = listener;
	}

	@Override
	protected void perform() {
            ActiveRegion ar = terminal().term().beginRegion(true);
            ar.setUserObject(listener);
            ar.setLink(true);
	}
    }

    static class EndActiveRegion extends Task {
	public EndActiveRegion(Terminal terminal) {
	    super(terminal);
	}

	@Override
	protected void perform() {
            terminal().term().endRegion();
	}
    }


    static class GetPosition extends ValueTask<Coord> implements Callable<Coord> {
	public GetPosition(Terminal terminal) {
	    super(terminal);
	}

	@Override
	public Coord call() {
	    return terminal().term().getCursorCoord();
	}
    }

    static class GetOut extends ValueTask<Writer> implements Callable<Writer> {
	public GetOut(Terminal terminal) {
	    super(terminal);
	}

	@Override
	public Writer call() {
	    return terminal().term().getOut();
	}
    }

    static class GetIn extends ValueTask<Reader> implements Callable<Reader> {
	public GetIn(Terminal terminal) {
	    super(terminal);
	}

	@Override
	public Reader call() {
	    return terminal().term().getIn();
	}
    }

    static class IsClosable extends ValueTask<Boolean> implements Callable<Boolean> {

	public IsClosable(Terminal terminal) {
	    super(terminal);
	}

	@Override
	public Boolean call() {
	    return terminal().isClosable();
	}
    }
}