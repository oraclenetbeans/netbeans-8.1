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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.php.api.editor.PhpClass;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.NameKind;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.PhpModifiers;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.api.elements.ClassElement;
import org.netbeans.modules.php.editor.api.elements.NamespaceElement;
import org.netbeans.modules.php.editor.index.PHPIndexer;
import org.netbeans.modules.php.editor.index.Signature;
import org.netbeans.modules.php.editor.model.impl.VariousUtils;
import org.netbeans.modules.php.editor.model.nodes.ClassDeclarationInfo;
import org.netbeans.modules.php.editor.parser.astnodes.ClassDeclaration;
import org.openide.util.Parameters;

/**
 * @author Radek Matous
 */
public final class ClassElementImpl extends TypeElementImpl implements ClassElement {
    public static final String IDX_FIELD = PHPIndexer.FIELD_CLASS;

    private final QualifiedName superClass;
    private Collection<QualifiedName> possibleFQSuperClassNames;
    private Collection<QualifiedName> usedTraits;

    private ClassElementImpl(
            final QualifiedName qualifiedName,
            final int offset,
            final QualifiedName superClsName,
            final Collection<QualifiedName> possibleFQSuperClassNames,
            final Set<QualifiedName> ifaceNames,
            final Collection<QualifiedName> fqSuperInterfaces,
            final int flags,
            final Collection<QualifiedName> usedTraits,
            final String fileUrl,
            final ElementQuery elementQuery,
            final boolean isDeprecated) {
        super(qualifiedName, offset, ifaceNames, fqSuperInterfaces, flags, fileUrl, elementQuery, isDeprecated);
        this.superClass = superClsName;
        this.possibleFQSuperClassNames = possibleFQSuperClassNames;
        this.usedTraits = usedTraits;
    }

    public static Set<ClassElement> fromSignature(final IndexQueryImpl indexScopeQuery, final IndexResult indexResult) {
        return fromSignature(NameKind.empty(), indexScopeQuery, indexResult);
    }

    public static Set<ClassElement> fromSignature(final NameKind query,
            final IndexQueryImpl indexScopeQuery, final IndexResult indexResult) {
        String[] values = indexResult.getValues(IDX_FIELD);
        Set<ClassElement> retval = values.length > 0 ? new HashSet<ClassElement>() : Collections.<ClassElement>emptySet();
        for (String val : values) {
            final ClassElement clz = fromSignature(query, indexScopeQuery, Signature.get(val));
            if (clz != null) {
                retval.add(clz);
            }
        }
        return retval;
    }

    private static ClassElement fromSignature(final NameKind query,
            final IndexQueryImpl indexScopeQuery, final Signature clsSignature) {
        Parameters.notNull("query", query);
        ClassSignatureParser signParser = new ClassSignatureParser(clsSignature);
        ClassElement retval = null;
        if (matchesQuery(query, signParser)) {
            retval = new ClassElementImpl(signParser.getQualifiedName(), signParser.getOffset(),
                    signParser.getSuperClassName(), signParser.getPossibleFQSuperClassName(),
                    signParser.getSuperInterfaces(), signParser.getFQSuperInterfaces(), signParser.getFlags(),
                    signParser.getUsedTraits(), signParser.getFileUrl(), indexScopeQuery,
                    signParser.isDeprecated());
        }
        return retval;
    }

    public static ClassElement fromNode(final NamespaceElement namespace, final ClassDeclaration node, final ElementQuery.File fileQuery) {
        Parameters.notNull("node", node);
        Parameters.notNull("fileQuery", fileQuery);
        ClassDeclarationInfo info = ClassDeclarationInfo.create(node);
        final QualifiedName fullyQualifiedName = namespace != null ? namespace.getFullyQualifiedName() : QualifiedName.createForDefaultNamespaceName();
        return new ClassElementImpl(
                fullyQualifiedName.append(info.getName()), info.getRange().getStart(),
                info.getSuperClassName(), Collections.<QualifiedName>emptySet(), info.getInterfaceNames(),
                Collections.<QualifiedName>emptySet(), info.getAccessModifiers().toFlags(), info.getUsedTraits(),
                fileQuery.getURL().toExternalForm(), fileQuery, VariousUtils.isDeprecatedFromPHPDoc(fileQuery.getResult().getProgram(), node));
    }

    public static ClassElement fromFrameworks(final PhpClass clz, final ElementQuery elementQuery) {
        Parameters.notNull("clz", clz);
        Parameters.notNull("elementQuery", elementQuery);
        String fullyQualifiedName = clz.getFullyQualifiedName();
        ClassElementImpl retval = new ClassElementImpl(QualifiedName.create(fullyQualifiedName == null ? clz.getName() : fullyQualifiedName),
                clz.getOffset(), null, Collections.<QualifiedName>emptySet(), Collections.<QualifiedName>emptySet(),
                Collections.<QualifiedName>emptySet(), PhpModifiers.NO_FLAGS, Collections.<QualifiedName>emptySet(), null, elementQuery, false);
        retval.setFileObject(clz.getFile());
        return retval;
    }

    private static boolean matchesQuery(final NameKind query, ClassSignatureParser signParser) {
        Parameters.notNull("query", query);
        return (query instanceof NameKind.Empty) || query.matchesName(ClassElement.KIND, signParser.getQualifiedName());
    }

    @Override
    public PhpElementKind getPhpElementKind() {
        return KIND;
    }

    @Override
    public QualifiedName getSuperClassName() {
        return superClass;
    }

    @Override
    public Collection<QualifiedName> getPossibleFQSuperClassNames() {
        return this.possibleFQSuperClassNames;
    }

    @Override
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName().toLowerCase()).append(Separator.SEMICOLON); //NOI18N
        sb.append(getName()).append(Separator.SEMICOLON); //NOI18N
        sb.append(getOffset()).append(Separator.SEMICOLON); //NOI18N
        QualifiedName superClassName = getSuperClassName();
        if (superClassName != null) {
            sb.append(superClassName.toString());
            sb.append("|");
            boolean first = true;
            for (QualifiedName qualifiedName : possibleFQSuperClassNames) {
                if (!first) {
                    sb.append(',');
                } else {
                    first = true;
                }
                sb.append(qualifiedName.toString());
            }
        }
        sb.append(Separator.SEMICOLON); //NOI18N
        QualifiedName namespaceName = getNamespaceName();
        sb.append(namespaceName.toString()).append(Separator.SEMICOLON); //NOI18N
        StringBuilder ifaceSb = new StringBuilder();
        for (QualifiedName ifaceName : getSuperInterfaces()) {
            if (ifaceSb.length() > 0) {
                ifaceSb.append(Separator.COMMA); //NOI18N
            }
            ifaceSb.append(ifaceName.toString()); //NOI18N
        }
        sb.append(ifaceSb);
        sb.append(Separator.SEMICOLON); //NOI18N
        sb.append(getPhpModifiers().toFlags()).append(Separator.SEMICOLON);
        if (!usedTraits.isEmpty()) {
            StringBuilder traitSb = new StringBuilder();
            for (QualifiedName usedTrait : usedTraits) {
                if (traitSb.length() > 0) {
                    traitSb.append(","); //NOI18N
                }
                traitSb.append(usedTrait.toString());
            }
            sb.append(traitSb);
        }
        sb.append(";"); //NOI18N
        sb.append(isDeprecated() ? 1 : 0).append(";"); //NOI18N
        sb.append(getFilenameUrl()).append(Separator.SEMICOLON);
        checkClassSignature(sb);
        return sb.toString();
    }

    @Override
    public String asString(PrintAs as) {
        StringBuilder retval = new StringBuilder();
        switch (as) {
            case NameAndSuperTypes:
                retval.append(getName());
                printAsSuperTypes(retval);
                break;
            case SuperTypes:
                printAsSuperTypes(retval);
                break;
            default:
                assert false : as;
        }
        return retval.toString();
    }

    private void printAsSuperTypes(StringBuilder sb) {
        QualifiedName superClassName = getSuperClassName();
        if (superClassName != null) {
            sb.append(" extends  "); //NOI18N
            sb.append(superClassName.getName());
        }
        Set<QualifiedName> superIfaces = getSuperInterfaces();
        if (!superIfaces.isEmpty()) {
            sb.append(" implements "); //NOI18N
        }
        StringBuilder ifacesBuffer = new StringBuilder();
        for (QualifiedName qualifiedName : superIfaces) {
            if (ifacesBuffer.length() > 0) {
                ifacesBuffer.append(", "); //NOI18N
            }
            ifacesBuffer.append(qualifiedName.getName());
        }
        sb.append(ifacesBuffer);
    }

    private void checkClassSignature(StringBuilder sb) {
        boolean checkEnabled = false;
        assert checkEnabled = true;
        if (checkEnabled) {
            String retval = sb.toString();
            ClassSignatureParser parser = new ClassSignatureParser(Signature.get(retval));
            assert getName().equals(parser.getQualifiedName().toName().toString());
            assert getNamespaceName().equals(parser.getQualifiedName().toNamespaceName());
            assert getOffset() == parser.getOffset();
            assert getPhpModifiers().toFlags() == parser.getFlags();
            QualifiedName superClassName = getSuperClassName();
            if (superClassName != null) {
                assert superClassName.equals(parser.getSuperClassName());
            }
            assert getSuperInterfaces().size() == parser.getSuperInterfaces().size();
        }
    }

    @Override
    public boolean isFinal() {
        return getPhpModifiers().isFinal();
    }

    @Override
    public boolean isAbstract() {
        return getPhpModifiers().isAbstract();
    }

    @Override
    public Collection<QualifiedName> getUsedTraits() {
        return usedTraits;
    }

    private static class ClassSignatureParser {

        private final Signature signature;

        ClassSignatureParser(Signature signature) {
            this.signature = signature;
        }

        QualifiedName getQualifiedName() {
            return composeQualifiedName(signature.string(4), signature.string(1));
        }

        @CheckForNull
        QualifiedName getSuperClassName() {
            String name = signature.string(3);
            if (name != null) {
                int index = name.indexOf('|');
                if (index > 0) {
                    name = name.substring(0, index);
                }
            }
            return name.trim().length() == 0 ? null : QualifiedName.create(name);
        }

        Collection<QualifiedName> getPossibleFQSuperClassName() {
            String field = signature.string(3);
            Collection<QualifiedName> retval = Collections.emptyList();
            if (field != null) {
                int index = field.indexOf('|');
                if (index > 0) {
                    field = field.substring(index + 1);
                    retval = new ArrayList<>();
                    for (StringTokenizer st = new StringTokenizer(field, ","); st.hasMoreTokens();) {
                        String token = st.nextToken();
                        retval.add(QualifiedName.create(token));
                    }
                }
            }
            return retval;
        }

        public Set<QualifiedName> getSuperInterfaces() {
            Set<QualifiedName> ifaces = Collections.emptySet();
            String separatedIfaces = signature.string(5);
            if (separatedIfaces != null && separatedIfaces.length() > 0) {
                int index = separatedIfaces.indexOf('|');
                if (index > 0) {
                    String field = separatedIfaces.substring(0, index);
                    ifaces = new HashSet<>();
                    final String[] ifaceNames = field.split(Separator.COMMA.toString());
                    for (String ifName : ifaceNames) {
                        ifaces.add(QualifiedName.create(ifName));
                    }
                }
            }
            return ifaces;
        }

        public Collection<QualifiedName> getFQSuperInterfaces() {
            Collection<QualifiedName> retval = Collections.<QualifiedName>emptySet();
            String separatedIfaces = signature.string(5);
            if (separatedIfaces != null) {
                int index = separatedIfaces.indexOf('|');
                if (index > 0) {
                    String field = separatedIfaces.substring(index + 1);
                    retval = new ArrayList<QualifiedName>();
                    for (StringTokenizer st = new StringTokenizer(field, ","); st.hasMoreTokens();) { //NOI18N
                        String token = st.nextToken();
                        retval.add(QualifiedName.create(token));
                    }
                }
            }
            return retval;
        }

        int getOffset() {
            return signature.integer(2);
        }

        int getFlags() {
            return signature.integer(6);
        }

        public Collection<QualifiedName> getUsedTraits() {
            Collection<QualifiedName> retval = new HashSet<>();
            String traits = signature.string(7);
            final String[] traitNames = traits.split(Separator.COMMA.toString());
            for (String trait : traitNames) {
                retval.add(QualifiedName.create(trait));
            }
            return retval;
        }

        boolean isDeprecated() {
            return signature.integer(8) == 1;
        }

        String getFileUrl() {
            return signature.string(9);
        }
    }
}
