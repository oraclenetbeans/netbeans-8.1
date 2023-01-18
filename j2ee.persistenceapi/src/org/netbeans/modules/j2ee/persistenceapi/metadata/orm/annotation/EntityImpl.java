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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.j2ee.persistenceapi.metadata.orm.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.AnnotationModelHelper;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.parser.AnnotationParser;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.JavaContextListener;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.parser.ParseResult;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.PersistentObject;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.parser.ValueProvider;
import org.netbeans.modules.j2ee.persistence.api.metadata.orm.*;

public class EntityImpl extends PersistentObject implements Entity, JavaContextListener {

    private final EntityMappingsImpl root;
    // persistent
    private String name;
    private String class2;
    private String accessType;//is used to store access annotation value only, it's not final access type
    private Table table;
    private boolean valid;
    // transient: set to null in javaContextLeft()
    private IdClassImpl idClass;
    private AttributesImpl attributes;
    ArrayList<NamedQuery> nqs;

    public EntityImpl(AnnotationModelHelper helper, EntityMappingsImpl root, TypeElement typeElement) {
        super(helper, typeElement);
        this.root = root;
        helper.addJavaContextListener(this);
        valid = refresh(typeElement);
        assert valid;
    }

    final boolean refresh(TypeElement typeElement) {
        class2 = typeElement.getQualifiedName().toString();
        AnnotationModelHelper helper = getHelper();
        Map<String, ? extends AnnotationMirror> annByType = helper.getAnnotationsByType(typeElement.getAnnotationMirrors());
        AnnotationMirror entityAnn = annByType.get("javax.persistence.Entity"); // NOI18N
        if (entityAnn == null) {
            return false;
        }
        AnnotationParser parser = AnnotationParser.create(helper);
        parser.expectString("name", AnnotationParser.defaultValue(typeElement.getSimpleName().toString())); // NOI18N
        ParseResult parseResult = parser.parse(entityAnn); // NOI18N
        name = parseResult.get("name", String.class); // NOI18N
        AnnotationMirror entityAcc = annByType.get("javax.persistence.Access"); // NOI18N
        if (entityAcc != null) {
            parser = AnnotationParser.create(helper);
            parser.expect("value", new ValueProvider() {
                @Override
                public Object getValue(AnnotationValue elementValue) {
                    return elementValue.toString();
                }

                @Override
                public Object getDefaultValue() {
                    return null;
                }
            });//NOI18N
            parseResult = parser.parse(entityAcc);
            accessType = parseResult.get("value", String.class);
        }
        // also reading the table element to avoid initializing the whole model
        // when a client looking the entity mapped to a specific table iterates
        // over all entities calling getTable().
        // XXX locale?
        AnnotationMirror tableAnn = annByType.get("javax.persistence.Table");//NOI18N
        if(tableAnn == null) {
            TypeMirror superclass = typeElement.getSuperclass();
            while(superclass!=null && superclass.getKind() == TypeKind.DECLARED && tableAnn == null) {
                Element el = ((DeclaredType)superclass).asElement();
                Map<String, ? extends AnnotationMirror> annotationsByType = helper.getAnnotationsByType( el.getAnnotationMirrors());
                if(annotationsByType.containsKey("javax.persistence.Entity") || annotationsByType.containsKey("javax.persistence.MappedSuperclass")) {//NOI18N
                    tableAnn = annotationsByType.get("javax.persistence.Table");//NOI18N
                } else {
                    break;
                }
               if(el instanceof TypeElement) {
                   superclass = ((TypeElement) el).getSuperclass();
               } else {
                   break;
               }
            }
        }
        table = new TableImpl(helper, tableAnn, name.toUpperCase()); // NOI18N
        //fill named queries
        AnnotationMirror nqsAnn = annByType.get("javax.persistence.NamedQueries");// NOI18N
        ArrayList<AnnotationMirror> nqAnn = null;
        if (nqsAnn == null) {
            nqsAnn = annByType.get("javax.persistence.NamedQuery");// NOI18N
            if (nqsAnn != null) {
                nqAnn = new ArrayList<AnnotationMirror>();
                nqAnn.add(nqsAnn);
            }
        } else {
            Map<? extends ExecutableElement, ? extends AnnotationValue> maps = nqsAnn.getElementValues();
            nqAnn = new ArrayList<AnnotationMirror>();
            for (AnnotationValue vl : maps.values()) {
                List lst = (List) vl.getValue();
                for (Object val : lst) {
                    if (val instanceof AnnotationMirror) {
                        AnnotationMirror am = (AnnotationMirror) val;
                        if ("javax.persistence.NamedQuery".equals(am.getAnnotationType().toString())) {//NOI18N
                            nqAnn.add(am);
                        }
                    }
                }
            }
        }
        nqs = null;//we reset all queries
        if (nqAnn != null && nqAnn.size() > 0) {
            parser = AnnotationParser.create(helper);
            parser.expectString("name", AnnotationParser.defaultValue("")); // NOI18N
            parser.expectString("query", AnnotationParser.defaultValue("")); // NOI18N
            for (AnnotationMirror am : nqAnn) {
                parseResult = parser.parse(am); // NOI18N
                String nm = parseResult.get("name", String.class); // NOI18N            
                String qr = parseResult.get("query", String.class); // NOI18N
                this.addNamedQuery(new NamedQueryImpl(nm, qr));
            }
        }
        //
        return true;
    }

    EntityMappingsImpl getRoot() {
        return root;
    }

    @Override
    public void javaContextLeft() {
        attributes = null;
        idClass = null;
        valid = false;//use soft refresh if possible
    }

    public void setName(String value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public String getName() {
        return name;
    }

    public void setClass2(String value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public String getClass2() {
        return class2;
    }

    public void setAccess(String value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public String getAccess() {
        if (accessType != null && accessType.length()>0) {
            //use access type specified by annotation by default, regardless of later fields/properties annitatons
            return accessType.equals("javax.persistence.AccessType.PROPERTY") ? PROPERTY_ACCESS : FIELD_ACCESS;
        } else {
            return getAttributes().hasFieldAccess() ? FIELD_ACCESS : PROPERTY_ACCESS;
        }
    }

    public void setMetadataComplete(boolean value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public boolean isMetadataComplete() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setDescription(String value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public String getDescription() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setTable(Table value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public Table getTable() {
        if (!valid) {
            TypeElement te = null;
            try {
                te = getTypeElement();
            } catch (IllegalStateException ex) {
                //refresh only if possible
                //table use videly used out of UserActionTask as cached value
            }
            if (te != null) {
                valid = refresh(te);
            }
        }
        return table;
    }

    public Table newTable() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setSecondaryTable(int index, SecondaryTable value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SecondaryTable getSecondaryTable(int index) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int sizeSecondaryTable() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setSecondaryTable(SecondaryTable[] value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SecondaryTable[] getSecondaryTable() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int addSecondaryTable(SecondaryTable value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int removeSecondaryTable(SecondaryTable value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SecondaryTable newSecondaryTable() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPrimaryKeyJoinColumn(int index, PrimaryKeyJoinColumn value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PrimaryKeyJoinColumn getPrimaryKeyJoinColumn(int index) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int sizePrimaryKeyJoinColumn() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPrimaryKeyJoinColumn(PrimaryKeyJoinColumn[] value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PrimaryKeyJoinColumn[] getPrimaryKeyJoinColumn() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int addPrimaryKeyJoinColumn(PrimaryKeyJoinColumn value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int removePrimaryKeyJoinColumn(PrimaryKeyJoinColumn value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PrimaryKeyJoinColumn newPrimaryKeyJoinColumn() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setIdClass(IdClass value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public IdClass getIdClass() {
        if (idClass == null) {
            TypeElement typeElement = getTypeElement();
            if (typeElement != null) {
                idClass = EntityMappingsUtilities.getIdClass(getRoot().getHelper(), typeElement);
            }
        }
        return idClass;
    }

    public IdClass newIdClass() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setInheritance(Inheritance value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public Inheritance getInheritance() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public Inheritance newInheritance() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setDiscriminatorValue(String value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public String getDiscriminatorValue() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setDiscriminatorColumn(DiscriminatorColumn value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public DiscriminatorColumn getDiscriminatorColumn() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public DiscriminatorColumn newDiscriminatorColumn() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setSequenceGenerator(SequenceGenerator value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SequenceGenerator getSequenceGenerator() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SequenceGenerator newSequenceGenerator() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setTableGenerator(TableGenerator value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public TableGenerator getTableGenerator() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public TableGenerator newTableGenerator() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    @Override
    public void setNamedQuery(int index, NamedQuery value) {
        if (nqs == null) {
            nqs = new ArrayList<NamedQuery>();
        }
        nqs.set(index, value); // NOI18N
    }

    @Override
    public NamedQuery getNamedQuery(int index) {
        return nqs != null && nqs.size() > index ? nqs.get(index) : null; // NOI18N
    }

    @Override
    public int sizeNamedQuery() {
        return nqs != null ? nqs.size() : 0; // NOI18N
    }

    @Override
    public void setNamedQuery(NamedQuery[] value) {
        nqs = new ArrayList<NamedQuery>(Arrays.asList(value));
    }

    @Override
    public NamedQuery[] getNamedQuery() {
        return nqs != null ? nqs.toArray(new NamedQuery[]{}) : new NamedQuery[]{}; // NOI18N
    }

    @Override
    public int addNamedQuery(NamedQuery value) {
        if (nqs == null) {
            nqs = new ArrayList<NamedQuery>();
        }
        nqs.add(value);
        return nqs.size() - 1;
    }

    @Override
    public int removeNamedQuery(NamedQuery value) {
        nqs.remove(value); // NOI18N
        return nqs.size();
    }

    @Override
    public NamedQuery newNamedQuery() {
        return new NamedQueryImpl(null, null); // NOI18N
    }

    @Override
    public void setNamedNativeQuery(int index, NamedNativeQuery value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public NamedNativeQuery getNamedNativeQuery(int index) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int sizeNamedNativeQuery() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setNamedNativeQuery(NamedNativeQuery[] value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public NamedNativeQuery[] getNamedNativeQuery() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int addNamedNativeQuery(NamedNativeQuery value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int removeNamedNativeQuery(NamedNativeQuery value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public NamedNativeQuery newNamedNativeQuery() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setSqlResultSetMapping(int index, SqlResultSetMapping value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SqlResultSetMapping getSqlResultSetMapping(int index) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int sizeSqlResultSetMapping() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setSqlResultSetMapping(SqlResultSetMapping[] value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SqlResultSetMapping[] getSqlResultSetMapping() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int addSqlResultSetMapping(SqlResultSetMapping value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int removeSqlResultSetMapping(SqlResultSetMapping value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public SqlResultSetMapping newSqlResultSetMapping() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setExcludeDefaultListeners(EmptyType value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public EmptyType getExcludeDefaultListeners() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public EmptyType newEmptyType() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setExcludeSuperclassListeners(EmptyType value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public EmptyType getExcludeSuperclassListeners() {
        throw new UnsupportedOperationException("This operation is not implemented yet"); // NOI18N
    }

    public void setEntityListeners(EntityListeners value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public EntityListeners getEntityListeners() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public EntityListeners newEntityListeners() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPrePersist(PrePersist value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PrePersist getPrePersist() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PrePersist newPrePersist() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPostPersist(PostPersist value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostPersist getPostPersist() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostPersist newPostPersist() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPreRemove(PreRemove value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PreRemove getPreRemove() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PreRemove newPreRemove() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPostRemove(PostRemove value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostRemove getPostRemove() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostRemove newPostRemove() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPreUpdate(PreUpdate value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PreUpdate getPreUpdate() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PreUpdate newPreUpdate() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPostUpdate(PostUpdate value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostUpdate getPostUpdate() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostUpdate newPostUpdate() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setPostLoad(PostLoad value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostLoad getPostLoad() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public PostLoad newPostLoad() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setAttributeOverride(int index, AttributeOverride value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AttributeOverride getAttributeOverride(int index) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int sizeAttributeOverride() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setAttributeOverride(AttributeOverride[] value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AttributeOverride[] getAttributeOverride() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int addAttributeOverride(AttributeOverride value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int removeAttributeOverride(AttributeOverride value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AttributeOverride newAttributeOverride() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setAssociationOverride(int index, AssociationOverride value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AssociationOverride getAssociationOverride(int index) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int sizeAssociationOverride() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setAssociationOverride(AssociationOverride[] value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AssociationOverride[] getAssociationOverride() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int addAssociationOverride(AssociationOverride value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public int removeAssociationOverride(AssociationOverride value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AssociationOverride newAssociationOverride() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public void setAttributes(Attributes value) {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public AttributesImpl getAttributes() {
        if (attributes == null) {
            attributes = new AttributesImpl(this);
        }
        return attributes;
    }

    public Attributes newAttributes() {
        throw new UnsupportedOperationException("This operation is not implemented yet."); // NOI18N
    }

    public String toString() {
        return "EntityImpl[name='" + name + "',class2='" + class2 + "']"; // NOI18N
    }
}
