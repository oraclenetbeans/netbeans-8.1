/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.bugtracking.tasks.dashboard;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.netbeans.modules.team.commons.treelist.LinkButton;
import org.netbeans.modules.team.commons.ColorManager;
import org.netbeans.modules.team.commons.treelist.LeafNode;
import org.netbeans.modules.team.commons.treelist.TreeLabel;
import org.netbeans.modules.team.commons.treelist.TreeListNode;

/**
 *
 * @author jpeska
 */
public class EmptyContentNode extends LeafNode {

    private final LinkButton linkButton;
    private final String message;
    private final Object LOCK = new Object();
    private JPanel panel;
    private TreeLabel lblMessage;

    public EmptyContentNode(TreeListNode parent, String message, LinkButton linkButton) {
        super(parent);
        this.message = message;
        this.linkButton = linkButton;
    }

    public EmptyContentNode(TreeListNode parent, String message) {
        this(parent, message, null);
    }

    public EmptyContentNode(TreeListNode parent, LinkButton linkButton) {
        this(parent, "", linkButton);
    }

    @Override
    protected JComponent getComponent(Color foreground, Color background, boolean isSelected, boolean hasFocus, int rowWidth) {
        synchronized (LOCK) {
            if (null == panel) {
                panel = new JPanel(new GridBagLayout());
                panel.setBorder(new EmptyBorder(0, 0, 0, 0));
                panel.setOpaque(false);
                if (message != null && !message.isEmpty()) {
                    lblMessage = new TreeLabel(message);
                    lblMessage.setBorder(new EmptyBorder(0, 0, 0, 5));
                    panel.add(lblMessage, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 3), 0, 0));
                }
                if (linkButton != null) {
                    panel.add(linkButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 3), 0, 0));
                }
                panel.add(new JLabel(), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 30), 0, 0));
            }
        }
        if (lblMessage != null) {
            lblMessage.setForeground(isSelected ? foreground : ColorManager.getDefault().getDisabledColor());
        }
        if (linkButton != null) {
            linkButton.setForeground(foreground, isSelected);
        }
        return panel;
    }
}
