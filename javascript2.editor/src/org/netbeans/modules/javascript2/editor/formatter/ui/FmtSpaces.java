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

package org.netbeans.modules.javascript2.editor.formatter.ui;

import org.netbeans.modules.javascript2.editor.formatter.Utils;
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
import org.netbeans.modules.javascript2.editor.formatter.FmtOptions;
import org.openide.util.NbBundle;
import static org.netbeans.modules.javascript2.editor.formatter.FmtOptions.*;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.formatter.Defaults;

/**
 *
 * @author  phrebejk
 */
public class FmtSpaces extends JPanel implements TreeCellRenderer, MouseListener, KeyListener {

    private SpacesCategorySupport scs;
    private DefaultTreeModel model;

    private DefaultTreeCellRenderer dr = new DefaultTreeCellRenderer();
    private JCheckBox renderer = new JCheckBox();

    /** Creates new form FmtSpaces */
    private FmtSpaces() {
        initComponents();
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

        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        for( int i = root.getChildCount(); i >= 0; i-- ) {
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
                    preview = Utils.loadPreviewText(FmtTabsIndents.class.getClassLoader().getResourceAsStream("org/netbeans/modules/javascript2/editor/formatter/ui/Spaces.js"));
                } catch (IOException ex) {
                    // TODO log it
                }
                return new SpacesCategorySupport(Defaults.getInstance(JsTokenId.JAVASCRIPT_MIME_TYPE),
                        preferences, new FmtSpaces(), preview);
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

        renderer.setBackground( selected ? dr.getBackgroundSelectionColor() : dr.getBackgroundNonSelectionColor() );
        renderer.setForeground( selected ? dr.getTextSelectionColor() : dr.getTextNonSelectionColor() );
        renderer.setEnabled( true );

        Object data = ((DefaultMutableTreeNode)value).getUserObject();

        if ( data instanceof Item ) {
            Item item = ((Item)data);

            if ( ((DefaultMutableTreeNode)value).getAllowsChildren() ) {
                Component c = dr.getTreeCellRendererComponent(tree, value, leaf, expanded, leaf, row, hasFocus);
                return c;
            }
            else {
                renderer.setText( item.displayName );
                renderer.setSelected( item.value );
            }
        }
        else {
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
        if ( path != null ) {
            Rectangle r = cfgTree.getPathBounds(path);
            if (r != null) {
                if ( r.contains(p)) {
                    toggle( path );
                }
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    // KeyListener implementation ----------------------------------------------

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER ) {

            if ( e.getSource() instanceof JTree ) {
                JTree tree = (JTree) e.getSource();
                TreePath path = tree.getSelectionPath();

                if ( toggle( path )) {
                    e.consume();
                }
            }
        }
    }

    // Private methods ---------------------------------------------------------

    private DefaultTreeModel createModel() {

        Item[] categories = new Item[] {
            new Item("BeforeKeywords",                          // NOI18N
                new Item(spaceBeforeWhile),
                new Item(spaceBeforeElse),
                new Item(spaceBeforeCatch),
                new Item(spaceBeforeFinally)),

            new Item("BeforeParentheses",                       // NOI18N
                new Item(spaceBeforeAnonMethodDeclParen),
                new Item(spaceBeforeMethodDeclParen),
                new Item(spaceBeforeMethodCallParen),
                new Item(spaceBeforeIfParen),
                new Item(spaceBeforeForParen),
                new Item(spaceBeforeWhileParen),
                new Item(spaceBeforeCatchParen),
                new Item(spaceBeforeSwitchParen),
                new Item(spaceBeforeWithParen)
                ),

            new Item("AroundOperators",                         // NOI18N
                new Item(spaceAroundUnaryOps),
                new Item(spaceAroundBinaryOps),
                new Item(spaceAroundTernaryOps),
//		new Item(spaceAroundStringConcatOps),
//		new Item(spaceAroundKeyValueOps),
                new Item(spaceAroundAssignOps)),
//		new Item(spaceAroundObjectOps)),

            new Item("BeforeLeftBraces",                        // NOI18N
//                new Item(spaceBeforeClassDeclLeftBrace),
                new Item(spaceBeforeMethodDeclLeftBrace),
                new Item(spaceBeforeIfLeftBrace),
                new Item(spaceBeforeElseLeftBrace),
                new Item(spaceBeforeWhileLeftBrace),
                new Item(spaceBeforeForLeftBrace),
                new Item(spaceBeforeDoLeftBrace),
                new Item(spaceBeforeSwitchLeftBrace),
                new Item(spaceBeforeTryLeftBrace),
                new Item(spaceBeforeCatchLeftBrace),
                new Item(spaceBeforeFinallyLeftBrace),
                new Item(spaceBeforeWithLeftBrace)
//                new Item(spaceBeforeSynchronizedLeftBrace),
//                new Item(spaceBeforeStaticInitLeftBrace),
//                new Item(spaceBeforeArrayInitLeftBrace) ),
                ),

            new Item("WithinParentheses",                       // NOI18N
                new Item(spaceWithinParens),
                new Item(spaceWithinMethodDeclParens),
                new Item(spaceWithinMethodCallParens),
                new Item(spaceWithinIfParens),
                new Item(spaceWithinForParens),
                new Item(spaceWithinWhileParens),
                new Item(spaceWithinSwitchParens),
                new Item(spaceWithinCatchParens),
                new Item(spaceWithinWithParens),
//                new Item(spaceWithinSynchronizedParens),
//		new Item(spaceWithinArrayDeclParens),
//                new Item(spaceWithinTypeCastParens),
//                new Item(spaceWithinAnnotationParens),
                new Item(spaceWithinBraces),
                new Item(spaceWithinArrayBrackets)
		),


             new Item("Other",                                  // NOI18N
                new Item(spaceBeforeComma),
                new Item(spaceAfterComma),
                new Item(spaceBeforeSemi),
                new Item(spaceAfterSemi),
                new Item(spaceBeforeColon),
                new Item(spaceAfterColon))

        };


        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true); // NOI18N
        DefaultTreeModel dtm = new DefaultTreeModel( root );


        for( Item item : categories ) {
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode( item, true );
            root.add(cn);
            for ( Item si : item.items ) {
                DefaultMutableTreeNode in = new DefaultMutableTreeNode( si, false );
                cn.add(in);
            }
        }

        return dtm;
    }

    private boolean toggle(TreePath treePath) {

        if( treePath == null ) {
            return false;
        }

        Object o = ((DefaultMutableTreeNode)treePath.getLastPathComponent()).getUserObject();

        DefaultTreeModel dtm = (DefaultTreeModel)cfgTree.getModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();

        if ( o instanceof Item ) {
            Item item = (Item)o;

            if ( node.getAllowsChildren() ) {
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
            this.displayName = NbBundle.getMessage(FmtSpaces.class, "LBL_" + id ); // NOI18N
        }

        @Override
        public String toString() {
            return displayName;
        }

    }

    private static final class SpacesCategorySupport extends FmtOptions.CategorySupport {

        public SpacesCategorySupport(Defaults.Provider provider, Preferences preferences, FmtSpaces panel, String preview) {
            super(JsTokenId.JAVASCRIPT_MIME_TYPE, provider, preferences, "spaces", panel, preview); // NOI18N
            assert provider != null;
            panel.scs = this;
        }

        @Override
        protected void addListeners() {
            // Should not do anything
        }

        @Override
        protected void loadFrom(Preferences preferences) {
            for (Item item : getAllItems()) {
                boolean df = provider.getDefaultAsBoolean(item.id);
                item.value = preferences.getBoolean(item.id, df);
            }
        }

        @Override
        protected void storeTo(Preferences preferences) {
            for (Item item : getAllItems()) {
                boolean df = provider.getDefaultAsBoolean(item.id);
                if (df == item.value)
                    preferences.remove(item.id);
                else
                    preferences.putBoolean(item.id, item.value);
            }
        }

        private List<Item> getAllItems() {
            List<Item> result = new LinkedList<FmtSpaces.Item>();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((FmtSpaces) panel).model.getRoot();
            Enumeration children = root.depthFirstEnumeration();
            while( children.hasMoreElements() ) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
                Object o = node.getUserObject();
                if (o instanceof Item) {
                    Item item = (Item) o;
                    if ( item.items == null || item.items.length == 0 ) {
                        result.add( item );
                    }
                }
            }
            return result;
        }
    }
}
