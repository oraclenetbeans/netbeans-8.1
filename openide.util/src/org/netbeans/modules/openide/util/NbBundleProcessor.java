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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.openide.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;
import org.openide.util.BaseUtilities;
import org.openide.util.EditableProperties;
import org.openide.util.NbBundle;
import org.openide.util.NbCollections;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class NbBundleProcessor extends AbstractProcessor {

    public @Override Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(NbBundle.Messages.class.getCanonicalName());
    }

    public @Override boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        Map</*package*/String,Set<Element>> annotatedElementsByPackage = new HashMap<String,Set<Element>>();
        for (Element e : roundEnv.getElementsAnnotatedWith(NbBundle.Messages.class)) {
            NbBundle.Messages messages = e.getAnnotation(NbBundle.Messages.class);
            if (messages == null) { // bug in java.source, apparently; similar to #195983
                continue;
            }
            String pkg = findPackage(e);
            Set<Element> annotatedElements = annotatedElementsByPackage.get(pkg);
            if (annotatedElements == null) {
                annotatedElements = new HashSet<Element>();
                annotatedElementsByPackage.put(pkg, annotatedElements);
            }
            annotatedElements.add(e);
        }
        PACKAGE: for (Map.Entry<String,Set<Element>> packageEntry : annotatedElementsByPackage.entrySet()) {
            String pkg = packageEntry.getKey();
            Set<Element> annotatedElements = packageEntry.getValue();
            PackageElement pkgE = processingEnv.getElementUtils().getPackageElement(pkg);
            if (pkgE != null) {
                Set<Element> unscannedTopElements = new HashSet<Element>();
                unscannedTopElements.add(pkgE);
                try {
                    unscannedTopElements.addAll(pkgE.getEnclosedElements());
                } catch (/*NullPointerException,BadClassFile*/RuntimeException x) { // #196556
                    processingEnv.getMessager().printMessage(Kind.WARNING, "#196556: reading " + pkg + " failed with " + x + " in " + x.getStackTrace()[0] + "; do a clean build!");
                }
                unscannedTopElements.removeAll(roundEnv.getRootElements());
                addToAnnotatedElements(unscannedTopElements, annotatedElements);
            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING, "Could not check for other source files in " + pkg);
            }
            Map</*key*/String,/*value*/String> pairs = new HashMap<String,String>();
            Map</*identifier*/String,Element> identifiers = new HashMap<String,Element>();
            Map</*key*/String,/*simplename*/String> compilationUnits = new HashMap<String,String>();
            Map</*key*/String,/*line*/String[]> comments = new HashMap<String,String[]>();
            for (Element e : annotatedElements) {
                String simplename = findCompilationUnitName(e);
                List<String> runningComments = new ArrayList<String>();
                for (String keyValue : e.getAnnotation(NbBundle.Messages.class).value()) {
                    if (keyValue.startsWith("#")) {
                        runningComments.add(keyValue);
                        if (keyValue.matches("# +(PART)?(NO)?I18N *")) {
                            processingEnv.getMessager().printMessage(Kind.ERROR, "#NOI18N and related keywords must not include spaces", e);
                        }
                        continue;
                    }
                    int i = keyValue.indexOf('=');
                    if (i == -1) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Bad key=value: " + keyValue, e);
                        continue;
                    }
                    String key = keyValue.substring(0, i);
                    if (key.isEmpty() || !key.equals(key.trim())) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Whitespace not permitted in key: " + keyValue, e);
                        continue;
                    }
                    Element original = identifiers.put(toIdentifier(key), e);
                    if (original != null) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Duplicate key: " + key, e);
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Duplicate key: " + key, original);
                        continue PACKAGE; // do not generate anything
                    }
                    String value = keyValue.substring(i + 1);
                    pairs.put(key, value);
                    compilationUnits.put(key, simplename);
                    if (!runningComments.isEmpty()) {
                        comments.put(key, runningComments.toArray(new String[runningComments.size()]));
                        runningComments.clear();
                    }
                }
                if (!runningComments.isEmpty()) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Comments must precede keys", e);
                }
            }
            Element[] elements = new HashSet<Element>(identifiers.values()).toArray(new Element[0]);
            try {
                EditableProperties p = new EditableProperties(true);
                // Load any preexisting bundle so we can just add our keys.
                try {
                    InputStream is = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, pkg, "Bundle.properties").openInputStream();
                    try {
                        p.load(is);
                    } finally {
                        is.close();
                    }
                } catch (IOException x) {
                    // OK, not there
                }
                for (String key : p.keySet()) {
                    if (pairs.containsKey(key)) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Key " + key + " is a duplicate of one from Bundle.properties", identifiers.get(toIdentifier(key)));
                    }
                }
                // Also check class output for (1) incremental builds, (2) preexisting bundles from Maven projects.
                try {
                    InputStream is = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, pkg, "Bundle.properties").openInputStream();
                    try {
                        // do not use p.load(is) as the impl in EditableProperties does not currently handle duplicates properly
                        EditableProperties p2 = new EditableProperties(true);
                        p2.load(is);
                        p.putAll(p2);
                    } finally {
                        is.close();
                    }
                } catch (IOException x) {
                    // OK, not there
                }
                p.putAll(pairs);
                for (Map.Entry<String,String[]> entry2 : comments.entrySet()) {
                    p.setComment(entry2.getKey(), entry2.getValue(), false);
                }
                OutputStream os = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, pkg, "Bundle.properties", elements).openOutputStream();
                try {
                    p.store(os);
                } finally {
                    os.close();
                }
                Map</*identifier*/String,/*method body*/String> methods = new TreeMap<String,String>();
                for (Map.Entry<String, String> entry2 : pairs.entrySet()) {
                    String key = entry2.getKey();
                    String value = entry2.getValue();
                    StringBuilder method = new StringBuilder();
                    method.append("    /**\n");
                    List<String> params = new ArrayList<String>();
                    int i = 0;
                    while (value.contains("{" + i)) {
                        params.add("arg" + i++);
                    }
                    String[] commentLines = comments.get(key);
                    if (commentLines != null) {
                        for (String comment : commentLines) {
                            Matcher m = Pattern.compile("# [{](\\d+)[}] - (.+)").matcher(comment);
                            if (m.matches()) {
                                i = Integer.parseInt(m.group(1));
                                while (i >= params.size()) {
                                    params.add("arg" + params.size());
                                }
                                String desc = m.group(2);
                                params.set(i, toIdentifier(desc));
                                method.append("     * @param ").append(params.get(i)).append(" ").append(toJavadoc(desc)).append("\n");
                            }
                        }
                    }
                    StringBuffer annotatedValue = new StringBuffer("<i>");
                    Matcher m = Pattern.compile("[{](\\d+)[}]").matcher(toJavadoc(value));
                    while (m.find()) {
                        i = Integer.parseInt(m.group(1));
                        m.appendReplacement(annotatedValue, i < params.size() ? "</i>{@code " + params.get(i) + "}<i>" : m.group());
                    }
                    m.appendTail(annotatedValue);
                    annotatedValue.append("</i>");
                    method.append("     * @return ").append(annotatedValue.toString().replace("<i></i>", "")).append('\n');
                    method.append("     * @see ").append(compilationUnits.get(key)).append('\n');
                    method.append("     */\n");
                    String name = toIdentifier(key);
                    method.append("    static String ").append(name).append("(");
                    boolean first = true;
                    i = 0;
                    for (String param : params) {
                        if (param.equals("arg" + i)) {
                            warnUndocumented(i, identifiers.get(name), key);
                        }
                        i++;
                        if (first) {
                            first = false;
                        } else {
                            method.append(", ");
                        }
                        method.append("Object ").append(param);
                    }
                    method.append(") {\n");
                    method.append("        return org.openide.util.NbBundle.getMessage(Bundle.class, \"").append(key).append("\"");
                    for (String param : params) {
                        method.append(", ").append(param);
                    }
                    method.append(");\n");
                    method.append("    }\n");
                    methods.put(name, method.toString());
                }
                try {
                    Set<String> restored = new TreeSet<String>();
                    Matcher m = Pattern.compile("    /[*][*]\r?\n(?:     [*].+\r?\n)+     [*] @see (?:[\\w-]+)\r?\n     [*]/\r?\n    static String (\\w+).+\r?\n        .+\r?\n    [}]\r?\n").matcher(processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, pkg, "Bundle.java").getCharContent(false));
                    while (m.find()) {
                        String identifier = m.group(1);
                        if (!methods.containsKey(identifier)) {
                            methods.put(identifier, m.group());
                            restored.add(identifier);
                        }
                    }
                    /*
                    if (!restored.isEmpty()) {
                        processingEnv.getMessager().printMessage(Kind.NOTE, "loaded " + pkg + ".Bundle identifiers " + restored + " from earlier run");
                    }
                    */
                } catch (IOException x) {
                    // OK, not there
                }
                String fqn = pkg + ".Bundle";
                Writer w = processingEnv.getFiler().createSourceFile(fqn, elements).openWriter();
                try {
                    PrintWriter pw = new PrintWriter(w);
                    pw.println("package " + pkg + ";");
                    pw.println("/** Localizable strings for {@link " + pkg + "}. */");
                    pw.println("@javax.annotation.Generated(value=\"" + NbBundleProcessor.class.getName() + "\")");
                    pw.println("class Bundle {");
                    for (String method : methods.values()) {
                        pw.print(method);
                    }
                    pw.println("    private void Bundle() {}");
                    pw.println("}");
                    pw.flush();
                    pw.close();
                } finally {
                    w.close();
                }
            } catch (IOException x) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Could not generate files: " + x, elements[0]);
            }
        }
        return true;
    }

    private String findPackage(Element e) {
        switch (e.getKind()) {
        case PACKAGE:
            return ((PackageElement) e).getQualifiedName().toString();
        default:
            return findPackage(e.getEnclosingElement());
        }
    }

    private String findCompilationUnitName(Element e) {
        switch (e.getKind()) {
        case PACKAGE:
            return "package-info";
        case CLASS:
        case INTERFACE:
        case ENUM:
        case ANNOTATION_TYPE:
            switch (e.getEnclosingElement().getKind()) {
            case PACKAGE:
                return e.getSimpleName().toString();
            }
        }
        return findCompilationUnitName(e.getEnclosingElement());
    }

    private String toIdentifier(String key) {
        if (BaseUtilities.isJavaIdentifier(key)) {
            return key;
        } else {
            String i = key.replaceAll("[^\\p{javaJavaIdentifierPart}]+", "_");
            if (BaseUtilities.isJavaIdentifier(i)) {
                return i;
            } else {
                return "_" + i;
            }
        }
    }

    private String toJavadoc(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace("*/", "&#x2A;/").replace("\n", "<br>").replace("@", "&#64;");
    }

    private void addToAnnotatedElements(Collection<? extends Element> unscannedElements, Set<Element> annotatedElements) {
        for (Element e : unscannedElements) {
            if (e.getAnnotation(NbBundle.Messages.class) != null) {
                annotatedElements.add(e);
            }
            if (e.getKind() != ElementKind.PACKAGE) {
                addToAnnotatedElements(e.getEnclosedElements(), annotatedElements);
            }
        }
    }

    private void warnUndocumented(int i, Element e, String key) {
        AnnotationMirror mirror = null;
        AnnotationValue value = null;
        if (e != null) {
            for (AnnotationMirror _mirror : e.getAnnotationMirrors()) {
                if (_mirror.getAnnotationType().toString().equals(NbBundle.Messages.class.getCanonicalName())) {
                    mirror = _mirror;
                    for (Map.Entry<? extends ExecutableElement,? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                        if (entry.getKey().getSimpleName().contentEquals("value")) {
                            // SimpleAnnotationValueVisitor6 unusable here since we need to determine the AnnotationValue in scope when visitString is called:
                            Object v = entry.getValue().getValue();
                            if (v instanceof String) {
                                if (((String) v).startsWith(key + "=")) {
                                    value = entry.getValue();
                                }
                            } else {
                                for (AnnotationValue subentry : NbCollections.checkedListByCopy((List<?>) v, AnnotationValue.class, true)) {
                                    v = subentry.getValue();
                                    if (v instanceof String) {
                                        if (((String) v).startsWith(key + "=")) {
                                            value = subentry;
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
        processingEnv.getMessager().printMessage(Kind.WARNING, "Undocumented format parameter {" + i + "}", e, mirror, value);
    }

}
