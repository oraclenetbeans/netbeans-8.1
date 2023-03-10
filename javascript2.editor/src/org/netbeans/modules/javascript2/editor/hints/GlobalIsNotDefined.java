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
package org.netbeans.modules.javascript2.editor.hints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.hints.JsHintsProvider.JsRuleContext;
import org.netbeans.modules.javascript2.editor.index.JsIndex;
import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.model.JsElement;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.Occurrence;
import org.netbeans.modules.javascript2.editor.model.Type;
import org.netbeans.modules.javascript2.editor.model.impl.ModelExtender;
import org.netbeans.modules.javascript2.editor.model.impl.ModelUtils;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.web.common.api.LexerUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Petr Pisl
 */
public class GlobalIsNotDefined extends JsAstRule {

    private static final List<String> KNOWN_GLOBAL_OBJECTS = Arrays.asList("window", "document", "console", //NOI18N
            "clearInterval", "clearTimeout", "event", "frames", "history", //NOI18N
            "Image", "location", "name", "navigator", "Option", "parent", "screen", "setInterval", "setTimeout", //NOI18N
            "XMLHttpRequest", "JSON", "Date", "undefined", "Math", "$", "jQuery",  //NOI18N
            "Error", "EvalError", "RangeError", "ReferenceError", "SyntaxError", "TypeError", "URIError", //NOI18N
            Type.ARRAY, Type.OBJECT, Type.BOOLEAN, Type.NULL, Type.NUMBER, Type.REGEXP, Type.STRING, Type.UNDEFINED, Type.UNRESOLVED);
    
    @Override
    void computeHints(JsRuleContext context, List<Hint> hints, int offset, HintsProvider.HintsManager manager) throws BadLocationException {
        if (!JsTokenId.JAVASCRIPT_MIME_TYPE.equals(context.getJsParserResult().getSnapshot().getMimePath().getPath())) {
            // compute this hint just for the js files.
            return;
        }
        JsObject globalObject = context.getJsParserResult().getModel().getGlobalObject();
        Collection<? extends JsObject> variables = ModelUtils.getVariables((DeclarationScope)globalObject);
        FileObject fo = context.parserResult.getSnapshot().getSource().getFileObject();
        JsIndex jsIndex = JsIndex.get(fo);
        Set<String> namesFromFrameworks = new HashSet<String>();
        for (JsObject globalFiles:  ModelExtender.getDefault().getExtendingGlobalObjects(context.getJsParserResult().getSnapshot().getSource().getFileObject())) {
            for (JsObject global : globalFiles.getProperties().values()) {
                namesFromFrameworks.add(global.getName());
            }
        }
        Collection<String> jsHintGlobalDefinition = findJsHintGlobalDefinition(context.getJsParserResult().getSnapshot());
        for (JsObject variable : variables) {
            String varName = variable.getName();
            if(!variable.isDeclared() 
                    && !KNOWN_GLOBAL_OBJECTS.contains(varName)
                    && !namesFromFrameworks.contains(varName)
                    && !jsHintGlobalDefinition.contains(varName)
                    && (variable.getJSKind() == JsElement.Kind.VARIABLE
                    || variable.getJSKind() == JsElement.Kind.OBJECT)) {

                if (context.isCancelled()) {
                    return;
                }

                // check whether is defined as window property
                Collection<? extends IndexResult> findByFqn = jsIndex.findByFqn("window." + varName, JsIndex.FIELD_BASE_NAME);
                if (findByFqn.isEmpty()) {
                    if (variable.getOccurrences().isEmpty()) {
                        addHint(context, hints, offset, varName, variable.getOffsetRange());
                    } else {
                        for(Occurrence occurrence : variable.getOccurrences()) {
                            addHint(context, hints, offset, varName, occurrence.getOffsetRange());
                        }
                    }
                }
            }
        }
    }

    private void addHint(JsRuleContext context, List<Hint> hints, int offset, String name, OffsetRange range) throws BadLocationException {
        boolean add = false;
        Document document = context.getJsParserResult().getSnapshot().getSource().getDocument(false);
        if (offset > -1) {
            if (document != null && document instanceof BaseDocument) {
                BaseDocument baseDocument = (BaseDocument)document;
                int lineOffset = Utilities.getLineOffset(baseDocument, offset);
                int lineOffsetRange = Utilities.getLineOffset(baseDocument, range.getStart());
                add = lineOffset == lineOffsetRange;
            }
        } else {
            add = true;
            if (document != null) {
                ((AbstractDocument)document).readLock();
                try {
                    TokenSequence ts = LexerUtils.getTokenSequence(document, range.getStart(), JsTokenId.javascriptLanguage(), true);
                    ts.move(range.getStart());
                    if (ts.moveNext()) {
                        add = ts.token().id() != JsTokenId.DOC_COMMENT;
                    }
                } finally {
                    ((AbstractDocument) document).readUnlock();
                }
                
            }
        }
        
        if (add) {
            List<HintFix> fixes;
            fixes = new ArrayList<HintFix>();
            fixes.add(new AddJsHintFix(context.getJsParserResult().getSnapshot(), offset, name));
            hints.add(new Hint(this, Bundle.JsGlobalIsNotDefinedHintDesc(name),
                    context.getJsParserResult().getSnapshot().getSource().getFileObject(),
                    ModelUtils.documentOffsetRange(context.getJsParserResult(),
                    range.getStart(), range.getEnd()), fixes, 500));
        }
    }
    
    @Override
    public Set<?> getKinds() {
        return Collections.singleton(JsAstRule.JS_OTHER_HINTS);
    }

    @Override
    public String getId() {
        return "jsglobalisnotdefined.hint";
    }

    @NbBundle.Messages({
            "JsGlobalIsNotDefinedDesc=The global variable is not declared.",
            "# {0} - name of global variable",
            "JsGlobalIsNotDefinedHintDesc=The global variable \"{0}\" is not declared."})
    @Override
    public String getDescription() {
        return Bundle.JsGlobalIsNotDefinedDesc();
    }

    @NbBundle.Messages("JsGlobalIsNotDefinedDN=The global variable is not declared")
    @Override
    public String getDisplayName() {
        return Bundle.JsGlobalIsNotDefinedDN();
    }

    @Override
    public HintSeverity getDefaultSeverity() {
        return HintSeverity.WARNING;
    }
   
    @Override
    public boolean getDefaultEnabled() {
        return true;
    }
    
    private Collection<String> findJsHintGlobalDefinition(Snapshot snapshot) {
        ArrayList<String> names = new ArrayList<String>();
        Collection<Identifier> definedGlobal = JSHintSupport.getDefinedGlobal(snapshot, 0);
        for (Identifier identifier: definedGlobal) {
            names.add(identifier.getName());
        }
        return names;
    }
    
    static class AddJsHintFix implements HintFix {

        private final Snapshot snapshot;
        private final String name;
        private int offset;

        public AddJsHintFix(final Snapshot snapshot, final int offset, final String name) {
            this.snapshot = snapshot;
            this.name = name;
            this.offset = offset;
        }
        
        
        @Override
        @NbBundle.Messages({"AddGlobalJsHint_Description=Generate JsHint global directive"})
        public String getDescription() {
            return Bundle.AddGlobalJsHint_Description();
        }

        @Override
        public void implement() throws Exception {
            JSHintSupport.addGlobalInline(snapshot, offset, name);
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }
        
    }
    
}
