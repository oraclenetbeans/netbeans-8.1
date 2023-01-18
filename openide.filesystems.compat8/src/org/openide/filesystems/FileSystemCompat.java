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

package org.openide.filesystems;

import java.awt.Image;
import java.awt.Toolkit;
import java.beans.BeanInfo;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;
import org.omg.CORBA.Environment;
import static org.openide.filesystems.FileSystem.LOG;
import org.openide.modules.PatchFor;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.SystemAction;

/**
 * The class provides an API compatibility bridge to support implementations
 * compiled against <code>org.openide.filesystems&lt;8.0</code> and earlier
 *
 * @author sdedic
 */
@PatchFor(FileSystem.class)
public abstract class FileSystemCompat {
    /** Property name giving capabilities state. @deprecated No more capabilities. */
    static final String PROP_CAPABILITIES = "capabilities"; // NOI18N    

    /** hidden flag */
    private boolean hidden = false;

    /** Describes capabilities of the filesystem.
    */
    @Deprecated // have to store it for compat
    private /* XXX JDK #6460147: javac still reports it even though @Deprecated,
               and @SuppressWarnings("deprecation") does not help either: FileSystemCapability*/Object capability;

    /** property listener on FileSystemCapability. */
    private transient PropertyChangeListener capabilityListener;

    /** Returns an array of actions that can be invoked on any file in
    * this filesystem.
    * These actions should preferably
    * support the {@link org.openide.util.actions.Presenter.Menu Menu},
    * {@link org.openide.util.actions.Presenter.Popup Popup},
    * and {@link org.openide.util.actions.Presenter.Toolbar Toolbar} presenters.
    *
    * @return array of available actions
    */
    public abstract SystemAction[] getActions();

    /**
     * Get actions appropriate to a certain file selection.
     * By default, returns the same list as {@link #getActions()}.
     * @param foSet one or more files which may be selected
     * @return zero or more actions appropriate to those files
     */
    public SystemAction[] getActions(Set<FileObject> foSet) {
        return this.getActions();
    }

    /** Allows filesystems to set up the environment for external execution
    * and compilation.
    * Each filesystem can add its own values that
    * influence the environment. The set of operations that can modify
    * environment is described by the {@link Environment} interface.
    * <P>
    * The default implementation throws an exception to signal that it does not
    * support external compilation or execution.
    *
    * @param env the environment to setup
    * @exception EnvironmentNotSupportedException if external execution
    *    and compilation cannot be supported
    * @deprecated Please use the <a href="@org-netbeans-api-java-classpath@/org/netbeans/api/java/classpath/ClassPath.html">ClassPath API</a> instead.
    */
    @Deprecated
    public void prepareEnvironment(FileSystem$Environment env) throws EnvironmentNotSupportedException {
        Object o = this;
        throw new EnvironmentNotSupportedException((FileSystem)o);
    }
    
    /**
     * Provides access to deprecated methods at runtime.
     * 
     * @param fs The FileSystem
     * @return the FileSystemCompat instanceo for the FileSystem.
     */
    public static FileSystemCompat compat(FileSystem fs) {
        Object o = fs;
        return (FileSystemCompat)o;
    }
    
    private FileSystem fs() {
        Object o = this;
        return (FileSystem)o;
    }

    /** The object describing capabilities of this filesystem.
     * Subclasses cannot override it.
     * @return object describing capabilities of this filesystem.
     * @deprecated Capabilities are no longer used.
     */
    @Deprecated
    public final FileSystemCapability getCapability() {
        if (capability == null) {
            capability = new FileSystemCapability.Bean();
            ((FileSystemCapability) capability).addPropertyChangeListener(getCapabilityChangeListener());
        }

        return (FileSystemCapability) capability;
    }

    /** Allows subclasses to change a set of capabilities of the
    * filesystem.
    * @param capability the capability to use
     * @deprecated Capabilities are no longer used.
    */
    @Deprecated
    protected final void setCapability(FileSystemCapability capability) {
        if (this.capability != null) {
            ((FileSystemCapability) this.capability).removePropertyChangeListener(getCapabilityChangeListener());
        }

        this.capability = capability;

        if (this.capability != null) {
            ((FileSystemCapability) this.capability).addPropertyChangeListener(getCapabilityChangeListener());
        }
    }

    /** returns property listener on FileSystemCapability. */
    private synchronized PropertyChangeListener getCapabilityChangeListener() {
        if (capabilityListener == null) {
            capabilityListener = new PropertyChangeListener() {
                        public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
                            fs().firePropertyChange(
                                PROP_CAPABILITIES, propertyChangeEvent.getOldValue(), propertyChangeEvent.getNewValue()
                            );
                        }
                    };
        }

        return capabilityListener;
    }
    
    /** Reads object from stream and creates listeners.
    * @param in the input stream to read from
    * @exception IOException error during read
    * @exception ClassNotFoundException when class not found
    */
    @SuppressWarnings("deprecation")
    private void readObject(java.io.ObjectInputStream in)
    throws java.io.IOException, java.lang.ClassNotFoundException {
        in.defaultReadObject();

        if (capability != null) {
            ((FileSystemCapability) capability).addPropertyChangeListener(getCapabilityChangeListener());
        }
    }

    /** Set hidden state of the object.
     * A hidden filesystem is not presented to the user in the Repository list (though it may be present in the Repository Settings list).
    *
    * @param hide <code>true</code> if the filesystem should be hidden
     * @deprecated This property is now useless.
    */
    @Deprecated
    public final void setHidden(boolean hide) {
        if (hide != hidden) {
            hidden = hide;
            fs().firePropertyChange(FileSystem.PROP_HIDDEN, (!hide) ? Boolean.TRUE : Boolean.FALSE, hide ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    /** Getter for the hidden property.
     * @return the hidden property.
     * @deprecated This property is now useless.
    */
    @Deprecated
    public final boolean isHidden() {
        return hidden;
    }

    /** Tests whether filesystem will survive reloading of system pool.
    * If true then when
    * {@link Repository} is reloading its content, it preserves this
    * filesystem in the pool.
    * <P>
    * This can be used when the pool contains system level and user level
    * filesystems. The system ones should be preserved when the user changes
    * the content (for example when he is loading a new project).
    * <p>The default implementation returns <code>false</code>.
    *
    * @return true if the filesystem should be persistent
     * @deprecated This property is long since useless.
    */
    @Deprecated
    protected boolean isPersistent() {
        return false;
    }
    
    static {
        try {
            Field f = FileSystem.class.getDeclaredField("SFS_STATUS"); // NOI18N
            f.setAccessible(true);
            StatusDecorator del = (StatusDecorator)f.get(null);
            f.set(null, new SystemStatus(del));
        } catch (NoSuchFieldException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Enhancement of the default SFS_Status, loads image through ImageUtilities.
     */
    private static class SystemStatus implements FileSystem$Status, StatusDecorator {
        private final StatusDecorator delegate;

        public SystemStatus(StatusDecorator delegate) {
            this.delegate = delegate;
        }

        @Override
        public String annotateName(String name, Set<? extends FileObject> files) {
            return delegate.annotateName(name, files);
        }

        @Override
        public Image annotateIcon(Image im, int type, Set<? extends FileObject> files) {
            for (FileObject fo : files) {
                Image img = annotateIcon(fo, type);
                if (img != null) {
                    return img;
                }
            }
            return im;
        }

        private Image annotateIcon(FileObject fo, int type) {
            String attr = null;
            if (type == BeanInfo.ICON_COLOR_16x16) {
                attr = "SystemFileSystem.icon"; // NOI18N
            } else if (type == BeanInfo.ICON_COLOR_32x32) {
                attr = "SystemFileSystem.icon32"; // NOI18N
            }
            if (attr != null) {
                Object value = fo.getAttribute(attr);
                if (value != null) {
                    if (value instanceof URL) {
                        return Toolkit.getDefaultToolkit().getImage((URL) value);
                    } else if (value instanceof Image) {
                        // #18832
                        return (Image) value;
                    } else {
                        LOG.warning("Attribute " + attr + " on " + fo + " expected to be a URL or Image; was: " + value);
                    }
                }
            }
            String base = (String) fo.getAttribute("iconBase"); // NOI18N
            if (base != null) {
                if (type == BeanInfo.ICON_COLOR_16x16) {
                    return ImageUtilities.loadImage(base, true);
                } else if (type == BeanInfo.ICON_COLOR_32x32) {
                    return ImageUtilities.loadImage(insertBeforeSuffix(base, "_32"), true); // NOI18N
                }
            }
            return null;
        }


        private static String insertBeforeSuffix(String path, String toInsert) {
            String withoutSuffix = path;
            String suffix = ""; // NOI18N
            if (path.lastIndexOf('.') >= 0) {
                withoutSuffix = path.substring(0, path.lastIndexOf('.'));
                suffix = path.substring(path.lastIndexOf('.'), path.length());
            }
            return withoutSuffix + toInsert + suffix;
        }

        @Override
        public String annotateNameHtml(String name, Set<? extends FileObject> files) {
            return name;
        }
    }
    
    private FileSystem$Status status;
    
    /**
     * Compatible implementation of FileSystem.getStatus()
     * 
     * @return 
     */
    public FileSystem$Status getStatus() {
        if (status == null) {
            status = new StatusImpl(fs().getDecorator());
        }
        return status;
    }
    
    /**
     * This is a compatible implementation of FileSystem.Status, which bridges
     * a new FS implementation (Decorator, IconDecorator) to the old Status interface
     */
    private static class StatusImpl implements FileSystem$Status, FileSystem$HtmlStatus {
        private final StatusDecorator decorator;
        private final Method annotateIconDel;
        
        public StatusImpl(StatusDecorator decorator) {
            this.decorator = decorator;
            Method m = null;
            try {
                m = decorator.getClass().getMethod("annotateIcon", Image.class, Integer.TYPE, Set.class);
                m.setAccessible(true);
            } catch (NoSuchMethodException ex) {
            } catch (SecurityException ex) {
                Exceptions.printStackTrace(ex);
            }
            this.annotateIconDel = m;
        }
        
        @Override
        public String annotateName(String name, Set<? extends FileObject> files) {
            return decorator.annotateName(name, files);
        }

        @Override
        public Image annotateIcon(Image icon, int iconType, Set<? extends FileObject> files) {
            if (annotateIconDel == null) {
                return icon;
            }
            try {
                return (Image)annotateIconDel.invoke(decorator, icon, iconType, files);
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
            return icon;
        }

        @Override
        public String annotateNameHtml(String name, Set<? extends FileObject> files) {
            return decorator.annotateNameHtml(name, files);
        }
    }
}
