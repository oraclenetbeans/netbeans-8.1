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
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package org.netbeans.lib.uihandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import org.netbeans.junit.NbTestCase;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.io.ReaderInputStream;

/**
 *
 * @author Jindrich Sedek
 */
public class LogFormatterTest extends NbTestCase {
    
    public LogFormatterTest(String testName) {
        super(testName);
    }
    
    public void testFormat() throws IOException {
        LogRecord rec = new LogRecord(Level.SEVERE, "PROBLEM");
        Throwable thrown = new NullPointerException("TESTING");
        thrown.initCause(new AssertionError("CAUSE PROBLEM"));
        rec.setThrown(thrown);
        String result = new LogFormatter().format(rec);
        assertTrue(result.contains("java.lang.NullPointerException: TESTING"));
        assertTrue(result.contains("<level>SEVERE</level>"));
        assertTrue(result.contains("<method>testFormat</method>"));
        assertTrue(result.contains("<message>java.lang.AssertionError: CAUSE PROBLEM</message>"));
        assertTrue(result.contains("<more>"));
        assertTrue(result.contains("</more>"));
        assertTrue(result.contains(" <class>junit.framework.TestCase</class>"));
        assertTrue(result.contains("<class>sun.reflect.NativeMethodAccessorImpl</class>"));
        assertFalse(result.contains("<more>80</more>"));
    }
        
    
    public void testEasy() throws IOException {
        Throwable thrown = new NullPointerException("TESTING");
        thrown.initCause(new AssertionError("CAUSE PROBLEM"));
        formatAndScan(thrown);
    }
    
    public void testManyCausesFormat() throws IOException{
        try{
            generateIOException();
        }catch(IOException exc){
            formatAndScan(exc);
        }
    }
    
    public void testDontPrintLocalizedMessage() throws IOException{
        LogRecord log = new LogRecord(Level.INFO, "test_msg");
        log.setResourceBundleName("org.netbeans.lib.uihandler.TestBundle");
        log.setResourceBundle(ResourceBundle.getBundle("org.netbeans.lib.uihandler.TestBundle"));
        log.setParameters(new Object[] { new Integer(1), "Ahoj" });
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        LogRecords.write(os, log);
        assertFalse("no localized message is printed" + os.toString(), os.toString().contains(" and "));
        assertTrue("key", os.toString().contains("<key>test_msg</key>"));
        assertTrue("no localized message", os.toString().contains("<message>test_msg</message>"));
    }

    /**
     * test whether the result of LogFormatter is the same as XMLFormatter 
     * if there is no nested exception
     */
    public void testXMLFormatterDifference(){
        LogRecord rec = new LogRecord(Level.SEVERE, "PROBLEM");
        LogFormatter logFormatter = new LogFormatter();
        XMLFormatter xmlFormatter = new XMLFormatter();
        String logResult = logFormatter.format(rec);
        String xmlResult = xmlFormatter.format(rec);
        assertEquals("WITHOUT THROWABLE", xmlResult, logResult);
        rec.setThrown(new NullPointerException("TESTING EXCEPTION"));
        rec.setResourceBundleName("MUJ BUNDLE");
        logResult = logFormatter.format(rec);
        //remove file names
        logResult = logResult.replaceAll("      <file>.*</file>\n", "");
        xmlResult = xmlFormatter.format(rec);
        assertEquals("WITH THROWABLE", xmlResult, logResult);
    }
    
    private void formatAndScan(Throwable thr) throws IOException{
        ByteArrayOutputStream oStream = new ByteArrayOutputStream(1000);
        LogRecord rec = new LogRecord(Level.SEVERE, "PROBLEM");
        rec.setThrown(thr);
        LogRecords.write(oStream, rec);//write to stream
        ByteArrayInputStream iStream = new ByteArrayInputStream(oStream.toByteArray());
        Formatter formatter = new LogFormatter();
        LogRecord readFromStream = new TestHandler(iStream).read();// read from stream
        //read by handler equals the writen by formatter
        assertEquals(formatter.format(readFromStream), formatter.format(rec));    
        oStream.reset();
        thr.printStackTrace(new PrintStream(oStream));
        String writen = oStream.toString();
        oStream.reset();
        rec.getThrown().printStackTrace(new PrintStream(oStream));
        String read = oStream.toString();
        assertEquals(writen, read);//both stacktraces are the same        
    }
    
    private void generateIOException()throws IOException{
        try{
            generateSQL();
        }catch(SQLException error){
            IOException except = new IOException("IO EXCEPTION");
            except.initCause(error);
            throw except;
        }
    }
            
    private void generateSQL() throws SQLException{
        try{
            generateClassNotFoundException();
        }catch(ClassNotFoundException exception){
            SQLException except = new SQLException("SQL TESTING EXCEPTION");
            except.initCause(exception);
            throw except;
        }
    }
    
    private void generateClassNotFoundException() throws ClassNotFoundException{
        java.lang.Class.forName("unknown name");
    }                  
    
    public void testFormatterDoesNotIncludeHashOnButton() throws ClassNotFoundException {
        LogRecord r = new LogRecord(Level.INFO, "BUTTON");
        r.setParameters(new Object[] { new JButton("kuk") });
        Formatter formatter = new LogFormatter();
        String s = formatter.format(r);
        assertEquals("No @\n" + s, -1, s.indexOf("@"));
        if (s.indexOf("kuk") == -1) {
            fail("kuk should be there:\n" + s);
        }
    }
    public void testFormatterDoesNotIncludeHashOnActions() throws ClassNotFoundException {
        LogRecord r = new LogRecord(Level.INFO, "ACTION");
        SA sa = SA.get(SA.class);
        r.setParameters(new Object[] { sa });
        Formatter formatter = new LogFormatter();
        String s = formatter.format(r);
        assertEquals("No @\n" + s, -1, s.indexOf("@"));
        if (s.indexOf("SomeName") == -1) {
            fail("SomeName should be there:\n" + s);
        }
        if (s.indexOf("LogFormatterTest$SA") == -1) {
            fail("LogFormatterTest$SA should be there:\n" + s);
        }
    }
    public void testFormatterDoesNotIncludeHashOnActionsClone() throws ClassNotFoundException {
        LogRecord r = new LogRecord(Level.INFO, "ACTION_CLONE");
        SA sa = SA.get(SA.class);
        r.setParameters(new Object[] { sa.createContextAwareInstance(Lookup.EMPTY) });
        Formatter formatter = new LogFormatter();
        String s = formatter.format(r);
        assertEquals("No @\n" + s, -1, s.indexOf("@"));
        if (s.indexOf("SomeName") == -1) {
            fail("SomeName should be there:\n" + s);
        }
        if (s.indexOf("LogFormatterTest$SA") == -1) {
            fail("LogFormatterTest$SA should be there:\n" + s);
        }
    }
    public void testFormatterDoesNotIncludeHashOnMenu() throws ClassNotFoundException {
        LogRecord r = new LogRecord(Level.INFO, "MENU");
        SA sa = SA.get(SA.class);
        r.setParameters(new Object[] { new JMenuItem(sa) });
        Formatter formatter = new LogFormatter();
        String s = formatter.format(r);
        assertEquals("No @\n" + s, -1, s.indexOf("@"));
        if (s.indexOf("SomeName") == -1) {
            fail("SomeName should be there:\n" + s);
        }
        if (s.indexOf("LogFormatterTest$SA") == -1) {
            fail("LogFormatterTest$SA should be there:\n" + s);
        }
    }
    public void testFormatterDoesNotIncludeHashOnEditor() throws ClassNotFoundException {
        LogRecord r = new LogRecord(Level.INFO, "EDIT");
        JEditorPane ep = new javax.swing.JEditorPane();
        ep.setName("SomeName");
        r.setParameters(new Object[] { ep });
        Formatter formatter = new LogFormatter();
        String s = formatter.format(r);
        assertEquals("No @\n" + s, -1, s.indexOf("@"));
        if (s.indexOf("SomeName") == -1) {
            fail("SomeName should be there:\n" + s);
        }
    }
    
    public void testUnknownLevels() throws IOException {
        TestLevel level = new TestLevel("WARN", 233);
        LogRecord r = new LogRecord(level, "Custom level test");
        Formatter formatter = new LogFormatter();
        String s = formatter.format(r);
        cleanKnownLevels();
        final LogRecord[] rPtr = new LogRecord[] { null };
        Handler h = new Handler() {
            @Override
            public void publish(LogRecord record) {
                rPtr[0] = record;
            }
            @Override
            public void flush() {}
            @Override
            public void close() throws SecurityException {}
        };
        LogRecords.scan(new ReaderInputStream(new StringReader(s)), h);
        assertEquals("level", r.getLevel(), rPtr[0].getLevel());
    }
    
    private static void cleanKnownLevels() {
        try {
            Class knownLevelClass = Class.forName(Level.class.getName()+"$KnownLevel");
            java.lang.reflect.Field nameToLevelsField = knownLevelClass.getDeclaredField("nameToLevels");
            nameToLevelsField.setAccessible(true);
            Map nameToLevels = (Map) nameToLevelsField.get(null);
            nameToLevels.clear();
            java.lang.reflect.Field intToLevelsField = knownLevelClass.getDeclaredField("intToLevels");
            intToLevelsField.setAccessible(true);
            Map intToLevels = (Map) intToLevelsField.get(null);
            intToLevels.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static class SA extends CallbackSystemAction {

        public String getName() {
            return "SomeName";
        }

        public HelpCtx getHelpCtx() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private static class TestLevel extends Level {
        
        public TestLevel(String name, int value) {
            super(name, value);
        }
        
    }
}


