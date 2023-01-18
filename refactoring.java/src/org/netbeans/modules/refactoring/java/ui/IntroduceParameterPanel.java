/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.refactoring.java.ui;

import com.sun.javadoc.Doc;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.io.IOException;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import org.netbeans.api.editor.DialogBinding;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CodeStyle;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.refactoring.java.RefactoringModule;
import org.netbeans.modules.refactoring.java.plugins.JavaPluginUtils;
import org.netbeans.modules.refactoring.java.ui.ChangeParametersPanel.Javadoc;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * @author Jan Becicka
 */
public class IntroduceParameterPanel extends JPanel implements CustomRefactoringPanel {
    private static final String OVERLOADMETHOD = "overloadmethod.introduceParameter"; // NOI18N
    private static final String REPLACEALL = "replaceall.introduceParameter"; // NOI18N
    private static final String DECLAREFINAL = "declarefinal.introduceParameter"; // NOI18N
    private static final String UPDATEJAVADOC = "updateJavadoc.introduceParameters"; // NOI18N
    private static final String GENJAVADOC = "generateJavadoc.introduceParameters"; // NOI18N
    private static final String MIME_JAVA = "text/x-java"; // NOI18N
    private static final String DEFAULT_NAME = "par"; // NOI18N

    TreePathHandle refactoredObj;
    private ChangeListener parent;
    private final JComponent[] singleLineEditor;
    private final DocumentListener nameChangedListener;
    private int startOffset;
    
    @Override
    public Component getComponent() {
        return this;
    }


    /** Creates new form ChangeMethodSignature */
    public IntroduceParameterPanel(TreePathHandle refactoredObj, final ChangeListener parent) {
        this.refactoredObj = refactoredObj;
        this.parent = parent;
        singleLineEditor = Utilities.createSingleLineEditor(MIME_JAVA);
        initComponents();
        nameChangedListener = new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                parent.stateChanged(null);
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                parent.stateChanged(null);
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                parent.stateChanged(null);
            }
        };
        ((JEditorPane) singleLineEditor[1]).getDocument().addDocumentListener(nameChangedListener);
    }
    
    private boolean initialized = false;
    @Override
    public void initialize() {
        try {
            if (initialized) {
                return;
            }
            JavaSource source = JavaSource.forFileObject(refactoredObj.getFileObject());
            source.runUserActionTask(new CancellableTask<CompilationController>() {
                
                @Override
                public void run(org.netbeans.api.java.source.CompilationController info) {
                    try {
                        info.toPhase(org.netbeans.api.java.source.JavaSource.Phase.RESOLVED);
                        
                        final FileObject fileObject = refactoredObj.getFileObject();
                        DataObject dob = DataObject.find(fileObject);
                        ((JEditorPane)singleLineEditor[1]).getDocument().putProperty(
                                Document.StreamDescriptionProperty,
                                dob);
                        final TreePath path = refactoredObj.resolve(info);
                        
                        final TreePath methodPath = JavaPluginUtils.findMethod(path);
                        MethodTree methodTree = (MethodTree) methodPath.getLeaf();
                        final int[] parameterSpan = info.getTreeUtilities().findMethodParameterSpan(methodTree);
                        TypeMirror typeMirror = info.getTrees().getTypeMirror(path);
                        final String tm = typeMirror == null ? "Object" : typeMirror.toString();
                        
                        Element methodElement = info.getTrees().getElement(methodPath);
                        Doc javadocDoc = info.getElementUtilities().javaDocFor(methodElement);
                        if(javadocDoc.commentText() == null || javadocDoc.getRawCommentText().equals("")) {
                            chkGenJavadoc.setEnabled(true);
                            chkGenJavadoc.setVisible(true);
                            chkUpdateJavadoc.setVisible(false);
                        } else {
                            chkUpdateJavadoc.setEnabled(true);
                            chkUpdateJavadoc.setVisible(true);
                            chkGenJavadoc.setVisible(false);
                        }
                                                
                        String name = JavaPluginUtils.getName(path.getLeaf());
                        if (name == null) {
                            name = DEFAULT_NAME;
                        }
                        
                        Scope scope =  null;
                        TreePath bodyPath = new TreePath(methodPath, methodTree.getBody());
                        scope = info.getTrees().getScope(bodyPath);
                        
                        CodeStyle cs;
                        Document doc = info.getDocument();
                        if(doc != null) {
                            cs = CodeStyle.getDefault(doc);
                        } else {
                            cs = CodeStyle.getDefault(info.getFileObject());
                        }
                        final String parameterName = JavaPluginUtils.makeNameUnique(info, scope, name, cs.getParameterNamePrefix(), cs.getParameterNameSuffix());
                        final boolean variableRewrite = path.getLeaf().getKind() == Tree.Kind.VARIABLE;
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                final JEditorPane editorPane = (JEditorPane)singleLineEditor[1];
                                
                                DialogBinding.bindComponentToFile(fileObject, parameterSpan[0] + 1, parameterSpan[1] - parameterSpan[0], editorPane);
                                editorPane.setText(tm + " " + parameterName); //NOI18N
                                startOffset = tm.length() + 1;
                                int endOffset = parameterName.length() + startOffset;
                                editorPane.select(startOffset, endOffset);
                                try {
                                    Position startPos = editorPane.getDocument().createPosition(startOffset);
                                    Position endPos = editorPane.getDocument().createPosition(endOffset);
                                    editorPane.putClientProperty("document-view-start-position", startPos); //NOI18N
                                    editorPane.putClientProperty("document-view-end-position", endPos); //NOI18N
                                } catch (BadLocationException ex) {
                                    Exceptions.printStackTrace(ex);
                                }

                                ((JEditorPane)singleLineEditor[1]).getDocument().addDocumentListener(nameChangedListener);
                                if(variableRewrite) {
                                    chkIsReplaceAll.setEnabled(false);
                                    chkIsReplaceAll.setSelected(false);
                                }
                                initialized = true;
                            }});
                    }
                    catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

                @Override
                public void cancel() {
                }
            }, true);
            initialized = true;
        }
        catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgOverloadMethod = new javax.swing.ButtonGroup();
        lblName = new javax.swing.JLabel();
        chkIsDeclareFinal = new javax.swing.JCheckBox();
        chkIsReplaceAll = new javax.swing.JCheckBox();
        jScrollPane1 = (JScrollPane)singleLineEditor[0];
        chkGenJavadoc = new javax.swing.JCheckBox();
        chkUpdateJavadoc = new javax.swing.JCheckBox();
        rbUpdateMethod = new javax.swing.JRadioButton();
        rbOverloadMethod = new javax.swing.JRadioButton();
        lblCodeGeneration = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 11, 11));
        setAutoscrolls(true);
        setName(getString("LBL_TitleIntroduceParameter"));

        lblName.setLabelFor(jScrollPane1);
        org.openide.awt.Mnemonics.setLocalizedText(lblName, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.lblName.text")); // NOI18N

        chkIsDeclareFinal.setSelected(((Boolean) RefactoringModule.getOption(DECLAREFINAL, Boolean.FALSE)).booleanValue());
        org.openide.awt.Mnemonics.setLocalizedText(chkIsDeclareFinal, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.chkIsDeclareFinal.text")); // NOI18N
        chkIsDeclareFinal.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkIsDeclareFinalItemStateChanged(evt);
            }
        });

        chkIsReplaceAll.setSelected(((Boolean) RefactoringModule.getOption(REPLACEALL, Boolean.FALSE)).booleanValue());
        org.openide.awt.Mnemonics.setLocalizedText(chkIsReplaceAll, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.chkIsReplaceAll.text")); // NOI18N
        chkIsReplaceAll.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkIsReplaceAllItemStateChanged(evt);
            }
        });

        chkGenJavadoc.setSelected(((Boolean) RefactoringModule.getOption(GENJAVADOC, Boolean.FALSE)).booleanValue());
        org.openide.awt.Mnemonics.setLocalizedText(chkGenJavadoc, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.chkGenJavadoc.text")); // NOI18N
        chkGenJavadoc.setEnabled(false);
        chkGenJavadoc.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkGenJavadocItemStateChanged(evt);
            }
        });

        chkUpdateJavadoc.setSelected(((Boolean) RefactoringModule.getOption(UPDATEJAVADOC, Boolean.FALSE)).booleanValue());
        org.openide.awt.Mnemonics.setLocalizedText(chkUpdateJavadoc, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.chkUpdateJavadoc.text")); // NOI18N
        chkUpdateJavadoc.setEnabled(false);
        chkUpdateJavadoc.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chkUpdateJavadocItemStateChanged(evt);
            }
        });

        bgOverloadMethod.add(rbUpdateMethod);
        rbUpdateMethod.setSelected(!((Boolean) RefactoringModule.getOption(OVERLOADMETHOD, Boolean.FALSE)).booleanValue());
        org.openide.awt.Mnemonics.setLocalizedText(rbUpdateMethod, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.rbUpdateMethod.text")); // NOI18N

        bgOverloadMethod.add(rbOverloadMethod);
        rbOverloadMethod.setSelected(((Boolean) RefactoringModule.getOption(OVERLOADMETHOD, Boolean.FALSE)).booleanValue());
        org.openide.awt.Mnemonics.setLocalizedText(rbOverloadMethod, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.rbOverloadMethod.text")); // NOI18N
        rbOverloadMethod.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                rbOverloadMethodItemStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lblCodeGeneration, org.openide.util.NbBundle.getMessage(IntroduceParameterPanel.class, "IntroduceParameterPanel.lblCodeGeneration.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lblName)
                .addGap(2, 2, 2)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE))
            .addComponent(chkIsDeclareFinal)
            .addComponent(chkIsReplaceAll)
            .addComponent(chkUpdateJavadoc)
            .addComponent(chkGenJavadoc)
            .addComponent(lblCodeGeneration)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbUpdateMethod))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbOverloadMethod))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addGap(18, 18, 18)
                .addComponent(chkIsDeclareFinal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkIsReplaceAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkUpdateJavadoc)
                    .addComponent(chkGenJavadoc))
                .addGap(18, 18, 18)
                .addComponent(lblCodeGeneration)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbUpdateMethod)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbOverloadMethod)
                .addContainerGap(24, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chkIsReplaceAllItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkIsReplaceAllItemStateChanged
        Boolean b = evt.getStateChange() == ItemEvent.SELECTED ? Boolean.TRUE : Boolean.FALSE;
        RefactoringModule.setOption(REPLACEALL, b);
        parent.stateChanged(null);
    }//GEN-LAST:event_chkIsReplaceAllItemStateChanged

    private void chkIsDeclareFinalItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkIsDeclareFinalItemStateChanged
        Boolean b = evt.getStateChange() == ItemEvent.SELECTED ? Boolean.TRUE : Boolean.FALSE;
        RefactoringModule.setOption(DECLAREFINAL, b);
        parent.stateChanged(null);
    }//GEN-LAST:event_chkIsDeclareFinalItemStateChanged

    private void chkGenJavadocItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkGenJavadocItemStateChanged
        Boolean b = evt.getStateChange() == ItemEvent.SELECTED ? Boolean.TRUE : Boolean.FALSE;
	RefactoringModule.setOption(GENJAVADOC, b); // NOI18N
	parent.stateChanged(null);
    }//GEN-LAST:event_chkGenJavadocItemStateChanged

    private void chkUpdateJavadocItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkUpdateJavadocItemStateChanged
        Boolean b = evt.getStateChange() == ItemEvent.SELECTED ? Boolean.TRUE : Boolean.FALSE;
	RefactoringModule.setOption(UPDATEJAVADOC, b); // NOI18N
	parent.stateChanged(null);
    }//GEN-LAST:event_chkUpdateJavadocItemStateChanged

    private void rbOverloadMethodItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_rbOverloadMethodItemStateChanged
        Boolean b = evt.getStateChange() == ItemEvent.SELECTED ? Boolean.TRUE : Boolean.FALSE;
	RefactoringModule.setOption(OVERLOADMETHOD, b);
	parent.stateChanged(null);
    }//GEN-LAST:event_rbOverloadMethodItemStateChanged
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgOverloadMethod;
    private javax.swing.JCheckBox chkGenJavadoc;
    private javax.swing.JCheckBox chkIsDeclareFinal;
    private javax.swing.JCheckBox chkIsReplaceAll;
    private javax.swing.JCheckBox chkUpdateJavadoc;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCodeGeneration;
    private javax.swing.JLabel lblName;
    private javax.swing.JRadioButton rbOverloadMethod;
    private javax.swing.JRadioButton rbUpdateMethod;
    // End of variables declaration//GEN-END:variables

    private static String getString(String key) {
        return NbBundle.getMessage(ChangeParametersPanel.class, key);
    }

    public boolean isCompatible() {
        return rbOverloadMethod.isSelected();
    }

    public boolean isDeclareFinal() {
        return chkIsDeclareFinal.isSelected();
    }

    public boolean isReplaceAll() {
        return chkIsReplaceAll.isSelected();
    }
    
    public String getParameterName() {
        final String text = ((JEditorPane)singleLineEditor[1]).getText();
        final String substring;
        if(text.length() < startOffset) {
            // try to recover from not fully implemented document-view-start-position #204788
            substring = text;
        } else {
            substring = text.substring(startOffset);
        }
        return substring;
    }
    
    protected Javadoc getJavadoc() {
        if(chkUpdateJavadoc.isVisible() && chkUpdateJavadoc.isSelected()) {
            return Javadoc.UPDATE;
        } else if(chkGenJavadoc.isVisible() && chkGenJavadoc.isSelected()) {
            return Javadoc.GENERATE;
        } else {
            return Javadoc.NONE;
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        ((JEditorPane)singleLineEditor[1]).requestFocusInWindow();
        return true;
    }
}
