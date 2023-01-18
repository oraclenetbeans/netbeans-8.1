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
package org.netbeans.modules.css.visual;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.css.indexing.api.CssIndex;
import org.netbeans.modules.css.lib.api.CssColor;
import org.netbeans.modules.css.lib.api.properties.FixedTextGrammarElement;
import org.netbeans.modules.css.lib.api.properties.Properties;
import org.netbeans.modules.css.lib.api.properties.PropertyDefinition;
import org.netbeans.modules.css.lib.api.properties.ResolvedProperty;
import org.netbeans.modules.css.lib.api.properties.ResolvedToken;
import org.netbeans.modules.css.lib.api.properties.Token;
import org.netbeans.modules.css.lib.api.properties.TokenAcceptor;
import org.netbeans.modules.css.lib.api.properties.UnitGrammarElement;
import org.netbeans.modules.css.model.api.Model;
import org.netbeans.modules.css.model.api.PropertyDeclaration;
import org.netbeans.modules.css.refactoring.api.RefactoringElementType;
import org.netbeans.modules.css.visual.actions.GoToSourceAction;
import org.netbeans.modules.web.common.api.WebUtils;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author marekfukala
 */
@NbBundle.Messages({
    "choose.color.item=Choose Color"
})
public class PropertyValuesEditor extends PropertyEditorSupport implements ExPropertyEditor {

    private Collection<UnitGrammarElement> unitElements;
    private Collection<FixedTextGrammarElement> fixedElements;
    private boolean addNoneProperty;
    private List<String> tags;
    private Map<String, FixedTextGrammarElement> tags2fixedElement = new HashMap<>();
    private boolean containsColor;
    private FileObject file;
    private PropertyDefinition pmodel;
    private RuleEditorPanel panel;
    private final boolean isAggregatedProperty;
    private static final String CHOOSE_COLOR_ITEM = new StringBuilder().append("<html><b>").append(Bundle.choose_color_item()).append("</b></html>").toString();  //NOI18N
    private static JColorChooser COLOR_CHOOSER;
    private PropertyDeclaration declaration;
    
    public PropertyValuesEditor(RuleEditorPanel panel, 
            PropertyDefinition pmodel, 
            Model model, 
            Collection<FixedTextGrammarElement> fixedElements, 
            Collection<UnitGrammarElement> unitElements, 
            PropertyDeclaration declaration,
            boolean addNoneProperty) {
        this.panel = panel;
        this.fixedElements = fixedElements;
        this.unitElements = unitElements;
        this.addNoneProperty = addNoneProperty;
        this.file = model.getLookup().lookup(FileObject.class);

        this.pmodel = pmodel; //may be null
        this.isAggregatedProperty = pmodel != null ? Properties.isAggregatedProperty(file, pmodel) : false;
        this.declaration = declaration;
    }
    
    private static JColorChooser getJColorChooser() {
        if(COLOR_CHOOSER == null) {
            COLOR_CHOOSER = new JColorChooser();
        }
        return COLOR_CHOOSER;
    }

    private boolean canIncrementDecrement() {
        if (declaration == null) {
            return false;
        }
        ResolvedProperty resolvedProperty = declaration.getResolvedProperty();
        if (resolvedProperty == null) {
            return false;
        }
        List<Token> tokens = resolvedProperty.getTokens();
        if (tokens.size() != 1) {
            return false; //zero or multiple tokens, cannot increment/decrement
        }
        Token value = tokens.get(0);

        for (TokenAcceptor genericAcceptor : TokenAcceptor.ACCEPTORS) {
            if (genericAcceptor instanceof TokenAcceptor.NumberPostfixAcceptor) {
                TokenAcceptor.NumberPostfixAcceptor acceptor = (TokenAcceptor.NumberPostfixAcceptor) genericAcceptor;
                if (acceptor.accepts(value)) {
                    return true;
                }
            } else if (genericAcceptor instanceof TokenAcceptor.Number) {
                TokenAcceptor.Number acceptor = (TokenAcceptor.Number) genericAcceptor;
                        if (acceptor.accepts(value)) {
                            return true;
                        }
                    }
        }
        return false; //none of the number acceptors accepts the value
    }
    
    @Override
    public Component getCustomEditor() {
        return null;
    }

    @Override
    public synchronized String[] getTags() {
//        if (isAggregatedProperty) {
//            //no drop down for aggregated properties
//            return null;
//        }
        if (tags == null) {
            tags = new ArrayList<>();

                //sort the items alphabetically first
                Collection<String> fixedElementNames = new TreeSet<>();
                for (FixedTextGrammarElement element : fixedElements) {
                    String value = element.getValue();
                    if (value.length() > 0 && Character.isLetter(value.charAt(0))) { //filter operators & similar
                        fixedElementNames.add(value);
                        tags2fixedElement.put(value, element);

                        //TBD possibly refactor out so it is not so hardcoded
                        if ("@colors-list".equals(element.origin())) { //NOI18N
                            containsColor = true;
                        }

                    }
                }

                //the rest will handle the order by itself
                tags.addAll(fixedElementNames);

                if (containsColor) {
                    if (file != null) {
                        Project project = FileOwnerQuery.getOwner(file);
                        if (project != null) {
                            try {
                                Collection<String> hashColorCodes = new TreeSet<>();
                                CssIndex index = CssIndex.create(project);
                                Map<FileObject, Collection<String>> result = index.findAll(RefactoringElementType.COLOR);
                                for (FileObject f : result.keySet()) {
                                    Collection<String> colors = result.get(f);
//                                boolean usedInCurrentFile = f.equals(file);
                                    for (String color : colors) {
                                        hashColorCodes.add(color);
                                    }
                                }
                                tags.addAll(0, hashColorCodes);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }

                        }
                    }

                    tags.add(0, CHOOSE_COLOR_ITEM);
                }
            
//            if (addNoneProperty) {
//                //put as first item
//                tagsList.add(0, RuleEditorNode.NONE_PROPERTY_NAME);
//            }
                
        }

        return tags.isEmpty() ? null : tags.toArray(new String[]{});
    }

    @Override
    public void setAsText(String str) {
        if (str == null) {
            return;
        }

        if (str.isEmpty()) {
            return;
        }

        //same value, ignore
        if (str.equals(getValue())) {
            return;
        }

        if (CHOOSE_COLOR_ITEM.equals(str)) {
            //color chooser
            final AtomicReference<Color> color_ref = new AtomicReference<>();
            JDialog dialog = JColorChooser.createDialog(EditorRegistry.lastFocusedComponent(), Bundle.choose_color_item(), true, getJColorChooser(),
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            //disalog confirmed
                            color_ref.set(getJColorChooser().getColor());
                        }
                    }, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //dialog cancelled
                }
            });
            dialog.setVisible(true);
            dialog.dispose();

            Color color = color_ref.get();
            if (color != null) {
                str = WebUtils.toHexCode(color);
            } else {
                //dialog cancelled, no value - do not allow the CHOOSE_COLOR_ITEM marker to be set the to property
                return;
            }

        }

        editingFinished();
        setValue(str);

    }

    @Override
    public String getAsText() {
        return getValue().toString();
    }

    private void editingFinished() {
        panel.editingFinished();
    }
    
    private void editingCancelled() {
        panel.disposeEditedDeclaration();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "; property: " + pmodel != null ? pmodel.getName() : "?"; //NOI18N
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        //if there's at least one unit element, then the text field needs to be editable
        env.getFeatureDescriptor().setValue("canEditAsText", Boolean.TRUE); //NOI18N
        
        env.getFeatureDescriptor().setValue("nb.property.editor.callback", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if("editingCancelled".equals(evt.getPropertyName())) {
                    editingCancelled();
                }
            }
            
        }); //NOI18N

        env.getFeatureDescriptor().setValue("nb.propertysheet.mouse.doubleclick.listener", new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(me)) {
                    if (me.getClickCount() > 1) {
                        FeatureDescriptor selected = panel.getSelected();
                        if (selected != null) {
                            if (selected instanceof RuleEditorNode.DeclarationProperty) {
                                RuleEditorNode.DeclarationProperty declarationProperty = (RuleEditorNode.DeclarationProperty) selected;
                                GoToSourceAction action = new GoToSourceAction(panel, declarationProperty);
                                action.actionPerformed(null);
                            }
                        }
                    }

                }
            }
        });

        if (containsColor) {
            env.getFeatureDescriptor().setValue("customListCellRendererSupport", new ColorListCellRendererSupport()); //NOI18N
        }

        if(canIncrementDecrement()) {
            env.getFeatureDescriptor().setValue("valueIncrement", new SpinnerModel() { //NOI18N
                private String getNextValue(boolean forward) {
                    String value = getAsText();
                    for (TokenAcceptor genericAcceptor : TokenAcceptor.ACCEPTORS) {

                        if (genericAcceptor instanceof TokenAcceptor.NumberPostfixAcceptor) {
                            TokenAcceptor.NumberPostfixAcceptor acceptor = (TokenAcceptor.NumberPostfixAcceptor) genericAcceptor;
                            if (acceptor.accepts(value)) {
                                int i = acceptor.getNumberValue(value).intValue();
                                CharSequence postfix = acceptor.getPostfix(value);

                                StringBuilder sb = new StringBuilder();
                                sb.append(i + (forward ? 1 : -1));
                                if (postfix != null) {
                                    sb.append(postfix);
                                }

                                return sb.toString();
                            }
                        } else if (genericAcceptor instanceof TokenAcceptor.Number) {
                            TokenAcceptor.Number acceptor = (TokenAcceptor.Number) genericAcceptor;
                            if (acceptor.accepts(value)) {
                                int i = acceptor.getNumberValue(value).intValue();

                                StringBuilder sb = new StringBuilder();
                                sb.append(i + (forward ? 1 : -1));

                                return sb.toString();
                            }
                        }

                    }

                    //not acceptable token
                    return null;
                }

                @Override
                public Object getValue() {
                    //no-op
                    return null;
                }

                @Override
                public void setValue(Object value) {
                    //no-op
                }

                @Override
                public Object getNextValue() {
                    return getNextValue(true);
                }

                @Override
                public Object getPreviousValue() {
                    return getNextValue(false);
                }

                @Override
                public void addChangeListener(ChangeListener l) {
                    //no-op
                }

                @Override
                public void removeChangeListener(ChangeListener l) {
                    //no-op
                }
            });
        }
    }

    private class ColorListCellRendererSupport extends AtomicReference<ListCellRenderer> implements ListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ListCellRenderer peer = get();

            assert peer != null; //the ComboInplaceEditor must set the original renreder!

            if (peer instanceof ColorListCellRendererSupport) {
                System.out.println("warning: nesting of ColorListCellRendererSupport!");
            }

            Component res = peer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (res instanceof JLabel) {
                JLabel label = (JLabel) res;
                String strval = (value == null) ? "null" : value.toString(); // NOI18N

                Icon icon = null;

                if (strval.startsWith("#")) { //NOI18N
                    String colorCode = strval.substring(1);
                    icon = WebUtils.createColorIcon(colorCode); //null CssColor will create default icon
                }

                if (strval.equals(CHOOSE_COLOR_ITEM)) {
                    Color chooserColor = getJColorChooser().getColor();
                    String hexCode = chooserColor != null ? WebUtils.toHexCode(chooserColor) : null;
                    icon = WebUtils.createColorIcon(hexCode);
                }

                FixedTextGrammarElement element = tags2fixedElement.get(strval);
                if (!"inherit".equals(strval)) { //filter out colors for inherit
                    if (element != null) {
                        if ("@colors-list".equals(element.origin())) { //NOI18N
                            //try to find color code
                            CssColor color = CssColor.getColor(strval);
                            icon = WebUtils.createColorIcon(color == null ? null : color.colorCode()); //null CssColor will create default icon
                        }
                    }
                }
                label.setIcon(icon);
            } else {
                System.out.println("res instance " + res);
            }
            return res;

        }
    }
}
