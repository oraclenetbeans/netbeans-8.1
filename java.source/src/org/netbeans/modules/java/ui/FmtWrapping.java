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

package org.netbeans.modules.java.ui;

import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JLabel;
import org.netbeans.api.java.source.CodeStyle;
import static org.netbeans.modules.java.ui.FmtOptions.*;
import static org.netbeans.modules.java.ui.CategorySupport.OPTION_ID;
import org.netbeans.modules.options.editor.spi.PreferencesCustomizer;


/**
 *
 * @author  phrebejk
 */
public class FmtWrapping extends javax.swing.JPanel implements FocusListener {
    
    /** Creates new form FmtWrapping */
    public FmtWrapping() {
        initComponents();
        
        scrollPane.getViewport().setBackground(java.awt.SystemColor.controlLtHighlight);
        scrollPane.getVerticalScrollBar().setUnitIncrement(new JLabel("X").getPreferredSize().height);
        
        extendsImplementsKeywordCombo.putClientProperty(OPTION_ID, wrapExtendsImplementsKeyword);
        extendsImplementsKeywordCombo.addFocusListener(this);
        extendsImplementsListCombo.putClientProperty(OPTION_ID, wrapExtendsImplementsList);
        extendsImplementsListCombo.addFocusListener(this);
        methodParamsCombo.putClientProperty(OPTION_ID, wrapMethodParams);
        methodParamsCombo.addFocusListener(this);
        methodCallArgsCombo.putClientProperty(OPTION_ID, wrapMethodCallArgs);
        methodCallArgsCombo.addFocusListener(this);
        annotationArgsCombo.putClientProperty(OPTION_ID, wrapAnnotationArgs);
        annotationArgsCombo.addFocusListener(this);
        chainedMethodCallsCombo.putClientProperty(OPTION_ID, wrapChainedMethodCalls);
        chainedMethodCallsCombo.addFocusListener(this);
        afterDotCheckBox.putClientProperty(OPTION_ID, wrapAfterDotInChainedMethodCalls);
        afterDotCheckBox.addFocusListener(this);
        throwsKeywordCombo.putClientProperty(OPTION_ID, wrapThrowsKeyword);
        throwsKeywordCombo.addFocusListener(this);
        throwsListCombo.putClientProperty(OPTION_ID, wrapThrowsList);
        throwsListCombo.addFocusListener(this);
        arrayInitCombo.putClientProperty(OPTION_ID, wrapArrayInit);
        arrayInitCombo.addFocusListener(this);
        tryCombo.putClientProperty(OPTION_ID, wrapTryResources);
        tryCombo.addFocusListener(this);
        multiCatchCombo.putClientProperty(OPTION_ID, wrapDisjunctiveCatchTypes);
        multiCatchCombo.addFocusListener(this);
        forCombo.putClientProperty(OPTION_ID, wrapFor);
        forCombo.addFocusListener(this);
        forStatementCombo.putClientProperty(OPTION_ID, wrapForStatement );
        forStatementCombo.addFocusListener(this);
        ifStatementCombo.putClientProperty(OPTION_ID, wrapIfStatement);
        ifStatementCombo.addFocusListener(this);
        whileStatementComboBox.putClientProperty(OPTION_ID, wrapWhileStatement);
        whileStatementComboBox.addFocusListener(this);
        doWhileStatementCombo.putClientProperty(OPTION_ID, wrapDoWhileStatement);
        doWhileStatementCombo.addFocusListener(this);
        caseStatementsCombo.putClientProperty(OPTION_ID, wrapCaseStatements);
        caseStatementsCombo.addFocusListener(this);
        assertCombo.putClientProperty(OPTION_ID, wrapAssert);
        assertCombo.addFocusListener(this);
        enumConstantsCombo.putClientProperty(OPTION_ID, wrapEnumConstants);
        enumConstantsCombo.addFocusListener(this);
        annotationsCombo.putClientProperty(OPTION_ID, wrapAnnotations);
        annotationsCombo.addFocusListener(this);
        binaryOpsCombo.putClientProperty(OPTION_ID, wrapBinaryOps);
        binaryOpsCombo.addFocusListener(this);
        afterBinaryOpsCheckBox.putClientProperty(OPTION_ID, wrapAfterBinaryOps);
        afterBinaryOpsCheckBox.addFocusListener(this);
        ternaryOpsCombo.putClientProperty(OPTION_ID, wrapTernaryOps);
        ternaryOpsCombo.addFocusListener(this);
        afterTernaryOpsCheckBox.putClientProperty(OPTION_ID, wrapAfterTernaryOps);
        afterTernaryOpsCheckBox.addFocusListener(this);
        assignOpsCombo.putClientProperty(OPTION_ID, wrapAssignOps);
        assignOpsCombo.addFocusListener(this);
        afterAssignOpsCheckBox.putClientProperty(OPTION_ID, wrapAfterAssignOps);
        afterAssignOpsCheckBox.addFocusListener(this);
        lambdaParamsCombo.putClientProperty(OPTION_ID, wrapLambdaParams);
        lambdaParamsCombo.addFocusListener(this);
        lambdaArrowCombo.putClientProperty(OPTION_ID, wrapLambdaArrow);
        lambdaArrowCombo.addFocusListener(this);
        afterLambdaArrowCheckBox.putClientProperty(OPTION_ID, wrapAfterLambdaArrow);
        afterLambdaArrowCheckBox.addFocusListener(this);
    }
    
    public static PreferencesCustomizer.Factory getController() {
        return new CategorySupport.Factory("wrapping", FmtWrapping.class, //NOI18N
                org.openide.util.NbBundle.getMessage(FmtWrapping.class, "SAMPLE_Wrapping"), //NOI18N
                new String[] { FmtOptions.rightMargin, "35" }, //NOI18N
                new String[] { FmtOptions.redundantDoWhileBraces, CodeStyle.BracesGenerationStyle.LEAVE_ALONE.name() },
                new String[] { FmtOptions.redundantForBraces, CodeStyle.BracesGenerationStyle.LEAVE_ALONE.name() },
                new String[] { FmtOptions.redundantIfBraces, CodeStyle.BracesGenerationStyle.LEAVE_ALONE.name() },
                new String[] { FmtOptions.redundantWhileBraces, CodeStyle.BracesGenerationStyle.LEAVE_ALONE.name() },
                new String[] { FmtOptions.blankLinesBeforeClass, "0" },
                new String[] { FmtOptions.blankLinesAfterMethods, "1" }
        ); // NOI18N
    }

    public void focusGained(FocusEvent e) {
        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        Rectangle bounds = e.getComponent().getBounds();
        if (viewRect.y > bounds.y || viewRect.y + viewRect.height < bounds.y + bounds.height) {
            scrollPane.getViewport().scrollRectToVisible(bounds);
        }
    }

    public void focusLost(FocusEvent e) {
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrollPane = new javax.swing.JScrollPane();
        panel1 = new javax.swing.JPanel();
        extendsImplemetsKeywordLabel = new javax.swing.JLabel();
        extendsImplementsKeywordCombo = new javax.swing.JComboBox();
        extendsImplementsListLabel = new javax.swing.JLabel();
        extendsImplementsListCombo = new javax.swing.JComboBox();
        methodParamsLabel = new javax.swing.JLabel();
        methodParamsCombo = new javax.swing.JComboBox();
        methodCallArgsLabel = new javax.swing.JLabel();
        methodCallArgsCombo = new javax.swing.JComboBox();
        annotationArgsLabel = new javax.swing.JLabel();
        annotationArgsCombo = new javax.swing.JComboBox();
        chainedMethodCallsLabel = new javax.swing.JLabel();
        chainedMethodCallsCombo = new javax.swing.JComboBox();
        afterDotCheckBox = new javax.swing.JCheckBox();
        throwsKeywordLabel = new javax.swing.JLabel();
        throwsKeywordCombo = new javax.swing.JComboBox();
        throwsListLabel = new javax.swing.JLabel();
        throwsListCombo = new javax.swing.JComboBox();
        arrayInitLabel = new javax.swing.JLabel();
        arrayInitCombo = new javax.swing.JComboBox();
        tryLabel = new javax.swing.JLabel();
        tryCombo = new javax.swing.JComboBox();
        multiCatchLabel = new javax.swing.JLabel();
        multiCatchCombo = new javax.swing.JComboBox();
        forLabel = new javax.swing.JLabel();
        forCombo = new javax.swing.JComboBox();
        forStatementLabel = new javax.swing.JLabel();
        forStatementCombo = new javax.swing.JComboBox();
        ifStatementLabel = new javax.swing.JLabel();
        ifStatementCombo = new javax.swing.JComboBox();
        whileStatementLabel = new javax.swing.JLabel();
        whileStatementComboBox = new javax.swing.JComboBox();
        doWhileStatementLabel = new javax.swing.JLabel();
        doWhileStatementCombo = new javax.swing.JComboBox();
        caseStatementsLabel = new javax.swing.JLabel();
        caseStatementsCombo = new javax.swing.JComboBox();
        assertLabel = new javax.swing.JLabel();
        assertCombo = new javax.swing.JComboBox();
        enumConstantsLabel = new javax.swing.JLabel();
        enumConstantsCombo = new javax.swing.JComboBox();
        annotationsLabel = new javax.swing.JLabel();
        annotationsCombo = new javax.swing.JComboBox();
        binaryOpsLabel = new javax.swing.JLabel();
        binaryOpsCombo = new javax.swing.JComboBox();
        afterBinaryOpsCheckBox = new javax.swing.JCheckBox();
        ternaryOpsLabel = new javax.swing.JLabel();
        ternaryOpsCombo = new javax.swing.JComboBox();
        afterTernaryOpsCheckBox = new javax.swing.JCheckBox();
        assignOpsLabel = new javax.swing.JLabel();
        assignOpsCombo = new javax.swing.JComboBox();
        afterAssignOpsCheckBox = new javax.swing.JCheckBox();
        lambdaParamsLabel = new javax.swing.JLabel();
        lambdaParamsCombo = new javax.swing.JComboBox();
        lambdaArrowLabel = new javax.swing.JLabel();
        lambdaArrowCombo = new javax.swing.JComboBox();
        afterLambdaArrowCheckBox = new javax.swing.JCheckBox();

        setName(org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_Wrapping")); // NOI18N
        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        scrollPane.setMinimumSize(new java.awt.Dimension(300, 200));
        scrollPane.setPreferredSize(new java.awt.Dimension(350, 600));

        panel1.setLayout(new java.awt.GridBagLayout());

        extendsImplemetsKeywordLabel.setLabelFor(extendsImplementsKeywordCombo);
        org.openide.awt.Mnemonics.setLocalizedText(extendsImplemetsKeywordLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_extendsImplementsKeyword")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 4, 0);
        panel1.add(extendsImplemetsKeywordLabel, gridBagConstraints);

        extendsImplementsKeywordCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 6, 4, 8);
        panel1.add(extendsImplementsKeywordCombo, gridBagConstraints);

        extendsImplementsListLabel.setLabelFor(extendsImplementsListCombo);
        org.openide.awt.Mnemonics.setLocalizedText(extendsImplementsListLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_extendsImplementsList")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(extendsImplementsListLabel, gridBagConstraints);

        extendsImplementsListCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(extendsImplementsListCombo, gridBagConstraints);

        methodParamsLabel.setLabelFor(methodParamsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(methodParamsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_methodParameters")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(methodParamsLabel, gridBagConstraints);

        methodParamsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(methodParamsCombo, gridBagConstraints);

        methodCallArgsLabel.setLabelFor(methodCallArgsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(methodCallArgsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_methodCallArgs")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(methodCallArgsLabel, gridBagConstraints);

        methodCallArgsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(methodCallArgsCombo, gridBagConstraints);

        annotationArgsLabel.setLabelFor(annotationArgsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(annotationArgsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_annotationArgs")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(annotationArgsLabel, gridBagConstraints);

        annotationArgsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(annotationArgsCombo, gridBagConstraints);

        chainedMethodCallsLabel.setLabelFor(chainedMethodCallsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(chainedMethodCallsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_chainedMethodCalls")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(chainedMethodCallsLabel, gridBagConstraints);

        chainedMethodCallsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(chainedMethodCallsCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(afterDotCheckBox, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_afeterDot")); // NOI18N
        afterDotCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 6, 0);
        panel1.add(afterDotCheckBox, gridBagConstraints);

        throwsKeywordLabel.setLabelFor(throwsKeywordCombo);
        org.openide.awt.Mnemonics.setLocalizedText(throwsKeywordLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_throwsKeyword")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(throwsKeywordLabel, gridBagConstraints);

        throwsKeywordCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(throwsKeywordCombo, gridBagConstraints);

        throwsListLabel.setLabelFor(throwsListCombo);
        org.openide.awt.Mnemonics.setLocalizedText(throwsListLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_throwsList")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(throwsListLabel, gridBagConstraints);

        throwsListCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(throwsListCombo, gridBagConstraints);

        arrayInitLabel.setLabelFor(arrayInitCombo);
        org.openide.awt.Mnemonics.setLocalizedText(arrayInitLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_arrayInit")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(arrayInitLabel, gridBagConstraints);

        arrayInitCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(arrayInitCombo, gridBagConstraints);

        tryLabel.setLabelFor(tryCombo);
        org.openide.awt.Mnemonics.setLocalizedText(tryLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_tryResources")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(tryLabel, gridBagConstraints);

        tryCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(tryCombo, gridBagConstraints);

        multiCatchLabel.setLabelFor(multiCatchCombo);
        org.openide.awt.Mnemonics.setLocalizedText(multiCatchLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_multiCatches")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(multiCatchLabel, gridBagConstraints);

        multiCatchCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(multiCatchCombo, gridBagConstraints);

        forLabel.setLabelFor(forCombo);
        org.openide.awt.Mnemonics.setLocalizedText(forLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_for")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(forLabel, gridBagConstraints);

        forCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(forCombo, gridBagConstraints);

        forStatementLabel.setLabelFor(forStatementCombo);
        org.openide.awt.Mnemonics.setLocalizedText(forStatementLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_forStatement")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(forStatementLabel, gridBagConstraints);

        forStatementCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(forStatementCombo, gridBagConstraints);

        ifStatementLabel.setLabelFor(ifStatementCombo);
        org.openide.awt.Mnemonics.setLocalizedText(ifStatementLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_ifStatement")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(ifStatementLabel, gridBagConstraints);

        ifStatementCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(ifStatementCombo, gridBagConstraints);

        whileStatementLabel.setLabelFor(whileStatementComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(whileStatementLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_whileStatement")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(whileStatementLabel, gridBagConstraints);

        whileStatementComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(whileStatementComboBox, gridBagConstraints);

        doWhileStatementLabel.setLabelFor(doWhileStatementCombo);
        org.openide.awt.Mnemonics.setLocalizedText(doWhileStatementLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_doWhileStatement")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(doWhileStatementLabel, gridBagConstraints);

        doWhileStatementCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(doWhileStatementCombo, gridBagConstraints);

        caseStatementsLabel.setLabelFor(caseStatementsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(caseStatementsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_caseStatements")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(caseStatementsLabel, gridBagConstraints);

        caseStatementsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(caseStatementsCombo, gridBagConstraints);

        assertLabel.setLabelFor(assertCombo);
        org.openide.awt.Mnemonics.setLocalizedText(assertLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_assert")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(assertLabel, gridBagConstraints);

        assertCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(assertCombo, gridBagConstraints);

        enumConstantsLabel.setLabelFor(enumConstantsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(enumConstantsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_enumConstants")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(enumConstantsLabel, gridBagConstraints);

        enumConstantsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(enumConstantsCombo, gridBagConstraints);

        annotationsLabel.setLabelFor(annotationsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(annotationsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_annotations")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(annotationsLabel, gridBagConstraints);

        annotationsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(annotationsCombo, gridBagConstraints);

        binaryOpsLabel.setLabelFor(binaryOpsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(binaryOpsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_binaryOps")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(binaryOpsLabel, gridBagConstraints);

        binaryOpsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(binaryOpsCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(afterBinaryOpsCheckBox, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_afeterBinaryOps")); // NOI18N
        afterBinaryOpsCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 6, 0);
        panel1.add(afterBinaryOpsCheckBox, gridBagConstraints);

        ternaryOpsLabel.setLabelFor(ternaryOpsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(ternaryOpsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_ternaryOps")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(ternaryOpsLabel, gridBagConstraints);

        ternaryOpsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(ternaryOpsCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(afterTernaryOpsCheckBox, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_afterTernaryOps")); // NOI18N
        afterTernaryOpsCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 6, 0);
        panel1.add(afterTernaryOpsCheckBox, gridBagConstraints);

        assignOpsLabel.setLabelFor(assignOpsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(assignOpsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_assignOps")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(assignOpsLabel, gridBagConstraints);

        assignOpsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(assignOpsCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(afterAssignOpsCheckBox, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_afterAssignOps")); // NOI18N
        afterAssignOpsCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 6, 0);
        panel1.add(afterAssignOpsCheckBox, gridBagConstraints);

        lambdaParamsLabel.setLabelFor(lambdaParamsCombo);
        org.openide.awt.Mnemonics.setLocalizedText(lambdaParamsLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_lambdaParams")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(lambdaParamsLabel, gridBagConstraints);

        lambdaParamsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(lambdaParamsCombo, gridBagConstraints);

        lambdaArrowLabel.setLabelFor(lambdaArrowCombo);
        org.openide.awt.Mnemonics.setLocalizedText(lambdaArrowLabel, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_lambdaArrow")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        panel1.add(lambdaArrowLabel, gridBagConstraints);

        lambdaArrowCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 4, 8);
        panel1.add(lambdaArrowCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(afterLambdaArrowCheckBox, org.openide.util.NbBundle.getMessage(FmtWrapping.class, "LBL_wrp_afterLambdaArrow")); // NOI18N
        afterLambdaArrowCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 6, 0);
        panel1.add(afterLambdaArrowCheckBox, gridBagConstraints);

        scrollPane.setViewportView(panel1);

        add(scrollPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox afterAssignOpsCheckBox;
    private javax.swing.JCheckBox afterBinaryOpsCheckBox;
    private javax.swing.JCheckBox afterDotCheckBox;
    private javax.swing.JCheckBox afterLambdaArrowCheckBox;
    private javax.swing.JCheckBox afterTernaryOpsCheckBox;
    private javax.swing.JComboBox annotationArgsCombo;
    private javax.swing.JLabel annotationArgsLabel;
    private javax.swing.JComboBox annotationsCombo;
    private javax.swing.JLabel annotationsLabel;
    private javax.swing.JComboBox arrayInitCombo;
    private javax.swing.JLabel arrayInitLabel;
    private javax.swing.JComboBox assertCombo;
    private javax.swing.JLabel assertLabel;
    private javax.swing.JComboBox assignOpsCombo;
    private javax.swing.JLabel assignOpsLabel;
    private javax.swing.JComboBox binaryOpsCombo;
    private javax.swing.JLabel binaryOpsLabel;
    private javax.swing.JComboBox caseStatementsCombo;
    private javax.swing.JLabel caseStatementsLabel;
    private javax.swing.JComboBox chainedMethodCallsCombo;
    private javax.swing.JLabel chainedMethodCallsLabel;
    private javax.swing.JComboBox doWhileStatementCombo;
    private javax.swing.JLabel doWhileStatementLabel;
    private javax.swing.JComboBox enumConstantsCombo;
    private javax.swing.JLabel enumConstantsLabel;
    private javax.swing.JComboBox extendsImplementsKeywordCombo;
    private javax.swing.JComboBox extendsImplementsListCombo;
    private javax.swing.JLabel extendsImplementsListLabel;
    private javax.swing.JLabel extendsImplemetsKeywordLabel;
    private javax.swing.JComboBox forCombo;
    private javax.swing.JLabel forLabel;
    private javax.swing.JComboBox forStatementCombo;
    private javax.swing.JLabel forStatementLabel;
    private javax.swing.JComboBox ifStatementCombo;
    private javax.swing.JLabel ifStatementLabel;
    private javax.swing.JComboBox lambdaArrowCombo;
    private javax.swing.JLabel lambdaArrowLabel;
    private javax.swing.JComboBox lambdaParamsCombo;
    private javax.swing.JLabel lambdaParamsLabel;
    private javax.swing.JComboBox methodCallArgsCombo;
    private javax.swing.JLabel methodCallArgsLabel;
    private javax.swing.JComboBox methodParamsCombo;
    private javax.swing.JLabel methodParamsLabel;
    private javax.swing.JComboBox multiCatchCombo;
    private javax.swing.JLabel multiCatchLabel;
    private javax.swing.JPanel panel1;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JComboBox ternaryOpsCombo;
    private javax.swing.JLabel ternaryOpsLabel;
    private javax.swing.JComboBox throwsKeywordCombo;
    private javax.swing.JLabel throwsKeywordLabel;
    private javax.swing.JComboBox throwsListCombo;
    private javax.swing.JLabel throwsListLabel;
    private javax.swing.JComboBox tryCombo;
    private javax.swing.JLabel tryLabel;
    private javax.swing.JComboBox whileStatementComboBox;
    private javax.swing.JLabel whileStatementLabel;
    // End of variables declaration//GEN-END:variables

}
