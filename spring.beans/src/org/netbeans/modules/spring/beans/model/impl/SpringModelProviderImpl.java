/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.spring.beans.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.source.ClassIndexListener;
import org.netbeans.api.java.source.RootsEvent;
import org.netbeans.api.java.source.TypesEvent;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.AnnotationModelHelper;
import org.netbeans.modules.j2ee.metadata.model.api.support.annotation.PersistentObjectManager;
import org.netbeans.modules.spring.api.beans.SpringAnnotations;
import org.netbeans.modules.spring.api.beans.model.SpringBean;
import org.netbeans.modules.spring.spi.beans.SpringModelProvider;

/**
 * @author Martin Fousek <marfous@netbeans.org>
 */
class SpringModelProviderImpl implements SpringModelProvider {

    private AnnotationModelHelper helper;
    private PersistentObjectManager<SpringBeanImpl> springBeanManager;
    
    private AtomicBoolean isDirty = new AtomicBoolean(true);
    private volatile boolean isIndexListenerAdded;
    private List<SpringBean> cachedSpringBeans;

    public SpringModelProviderImpl(SpringModelImplementation springModelImplementation) {
        this.helper = springModelImplementation.getHelper();
        this.springBeanManager = helper.createPersistentObjectManager(new ObjectProviders.SpringBeanProvider(helper));
    }

    /**
     * Returns annotated Spring bean classes. It means every class annotated with one of
     * {@link SpringAnnotations#SPRING_COMPONENTS} string type.
     * @return 
     */
    @Override
    public List<SpringBean> getBeans() {
        boolean dirty = isDirty.getAndSet(false);

        if (!isIndexListenerAdded) {
            addIndexListener();
        }
        if (!dirty) {
            List<SpringBean> result = getCachedNamedElements();
            if (!isDirty.get()) {
                return result;
            }
        }

        List<SpringBean> result = new LinkedList<SpringBean>();
        Collection<SpringBeanImpl> springBeans = getSpringBeanManager().getObjects();
        for (SpringBeanImpl springBeanImpl : springBeans) {
            result.add(springBeanImpl);
        }

        setCachedResult(result);
        return result;
    }

    private PersistentObjectManager<SpringBeanImpl> getSpringBeanManager() {
        return springBeanManager;
    }

    private void addIndexListener() {
        isIndexListenerAdded = true;
        helper.getClasspathInfo().getClassIndex().addClassIndexListener(
                new ClassIndexListener() {

                    @Override
                    public void typesAdded(final TypesEvent event) {
                        setDirty();
                    }

                    @Override
                    public void typesRemoved(final TypesEvent event) {
                        setDirty();
                    }

                    @Override
                    public void typesChanged(final TypesEvent event) {
                        setDirty();
                    }

                    @Override
                    public void rootsAdded(RootsEvent event) {
                        setDirty();
                    }

                    @Override
                    public void rootsRemoved(RootsEvent event) {
                        setDirty();
                    }

                    private void setDirty() {
                        isDirty.set(true);
                    }
                });
    }

    private void setCachedResult(List<SpringBean> list) {
        cachedSpringBeans = new ArrayList<SpringBean>(list);
    }

    private List<SpringBean> getCachedNamedElements() {
        List<SpringBean> result = new ArrayList<SpringBean>(cachedSpringBeans);
        return result;
    }
}
