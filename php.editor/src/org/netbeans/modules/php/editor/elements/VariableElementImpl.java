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

package org.netbeans.modules.php.editor.elements;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.php.api.editor.PhpClass;
import org.netbeans.modules.php.api.editor.PhpVariable;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.FileElementQuery;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.elements.TypeResolver;
import org.netbeans.modules.php.editor.api.elements.VariableElement;
import org.netbeans.modules.php.editor.index.PHPIndexer;
import org.netbeans.modules.php.editor.index.Signature;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.model.nodes.ASTNodeInfo;
import org.netbeans.modules.php.editor.parser.astnodes.ArrayAccess;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.Variable;
import org.openide.filesystems.FileObject;
import org.openide.util.Parameters;


/**
 * @author Radek Matous
 */
public class VariableElementImpl extends PhpElementImpl implements VariableElement {
    public static final String DOLLAR_PREFIX = "$"; //NOI18N
    public static final String REFERENCE_PREFIX = "&"; //NOI18N
    public static final String VARIADIC_PREFIX = "..."; //NOI18N
    public static final String IDX_FIELD = PHPIndexer.FIELD_VAR;

    private final Set<TypeResolver> instanceTypes;
    private Set<TypeResolver> instanceFQTypes;
    protected VariableElementImpl(
            final String variableName,
            final int offset,
            final String fileUrl,
            final ElementQuery elementQuery,
            final Set<TypeResolver> instanceTypes,
            final Set<TypeResolver> instanceFQTypes,
            final boolean isDeprecated) {
        super(VariableElementImpl.getName(variableName, true), null, fileUrl, offset, elementQuery, isDeprecated);
        this.instanceTypes = instanceTypes;
        this.instanceFQTypes = instanceFQTypes;
    }

    public static VariableElementImpl create(
            final String variableName,
            final int offset,
            final String fileUrl,
            final ElementQuery elementQuery,
            final Set<TypeResolver> instanceTypes,
            final boolean isDeprecated) {
        return new VariableElementImpl(variableName, offset, fileUrl, elementQuery, instanceTypes, instanceTypes, isDeprecated);
    }

    public static VariableElementImpl create(
            final String variableName,
            final int offset,
            final FileObject fo,
            final ElementQuery elementQuery,
            final Set<TypeResolver> instanceTypes,
            final boolean isDeprecated) {
        return new VariableElementImpl(variableName, offset, null, elementQuery, instanceTypes, instanceTypes, isDeprecated) {
            @Override
            public synchronized FileObject getFileObject() {
                return fo;
            }
        };
    }

    public static Set<VariableElement> fromSignature(final IndexQueryImpl indexQuery, final IndexResult indexResult) {
        return fromSignature(NameKind.empty(), indexQuery, indexResult);
    }
    public static Set<VariableElement> fromSignature(final NameKind query,
            final IndexQueryImpl indexQuery, final IndexResult indexResult) {
        final String[] values = indexResult.getValues(IDX_FIELD);
        final Set<VariableElement> retval = values.length > 0
                ? new HashSet<VariableElement>() : Collections.<VariableElement>emptySet();
        for (final String val : values) {
            final VariableElement var = fromSignature(query, indexQuery, Signature.get(val));
            if (var != null) {
                retval.add(var);
            }
        }
        return retval;
    }

    public static VariableElement fromSignature(final NameKind query,
            final IndexQueryImpl indexScopeQuery, final Signature sig) {
        final VariableSignatureParser signParser = new VariableSignatureParser(sig);
        VariableElement retval = null;
        if (matchesQuery(query, signParser)) {
            retval = new VariableElementImpl(signParser.getVariableName(),
                    signParser.getOffset(), signParser.getFileUrl(),
                    indexScopeQuery, signParser.getTypes(), signParser.getFQTypes(),
                    signParser.isDeprecated());
        }
        return retval;
    }

    public static VariableElement fromNode(final Variable node, Set<TypeResolver> typeResolvers, final FileElementQuery fileQuery) {
        Parameters.notNull("node", node);
        Parameters.notNull("fileQuery", fileQuery);
        ASTNodeInfo<Variable> info = ASTNodeInfo.create(node);
        return new VariableElementImpl(info.getName(), info.getRange().getStart(),
                fileQuery.getURL().toExternalForm(), fileQuery, typeResolvers, typeResolvers,
                VariousUtils.isDeprecatedFromPHPDoc(fileQuery.getResult().getProgram(), node));
    }

    public static VariableElement fromFrameworks(final PhpVariable variable, final ElementQuery elementQuery) {
        Parameters.notNull("variable", variable);
        PhpClass variableType = variable.getType();
        Set<TypeResolver> typeResolvers = variableType == null
                    ? Collections.<TypeResolver>emptySet()
                    : Collections.<TypeResolver>singleton(new TypeResolverImpl(variableType.getFullyQualifiedName()));
        VariableElementImpl retval = new VariableElementImpl(variable.getName(), variable.getOffset(), null, elementQuery,
                typeResolvers, typeResolvers, false);
        retval.setFileObject(variable.getFile());
        return retval;
    }

    private static boolean matchesQuery(final NameKind query, VariableSignatureParser signParser) {
        Parameters.notNull("query", query); //NOI18N
        return (query instanceof NameKind.Empty)
                || query.matchesName(VariableElement.KIND, signParser.getVariableName());
    }


    @Override
    public final String getName(final boolean dollared) {
        final String name = getName();
        final boolean startsWithDollar = name.startsWith(DOLLAR_PREFIX);
        if (startsWithDollar == dollared) {
            return name;
        }
        return dollared ? String.format("%s%s", DOLLAR_PREFIX, name) : name.substring(1); //NOI18N
    }

    @Override
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        final String varName = getName();
        sb.append(varName.toLowerCase()).append(Separator.SEMICOLON); //NOI18N
        sb.append(varName).append(Separator.SEMICOLON); //NOI18N
        sb.append(Separator.SEMICOLON); //NOI18N
        sb.append(getOffset()).append(Separator.SEMICOLON); //NOI18N
        sb.append(isDeprecated() ? 1 : 0).append(Separator.SEMICOLON);
        sb.append(getFilenameUrl()).append(Separator.SEMICOLON);
        checkSignature(sb);
        return sb.toString();
    }

    @Override
    public final PhpElementKind getPhpElementKind() {
        return VariableElement.KIND;
    }

    @Override
    public final Set<TypeResolver> getInstanceTypes() {
        return instanceTypes;
    }

    @Override
    public final Set<TypeResolver> getInstanceFQTypes() {
        return instanceFQTypes;
    }

    private void checkSignature(StringBuilder sb) {
        boolean checkEnabled = false;
        assert checkEnabled = true;
        if (checkEnabled) {
            String retval = sb.toString();
            VariableSignatureParser parser = new VariableSignatureParser(Signature.get(retval));
            assert getName().equals(parser.getVariableName());
            assert getOffset() == parser.getOffset();
            assert getInstanceTypes().size() == parser.getTypes().size();
            assert getInstanceFQTypes().size() == parser.getFQTypes().size();
        }
    }

    private static String getName(final String name, final boolean dollared) {
        final boolean startsWithDollar = name.startsWith(VariableElementImpl.DOLLAR_PREFIX);
        if (startsWithDollar == dollared) {
            return name;
        }
        return dollared ? String.format("%s%s", VariableElementImpl.DOLLAR_PREFIX, name) : name.substring(1); //NOI18N
    }

    private static class VariableSignatureParser {

        private final Signature signature;

        VariableSignatureParser(Signature signature) {
            this.signature = signature;
        }

        String getVariableName() {
            return signature.string(1);
        }

        int getOffset() {
            return signature.integer(3);
        }

        Set<TypeResolver> getTypes() {
            return TypeResolverImpl.parseTypes(signature.string(2));
        }

        Set<TypeResolver> getFQTypes() {
            return TypeResolverImpl.parseTypes(signature.string(4));
        }

        boolean isDeprecated() {
            return signature.integer(4) == 1;
        }

        String getFileUrl() {
            return signature.string(5);
        }

    }

    private static String toName(final Variable node) {
        return CodeUtils.extractVariableName(node);
    }

    private static OffsetRange toOffsetRange(final Variable node) {
        Expression name = node.getName();
        //TODO: dangerous never ending loop
        while ((name instanceof Variable)) {
            while (name instanceof ArrayAccess) {
                ArrayAccess access = (ArrayAccess) name;
                name = access.getName();
            }
            if (name instanceof Variable) {
                Variable var = (Variable) name;
                name = var.getName();
            }
        }
        return new OffsetRange(name.getStartOffset(), name.getEndOffset());
    }

}
