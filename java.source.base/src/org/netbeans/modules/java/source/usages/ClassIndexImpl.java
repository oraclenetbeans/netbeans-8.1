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

package org.netbeans.modules.java.source.usages;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.TypeElement;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.parsing.lucene.support.Convertor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Pair;
import org.openide.util.BaseUtilities;

/** Should probably final class with private constructor.
 *
 * @author Petr Hrebejk, Tomas Zezula
 */
public abstract class ClassIndexImpl {

    public static enum State {
        NEW,
        INITIALIZED,
    }

    public static enum UsageType {
        SUPER_CLASS,
        SUPER_INTERFACE,
        FIELD_REFERENCE,
        METHOD_REFERENCE,
        TYPE_REFERENCE,
        FUNCTIONAL_IMPLEMENTORS;
    }

    /**
     * Type of ClassIndexImpl
     */
    public static enum Type {
        /**
         * Index does not exist yet or
         * it's broken
         */
        EMPTY,

        /**
         * Index for source root
         */
        SOURCE,

        /**
         * Index for binary root
         */
        BINARY;
    }

    public static final ThreadLocal<AtomicBoolean> cancel = new ThreadLocal<AtomicBoolean> ();       
    public static ClassIndexFactory FACTORY;
    private static final Logger LOG = Logger.getLogger(ClassIndexImpl.class.getName());
    
    private State state = State.NEW;
    private final List<WeakReference<ClassIndexImplListener>> listeners = Collections.synchronizedList(new ArrayList<WeakReference<ClassIndexImplListener>> ());

    public abstract <T> void search (
            @NonNull ElementHandle<?> binaryName,
            @NonNull Set<? extends UsageType> usageType,
            @NonNull Set<? extends ClassIndex.SearchScopeType> scope,
            @NonNull Convertor<? super Document, T> convertor,
            @NonNull Set<? super T> result) throws IOException, InterruptedException;

    public abstract <T> void getDeclaredTypes (
            @NonNull String name,
            @NonNull ClassIndex.NameKind kind,
            @NonNull Set<? extends ClassIndex.SearchScopeType> scope,
            @NonNull FieldSelector selector,
            @NonNull Convertor<? super Document, T> convertor,
            @NonNull Collection<? super T> result) throws IOException, InterruptedException;

    public final <T> void getDeclaredTypes (
            @NonNull String name,
            @NonNull ClassIndex.NameKind kind,
            @NonNull Set<? extends ClassIndex.SearchScopeType> scope,
            @NonNull Convertor<? super Document, T> convertor,
            @NonNull Collection<? super T> result) throws IOException, InterruptedException {
        getDeclaredTypes(
            name,
            kind,
            scope,
            DocumentUtil.declaredTypesFieldSelector(false),
            convertor,
            result);
    }

    public abstract <T> void getDeclaredElements (
            String ident,
            ClassIndex.NameKind kind,
            Convertor<? super Document, T> convertor,
            Map<T,Set<String>> result) throws IOException, InterruptedException;
    
    public abstract void getPackageNames (String prefix, boolean directOnly, Set<String> result) throws IOException, InterruptedException;
    
    public abstract void getReferencesFrequences (
            @NonNull final Map<String,Integer> typeFreq,
            @NonNull final Map<String,Integer> pkgFreq) throws IOException, InterruptedException;
    
    public abstract FileObject[] getSourceRoots ();
   
    public abstract FileObject[] getBinaryRoots ();
    
    public abstract BinaryAnalyser getBinaryAnalyser ();
    
    public abstract SourceAnalyzerFactory.StorableAnalyzer getSourceAnalyser ();
    
    public abstract String getSourceName (String binaryName) throws IOException, InterruptedException;
    
    public abstract void setDirty (URL url);
    
    public abstract boolean isValid ();

    public abstract Type getType();

    protected abstract void close () throws IOException;    
    
    public void addClassIndexImplListener (final ClassIndexImplListener listener) {
        assert listener != null;        
        this.listeners.add (new Ref (listener));
    }
    
    public void removeClassIndexImplListener (final ClassIndexImplListener listener) {
        assert listener != null;
        synchronized (this.listeners) {
            for (Iterator<WeakReference<ClassIndexImplListener>> it = this.listeners.iterator(); it.hasNext();) {
                WeakReference<ClassIndexImplListener> lr = it.next();
                ClassIndexImplListener l = lr.get();
                if (listener == l) {
                    it.remove();
                }
            }
        }
    }

    void typesEvent (
            @NonNull final Collection<? extends ElementHandle<TypeElement>> added,
            @NonNull final Collection<? extends ElementHandle<TypeElement>> removed,
            @NonNull final Collection<? extends ElementHandle<TypeElement>> changed) {
        final ClassIndexImplEvent a = added == null || added.isEmpty() ? null : new ClassIndexImplEvent(this, added);
        final ClassIndexImplEvent r = removed == null || removed.isEmpty() ? null : new ClassIndexImplEvent(this, removed);
        final ClassIndexImplEvent ch = changed == null || changed.isEmpty() ? null : new ClassIndexImplEvent(this, changed);
        typesEvent(a, r, ch);
    }

    private void typesEvent (final ClassIndexImplEvent added, final ClassIndexImplEvent removed, final ClassIndexImplEvent changed) {
        WeakReference<ClassIndexImplListener>[] _listeners;
        synchronized (this.listeners) {
            _listeners = this.listeners.toArray(new WeakReference[this.listeners.size()]);
        }
        for (WeakReference<ClassIndexImplListener> lr : _listeners) {
            ClassIndexImplListener l = lr.get();
            if (l != null) {
                if (added != null) {
                    l.typesAdded(added);
                }
                if (removed != null) {
                    l.typesRemoved(removed);
                }
                if (changed != null) {
                    l.typesChanged(changed);
                }
            }
        }
    }

    public final State getState() {
        return this.state;
    }

    public final void setState(final State state) {
        assert state != null;
        assert this.state != null;
        if (state.ordinal() < this.state.ordinal()) {
            throw new IllegalArgumentException();
        }
        this.state=state;
    }
    
    /**
     * Handles exception. When exception is thrown from the non initialized index,
     * the index has not been checked if it's corrupted. If it's corrupted don't display
     * the error to user just log it. The index will be recovered during the scan.
     * @param ret ret value
     * @param e exception
     * @return ret
     * @throws Exception 
     */
    @CheckForNull
    protected final <R, E extends Exception> R handleException (
            @NullAllowed final R ret,
            @NonNull final E e,
            @NullAllowed final URL root) throws E {
        if (State.NEW == getState()) {
            LOG.log(Level.FINE, "Exception from non initialized index", e); //NOI18N
            return ret;
        } else {
            throw Exceptions.attachMessage(e, "Index state: " + state + ", Root: " + root); //NOI18N
        }
    }
    
    public static interface Writer {
        void clear() throws IOException;
        void deleteAndStore(final List<Pair<Pair<String,String>, Object[]>> refs, final Set<Pair<String,String>> toDelete) throws IOException;
        /**
         * Different from deleteAndStore in that the data is NOT committed, but just flushed. Make sure, deleteAndStore is called from the
         * indexer's finish!
         * 
         * @param refs
         * @param toDelete
         * @throws IOException 
         */
        void deleteAndFlush(final List<Pair<Pair<String,String>, Object[]>> refs, final Set<Pair<String,String>> toDelete) throws IOException;
        
        /**
         * Flushes any pending data from deleteAndFlush as if deleteAndStore was called with empty collections
         */
        void commit() throws IOException;
        
        void rollback() throws IOException;
    }
    
    private class Ref extends WeakReference<ClassIndexImplListener> implements Runnable {
        public Ref (ClassIndexImplListener listener) {
            super (listener, BaseUtilities.activeReferenceQueue());
        }

        public void run() {
            listeners.remove(this);
        }
    }
}
