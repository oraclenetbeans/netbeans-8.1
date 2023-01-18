/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2007-2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.providers.spi.HintMetadata;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.modules.java.hints.spiimpl.options.HintsSettings;
import org.netbeans.modules.java.source.tasklist.CompilerSettings;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author phrebejk
 */
public class StandardJavacWarnings extends AbstractHint {
  
    private static StandardJavacWarnings deprecated;
    private static StandardJavacWarnings unchecked;
    private static StandardJavacWarnings fallthrough;
    private static StandardJavacWarnings serialization;
    private static StandardJavacWarnings fajnly;
    private static StandardJavacWarnings unnecessaryCast;
    private static StandardJavacWarnings emptyStatementAfterIf;
    private static StandardJavacWarnings overrides;
    private static StandardJavacWarnings divisionByZero;
    private static StandardJavacWarnings rawTypes;
        
    private static final String JAVAC_ID = "Javac_"; // NOI18N
    
    private static final Set<Tree.Kind> treeKinds = EnumSet.noneOf(Tree.Kind.class);
    
    private final Kind kind;
    
    private StandardJavacWarnings(Kind kind) {
        super( kind.defaultOn(), false, HintSeverity.WARNING, kind.suppressWarnings );
        this.kind = kind;        
    }

    public static synchronized StandardJavacWarnings createDeprecated() {
        if ( deprecated == null ) {
            deprecated = new StandardJavacWarnings(Kind.DEPRECATED);
        }
        return deprecated;
    }
    
    public static synchronized StandardJavacWarnings createUnchecked() {
        if ( unchecked == null ) {
            unchecked = new StandardJavacWarnings(Kind.UNCHECKED);
        }
        return unchecked;
    }
    
    public static synchronized StandardJavacWarnings createFallthrough() {
        if ( fallthrough == null ) {
            fallthrough = new StandardJavacWarnings(Kind.FALLTHROUGH);
        }
        return fallthrough;
    }
    
    public static synchronized StandardJavacWarnings createSerialization() {
        if ( serialization == null ) {
            serialization = new StandardJavacWarnings(Kind.SERIALIZATION);
        }
        return serialization;
    }
    
    public static synchronized StandardJavacWarnings createFinally() {
        if ( fajnly == null ) {
            fajnly = new StandardJavacWarnings(Kind.FINALLY);
        }
        return fajnly;
    }
    
    public static synchronized StandardJavacWarnings createUnnecessaryCast() {
        if ( unnecessaryCast == null ) {
            unnecessaryCast = new StandardJavacWarnings(Kind.UNNECESSARY_CAST);
        }
        return unnecessaryCast;
    }
    
    public static synchronized StandardJavacWarnings createEmptyStatementAfterIf() {
        if ( emptyStatementAfterIf == null ) {
            emptyStatementAfterIf = new StandardJavacWarnings(Kind.EMPTY_STATEMENT_AFTER_IF);
        }
        return emptyStatementAfterIf;
    }
    
    public static synchronized StandardJavacWarnings createOverrides() {
        if ( overrides == null ) {
            overrides = new StandardJavacWarnings(Kind.OVERRIDES);
        }
        return overrides;
    }
    
    public static synchronized StandardJavacWarnings createDivisionByZero() {
        if ( divisionByZero == null ) {
            divisionByZero = new StandardJavacWarnings(Kind.DIVISION_BY_ZERO);
        }
        return divisionByZero;
    }
            
    public static synchronized StandardJavacWarnings createRawTypes() {
        if ( rawTypes == null ) {
            rawTypes = new StandardJavacWarnings(Kind.RAWTYPES);
        }
        return rawTypes;
    }

    public Set<Tree.Kind> getTreeKinds() {
        return treeKinds;        
    }

    public List<ErrorDescription> run(CompilationInfo compilationInfo, TreePath treePath) {
        // Will never run
        return null;
    }
        
    public void cancel() {
        // Will never run
    }

    public String getId() {
        return JAVAC_ID + kind.toString();
    }
    
    public String getDisplayName() {        
        return NbBundle.getMessage(Imports.class, "LBL_Javac_" + kind.toString()); // NOI18N
    }

    public String getDescription() {
        return NbBundle.getMessage(Imports.class, "DSC_Javac_" + kind.toString()); // NOI18N
    }

    // Private methods ---------------------------------------------------------
    
    private static enum Kind {

        DEPRECATED("enable_lint_deprecation", "deprecation"),
        UNCHECKED("enable_lint_unchecked", "unchecked"),
        FALLTHROUGH("enable_lint_fallthrough", "fallthrough"),
        SERIALIZATION("enable_lint_serial", "serial"),
        FINALLY("enable_lint_finally", "finally"),
        UNNECESSARY_CAST("enable_lint_cast", "cast", "", "RedundantCast"),
        EMPTY_STATEMENT_AFTER_IF("enable_lint_empty", "empty"),
        OVERRIDES("enable_lint_overrides", "overrides"),
        DIVISION_BY_ZERO("enable_lint_dvizero", "divzero"),
        RAWTYPES("enable_lint_rawtypes", "rawtypes");
        
        private final String key;
        private final String lintKey;
        private final String[] suppressWarnings;

        private Kind(String key, String lintKey, String... suppressWarnings) {
            this.key = key;
            this.lintKey = lintKey;
            this.suppressWarnings = new String[suppressWarnings.length + 1];
            this.suppressWarnings[0] = lintKey;
            System.arraycopy(suppressWarnings, 0, this.suppressWarnings, 1, suppressWarnings.length);
        }
        
        boolean defaultOn() {        
            return false;
        }
        
        String key() {
            return key;
        }
    }
   
    @ServiceProvider(service=CompilerSettings.class)
    public static final class CompilerSettingsImpl extends CompilerSettings {
        @Override protected String buildCommandLine(FileObject file) {
            HintsSettings hs = file != null ? HintsSettings.getSettingsFor(file) : HintsSettings.getGlobalSettings();

            StringBuilder sb = new StringBuilder();

            for (Kind k : Kind.values()) {
                if (hs.isEnabled(HintMetadata.Builder.create(JAVAC_ID + k.name()).setEnabled(k.defaultOn()).build())) {
                    sb.append("-Xlint:").append(k.lintKey).append(" ");
                }
            }
            
            sb.append("-XDidentifyLambdaCandidate=true ");
            sb.append("-XDfindDiamond ");

            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }
}
