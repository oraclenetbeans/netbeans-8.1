/*
 * SimpleConstraint.java
 *
 * Created on January 25, 2004, 5:52 PM
 */

package org.netbeans.actions.simple;

import java.util.Arrays;
import java.util.Map;
import org.xml.sax.SAXException;

/**
 *
 * @author  Tim Boudreau
 */
class SimpleConstraint {
    private String name;
    private SimpleKey[] includeKeys;
    private SimpleKey[] excludeKeys;
    private boolean enabledType;
    
    /** Creates a new instance of SimpleConstraint */
    public SimpleConstraint(String name, SimpleKey[] includeKeys, SimpleKey[] excludeKeys, boolean enabledType) throws SAXException {
        this.name = name;
        this.includeKeys = includeKeys;
        this.excludeKeys = excludeKeys;
        this.enabledType = enabledType;
        if (name == null) {
            throw new SAXException ("Name may not be null");
        }
        if (includeKeys.length == 0 && excludeKeys.length ==0) {
            throw new SAXException ("Constraint has no keys");
        }
        System.err.println("Constraint: " + name + " includeKeys " + Arrays.asList(includeKeys) + " excludeKeys: " + Arrays.asList(excludeKeys) + " enabledType: " + enabledType);
    }
    
    public boolean isEnabledType() {
        return enabledType;
    }
    
    public boolean test (Map context) {
        boolean result = true;
//        System.err.println("Test constraint " + name);
        for (int i=0; i < excludeKeys.length; i++) {
            result &= !context.containsKey (excludeKeys[i]);
//            System.err.println(" check must not contain " + excludeKeys[i]);
            if (result && excludeKeys[i].mustTest()) {
                result &= excludeKeys[i].test(context);
//                System.err.println("    Must test: " + result);
            }
            if (!result) {
//                System.err.println(" FOUND AN INCLUDED KEY IN EXCLUSION SET FOR " + name + " returning false");
                return result;
            }
        }
        for (int i=0; i < includeKeys.length; i++) {
            result &= context.containsKey (includeKeys[i]);
//            System.err.println(" check must contain " + includeKeys[i] + " contained? " + result);
            if (result && includeKeys[i].mustTest()) {
                result &= includeKeys[i].test(context);
//                System.err.println("  must test it - got " + result);
            }
            if (!result) {
                return result;
            }
        }
        return result;
    }
    
    public String getName() {
        return name;
    }
    
    public String toString() {
        return getName();
    }
    
    public int hashCode() {
        return getName().hashCode();
    }
    
    public boolean equals (Object o) {
        boolean result = false;
        if (o.getClass() == SimpleConstraint.class) {
            result = o.toString().equals(toString());
        }
        return result;
    }
}
