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
package org.netbeans.modules.java.editor.semantic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.editor.settings.EditorStyleConstants;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.modules.java.editor.base.semantic.ColoringAttributes;
import static org.netbeans.modules.java.editor.base.semantic.ColoringAttributes.*;

/**
 *
 * @author Jan Lahoda
 */
public final class ColoringManager {

    private static final Map<Set<ColoringAttributes>, String> type2Coloring;
    
    static {
        type2Coloring = new LinkedHashMap<Set<ColoringAttributes>, String>();
        
        put("mark-occurrences", MARK_OCCURRENCES);
        put("mod-type-parameter-use", TYPE_PARAMETER_USE);
        put("mod-type-parameter-declaration", TYPE_PARAMETER_DECLARATION);
        put("mod-enum-declaration", ENUM, DECLARATION);
        put("mod-annotation-type-declaration", ANNOTATION_TYPE, DECLARATION);
        put("mod-interface-declaration", INTERFACE, DECLARATION);
        put("mod-class-declaration", CLASS, DECLARATION);
        put("mod-constructor-declaration", CONSTRUCTOR, DECLARATION);
        put("mod-method-declaration", METHOD, DECLARATION);
        put("mod-parameter-declaration", PARAMETER, DECLARATION);
        put("mod-local-variable-declaration", LOCAL_VARIABLE, DECLARATION);
        put("mod-field-declaration", FIELD, DECLARATION);
        put("mod-enum", ENUM);
        put("mod-annotation-type", ANNOTATION_TYPE);
        put("mod-interface", INTERFACE);
        put("mod-class", CLASS);
        put("mod-constructor", CONSTRUCTOR);
        put("mod-method", METHOD);
        put("mod-parameter", PARAMETER);
        put("mod-local-variable", LOCAL_VARIABLE);
        put("mod-field", FIELD);
        put("mod-public", PUBLIC);
        put("mod-protected", PROTECTED);
        put("mod-package-private", PACKAGE_PRIVATE);
        put("mod-private", PRIVATE);
        put("mod-static", STATIC);
        put("mod-abstract", ABSTRACT);
        put("mod-deprecated", DEPRECATED);
        put("mod-undefined", UNDEFINED);
        put("mod-unused", UNUSED);
        put("javadoc-identifier", JAVADOC_IDENTIFIER);
    }
    
    private static void put(String coloring, ColoringAttributes... attributes) {
        Set<ColoringAttributes> attribs = EnumSet.copyOf(Arrays.asList(attributes));
        
        type2Coloring.put(attribs, coloring);
    }
    
    public static AttributeSet getColoringImpl(Coloring colorings) {
        FontColorSettings fcs = MimeLookup.getLookup(MimePath.get("text/x-java")).lookup(FontColorSettings.class);
        
        if (fcs == null) {
            //in tests:
            return AttributesUtilities.createImmutable();
        }
        
        assert fcs != null;
        
        List<AttributeSet> attribs = new LinkedList<AttributeSet>();
        
        EnumSet<ColoringAttributes> es = EnumSet.noneOf(ColoringAttributes.class);
        
        es.addAll(colorings);
        
        if (colorings.contains(UNUSED)) {
            attribs.add(AttributesUtilities.createImmutable(EditorStyleConstants.Tooltip, new UnusedTooltipResolver()));
            attribs.add(AttributesUtilities.createImmutable("unused-browseable", Boolean.TRUE));
        }
        
//        colorings = colorings.size() > 0 ? EnumSet.copyOf(colorings) : EnumSet.noneOf(ColoringAttributes.class);
        
        for (Entry<Set<ColoringAttributes>, String> attribs2Colorings : type2Coloring.entrySet()) {
//            System.err.println("type = " + type );
            if (es.containsAll(attribs2Colorings.getKey())) {
//                System.err.println("type2Coloring.get(type)=" + type2Coloring.get(type));
                String key = attribs2Colorings.getValue();
                
                es.removeAll(attribs2Colorings.getKey());
                
                if (key != null) {
                    AttributeSet colors = fcs.getTokenFontColors(key);
                    
                    if (colors == null) {
                        Logger.getLogger(ColoringManager.class.getName()).log(Level.SEVERE, "no colors for: {0}", key);
                        continue;
                    }
                    
                    attribs.add(adjustAttributes(colors));
                }
                
//                System.err.println("c = " + c );
            }
        }
        
        Collections.reverse(attribs);
        
//        System.err.println("c = " + c );
        AttributeSet result = AttributesUtilities.createComposite(attribs.toArray(new AttributeSet[0]));
        
        return result;
    }
    
    private static AttributeSet adjustAttributes(AttributeSet as) {
        Collection<Object> attrs = new LinkedList<Object>();
        
        for (Enumeration<?> e = as.getAttributeNames(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object value = as.getAttribute(key);
            
            if (value != Boolean.FALSE) {
                attrs.add(key);
                attrs.add(value);
            }
        }
        
        return AttributesUtilities.createImmutable(attrs.toArray());
    }
}
