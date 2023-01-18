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

package org.netbeans.tax.dom;

import org.w3c.dom.*;
import org.netbeans.tax.*;

/**
 *
 * @author  Petr Kuzel
 */
class NamedNodeMapImpl implements NamedNodeMap {

    private final TreeNamedObjectMap peer;

    /** Creates a new instance of AttrImpl */
    public NamedNodeMapImpl(TreeNamedObjectMap peer) {
        this.peer = peer;
    }

    /** The number of nodes in this map. The range of valid child node indices
     * is <code>0</code> to <code>length-1</code> inclusive.
     *
     */
    public int getLength() {
        return peer.size();
    }
    
    /** Retrieves a node specified by name.
     * @param name The <code>nodeName</code> of a node to retrieve.
     * @return A <code>Node</code> (of any type) with the specified
     *   <code>nodeName</code>, or <code>null</code> if it does not identify
     *   any node in this map.
     *
     */
    public Node getNamedItem(String name) {
        return Wrapper.wrap((TreeObject)peer.get(name));
    }
    
    /** Retrieves a node specified by local name and namespace URI.
     * <br>Documents which do not support the "XML" feature will permit only
     * the DOM Level 1 calls for creating/setting elements and attributes.
     * Hence, if you specify a non-null namespace URI, these DOMs will never
     * find a matching node.
     * @param namespaceURI The namespace URI of the node to retrieve.
     * @param localName The local name of the node to retrieve.
     * @return A <code>Node</code> (of any type) with the specified local
     *   name and namespace URI, or <code>null</code> if they do not
     *   identify any node in this map.
     * @since DOM Level 2
     *
     */
    public Node getNamedItemNS(String namespaceURI, String localName) {
        throw new UOException();
    }
    
    /** Returns the <code>index</code>th item in the map. If <code>index</code>
     * is greater than or equal to the number of nodes in this map, this
     * returns <code>null</code>.
     * @param index Index into this map.
     * @return The node at the <code>index</code>th position in the map, or
     *   <code>null</code> if that is not a valid index.
     *
     */
    public Node item(int index) {
        return Wrapper.wrap((TreeObject)peer.get(index));
    }
    
    /** Removes a node specified by name. When this map contains the attributes
     * attached to an element, if the removed attribute is known to have a
     * default value, an attribute immediately appears containing the
     * default value as well as the corresponding namespace URI, local name,
     * and prefix when applicable.
     * @param name The <code>nodeName</code> of the node to remove.
     * @return The node removed from this map if a node with such a name
     *   exists.
     * @exception DOMException
     *   NOT_FOUND_ERR: Raised if there is no node named <code>name</code> in
     *   this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     *
     */
    public Node removeNamedItem(String name) throws DOMException {
        throw new ROException();
    }
    
    /** Removes a node specified by local name and namespace URI. A removed
     * attribute may be known to have a default value when this map contains
     * the attributes attached to an element, as returned by the attributes
     * attribute of the <code>Node</code> interface. If so, an attribute
     * immediately appears containing the default value as well as the
     * corresponding namespace URI, local name, and prefix when applicable.
     * <br>Documents which do not support the "XML" feature will permit only
     * the DOM Level 1 calls for creating/setting elements and attributes.
     * Hence, if you specify a non-null namespace URI, these DOMs will never
     * find a matching node.
     * @param namespaceURI The namespace URI of the node to remove.
     * @param localName The local name of the node to remove.
     * @return The node removed from this map if a node with such a local
     *   name and namespace URI exists.
     * @exception DOMException
     *   NOT_FOUND_ERR: Raised if there is no node with the specified
     *   <code>namespaceURI</code> and <code>localName</code> in this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     * @since DOM Level 2
     *
     */
    public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
        throw new ROException();
    }
    
    /** Adds a node using its <code>nodeName</code> attribute. If a node with
     * that name is already present in this map, it is replaced by the new
     * one.
     * <br>As the <code>nodeName</code> attribute is used to derive the name
     * which the node must be stored under, multiple nodes of certain types
     * (those that have a "special" string value) cannot be stored as the
     * names would clash. This is seen as preferable to allowing nodes to be
     * aliased.
     * @param arg A node to store in this map. The node will later be
     *   accessible using the value of its <code>nodeName</code> attribute.
     * @return If the new <code>Node</code> replaces an existing node the
     *   replaced <code>Node</code> is returned, otherwise <code>null</code>
     *   is returned.
     * @exception DOMException
     *   WRONG_DOCUMENT_ERR: Raised if <code>arg</code> was created from a
     *   different document than the one that created this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     *   <br>INUSE_ATTRIBUTE_ERR: Raised if <code>arg</code> is an
     *   <code>Attr</code> that is already an attribute of another
     *   <code>Element</code> object. The DOM user must explicitly clone
     *   <code>Attr</code> nodes to re-use them in other elements.
     *   <br>HIERARCHY_REQUEST_ERR: Raised if an attempt is made to add a node
     *   doesn't belong in this NamedNodeMap. Examples would include trying
     *   to insert something other than an Attr node into an Element's map
     *   of attributes, or a non-Entity node into the DocumentType's map of
     *   Entities.
     *
     */
    public Node setNamedItem(Node arg) throws DOMException {
        throw new ROException();
    }
    
    /** Adds a node using its <code>namespaceURI</code> and
     * <code>localName</code>. If a node with that namespace URI and that
     * local name is already present in this map, it is replaced by the new
     * one.
     * @param arg A node to store in this map. The node will later be
     *   accessible using the value of its <code>namespaceURI</code> and
     *   <code>localName</code> attributes.
     * @return If the new <code>Node</code> replaces an existing node the
     *   replaced <code>Node</code> is returned, otherwise <code>null</code>
     *   is returned.
     * @exception DOMException
     *   WRONG_DOCUMENT_ERR: Raised if <code>arg</code> was created from a
     *   different document than the one that created this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     *   <br>INUSE_ATTRIBUTE_ERR: Raised if <code>arg</code> is an
     *   <code>Attr</code> that is already an attribute of another
     *   <code>Element</code> object. The DOM user must explicitly clone
     *   <code>Attr</code> nodes to re-use them in other elements.
     *   <br>HIERARCHY_REQUEST_ERR: Raised if an attempt is made to add a node
     *   doesn't belong in this NamedNodeMap. Examples would include trying
     *   to insert something other than an Attr node into an Element's map
     *   of attributes, or a non-Entity node into the DocumentType's map of
     *   Entities.
     *   <br>NOT_SUPPORTED_ERR: Always thrown if the current document does not
     *   support the <code>"XML"</code> feature, since namespaces were
     *   defined by XML.
     * @since DOM Level 2
     *
     */
    public Node setNamedItemNS(Node arg) throws DOMException {
        throw new ROException();
    }
    
}
