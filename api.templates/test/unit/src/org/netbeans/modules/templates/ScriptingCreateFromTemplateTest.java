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
 * Software is Sun Microsystems, Inc.
 *
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */

package org.netbeans.modules.templates;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.openide.loaders.CreateFromTemplateHandler;
import org.openide.util.SharedClassObject;
import org.openide.util.test.MockLookup;

/**
 *
 * @author Marek Fukala
 * @author Jaroslav Tulach
 */
public class ScriptingCreateFromTemplateTest extends NbTestCase {
    
    public ScriptingCreateFromTemplateTest(String testName) {
        super(testName);
    }
    
    @Override
    protected boolean runInEQ() {
        return true;
    }
    
    @Override
    protected void setUp() throws Exception {
        MockLookup.setInstances(SharedClassObject.findObject(SimpleLoader.class, true));
    }

    public void testCreateFromTemplateEncodingProperty() throws Exception {
        FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        FileObject fo = FileUtil.createData(root, "simpleObject.txt");
        OutputStream os = fo.getOutputStream();
        os.write("print(encoding)".getBytes());
        os.close();
        assertEquals("content/unknown", fo.getMIMEType());
        fo.setAttribute ("template", Boolean.TRUE);
        assertEquals("content/unknown", fo.getMIMEType());
        fo.setAttribute(ScriptingCreateFromTemplateHandler.SCRIPT_ENGINE_ATTR, "js");
        
        DataObject obj = DataObject.find(fo);
        DataFolder folder = DataFolder.findFolder(FileUtil.createFolder(root, "target"));
        
        Map<String,String> parameters = Collections.emptyMap();
        DataObject inst = obj.createFromTemplate(folder, "complex", parameters);
        FileObject instFO = inst.getPrimaryFile();
        
        Charset targetEnc = FileEncodingQuery.getEncoding(instFO);
        assertNotNull("Template encoding is null", targetEnc);
        String instText = IndentEngineIntTest.stripNewLines(instFO.asText());
        assertEquals("Encoding in template doesn't match", targetEnc.name(), instText);
    }

    public void testFreeFileExtension() throws Exception {
        FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        FileObject template = FileUtil.createData(root, "simple.pl");
        OutputStream os = template.getOutputStream();
        ScriptEngine jsEngine = new javax.script.ScriptEngineManager().getEngineByExtension("js");
        boolean isNashorn = (jsEngine != null && jsEngine.toString().indexOf("Nashorn") > 0);
        if (isNashorn) {
            // print() behaves like println() and println() does not exist:
            os.write("print('#!/usr/bin/perl'); print('# '+license); print('# '+name+' in '+nameAndExt);".getBytes());
        } else {
            os.write("println('#!/usr/bin/perl'); print('# ');println(license);print('# ');print(name);print(' in ');println(nameAndExt);".getBytes());
        }
        os.close();
        template.setAttribute("template", true);
        template.setAttribute(ScriptingCreateFromTemplateHandler.SCRIPT_ENGINE_ATTR, "js");
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("license", "GPL");
        parameters.put(CreateFromTemplateHandler.FREE_FILE_EXTENSION, true);
        String newLine = isNashorn ? System.getProperty("line.separator") : "\n";
            FileObject inst = DataObject.find(template).createFromTemplate(DataFolder.findFolder(root), "nue", parameters).getPrimaryFile();
            assertEquals("#!/usr/bin/perl"+newLine+"# GPL"+newLine+"# nue in nue.pl"+newLine, inst.asText());
            assertEquals("nue.pl", inst.getPath());
            /* XXX perhaps irrelevant since typical wizards disable Finish in this condition
            inst = DataObject.find(template).createFromTemplate(DataFolder.findFolder(root), "nue", parameters).getPrimaryFile();
            assertEquals("#!/usr/bin/perl\n# GPL\n# nue_1 in nue_1.pl\n", inst.asText());
            assertEquals("nue_1.pl", inst.getPath());
             */
            inst = DataObject.find(template).createFromTemplate(DataFolder.findFolder(root), "nue.cgi", parameters).getPrimaryFile();
            assertEquals("#!/usr/bin/perl"+newLine+"# GPL"+newLine+"# nue in nue.cgi"+newLine, inst.asText());
            assertEquals("nue.cgi", inst.getPath());
            /* XXX
            inst = DataObject.find(template).createFromTemplate(DataFolder.findFolder(root), "nue.cgi", parameters).getPrimaryFile();
            assertEquals("#!/usr/bin/perl\n# GPL\n# nue_1 in nue_1.cgi\n", inst.asText());
            assertEquals("nue_1.cgi", inst.getPath());
             */
            inst = DataObject.find(template).createFromTemplate(DataFolder.findFolder(root), "explicit.pl", parameters).getPrimaryFile();
            assertEquals("#!/usr/bin/perl"+newLine+"# GPL"+newLine+"# explicit in explicit.pl"+newLine, inst.asText());
            assertEquals("explicit.pl", inst.getPath());
            /* XXX
            inst = DataObject.find(template).createFromTemplate(DataFolder.findFolder(root), "explicit.pl", parameters).getPrimaryFile();
            assertEquals("#!/usr/bin/perl\n# GPL\n# explicit_1 in explicit_1.pl\n", inst.asText());
            assertEquals("explicit_1.pl", inst.getPath());
             */
    }
    
    //fix for this test was rolled back because of issue #120865
    public void XtestCreateFromTemplateDocumentCreated() throws Exception {
        FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        FileObject fo = FileUtil.createData(root, "simpleObject.txt");
        OutputStream os = fo.getOutputStream();
        os.write("test".getBytes());
        os.close();
        fo.setAttribute ("template", Boolean.TRUE);
        fo.setAttribute(ScriptingCreateFromTemplateHandler.SCRIPT_ENGINE_ATTR, "js");

        MockServices.setServices(MockMimeLookup.class);
        MockMimeLookup.setInstances(MimePath.parse("content/unknown"), new TestEditorKit());
        
        DataObject obj = DataObject.find(fo);
        DataFolder folder = DataFolder.findFolder(FileUtil.createFolder(root, "target"));
        
        assertFalse(TestEditorKit.createDefaultDocumentCalled);
        DataObject inst = obj.createFromTemplate(folder, "test");
        assertTrue(TestEditorKit.createDefaultDocumentCalled);
        
        String exp = "test";
        assertEquals(exp, inst.getPrimaryFile().asText());
    }
    
    public static final class SimpleLoader extends MultiFileLoader {
        public SimpleLoader() {
            super(SimpleObject.class.getName());
        }
        protected String displayName() {
            return "SimpleLoader";
        }
        protected FileObject findPrimaryFile(FileObject fo) {
            if (fo.hasExt("prima")) {
                return fo;
            }
            return null;
        }
        protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
            return new SimpleObject(this, primaryFile);
        }
        protected MultiDataObject.Entry createPrimaryEntry(MultiDataObject obj, FileObject primaryFile) {
            return new FE(obj, primaryFile);
        }
        protected MultiDataObject.Entry createSecondaryEntry(MultiDataObject obj, FileObject secondaryFile) {
            return new FileEntry(obj, secondaryFile);
        }
    }
    
    private static final class FE extends FileEntry {
        public FE(MultiDataObject mo, FileObject fo) {
            super(mo, fo);
        }

        @Override
        public FileObject createFromTemplate(FileObject f, String name) throws IOException {
            fail("I do not want to be called");
            return null;
        }
    }
    
    public static final class SimpleObject extends MultiDataObject {
        public SimpleObject(SimpleLoader l, FileObject fo) throws DataObjectExistsException {
            super(fo, l);
        }
        
        public String getName() {
            return getPrimaryFile().getNameExt();
        }
    }
    
    private static final class TestEditorKit extends DefaultEditorKit {
        
        static boolean createDefaultDocumentCalled;

        @Override
        public Document createDefaultDocument() {
            createDefaultDocumentCalled = true;
            return super.createDefaultDocument();
        }
        
    }

}
