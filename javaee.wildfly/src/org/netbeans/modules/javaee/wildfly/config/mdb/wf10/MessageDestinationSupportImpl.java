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
package org.netbeans.modules.javaee.wildfly.config.mdb.wf10;


import org.netbeans.modules.javaee.wildfly.config.WildflyMessageDestination;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.j2ee.deployment.common.api.ConfigurationException;
import org.netbeans.modules.j2ee.deployment.common.api.MessageDestination;
import org.netbeans.modules.j2ee.deployment.common.api.MessageDestination.Type;
import org.netbeans.modules.javaee.wildfly.WildflyDeploymentManager;
import org.netbeans.modules.javaee.wildfly.config.ResourceConfigurationHelper;
import org.netbeans.modules.javaee.wildfly.config.gen.wf10.JmsTopicType;
import org.netbeans.modules.javaee.wildfly.config.gen.wf10.JmsQueueType;
import org.netbeans.modules.javaee.wildfly.config.gen.wf10.MessagingDeployment;
import org.netbeans.modules.javaee.wildfly.config.gen.wf10.ServerType;
import org.netbeans.modules.javaee.wildfly.config.mdb.MessageDestinationSupport;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Libor Kotouc
 */
public class MessageDestinationSupportImpl implements MessageDestinationSupport {

    public static final String MSG_DEST_RESOURCE_NAME_WILDFLY = "-jms.xml"; // NOI18N

    public static final String CONN_FACTORY_JNDI_NAME_JB4 = "ConnectionFactory"; // NOI18N

// the directory with resources - supplied by the configuration support in the construction time
    private File resourceDir;

    //model of the destination service file
    private MessagingDeployment destinationServiceModel;

    //destination service file (placed in the resourceDir)
    private File destinationsFile;

    //destination service file object
    private FileObject destinationsFO;
    
    private WildflyDeploymentManager dm;

    public MessageDestinationSupportImpl(File resourceDir, String moduleName) throws IOException {
        this.resourceDir = resourceDir;
        this.destinationsFile = new File(resourceDir, moduleName + MSG_DEST_RESOURCE_NAME_WILDFLY);
        ensureDestinationsFOExists();
        dm = Lookup.getDefault().lookup(WildflyDeploymentManager.class);
    }

    /**
     * Listener of netbeans-destinations-service.xml document changes.
     */
    private class MessageDestinationFileListener extends FileChangeAdapter {

        public void fileChanged(FileEvent fe) {
            assert (fe.getSource() == destinationsFO);
            destinationServiceModel = null;
        }

        public void fileDeleted(FileEvent fe) {
            assert (fe.getSource() == destinationsFO);
            destinationServiceModel = null;
        }
    }

    private void ensureDestinationsFOExists() throws IOException {
        if (!destinationsFile.exists()) {
            return;
        }
        if (destinationsFO == null || !destinationsFO.isValid()) {
            destinationsFO = FileUtil.toFileObject(destinationsFile);
            assert (destinationsFO != null);
            destinationsFO.addFileChangeListener(new MessageDestinationFileListener());
        }
    }

//---------------------------------------- READING --------------------------------------
    public Set<MessageDestination> getMessageDestinations() throws ConfigurationException {
        return getMessageDestinations(getMessageDestinationModel(false));
    }

    private static Set<MessageDestination> getMessageDestinations(MessagingDeployment model) throws ConfigurationException {

        if (model == null) {
            return Collections.<MessageDestination>emptySet();
        }

        HashSet<MessageDestination> destinations = new HashSet<MessageDestination>();

        for (ServerType serverType : model.getServer()) {
            if(serverType.getJmsDestinations() == null) {
                continue;
            }
            JmsQueueType[] queues = serverType.getJmsDestinations().getJmsQueue();
            for(JmsQueueType queue : serverType.getJmsDestinations().getJmsQueue()) {
                destinations.add(new WildflyMessageDestination(queue.getName(), Type.QUEUE));
            }
            for(JmsTopicType topic : serverType.getJmsDestinations().getJmsTopic()) {
                destinations.add(new WildflyMessageDestination(topic.getName(), Type.TOPIC));
            }
        }

        return destinations;
    }

    /**
     * Return destination service graph. If it was not created yet, load it from
     * the file and cache it. If the file does not exist, generate it.
     *
     * @return Destination service graph or null if the
     * jboss#-netbeans-destinations-service.xml file is not parseable.
     */
    private synchronized MessagingDeployment getMessageDestinationModel(boolean create) {

        try {
            if (destinationsFile.exists()) {
                // load configuration if already exists
                try {
                    if (destinationServiceModel == null) {
                        destinationServiceModel = MessagingDeployment.createGraph(destinationsFile);
                    }
                } catch (IOException ioe) {
                    Exceptions.printStackTrace(ioe);
                } catch (RuntimeException re) {
                    // netbeans-destinations-service.xml is not parseable, do nothing
                }
            } else if (create) {
                // create netbeans-destinations-service.xml if it does not exist yet
                destinationServiceModel = new MessagingDeployment();
                ResourceConfigurationHelper.writeFile(destinationsFile, destinationServiceModel);
                ensureDestinationsFOExists();
            }
        } catch (IOException ce) {
            Exceptions.printStackTrace(ce);
            destinationServiceModel = null;
        } catch (ConfigurationException ex) {
            Exceptions.printStackTrace(ex);
            destinationServiceModel = null;
        }

        return destinationServiceModel;
    }

//---------------------------------------- WRITING --------------------------------------
    public MessageDestination createMessageDestination(String name, MessageDestination.Type type)
            throws UnsupportedOperationException, ConfigurationException {

//        return new WildflyMessageDestination(name, type);
        if (!resourceDir.exists()) {
            resourceDir.mkdir();
        }

        if (!destinationsFile.exists()) {
            getMessageDestinationModel(true);
        }

        DataObject destinationsDO = null;
        try {
            destinationsDO = DataObject.find(destinationsFO);
        } catch (DataObjectNotFoundException donfe) {
            Exceptions.printStackTrace(donfe);
            return null;
        }

        // Model for the editor content or the latest saved model
        // if the editor content is not parseable or valid for any reason.
        MessagingDeployment newDestinationServiceModel = null;

        StyledDocument doc = null;
        try {
            // get the up-to-date model
            EditorCookie editor = (EditorCookie)destinationsDO.getCookie(EditorCookie.class);
            doc = editor.getDocument();
            if (doc == null) {
                doc = editor.openDocument();
            }
            // try to create a graph from the editor content
            byte[] docString = doc.getText(0, doc.getLength()).getBytes();
            newDestinationServiceModel = MessagingDeployment.createGraph(new ByteArrayInputStream(docString));
        } catch (IOException ioe) {
            String msg = NbBundle.getMessage(MessageDestinationSupport.class,
                    "MSG_CannotUpdateFile", destinationsFile.getAbsolutePath());    // NOI18N
            throw new ConfigurationException(msg, ioe);
        } catch (BadLocationException ble) {
            // this should not occur, just log it if it happens
            Logger.getLogger("global").log(Level.INFO, null, ble);
        } catch (RuntimeException e) {
            MessagingDeployment oldDestinationServiceModel = getMessageDestinationModel(true);
            if (oldDestinationServiceModel == null) {
                // neither the old graph is parseable, there is not much we can do here
                // TODO: should we notify the user?
                throw new ConfigurationException(
                        NbBundle.getMessage(MessageDestinationSupport.class,
                                "MSG_msgdestXmlCannotParse", destinationsFile.getAbsolutePath())); // NOI18N
            }
            // current editor content is not parseable, ask whether to override or not
            NotifyDescriptor notDesc = new NotifyDescriptor.Confirmation(
                    NbBundle.getMessage(MessageDestinationSupport.class,
                    "MSG_msgdestXmlNotValid", destinationsFile.getAbsolutePath()),       // NOI18N
                    NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(notDesc);
            if (result == NotifyDescriptor.CANCEL_OPTION) {
                // keep the old content
                return null;
            }
            // use the old graph
            newDestinationServiceModel = oldDestinationServiceModel;
        }

        WildflyMessageDestination dest = modifyMessageDestinationModel(
                newDestinationServiceModel, name, type);

        // save if needed
        boolean modified = destinationsDO.isModified();
        ResourceConfigurationHelper.replaceDocument(doc, newDestinationServiceModel);
        if (!modified) {
            SaveCookie cookie = (SaveCookie)destinationsDO.getCookie(SaveCookie.class);
            try {
                cookie.save();
            } catch (IOException ioe) {
                String msg = NbBundle.getMessage(MessageDestinationSupport.class,
                        "MSG_CannotSaveFile", destinationsFile.getAbsolutePath());    // NOI18N
                throw new ConfigurationException(msg, ioe);
            }
        }

        destinationServiceModel = newDestinationServiceModel;

        return dest;

    }

    private WildflyMessageDestination modifyMessageDestinationModel(
            MessagingDeployment model, String name, MessageDestination.Type type) throws ConfigurationException {
        if (model == null) {
            return null;
        }
        // check whether the destination doesn't exist yet
        for (MessageDestination destination : getMessageDestinations(model)) {
            if (name.equals(destination.getName()) && type == destination.getType()) {
                // already exists
                return null;
            }
        }
        if (type == MessageDestination.Type.QUEUE) {
            JmsQueueType queue = new JmsQueueType();
            queue.setName(name);
            model.getServer(0).getJmsDestinations().addJmsQueue(queue);
        } else if (type == MessageDestination.Type.TOPIC) {
           JmsTopicType topic = new JmsTopicType();
            topic.setName(name);
            model.getServer(0).getJmsDestinations().addJmsTopic(topic);
        }
        return new WildflyMessageDestination(name, type);
    }

}
