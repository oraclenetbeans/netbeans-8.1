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

/*
 * HtmlCompletionOptionsPanel.java
 *
 * Created on 1.2.2010, 11:38:44
 */

package org.netbeans.modules.html.editor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.modules.options.editor.spi.PreferencesCustomizer;
import org.openide.util.HelpCtx;

/**
 *
 * @author marekfukala
 */
public class HtmlCompletionOptionsPanel extends javax.swing.JPanel {

    public static final String HTML_AUTOCOMPLETE_QUOTES_AFTER_EQS = "htmlAutocompleteQuotesAfterEQS"; //NOI18N
    public static final boolean HTML_AUTOCOMPLETE_QUOTES_AFTER_EQS_DEFAULT = true;
    public static final String HTML_AUTOCOMPLETE_QUOTES = "htmlAutocompleteQuotes"; //NOI18N
    public static final boolean HTML_AUTOCOMPLETE_QUOTES_DEFAULT = true;
    public static final String HTML_COMPLETION_END_TAG_ADTER_LT = "htmlCompletionOffersEndTagsAfterLt"; //NOI18N
    public static final boolean HTML_COMPLETION_END_TAG_ADTER_LT_DEFAULT = false;
    public static final String HTML_COMPLETION_AUTOPOPUP_WINDOW = "htmlCompletionAutoPopupWindow"; //NOI18N
    public static final boolean HTML_COMPLETION_AUTOPOPUP_WINDOW_DEFAULT = true;
    public static final String HTML_END_TAG_AUTOCOMPLETION_AUTOPOPUP = "htmlEndTagAutocompletionAutopopup"; //NOI18N
    public static final boolean HTML_END_TAG_AUTOCOMPLETION_AUTOPOPUP_DEFAULT = true;
    private final Map<String, Boolean> id2Saved = new HashMap<>();

    private Preferences preferences;

    /** Creates new form HtmlCompletionOptionsPanel */
    private HtmlCompletionOptionsPanel(Preferences preferences) {
        this.preferences = preferences;
        initComponents();
        autocompleteQuotesAfterEQSCheckBox.setSelected(preferences.getBoolean(HTML_AUTOCOMPLETE_QUOTES_AFTER_EQS, HTML_AUTOCOMPLETE_QUOTES_AFTER_EQS_DEFAULT));
        autocompleteQuotesCheckBox.setSelected(preferences.getBoolean(HTML_AUTOCOMPLETE_QUOTES, HTML_AUTOCOMPLETE_QUOTES_DEFAULT));
        completionOffersEndTagAfterLt.setSelected(preferences.getBoolean(HTML_COMPLETION_END_TAG_ADTER_LT, HTML_COMPLETION_END_TAG_ADTER_LT_DEFAULT));
        autoPopupCompletionWindow.setSelected(preferences.getBoolean(HTML_COMPLETION_AUTOPOPUP_WINDOW, HTML_COMPLETION_AUTOPOPUP_WINDOW_DEFAULT));
        endTagAutocompletionAutoPopupCheckBox.setSelected(preferences.getBoolean(HTML_END_TAG_AUTOCOMPLETION_AUTOPOPUP, HTML_END_TAG_AUTOCOMPLETION_AUTOPOPUP_DEFAULT));
        id2Saved.put(HTML_AUTOCOMPLETE_QUOTES_AFTER_EQS, autocompleteQuotesAfterEQSCheckBox.isSelected());
        id2Saved.put(HTML_AUTOCOMPLETE_QUOTES, autocompleteQuotesCheckBox.isSelected());
        id2Saved.put(HTML_COMPLETION_END_TAG_ADTER_LT, completionOffersEndTagAfterLt.isSelected());
        id2Saved.put(HTML_COMPLETION_AUTOPOPUP_WINDOW, autoPopupCompletionWindow.isSelected());
        id2Saved.put(HTML_END_TAG_AUTOCOMPLETION_AUTOPOPUP, endTagAutocompletionAutoPopupCheckBox.isSelected());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        autocompleteQuotesAfterEQSCheckBox = new javax.swing.JCheckBox();
        autocompleteQuotesCheckBox = new javax.swing.JCheckBox();
        completionOffersEndTagAfterLt = new javax.swing.JCheckBox();
        autoPopupCompletionWindow = new javax.swing.JCheckBox();
        endTagAutocompletionAutoPopupCheckBox = new javax.swing.JCheckBox();

        setPreferredSize(new java.awt.Dimension(550, 400));

        autocompleteQuotesAfterEQSCheckBox.setText(org.openide.util.NbBundle.getMessage(HtmlCompletionOptionsPanel.class, "HtmlCompletionOptionsPanel.autocompleteQuotesAfterEQSCheckBox.text")); // NOI18N
        autocompleteQuotesAfterEQSCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autocompleteQuotesAfterEQSCheckBoxActionPerformed(evt);
            }
        });

        autocompleteQuotesCheckBox.setText(org.openide.util.NbBundle.getMessage(HtmlCompletionOptionsPanel.class, "HtmlCompletionOptionsPanel.autocompleteQuotesCheckBox.text")); // NOI18N
        autocompleteQuotesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autocompleteQuotesCheckBoxActionPerformed(evt);
            }
        });

        completionOffersEndTagAfterLt.setText(org.openide.util.NbBundle.getMessage(HtmlCompletionOptionsPanel.class, "HtmlCompletionOptionsPanel.completionOffersEndTagAfterLt.text")); // NOI18N
        completionOffersEndTagAfterLt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                completionOffersEndTagAfterLtActionPerformed(evt);
            }
        });

        autoPopupCompletionWindow.setText(org.openide.util.NbBundle.getMessage(HtmlCompletionOptionsPanel.class, "HtmlCompletionOptionsPanel.autoPopupCompletionWindow.text")); // NOI18N
        autoPopupCompletionWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoPopupCompletionWindowActionPerformed(evt);
            }
        });

        endTagAutocompletionAutoPopupCheckBox.setText(org.openide.util.NbBundle.getMessage(HtmlCompletionOptionsPanel.class, "HtmlCompletionOptionsPanel.endTagAutocompletionAutoPopupCheckBox.text")); // NOI18N
        endTagAutocompletionAutoPopupCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(HtmlCompletionOptionsPanel.class, "HtmlCompletionOptionsPanel.endTagAutocompletionAutoPopupCheckBox.tooltip")); // NOI18N
        endTagAutocompletionAutoPopupCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endTagAutocompletionAutoPopupCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(autoPopupCompletionWindow)
                    .addComponent(autocompleteQuotesAfterEQSCheckBox)
                    .addComponent(autocompleteQuotesCheckBox)
                    .addComponent(completionOffersEndTagAfterLt)
                    .addComponent(endTagAutocompletionAutoPopupCheckBox))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(autoPopupCompletionWindow)
                .addGap(2, 2, 2)
                .addComponent(autocompleteQuotesAfterEQSCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(autocompleteQuotesCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(completionOffersEndTagAfterLt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(endTagAutocompletionAutoPopupCheckBox)
                .addContainerGap(259, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void autocompleteQuotesAfterEQSCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autocompleteQuotesAfterEQSCheckBoxActionPerformed
        preferences.putBoolean(HTML_AUTOCOMPLETE_QUOTES_AFTER_EQS, autocompleteQuotesAfterEQSCheckBox.isSelected());
    }//GEN-LAST:event_autocompleteQuotesAfterEQSCheckBoxActionPerformed

    private void autocompleteQuotesCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autocompleteQuotesCheckBoxActionPerformed
        preferences.putBoolean(HTML_AUTOCOMPLETE_QUOTES, autocompleteQuotesCheckBox.isSelected());
    }//GEN-LAST:event_autocompleteQuotesCheckBoxActionPerformed

    private void completionOffersEndTagAfterLtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_completionOffersEndTagAfterLtActionPerformed
        preferences.putBoolean(HTML_COMPLETION_END_TAG_ADTER_LT, completionOffersEndTagAfterLt.isSelected());
    }//GEN-LAST:event_completionOffersEndTagAfterLtActionPerformed

    private void autoPopupCompletionWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoPopupCompletionWindowActionPerformed
        preferences.putBoolean(HTML_COMPLETION_AUTOPOPUP_WINDOW, autoPopupCompletionWindow.isSelected());
    }//GEN-LAST:event_autoPopupCompletionWindowActionPerformed

    private void endTagAutocompletionAutoPopupCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endTagAutocompletionAutoPopupCheckBoxActionPerformed
        preferences.putBoolean(HTML_END_TAG_AUTOCOMPLETION_AUTOPOPUP, endTagAutocompletionAutoPopupCheckBox.isSelected());        
    }//GEN-LAST:event_endTagAutocompletionAutoPopupCheckBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoPopupCompletionWindow;
    private javax.swing.JCheckBox autocompleteQuotesAfterEQSCheckBox;
    private javax.swing.JCheckBox autocompleteQuotesCheckBox;
    private javax.swing.JCheckBox completionOffersEndTagAfterLt;
    private javax.swing.JCheckBox endTagAutocompletionAutoPopupCheckBox;
    // End of variables declaration//GEN-END:variables

    public static PreferencesCustomizer.Factory getCustomizerFactory() {
        return new PreferencesCustomizer.Factory() {

            @Override
            public PreferencesCustomizer create(Preferences preferences) {
                return new CodeCompletionPreferencesCustomizer(preferences);
            }
        };
    }


    private static class CodeCompletionPreferencesCustomizer implements PreferencesCustomizer {

        private final Preferences preferences;
        private static final String CUSTOMIZER_NAME = "htmlCodeCompletionPreferencesCustomizer"; //NOI18N
        private HtmlCompletionOptionsPanel component;

        private CodeCompletionPreferencesCustomizer(Preferences p) {
            preferences = p;
        }

        @Override
        public String getId() {
            return CUSTOMIZER_NAME;
        }

        @Override
        public String getDisplayName() {
            return CUSTOMIZER_NAME; //doesn't seem to show up anywhere, not implemented in java
        }

        @Override
        public HelpCtx getHelpCtx() {
            return new HelpCtx("netbeans.optionsDialog.editor.codeCompletion.java"); //NOI18N
        }

        @Override
        public JComponent getComponent() {
            if (component == null) {
                component = new HtmlCompletionOptionsPanel(preferences);
        }
            return component;
    }
    }


    String getSavedValue(String key) {
        return Boolean.toString(id2Saved.get(key));
}
    
    public static final class CustomCustomizerImpl extends PreferencesCustomizer.CustomCustomizer {
        @Override
        public String getSavedValue(PreferencesCustomizer customCustomizer, String key) {
            if(customCustomizer instanceof CodeCompletionPreferencesCustomizer) {
                return ((HtmlCompletionOptionsPanel) customCustomizer.getComponent()).getSavedValue(key);
            }
            return null;
        }
    }

}
