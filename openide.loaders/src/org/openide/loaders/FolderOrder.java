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

package org.openide.loaders;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import java.io.IOException;
import java.util.*;

import org.openide.filesystems.*;
import org.openide.filesystems.FileSystem.AtomicAction;
import org.openide.loaders.DataFolder.SortMode;


/** A support for keeping order of children for folder list.
 *
 * @author  Jaroslav Tulach
 */
final class FolderOrder extends Object implements Comparator<Object> {
    
    /** a static map with (FileObject, Reference (Folder))
     */
    private static final WeakHashMap<FileObject, Reference<FolderOrder>> map = 
            new WeakHashMap<FileObject, Reference<FolderOrder>> (101);
    /** A static of known folder orders. Though we hold the
     * FolderOrder with a soft reference which can be collected, even
     * if this happens we would like the new FolderOrder to have any
     * previously determined order attribute. Otherwise under obscure
     * circumstances (#15381) it is possible for the IDE to go into an
     * endless loop recalculating folder orders, since they keep
     * getting collected.
     */
    private static final Map<FileObject, Object> knownOrders = 
            Collections.synchronizedMap(new WeakHashMap<FileObject, Object>(50));
    

    /** map of names of primary files of objects to their index or null */
    private Map<String,Integer> order;
    /** file to store data in */
    private FileObject folder;
    /** a reference to sort mode of this folder order */
    private SortMode sortMode;
    /** previous value of the order */
    private Object previous;

    /** Constructor.
    * @param folder the folder to create order for
    */
    private FolderOrder (FileObject folder) {
        this.folder = folder;
    }
    
    
    /** Changes a sort order for this order
     * @param mode sort mode.
     */
    public void setSortMode (SortMode mode) throws IOException {
        // store the mode to properties
        sortMode = mode;
        mode.write (folder); // writes attribute EA_SORT_MODE -> updates FolderList
        
        // FolderList.changedFolderOrder (folder);
    }
    
    /** Getter for the sort order.
     */
    public SortMode getSortMode () {
        if (sortMode == null) {
            sortMode = SortMode.read (folder);
        }
        return sortMode;
    }
    
    /** Changes the order of data objects.
     */
    public void setOrder (final DataObject[] arr) throws IOException {
        // #251398: setOrder is internally synchronized(this) and fires FS events
        // while holding the lock over this FolderOrder, although the Order is
        // already updated. FSAA will defer event dispatch until after the FolderOrder
        // lock is released. Also helps Listeners to see the oder consistent, not
        // partially set as individual FObj attributes are changed.
        folder.getFileSystem().runAtomicAction(new AtomicAction() {
            public void run() throws IOException {
                doSetOrder(arr);
            }
        });
    }
        
    private synchronized void doSetOrder(DataObject[] arr) throws IOException {
        if (arr != null) {
            order = new HashMap<String, Integer> (arr.length * 4 / 3 + 1);

            // each object only once
            Enumeration<DataObject> en = org.openide.util.Enumerations.removeDuplicates (
                org.openide.util.Enumerations.array (arr)
            );

            int i = 0;
            while (en.hasMoreElements ()) {
                DataObject obj = en.nextElement ();
                FileObject fo = obj.getPrimaryFile ();
                if (folder.equals (fo.getParent ())) {
                    // object for my folder
                    order.put (fo.getNameExt (), Integer.valueOf (i++));
                }
            }
        } else {
            order = null;
        }
        
        write (); // writes attribute EA_ORDER -> updates FolderList
        
        
        // FolderList.changedFolderOrder (folder);
    }

    /** Compares two data object or two nodes.
    */
    public int compare (Object obj1, Object obj2) {
        Integer i1 = (order == null) ? null : order.get(FolderComparator.findFileObject(obj1).getNameExt());
        Integer i2 = (order == null) ? null : order.get(FolderComparator.findFileObject(obj2).getNameExt());

        if (i1 == null) {
            if (i2 != null) {
                return 1;
            }

            // compare by the provided comparator
            return ((FolderComparator)(getSortMode())).doCompare(obj1, obj2);
        } else {
            if (i2 == null) {
                return -1;
            }
            // compare integers
            if (i1.intValue () == i2.intValue ()) {
                return 0;
            }
            if (i1.intValue () < i2.intValue ()) {
                return -1;
            }
            return 1;
        }
    }

    /** Stores the order to files.
    */
    public void write () throws IOException {
        // Let it throw the IOException:
        //if (folder.getFileSystem ().isReadOnly ()) return; // cannot write to read-only FS
        if (folder.getAttribute(DataFolder.EA_ORDER) != null) {
            folder.setAttribute(DataFolder.EA_ORDER, null);
        }
        if (order != null) {
            FileObject[] children = new FileObject[order.size()];
            for (Map.Entry<String,Integer> entry : order.entrySet()) {
                children[entry.getValue()] = folder.getFileObject(entry.getKey());
            }
            FileUtil.setOrder(Arrays.asList(children));
        }
    }
    
    /** Reads the order from disk.
     */
    private void read () {
        Object o = folder.getAttribute (DataFolder.EA_ORDER);
        
        if ((previous == null && o == null) ||
            (previous != null && previous.equals (o))) {
            // no change in order
            return;
        }
        
        if ((o instanceof Object[]) && (previous instanceof Object[])) {
            if (compare((Object[]) o, (Object[]) previous)) {
                return;
            }
        }
        
        doRead (o);
        
        previous = o;
        if (previous != null) {
            knownOrders.put(folder, previous);
        }
        
        FolderList.changedFolderOrder (folder);
    }

    /** Compares two arrays */
    private static boolean compare(Object[] a, Object[] b) {
        if (a == b) {
            return true;
        }
        
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            if (a[i] != b[i]) {
                if (a[i] == null) {
                    return false;
                }
                
                if (a[i].equals(b[i])) {
                    continue;
                }
                
                if ((a[i] instanceof Object[]) && (b[i] instanceof Object[])) {
                    if (compare((Object[]) a[i], (Object[]) b[i])) {
                        continue;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        
        Object[] arr = (a.length > b.length) ? a : b;
        if (checkNonNull(arr, len)) {
            return false;
        }
        
        return true;
    }
    
    private static boolean checkNonNull(Object[] a, int from) {
        for (int i = from; i < a.length; i++) {
            if (a[i] != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /** Reads the values from the object o
     * @param o value of attribute EA_ORDER
     */
    private void doRead (Object o) {
        if (o == null) {
            order = null;
            return;
        } else if (o instanceof String[][]) {
            // Compatibility:
            String[][] namesExts = (String[][]) o;

            if (namesExts.length != 2) {
                order = null;
                return;
            }
            String[] names = namesExts[0];
            String[] exts = namesExts[1];

            if (names == null || exts == null || names.length != exts.length) {
                // empty order
                order = null;
                return;
            }


            Map<String, Integer> set = new HashMap<String, Integer> (names.length);

            for (int i = 0; i < names.length; i++) {
                set.put (names[i], Integer.valueOf (i));
            }
            order = set;
            return;
            
        } else if (o instanceof String) {
            // Current format:
            String sepnames = (String) o;
            Map<String, Integer> set = new HashMap<String, Integer> ();
            StringTokenizer tok = new StringTokenizer (sepnames, "/"); // NOI18N
            int i = 0;
            while (tok.hasMoreTokens ()) {
                String file = tok.nextToken ();
                set.put (file, Integer.valueOf (i));
                i++;
            }
            
            order = set;
            return;
        } else {
            // Unknown format:
            order = null;
            return;
        }
    }
    

    /** Creates order for given folder object.
    * @param f the folder
    * @return the order
    */
    public static FolderOrder findFor (FileObject folder) {
        FolderOrder order = null;
        synchronized (map) {
            Reference<FolderOrder> ref = map.get (folder);
            order = ref == null ? null : ref.get ();
            if (order == null) {
                order = new FolderOrder (folder);
                order.previous = knownOrders.get(folder);
                order.doRead(order.previous);
                
                map.put (folder, new SoftReference<FolderOrder> (order));
            }
        }
        // always reread the order from disk, so it is uptodate
        synchronized (order) {
            order.read ();
            return order;            
        }        
    }
        
     
    
}
