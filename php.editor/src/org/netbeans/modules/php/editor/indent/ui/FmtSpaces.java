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

package org.netbeans.modules.php.editor.indent.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import org.netbeans.modules.options.editor.spi.PreferencesCustomizer;
import org.netbeans.modules.php.editor.indent.FmtOptions;
import static org.netbeans.modules.php.editor.indent.FmtOptions.*;
import org.openide.util.NbBundle;

/**
 *
 * @author  phrebejk
 */
public final class FmtSpaces extends JPanel implements TreeCellRenderer, MouseListener, KeyListener {

    private static final Logger LOGGER = Logger.getLogger(FmtSpaces.class.getName());

    private SpacesCategorySupport scs;
    private DefaultTreeModel model;

    private DefaultTreeCellRenderer dr = new DefaultTreeCellRenderer();
    private JCheckBox renderer = new JCheckBox();

    /** Creates new form FmtSpaces */
    private FmtSpaces() {
        initComponents();
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(20);
        model = createModel();
        cfgTree.setModel(model);
        cfgTree.setRootVisible(false);
        cfgTree.setShowsRootHandles(true);
        cfgTree.setCellRenderer(this);
        cfgTree.setEditable(false);
        cfgTree.addMouseListener(this);
        cfgTree.addKeyListener(this);

        dr.setIcon(null);
        dr.setOpenIcon(null);
        dr.setClosedIcon(null);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        for (int i = root.getChildCount(); i >= 0; i--) {
            cfgTree.expandRow(i);
        }

        Dimension dimension = new Dimension((int) cfgTree.getPreferredSize().getWidth() + Utils.POSSIBLE_SCROLL_BAR_WIDTH, (int) jScrollPane1.getMinimumSize().getHeight());
        jScrollPane1.setMinimumSize(dimension);
    }

    public static PreferencesCustomizer.Factory getController() {
        return new PreferencesCustomizer.Factory() {
            @Override
            public PreferencesCustomizer create(Preferences preferences) {
                String preview = "";
                try {
                    preview = Utils.loadPreviewText(FmtTabsIndents.class.getClassLoader().getResourceAsStream("org/netbeans/modules/php/editor/indent/ui/Spaces.php"));
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
                return new SpacesCategorySupport(preferences, new FmtSpaces(), preview);
            }
        };
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPane1 = new javax.swing.JScrollPane();
        cfgTree = new javax.swing.JTree();

        setName(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "LBL_Spaces")); // NOI18N
        setOpaque(false);
        setLayout(new java.awt.GridBagLayout());

        cfgTree.setRootVisible(false);
        jScrollPane1.setViewportView(cfgTree);
        cfgTree.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "FmtSpaces.cfgTree.AccessibleContext.accessibleName")); // NOI18N
        cfgTree.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "FmtSpaces.cfgTree.AccessibleContext.accessibleDescription")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jScrollPane1, gridBagConstraints);
        jScrollPane1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "FmtSpaces.jScrollPane1.AccessibleContext.accessibleName")); // NOI18N
        jScrollPane1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "FmtSpaces.jScrollPane1.AccessibleContext.accessibleDescription")); // NOI18N

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "FmtSpaces.AccessibleContext.accessibleName")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(FmtSpaces.class, "FmtSpaces.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree cfgTree;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    // TreeCellRenderer implemetation ------------------------------------------

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        renderer.setBackground(selected ? dr.getBackgroundSelectionColor() : dr.getBackgroundNonSelectionColor());
        renderer.setForeground(selected ? dr.getTextSelectionColor() : dr.getTextNonSelectionColor());
        renderer.setEnabled(true);

        Object data = ((DefaultMutableTreeNode) value).getUserObject();

        if (data instanceof Item) {
            Item item = ((Item) data);

            if (((DefaultMutableTreeNode) value).getAllowsChildren()) {
                Component c = dr.getTreeCellRendererComponent(tree, value, leaf, expanded, leaf, row, hasFocus);
                return c;
            } else {
                renderer.setText(item.displayName);
                renderer.setSelected(item.value);
            }
        } else {
            Component c = dr.getTreeCellRendererComponent(tree, value, leaf, expanded, leaf, row, hasFocus);
            return c;
        }

        return renderer;
    }


    // MouseListener implementation --------------------------------------------

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        TreePath path = cfgTree.getPathForLocation(e.getPoint().x, e.getPoint().y);
        if (path != null) {
            Rectangle r = cfgTree.getPathBounds(path);
            if (r != null) {
                if (r.contains(p)) {
                    toggle(path);
                }
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    // KeyListener implementation ----------------------------------------------

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {

            if (e.getSource() instanceof JTree) {
                JTree tree = (JTree) e.getSource();
                TreePath path = tree.getSelectionPath();

                if (toggle(path)) {
                    e.consume();
                }
            }
        }
    }

    // Private methods ---------------------------------------------------------

    private DefaultTreeModel createModel() {

        Item[] categories = new Item[] {
            new Item("BeforeKeywords",                          // NOI18N
                new Item(SPACE_BEFORE_WHILE),
                new Item(SPACE_BEFORE_ELSE),
                new Item(SPACE_BEFORE_CATCH),
                new Item(SPACE_BEFORE_FINALLY)),

            new Item("BeforeParentheses",                       // NOI18N
                new Item(SPACE_BEFORE_METHOD_DECL_PAREN),
                new Item(SPACE_BEFORE_METHOD_CALL_PAREN),
                new Item(SPACE_BEFORE_IF_PAREN),
                new Item(SPACE_BEFORE_FOR_PAREN),
                new Item(SPACE_BEFORE_WHILE_PAREN),
                new Item(SPACE_BEFORE_CATCH_PAREN),
                new Item(SPACE_BEFORE_SWITCH_PAREN),
                new Item(SPACE_BEFORE_ARRAY_DECL_PAREN)
                ),

            new Item("AroundOperators",                         // NOI18N
                new Item(SPACE_AROUND_UNARY_OPS),
                new Item(SPACE_AROUND_BINARY_OPS),
                new Item(SPACE_AROUND_TERNARY_OPS),
                new Item(SPACE_AROUND_STRING_CONCAT_OPS),
                new Item(SPACE_AROUND_KEY_VALUE_OPS),
                new Item(SPACE_AROUND_ASSIGN_OPS),
                new Item(SPACE_AROUND_OBJECT_OPS)),

            new Item("BeforeLeftBraces",                        // NOI18N
                new Item(SPACE_BEFORE_CLASS_DECL_LEFT_BRACE),
                new Item(SPACE_BEFORE_METHOD_DECL_LEFT_BRACE),
                new Item(SPACE_BEFORE_IF_LEFT_BRACE),
                new Item(SPACE_BEFORE_ELSE_LEFT_BRACE),
                new Item(SPACE_BEFORE_WHILE_LEFT_BRACE),
                new Item(SPACE_BEFORE_FOR_LEFT_BRACE),
                new Item(SPACE_BEFORE_DO_LEFT_BRACE),
                new Item(SPACE_BEFORE_SWITCH_LEFT_BRACE),
                new Item(SPACE_BEFORE_TRY_LEFT_BRACE),
                new Item(SPACE_BEFORE_CATCH_LEFT_BRACE),
                new Item(SPACE_BEFORE_FINALLY_LEFT_BRACE),
                new Item(SPACE_BEFORE_USE_TRAIT_BODY_LEFT_BRACE)
                ),

            new Item("WithinParentheses",                       // NOI18N
                new Item(SPACE_WITHIN_METHOD_DECL_PARENS),
                new Item(SPACE_WITHIN_METHOD_CALL_PARENS),
                new Item(SPACE_WITHIN_IF_PARENS),
                new Item(SPACE_WITHIN_FOR_PARENS),
                new Item(SPACE_WITHIN_WHILE_PARENS),
                new Item(SPACE_WITHIN_SWITCH_PARENS),
                new Item(SPACE_WITHIN_CATCH_PARENS),
                new Item(SPACE_WITHIN_ARRAY_DECL_PARENS),
                new Item(SPACE_WITHIN_TYPE_CAST_PARENS),
                new Item(SPACE_WITHIN_ARRAY_BRACKETS)
            ),


             new Item("Other",                                  // NOI18N
                new Item(SPACE_BEFORE_COMMA),
                new Item(SPACE_AFTER_COMMA),
                new Item(SPACE_BEFORE_SEMI),
                new Item(SPACE_AFTER_SEMI),
                new Item(SPACE_AFTER_TYPE_CAST),
                new Item(SPACE_CHECK_AFTER_KEYWORDS),
                new Item(SPACE_AFTER_SHORT_PHP_TAG),
                new Item(SPACE_BEFORE_CLOSE_PHP_TAG))

        };


        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true); // NOI18N
        DefaultTreeModel dtm = new DefaultTreeModel(root);


        for (Item item : categories) {
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode(item, true);
            root.add(cn);
            for (Item si : item.items) {
                DefaultMutableTreeNode in = new DefaultMutableTreeNode(si, false);
                cn.add(in);
            }
        }

        return dtm;
    }

    private boolean toggle(TreePath treePath) {

        if (treePath == null) {
            return false;
        }

        Object o = ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();

        DefaultTreeModel dtm = (DefaultTreeModel) cfgTree.getModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();

        if (o instanceof Item) {
            Item item = (Item) o;

            if (node.getAllowsChildren()) {
                return false;
            }

            item.value = !item.value;
            dtm.nodeChanged(node);
            dtm.nodeChanged(node.getParent());
            scs.notifyChanged();
        }

        return false;
    }

    // Innerclasses ------------------------------------------------------------

    private static class Item {

        String id;
        String displayName;
        boolean value;
        Item[] items;

        public Item(String id, Item... items) {
            this.id = id;
            this.items = items;
            this.displayName = NbBundle.getMessage(FmtSpaces.class, "LBL_" + id); // NOI18N
        }

        @Override
        public String toString() {
            return displayName;
        }

    }

    private static final class SpacesCategorySupport extends FmtOptions.CategorySupport {

        public SpacesCategorySupport(Preferences preferences, FmtSpaces panel, String preview) {

            super(preferences, "spaces", panel, //NOI18N
                  preview); //,
//                  new String[] {FmtOptions.placeCatchOnNewLine, Boolean.FALSE.toString()},
//                  new String[] {FmtOptions.placeElseOnNewLine, Boolean.FALSE.toString()},
//                  new String[] {FmtOptions.placeWhileOnNewLine, Boolean.FALSE.toString()},
//                  new String[] {FmtOptions.placeFinallyOnNewLine, Boolean.FALSE.toString()} );
            panel.scs = this;
        }

        @Override
        protected void addListeners() {
            // Should not do anything
        }

        @Override
        protected void loadFrom(Preferences preferences) {
            for (Item item : getAllItems()) {
                boolean df = FmtOptions.getDefaultAsBoolean(item.id);
                item.value = preferences.getBoolean(item.id, df);
            }
        }

        @Override
        protected void storeTo(Preferences preferences) {
            for (Item item : getAllItems()) {
                boolean df = FmtOptions.getDefaultAsBoolean(item.id);
                if (df == item.value) {
                    preferences.remove(item.id);
                } else {
                    preferences.putBoolean(item.id, item.value);
                }
            }
        }

        private List<Item> getAllItems() {
            List<Item> result = new LinkedList<>();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((FmtSpaces) panel).model.getRoot();
            Enumeration children = root.depthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
                Object o = node.getUserObject();
                if (o instanceof Item) {
                    Item item = (Item) o;
                    if (item.items == null || item.items.length == 0) {
                        result.add(item);
                    }
                }
            }
            return result;
        }
    }
}
