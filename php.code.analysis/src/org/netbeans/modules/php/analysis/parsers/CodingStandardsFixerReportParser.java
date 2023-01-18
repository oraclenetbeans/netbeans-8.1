/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.analysis.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.php.analysis.results.Result;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.api.util.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public final class CodingStandardsFixerReportParser extends DefaultHandler {

    private static final Logger LOGGER = Logger.getLogger(CodingStandardsFixerReportParser.class.getName());

    private final List<Result> results = new ArrayList<>();
    private final XMLReader xmlReader;

    private Result currentResult = null;
    private String currentFile = null;
    private final List<String> appliedFixers = new ArrayList<>();
    private StringBuilder description = null;
    private final FileObject root;

    private CodingStandardsFixerReportParser(FileObject root) throws SAXException {
        this.xmlReader = FileUtils.createXmlReader();
        this.root = root;
    }

    private static CodingStandardsFixerReportParser create(Reader reader, FileObject root) throws SAXException, IOException {
        CodingStandardsFixerReportParser parser = new CodingStandardsFixerReportParser(root);
        parser.xmlReader.setContentHandler(parser);
        parser.xmlReader.parse(new InputSource(reader));
        return parser;
    }

    @CheckForNull
    public static List<Result> parse(File resultFile, FileObject root) {
        try {
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(resultFile), "UTF-8"))) { // NOI18N
                return create(reader, root).getResults();
            }
        } catch (IOException | SAXException ex) {
            LOGGER.log(Level.INFO, null, ex);
        }
        return null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("file".equals(qName)) { // NOI18N
            processFileStart(attributes);
        } else if ("applied_fixer".equals(qName)) { // NOI18N
            appliedFixers.add(attributes.getValue("name")); // NOI18N
        } else if ("diff".equals(qName)) { // NOI18N
            processResultStart(attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("file".equals(qName)) { // NOI18N
            processFileEnd();
        } else if ("diff".equals(qName)) { // NOI18N
            processResultEnd();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (description != null) {
            description.append(new String(ch, start, length));
        }
    }

    private void processFileStart(Attributes attributes) {
        assert currentResult == null : currentResult.getFilePath();
        assert currentFile == null : currentFile;
        currentFile = getCurrentFile(attributes.getValue("name")); // NOI18N
    }

    private void processFileEnd() {
        currentFile = null;
    }

    private void processResultStart(Attributes attributes) {
        if (currentFile == null) {
            // #242935
            return;
        }
        assert currentResult == null : currentResult.getFilePath();
        assert description == null : description.toString();

        currentResult = new Result(currentFile);
        currentResult.setLine(1);
        currentResult.setColumn(0);
        currentResult.setCategory(StringUtils.implode(appliedFixers, ",")); // NOI18N
        description = new StringBuilder(200);
    }

    private void processResultEnd() {
        if (currentFile != null) {
            assert currentResult != null;
            assert description != null;
            currentResult.setDescription(formatDescription(description.toString()));
            results.add(currentResult);
        }
        currentResult = null;
        appliedFixers.clear();
        description = null;
    }

    private String formatDescription(String description) {
        String replaced = description.replaceAll("      (.+\n)", "$1"); // NOI18N
        return String.format("<pre>%s</pre>", replaced); // NOI18N
    }

    private String getCurrentFile(String fileName) {
        if (root.isFolder()) {
            FileObject current = root.getFileObject(fileName);
            if (current == null) {
                // #242935
                return null;
            }
            return FileUtil.toFile(current).getAbsolutePath();
        }
        return fileName;
    }

    //~ Getters

    public List<Result> getResults() {
        return Collections.unmodifiableList(results);
    }

}
