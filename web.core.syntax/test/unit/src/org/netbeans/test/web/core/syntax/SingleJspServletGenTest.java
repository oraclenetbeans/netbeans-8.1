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
package org.netbeans.test.web.core.syntax;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.Document;

import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.j2ee.core.Profile;
import org.netbeans.api.jsp.lexer.JspTokenId;
import org.netbeans.api.lexer.Language;
import org.netbeans.editor.BaseDocument;
import org.netbeans.lib.lexer.test.TestLanguageProvider;
import org.netbeans.modules.css.lib.api.CssTokenId;
import org.netbeans.modules.j2ee.dd.api.web.WebAppMetadata;
import org.netbeans.modules.j2ee.metadata.model.api.MetadataModel;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.web.api.webmodule.WebModule;
import org.netbeans.modules.web.core.syntax.EmbeddingProviderImpl;
import org.netbeans.modules.web.core.syntax.JSPProcessor;
import org.netbeans.modules.web.core.syntax.JspKit;
import org.netbeans.modules.web.core.syntax.SimplifiedJspServlet;
import org.netbeans.modules.web.core.syntax.gsf.JspEmbeddingProvider;
import org.netbeans.modules.web.core.syntax.indent.JspIndentTaskFactory;
import org.netbeans.modules.web.spi.webmodule.WebModuleFactory;
import org.netbeans.modules.web.spi.webmodule.WebModuleImplementation2;
import org.netbeans.modules.web.spi.webmodule.WebModuleProvider;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.test.MockLookup;



/**
 * @author ads
 *
 */
public class SingleJspServletGenTest extends TestBase2 {
    
    private static final String TEST_FOLDER_SINGLE_JPS = "testSingleJsps";

    public SingleJspServletGenTest( String name ) {
        super(name);
    }
    
    public void testTrivialCase() throws Exception{
        generateServlet("TrivialCase");
    }

    public void testJSPDeclaration() throws Exception{
        generateServlet("JSPDeclaration");
    }
    
    protected void generateServlet( String testName ) throws Exception{
        String fileName = testName + ".jsp";
        SimplifiedJspServlet processor = getProcessor(fileName);
        processor.process();
        Embedding servlet = processor.getSimplifiedServlet();
        assertServletMatches( fileName, servlet.getSnapshot().getText().toString());
    }
    
    protected void generateServlet( String testFolder, String fileName ) throws Exception{
        SimplifiedJspServlet processor = getProcessor( testFolder +"/"+fileName);
        processor.process();
        Embedding servlet = processor.getSimplifiedServlet();
        assertServletMatches( testFolder , fileName , 
                servlet.getSnapshot().getText().toString());
    }
    
    protected void assertServletMatches( String fileName , String content ) 
        throws Exception
    {
        String filePath = TEST_FOLDER_SINGLE_JPS+"/"+fileName;
        String newFile;
        int i = filePath.lastIndexOf('.');
        if ( i >-1){
            newFile = filePath.substring(0, i);
        }
        else {
            newFile = filePath;
        }
        newFile = newFile+".java";                      // NOI18N
        assertFileContentsMatches(filePath, newFile,content);
    }
    
    protected void assertServletMatches( String testFolder , String fileName , 
            String content ) throws Exception
    {
        assertServletMatches( testFolder+"/"+fileName, content);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        JSPProcessor.ignoreLockFromUnitTest = true;
        MockLookup.setInstances(new TestClassPathProvider(createClassPaths()),
                new TestLanguageProvider(), 
                new TestWebModuleProvider(getTestFile(TEST_FOLDER_SINGLE_JPS)));
        initParserJARs();
        
        // init TestLanguageProvider
        assert Lookup.getDefault().lookup(TestLanguageProvider.class) != null;

        TestLanguageProvider.register(CssTokenId.language());
        TestLanguageProvider.register(HTMLTokenId.language());
        TestLanguageProvider.register(JspTokenId.language());
        TestLanguageProvider.register(JavaTokenId.language());
        TestLanguageProvider.register(JsTokenId.javascriptLanguage());
        
        JspIndentTaskFactory jspReformatFactory = new JspIndentTaskFactory();
        MockMimeLookup.setInstances(MimePath.parse("text/x-jsp"), new JspKit("text/x-jsp"), 
                jspReformatFactory, new EmbeddingProviderImpl.Factory(), 
                new JspEmbeddingProvider.Factory());

    }
    
    @Override
    protected BaseDocument getDocument(FileObject fo, String mimeType, Language language) {
        try {
            DataObject dobj = DataObject.find(fo);
            assertNotNull(dobj);

            EditorCookie ec = (EditorCookie) dobj.getCookie(EditorCookie.class);
            assertNotNull(ec);

            return (BaseDocument) ec.openDocument();
        } catch (Exception ex) {
            fail(ex.toString());
            return null;
        }
    }
    
    private SimplifiedJspServlet getProcessor( String fileName ){
        FileObject fo = getTestFile(TEST_FOLDER_SINGLE_JPS +"/" +fileName );
        assertNotNull(fo);
        BaseDocument doc = getDocument(fo);
        SimplifiedJspServlet processor = new SimplifiedJspServlet( 
                createSnaphot(doc) , doc );
        return processor;
    }
    
    private Snapshot createSnaphot(Document doc){
        return Source.create( doc ).createSnapshot();
    }
    
    private Map<String, ClassPath> createClassPaths() throws Exception {
        Map<String, ClassPath> cps = new HashMap<String, ClassPath>();
        ClassPath cp = createServletAPIClassPath();
        cps.put(ClassPath.COMPILE, cp);
        return cps;
    }
    
    private static class TestWebModuleProvider implements WebModuleProvider {

        public TestWebModuleProvider(FileObject webRoot) {
            myWebRoot = webRoot;
        }

        public WebModule findWebModule(FileObject file) {
            return WebModuleFactory.createWebModule(
                    new TestWebModuleImplementation2(myWebRoot));
        }
        
        private FileObject myWebRoot;
        
    }

    private static class TestWebModuleImplementation2 
        implements WebModuleImplementation2 
    {

        public TestWebModuleImplementation2(FileObject webRoot) {
            myWebRoot = webRoot;
        }

        public FileObject getDocumentBase() {
            return myWebRoot;
        }

        public String getContextPath() {
            return "/";
        }

        public Profile getJ2eeProfile() {
            return Profile.JAVA_EE_6_FULL;
        }

        public FileObject getWebInf() {
            return null;
        }

        public FileObject getDeploymentDescriptor() {
            return null;
        }

        public FileObject[] getJavaSources() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public MetadataModel<WebAppMetadata> getMetadataModel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }
        
        private FileObject myWebRoot;

    }

}
