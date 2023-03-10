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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.nativeexecution.support.ui.api;

import org.netbeans.modules.nativeexecution.support.ui.Completable;
import org.netbeans.modules.nativeexecution.support.ui.CompletionPopup;
import java.awt.AWTKeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.openide.util.RequestProcessor;

/**
 *
 * @author ak119685
 */
public final class FileSelectorField extends JTextField
        implements Completable, PopupMenuListener {

    private final static RequestProcessor rp = new RequestProcessor("FileSelectorField", 3); // NOI18N
    private final AtomicReference<CompletionTask> currentTask;
    private boolean listenersInactive = false;
    private AutocompletionProvider provider;
    private CompletionPopup popup;

    public FileSelectorField() {
        this(null);
    }

    public FileSelectorField(AutocompletionProvider provider) {
        super();
        this.provider = provider;

        currentTask = new AtomicReference<>();

        getDocument().addDocumentListener(new DocumentListener() {

            private void doUpdate() {
                if (!listenersInactive) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            updateCompletions();
                        }
                    });
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                doUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                doUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent evt) {
                int keyCode = evt.getKeyCode();
                if ((keyCode == KeyEvent.VK_RIGHT && isAtLastPosition())
                        || keyCode == KeyEvent.VK_TAB) {
                    if (popup == null || !popup.isShowing()) {
                        updateCompletions();
                    }
                }
            }
        });
    }

    private void updateCompletions() {
        if (provider == null) {
            return;
        }

        if (!isFocusOwner()) {
            return;
        }

        if (popup == null) {
            popup = new CompletionPopup(this);
            popup.addPopupMenuListener(this);
        }

        String text = getText();
        int pos = getCaretPosition();
        final String textToComplete = text.substring(0, Math.min(text.length(), pos));
        final CompletionTask newTask = new CompletionTask(textToComplete);

        CompletionTask old = currentTask.getAndSet(newTask);

        if (old != null) {
            old.cancel();
        }

        rp.post(newTask);
        popup.setWaiting();

        if (!popup.isShowing()) {
            popup.showPopup(this, 0, getHeight() - 2);
        }
    }

    private boolean isAtLastPosition() {
        return getCaretPosition() >= (getDocument().getLength() - 1);
    }

    public void setAutocompletionProvider(AutocompletionProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean completeWith(final String completion) {
        String orig = getText().substring(0, getCaretPosition());
        String newValue;
        if (orig.startsWith("/") || orig.startsWith(".") || orig.startsWith("~")) { // NOI18N
            newValue = orig.substring(0, orig.lastIndexOf('/') + 1) + completion;
        } else {
            newValue = completion;
        }

        boolean updateCompletions = newValue.endsWith("/"); // NOI18N
        setText(newValue, updateCompletions);
        return updateCompletions;
    }

    public void setText(String text, boolean updateCompletions) {
        if (updateCompletions) {
            super.setText(text);
        } else {
            listenersInactive = true;
            super.setText(text);
            listenersInactive = false;
        }
    }

    private void setTabTraversalEnabled(boolean enabled) {
        Set<AWTKeyStroke> tKeys = getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        Set<AWTKeyStroke> newTKeys = new HashSet<>(tKeys);
        if (enabled) {
            newTKeys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
        } else {
            newTKeys.remove(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
        }
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newTKeys);
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        setTabTraversalEnabled(false);
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        setTabTraversalEnabled(true);
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    private class CompletionTask implements Runnable {

        private volatile boolean cancelled;
        private final String textToComplete;

        public CompletionTask(String textToComplete) {
            this.textToComplete = textToComplete;
        }

        void cancel() {
            cancelled = true;
        }

        @Override
        public void run() {
            final List<String> result = provider.autocomplete(textToComplete);
            if (!cancelled) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        popup.setOptionsList(result);
                    }
                });

            }
        }
    }
}
