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

package org.netbeans.modules.j2ee.persistence.dd;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.modules.j2ee.metadata.model.api.MetadataModel;
import org.netbeans.modules.j2ee.metadata.model.api.MetadataModelAction;
import org.netbeans.modules.j2ee.metadata.model.api.MetadataModelException;
import org.netbeans.modules.j2ee.persistence.api.EntityClassScope;
import org.netbeans.modules.j2ee.persistence.api.PersistenceScope;
import org.netbeans.modules.j2ee.persistence.api.PersistenceScopes;
import org.netbeans.modules.j2ee.persistence.api.metadata.orm.Entity;
import org.netbeans.modules.j2ee.persistence.api.metadata.orm.EntityMappings;
import org.netbeans.modules.j2ee.persistence.api.metadata.orm.EntityMappingsMetadata;
import org.netbeans.modules.j2ee.persistence.dd.common.Persistence;
import org.netbeans.modules.j2ee.persistence.dd.common.PersistenceUnit;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 *
 * @author Marek Fukala, Andrei Badea
 */
public class PersistenceUtils {

    private static final Logger USG_LOGGER = Logger.getLogger("org.netbeans.ui.metrics.j2ee.persistence"); // NOI18N
    private static final Logger LOG = Logger.getLogger(PersistenceUtils.class.getName());

    public static EntityMappings getEntityMappings(FileObject documentFO) {
        Project project = FileOwnerQuery.getOwner(documentFO);
        if (project == null) {
            return null;
        }
        EntityClassScope entityClassScope = EntityClassScope.getEntityClassScope(project.getProjectDirectory());
        if(entityClassScope == null){
            return null;
        }
        MetadataModel<EntityMappingsMetadata> model = entityClassScope.getEntityMappingsModel(true);
        EntityMappings mappings = null;
        try {
            mappings = model.runReadAction(
                    new MetadataModelAction<EntityMappingsMetadata, EntityMappings>(){

                        @Override
                        public EntityMappings run(EntityMappingsMetadata metadata) throws Exception {
                            return metadata.getRoot();
                        }
            
                    }
            );
        } catch (MetadataModelException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return mappings;
    }
    
    // TODO multiple mapping files
    
    private PersistenceUtils() {
    }
    
    /**
     * Returns the persistence unit(s) the given entity class belongs to. Since
     * an entity class can belong to any persistence unit, this returns all
     * persistence units in all persistence.xml files in the project which owns
     * the given entity class.
     *
     * @return an array of PersistenceUnit's; never null.
     * @throws NullPointerException if <code>sourceFile</code> is null.
     */
    public static PersistenceUnit[] getPersistenceUnits(FileObject sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new NullPointerException("The sourceFile parameter cannot be null"); // NOI18N
        }
        
        Project project = FileOwnerQuery.getOwner(sourceFile);
        if (project == null) {
            return new PersistenceUnit[0];
        }
        
        List<PersistenceUnit> result = new ArrayList<PersistenceUnit>();
        for (PersistenceScope persistenceScope : getPersistenceScopes(project)) {
            Persistence persistence = null;
            try{
                persistence = PersistenceMetadata.getDefault().getRoot(persistenceScope.getPersistenceXml());
            } catch (RuntimeException ex) {// must catch RTE (thrown by schema2beans when document is not valid)
                LOG.log(Level.INFO, null, ex);
            }
            if(persistence != null) {
                result.addAll(Arrays.asList(persistence.getPersistenceUnit()));
            }
        }
        
        return (PersistenceUnit[])result.toArray(new PersistenceUnit[result.size()]);
    }
    
    /**
     * Searches the given entity mappings for the specified entity class.
     *
     * @param  className the Java class to search for.
     * @param  entityMappings the entity mappings to be searched.
     * @return the entity class or null if it could not be found.
     * @throws NullPointerException if <code>className</code> or
     *         <code>entityMappings</code> were null.
     */
    public static Entity getEntity(String className, EntityMappings entityMappings) {
        if (className == null) {
            throw new NullPointerException("The javaClass parameter cannot be null"); // NOI18N
        }
        if (entityMappings == null) {
            throw new NullPointerException("The entityMappings parameter cannot be null"); // NOI18N
        }
        
        for (Entity entity : entityMappings.getEntity()) {
            if (className.equals(entity.getClass2())) {
                return entity;
            }
        }
        return null;
    }
    
    /**
     * Returns an array containing all persistence scopes provided by the
     * given project. This is just an utility method which does:
     *
     * <pre>
     * PersistenceScopes.getPersistenceScopes(project).getPersistenceScopes();
     * </pre>
     *
     * <p>with all the necessary checks for null (returning an empty
     * array in this case).</p>
     *
     * @param  project the project to retrieve the persistence scopes from.
     * @return the list of persistence scopes provided by <code>project</code>;
     *         or an empty array if the project provides no persistence
     *         scopes; never null.
     * @throws NullPointerException if <code>project</code> was null.
     */
    public static PersistenceScope[] getPersistenceScopes(Project project) {
        if (project == null) {
            throw new NullPointerException("The project parameter cannot be null"); // NOI18N
        }
        
        PersistenceScopes persistenceScopes = PersistenceScopes.getPersistenceScopes(project);
        if (persistenceScopes != null) {
            return persistenceScopes.getPersistenceScopes();
        }
        return new PersistenceScope[0];
    }

    /**
     * method check target compile classpath for presence of persitence classes of certain version
     * returns max supported specification
     * @param project
     * @return
     */
    public static String getJPAVersion(Project target)
    {
        String version=null;
        Sources sources=ProjectUtils.getSources(target);
        SourceGroup groups[]=sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        SourceGroup firstGroup=groups[0];
        FileObject fo=firstGroup.getRootFolder();
        ClassPath compile=ClassPath.getClassPath(fo, ClassPath.COMPILE);
        if(compile.findResource("javax/persistence/criteria/CriteriaUpdate.class")!=null) {
            version=Persistence.VERSION_2_1;
        } else if(compile.findResource("javax/persistence/criteria/JoinType.class")!=null) {
            version=Persistence.VERSION_2_0;
        } else if(compile.findResource("javax/persistence/Entity.class")!=null) {
            version=Persistence.VERSION_1_0;
        }
        return version;
    }

    public static String getJPAVersion(Library lib) {
        List<URL> roots=lib.getContent("classpath");
        ClassPath cp = ClassPathSupport.createClassPath(roots.toArray(new URL[roots.size()]));
        String version=null;
        if(cp.findResource("javax/persistence/criteria/CriteriaUpdate.class")!=null) {
            version=Persistence.VERSION_2_1;
        } else if(cp.findResource("javax/persistence/criteria/JoinType.class")!=null) {
            version=Persistence.VERSION_2_0;
        } else if(cp.findResource("javax/persistence/Entity.class")!=null) {
            version=Persistence.VERSION_1_0;
        }
        return version;
    }

        /**
     * Logs feature usage.
     *
     * @param srcClass source class
     * @param message message key
     * @param params message parameters, may be <code>null</code>
     */
    public static void logUsage(Class srcClass, String message, Object[] params) {
        Parameters.notNull("message", message);

        LogRecord logRecord = new LogRecord(Level.INFO, message);
        logRecord.setLoggerName(USG_LOGGER.getName());
        logRecord.setResourceBundle(NbBundle.getBundle(srcClass));
        logRecord.setResourceBundleName(srcClass.getPackage().getName() + ".Bundle"); // NOI18N
        if (params != null) {
            logRecord.setParameters(params);
        }
        USG_LOGGER.log(logRecord);
    }
}
