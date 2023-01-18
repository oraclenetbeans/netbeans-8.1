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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.javacard.ri.platform.loader;

import org.netbeans.modules.propdos.PropertiesAdapter;
import org.netbeans.modules.propdos.PropertiesBasedDataObject;
import org.netbeans.modules.propdos.ObservableProperties;
import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import org.netbeans.modules.javacard.common.Utils;
import org.netbeans.modules.javacard.common.JCConstants;
import org.netbeans.modules.javacard.spi.JavacardDeviceKeyNames;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.InstanceContent;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.Action;
import org.netbeans.modules.javacard.common.NodeRefresher;
import org.netbeans.modules.javacard.ri.card.RICard;
import org.netbeans.modules.javacard.spi.AbstractCard;
import org.netbeans.modules.javacard.spi.Card;
import org.netbeans.modules.javacard.spi.capabilities.CardInfo;
import org.netbeans.modules.javacard.spi.CardState;
import org.netbeans.modules.javacard.spi.CardStateObserver;
import org.netbeans.modules.javacard.spi.ICardCapability;
import org.netbeans.modules.javacard.spi.JavacardPlatform;
import org.netbeans.modules.javacard.spi.capabilities.StopCapability;
import org.netbeans.modules.javacard.spi.actions.CardActions;
import org.openide.loaders.DataNode;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.PropertySupport;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

public class CardDataObject extends PropertiesBasedDataObject<Card> implements CardStateObserver {

    private static final String ICON_BASE = "org/netbeans/modules/javacard/ri/platform/loader/card.png"; //NOI18N
    private Reference<Card> cardRef;
    private Reference<CardDataNode> nodeRef;
    private String platformName;
    private String myName;
    private static final RequestProcessor RP = new RequestProcessor(CardDataObject.class);

    public CardDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader, Card.class);
        content.add(new StringBuilder("platform"), new PlatformConverter()); //NOI18N
        content.add(new NR());
        platformName = pf.getParent().getName();
        myName = pf.getName();
    }

    private class NR implements NodeRefresher {
        public void refreshNode() {
            CardDataObject.this.refreshNode();
        }
    }

    @Override
    protected Node createNodeDelegate() {
        CardDataNode result = new CardDataNode(this);
        nodeRef = new WeakReference<CardDataNode>(result);
        return result;
    }

    private boolean deleting;
    public void refreshNode() {
        if (deleting || !isValid()) {
            return;
        }
        CardDataNode nd = nodeRef == null ? null : nodeRef.get();
        if (nd != null) {
            nd.checkForRunningStateChange();
            nd.updateChildren();
        }
    }

    @Override
    public boolean isDeleteAllowed() {
        return true;
    }

    @Override
    protected void onDelete(FileObject parentFolder) throws Exception {
        deleting = true;
        try {
            Card card = getLookup().lookup(Card.class);
            if (card != null && card.getState().isRunning()) {
                StopCapability c = card.getCapability(StopCapability.class);
                if (c != null) {
                    c.stop();
                }
            }
            File eepromfile = Utils.eepromFileForDevice(platformName, myName, false);
            if (eepromfile != null) {
                //Use FileObject so any views will be notified
                FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(eepromfile));
                if (fo != null) {
                    fo.delete();
                }
            }
        } finally {
            deleting = false;
        }
    }

    @Override
    protected void onReplaceObject() {
        Card old;
        synchronized (cardLock) {
            old = cardRef == null ? null : cardRef.get();
            cardRef = null;
        }
        if (old != null && old.getState().isRunning()) {
            StopCapability stopper = old.getCapability(StopCapability.class);
            if (stopper != null) {
                stopper.stop();
            }
        }
    }
    private final Object cardLock = new Object();

    @Override
    protected Card createFrom(ObservableProperties properties) {
        Card result = null;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "CardDataObject.createFrom() " + //NOI18N
                    getPrimaryFile().getPath());
        }
        synchronized (cardLock) {
            if (cardRef != null) {
                result = cardRef.get();
                if (result != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "CardDataObject.createFrom() " + //NOI18N
                                "returning cached instance"); //NOI18N
                    }
                    return result;
                }
            }
        }
        if (result == null) {
            JavacardPlatform platform = findPlatform();
            if (properties.isEmpty() || platform == null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Empty properties - " + //NOI18N
                                "returning broken card instance"); //NOI18N
                    }
                result = AbstractCard.createBrokenCard(getName());
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "No cached instance - invoking " + //NOI18N
                            "Card.create() for {0} with {1}", 
                            new Object[]{platform.getDisplayName(), properties}); //NOI18N
                }
                result = new RICard(this, platform, getName());
            }
        }
        assert result != null;
        result.addCardStateObserver(WeakListeners.create(
                CardStateObserver.class, this, result));
        synchronized (cardLock) {
            cardRef = new WeakReference<Card>(result);
        }
        CardDataNode nd = nodeRef == null ? null : nodeRef.get();
        if (nd != null) {
            nd.checkForRunningStateChange();
        }
        return result;
    }

    public JavacardPlatform findPlatform() {
        DataObject ob = findPlatformDataObject();
        JavacardPlatform platform = ob == null ? null : ob.getLookup().lookup(JavacardPlatform.class);
        return platform;
    }

    private DataObject findPlatformDataObject() {
        FileObject fo = getPrimaryFile().getParent();
        String lookFor = fo.getName();
        return Utils.findPlatformDataObjectNamed(lookFor);
    }

    @Override
    public void onStateChange(Card card, CardState old, CardState nue) {
        RP.post(new Runnable() {
            @Override
            public void run() {
                CardDataNode nd = nodeRef == null ? null : nodeRef.get();
                if (nd != null) {
                    nd.checkForRunningStateChange();
                }
            }
        });
    }

    @Override
    protected void propertyChanged(String propertyName, String newValue) {
        firePropertyChange(propertyName, null, newValue);
    }

    private class PlatformConverter implements InstanceContent.Convertor<StringBuilder, JavacardPlatform> {

        public JavacardPlatform convert(StringBuilder arg0) {
            return findPlatform();
        }

        public Class<? extends JavacardPlatform> type(StringBuilder arg0) {
            return JavacardPlatform.class;
        }

        public String id(StringBuilder arg0) {
            return "platform";
        }

        public String displayName(StringBuilder arg0) {
            return getName();
        }
    }

    private static final class WeakCardStateObserver implements CardStateObserver {
        private final Reference<CardStateObserver> proxy;
        WeakCardStateObserver(Card card, CardStateObserver real) {
            this.proxy = new WeakReference<CardStateObserver> (real);
        }

        public void onStateChange(Card card, CardState old, CardState nue) {
            CardStateObserver real = proxy.get();
            if (real == null) {
                card.removeCardStateObserver(this);
            } else {
                real.onStateChange(card, old, nue);
            }
        }
    }

    final class CardDataNode extends DataNode {
        private volatile boolean listening;
        private CardStateObserver childrenUpdater = new CardStateObserver() {
            public void onStateChange(Card card, CardState old, CardState nue) {
                if (getDataObject().isValid()) {
                    updateChildren();
                }
            }
        };

        CardDataNode(CardDataObject ob) {
            super(ob, Children.LEAF, ob.getLookup());
            setName(ob.getName());
            updateChildren();
        }

        private Card card;
        private void startListening (Card card) {
            if (card != null && (!listening || this.card != card)) {
                card.addCardStateObserver(new WeakCardStateObserver(card,
                        childrenUpdater));
                listening = true;
            }
            this.card = card; //hold a reference so it doesn't disappear
        }

        void updateChildren() {
            checkForRunningStateChange();
            if (!getDataObject().isValid()) {
                setChildren(Children.LEAF);
                return;
            }
            Card c = getDataObject().getLookup().lookup(Card.class);
            //Use !c.isNotRunning() so if the state is "starting" we will
            //have appropriate children
            boolean activeChildren = c == null ? false : !c.getState().isNotRunning();
            startListening (card);
            if (activeChildren) {
                Children kids = Children.create (new CardChildren(CardDataObject.this), true);
                setChildren(kids);
            } else {
                AbstractNode nd = new AbstractNode (Children.LEAF) {
                    //XXX replace when SimpleNode is integrated
                    @Override
                    public String getHtmlDisplayName() {
                        return "<font color='!controlShadow'>" + getDisplayName();
                    }

                    @Override
                    public Image getIcon (int icon) {
                        return super.getIcon(icon);//null;
                    }

                    @Override
                    public Image getOpenedIcon(int type) {
                        return super.getOpenedIcon(type);//null;
                    }
                };
                nd.setDisplayName (NbBundle.getMessage(CardDataNode.class,
                        "MSG_NOT_STARTED")); //NOI18N
                Children kids = new Children.Array();
                kids.add(new Node[] { nd });
                setChildren(kids);
            }
        }

        @Override
        public Image getIcon(int ignored) {
            Image result = ImageUtilities.loadImage(ICON_BASE);
            Card cardLocal = cardRef == null ? null : cardRef.get();
            if (cardLocal != null && cardLocal.getState().isRunning()) {
                Image badge = ImageUtilities.loadImage(
                        "org/netbeans/modules/javacard/spi/resources/running.png"); //NOI18N
                result = ImageUtilities.mergeImages(result, badge, 11, 11);
            } else if (cardLocal != null && !cardLocal.isValid()) {
                Image badge = ImageUtilities.loadImage(
                        "org/netbeans/modules/javacard/ri/platform/loader/errorBadge.png"); //NOI18N
                result = ImageUtilities.mergeImages(result, badge, 11, 11);
            }
            return result;
        }

        @Override
        protected Sheet createSheet() {
            Sheet sheet = super.createSheet();
            PropertiesBasedDataObject<?> ob = getLookup().lookup(PropertiesBasedDataObject.class);
            sheet.put(ob.getPropertiesAsPropertySet());
            Sheet.Set set = new Sheet.Set();
            set.setDisplayName(NbBundle.getMessage(CardDataNode.class,
                    "PROP_SET_OTHER")); //NOI18N
            set.setName(set.getDisplayName());
            set.put(new StateProp());
            set.put(new CapabilitiesProp());
            set.put(new EnabledCapabilitiesProp());
            sheet.put(set);
            return sheet;
        }

        @Override
        public Action getPreferredAction() {
            return CardActions.createCustomizeAction();
        }

        @Override
        public String getHtmlDisplayName() {
            if (card != null && !card.isValid()) {
                CardInfo info = card.getCapability(CardInfo.class);
                String name = info == null ? card.toString() : info.getDisplayName();
                return "<font color='!nb.errorForeground'>" + //NOI18N
                        name;
            }
            return null;
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getName() {
            DataObject dob = getLookup().lookup(DataObject.class);
            return dob.getName();
        }

        @Override
        public String getDisplayName() {
            DataObject dob = getLookup().lookup(DataObject.class);
            String result = null;
            if (dob != null) {
                if (JCConstants.TEMPLATE_DEFAULT_DEVICE_NAME.equals(dob.getName())) { //NOI18N
                    result = NbBundle.getMessage(CardDataObject.class,
                            "DEFAULT_DEVICE_NAME"); //NOI18N
                }
            } else {
                PropertiesAdapter p = getLookup().lookup(PropertiesAdapter.class);
                result = p.asProperties().getProperty(JavacardDeviceKeyNames.DEVICE_DISPLAY_NAME);
            }
            if (result == null) {
                result = dob.getName();
            }
            return result;
        }
        boolean checkingState;

        public void checkForRunningStateChange() {
            if (checkingState) {
                return;
            }
            checkingState = true;
            try {
                fireIconChange();
            } finally {
                checkingState = false;
            }
        }

        private class CapabilitiesProp extends PropertySupport.ReadOnly<String> {
            CapabilitiesProp() {
                this ("capabilities", "PROP_CAPABILITIES"); //NOI18N
            }

            CapabilitiesProp(String name, String key) {
                super (name, String.class, NbBundle.getMessage(CapabilitiesProp.class, key),
                        NbBundle.getMessage(CapabilitiesProp.class, "DESC_" + key)); //NOI18N
            }

            protected Set<? extends Class<? extends ICardCapability>> value() {
                Card card = getLookup().lookup(Card.class);
                return card.getSupportedCapabilities();
            }

            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                Set<? extends Class<? extends ICardCapability>> set = value();
                List<String> s = new ArrayList<String>();
                for (Class <? extends ICardCapability> c : set) {
                    s.add(c.getName());
                }
                Collections.sort(s);
                StringBuilder sb = new StringBuilder();
                for (String st : s) {
                    if (sb.length() != 0) {
                        sb.append (','); //NOI18N
                    }
                    sb.append(st);
                }
                return sb.toString();
            }
        }

        private class EnabledCapabilitiesProp extends CapabilitiesProp {
            EnabledCapabilitiesProp() {
                super ("enabledCapabilities", "PROP_ENABLED_CAPABILITIES"); //NOI18N
            }

            protected Set<? extends Class<? extends ICardCapability>> value() {
                Card card = getLookup().lookup(Card.class);
                return card.getEnabledCapabilities();
            }
        }

        private class StateProp extends PropertySupport.ReadOnly<String> {

            StateProp() {
                super("state", String.class, NbBundle.getMessage(StateProp.class, //NOI18N
                        "PROP_STATE"), NbBundle.getMessage(StateProp.class, //NOI18N
                        "DESC_PROP_STATE")); //NOI18N

            }

            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                Card card = getLookup().lookup(Card.class);
                return card.getState().toString();
            }
        }
    }
}
