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
package org.netbeans.modules.javascript2.editor.model.impl;

import java.util.Collections;
import org.netbeans.modules.csl.api.Documentation;
import org.netbeans.modules.javascript2.editor.doc.JsDocumentationPrinter;
import org.netbeans.modules.javascript2.editor.doc.spi.DocParameter;
import org.netbeans.modules.javascript2.editor.doc.spi.JsComment;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.model.JsElement;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.openide.util.Exceptions;

/**
 *
 * @author Petr Pisl
 */
public class ParameterObject extends JsObjectImpl {

    public ParameterObject(JsObject parent, Identifier name, String mimeType, String sourceLabel) {
        super(parent, name, name.getOffsetRange(), mimeType, sourceLabel);
        if (hasExactName()) {
            addOccurrence(name.getOffsetRange());
        }
    }

    @Override
    public boolean isDeclared() {
        return true;
    }

    @Override
    public Kind getJSKind() {
        return JsElement.Kind.PARAMETER;
    }

    @Override
    public Documentation getDocumentation() {
        final String[] result = new String[1];
        try {
            ParserManager.parse(Collections.singleton(Source.create(getParent().getFileObject())), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Parser.Result parserResult = resultIterator.getParserResult(getParent().getOffset());
                    if (parserResult instanceof JsParserResult) {
                        JsDocumentationHolder holder = ((JsParserResult) parserResult).getDocumentationHolder();
                        JsComment comment = holder.getCommentForOffset(getParent().getOffset(), holder.getCommentBlocks());
                        if (comment != null) {
                            for (DocParameter docParameter : comment.getParameters()) {
                                if (docParameter.getParamName().getName().equals(getDeclarationName().getName())) {
                                    result[0] = JsDocumentationPrinter.printParameterDocumentation(docParameter);
                                    return;
                                }
                            }
                        }
                    }
                }
            });
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (result[0] != null && !result[0].isEmpty()) {
            return Documentation.create(result[0]);
        }
        return null;
    }
}
