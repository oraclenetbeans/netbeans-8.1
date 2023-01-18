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

package org.netbeans.modules.profiler.heapwalk;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.modules.profiler.api.ProfilerStorage;
import org.netbeans.modules.profiler.oql.repository.api.OQLQueryCategory;
import org.netbeans.modules.profiler.oql.repository.api.OQLQueryDefinition;
import org.netbeans.modules.profiler.oql.repository.api.OQLQueryRepository;
import org.openide.filesystems.FileLock;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "OQLSupport_CategoryCaption=Category",
    "OQLSupport_CustomCategoryName=Custom",
    "OQLSupport_CustomCategoryDescr=Custom OQL queries created by the Save button in Query Editor.",
    "OQLSupport_QueryCaption=Query",
    "OQLSupport_NoCustomQueryName=<No Custom Queries Defined>",
    "OQLSupport_NoCustomQueryDescr=Use Save button in Query Editor to create custom OQL query."
})
public final class OQLSupport {
    private static final String SAVED_OQL_QUERIES_FILENAME = "oqlqueries"; // NOI18N
    private static final String SNAPSHOT_VERSION = "oqlqueries_version_1"; // NOI18N
    private static final String PROP_QUERY_NAME_KEY = "query-name"; // NOI18N
    private static final String PROP_QUERY_DESCR_KEY = "query-descr"; // NOI18N
    private static final String PROP_QUERY_SCRIPT_KEY = "query-script"; // NOI18N


    public static OQLTreeModel createModel() {
        return new OQLTreeModel();
    }

    public static void loadModel(OQLTreeModel model) {
        // Root
        RootNode root = (RootNode)model.getRoot();

        // Custom category
        try {
            Properties p = new Properties();
            ProfilerStorage.loadGlobalProperties(p, SAVED_OQL_QUERIES_FILENAME);
            if (!p.isEmpty()) propertiesToModel(p, model);
        } catch (Exception e) {
            ProfilerLogger.log(e);
        }

        // Defined categories
        List<? extends OQLQueryCategory> categories =
                OQLQueryRepository.getInstance().listCategories();
        for (OQLQueryCategory category : categories) {
            OQLCategoryNode cnode = new OQLCategoryNode(category);
            root.add(cnode);

        // Queries
            List<? extends OQLQueryDefinition> queries = category.listQueries();
            for (OQLQueryDefinition query : queries)
                cnode.add(new OQLQueryNode(new Query(query)));
        }

        model.reload();
    }

    public static void saveModel(OQLTreeModel model) {
        FileLock lock = null;
        try {
            Properties p = modelToProperties(model);
            if (!p.isEmpty()) ProfilerStorage.saveGlobalProperties(p, SAVED_OQL_QUERIES_FILENAME);
        } catch (Exception e) {
            ProfilerLogger.log(e);
        } finally {
            if (lock != null) lock.releaseLock();
        }
    }


    private static void propertiesToModel(Properties properties, OQLTreeModel model) {
        OQLCategoryNode custom = model.customCategory;
        
        int i = -1;
        while (properties.containsKey(PROP_QUERY_NAME_KEY + "-" + ++i)) { // NOI18N
            String name =
                properties.getProperty(PROP_QUERY_NAME_KEY + "-" + i).trim(); // NOI18N
            String description =
                properties.getProperty(PROP_QUERY_DESCR_KEY + "-" + i, "").trim(); // NOI18N
            String script =
                properties.getProperty(PROP_QUERY_SCRIPT_KEY + "-" + i, "").trim(); // NOI18N
            if (name != null && script != null)
                custom.add(new OQLQueryNode(new OQLSupport.Query(script, name, description)));
        }
    }

    private static Properties modelToProperties(OQLTreeModel model) {
        Properties properties = new Properties();

        if (model.hasCustomQueries()) {
            int i = -1;
            Enumeration<OQLQueryNode> queries = model.customCategory.children();
            while (queries.hasMoreElements()) {
                OQLSupport.Query q = queries.nextElement().getUserObject();
                properties.put(PROP_QUERY_NAME_KEY + "-" + ++i, q.getName().trim()); // NOI18N
                properties.put(PROP_QUERY_SCRIPT_KEY + "-" + i, q.getScript().trim()); // NOI18N
                if (q.getDescription() != null)
                    properties.put(PROP_QUERY_DESCR_KEY + "-" + i, q.getDescription().trim()); // NOI18N
            }
        }

        return properties;
    }


    public static final class OQLTreeModel extends DefaultTreeModel {

        private final OQLCategoryNode customCategory;

        public OQLTreeModel() {
            super(new RootNode());
            customCategory = new CustomCategoryNode();
            root().add(customCategory);
        }

        public boolean hasCustomQueries() {
            return customCategory.getChildCount() > 1 ||
                   !(customCategory.getChildAt(0) instanceof NoCustomQueriesNode);
        }

        public boolean hasDefinedCategories() {
            return root.getChildCount() > 1 ||
                   !(root.getChildAt(0) instanceof CustomCategoryNode);
        }

        public OQLCategoryNode customCategory() {
            return customCategory;
        }

        private OQLNode root() {
            return (OQLNode)root;
        }
        
    }


    public static abstract class OQLNode<T extends Object> extends DefaultMutableTreeNode {

        public OQLNode(T userObject) { super(userObject); }

        public final T getUserObject() { return (T)super.getUserObject(); }

        public String getDescription() { return null; }

        public String getCaption() { return ""; } // NOI18N

        public boolean supportsProperties() { return true; }

        public boolean isReadOnly() { return true; }

        public boolean supportsDelete() { return false; }

        public boolean supportsOpen() { return false; }

    }

    private static abstract class SpecialNode extends OQLNode<Object> {

        public SpecialNode() { super(null); }

        public boolean supportsProperties() { return false; }

    }

    public static class OQLCategoryNode extends OQLNode<OQLQueryCategory> {

        private final NoCustomQueriesNode noQueries = new NoCustomQueriesNode();

        public OQLCategoryNode(OQLQueryCategory category) {
            super(category);
            super.insert(noQueries, 0);
        }

        public String toString() { return getUserObject().getName(); }

        public String getCaption() { return Bundle.OQLSupport_CategoryCaption(); }

        public String getDescription() { return getUserObject().getDescription(); }

        public boolean supportsProperties() { return false; }

        public boolean isLeaf() { return false; }

        public void insert(MutableTreeNode newChild, int childIndex) {
            super.insert(newChild, childIndex);
            if (isNodeChild(noQueries)) super.remove(0); // Doesn't update model!
        }

        public void remove(int childIndex) {
            super.remove(childIndex); // Doesn't update model!
            if (getChildCount() == 0) super.insert(noQueries, 0);
        }

    }

    private static class CustomCategoryNode extends OQLCategoryNode {

        private CustomCategoryNode() { super(null); }

        public String toString() { return Bundle.OQLSupport_CustomCategoryName(); }

        public String getDescription() { return Bundle.OQLSupport_CustomCategoryDescr(); }

        public boolean isLeaf() { return false; }

    }

    public static class OQLQueryNode extends OQLNode<Query> {

        public OQLQueryNode(Query query) { super(query); }

        public boolean supportsOpen() { return true; }

        public boolean supportsDelete() { return isCustomQuery(); }

        public boolean isReadOnly() { return !isCustomQuery(); }

        public String toString() { return getUserObject().getName(); }

        public String getCaption() { return Bundle.OQLSupport_QueryCaption(); }

        public String getDescription() { return getUserObject().getDescription(); }

        public boolean isLeaf() { return true; }

        private boolean isCustomQuery() { return getParent() instanceof CustomCategoryNode; }

    }

    private static class RootNode extends SpecialNode {

        public String toString() { return ""; } // NOI18N

        public boolean isLeaf() { return false; }

    }

    private static class NoCustomQueriesNode extends SpecialNode {

        public String toString() { return Bundle.OQLSupport_NoCustomQueryName(); }

        public String getDescription() { return Bundle.OQLSupport_NoCustomQueryDescr(); }

        public boolean isLeaf() { return true; }

    }

    public static final class Query {

        private String script;
        private String name;
        private String description;

        
        public Query(OQLQueryDefinition qdef) {
            this(qdef.getContent(), qdef.getName(), qdef.getDescription());
        }

        public Query(String script, String name, String description) {
            setScript(script);
            setName(name);
            setDescription(description);
        }


        public void setScript(String script) {
            if (script == null)
                throw new IllegalArgumentException("Script cannot be null"); // NOI18N
            this.script = script;
        }

        public String getScript() {
            return script;
        }

        public void setName(String name) {
            this.name = normalizeString(name);
            if (this.name == null)
                throw new IllegalArgumentException("Name cannot be null"); // NOI18N
        }

        public String getName() {
            return name;
        }

        public void setDescription(String description) {
            this.description = normalizeString(description);
        }

        public String getDescription() {
            return description;
        }

        public String toString() {
            return name;
        }

        private static String normalizeString(String string) {
            String normalizedString = null;
            if (string != null) {
                normalizedString = string.trim();
                if (normalizedString.length() == 0) normalizedString = null;
            }
            return normalizedString;
        }

    }

}
