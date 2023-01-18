/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009-2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints.spiimpl.batch;

import org.netbeans.modules.java.hints.spiimpl.batch.TestUtils.File;
import org.netbeans.modules.java.hints.spiimpl.MessageImpl;
import org.netbeans.modules.java.hints.providers.spi.HintDescription;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import junit.framework.TestSuite;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.classpath.GlobalPathRegistryEvent;
import org.netbeans.api.java.classpath.GlobalPathRegistryListener;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.junit.RandomlyFails;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.Folder;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.Resource;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.MimeTypes;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;

import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import static org.netbeans.modules.java.hints.spiimpl.batch.TestUtils.writeFilesAndWaitForScan;
import static org.netbeans.modules.java.hints.spiimpl.batch.TestUtils.prepareHints;

/**
 *
 * @author lahvac
 */
public class BatchSearchTest extends NbTestCase {

    public BatchSearchTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        TestSuite result = new NbTestSuite();

        result.addTestSuite(BatchSearchTest.class);
//        result.addTest(new BatchSearchTest("testBatchSearchFolderRemoteIndex"));

        return result;
    }

    //XXX: copied from CustomIndexerImplTest:
    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[0], new Object[0]);
        Main.initializeURLFactory();
        org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        prepareTest();
        MimeTypes.setAllMimeTypes(Collections.singleton("text/x-java"));
        sourceCP = ClassPathSupport.createClassPath(src1, src2);
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {sourceCP});
        RepositoryUpdater.getDefault().start(true);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[] {sourceCP});
    }

    public void testBatchSearch1() throws Exception {
        writeFilesAndWaitForScan(src1,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));
        writeFilesAndWaitForScan(src2,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));

        Iterable<? extends HintDescription> hints = prepareHints("$1.isDirectory()");
        BatchResult result = BatchSearch.findOccurrences(hints, Scopes.allOpenedProjectsScope());
        Map<String, Iterable<String>> output = new HashMap<String, Iterable<String>>();

        for (Entry<FileObject, Collection<? extends Resource>> e : result.getResourcesWithRoots().entrySet()) {
            Collection<String> resourcesRepr = new LinkedList<String>();

            for (Resource r : e.getValue()) {
                resourcesRepr.add(r.getRelativePath());
            }

            output.put(e.getKey().getURL().toExternalForm(), resourcesRepr);
        }

        Map<String, Iterable<String>> golden = new HashMap<String, Iterable<String>>();

        golden.put(src1.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));
        golden.put(src2.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));

        assertEquals(golden, output);
    }

    public void testBatchSearchSpan() throws Exception {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void m() {\n" +
                      "        a(c.i().getFileObject());\n" +
                      "        if (span != null && span[0] != (-1) && span[1] != (-1));\n" +
                      "        c.i().getFileObject(\"\");\n" +
                      "    }\n" +
                      "}\n";

        writeFilesAndWaitForScan(src1, new File("test/Test.java", code));

        Iterable<? extends HintDescription> hints = prepareHints("$0.getFileObject($1)");
        BatchResult result = BatchSearch.findOccurrences(hints, Scopes.allOpenedProjectsScope());

        assertEquals(1, result.getResources().size());
        Iterator<? extends Resource> resources = result.getResources().iterator().next().iterator();
        Resource r = resources.next();

        assertFalse(resources.hasNext());

        Set<String> snipets = new HashSet<String>();

        for (int[] span : r.getCandidateSpans()) {
            snipets.add(code.substring(span[0], span[1]));
        }

        Set<String> golden = new HashSet<String>(Arrays.asList("c.i().getFileObject(\"\")"));
        assertEquals(golden, snipets);
    }

    @RandomlyFails
    public void testBatchSearchNotIndexed() throws Exception {
        writeFilesAndWaitForScan(src1,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));
        writeFilesAndWaitForScan(src3,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { Test2 f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { public boolean isDirectory() {return false} }"));

        Iterable<? extends HintDescription> hints = prepareHints("$1.isDirectory()", "$1", "test.Test2");
        BatchResult result = BatchSearch.findOccurrences(hints, Scopes.specifiedFoldersScope(Folder.convert(src1, src3, empty)));
        Map<String, Iterable<String>> output = new HashMap<String, Iterable<String>>();

        for (Entry<FileObject, Collection<? extends Resource>> e : result.getResourcesWithRoots().entrySet()) {
            Collection<String> resourcesRepr = new LinkedList<String>();

            for (Resource r : e.getValue()) {
                resourcesRepr.add(r.getRelativePath());
            }

            output.put(e.getKey().getURL().toExternalForm(), resourcesRepr);
        }

        Map<String, Iterable<String>> golden = new HashMap<String, Iterable<String>>();

        golden.put(src1.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));
        golden.put(src3.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));

        assertEquals(golden, output);

        //check verification:
        Map<String, Map<String, Iterable<String>>> verifiedOutput = verifiedSpans(result, false);
        Map<String, Map<String, Iterable<String>>> verifiedGolden = new HashMap<String, Map<String, Iterable<String>>>();

        verifiedGolden.put(src1.getURL().toExternalForm(), Collections.<String, Iterable<String>>singletonMap("test/Test1.java", Arrays.<String>asList()));
        verifiedGolden.put(src3.getURL().toExternalForm(), Collections.<String, Iterable<String>>singletonMap("test/Test1.java", Arrays.asList("0:75-0:86:verifier:")));

        assertEquals(verifiedGolden, verifiedOutput);
    }

    public void testBatchSearchForceIndexingOfProperDirectory() throws Exception {
        FileObject data = FileUtil.createFolder(workdir, "data");
        FileObject dataSrc1 = FileUtil.createFolder(data, "src1");
        FileObject dataSrc2 = FileUtil.createFolder(data, "src2");
        writeFilesAndWaitForScan(dataSrc1,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));
        writeFilesAndWaitForScan(dataSrc2,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { Test2 f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { public boolean isDirectory() {return false} }"));

        ClassPathProviderImpl.setSourceRoots(Arrays.asList(dataSrc1, dataSrc2));

        Iterable<? extends HintDescription> hints = prepareHints("$1.isDirectory()", "$1", "test.Test2");
        BatchResult result = BatchSearch.findOccurrences(hints, Scopes.specifiedFoldersScope(Folder.convert(data)));
        Map<String, Iterable<String>> output = new HashMap<String, Iterable<String>>();

        for (Entry<FileObject, Collection<? extends Resource>> e : result.getResourcesWithRoots().entrySet()) {
            Collection<String> resourcesRepr = new HashSet<String>();

            for (Resource r : e.getValue()) {
                resourcesRepr.add(r.getRelativePath());
            }

            output.put(e.getKey().getURL().toExternalForm(), resourcesRepr);
        }

        Map<String, Iterable<String>> golden = new HashMap<String, Iterable<String>>();

        golden.put(data.getURL().toExternalForm(), new HashSet<String>(Arrays.asList("src1/test/Test1.java", "src2/test/Test1.java")));

        assertEquals(golden, output);

        //check verification:
        final Set<FileObject> added = new HashSet<FileObject>();
        final Set<FileObject> removed = new HashSet<FileObject>();

        GlobalPathRegistry.getDefault().addGlobalPathRegistryListener(new GlobalPathRegistryListener() {
            public void pathsAdded(GlobalPathRegistryEvent event) {
                for (ClassPath cp : event.getChangedPaths()) {
                    added.addAll(Arrays.asList(cp.getRoots()));
                }
            }
            public void pathsRemoved(GlobalPathRegistryEvent event) {
                for (ClassPath cp : event.getChangedPaths()) {
                    removed.addAll(Arrays.asList(cp.getRoots()));
                }
            }
        });

//        verifiedGolden.put(data.getURL().toExternalForm(), Arrays.asList("0:75-0:86:verifier:TODO: No display name"));
        Map<String, Map<String, Iterable<String>>> verifiedOutput = verifiedSpans(result, false);
        Map<String, Map<String, Iterable<String>>> verifiedGolden = new HashMap<String, Map<String, Iterable<String>>>();

        Map<String, Iterable<String>> verifiedGoldenPart = new HashMap<String, Iterable<String>>();

        verifiedGoldenPart.put("src1/test/Test1.java", Arrays.<String>asList());
        verifiedGoldenPart.put("src2/test/Test1.java", Arrays.<String>asList("0:75-0:86:verifier:"));

        verifiedGolden.put(data.getURL().toExternalForm(), verifiedGoldenPart);

        assertEquals(verifiedGolden, verifiedOutput);
        assertEquals(new HashSet<FileObject>(Arrays.asList(dataSrc1, dataSrc2)), added);
        assertEquals(new HashSet<FileObject>(Arrays.asList(dataSrc1, dataSrc2)), removed);
    }

    public void testBatchSearchFolderNoIndex() throws Exception {
        FileObject data = FileUtil.createFolder(workdir, "data");
        FileObject dataSrc1 = FileUtil.createFolder(data, "src1");
        FileObject dataSrc2 = FileUtil.createFolder(data, "src2");
        writeFilesAndWaitForScan(dataSrc1,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));
        writeFilesAndWaitForScan(dataSrc2,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { Test2 f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { public boolean isDirectory() {return false} }"));

        Iterable<? extends HintDescription> hints = prepareHints("$1.isDirectory()");
        BatchResult result = BatchSearch.findOccurrences(hints, Scopes.specifiedFoldersScope(Folder.convert(Collections.singleton(data)))); //XXX: should be a no-index variant!
        Map<String, Iterable<String>> output = toDebugOutput(result);
        Map<String, Iterable<String>> golden = new HashMap<String, Iterable<String>>();

        golden.put(data.getURL().toExternalForm(), new HashSet<String>(Arrays.asList("src1/test/Test1.java", "src2/test/Test1.java")));

        assertEquals(golden, output);
    }

    private FileObject workdir;
    private FileObject src1;
    private FileObject src2;
    private FileObject src3;
    private FileObject empty;
    private ClassPath sourceCP;

    private void prepareTest() throws Exception {
        workdir = SourceUtilsTestUtil.makeScratchDir(this);

        src1 = FileUtil.createFolder(workdir, "src1");
        src2 = FileUtil.createFolder(workdir, "src2");
        src3 = FileUtil.createFolder(workdir, "src3");
        empty = FileUtil.createFolder(workdir, "empty");

        ClassPathProviderImpl.setSourceRoots(Arrays.asList(src1, src2, src3));

        FileObject cache = FileUtil.createFolder(workdir, "cache");

        CacheFolder.setCacheFolder(cache);
    }

    private Map<String, Iterable<String>> toDebugOutput(BatchResult result) throws Exception {
        Map<String, Iterable<String>> output = new HashMap<String, Iterable<String>>();

        for (Entry<FileObject, Collection<? extends Resource>> e : result.getResourcesWithRoots().entrySet()) {
            Collection<String> resourcesRepr = new HashSet<String>();

            for (Resource r : e.getValue()) {
                resourcesRepr.add(r.getRelativePath());
            }

            output.put(e.getKey().getURL().toExternalForm(), resourcesRepr);
        }

        return output;
    }

    private Map<String, Map<String, Iterable<String>>> verifiedSpans(BatchResult candidates, boolean doNotRegisterClassPath) throws Exception {
        final Map<String, Map<String, Iterable<String>>> result = new HashMap<String, Map<String, Iterable<String>>>();
        List<MessageImpl> errors = new LinkedList<MessageImpl>();
        BatchSearch.getVerifiedSpans(candidates, new ProgressHandleWrapper(1), new BatchSearch.VerifiedSpansCallBack() {
            public void groupStarted() {}
            public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                Map<String, Iterable<String>> files = result.get(r.getRoot().getURL().toExternalForm());

                if (files == null) {
                    result.put(r.getRoot().getURL().toExternalForm(), files = new HashMap<String, Iterable<String>>());
                }

                Collection<String> currentHints = new LinkedList<String>();

                for (ErrorDescription ed : hints) {
                    currentHints.add(ed.toString());
                }

                files.put(r.getRelativePath(), currentHints);

                return true;
            }
            public void groupFinished() {}
            public void cannotVerifySpan(Resource r) {
                fail("Cannot verify: " +r.getRelativePath());
            }
        }, doNotRegisterClassPath, errors, new AtomicBoolean());

        return result;
    }

    @ServiceProvider(service=ClassPathProvider.class)
    public static final class ClassPathProviderImpl implements ClassPathProvider {

        private static Collection<FileObject> sourceRoots;

        public synchronized static void setSourceRoots(Collection<FileObject> sourceRoots) {
            ClassPathProviderImpl.sourceRoots = sourceRoots;
        }

        public synchronized static Collection<FileObject> getSourceRoots() {
            return sourceRoots;
        }

        public synchronized ClassPath findClassPath(FileObject file, String type) {
            if (ClassPath.BOOT.equals(type)) {
                return ClassPathSupport.createClassPath(getBootClassPath().toArray(new URL[0]));
            }

            if (ClassPath.COMPILE.equals(type)) {
                return ClassPathSupport.createClassPath(new URL[0]);
            }

            if (ClassPath.SOURCE.equals(type) && sourceRoots != null) {
                for (FileObject sr : sourceRoots) {
                    if (file.equals(sr) || FileUtil.isParentOf(sr, file)) {
                        return ClassPathSupport.createClassPath(sr);
                    }
                }
            }

            return null;
        }

    }

    //TODO: copied from SourceUtilsTestUtil:
    private static List<URL> bootClassPath;

    public static synchronized List<URL> getBootClassPath() {
        if (bootClassPath == null) {
            try {
                String cp = System.getProperty("sun.boot.class.path");
                List<URL> urls = new ArrayList<URL>();
                String[] paths = cp.split(Pattern.quote(System.getProperty("path.separator")));

                for (String path : paths) {
                    java.io.File f = new java.io.File(path);

                    if (!f.canRead())
                        continue;

                    FileObject fo = FileUtil.toFileObject(f);

                    if (FileUtil.isArchiveFile(fo)) {
                        fo = FileUtil.getArchiveRoot(fo);
                    }

                    if (fo != null) {
                        urls.add(fo.getURL());
                    }
                }

                bootClassPath = urls;
            } catch (FileStateInvalidException e) {
                Exceptions.printStackTrace(e);
            }
        }

        return bootClassPath;
    }

}
