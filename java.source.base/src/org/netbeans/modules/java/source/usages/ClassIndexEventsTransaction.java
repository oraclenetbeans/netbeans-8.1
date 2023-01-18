/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.source.usages;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.java.source.indexing.JavaIndex;
import org.netbeans.modules.java.source.indexing.TransactionContext;
import org.openide.util.Parameters;

/**
 * Transaction service for delivering {@link ClassIndex} events and updating
 * {@link BuildArtifactMapperImpl}.
 * The events are collected during indexing and firer when scan finished.
 * @author Tomas Zezula
 */
//@NotThreadSafe
public final class ClassIndexEventsTransaction extends TransactionContext.Service {

    private final boolean source;
    private Set<URL> removedRoots;
    private Collection<ElementHandle<TypeElement>> addedTypes;
    private Collection<ElementHandle<TypeElement>> removedTypes;
    private Collection<ElementHandle<TypeElement>> changedTypes;
    private Collection<File> addedFiles;
    private Collection<File> removedFiles;
    private URL addedRoot;
    private URL changesInRoot;
    private boolean closed;

    private ClassIndexEventsTransaction(final boolean src) {
        source = src;
        removedRoots = new HashSet<URL>();
        addedTypes = new HashSet<ElementHandle<TypeElement>>();
        removedTypes = new HashSet<ElementHandle<TypeElement>>();
        changedTypes = new HashSet<ElementHandle<TypeElement>>();
        addedFiles = new ArrayDeque<File>();
        removedFiles = new ArrayDeque<File>();
    }


    /**
     * Notifies the {@link ClassIndexEventsTransaction} that a root was added
     * into {@link ClassIndexManager}.
     * @param root the added root.
     */
    public void rootAdded(@NonNull final URL root) {
        checkClosedTx();
        assert root != null;
        assert addedRoot == null;
        assert changesInRoot == null || changesInRoot.equals(root);
        addedRoot = root;
    }

    /**
     * Notifies the {@link ClassIndexEventsTransaction} that a root was removed
     * from {@link ClassIndexManager}.
     * @param root the removed root.
     */
    public void rootRemoved(@NonNull final URL root) {
        checkClosedTx();
        assert root != null;
        removedRoots.add(root);
    }

    /**
     * Notifies the {@link ClassIndexEventsTransaction} that types were added
     * in the root.
     * @param root the root in which the types were added.
     * @param added the added types.
     */
    public void addedTypes(
        @NonNull final URL root,
        @NonNull final Collection<? extends ElementHandle<TypeElement>> added) {
        checkClosedTx();
        assert root != null;
        assert added != null;
        assert changesInRoot == null || changesInRoot.equals(root);
        assert addedRoot == null || addedRoot.equals(root);
        addedTypes.addAll(added);
        changesInRoot = root;
    }

    /**
     * Notifies the {@link ClassIndexEventsTransaction} that types were removed
     * from the root.
     * @param root the root from which the types were removed.
     * @param removed the removed types.
     */
    public void removedTypes(
        @NonNull final URL root,
        @NonNull final Collection<? extends ElementHandle<TypeElement>> removed) {
        checkClosedTx();
        assert root != null;
        assert removed != null;
        assert changesInRoot == null || changesInRoot.equals(root);
        assert addedRoot == null || addedRoot.equals(root);
        removedTypes.addAll(removed);
        changesInRoot = root;
    }

    /**
     * Notifies the {@link ClassIndexEventsTransaction} that types were changed
     * in the root.
     * @param root the root in which the types were changed.
     * @param changed the changed types.
     */
    public void changedTypes(
        @NonNull final URL root,
        @NonNull final Collection<? extends ElementHandle<TypeElement>> changed) {
        checkClosedTx();
        assert root != null;
        assert changed != null;
        assert changesInRoot == null || changesInRoot.equals(root);
        assert addedRoot == null || addedRoot.equals(root);
        changedTypes.addAll(changed);
        changesInRoot = root;
    }
    
    /**
     * Notifies the {@link ClassIndexEventsTransaction} that signature files were
     * added in the transaction for given root.
     * @param root the root for which the signature files were added.
     * @param files the added files.
     * @throws IllegalStateException if the {@link ClassIndexEventsTransaction} is
     * created for binary root.
     */
    public void addedCacheFiles(
        @NonNull final URL root,
        @NonNull final Collection<? extends File> files) throws IllegalStateException {
        checkClosedTx();
        Parameters.notNull("root", root); //NOI18N
        Parameters.notNull("files", files); //NOI18N
        if (!source) {
            throw new IllegalStateException("The addedCacheFiles can be called only for source root."); //NOI18N
        }
        assert changesInRoot == null || changesInRoot.equals(root);
        assert addedRoot == null || addedRoot.equals(root);
        addedFiles.addAll(files);
        changesInRoot = root;
    }
    
    /**
     * Notifies the {@link ClassIndexEventsTransaction} that signature files were
     * removed in the transaction for given root.
     * @param root the root for which the signature files were removed.
     * @param files the removed files.
     * @throws IllegalStateException if the {@link ClassIndexEventsTransaction} is
     * created for binary root.
     */
    public void removedCacheFiles(
        @NonNull final URL root,
        @NonNull final Collection<? extends File> files) throws IllegalStateException {
        checkClosedTx();
        Parameters.notNull("root", root);   //NOI18N
        Parameters.notNull("files", files); //NOI18N
        if (!source) {
            throw new IllegalStateException("The removedCacheFiles can be called only for source root.");   //NOI18N
        }
        assert changesInRoot == null || changesInRoot.equals(root);
        assert addedRoot == null || addedRoot.equals(root);
        removedFiles.addAll(files);
        changesInRoot = root;
    }

    @Override
    protected void commit() throws IOException {
        closeTx();
        try {
            try {
                if (!addedFiles.isEmpty() || !removedFiles.isEmpty()) {
                    assert changesInRoot != null;
                    BuildArtifactMapperImpl.classCacheUpdated(
                        changesInRoot,
                        JavaIndex.getClassFolder(changesInRoot),
                        Collections.unmodifiableCollection(removedFiles),
                        Collections.unmodifiableCollection(addedFiles),
                        false);
                }
            } finally {
                final ClassIndexManager ciManager = ClassIndexManager.getDefault();
                ciManager.fire(
                    addedRoot == null ? Collections.<URL>emptySet() : Collections.<URL>singleton(addedRoot),
                    Collections.unmodifiableSet(removedRoots));
                final ClassIndexImpl ci = changesInRoot == null ?
                    null:
                    ciManager.getUsagesQuery(changesInRoot, false);
                if (ci != null) {
                    ci.typesEvent(
                        Collections.unmodifiableCollection(addedTypes),
                        Collections.unmodifiableCollection(removedTypes),
                        Collections.unmodifiableCollection(changedTypes));
                }
            }
        } finally {
            clear();
        }
    }

    @Override
    protected void rollBack() throws IOException {
        closeTx();
        clear();
    }

    private void clear() {
        addedRoot = null;
        changesInRoot = null;
        removedRoots = null;
        addedTypes = null;
        removedTypes = null;
        changedTypes = null;
        addedFiles = null;
        removedFiles = null;
    }

    private void checkClosedTx() {
        if (closed) {
            throw new IllegalStateException("Already commited or rolled back transaction.");    //NOI18N
        }
    }

    private void closeTx() {
        checkClosedTx();
        closed = true;
    }

    /**
     * Creates a new instance of {@link ClassIndexEventsTransaction} service.
     * @param source the source flag, true for source roots, false for binary roots.
     * @return the {@link ClassIndexEventsTransaction}.
     */
    @NonNull
    public static ClassIndexEventsTransaction create(final boolean source) {
        return new ClassIndexEventsTransaction(source);
    }

}
