/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.templates;

import java.util.Enumeration;
import org.openide.loaders.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Map;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.openide.loaders.DataObjectEncodingQueryImplementation;
import org.netbeans.modules.templates.ScriptingCreateFromTemplateHandler;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Enumerations;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author  Marian Petras
 */
public class Bug138973Test extends NbTestCase {

    private static final String TESTING_TEXT = "print('This is a testing text.')";
    private static final TestCharset TEST_CHARSET = new TestCharset();
    private static final String EXT = ".test";
    private static final String TEMPLATE_NAME = "Bug138973TestTemplate";
    private static final String TEMPLATE_NAME_EXT = TEMPLATE_NAME + EXT;
    private static final String TESTFILE_NAME = "testfile";
    private static final String TESTFILE_NAME_EXT = TESTFILE_NAME + EXT;

    public Bug138973Test(String n) {
        super(n);
    }

    public void testBug() throws Exception {
        MockServices.setServices(Pool.class, DataObjectEncodingQueryImplementation.class);
        FileUtil.setMIMEType("test", "text/test");
        MockMimeLookup.setInstances(MimePath.get("text/test"), new TestEncoding());
//        FileUtil.createData(FileUtil.getConfigRoot(), "Editors/text/test/" + TestEncoding.class.getName().replace('.', '-') + ".instance");

        FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        FileObject templatesFolder = root.createFolder("templates");
        assert templatesFolder != null;
        FileObject templateFile = FileUtil.createData(templatesFolder,
                                                      TEMPLATE_NAME_EXT);
        templateFile.setAttribute ("template", Boolean.TRUE);
        templateFile.setAttribute(ScriptingCreateFromTemplateHandler.SCRIPT_ENGINE_ATTR, "js");
        byte[] templateBytes = TESTING_TEXT.getBytes("ISO-8859-1");
        InputStream source = new ByteArrayInputStream(templateBytes);
        OutputStream target = templateFile.getOutputStream();
        FileUtil.copy(source, target);
        target.close();
        source.close();
        assert templateFile.getSize() != 0L;
        templateFile.setAttribute("template", Boolean.TRUE);

        assertEquals("text/test", templateFile.getMIMEType());

        assertEquals("No Decoder yet", 0, TestCharset.newDecoder);
        DataObject templateDataObj = DataObject.find(templateFile);
        DataObject newDataObj= templateDataObj.createFromTemplate(
                                                    DataFolder.findFolder(root),
                                                    TESTFILE_NAME);

        assertTrue("Decoder created", TestCharset.newDecoder >= 1);
    }

    public static final class SimpleTemplateHandler extends CreateFromTemplateHandler {
        @Override
        public boolean accept(FileObject orig) {
            return true;
        }
        @Override
        public FileObject createFromTemplate(FileObject template,
                                                FileObject targetFolder,
                                                String name,
                                                Map<String, Object> parameters) throws IOException {
            String nameUniq = FileUtil.findFreeFileName(targetFolder, name, template.getExt());
            FileObject newFile = FileUtil.createData(targetFolder, nameUniq + '.' + template.getExt());

            Charset templateEnc = FileEncodingQuery.getEncoding(template);
            Charset newFileEnc = FileEncodingQuery.getEncoding(newFile);

            InputStream is = template.getInputStream();
            Reader reader = new BufferedReader(new InputStreamReader(is, templateEnc));
            OutputStream os = newFile.getOutputStream();
            Writer writer = new BufferedWriter(new OutputStreamWriter(os, newFileEnc));
            int cInt;
            while ((cInt = reader.read()) != -1) {
                writer.write(cInt);
            }
            writer.close();
            reader.close();

            return newFile;
        }
    }

    public static final class SimpleLoader extends MultiFileLoader {
        public SimpleLoader() {
            super(SimpleObject.class.getName());
        }
        protected String displayName() {
            return "SimpleLoader";
        }
        @Override
        protected FileObject findPrimaryFile(FileObject fo) {
            if (fo.getNameExt().equals(TEMPLATE_NAME_EXT)) {
                return fo;
            }
            if (fo.getNameExt().equals(TESTFILE_NAME_EXT)) {
                return fo;
            }
            return null;
        }
        @Override
        protected MultiDataObject createMultiObject(FileObject primaryFile)
                                            throws DataObjectExistsException,
                                                   IOException {
            return new SimpleObject(this, primaryFile, isTestingFile(primaryFile));
        }
        @Override
        protected MultiDataObject.Entry createPrimaryEntry(MultiDataObject obj,
                                                           FileObject primaryFile) {
            return new FE(obj, primaryFile);
        }
        @Override
        protected MultiDataObject.Entry createSecondaryEntry(MultiDataObject obj,
                                                             FileObject secondaryFile) {
            return new FE(obj, secondaryFile);
        }
        private static boolean isTestingFile(FileObject fileObj) {
            return fileObj.getNameExt().equals(TESTFILE_NAME_EXT);
        }
    }

    private static final class FE extends FileEntry {
        public FE(MultiDataObject mo, FileObject fo) {
            super(mo, fo);
        }
        @Override
        public FileObject createFromTemplate(FileObject f, String name) throws IOException {
            fail("FileEntry.createFromTemplate() should not be called");
            return null;
        }
    }

    public static final class SimpleObject extends MultiDataObject {
        private final Lookup lookup;
        public SimpleObject(SimpleLoader l,
                            FileObject fo,
                            boolean useSpecialEncoding)
                                              throws DataObjectExistsException {
            super(fo, l);
            lookup = useSpecialEncoding
                     ? Lookups.fixed(this, new TestEncoding())
                     : Lookups.singleton(this);
        }
        @Override
        public String getName() {
            return getPrimaryFile().getNameExt();
        }
        @Override
        public Lookup getLookup() {
            return lookup;
        }
    }

    public static final class TestEncoding extends FileEncodingQueryImplementation {
        @Override
        public Charset getEncoding(FileObject file) {
            return TEST_CHARSET;
        }
    }

    static final class TestCharset extends Charset {
        static int newDecoder;
        static int newEncoder;

        TestCharset() {
            super("test_charset", null);
        }
        @Override
        public boolean contains(Charset charset) {
            return true;
        }
        @Override
        public CharsetDecoder newDecoder() {
            newDecoder++;
            return Charset.forName("UTF-8").newDecoder();
        }
        @Override
        public CharsetEncoder newEncoder() {
            newEncoder++;
            return Charset.forName("UTF-8").newEncoder();
        }
    }

    public static final class Pool extends DataLoaderPool {
        @Override
        protected Enumeration<? extends DataLoader> loaders() {
            return Enumerations.singleton(SimpleLoader.getLoader(SimpleLoader.class));
        }

    }
}