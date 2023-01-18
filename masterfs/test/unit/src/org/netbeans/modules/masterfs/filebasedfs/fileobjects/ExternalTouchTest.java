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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.masterfs.filebasedfs.fileobjects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

public class ExternalTouchTest extends NbTestCase {
    private Logger LOG;
    private FileObject testFolder;

    public ExternalTouchTest(String testName) {
        super(testName);
    }

    @Override
    protected Level logLevel() {
        return Level.FINE;
    }
    
    @Override
    protected void setUp() throws Exception {
        clearWorkDir();
        
        LOG = Logger.getLogger("test." + getName());
        Logger.getLogger("org.openide.util.Mutex").setUseParentHandlers(false);

        File dir = new File(getWorkDir(), "test");
        dir.mkdirs();
        testFolder = FileUtil.toFileObject(dir);
        assertNotNull("Test folder created", testFolder);

    }

    public void testChangeInChildrenNoticed() throws Exception {
        long lm = System.currentTimeMillis();
        FileObject fileObject1 = testFolder.createData("fileObject1");
        assertNotNull("Just to initialize the stamp", lm);
        FileObject[] arr = testFolder.getChildren();
        assertEquals("One child", 1, arr.length);
        assertEquals("Right child", fileObject1, arr[0]);

        File file = FileUtil.toFile(fileObject1);
        assertNotNull("File found", file);
        Reference<FileObject> ref = new WeakReference<FileObject>(fileObject1);
        arr = null;
        fileObject1 = null;
        assertGC("File Object can disappear", ref);


        class L extends FileChangeAdapter {
            int cnt;
            FileEvent event;
            
            @Override
            public void fileChanged(FileEvent fe) {
                LOG.info("file change " + fe.getFile());
                cnt++;
                event = fe;
            }
        }
        L listener = new L();
        testFolder.addRecursiveListener(listener);

        Thread.sleep(1000);

        FileOutputStream os = new FileOutputStream(file);
        os.write(10);
        os.close();

        if (lm > file.lastModified() - 50) {
            fail("New modification time shall be at last 50ms after the original one: " + (file.lastModified() - lm));
        }

        testFolder.refresh();

        assertEquals("Change notified", 1, listener.cnt);
        assertEquals("Right file", file, FileUtil.toFile(listener.event.getFile()));
        assertEquals("Right source", file.getParentFile(), FileUtil.toFile((FileObject)listener.event.getSource()));
    }
    public void testFindResourceDoesNotRefresh() throws Exception {
        FileObject fileObject1 = testFolder.createData("fileObject1");
        FileObject[] arr = testFolder.getChildren();
        assertEquals("One child", 1, arr.length);
        assertEquals("Right child", fileObject1, arr[0]);
        
        File testFile = FileUtil.toFile(testFolder);
        assertNotNull("Folder File found", testFile);
        final String path = testFolder.getPath() + "/file1.txt";
        final FileSystem fs = testFolder.getFileSystem();
        
        File newCh = new File(testFile, "file1.txt");
        newCh.createNewFile();
        
        FileObject fromResource = fs.findResource(path);
        FileObject fromToFO = FileUtil.toFileObject(newCh);
        FileObject fromSndResource = fs.findResource(path);
        
        assertNotNull("toFileObject does refresh", fromToFO);
        assertNull("fromResource does not refresh", fromResource);
        assertEquals("after refresh the result reflects reality", fromToFO, fromSndResource);
    }
    public void testNewChildNoticed() throws Exception {
        FileObject fileObject1 = testFolder.createData("fileObject1");
        FileObject[] arr = testFolder.getChildren();
        assertEquals("One child", 1, arr.length);
        assertEquals("Right child", fileObject1, arr[0]);

        File file = FileUtil.toFile(fileObject1);
        assertNotNull("File found", file);
        arr = null;
        fileObject1 = null;
        Reference<FileObject> ref = new WeakReference<FileObject>(fileObject1);
        assertGC("File Object can disappear", ref);

        Thread.sleep(100);

        class L extends FileChangeAdapter {
            int cnt;
            FileEvent event;

            @Override
            public void fileDataCreated(FileEvent fe) {
                cnt++;
                event = fe;
            }

        }
        L listener = new L();
        testFolder.addRecursiveListener(listener);

        File nfile = new File(file.getParentFile(), "new.txt");
        nfile.createNewFile();

        testFolder.refresh();

        assertEquals("Change notified", 1, listener.cnt);
        assertEquals("Right file", nfile, FileUtil.toFile(listener.event.getFile()));
    }
    public void testDeleteOfAChildNoticed() throws Exception {
        FileObject fileObject1 = testFolder.createData("fileObject1");
        FileObject[] arr = testFolder.getChildren();
        assertEquals("One child", 1, arr.length);
        assertEquals("Right child", fileObject1, arr[0]);

        File file = FileUtil.toFile(fileObject1);
        assertNotNull("File found", file);
        arr = null;
        fileObject1 = null;
        Reference<FileObject> ref = new WeakReference<FileObject>(fileObject1);
        assertGC("File Object can disappear", ref);

        Thread.sleep(100);

        class L extends FileChangeAdapter {
            int cnt;
            FileEvent event;

            @Override
            public void fileDeleted(FileEvent fe) {
                cnt++;
                event = fe;
            }

        }
        L listener = new L();
        testFolder.addRecursiveListener(listener);

        file.delete();

        testFolder.refresh();

        assertEquals("Change notified", 1, listener.cnt);
        assertEquals("Right file", file, FileUtil.toFile(listener.event.getFile()));
    }

    public void testRecursiveListener() throws Exception {
        FileObject sub;
        File fobj;
        File fsub;
        {
            FileObject obj = FileUtil.createData(testFolder, "my/sub/children/children.java");
            fobj = FileUtil.toFile(obj);
            assertNotNull("File found", fobj);
            sub = obj.getParent().getParent();
            fsub = FileUtil.toFile(sub);

            WeakReference<Object> ref = new WeakReference(obj);
            obj = null;
            assertGC("File object can disappear", ref);
        }

        class L implements FileChangeListener {
            StringBuilder sb = new StringBuilder();

            public void fileFolderCreated(FileEvent fe) {
                LOG.info("FolderCreated: " + fe.getFile());
                sb.append("FolderCreated");
            }

            public void fileDataCreated(FileEvent fe) {
                LOG.info("DataCreated: " + fe.getFile());
                sb.append("DataCreated");
            }

            public void fileChanged(FileEvent fe) {
                LOG.info("Changed: " + fe.getFile());
                sb.append("Changed");
            }

            public void fileDeleted(FileEvent fe) {
                LOG.info("Deleted: " + fe.getFile());
                sb.append("Deleted");
            }

            public void fileRenamed(FileRenameEvent fe) {
                LOG.info("Renamed: " + fe.getFile());
                sb.append("Renamed");
            }

            public void fileAttributeChanged(FileAttributeEvent fe) {
                LOG.info("AttributeChanged: " + fe.getFile());
                sb.append("AttributeChanged");
            }

            public void assertMessages(String txt, String msg) {
                assertEquals(txt, msg, sb.toString());
                sb.setLength(0);
            }
        }
        L recursive = new L();
        L flat = new L();

        sub.addFileChangeListener(flat);
        LOG.info("Adding listener");
        sub.addRecursiveListener(recursive);
        LOG.info("Adding listener finished");

        Thread.sleep(1000);

        File fo = new File(fobj.getParentFile(), "sibling.java");
        fo.createNewFile();
        LOG.info("sibling created, now refresh");
        FileUtil.refreshAll();
        LOG.info("sibling refresh finished");

        recursive.assertMessages("Creation", "DataCreated");
        flat.assertMessages("No messages in flat mode", "");

        Thread.sleep(1000);

        final OutputStream os = new FileOutputStream(fo);
        os.write(10);
        os.close();
        LOG.info("Before refresh");
        FileUtil.refreshAll();
        LOG.info("After refresh");

        flat.assertMessages("No messages in flat mode", "");
        recursive.assertMessages("written", "Changed");
        
        fo.setReadOnly();
        LOG.info("Read-only refresh before");
        FileUtil.refreshAll();
        LOG.info("Read-only refresh after");
        
        flat.assertMessages("No messages in flat mode", "");
        recursive.assertMessages("attribute changed", "AttributeChanged");
        
        fo.setWritable(true);

        fo.delete();
        FileUtil.refreshAll();

        flat.assertMessages("No messages in flat mode", "");
        recursive.assertMessages("gone", "Deleted");

        new File(fsub, "testFolder").mkdirs();
        FileUtil.refreshAll();

        flat.assertMessages("Direct Folder notified", "FolderCreated");
        recursive.assertMessages("Direct Folder notified", "FolderCreated");

        new File(fsub.getParentFile(), "unimportant.txt").createNewFile();
        FileUtil.refreshAll();

        flat.assertMessages("No messages in flat mode", "");
        recursive.assertMessages("No messages in recursive mode", "");

        File deepest = new File(new File(new File(fsub, "deep"), "deeper"), "deepest");
        deepest.mkdirs();
        FileUtil.refreshAll();

        flat.assertMessages("Folder in flat mode", "FolderCreated");
        recursive.assertMessages("Folder detected", "FolderCreated");

        File hidden = new File(deepest, "hide.me");
        hidden.createNewFile();
        FileUtil.refreshAll();

        flat.assertMessages("No messages in flat mode", "");
        recursive.assertMessages("Folder detected", "DataCreated");


        sub.removeRecursiveListener(recursive);

        new File(fsub, "test.data").createNewFile();
        FileUtil.refreshAll();

        flat.assertMessages("Direct file notified", "DataCreated");
        recursive.assertMessages("No longer active", "");

        WeakReference<L> ref = new WeakReference<L>(recursive);
        recursive = null;
        assertGC("Listener can be GCed", ref);
    }

}
