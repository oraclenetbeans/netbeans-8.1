/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.toolchain.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.cnd.spi.toolchain.ErrorParserProvider.OutputListenerRegistry;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.NbBundle;
import org.openide.windows.IOPosition;
import org.openide.windows.IOSelect;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

public final class OutputListenerImpl implements OutputListener {
    private static final String CC_compiler_errors = "C/C++ compiler errors"; // NOI18N

    private final OutputListenerRegistry registry;
    private final FileObject file;
    private final int line;
    private final boolean isError;
    private final String description;
    private final IOPosition.Position ioPos;

    public OutputListenerImpl(OutputListenerRegistry registry, FileObject file, int line, boolean isError, String description, IOPosition.Position ioPos) {
        this.registry = registry;
        this.file = file;
        this.line = line;
	this.isError = isError;
        this.description = description;
        this.ioPos = ioPos;
    }

    public int getLine() {
        return line;
    }

    public String getDescription() {
        return description;
    }

    public FileObject getFile() {
        return file;
    }

    public IOPosition.Position getIOPos() {
        return ioPos;
    }
 
    @Override
    public void outputLineSelected(OutputEvent ev) {
        showLine(false);
    }

    @Override
    public void outputLineAction(OutputEvent ev) {
        showLine(true);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (this.file != null ? this.file.hashCode() : 0);
        hash = 83 * hash + this.line;
        hash = 83 * hash + (this.isError ? 1 : 0);
        hash = 83 * hash + (this.description != null ? this.description.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OutputListenerImpl other = (OutputListenerImpl) obj;
        if (this.file != other.file && (this.file == null || !this.file.equals(other.file))) {
            return false;
        }
        if (this.line != other.line) {
            return false;
        }
        if (this.isError != other.isError) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        return true;
    }

    @Override
    public void outputLineCleared(OutputEvent ev) {
        try {
            DataObject dob = DataObject.find(file);
            StyledDocument doc = null;
            if (dob.isValid()) {
                EditorCookie ec = dob.getLookup().lookup(EditorCookie.class);
                if (ec != null) {
                    doc = ec.getDocument();
                }
            }
            if (doc != null) {
                HintsController.setErrors(doc, CC_compiler_errors, Collections.<ErrorDescription>emptyList());
            }
        } catch (DataObjectNotFoundException ex) {
        }
    }

    public boolean isError(){
	return isError;
    }

    public static void attach(OutputListenerRegistry registry) {
        JTextComponent pane = EditorRegistry.lastFocusedComponent();
        if (pane == null) {
            return;
        }
        Document doc = pane.getDocument();
        if (doc == null) {
            return;
        }
        DataObject dobj = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        if (dobj == null) {
            return;
        }
        FileObject file = dobj.getPrimaryFile();
        if (file == null) {
            return;
        }
        attachFile(registry, file, doc);
    }
    
    private void showLine(boolean openTab) {
        try {
            DataObject dob = DataObject.find(file);
            LineCookie lc = dob.getLookup().lookup(LineCookie.class);
            if (lc != null) {
                try {
                    // TODO: IZ#119211
                    // Preprocessor supports #line directive =>
                    // line number can be out of scope
                    Line l = lc.getLineSet().getOriginal(line);
                    if (!l.isDeleted()) {
                        if (openTab) {
                            l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                        } else {
                            l.show(Line.ShowOpenType.NONE, Line.ShowVisibilityType.NONE);
                        }
                        StyledDocument doc = null;
                        if (dob.isValid()) {
                            EditorCookie ec = dob.getLookup().lookup(EditorCookie.class);
                            if (ec != null) {
                                doc = ec.getDocument();
                            }
                        }
                        if (doc != null) {
                            attachFile(registry, file, doc);
                        }
                    }
                } catch (IndexOutOfBoundsException ex) {
                }
            }
        } catch (DataObjectNotFoundException ex) {
        }
    }

    private static void attachFile(OutputListenerRegistry registry, FileObject file, Document doc) throws MissingResourceException {
        final Set<OutputListener> fileListeners = registry.getFileListeners(file);
        if (fileListeners == null) {
            return;
        }
        List<ErrorDescription> errors = new ArrayList<ErrorDescription>();
        for(OutputListener listener : fileListeners){
            if (listener instanceof OutputListenerImpl) {
                OutputListenerImpl impl = (OutputListenerImpl) listener;
                String aDescription = impl.description;
                List<Fix> fixes = new ArrayList<Fix>();
                if (IOPosition.isSupported(registry.getIO()) && IOSelect.isSupported(registry.getIO())) {
                    fixes.add(new ShowInOutputFix(impl.description, registry.getIO(), impl.ioPos));
                }
                try {
                    if (impl.isError) {
                        if (aDescription == null) {
                            aDescription = NbBundle.getMessage(OutputListenerImpl.class, "HINT_CompilerError"); // NOI18N
                        }
                        errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, aDescription, fixes, doc, impl.line + 1));
                    } else {
                        if (aDescription == null) {
                            aDescription = NbBundle.getMessage(OutputListenerImpl.class, "HINT_CompilerWarning"); // NOI18N
                        }
                        errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING, aDescription, fixes, doc, impl.line + 1));
                    }
                } catch (IndexOutOfBoundsException e) {
                    //probably the document has been modified or
                    //compiler error parser detect wrong line
                }
            }
        }
        HintsController.setErrors(doc, CC_compiler_errors, errors);
    }
}
