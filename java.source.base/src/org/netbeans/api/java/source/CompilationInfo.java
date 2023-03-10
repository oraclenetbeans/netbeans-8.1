/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2011 Sun
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

package org.netbeans.api.java.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.model.JavacElements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.CheckReturnValue;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.annotations.common.NullUnknown;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.modules.java.preprocessorbridge.spi.WrapperFactory;
import org.netbeans.modules.java.source.parsing.CompilationInfoImpl;
import org.netbeans.modules.java.source.parsing.DocPositionRegion;
import org.netbeans.modules.java.source.parsing.FileObjects;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.java.source.parsing.JavacParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Pair;
import org.openide.util.Parameters;

/** Assorted information about the JavaSource.
 *
 * @author Petr Hrebejk, Tomas Zezula
 */
public class CompilationInfo {
    
    private static final boolean VERIFY_CONFINEMENT = Boolean.getBoolean(CompilationInfo.class.getName()+".vetifyConfinement"); //NOI18N
    
    //INV: never null
    final CompilationInfoImpl impl;
    //Expert: set to true when the runUserActionTask(,true), runModificationTask(,true)
    //ended or when reschedulable task leaved run method to verify confinement
    private boolean invalid;
    //@GuarderBy(this)
    private Trees trees;
    //@GuarderBy(this)
    private ElementUtilities elementUtilities;
    //@GuarderBy(this)
    private TreeUtilities treeUtilities;
    //@GuarderBy(this)
    private TypeUtilities typeUtilities;
    //@NotThreadSafe
    private JavaSource javaSource;
    
    
    CompilationInfo (final CompilationInfoImpl impl)  {
        assert impl != null;
        this.impl = impl;
    }

    /**
     * Returns an instance of the {@link CompilationInfo} for
     * given {@link Parser.Result} if it is a result
     * of a java parser.
     * @param result for which the {@link CompilationInfo} should be
     * returned.
     * @return a {@link CompilationInfo} or null when the given result
     * is not a result of java parsing.
     * @since 0.42
     */
    public static @NullUnknown CompilationInfo get (@NonNull final Parser.Result result) {
        Parameters.notNull("result", result);   //NOI18N
        CompilationInfo info = null;
        if (result instanceof JavacParserResult) {
            final JavacParserResult javacResult = (JavacParserResult)result;            
            info = javacResult.get(CompilationInfo.class);            
        }
        return info;
    }
             
    // API of the class --------------------------------------------------------
    
    /**
     * Returns the current phase of the {@link JavaSource}.
     * @return {@link JavaSource.Phase} the state which was reached by the {@link JavaSource}.
     */
    public @NonNull JavaSource.Phase getPhase() {
        checkConfinement();
        return this.impl.getPhase();
    }
    
    /**
     * Returns tree which was reparsed by an incremental reparse.
     * When the source file wasn't parsed yet or the parse was a full parse
     * this method returns null.
     * <p class="nonnormative">
     * Currently the leaf tree is a MethodTree but this may change in the future.
     * Client of this method is responsible to check the corresponding TreeKind
     * to find out if it may perform on the changed subtree or it needs to
     * reprocess the whole tree.
     * </p>
     * @return {@link TreePath} or null
     * @since 0.31
     */
    public @CheckForNull @CheckReturnValue TreePath getChangedTree () {
        checkConfinement();
        if (JavaSource.Phase.PARSED.compareTo (impl.getPhase())>0) {
            return null;
        }
        final Pair<DocPositionRegion,MethodTree> changedTree = impl.getChangedTree();
        if (changedTree == null) {
            return null;
        }
        final CompilationUnitTree cu = impl.getCompilationUnit();
        if (cu == null) {
            return null;
        }
        return TreePath.getPath(cu, changedTree.second());
    }       
    
    /**
     * Returns the javac tree representing the source file.
     * @return {@link CompilationUnitTree} the compilation unit containing the top level classes contained in the,
     * java source file. 
     * @throws java.lang.IllegalStateException  when the phase is less than {@link JavaSource.Phase#PARSED}
     */
    public CompilationUnitTree getCompilationUnit() {
        checkConfinement();
        return this.impl.getCompilationUnit();
    }
    
    /**
     * Returns the content of the file represented by the {@link JavaSource}.
     * @return String the java source
     */
    public @NonNull String getText() {
        checkConfinement();
        return this.impl.getText();
    }

    /**
     * Returns the snapshot used by java parser
     * @return the snapshot
     * @since 0.42
     */
    public @NonNull Snapshot getSnapshot () {
        checkConfinement();
        return this.impl.getSnapshot();
    }
    
    /**
     * Returns the {@link TokenHierarchy} for the file represented by the {@link JavaSource}.
     * @return lexer TokenHierarchy
     */
    public @NonNull TokenHierarchy<?> getTokenHierarchy() {
        checkConfinement();
        return this.impl.getTokenHierarchy();
    }
    
    /**
     * Returns the errors in the file represented by the {@link JavaSource}.
     * @return an list of {@link Diagnostic} 
     */
    public @NonNull List<Diagnostic> getDiagnostics() {
        checkConfinement();
        return this.impl.getDiagnostics();
    }
    
    /**
     * Returns all top level elements defined in file for which the {@link CompilationInfo}
     * was created. The {@link CompilationInfo} has to be in phase {@link JavaSource#Phase#ELEMENTS_RESOLVED}.
     * @return list of top level elements, it may return null when this {@link CompilationInfo} is not
     * in phase {@link JavaSource#Phase#ELEMENTS_RESOLVED} or higher.
     * @throws IllegalStateException is thrown when the {@link JavaSource} was created with no files
     * @since 0.14
     */
    public @NullUnknown List<? extends TypeElement> getTopLevelElements () throws IllegalStateException {
        checkConfinement();
        if (this.impl.getFileObject() == null) {
            throw new IllegalStateException ();
        }
        final List<TypeElement> result = new ArrayList<TypeElement>();
        if (this.impl.isClassFile()) {
            Elements elements = getElements();
            assert elements != null;
            assert this.impl.getRoot() != null;
            String name = FileObjects.convertFolder2Package(FileObjects.stripExtension(FileUtil.getRelativePath(this.impl.getRoot(), this.impl.getFileObject())));
            TypeElement e = ((JavacElements)elements).getTypeElementByBinaryName(name);
            if (e != null) {                
                result.add (e);
            }
        }
        else {
            CompilationUnitTree cu = getCompilationUnit();
            if (cu == null) {
                return null;
            }
            else {
                final Trees ts = getTrees();
                assert ts != null;
                List<? extends Tree> typeDecls = cu.getTypeDecls();
                TreePath cuPath = new TreePath(cu);
                for( Tree t : typeDecls ) {
                    TreePath p = new TreePath(cuPath,t);
                    Element e = ts.getElement(p);
                    if ( e != null && ( e.getKind().isClass() || e.getKind().isInterface() ) ) {
                        result.add((TypeElement)e);
                    }
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
        
    
    /**
     * Return the {@link Trees} service of the javac represented by this {@link CompilationInfo}.
     * @return javac Trees service
     */
    public synchronized @NonNull Trees getTrees() {
        checkConfinement();
        if (trees == null) {
            trees = JavacTrees.instance(impl.getJavacTask().getContext());
            Snapshot snapshot = impl.getSnapshot();
            Document doc = snapshot != null ? snapshot.getSource().getDocument(false) : null;
            WrapperFactory factory = doc != null ? (WrapperFactory)doc.getProperty(WrapperFactory.class) : null;
            if (factory != null) {
                trees = factory.wrapTrees(trees);
            }
        }
        return trees;
    }
    
    /**
     * Return the {@link DocTrees} service of the javac represented by this {@link CompilationInfo}.
     * @return javac DocTrees service
     * @since 0.124
     */
    public @NonNull DocTrees getDocTrees() {
        return (DocTrees) getTrees();
    }
    
    /**
     * Return the {@link Types} service of the javac represented by this {@link CompilationInfo}.
     * @return javac Types service
     */
    public @NonNull Types getTypes() {
        checkConfinement();
        return impl.getJavacTask().getTypes();
    }
    
    /**
     * Return the {@link Elements} service of the javac represented by this {@link CompilationInfo}.
     * @return javac Elements service
     */
    public @NonNull Elements getElements() {
        checkConfinement();
	return impl.getJavacTask().getElements();
    }
        
    /**
     * Returns {@link JavaSource} for which this {@link CompilationInfo} was created.
     * @return JavaSource or null
     * @deprecated Works only when the CompilationInfo was created by JavaSource using
     * the compatibility bridge, when the CompilationInfo was created by the parsing api
     * it returns null. Use {@link CompilationInfo#getSnapshot()} instead.
     */
    public @NullUnknown JavaSource getJavaSource() {
        checkConfinement();
        return javaSource;
    }

    void setJavaSource (final JavaSource javaSource) {
        this.javaSource = javaSource;
    }
    
    /**
     * Returns {@link ClasspathInfo} for which this {@link CompilationInfo} was created.
     * @return ClasspathInfo
     */
    public @NonNull ClasspathInfo getClasspathInfo() {
        checkConfinement();
	return this.impl.getClasspathInfo();
    }
    
    /**
     * Returns the {@link FileObject} represented by this {@link CompilationInfo}.
     * @return FileObject
     */
    public @NullUnknown FileObject getFileObject() {
        checkConfinement();
        return impl.getFileObject();
    }
    
    /**Return {@link PositionConverter} binding virtual Java source and the real source.
     * Please note that this method is needed only for clients that need to work
     * in non-Java files (e.g. JSP files) or in dialogs, like code completion.
     * Most clients do not need to use {@link PositionConverter}.
     * 
     * @return PositionConverter binding the virtual Java source and the real source.
     * @since 0.21
     * @deprecated as of 0.42, this is superseded by Parsing API.
     */
    @Deprecated
    public PositionConverter getPositionConverter() {
        checkConfinement();
        if (this.impl.getFileObject() == null) {
            throw new IllegalStateException ();
        }
        return new PositionConverter(impl.getSnapshot());
    }
            
    /**
     * Returns {@link Document} of this {@link CompilationInfoImpl}
     * @return Document or null when the {@link DataObject} doesn't
     * exist or has no {@link EditorCookie}.
     * @throws java.io.IOException
     */
    public @CheckForNull Document getDocument() throws IOException { //XXX cleanup: IOException is no longer required? Used by PositionEstimator, DiffFacility
        checkConfinement();
        return this.impl.getDocument();
    }
    
    
    /**
     * Returns {@link TreeUtilities}.
     * @return TreeUtilities
     */
    public synchronized @NonNull TreeUtilities getTreeUtilities() {
        checkConfinement();
        if (treeUtilities == null) {
            treeUtilities = new TreeUtilities(this);
        }
        return treeUtilities;
    }
    
    /**
     * Returns {@link ElementUtilities}.
     * @return ElementUtilities
     */
    public synchronized @NonNull ElementUtilities getElementUtilities() {
        checkConfinement();
        if (elementUtilities == null) {
            elementUtilities = new ElementUtilities(this);

        }
        return elementUtilities;
    }
    
    /**Get the TypeUtilities.
     * @return an instance of TypeUtilities
     */
    public synchronized @NonNull TypeUtilities getTypeUtilities() {
        checkConfinement();
        if (typeUtilities == null) {
            typeUtilities = new TypeUtilities(this);
        }
        return typeUtilities;
    }
    
    /**
     * Returns the {@link SourceVersion} used by the javac represented by this {@link CompilationInfo}.
     * @return SourceVersion
     * @since 0.47
     */
    public @NonNull SourceVersion getSourceVersion() {
        checkConfinement();
        return Source.toSourceVersion(Source.instance(impl.getJavacTask().getContext()));
    }

    /**Retrieve a value cached under the given key using the
     * {@link #putCachedValue(java.lang.Object, java.lang.Object, org.netbeans.api.java.source.CompilationInfo.CacheClearPolicy)} method.
     *
     * @param key for which the cached value should be retrieved
     * @return value originally passed to {@link #putCachedValue(java.lang.Object, java.lang.Object, org.netbeans.api.java.source.CompilationInfo.CacheClearPolicy)}, or null if none
     * @since 0.90
     */
    public @CheckForNull Object getCachedValue(@NonNull Object key) {
        Parameters.notNull("key", key);
        return impl.getCachedValue(key);
    }

    /**Put a value into a cache under the given key. The {@link #clearPolicy} parameter specifies the latest time the
     * references to the key and value should be cleared. The infrastructure is free to clear the references at any earlier time.
     * The clients should not depend on this cache for correctness, only to improve performance.
     *
     * @param key a unique key under which the value should be stored
     * @param value the value to store - any value previously set under the same key will be erased from the cache
     * @param clearPolicy the latest time when the mapping should be cleared from the cache
     * @since 0.90
     */
    public void putCachedValue(@NonNull Object key, @NullAllowed Object value, @NonNull CacheClearPolicy clearPolicy) {
        Parameters.notNull("key", key);
        Parameters.notNull("clearPolicy", clearPolicy);
        impl.putCachedValue(key, value, clearPolicy);
    }

    /**
     * Marks this {@link CompilationInfo} as invalid, may be used to
     * verify confinement.
     */
    final void invalidate () {
        this.invalid = true;
        this.impl.taskFinished();
        doInvalidate();
    }
    
    protected void doInvalidate () {
        final JavacParser parser = this.impl.getParser();
        if (parser != null) {
            parser.resultFinished (true);
        }
    }
    
    /**
     * Checks concurrency confinement.
     * When {@link VERIFY_CONFINEMENT} is enabled & thread accesses the CompilationInfo
     * outside guarded run() method the {@link IllegalStateException} is thrown
     * @throws java.lang.IllegalStateException
     */
    final void checkConfinement () throws IllegalStateException {
        if (VERIFY_CONFINEMENT && this.invalid) {
            throw new IllegalStateException (String.format("Access to the shared %s outside a guarded run method.", this.getClass().getSimpleName()));
        }
    }

    /**Constants to specify when a valued cached by {@link #putCachedValue(java.lang.Object, java.lang.Object, org.netbeans.api.java.source.CompilationInfo.CacheClearPolicy)}
     * should be evicted from the cache.
     *
     * @since 0.90
     */
    public enum CacheClearPolicy {
        /** The cached value will be cleared at the end of the current parsing.api's task. */
        ON_TASK_END,
        /** The cached value will be cleared on any change in the source document. */
        ON_CHANGE,
        /** The cached value will be cleared when a method/field/class is added, removed or its signature changes. */
        ON_SIGNATURE_CHANGE;
    }
}
