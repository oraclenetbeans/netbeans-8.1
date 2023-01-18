/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.declarative;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.java.hints.declarative.Condition.Instanceof;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintsParser.FixTextDescription;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.java.hints.declarative.DeclarativeHintsParser.Result;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.EmbeddingProvider;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 *
 * @author lahvac
 */
public class EmbeddingProviderImpl extends EmbeddingProvider {

    @Override
    public List<Embedding> getEmbeddings(Snapshot snapshot) {
        TokenSequence<DeclarativeHintTokenId> ts = snapshot.getTokenHierarchy().tokenSequence(DeclarativeHintTokenId.language());

        Result parsed = new DeclarativeHintsParser().parse(snapshot.getSource().getFileObject(), snapshot.getText(), ts);

        if (parsed.blocks.isEmpty()) {
            return Arrays.asList(Embedding.create(rules(snapshot, parsed)));
        } else {
            return Arrays.asList(Embedding.create(rules(snapshot, parsed)), Embedding.create(predicates(snapshot, parsed)));
        }
    }

    private List<Embedding> rules(Snapshot snapshot, Result parsed) {
        int index = 0;
        List<Embedding> result = new LinkedList<Embedding>();

        result.add(snapshot.create("//no-errors\n", "text/x-java"));
        result.add(snapshot.create(GLOBAL_PATTERN_PACKAGE, "text/x-java"));

        if (parsed.importsBlock != null) {
            result.add(snapshot.create(snapshot.getText().subSequence(parsed.importsBlock[0], parsed.importsBlock[1]), "text/x-java"));
            result.add(snapshot.create("\n", "text/x-java"));
        }

        result.add(snapshot.create(GLOBAL_PATTERN_CLASS, "text/x-java"));

        for (HintTextDescription hint : parsed.hints) {
            result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART1.replaceAll("\\{0\\}", "" + (index++)), "text/x-java"));

            StringBuilder builder = new StringBuilder();
            boolean first = true;

            for (Condition c : hint.conditions) {
                if (!(c instanceof Instanceof))
                    continue;

                Instanceof i = (Instanceof) c;

                if (!first) {
                    result.add(snapshot.create(", ", "text/x-java"));
                    builder.append(", ");
                }

                Embedding e1 = snapshot.create(i.constraintSpan[0], i.constraintSpan[1] - i.constraintSpan[0], "text/x-java");
                Embedding e2 = snapshot.create(" " + i.variable, "text/x-java");

                result.add(Embedding.create(Arrays.asList(e1, e2)));

                builder.append(i.constraint);
                builder.append(" " + i.variable);

                first = false;
            }

            result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));
            result.add(snapshot.create(hint.textStart, hint.textEnd - hint.textStart, "text/x-java"));
            result.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));

            for (FixTextDescription f : hint.fixes) {
                int[] fixes = f.fixSpan;
                result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART1.replaceAll("\\{0\\}", "" + (index++)), "text/x-java"));
                result.add(snapshot.create(builder.toString(), "text/x-java"));
                result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));
                result.add(snapshot.create(fixes[0], fixes[1] - fixes[0], "text/x-java"));
                result.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));
            }
        }

        result.add(snapshot.create(GLOBAL_PATTERN_SUFFIX, "text/x-java"));

        return result;
    }
    
    private List<Embedding> predicates(Snapshot snapshot, Result parsed) {
        List<Embedding> result = new LinkedList<Embedding>();

        result.add(snapshot.create(GLOBAL_PATTERN_PACKAGE, "text/x-java"));

        if (parsed.importsBlock != null) {
            result.add(snapshot.create(parsed.importsBlock[0], parsed.importsBlock[1] - parsed.importsBlock[0], "text/x-java"));
            result.add(snapshot.create("\n", "text/x-java"));
        }

        for (String imp : MethodInvocationContext.AUXILIARY_IMPORTS) {
            result.add(snapshot.create(imp + "\n", "text/x-java"));
        }

        result.add(snapshot.create(GLOBAL_PATTERN_CLASS, "text/x-java"));
        result.add(snapshot.create(CUSTOM_CONDITIONS_VARIABLES, "text/x-java"));

        for (int[] span : parsed.blocks) {
            result.add(snapshot.create(span[0], span[1] - span[0], "text/x-java"));
        }

        result.add(snapshot.create(GLOBAL_PATTERN_SUFFIX, "text/x-java"));

        return result;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void cancel() {}

    private static final String GLOBAL_PATTERN_PACKAGE = "package $;\n";
    private static final String GLOBAL_PATTERN_CLASS = "class $ {\n";
    private static final String GLOBAL_PATTERN_SUFFIX = "\n}\n";
    private static final String SNIPPET_PATTERN_PREFIX_PART1 = "private void ${0}(";
    private static final String SNIPPET_PATTERN_PREFIX_PART2 = ") throws Throwable {\n";
    private static final String SNIPPET_PATTERN_SUFFIX = " ;\n}\n";

    private static final String CUSTOM_CONDITIONS_VARIABLES = "private final Context context = null;\nprivate final Matcher matcher = null;\n";

    @MimeRegistration(mimeType=DeclarativeHintTokenId.MIME_TYPE, service=TaskFactory.class)
    public static final class FactoryImpl extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singletonList(new EmbeddingProviderImpl());
        }

    }

}
