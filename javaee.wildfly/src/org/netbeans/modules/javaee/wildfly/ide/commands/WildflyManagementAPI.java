/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javaee.wildfly.ide.commands;

import static org.netbeans.modules.javaee.wildfly.ide.commands.Constants.DEPLOYMENT;
import static org.netbeans.modules.javaee.wildfly.ide.commands.Constants.UNDEFINED;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;
import org.netbeans.modules.javaee.wildfly.WildflyDeploymentFactory;
import org.netbeans.modules.javaee.wildfly.ide.ui.WildflyPluginUtils;
import org.netbeans.modules.javaee.wildfly.ide.ui.WildflyPluginUtils.Version;

/**
 *
 * @author Emmanuel Hugonnet (ehsavoie) <ehsavoie@netbeans.org>
 */
public class WildflyManagementAPI {
    
    private static final String SASL_DISALLOWED_MECHANISMS = "SASL_DISALLOWED_MECHANISMS";
    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";

    private static final Map<String, String> DISABLED_LOCAL_AUTH = Collections.singletonMap(SASL_DISALLOWED_MECHANISMS, JBOSS_LOCAL_USER);
    private static final Map<String, String> ENABLED_LOCAL_AUTH = Collections.emptyMap();
    private static final int TIMEOUT = 1000;

    static Object createClient(WildflyDeploymentFactory.WildFlyClassLoader cl, Version version, final String serverAddress, final int serverPort,
            final CallbackHandler handler) throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, NoSuchAlgorithmException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.ModelControllerClient$Factory"); // NOI18N
        if (version.compareTo(WildflyPluginUtils.WILDFLY_9_0_0) >= 0) {
            Method method = clazz.getDeclaredMethod("create", String.class, int.class, CallbackHandler.class, SSLContext.class, int.class, Map.class);
            return method.invoke(null, serverAddress, serverPort, handler, SSLContext.getDefault(), TIMEOUT, ENABLED_LOCAL_AUTH);
        }
        Method method = clazz.getDeclaredMethod("create", String.class, int.class, CallbackHandler.class, SSLContext.class, int.class);
        return method.invoke(null, serverAddress, serverPort, handler, SSLContext.getDefault(), TIMEOUT);
    }

    static void closeClient(WildflyDeploymentFactory.WildFlyClassLoader cl, Object client) throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Method method = client.getClass().getMethod("close", new Class[]{});
        method.invoke(client, (Object[]) null);
    }

    // ModelNode
    static Object createDeploymentPathAddressAsModelNode(WildflyDeploymentFactory.WildFlyClassLoader cl, String name)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class paClazz = cl.loadClass("org.jboss.as.controller.PathAddress"); // NOI18N
        Class peClazz = cl.loadClass("org.jboss.as.controller.PathElement"); // NOI18N

        Method peFactory = peClazz.getDeclaredMethod("pathElement",// NOI18N
                name != null ? new Class[]{String.class, String.class} : new Class[]{String.class});
        Object pe = peFactory.invoke(null,
                name != null ? new Object[]{DEPLOYMENT, name} : new Object[]{DEPLOYMENT});// NOI18N

        Object array = Array.newInstance(peClazz, 1);
        Array.set(array, 0, pe);
        Method paFactory = paClazz.getDeclaredMethod("pathAddress", array.getClass()); // NOI18N
        Object pa = paFactory.invoke(null, array);

        Method toModelNode = pa.getClass().getMethod("toModelNode", (Class<?>[]) null); // NOI18N
        return toModelNode.invoke(pa, (Object[]) null);
    }

    // ModelNode
    static Object createPathAddressAsModelNode(WildflyDeploymentFactory.WildFlyClassLoader cl, LinkedHashMap<Object, Object> elements)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class paClazz = cl.loadClass("org.jboss.as.controller.PathAddress"); // NOI18N
        Class peClazz = cl.loadClass("org.jboss.as.controller.PathElement"); // NOI18N

        Method peFactory = peClazz.getDeclaredMethod("pathElement", new Class[]{String.class, String.class});
        Object array = Array.newInstance(peClazz, elements.size());
        int i = 0;
        for (Map.Entry<Object, Object> entry : elements.entrySet()) {
            Array.set(array, i, peFactory.invoke(null, new Object[]{entry.getKey(), entry.getValue()}));
            i++;
        }

        Method paFactory = paClazz.getDeclaredMethod("pathAddress", array.getClass()); // NOI18N
        Object pa = paFactory.invoke(null, array);

        Method toModelNode = pa.getClass().getMethod("toModelNode", (Class<?>[]) null); // NOI18N
        return toModelNode.invoke(pa, (Object[]) null);
    }

    // ModelNode
    static Object createOperation(WildflyDeploymentFactory.WildFlyClassLoader cl, Object name, Object modelNode)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.helpers.Operations"); // NOI18N
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = clazz.getDeclaredMethod("createOperation", new Class[]{String.class, modelClazz});
        return method.invoke(null, name, modelNode);
    }

    // ModelNode
    static Object createReadResourceOperation(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, boolean recursive)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.helpers.Operations"); // NOI18N
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = clazz.getDeclaredMethod("createReadResourceOperation", new Class[]{modelClazz, boolean.class});
        return method.invoke(null, modelNode, recursive);
    }

    // ModelNode
    static Object createRemoveOperation(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.helpers.Operations"); // NOI18N
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = clazz.getDeclaredMethod("createRemoveOperation", new Class[]{modelClazz});
        return method.invoke(null, modelNode);
    }

    // ModelNode
    static Object createAddOperation(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.helpers.Operations"); // NOI18N
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = clazz.getDeclaredMethod("createAddOperation", new Class[]{modelClazz});
        return method.invoke(null, modelNode);
    }

    // ModelNode
    static Object readResult(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.helpers.Operations"); // NOI18N
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = clazz.getDeclaredMethod("readResult", new Class[]{modelClazz});
        return method.invoke(null, modelNode);
    }

    // ModelNode
    static Object getModelNodeChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, Object name) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("get", String.class);
        return method.invoke(modelNode, name);
    }

    // ModelNode
    static Object getModelNodeChildAtIndex(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, int index) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("get", int.class);
        return method.invoke(modelNode, index);
    }

    // ModelNode
    static Object getModelNodeChildAtPath(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, Object[] path) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("get", String[].class);
        Object array = Array.newInstance(String.class, path.length);
        for (int i = 0; i < path.length; i++) {
            Array.set(array, i, path[i]);
        }
        return method.invoke(modelNode, array);
    }

    // ModelNode
    static boolean modelNodeHasChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, String child) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("has", String.class);
        return (Boolean) method.invoke(modelNode, child);
    }

    // ModelNode
    static boolean modelNodeHasDefinedChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, String child) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("hasDefined", String.class);
        return (Boolean) method.invoke(modelNode, child);
    }

    // ModelNode
    static Object createModelNode(WildflyDeploymentFactory.WildFlyClassLoader cl) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        return modelClazz.newInstance();
    }

    // ModelNode
    static Object setModelNodeChildString(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, Object value) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        assert value != null;
        Method method = modelNode.getClass().getMethod("set", String.class);
        return method.invoke(modelNode, value);
    }

    // ModelNode
    static Object setModelNodeChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, Object value) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        assert value != null;
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = modelNode.getClass().getMethod("set", modelClazz);
        return method.invoke(modelNode, value);
    }

    // ModelNode
    static Object setModelNodeChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, int value) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("set", int.class);
        return method.invoke(modelNode, value);
    }

    // ModelNode
    static Object setModelNodeChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, boolean value) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("set", boolean.class);
        return method.invoke(modelNode, value);
    }

    // ModelNode
    static Object setModelNodeChildEmptyList(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Method method = modelNode.getClass().getMethod("setEmptyList", (Class<?>[]) null);
        return method.invoke(modelNode, (Object[]) null);
    }

    // ModelNode
    static Object setModelNodeChildBytes(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, byte[] value) throws IllegalAccessException,
            ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Method method = modelNode.getClass().getMethod("set", byte[].class);
        return method.invoke(modelNode, value);
    }

    // ModelNode
    static Object addModelNodeChild(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode, Object toAddModelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = modelNode.getClass().getMethod("add", modelClazz);
        return method.invoke(modelNode, toAddModelNode);
    }

    static boolean modelNodeIsDefined(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("isDefined", (Class<?>[]) null);
        return (Boolean) method.invoke(modelNode, (Object[]) null);
    }

    static String modelNodeAsString(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asString", (Class<?>[]) null);
        return (String) method.invoke(modelNode, (Object[]) null);
    }

    static String modelNodeAsPropertyForName(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asProperty", (Class<?>[]) null);
        Object property = method.invoke(modelNode, (Object[]) null);
        return getPropertyName(cl, property);
    }

    static Object modelNodeAsPropertyForValue(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asProperty", (Class<?>[]) null);
        Object property = method.invoke(modelNode, (Object[]) null);
        return getPropertyValue(cl, property);
    }

    static String getPropertyName(WildflyDeploymentFactory.WildFlyClassLoader cl, Object property) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = property.getClass().getMethod("getName", (Class<?>[]) null);
        return (String) method.invoke(property, (Object[]) null);
    }

    static Object getPropertyValue(WildflyDeploymentFactory.WildFlyClassLoader cl, Object property) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = property.getClass().getMethod("getValue", (Class<?>[]) null);
        return method.invoke(property, (Object[]) null);
    }


    // List<ModelNode>
    static List modelNodeAsList(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asList", (Class<?>[]) null);
        return (List) method.invoke(modelNode, (Object[]) null);
    }

    static List modelNodeAsPropertyList(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asPropertyList", (Class<?>[]) null);
        return (List) method.invoke(modelNode, (Object[]) null);
    }

    static boolean modelNodeAsBoolean(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asBoolean", (Class<?>[]) null);
        return (boolean) method.invoke(modelNode, (Object[]) null);
    }

    static int modelNodeAsInt(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Method method = modelNode.getClass().getMethod("asInt", (Class<?>[]) null);
        return (int) method.invoke(modelNode, (Object[]) null);
    }

    static boolean isSuccessfulOutcome(WildflyDeploymentFactory.WildFlyClassLoader cl, Object modelNode) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class clazz = cl.loadClass("org.jboss.as.controller.client.helpers.Operations"); // NOI18N
        Class modelClazz = cl.loadClass("org.jboss.dmr.ModelNode"); // NOI18N
        Method method = clazz.getDeclaredMethod("isSuccessfulOutcome", modelClazz);
        return (Boolean) method.invoke(null, modelNode);
    }

    static boolean isDefined(String value) {
        return value != null && !value.isEmpty() && !UNDEFINED.equalsIgnoreCase(value);
    }
}
