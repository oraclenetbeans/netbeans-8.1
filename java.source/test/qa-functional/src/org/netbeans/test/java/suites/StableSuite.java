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
package org.netbeans.test.java.suites;

import junit.framework.Test;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.test.java.generating.ConstructorElem;
import org.netbeans.test.java.generating.FieldElem;
import org.netbeans.test.java.generating.InitializerElem;
import org.netbeans.test.java.generating.InnerClasses;
import org.netbeans.test.java.generating.MethodElem;
import org.netbeans.test.java.generating.SourceElem;
import org.netbeans.test.java.generating.SuperClassInterfaces;
import org.netbeans.test.java.gui.copypaste.ClassNodeTest;
import org.netbeans.test.java.gui.copypaste.PackageNodeTest;
import org.netbeans.test.java.gui.errorannotations.ErrorAnnotations;
import org.netbeans.test.java.gui.fiximports.FixImportsTest;
import org.netbeans.test.java.gui.parser.ParserTest;
import org.netbeans.test.java.gui.wizards.NewFileWizardTest;
import org.netbeans.test.java.hints.AddElementHintTest;
import org.netbeans.test.java.hints.AddImportTest;
import org.netbeans.test.java.hints.HintsTest;
import org.netbeans.test.java.hints.ImplAllAbstractTest;
import org.netbeans.test.java.hints.IntroduceInlineTest;
import org.netbeans.test.java.hints.SurroundTest;
import org.netbeans.test.java.rename.InstantRename;

/**
 *
 * @author Jiri Prox
 */
public class StableSuite {

    public static Test suite() {
        return NbModuleSuite.create(
                NbModuleSuite.createConfiguration(ClassNodeTest.class)
                .addTest(ClassNodeTest.class)
                .addTest(PackageNodeTest.class)
                .addTest(PackageNodeTest.class)
                .addTest(ErrorAnnotations.class)
                .addTest(FixImportsTest.class)
                .addTest(NewFileWizardTest.class)
                .addTest(AddElementHintTest.class)
                .addTest(AddImportTest.class)
                .addTest(HintsTest.class)
                .addTest(ImplAllAbstractTest.class)
//                .addTest(IntroduceInlineTest.class)
//                .addTest(SurroundTest.class)                
//                .addTest(ConstructorElem.class)
//                .addTest(FieldElem.class)
//                .addTest(InitializerElem.class)
//                .addTest(InnerClasses.class)
//                .addTest(MethodElem.class)
//                .addTest(SourceElem.class)
//                .addTest(SuperClassInterfaces.class)
//	          .addTest(ParserTest.class)
                .addTest(InstantRename.class)                        
                .enableModules(".*")
                .clusters(".*")
                );
    }
}
