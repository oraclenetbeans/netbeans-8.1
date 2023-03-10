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

package org.netbeans.modules.java.source.usages;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.java.source.classpath.AptCacheForSourceQuery;
import org.netbeans.modules.java.source.indexing.JavaIndex;
import org.netbeans.modules.java.source.indexing.TransactionContext;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Pair;
import org.openide.util.Parameters;

/**
 *
 * @author Tomas Zezula
 */
public final class ClassIndexManager {

    public static final String PROP_DIRTY_ROOT = "dirty"; //NOI18N
    public static final String PROP_SOURCE_ROOT = "source";  //NOI18N

    private static final Logger LOG = Logger.getLogger(ClassIndexManager.class.getName());
    
    private static ClassIndexManager instance;
    private final Map<URL, ClassIndexImpl> instances = new HashMap<URL, ClassIndexImpl> ();
    private final Map<URL, ClassIndexImpl> transientInstances = new HashMap<URL, ClassIndexImpl> ();
    private final InternalLock internalLock = new InternalLock();
    private final Map<ClassIndexManagerListener,Void> listeners = Collections.synchronizedMap(new IdentityHashMap<ClassIndexManagerListener, Void>());
    private boolean closed;


    private ClassIndexManager() {
    }

    public void addClassIndexManagerListener (final ClassIndexManagerListener listener) {
        assert listener != null;
        this.listeners.put(listener,null);
    }

    public void removeClassIndexManagerListener (final ClassIndexManagerListener listener) {
        assert listener != null;
        this.listeners.remove(listener);
    }    

    @CheckForNull
    public ClassIndexImpl getUsagesQuery (@NonNull final URL root, final boolean beforeCreateAllowed) {
        final ClassIndexImpl[] index = new ClassIndexImpl[] {null};
        FileUtil.runAtomicAction(new Runnable() {
            @Override
            public void run() {
                synchronized (internalLock) {
                    assert root != null;
                    if (closed) {
                        return;
                    }
                    Pair<ClassIndexImpl,Boolean> pair = getClassIndex(root, beforeCreateAllowed, false);
                    index[0] = pair.first();
                    if (index[0] != null) {
                        return;
                    }
                    URL translatedRoot = AptCacheForSourceQuery.getSourceFolder(root);
                    if (translatedRoot != null) {
                        pair = getClassIndex(translatedRoot, beforeCreateAllowed, false);
                        index[0] = pair.first();
                        if (index[0] != null) {
                            return;
                        }
                    } else {
                        translatedRoot = root;
                    }
                    if (beforeCreateAllowed) {
                        try {
                            final String typeAttr = JavaIndex.getAttribute(translatedRoot, PROP_SOURCE_ROOT, null);
                            final String dirtyAttr = JavaIndex.getAttribute(translatedRoot, PROP_DIRTY_ROOT, null);
                            if (!Boolean.TRUE.toString().equals(dirtyAttr)) {                                
                                if (Boolean.TRUE.toString().equals(typeAttr)) {
                                    index[0] = PersistentClassIndex.create (
                                            root,
                                            JavaIndex.getIndex(root),
                                            ClassIndexImpl.Type.SOURCE,
                                            ClassIndexImpl.Type.SOURCE);
                                    transientInstances.put(root,index[0]);
                                } else if (Boolean.FALSE.toString().equals(typeAttr)) {
                                    index[0] = PersistentClassIndex.create (
                                            root,
                                            JavaIndex.getIndex(root),
                                            ClassIndexImpl.Type.BINARY,
                                            ClassIndexImpl.Type.BINARY);
                                    transientInstances.put(root,index[0]);
                                }
                            } else {
                                LOG.log(
                                    Level.FINE,
                                    "Index for root: {0} is broken.",   //NOI18N
                                    root);
                            }
                        } catch(IOException ioe) {
                            /*Handled bellow by return null*/
                        } catch(IllegalStateException ise) {
                          /* Required by some wrongly written tests
                           * which access ClassIndex without setting the cache dir
                           * Handled bellow by return null
                           */
                        }
                    }
                }
            }
        });
        return index[0];
    }

    @CheckForNull
    public ClassIndexImpl createUsagesQuery (
            @NonNull final URL root,
            final boolean source) throws IOException {
        final TransactionContext txc = TransactionContext.get();
        if (txc == null) {
            throw new IllegalStateException("Not in transaction");  //NOI18N
        }
        return createUsagesQuery(
            root,
            source,
            txc.get(ClassIndexEventsTransaction.class));
    }

    @CheckForNull
    public ClassIndexImpl createUsagesQuery (
            @NonNull final URL root,
            final boolean source,
            @NonNull final ClassIndexEventsTransaction cietx) throws IOException {
        Parameters.notNull("root", root);   //NOI18N
        Parameters.notNull("cietx", cietx); //NOI18N
        synchronized (internalLock) {
            if (closed) {
                return null;
            }
            Pair<ClassIndexImpl,Boolean> pair = getClassIndex (root, true, true);
            ClassIndexImpl qi = pair.first();
            if (qi == null) {
                qi = getUsagesQuery(root, true);
                if (qi == null) {
                    qi = PersistentClassIndex.create (
                            root,
                            JavaIndex.getIndex(root),
                            ClassIndexImpl.Type.EMPTY,
                            source ? ClassIndexImpl.Type.SOURCE : ClassIndexImpl.Type.BINARY);
                    this.instances.put(root,qi);
                    markAddedRoot(cietx, root);
                }
            }
            if (source && qi.getType() == ClassIndexImpl.Type.BINARY){
                //Wrongly set up freeform project, which is common for it, prefer source
                qi.close ();
                qi = PersistentClassIndex.create (
                        root,
                        JavaIndex.getIndex(root),
                        ClassIndexImpl.Type.SOURCE,
                        ClassIndexImpl.Type.SOURCE);
                this.instances.put(root,qi);
                this.transientInstances.remove(root);
                markAddedRoot(cietx, root);
            } else if (pair.second()) {
                markAddedRoot(cietx, root);
            }
            return qi;
        }
    }

    public void removeRoot (final URL root) throws IOException {
        synchronized (internalLock) {
            ClassIndexImpl ci = this.instances.remove(root);
            if (ci == null) {
                ci = this.transientInstances.remove(root);
            } else {
                assert !this.transientInstances.containsKey(root);
            }
            if (ci != null) {
                ci.close();
                markRemovedRoot(root);
            }
        }
    }

    public void close () {
        synchronized (internalLock) {
            closed = true;
            for (ClassIndexImpl ci : instances.values()) {
                try {
                    ci.close();
                } catch (IOException ioe) {
                    Exceptions.printStackTrace(ioe);
                }
            }
        }
    }

    void fire (
        @NonNull final Set<? extends URL> added,
        @NonNull final Set<? extends URL> removed) {
        final ClassIndexManagerEvent addEvent = added.isEmpty() ? null : new ClassIndexManagerEvent (this, added);
        final ClassIndexManagerEvent rmEvent = removed.isEmpty()? null : new ClassIndexManagerEvent (this, removed);
        fire(addEvent, rmEvent);
    }


    private void fire(
        @NullAllowed ClassIndexManagerEvent addEvent,
        @NullAllowed ClassIndexManagerEvent rmEvent) {
        if (!this.listeners.isEmpty()) {
            ClassIndexManagerListener[] _listeners;
            synchronized (this.listeners) {
                _listeners = this.listeners.keySet().toArray(new ClassIndexManagerListener[this.listeners.size()]);
            }
            for (ClassIndexManagerListener listener : _listeners) {
                if (addEvent != null) {
                    listener.classIndexAdded(addEvent);
                }
                if (rmEvent != null) {
                    listener.classIndexRemoved(rmEvent);
                }
            }
        }
    }
    
    @NonNull
    private Pair<ClassIndexImpl,Boolean> getClassIndex(
            final URL root,
            final boolean allowTransient,
            final boolean promote) {
        ClassIndexImpl index = this.instances.get (root);
        boolean promoted = false;
        if (index == null && allowTransient) {            
            if (promote) {
                index = this.transientInstances.remove(root);
                if (index != null) {
                    this.instances.put(root, index);
                    promoted = true;
                }
            } else {
                index = this.transientInstances.get(root);
            }
        }
        return Pair.<ClassIndexImpl,Boolean>of(index,promoted);
    }

    private void markAddedRoot(
        @NonNull ClassIndexEventsTransaction cietx,
        @NonNull URL root) {
        cietx.rootAdded(root);
    }

    private void markRemovedRoot(@NonNull URL root) {
        final TransactionContext txCtx = TransactionContext.get();
        txCtx.get(ClassIndexEventsTransaction.class).rootRemoved(root);
    }


    public static synchronized ClassIndexManager getDefault () {
        if (instance == null) {
            instance = new ClassIndexManager ();            
        }
        return instance;
    }

    private class InternalLock {}
}
