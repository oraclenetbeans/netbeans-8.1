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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.source.ant;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.modules.java.source.usages.BuildArtifactMapperImpl;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Utilities;

/**
 *
 * @author Jan Lahoda
 */
public class TranslateClassPath extends Task {

    private String classpath;
    private String targetProperty;
    private boolean clean;
    
    public void setClasspath(String cp) {
        this.classpath = cp;
    }
    
    public void setTargetProperty(String tp) {
        this.targetProperty = tp;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    @Override
    public void execute() throws BuildException {
        if (classpath == null) {
            throw new BuildException("Classpath must be set.");
        }
        if (targetProperty == null) {
            throw new BuildException("Target property must be set.");
        }
        
        Project p = getProject();

        String translated = translate(classpath);
        
        p.setProperty(targetProperty, translated);
    }
    
    private String translate(String classpath) {
        StringBuilder cp = new StringBuilder();
        boolean first = true;
        boolean disableSources = Boolean.valueOf(getProject().getProperty("maven.disableSources"));
        
        for (String path : PropertyUtils.tokenizePath(classpath)) {
            File[] files = translateEntry(path, disableSources);

            if (files.length == 0) {
                //TODO: log
//                LOG.log(Level.FINE, "cannot translate {0} to file", e.getURL().toExternalForm());
                continue;
            }

            for (File f : files) {
                if (!first) {
                    cp.append(File.pathSeparatorChar);
                }

                cp.append(f.getAbsolutePath());
                first = false;
            }
        }

        return cp.toString();
    }
    
    private File[] translateEntry(String path, boolean disableSources) throws BuildException {
        final File entryFile = new File(path);
        try {
            final URL entry = FileUtil.urlForArchiveOrDir(entryFile);
            final SourceForBinaryQuery.Result2 r = SourceForBinaryQuery.findSourceRoots2(entry);
            boolean appendEntry = false;

            if (!disableSources && r.preferSources() && r.getRoots().length > 0) {
                final List<File> translated = new ArrayList<File>();
                for (FileObject source : r.getRoots()) {
                    final File sourceFile = FileUtil.toFile(source);
                    if (sourceFile == null) {
                        log("Source URL: " + source.toURL().toExternalForm() + " cannot be translated to file, skipped", Project.MSG_WARN);
                        appendEntry = true;
                        continue;
                    }

                    final Boolean bamiResult = clean ? BuildArtifactMapperImpl.clean(Utilities.toURI(sourceFile).toURL())
                                               : BuildArtifactMapperImpl.ensureBuilt(Utilities.toURI(sourceFile).toURL(), getProject(), true, true);
                    if (bamiResult == null) {
                        appendEntry = true;
                        continue;
                    }
                    if (!bamiResult) {
                        throw new UserCancel();
                    }
                    
                    for (URL binary : BinaryForSourceQuery.findBinaryRoots(source.toURL()).getRoots()) {
                        final FileObject binaryFO = URLMapper.findFileObject(binary);
                        final File finaryFile = binaryFO != null ? FileUtil.toFile(binaryFO) : null;
                        if (finaryFile != null) {
                            translated.add(finaryFile);
                        }
                    }
                    
                }
                
                if (appendEntry) {
                    translated.add(entryFile);
                }
                return translated.toArray(new File[translated.size()]);
            } else {
                return new File[] {entryFile};
            }
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }

}
