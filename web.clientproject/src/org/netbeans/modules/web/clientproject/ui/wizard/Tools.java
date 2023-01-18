/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.clientproject.ui.wizard;

import java.awt.EventQueue;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeListener;
import org.openide.awt.Mnemonics;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public class Tools extends JPanel implements HelpCtx.Provider {

    private final ChangeSupport changeSupport = new ChangeSupport(this);


    public Tools() {
        assert EventQueue.isDispatchThread();
        initComponents();
        init();
    }

    private void init() {
        ItemListener defaultItemListener = new DefaultItemListener();
        packageJsonCheckBox.addItemListener(defaultItemListener);
        bowerJsonCheckBox.addItemListener(defaultItemListener);
        gruntfileCheckBox.addItemListener(defaultItemListener);
        gulpfileCheckBox.addItemListener(defaultItemListener);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("org.netbeans.modules.web.clientproject.ui.wizard.Tools"); // NOI18N
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    public boolean isNpmEnabled() {
        return packageJsonCheckBox.isSelected();
    }

    public void setNpmEnabled(boolean enabled) {
        packageJsonCheckBox.setSelected(enabled);
    }

    public boolean isBowerEnabled() {
        return bowerJsonCheckBox.isSelected();
    }

    public void setBowerEnabled(boolean enabled) {
        bowerJsonCheckBox.setSelected(enabled);
    }

    public boolean isGruntEnabled() {
        return gruntfileCheckBox.isSelected();
    }

    public void setGruntEnabled(boolean enabled) {
        gruntfileCheckBox.setSelected(enabled);
    }

    public boolean isGulpEnabled() {
        return gulpfileCheckBox.isSelected();
    }

    public void setGulpEnabled(boolean enabled) {
        gulpfileCheckBox.setSelected(enabled);
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
     * Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        packageJsonCheckBox = new JCheckBox();
        bowerJsonCheckBox = new JCheckBox();
        gruntfileCheckBox = new JCheckBox();
        gulpfileCheckBox = new JCheckBox();

        packageJsonCheckBox.setSelected(true);
        Mnemonics.setLocalizedText(packageJsonCheckBox, NbBundle.getMessage(Tools.class, "Tools.packageJsonCheckBox.text")); // NOI18N

        bowerJsonCheckBox.setSelected(true);
        Mnemonics.setLocalizedText(bowerJsonCheckBox, NbBundle.getMessage(Tools.class, "Tools.bowerJsonCheckBox.text")); // NOI18N

        gruntfileCheckBox.setSelected(true);
        Mnemonics.setLocalizedText(gruntfileCheckBox, NbBundle.getMessage(Tools.class, "Tools.gruntfileCheckBox.text")); // NOI18N

        gulpfileCheckBox.setSelected(true);
        Mnemonics.setLocalizedText(gulpfileCheckBox, NbBundle.getMessage(Tools.class, "Tools.gulpfileCheckBox.text")); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(packageJsonCheckBox)
            .addComponent(bowerJsonCheckBox)
            .addComponent(gruntfileCheckBox)
            .addComponent(gulpfileCheckBox)
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(packageJsonCheckBox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bowerJsonCheckBox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gruntfileCheckBox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gulpfileCheckBox))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox bowerJsonCheckBox;
    private JCheckBox gruntfileCheckBox;
    private JCheckBox gulpfileCheckBox;
    private JCheckBox packageJsonCheckBox;
    // End of variables declaration//GEN-END:variables

    //~ Inner classes

    private final class DefaultItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            fireChange();
        }

    }

}