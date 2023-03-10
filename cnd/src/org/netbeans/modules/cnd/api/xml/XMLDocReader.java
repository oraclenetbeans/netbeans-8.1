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
 * Software is Sun Microsystems, Inc. Portions Copyright 2005-2006 Sun
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

package org.netbeans.modules.cnd.api.xml;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.netbeans.modules.cnd.support.Interrupter;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;


/**
 * Drive the reading of and receive notification of the content of an
 * XML document.
 * <p>
 * While one can implement the {@link XMLDecoder} interface directly,
 * the recommended practice
 * is to define one or more specialized <code>XMLDecoder</code>s for the
 * expected top-level elements and register them using
 * {@link XMLDecoder#registerXMLDecoder} while leaving all the other 
 * <code>XMLDecoder</code> callbacks empty.
 */

abstract public class XMLDocReader extends XMLDecoder {

    /**
     * Set to true to get a trace of what's being read.
     */

    private static final boolean debug = false;	// echo SAX callbacks

    private String sourceName;			// remember for error messages

    // This probably SHOULD be per nested XMLDecoder!
    private String currentText = null;
    private String comment = null;

    public XMLDocReader() {
    }

    protected String getMasterComment() {
        return comment;
    }
       
    /**
     * Drive the reading of XML from the given InputStream.
     * <p>
     * This typically results in the callback of implemented 
     * {@link XMLDecoder} getting called, either directly or recursively
     * through an {@link XMLDecoder} registered at construction time.
     *
     * @param sourceName the name of the source of data used by error messages
     */
    public boolean read(InputStream inputStream, String sourceName) {
        return read(inputStream, sourceName, null);
    }

    public final boolean read(InputStream inputStream, String sourceName, Interrupter interrupter) {
	this.sourceName = sourceName;
	if (sourceName == null) {
            this.sourceName = getString("UNKNOWN_sourceName"); // NOI18N
        }

	SAXParserFactory spf = SAXParserFactory.newInstance();
	spf.setValidating(false);

	org.xml.sax.XMLReader xmlReader;
	try {
	    SAXParser saxParser = spf.newSAXParser();
	    xmlReader = saxParser.getXMLReader();
	} catch(Exception ex) {
	    ErrorManager.getDefault().notify(ex);
	    return false;
	}

	Parser parser = new Parser(interrupter);

	xmlReader.setContentHandler(parser);
	xmlReader.setEntityResolver(parser);
	xmlReader.setErrorHandler(new ErrHandler());
        try {
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", new CommentsParser()); //NOI18N
        } catch (SAXNotRecognizedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SAXNotSupportedException ex) {
            Exceptions.printStackTrace(ex);
        }

	String fmt = getString("MSG_Whilereading");	// NOI18N
	String whileMsg = MessageFormat.format(fmt, new Object[] {sourceName});

	try {
	    InputSource inputSource = new InputSource(inputStream);
	    xmlReader.parse(inputSource);

	} catch (SAXException ex) {

	    VersionException versionException = null;
	    if (ex.getException() instanceof VersionException) {
		versionException = (VersionException) ex.getException();
	    }

	    if (versionException != null) {
                if (versionException.showDetails()) {
                    String what = versionException.element();
                    int expectedVersion = versionException.expectedVersion();
                    int actualVersion = versionException.actualVersion();

                    fmt = getString("MSG_versionerror");	// NOI18N
                    String errmsg = whileMsg + MessageFormat.format(fmt,
                        new Object[] {what,
                                      "" + actualVersion, // NOI18N
                                      "" + expectedVersion}); // NOI18N
                    if (CndUtils.isStandalone()) {
                        System.err.println(errmsg);
                    } else {
                        NotifyDescriptor.Message msg = new NotifyDescriptor.
                            Message(errmsg, NotifyDescriptor.ERROR_MESSAGE);

                        DialogDisplayer.getDefault().notify(msg);
                    }
                }
	    } else {
                if (ex instanceof CancelledException) {
                    System.err.println("Canceled reading of "+sourceName);
                } else {
                    ErrorManager.getDefault().annotate(ex, whileMsg);
                    ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
                }
	    }
	    return false;

	} catch (IOException ex) {
            ErrorManager.getDefault().annotate(ex, whileMsg);
            ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
	    return false;

	} catch (Exception ex) {
	    // catchall
	    ErrorManager.getDefault().annotate(ex, whileMsg);
	    ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
	    return false;
	}
	return true;
    }

    
    private class CommentsParser implements LexicalHandler {

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            // Not used
        }

        @Override
        public void endDTD() throws SAXException {
            // Not used
        }

        @Override
        public void startEntity(String name) throws SAXException {
            // Not used
        }

        @Override
        public void endEntity(String name) throws SAXException {
            // Not used
        }

        @Override
        public void startCDATA() throws SAXException {
            // Not used
        }

        @Override
        public void endCDATA() throws SAXException {
            // Not used
        }

        @Override
        public void comment(char[] ch, int start, int length) throws SAXException {
            if (comment == null) {
                comment = new String(ch, start, length);
            }
        }
        
    }

    private class Parser implements ContentHandler, EntityResolver {
        private final Interrupter interrupter;

        private Parser(Interrupter interrupter) {
            this.interrupter = interrupter;
        }

        /**
	 * Set out own EntityResolver to return an "empty" stream. AFAIK this
	 * is to bypass DTD's and errors of this sort:
	 *
	 *	Warning: in nbrescurr:/<URL>, the nbrescurr URL protocol
	 *	has been deprecated as it assumes Filesystems == classpath.
	 *
	 * followed by IOExceptions
	 */

	// interface EntityResolver
        @Override
	public InputSource resolveEntity(String pubid, String sysid) throws SAXException, IOException {
	    if (debug) {
		System.out.println("SAX resolveEntity: " + pubid + " " + sysid); // NOI18N
	    }
	    byte[] empty = new byte[0];
	    return new InputSource(new java.io.ByteArrayInputStream(empty));
	}

	// interface ContentHandler
        @Override
	public void startDocument() throws SAXException {
	    if (debug) {
		System.out.println("SAX startDocument"); // NOI18N
	    }
	    try {
		start(null);
	    } catch (VersionException x) {
		throw new SAXException(x);
	    } 
	}

	// interface ContentHandler
        @Override
	public void endDocument() throws SAXException {
	    if (debug) {
		System.out.println("SAX endDocument"); // NOI18N
	    }
	    end();
	} 

	// interface ContentHandler
        @Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	    String s = new String(ch, start, length);
	    currentText = currentText + s;
	    if (debug) {
		s = s.trim();
		if (s.length() == 0) {
                    System.out.println("SAX characters[" + length + "]: " + "<trimmed>"); // NOI18N
                } else {
                    System.out.println("SAX characters[" + length + "]: " + s); // NOI18N
                }
	    }
	}


	// interface ContentHandler
        @Override
	public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {

	    if (debug) {
		System.out.println("SAX startElement: " + // NOI18N
		    uri + " " + localName + "/" + qName); // NOI18N
		for (int ax = 0; ax < atts.getLength(); ax++) {
		    String AlocalName = atts.getLocalName(ax);
		    String AqName = atts.getQName(ax);
		    String Avalue = atts.getValue(ax);
		    System.out.println("SAX\t" + AlocalName + "/" + AqName + "=" // NOI18N
				       + Avalue);
		}
	    }
            if (interrupter != null && interrupter.cancelled()) {
                throw new CancelledException();
            }
	    currentText = "";	// NOI18N
	    try {
		_startElement(qName, atts);
	    } catch (VersionException x) {
		throw new SAXException(x);
	    } 
	}

	// interface ContentHandler
        @Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
	    if (debug) {
		System.out.println("SAX endElement: " + uri + " " + localName + " " + // NOI18N
		    qName);
	    }
            if (interrupter != null && interrupter.cancelled()) {
                throw new CancelledException();
            }
	    _endElement(qName, currentText);
	}

	// interface ContentHandler
        @Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	// interface ContentHandler
        @Override
	public void endPrefixMapping(String prefix) throws SAXException {
	    if (debug) {
		System.out.println("SAX endPrefixMapping: " + prefix); // NOI18N
	    }
	}

	// interface ContentHandler
        @Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	    if (debug) {
		System.out.println("SAX ignorableWhitespace " + length); // NOI18N
	    }
	}

	// interface ContentHandler
        @Override
	public void processingInstruction(String target, String data) throws SAXException {
	    if (debug) {
		System.out.println("SAX processingInstruction: " + target + " " + // NOI18N
		    data);
	    }
	}

	// interface ContentHandler
        @Override
	public void setDocumentLocator(org.xml.sax.Locator locator) {
	    if (debug) {
		System.out.println("SAX setDocumentLocator"); // NOI18N
	    }
	}

	// interface ContentHandler
        @Override
	public void skippedEntity(String name) throws SAXException {
	    if (debug) {
		System.out.println("SAX skippedEntity: " + name); // NOI18N
	    }
	}

    }

    private final static class ErrHandler implements ErrorHandler {
	public ErrHandler() {
	} 

	private void annotate(SAXParseException ex) {
	    String fmt = getString("MSG_sax_error_location");	// NOI18N
	    String msg = MessageFormat.format(fmt, new Object[] {
			    ex.getSystemId(),
			    "" + ex.getLineNumber() // NOI18N
			});
	    ErrorManager.getDefault().annotate(ex,
					       ErrorManager.UNKNOWN,
					       msg,
					       null, null, null);
	}

        @Override
	public void fatalError(SAXParseException ex) throws SAXException {
	    annotate(ex);
	    throw ex;
	}

        @Override
	public void error(SAXParseException ex) throws SAXException {
	    annotate(ex);
	    throw ex;
	}

        @Override
	public void warning(SAXParseException ex) throws SAXException {
	    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
	}
    }

    private static String getString(String key) {
        return NbBundle.getMessage(XMLDocReader.class, key);
    }
}
