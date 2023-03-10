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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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

package org.netbeans.modules.editor.java;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.*;

import org.netbeans.api.editor.EditorActionNames;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldUtilities;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.api.java.source.CodeStyle;
import org.netbeans.editor.*;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.ext.java.*;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplateManager;
import org.netbeans.modules.editor.MainMenuAction;
import org.netbeans.modules.editor.NbEditorKit;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.modules.java.editor.codegen.InsertSemicolonAction;
import org.netbeans.modules.java.editor.imports.ClipboardHandler;
import org.netbeans.modules.java.editor.imports.FastImportAction;
import org.netbeans.modules.java.editor.imports.JavaFixAllImports;
import org.netbeans.modules.java.editor.overridden.GoToSuperTypeAction;
import org.netbeans.modules.java.editor.rename.InstantRenameAction;
import org.netbeans.modules.java.editor.semantic.GoToMarkOccurrencesAction;
import org.netbeans.spi.editor.typinghooks.DeletedTextInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
* Java editor kit with appropriate document
*
* @author Miloslav Metelka
* @version 1.00
*/

public class JavaKit extends NbEditorKit {

    public static final String JAVA_MIME_TYPE = "text/x-java"; // NOI18N

    static final long serialVersionUID =-5445829962533684922L;
    
    private static final boolean INSTANT = Boolean.getBoolean("org.netbeans.modules.java.refactoring.instantRename");

//    private static final Object sourceLevelKey = new Object();

    public JavaKit(){
    }
    
    public String getContentType() {
        return JAVA_MIME_TYPE;
    }

//    /** Create new instance of syntax coloring scanner
//    * @param doc document to operate on. It can be null in the cases the syntax
//    *   creation is not related to the particular document
//    */
//    public Syntax createSyntax(Document doc) {
//        // XXX: sourcelevel can be subject of changes, ignored by this cache
//        // Should not be a problem here however. Covered by #171330.
//        String sourceLevel = (String) doc.getProperty(sourceLevelKey);
//        if (sourceLevel == null) {
//            sourceLevel = getSourceLevel((BaseDocument) doc);
//            doc.putProperty(sourceLevelKey, sourceLevel);
//        }
//        return new JavaSyntax(sourceLevel);
//    }

    public String getSourceLevel(BaseDocument doc) {
        DataObject dob = NbEditorUtilities.getDataObject(doc);
        return dob != null ? SourceLevelQuery.getSourceLevel(dob.getPrimaryFile()) : null;
    }

//    /** Create the formatter appropriate for this kit */
//    public Formatter createFormatter() {
//        return new JavaFormatter(this.getClass());
//    }

    protected void initDocument(BaseDocument doc) {
//        doc.addLayer(new JavaDrawLayerFactory.JavaLayer(),
//                JavaDrawLayerFactory.JAVA_LAYER_VISIBILITY);
        doc.putProperty(SyntaxUpdateTokens.class,
              new SyntaxUpdateTokens() {
                  
                  private List tokenList = new ArrayList();
                  
                  public void syntaxUpdateStart() {
                      tokenList.clear();
                  }
      
                  public List syntaxUpdateEnd() {
                      return tokenList;
                  }
      
                  public void syntaxUpdateToken(TokenID id, TokenContextPath contextPath, int offset, int length) {
                      if (JavaTokenContext.LINE_COMMENT == id) {
                          tokenList.add(new TokenInfo(id, contextPath, offset, length));
                      }
                  }
              }
          );
      }
    
    private static final String[] getSetIsPrefixes = new String[] {
                "get", "set", "is" // NOI18N
            };

    /** Switch first letter of word to capital and insert 'get'
    * at word begining.
    */
    public static final String makeGetterAction = "make-getter"; // NOI18N

    /** Switch first letter of word to capital and insert 'set'
    * at word begining.
    */
    public static final String makeSetterAction = "make-setter"; // NOI18N

    /** Switch first letter of word to capital and insert 'is'
    * at word begining.
    */
    public static final String makeIsAction = "make-is"; // NOI18N

    /** Add the watch depending on the context under the caret */
    public static final String addWatchAction = "add-watch"; // NOI18N

    /** Toggle the breakpoint of the current line */
    public static final String toggleBreakpointAction = "toggle-breakpoint"; // NOI18N

    /** Debug source and line number */
    public static final String abbrevDebugLineAction = "abbrev-debug-line"; // NOI18N

    /** Menu item for adding all necessary imports in a file */
    public static final String fixImportsAction = "fix-imports"; // NOI18N

    /** Open dialog for choosing the import statement to be added */
    public static final String fastImportAction = "fast-import"; // NOI18N

    /** Opens Go To Class dialog */
    //public static final String gotoClassAction = "goto-class"; //NOI18N

    public static final String tryCatchAction = "try-catch"; // NOI18N

    public static final String javaDocShowAction = "javadoc-show-action"; // NOI18N

    public static final String expandAllJavadocFolds = "expand-all-javadoc-folds"; //NOI18N

    public static final String collapseAllJavadocFolds = "collapse-all-javadoc-folds"; //NOI18N

    public static final String expandAllCodeBlockFolds = "expand-all-code-block-folds"; //NOI18N

    public static final String collapseAllCodeBlockFolds = "collapse-all-code-block-folds"; //NOI18N

    public static final String selectNextElementAction = "select-element-next"; //NOI18N

    public static final String selectPreviousElementAction = "select-element-previous"; //NOI18N

//    public static Action create(FileObject file) {
//        initialize();
//
//        return name2Action.get(file.getName());
//    }
//
//    private static Map<String, Action> name2Action;
//
//    private static synchronized void initialize() {
//        if (name2Action != null) {
//            return ;
//        }
//
//        name2Action = new HashMap<String, Action>();
//
//        for (BaseAction a : createActionsForLayer()) {
//            name2Action.put((String) a.getValue(Action.NAME), a);
//
////            System.err.println("<file name=\"" + (String) a.getValue(Action.NAME) + ".instance\">");
////            System.err.println("    <attr name=\"instanceCreate\" methodvalue=\"org.netbeans.modules.editor.java.JavaKit.create\" />");
////            System.err.println("</file>");
//        }
//    }


    @Override
    protected Action[] createActions() {
        Action[] superActions = super.createActions();

        Action[] actions = new BaseAction[] {
            new PrefixMakerAction(makeGetterAction, "get", getSetIsPrefixes), // NOI18N
            new PrefixMakerAction(makeSetterAction, "set", getSetIsPrefixes), // NOI18N
            new PrefixMakerAction(makeIsAction, "is", getSetIsPrefixes), // NOI18N
            new ToggleCommentAction("//"), // NOI18N
            new JavaGenerateFoldPopupAction(), // NO_KEYBINDING in super
            new InstantRenameAction(),
            new InsertSemicolonAction(true),
            new InsertSemicolonAction(false),
            new SelectCodeElementAction(selectNextElementAction, true),
            new SelectCodeElementAction(selectPreviousElementAction, false),
            new JavaMoveCodeElementAction(EditorActionNames.moveCodeElementUp, false),
            new JavaMoveCodeElementAction(EditorActionNames.moveCodeElementDown, true),

            new FastImportAction(),
            new GoToSuperTypeAction(),

            new GoToMarkOccurrencesAction(false),
            new GoToMarkOccurrencesAction(true),
            new ClipboardHandler.JavaCutAction(),
        };
        final Action[] value = TextAction.augmentList(superActions, actions);
        
        return !INSTANT ? value : removeInstant(value);
    }
    
    private Action[] removeInstant(Action[] actions) {
        List<Action> value = new LinkedList<>();
        for (Action action : actions) {
            if(!(action instanceof InstantRenameAction)) {
                value.add(action);
            }
        }
        return value.toArray(new Action[value.size()]);
    }

    @Override
    public void install(JEditorPane c) {
        super.install(c);
        ClipboardHandler.install(c);
    }

    @EditorActionRegistration(name = generateGoToPopupAction, mimeType = JAVA_MIME_TYPE)
    public static class JavaGenerateGoToPopupAction extends NbGenerateGoToPopupAction {

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
        }

        private void addAcceleretors(Action a, JMenuItem item, JTextComponent target){
            // Try to get the accelerator
            Keymap km = target.getKeymap();
            if (km != null) {

                KeyStroke[] keys = km.getKeyStrokesForAction(a);
                if (keys != null && keys.length > 0) {
                    item.setAccelerator(keys[0]);
                }else if (a!=null){
                    KeyStroke ks = (KeyStroke)a.getValue(Action.ACCELERATOR_KEY);
                    if (ks!=null) {
                        item.setAccelerator(ks);
                    }
                }
            }
        }

        private void addAction(JTextComponent target, JMenu menu, Action a){
            if (a != null) {
                String actionName = (String) a.getValue(Action.NAME);
                JMenuItem item = null;
                if (a instanceof BaseAction) {
                    item = ((BaseAction)a).getPopupMenuItem(target);
                }
                if (item == null) {
                    // gets trimmed text that doesn' contain "go to"
                    String itemText = (String)a.getValue(ExtKit.TRIMMED_TEXT);
                    if (itemText == null){
                        itemText = getItemText(target, actionName, a);
                    }
                    if (itemText != null) {
                        item = new JMenuItem(itemText);
                        Mnemonics.setLocalizedText(item, itemText);
                        item.addActionListener(a);
                        addAcceleretors(a, item, target);
                        item.setEnabled(a.isEnabled());
                        Object helpID = a.getValue ("helpID"); // NOI18N
                        if (helpID != null && (helpID instanceof String))
                            item.putClientProperty ("HelpID", helpID); // NOI18N
                    }else{
                        if (ExtKit.gotoSourceAction.equals(actionName)){
                            item = new JMenuItem(NbBundle.getBundle(JavaKit.class).getString("goto_source_open_source_not_formatted")); //NOI18N
                            addAcceleretors(a, item, target);
                            item.setEnabled(false);
                        }
                    }
                }

                if (item != null) {
                    menu.add(item);
                }

            }
        }

        protected void addAction(JTextComponent target, JMenu menu,
        String actionName) {
            BaseKit kit = Utilities.getKit(target);
            if (kit == null) return;
            Action a = kit.getActionByName(actionName);
            if (a!=null){
                addAction(target, menu, a);
            } else { // action-name is null, add the separator
                menu.addSeparator();
            }
        }

        protected String getItemText(JTextComponent target, String actionName, Action a) {
            String itemText;
            if (a instanceof BaseAction) {
                itemText = ((BaseAction)a).getPopupMenuText(target);
            } else {
                itemText = actionName;
            }
            return itemText;
        }

        public JMenuItem getPopupMenuItem(final JTextComponent target) {
            String menuText = NbBundle.getBundle(JavaKit.class).getString("generate-goto-popup"); //NOI18N
            JMenu jm = new JMenu(menuText);
            addAction(target, jm, ExtKit.gotoSourceAction);
            addAction(target, jm, ExtKit.gotoDeclarationAction);
            addAction(target, jm, gotoSuperImplementationAction);
            addAction(target, jm, ExtKit.gotoAction);
            return jm;
        }

    }

    @EditorActionRegistration(
            name = abbrevDebugLineAction,
            mimeType = JAVA_MIME_TYPE
    )
    public static class AbbrevDebugLineAction extends BaseAction {

        public AbbrevDebugLineAction() {
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                BaseDocument doc = (BaseDocument)target.getDocument();
                StringBuffer sb = new StringBuffer("System.out.println(\""); // NOI18N
                String title = (String)doc.getProperty(Document.TitleProperty);
                if (title != null) {
                    sb.append(title);
                    sb.append(':');
                }
                try {
                    sb.append(Utilities.getLineOffset(doc, target.getCaret().getDot()) + 1);
                } catch (BadLocationException e) {
                }
                sb.append(' ');

                BaseKit kit = Utilities.getKit(target);
                if (kit == null) return;
                Action a = kit.getActionByName(BaseKit.insertContentAction);
                if (a != null) {
                    Utilities.performAction(
                        a,
                        new ActionEvent(target, ActionEvent.ACTION_PERFORMED, sb.toString()),
                        target
                    );
                }
            }
        }

    }

    
    public static class JavaTypedBreakInterceptor implements TypedBreakInterceptor {

        private boolean isJavadocTouched = false;

        @Override
        public boolean beforeInsert(Context context) throws BadLocationException {
            return false;
        }

        @Override
        public void insert(MutableContext context) throws BadLocationException {
            int dotPos = context.getCaretOffset();
            Document doc = context.getDocument();
            
            if (TypingCompletion.posWithinString(doc, dotPos)) {
                if (CodeStyle.getDefault(doc).wrapAfterBinaryOps()) {
                    context.setText("\" +\n \"", 3, 6); // NOI18N
                } else {
                    context.setText("\"\n + \"", 1, 6); // NOI18N
                }
                return;
            } 
            
            BaseDocument baseDoc = (BaseDocument) context.getDocument();
            if (TypingCompletion.isCompletionSettingEnabled() && TypingCompletion.isAddRightBrace(baseDoc, dotPos)) {
                boolean insert[] = {true};
                int end = TypingCompletion.getRowOrBlockEnd(baseDoc, dotPos, insert);
                if (insert[0]) {
                    doc.insertString(end, "}", null); // NOI18N
                    Indent.get(doc).indentNewLine(end);
                }
                context.getComponent().getCaret().setDot(dotPos);
            } else {
                if (TypingCompletion.blockCommentCompletion(context)) {
                    blockCommentComplete(doc, dotPos, context);
                }
                isJavadocTouched = TypingCompletion.javadocBlockCompletion(context);
                if (isJavadocTouched) {
                    blockCommentComplete(doc, dotPos, context);
                }
            }
        }

        @Override
        public void afterInsert(Context context) throws BadLocationException {
            if (isJavadocTouched) {
                Lookup.Result<TextAction> res = MimeLookup.getLookup(MimePath.parse("text/x-javadoc")).lookupResult(TextAction.class); // NOI18N
                ActionEvent newevt = new ActionEvent(context.getComponent(), ActionEvent.ACTION_PERFORMED, "fix-javadoc"); // NOI18N
                for (TextAction action : res.allInstances()) {
                    action.actionPerformed(newevt);
                }
                isJavadocTouched = false;
            }
        }

        @Override
        public void cancelled(Context context) {
        }

        private void blockCommentComplete(Document doc, int dotPos, MutableContext context) throws BadLocationException {
            // note that the formater will add one line of javadoc
            doc.insertString(dotPos, "*/", null); // NOI18N
            Indent.get(doc).indentNewLine(dotPos);
            context.getComponent().getCaret().setDot(dotPos);
        }

        @MimeRegistrations({
            @MimeRegistration(mimeType = JAVA_MIME_TYPE, service = TypedBreakInterceptor.Factory.class),
            @MimeRegistration(mimeType = "text/x-javadoc", service = TypedBreakInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-string", service = TypedBreakInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-character", service = TypedBreakInterceptor.Factory.class) //NOI18N
        })
        @MimeRegistration(mimeType = JAVA_MIME_TYPE, service = TypedBreakInterceptor.Factory.class)
        public static class JavaFactory implements TypedBreakInterceptor.Factory {

            @Override
            public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
                return new JavaTypedBreakInterceptor();
            }
        }
    }
    
    public static class JavaDeletedTextInterceptor implements DeletedTextInterceptor {

        @Override
        public boolean beforeRemove(Context context) throws BadLocationException {
            return false;
        }

        @Override
        public void remove(Context context) throws BadLocationException {            
            char removedChar = context.getText().charAt(0);
            switch(removedChar) {
                case '(':
                case '[':
                    if (TypingCompletion.isCompletionSettingEnabled())
                        TypingCompletion.removeBrackets(context);
                    break;
                case '\"':
                case '\'':
                    if (TypingCompletion.isCompletionSettingEnabled())
                        TypingCompletion.removeCompletedQuote(context);
                    break;
            }
        }

        @Override
        public void afterRemove(Context context) throws BadLocationException {
        }

        @Override
        public void cancelled(Context context) {
        }

        @MimeRegistrations({
            @MimeRegistration(mimeType = JAVA_MIME_TYPE, service = DeletedTextInterceptor.Factory.class),
            @MimeRegistration(mimeType = "text/x-javadoc", service = DeletedTextInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-string", service = DeletedTextInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-character", service = DeletedTextInterceptor.Factory.class) //NOI18N
        })
        public static class Factory implements DeletedTextInterceptor.Factory {

            @Override
            public DeletedTextInterceptor createDeletedTextInterceptor(MimePath mimePath) {
                return new JavaDeletedTextInterceptor();
            }
        }
    }
    
    public static class JavaTypedTextInterceptor implements TypedTextInterceptor {
        private int caretPosition = -1;

        @Override
        public boolean beforeInsert(Context context) throws BadLocationException {
            return false;
        }

        @Override
        public void insert(MutableContext context) throws BadLocationException {
            char insertedChar = context.getText().charAt(0);
            switch(insertedChar) {
                case '(':
                case '[':
                    if (TypingCompletion.isCompletionSettingEnabled())
                        TypingCompletion.completeOpeningBracket(context);
                    break;
                case ')':
                case ']':
                    if (TypingCompletion.isCompletionSettingEnabled())
                        caretPosition = TypingCompletion.skipClosingBracket(context);
                    break;
                case ';':
                    if (TypingCompletion.isCompletionSettingEnabled())
                        caretPosition = TypingCompletion.moveOrSkipSemicolon(context);
                    break;
                case '\"':
                case '\'':
                    if (TypingCompletion.isCompletionSettingEnabled())
                        caretPosition = TypingCompletion.completeQuote(context);
                    break;
            }
        }

        @Override
        public void afterInsert(Context context) throws BadLocationException { 
            if (caretPosition != -1) {
                context.getComponent().setCaretPosition(caretPosition);
                caretPosition = -1;
            }
        }

        @Override
        public void cancelled(Context context) {
        }

        @MimeRegistrations({
            @MimeRegistration(mimeType = JAVA_MIME_TYPE, service = TypedTextInterceptor.Factory.class),
            @MimeRegistration(mimeType = "text/x-javadoc", service = TypedTextInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-string", service = TypedTextInterceptor.Factory.class), //NOI18N
            @MimeRegistration(mimeType = "text/x-java-character", service = TypedTextInterceptor.Factory.class) //NOI18N
        })
        public static class Factory implements TypedTextInterceptor.Factory {

            @Override
            public TypedTextInterceptor createTypedTextInterceptor(MimePath mimePath) {
                return new JavaTypedTextInterceptor();
            }
        }
    }
    
    @EditorActionRegistration(
            name = expandAllJavadocFolds,
            mimeType = JAVA_MIME_TYPE,
            popupText = "#popup-expand-all-javadoc-folds"
    )
    public static class ExpandAllJavadocFolds extends BaseAction {

        public ExpandAllJavadocFolds(){
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            // Hierarchy locking done in the utility method
            FoldUtilities.expand(hierarchy, JavaFoldManager.JAVADOC_FOLD_TYPE);
        }
    }

    @EditorActionRegistration(
            name = collapseAllJavadocFolds,
            mimeType = JAVA_MIME_TYPE,
            shortDescription = "#popup-collapse-all-javadoc-folds"
    )
    public static class CollapseAllJavadocFolds extends BaseAction{

        public CollapseAllJavadocFolds(){
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            // Hierarchy locking done in the utility method
            FoldUtilities.collapse(hierarchy, JavaFoldManager.JAVADOC_FOLD_TYPE);
        }
    }

    @EditorActionRegistration(
            name = expandAllCodeBlockFolds,
            mimeType = JAVA_MIME_TYPE,
            popupText = "#popup-expand-all-code-block-folds"

    )
    public static class ExpandAllCodeBlockFolds extends BaseAction{

        public ExpandAllCodeBlockFolds(){
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            // Hierarchy locking done in the utility method
            List types = new ArrayList();
            types.add(JavaFoldManager.CODE_BLOCK_FOLD_TYPE);
            types.add(JavaFoldManager.IMPORTS_FOLD_TYPE);
            FoldUtilities.expand(hierarchy, types);
        }
    }

    @EditorActionRegistration(
            name = collapseAllCodeBlockFolds,
            mimeType = JAVA_MIME_TYPE,
            shortDescription = "#popup-collapse-all-code-block-folds"
    )
    public static class CollapseAllCodeBlockFolds extends BaseAction {

        public CollapseAllCodeBlockFolds(){
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            // Hierarchy locking done in the utility method
            List types = new ArrayList();
            types.add(JavaFoldManager.CODE_BLOCK_FOLD_TYPE);
            types.add(JavaFoldManager.IMPORTS_FOLD_TYPE);
            FoldUtilities.collapse(hierarchy, types);
        }
    }

// extends from NbEditorKit
//    @EditorActionRegistration(name = generateFoldPopupAction, mimeType = JAVA_MIME_TYPE)
    public static class JavaGenerateFoldPopupAction extends GenerateFoldPopupAction {

        protected void addAdditionalItems(JTextComponent target, JMenu menu){
            addAction(target, menu, collapseAllJavadocFolds);
            addAction(target, menu, expandAllJavadocFolds);
            setAddSeparatorBeforeNextAction(true);
            addAction(target, menu, collapseAllCodeBlockFolds);
            addAction(target, menu, expandAllCodeBlockFolds);
        }

    }

// extends from NbEditorKit
//    @EditorActionRegistration(name = gotoDeclarationAction, mimeType = JAVA_MIME_TYPE)
    @Deprecated //unused, to be removed:
    public static class JavaGoToDeclarationAction extends GotoDeclarationAction {

        public JavaGoToDeclarationAction() {
        }

        public @Override boolean gotoDeclaration(JTextComponent target) {
            if (!(target.getDocument() instanceof BaseDocument)) // Fixed #113062
                return false;
            GoToSupport.goTo((BaseDocument) target.getDocument(), target.getCaretPosition(), false);
            return true;
        }
    }

    @EditorActionRegistration(
            name = gotoSourceAction,
            mimeType = JAVA_MIME_TYPE,
            popupText = "#goto_source_open_source_not_formatted"
    )
    public static class JavaGoToSourceAction extends BaseAction {

        static final long serialVersionUID =-6440495023918097760L;

        public JavaGoToSourceAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | SAVE_POSITION);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null && (target.getDocument() instanceof BaseDocument)) {
                GoToSupport.goTo((BaseDocument) target.getDocument(), target.getCaretPosition(), true);
            }
        }

        public String getPopupMenuText(JTextComponent target) {
            return NbBundle.getBundle(JavaKit.class).getString("goto_source_open_source_not_formatted"); //NOI18N
        }

        protected Class getShortDescriptionBundleClass() {
            return BaseKit.class;
        }
    }

    @EditorActionRegistration(
            name = fixImportsAction,
            mimeType = JAVA_MIME_TYPE,
            shortDescription = "#desc-fix-imports",
            popupText = "#popup-fix-imports"
    )
    public static class JavaFixImports extends BaseAction {

        public JavaFixImports() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            putValue(TRIMMED_TEXT, NbBundle.getBundle(JavaKit.class).getString("fix-imports-trimmed"));
            putValue(SHORT_DESCRIPTION, NbBundle.getBundle(JavaKit.class).getString("desc-fix-imports")); // NOI18N
            putValue(POPUP_MENU_TEXT, NbBundle.getBundle(JavaKit.class).getString("popup-fix-imports")); // NOI18N
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Document doc = target.getDocument();
                Object source = doc.getProperty(Document.StreamDescriptionProperty);

                if (source instanceof DataObject) {
                    FileObject fo = ((DataObject) source).getPrimaryFile();

                    JavaFixAllImports.getDefault().fixAllImports(fo, target);
                }
            }
        }

        public static final class GlobalAction extends MainMenuAction {
            public GlobalAction() {
                super();
                postSetMenu();
            }

            protected String getMenuItemText() {
                return NbBundle.getBundle(GlobalAction.class).getString("fix-imports-main-menu-source-item"); //NOI18N
            }

            protected String getActionName() {
                return fixImportsAction;
            }
        } // End of GlobalAction class
    } // End of JavaFixImports class

    @EditorActionRegistration(
            name = gotoHelpAction,
            mimeType = JAVA_MIME_TYPE,
            shortDescription = "#java-desc-goto-help",
            popupText = "#show_javadoc"
    )
    public static class JavaGotoHelpAction extends BaseAction {

        public JavaGotoHelpAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET |SAVE_POSITION);
            putValue ("helpID", JavaGotoHelpAction.class.getName ()); // NOI18N
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                GoToSupport.goToJavadoc(target.getDocument(), target.getCaretPosition());
            }
        }

        public String getPopupMenuText(JTextComponent target) {
            return NbBundle.getBundle(JavaKit.class).getString("show_javadoc"); // NOI18N
        }

    }
}
