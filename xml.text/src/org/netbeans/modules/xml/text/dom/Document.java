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

package org.netbeans.modules.xml.text.dom;

import org.netbeans.modules.xml.spi.dom.*;

public class Document extends AbstractNode implements org.w3c.dom.Document {

    SyntaxElement syntax;

    Document(SyntaxElement element) {
        syntax = element;
    }

    public org.w3c.dom.Attr createAttribute(String str) throws org.w3c.dom.DOMException {
        return null;
    }

    public org.w3c.dom.Element getElementById(String str) {
        return null;
    }
    
    public String getVersion() {
        throw new UOException();
    }
    
    public org.w3c.dom.Element createElement(String str) throws org.w3c.dom.DOMException {
        return null;
    }
    
    public boolean getStrictErrorChecking() {
        throw new UOException();
    }
    
    public org.w3c.dom.DOMImplementation getImplementation() {
        return new DOMImplementationImpl();
    }
    
    public org.w3c.dom.Element createElementNS(String str, String str1) throws org.w3c.dom.DOMException {
        throw new UOException();
    }
    
    public org.w3c.dom.DocumentFragment createDocumentFragment() {
        return null;
    }
    
    public org.w3c.dom.NodeList getElementsByTagNameNS(String str, String str1) {
        return NodeListImpl.EMPTY;  //???
    }
    
    public void setVersion(String str) {
        throw new UOException();
    }
    
    public org.w3c.dom.Attr createAttributeNS(String str, String str1) throws org.w3c.dom.DOMException {
        return null;
    }
    
    public void setStrictErrorChecking(boolean param) {
        throw new UOException();
    }
    
    public void setEncoding(String str) {
        throw new UOException();
    }
    
    public org.w3c.dom.ProcessingInstruction createProcessingInstruction(String str, String str1) throws org.w3c.dom.DOMException {
        return null;
    }
    
    public org.w3c.dom.NodeList getElementsByTagName(String str) {
        return NodeListImpl.EMPTY;  //???
    }
    
    public org.w3c.dom.Element getDocumentElement() {
        return null;  //!!! parse for it
    }
    
    public org.w3c.dom.DocumentType getDoctype() {
//        try {
            //SyntaxElement e = syntax.support.getElementChain(0);
            //!!! locate declaration and return wrapper
//            return new DocumentTypeImpl(null, null, 0);
//        } catch (BadLocationException ex) {
            return null;
//        }
    }
    
    public org.w3c.dom.CDATASection createCDATASection(String str) throws org.w3c.dom.DOMException {
        return null;
    }
    
    public org.w3c.dom.EntityReference createEntityReference(String str) throws org.w3c.dom.DOMException {
        return null;
    }
    
    public boolean getStandalone() {
        throw new UOException();
    }
    
    public short getNodeType() {
        return org.w3c.dom.Node.DOCUMENT_NODE;
    }
    
    public org.w3c.dom.Node adoptNode(org.w3c.dom.Node node) throws org.w3c.dom.DOMException {
        throw new ROException();
    }
    
    public org.w3c.dom.Text createTextNode(String str) {
        return null;
    }
    
    public String getEncoding() {
        throw new UOException();
    }
    
    public org.w3c.dom.Comment createComment(String str) {
        return null;
    }
    
    public void setStandalone(boolean param) {
        throw new UOException();
    }
    
    public org.w3c.dom.Node importNode(org.w3c.dom.Node node, boolean param) throws org.w3c.dom.DOMException {
        throw new ROException();
    }


    /**
     *
     * @author  Petr Kuzel
     */
    private class DOMImplementationImpl implements org.w3c.dom.DOMImplementation {

        /** Creates a new instance of DOMImplementationImpl */
        public DOMImplementationImpl() {
        }

        /** Creates a DOM Document object of the specified type with its document
         * element.
         * @param namespaceURI The namespace URI of the document element to
         *   create.
         * @param qualifiedName The qualified name of the document element to be
         *   created.
         * @param doctype The type of document to be created or <code>null</code>.
         *   When <code>doctype</code> is not <code>null</code>, its
         *   <code>Node.ownerDocument</code> attribute is set to the document
         *   being created.
         * @return A new <code>Document</code> object.
         * @exception DOMException
         *   INVALID_CHARACTER_ERR: Raised if the specified qualified name
         *   contains an illegal character.
         *   <br>NAMESPACE_ERR: Raised if the <code>qualifiedName</code> is
         *   malformed, if the <code>qualifiedName</code> has a prefix and the
         *   <code>namespaceURI</code> is <code>null</code>, or if the
         *   <code>qualifiedName</code> has a prefix that is "xml" and the
         *   <code>namespaceURI</code> is different from "
         *   http://www.w3.org/XML/1998/namespace" , or if the DOM
         *   implementation does not support the <code>"XML"</code> feature but
         *   a non-null namespace URI was provided, since namespaces were
         *   defined by XML.
         *   <br>WRONG_DOCUMENT_ERR: Raised if <code>doctype</code> has already
         *   been used with a different document or was created from a different
         *   implementation.
         *   <br>NOT_SUPPORTED_ERR: May be raised by DOM implementations which do
         *   not support the "XML" feature, if they choose not to support this
         *   method. Other features introduced in the future, by the DOM WG or
         *   in extensions defined by other groups, may also demand support for
         *   this method; please consult the definition of the feature to see if
         *   it requires this method.
         * @since DOM Level 2
         *
         */
        public Document createDocument(String namespaceURI, String qualifiedName,
                org.w3c.dom.DocumentType doctype) throws org.w3c.dom.DOMException {
            throw new ROException();
        }

        /** Creates an empty <code>DocumentType</code> node. Entity declarations
         * and notations are not made available. Entity reference expansions and
         * default attribute additions do not occur. It is expected that a
         * future version of the DOM will provide a way for populating a
         * <code>DocumentType</code>.
         * @param qualifiedName The qualified name of the document type to be
         *   created.
         * @param publicId The external subset public identifier.
         * @param systemId The external subset system identifier.
         * @return A new <code>DocumentType</code> node with
         *   <code>Node.ownerDocument</code> set to <code>null</code>.
         * @exception DOMException
         *   INVALID_CHARACTER_ERR: Raised if the specified qualified name
         *   contains an illegal character.
         *   <br>NAMESPACE_ERR: Raised if the <code>qualifiedName</code> is
         *   malformed.
         *   <br>NOT_SUPPORTED_ERR: May be raised by DOM implementations which do
         *   not support the <code>"XML"</code> feature, if they choose not to
         *   support this method. Other features introduced in the future, by
         *   the DOM WG or in extensions defined by other groups, may also
         *   demand support for this method; please consult the definition of
         *   the feature to see if it requires this method.
         * @since DOM Level 2
         *
         */
        public DocumentType createDocumentType(String qualifiedName, String publicId,
                String systemId) throws org.w3c.dom.DOMException {
            throw new ROException();
        }

        /** Test if the DOM implementation implements a specific feature.
         * @param feature The name of the feature to test (case-insensitive). The
         *   values used by DOM features are defined throughout the DOM Level 2
         *   specifications and listed in the  section. The name must be an XML
         *   name. To avoid possible conflicts, as a convention, names referring
         *   to features defined outside the DOM specification should be made
         *   unique.
         * @param version This is the version number of the feature to test. In
         *   Level 2, the string can be either "2.0" or "1.0". If the version is
         *   not specified, supporting any version of the feature causes the
         *   method to return <code>true</code>.
         * @return <code>true</code> if the feature is implemented in the
         *   specified version, <code>false</code> otherwise.
         *
         */
        public boolean hasFeature(String feature, String version) {
            return "1.0".equals(version);
        }

        public Object getFeature (String a, String b) {
            throw new UOException ();
        }

    }

}

