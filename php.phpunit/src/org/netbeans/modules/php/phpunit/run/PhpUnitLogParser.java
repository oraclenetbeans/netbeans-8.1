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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.phpunit.run;

import java.io.IOException;
import java.io.Reader;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.php.api.util.FileUtils;
import org.openide.util.Exceptions;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser of PHPUnit XML log files (version 3.x).
 * @author Tomas Mysik
 */
public final class PhpUnitLogParser extends DefaultHandler {

    enum Content { NONE, ERROR, FAILURE };
    private static final Logger LOGGER = Logger.getLogger(PhpUnitLogParser.class.getName());
    private static final String NO_FILE = "NO_FILE"; // NOI18N

    private final XMLReader xmlReader;
    private final TestSessionVo givenTestSession;
    private final TestSessionVo tmpTestSession = new TestSessionVo(null);
    private Deque<TestSuiteVo> testSuites = new LinkedList<>();
    private Set<TestSuiteVo> parentTestSuites = new HashSet<>();
    private TestCaseVo testCase;
    private String file; // actual file
    private Content content = Content.NONE;
    private StringBuilder buffer = new StringBuilder(200); // for error/failure: buffer for the whole message

    private PhpUnitLogParser(TestSessionVo testSession) throws SAXException {
        assert testSession != null;
        this.givenTestSession = testSession;
        xmlReader = FileUtils.createXmlReader();
    }

    static boolean parse(Reader reader, TestSessionVo testSession) {
        try {
            PhpUnitLogParser parser = new PhpUnitLogParser(testSession);
            parser.xmlReader.setContentHandler(parser);
            parser.xmlReader.parse(new InputSource(reader));
            parser.finish();
            return true;
        } catch (SAXException ex) {
            // ignore (this can happen e.g. if one interrupts debugging)
            LOGGER.log(Level.INFO, null, ex);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return false;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("testsuite".equals(qName)) { // NOI18N
            processTestSuiteStart(attributes);
        } else if ("testcase".equals(qName)) { // NOI18N
            processTestCase(attributes);
        } else if ("failure".equals(qName)) { // NOI18N
            startTestFailure(attributes);
        } else if ("error".equals(qName)) { // NOI18N
            startTestError(attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("testsuite".equals(qName)) { // NOI18N
            processTestSuiteEnd();
        } else if ("testcase".equals(qName)) { // NOI18N
            assert testCase != null;
            testCase = null;
        } else if ("failure".equals(qName) // NOI18N
                || "error".equals(qName)) { // NOI18N
            endTestContent();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (content) {
            case FAILURE:
            case ERROR:
                buffer.append(new String(ch, start, length));
                break;
            case NONE:
                // noop
                break;
            default:
                assert false : "Unknown content: " + content;
        }
    }

    private void processTestSuiteStart(Attributes attributes) {
        if (tmpTestSession.getTime() == -1
                && tmpTestSession.getTests() == -1) {
            // no active suite yet => set total/session info
            tmpTestSession.setTests(getTests(attributes));
            tmpTestSession.setTime(getTime(attributes));
            file = getFile(attributes, NO_FILE);
        } else {
            file = getFile(attributes);
        }
        assert file != null;

        // in any other suite?
        TestSuiteVo parentSuite = testSuites.peek();
        if (parentSuite != null) {
            parentTestSuites.add(parentSuite);
        }

        TestSuiteVo testSuite = new TestSuiteVo(getName(attributes), file, getTime(attributes));
        testSuites.push(testSuite);
        if (!file.equals(NO_FILE)) {
            tmpTestSession.addTestSuite(testSuite);
        }
    }

    private void processTestSuiteEnd() {
        testSuites.pop();
    }

    private void processTestCase(Attributes attributes) {
        assert testCase == null;
        testCase = new TestCaseVo(
                getClass(attributes),
                getName(attributes),
                getFile(attributes),
                getLine(attributes),
                getTime(attributes));
        testSuites.getFirst().addTestCase(testCase);
    }

    private void startTestError(Attributes attributes) {
        content = Content.ERROR;
    }

    private void startTestFailure(Attributes attributes) {
        content = Content.FAILURE;
    }

    private void endTestContent() {
        assert testCase != null;
        assert buffer.length() > 0;

        fillStacktrace();
        switch (content) {
            case FAILURE:
                testCase.setFailureStatus();
                break;
            case ERROR:
                testCase.setErrorStatus();
                break;
            default:
                assert false : "Unknown content type: " + content;
        }
        buffer = new StringBuilder(200);
        content = Content.NONE;
    }

    private void fillStacktrace() {
        String[] lines = buffer.toString().trim().split("\n"); // NOI18N
        assert lines.length >= 2 : "At least 2 lines must be found (message + stacktrace)";

        buffer = new StringBuilder(200);
        boolean stacktraceStarted = false;
        // 1st line is skipped
        for (int i = 1; i < lines.length; ++i) {
            String line = lines[i];
            if (line.trim().length() == 0) {
                if (!stacktraceStarted) {
                    // empty line => stacktrace started
                    stacktraceStarted = true;
                    testCase.addStacktrace(buffer.toString().trim());
                }
                continue;
            } else if (line.startsWith("\n\n")) { // NOI18N
                // empty line => stacktrace started
                stacktraceStarted = true;
                testCase.addStacktrace(buffer.toString().trim());
            } else if (!stacktraceStarted) {
                buffer.append(line);
                buffer.append("\n"); // NOI18N
            } else {
                testCase.addStacktrace(line.trim());
            }
        }
        // #157846
        if (testCase.getStackTrace().length == 0) {
            testCase.addStacktrace(buffer.toString().trim());
        }
    }

    private int getTests(Attributes attributes) {
        return getInt(attributes, "tests"); // NOI18N
    }

    private long getTime(Attributes attributes) {
        long l = -1;
        try {
            l = Math.round(Double.parseDouble(attributes.getValue("time")) * 1000); // NOI18N
        } catch (NumberFormatException exc) {
            // ignored
        }
        return l;
    }

    private String getClass(Attributes attributes) {
        return attributes.getValue("class"); // NOI18N
    }

    private String getName(Attributes attributes) {
        return attributes.getValue("name"); // NOI18N
    }

    private String getFile(Attributes attributes) {
        return getFile(attributes, null);
    }

    private String getFile(Attributes attributes, String defaultFile) {
        String f = attributes.getValue("file");
        if (f != null) {
            return f;
        }
        if (defaultFile != null) {
            return defaultFile;
        }
        return file;
    }

    private int getLine(Attributes attributes) {
        return getInt(attributes, "line"); // NOI18N
    }

    private int getInt(Attributes attributes, String name) {
        int i = -1;
        try {
            i = Integer.parseInt(attributes.getValue(name));
        } catch (NumberFormatException exc) {
            // ignored
        }
        return i;
    }

    // #200503
    private void finish() {
        givenTestSession.setTime(tmpTestSession.getTime());
        givenTestSession.setTests(tmpTestSession.getTests());
        for (TestSuiteVo testSuiteVo : tmpTestSession.getTestSuites()) {
            if (testSuiteVo.getPureTestCases().isEmpty()
                    && parentTestSuites.contains(testSuiteVo)) {
                // test suite that contains only other test suites and no tests => ignore it completely
                continue;
            }
            givenTestSession.addTestSuite(testSuiteVo);
        }
    }

}
