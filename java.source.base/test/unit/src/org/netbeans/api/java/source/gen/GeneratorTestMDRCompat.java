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
package org.netbeans.api.java.source.gen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.*;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.modules.java.source.transform.Transformer;
import org.netbeans.modules.java.JavaDataLoader;
import org.netbeans.modules.java.source.ClassIndexTestCase;
import org.netbeans.modules.java.source.TestUtil;
import org.netbeans.modules.java.source.save.Reindenter;
import org.netbeans.modules.java.source.usages.IndexUtil;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.SharedClassObject;
import org.openide.util.Utilities;

/**
 *
 * @author Pavel Flaska
 */
public abstract class GeneratorTestMDRCompat extends ClassIndexTestCase {

    File testFile = null;
    
    public GeneratorTestMDRCompat(String aName) {
        super(aName);
    }
    
    @Override
    protected void setUp() throws Exception {
        ClassPathProvider cpp = new ClassPathProvider() {
            public ClassPath findClassPath(FileObject file, String type) {
                if (type == ClassPath.SOURCE)
                    return ClassPathSupport.createClassPath(getSourcePath());
                    if (type == ClassPath.COMPILE)
                        return ClassPathSupport.createClassPath(new FileObject[0]);
                    if (type == ClassPath.BOOT)
                        return createClassPath(System.getProperty("sun.boot.class.path"));
                    return null;
            }
        };
        SharedClassObject loader = JavaDataLoader.findObject(JavaDataLoader.class, true);
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/source/resources/layer.xml"}, new Object[] {loader, cpp});
        MockMimeLookup.setInstances(MimePath.get("text/x-java"), new Reindenter.Factory());
        
        TestUtil.setupEditorMockServices();
        JEditorPane.registerEditorKitForContentType("text/x-java", "org.netbeans.modules.editor.java.JavaKit");
        File cacheFolder = new File(getWorkDir(), "var/cache/index");
        cacheFolder.mkdirs();
        IndexUtil.setCacheFolder(cacheFolder);
    }
    
    public <R, P> void process(final Transformer<R, P> transformer) throws IOException {
        assertNotNull(testFile);
        FileObject testSourceFO = FileUtil.toFileObject(testFile);
        assertNotNull(testSourceFO);
        JavaSource js = JavaSource.forFileObject(testSourceFO);
        js.runModificationTask(new Task<WorkingCopy>() {

            public void run(WorkingCopy wc) throws IOException {
                wc.toPhase(Phase.RESOLVED);
                SourceUtilsTestUtil2.run(wc, transformer);
            }
        }).commit();
        printFile();
    }
    
    static ClassPath createClassPath(String classpath) {
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
        List/*<PathResourceImplementation>*/ list = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            String item = tokenizer.nextToken();
            File f = FileUtil.normalizeFile(new File(item));
            URL url = getRootURL(f);
            if (url!=null) {
                list.add(ClassPathSupport.createResource(url));
            }
        }
        return ClassPathSupport.createClassPath(list);
    }
    
    // XXX this method could probably be removed... use standard FileUtil stuff
    private static URL getRootURL  (File f) {
        URL url = null;
        try {
            if (isArchiveFile(f)) {
                url = FileUtil.getArchiveRoot(Utilities.toURI(f).toURL());
            } else {
                url = Utilities.toURI(f).toURL();
                String surl = url.toExternalForm();
                if (!surl.endsWith("/")) {
                    url = new URL(surl+"/");
                }
            }
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
        return url;
    }
    
    private static boolean isArchiveFile(File f) {
        // the f might not exist and so you cannot use e.g. f.isFile() here
        String fileName = f.getName().toLowerCase();
        return fileName.endsWith(".jar") || fileName.endsWith(".zip");    //NOI18N
    }
    
    String getGoldenDir() {
        return getDataDir() + "/goldenfiles";
    }
    
    String getSourceDir() {
        return getDataDir().getAbsolutePath();
    }
    
    public static File getFile(String aDataDir, String aFileName) throws FileStateInvalidException {
        String result = new File(aDataDir).getAbsolutePath() + '/' + aFileName;
        return new File(result);
    }
    
    static JavaSource getJavaSource(File aFile) throws IOException {
        FileObject testSourceFO = FileUtil.toFileObject(aFile);
        assertNotNull(testSourceFO);
        return JavaSource.forFileObject(testSourceFO);
    }

    File getTestFile() {
        return testFile;
    }
    
    void assertFiles(final String aGoldenFile) throws IOException, FileStateInvalidException {
        assertFile("File is not correctly generated.",
            getTestFile(),
            getFile(getGoldenDir(), getGoldenPckg() + aGoldenFile),
            getWorkDir()
        );
    }
    
    void printFile() throws FileNotFoundException, IOException {
        PrintStream log = getLog();
        BufferedReader in = new BufferedReader(new FileReader(getTestFile()));
        String str;
        while ((str = in.readLine()) != null) {
            log.println(str);
        }
        in.close();
    }

    abstract String getGoldenPckg();

    abstract String getSourcePckg();

    FileObject[] getSourcePath() {
        return new FileObject[] {FileUtil.toFileObject(getDataDir())};
    }
}
