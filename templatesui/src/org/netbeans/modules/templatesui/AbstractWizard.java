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
package org.netbeans.modules.templatesui;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.html.boot.fx.FXBrowsers;
import net.java.html.js.JavaScriptBody;
import net.java.html.json.Model;
import net.java.html.json.Models;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 */
@Model(className = "InitWizard", targetId = "", properties = {
})
abstract class AbstractWizard 
implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {
    private static final Logger LOG = Logger.getLogger(AbstractWizard.class.getName());
    
    private int index;
    private List<String> steps = Collections.emptyList();
    private List<String> stepNames = Collections.emptyList();
    private String current;
    private List<WizardDescriptor.Panel<WizardDescriptor>> panels;
    private Object data;
    private JFXPanel p;
    private /* final */ WebView v;
    private ChangeListener listener;
    private int errorCode = 0;
    private WizardDescriptor wizard;
    
    protected abstract Object initSequence(ClassLoader l) throws Exception;
    protected abstract URL initPage(ClassLoader l);
    protected abstract void initializationDone(Throwable error);
    protected abstract String[] getTechIds();

    @Override
    public Set<? extends Object> instantiate() throws IOException {
        try {
            FutureTask<?> t = new FutureTask<>(new Callable<Map<String,Object>>() {
                @Override
                public Map<String,Object> call() throws Exception {
                    Object[] namesAndValues = rawProps(data);
                    Map<String,Object> map = new TreeMap<>();
                    for (int i = 0; i < namesAndValues.length; i += 2) {
                        String name = (String) namesAndValues[i];
                        Object value = namesAndValues[i + 1];
                        map.put(name, value);
                    }
                    return map;
                }
            });
            FXBrowsers.runInBrowser(v, t);
            
            TemplateWizard tw = (TemplateWizard) wizard;
            Map<String, ? extends Object> params = Collections.singletonMap(
                "wizard", t.get()
            );
            DataObject obj = tw.getTemplate().createFromTemplate(tw.getTargetFolder(), tw.getTargetName(), params);
            return Collections.singleton(obj);
        } catch (Exception ex) {
            throw (IOException)new InterruptedIOException().initCause(ex);
        }
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        this.wizard = null;
    }

    
    private List<? extends WizardDescriptor.Panel<WizardDescriptor>> getPanels() {
        panels = new ArrayList<>();
        fillPanels((TemplateWizard)wizard, this, panels, steps);
        return Collections.unmodifiableList(panels);
    }
    
    static void fillPanels(
        TemplateWizard wizard, AbstractWizard aw,
        List<WizardDescriptor.Panel<WizardDescriptor>> panels, List<String> steps
    ) {
        int cnt = steps.size();
        if (cnt == 0) {
            cnt = 1;
        }
        for (int i = 0; i < cnt; i++) {
            if (steps.size() > i) {
                final String panelName = steps.get(i);
                if ("targetChooser".equals(panelName)) { // NOI18N
                    panels.add(wizard.targetChooser());
                    continue;
                }
                final String tcPrefix = "targetChooser:"; // NOI18N
                if (panelName != null && panelName.startsWith(tcPrefix)) {
                    WizardDescriptor.Panel<WizardDescriptor> panel = aw.getChooser(wizard, panelName.substring(tcPrefix.length()));
                    panels.add(panel);
                    continue;
                }
            }
            final HTMLPanel p = new HTMLPanel(i, aw);
            panels.add(p);
        }
    }
    
    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return getPanels().get(index);
    }

    @NbBundle.Messages({
        "# {0} - current index",
        "# {1} - number of panels",
        "MSG_HTMLWizardName={0} of {1}"
    })
    @Override
    public String name() {
        return Bundle.MSG_HTMLWizardName(index + 1, getPanels().size());
    }

    @Override
    public boolean hasNext() {
        return index < getPanels().size() - 1;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
        onStepsChange(null);
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
        onStepsChange(null);
    }

    @Override
    public synchronized void addChangeListener(ChangeListener l) {
        assert this.listener == null;
        this.listener = l;
    }

    @Override
    public synchronized void removeChangeListener(ChangeListener l) {
        if (this.listener == l) {
            this.listener = null;
        }
    }
    
    private void fireChange() {
        ChangeListener l;
        synchronized (this) {
            l = this.listener;
            notifyAll();
        }
        if (l != null) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    final JComponent component(final int index) {
        if (p == null) {
            p = new JFXPanel();
            p.setPreferredSize(new Dimension(500, 340));
            p.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
            p.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
            p.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (v == null) {
                        Platform.setImplicitExit(false);
                        try {
                            // workaround for 
                            // https://javafx-jira.kenai.com/browse/RT-38536
                            Class.forName("com.sun.javafx.image.impl.ByteBgra");
                        } catch (ClassNotFoundException ignore) {
                        }
                        v = new WebView();
                        BorderPane bp = new BorderPane();
                        Scene scene = new Scene(bp, Color.ALICEBLUE);
                        bp.setCenter(v);
                        p.setScene(scene);

                        ClassLoader tmpL = Lookup.getDefault().lookup(ClassLoader.class);
                        if (tmpL == null) {
                            tmpL = Thread.currentThread().getContextClassLoader();
                        }
                        if (tmpL == null) {
                            tmpL = HTMLPanel.class.getClassLoader();
                        }
                        
                        final ClassLoader l = tmpL;
                        
                        URL u = initPage(l);
                        
                        FXBrowsers.load(v, u, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    v.setContextMenuEnabled(false);
                                    Models.toRaw(new InitWizard());
                                    Object ret = initSequence(l);
                                    
                                    if (ret instanceof String) {
                                        data = v.getEngine().executeScript((String) ret);
                                        if (data == null || "undefined".equals(data)) {
                                            throw new IllegalArgumentException("Executing " + ret + " returned null, that is wrong, should get JSON object with ko bindings");
                                        }
                                    } else {
                                        if (ret != null && Models.isModel(ret.getClass())) {
                                            data = Models.toRaw(ret);
                                        } else {
                                            throw new IllegalStateException("Returned value should be string or class generated by @Model annotation: " + ret);
                                        }
                                    }
                                    registerStepHandler(data);
                                    
                                    boolean stepsOK = listenOnProp(data, AbstractWizard.this, "steps");
                                    boolean errorCodeOK = listenOnProp(data, AbstractWizard.this, "errorCode");

                                    applyBindings(data);
                                    initializationDone(null);
                                } catch (Exception ex) {
                                    initializationDone(ex);
                                } catch (Error ex) {
                                    initializationDone(ex);
                                }
                            }
                        }, l, (Object[])getTechIds());
                    }
                }
            });
        }
        if (index < stepNames.size()) {
            p.setName(stepNames.get(index));
        }
        return p;
    }
    
    
    final void onChange(String prop, Object data) {
        if ("steps".equals(prop)) {
            onStepsChange((Object[])data);
        }
        if ("errorCode".equals(prop)) {
            errorCode = data instanceof Number ? ((Number)data).intValue() : -1;
            fireChange();
        }
    }

    boolean isValid() {
        return errorCode == 0;
    }
    
    final Object executeScript(final String prop) throws InterruptedException, ExecutionException {
        FutureTask<?> t = new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return v.getEngine().executeScript(prop);
            }
        });
        FXBrowsers.runInBrowser(v, t);
        return t.get();
    }
    final Object evaluateCall(final Object fn, final Object... args) throws InterruptedException, ExecutionException {
        FutureTask<?> t = new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return callFn(fn, args);
            }
        });
        FXBrowsers.runInBrowser(v, t);
        return t.get();
    }
    final Object evaluateProp(final String prop) throws InterruptedException, ExecutionException {
        FutureTask<?> t = new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return getPropertyValue(data, prop);
            }
        });
        FXBrowsers.runInBrowser(v, t);
        return t.get();
    }
    
    final void setProp(final String prop, final Object value) throws InterruptedException, ExecutionException {
        FutureTask<?> t = new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return changeProperty(data, prop, value);
            }
        });
        FXBrowsers.runInBrowser(v, t);
        t.get();
    }
    
    final Object data() {
        return data;
    }
    
    final String[] steps(boolean localized) {
        return (localized ? stepNames : steps).toArray(new String[0]);
    }
    
    final String currentStep() {
        return current;
    }
    
   @NbBundle.Messages({
        "LBL_TemplatesPanel_Name=Choose File Type",
        "LBL_TargetPanel_Name=Name and Location"
    })
   private void onStepsChange(Object[] obj) {
        if (obj != null) {
            List<String> arr = new ArrayList<>();
            for (Object s : obj) {
                arr.add(stringOrId(s, "id", null)); // NOI18N
            }
            if (!arr.equals(steps)) {
                steps = arr;
                fireChange();
            }
            List<String> names = new ArrayList<>();
            for (Object s : obj) {
                String id = stringOrId(s, "text", "id"); // NOI18N
                if (id != null && id.equals("targetChooser") || id.startsWith("targetChooser:")) { // NOI18N
                    id = Bundle.LBL_TargetPanel_Name();
                }
                names.add(id);
            }
            stepNames = new ArrayList<>(names);
            names.add(0, Bundle.LBL_TemplatesPanel_Name());
            p.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, names.toArray(new String[names.size()]));
            fireChange();
        }
        p.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, index);
        if (steps != null && steps.size() > index) {
            current = steps.get(index);
            FXBrowsers.runInBrowser(v, new Runnable() {
                @Override
                public void run() {
                    changeProperty(data, "current", current); // NOI18N
                }
            });
        }
    }

    boolean validationRequested;
    boolean prepareValidation() {
        FutureTask<Boolean> t = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return validationRequested = callValidate(data);
            }
        });
        FXBrowsers.runInBrowser(v, t);
        try {
            return t.get();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    synchronized void waitForValidation() throws WizardValidationException {
        if (!validationRequested) {
            return;
        }
        while (errorCode == -1) {
            try {
                wait();
            } catch (InterruptedException ex) {
                LOG.log(Level.INFO, null, ex);
            }
        }
        if (errorCode != 0) {
            throw new WizardValidationException(p, null, null);
        }
    }
    
    @JavaScriptBody(args = { "arr" }, body = 
        "for (var i = 0; i < arr.length; i++) {\n" +
        "  arr[i]();\n" +
        "}\n" +
        ""
    )
    native void invokeFn(Object[] arr);
    
    @JavaScriptBody(args = { "raw" }, body = 
        "if (raw.errorCode() !== -1) return false;" +
        "if (raw.validate) {" +
        "  raw.validate();" +
        "  return true;" +
        "}" +
        "return false;"
    )
    static native boolean callValidate(Object raw);
   
    @JavaScriptBody(args = {"data", "onChange", "p" }, 
        javacall = true, body = ""
        + "if (typeof data[p] !== 'function') {\n"
        + "  throw 'Type of property ' + p + ' should be a function!';\n"
        + "}\n"
        + "data[p].subscribe(function(value) {\n"
        + "  onChange.@org.netbeans.modules.templatesui.AbstractWizard::onChange(Ljava/lang/String;Ljava/lang/Object;)(p, value);\n"
        + "});\n"
        + "onChange.@org.netbeans.modules.templatesui.AbstractWizard::onChange(Ljava/lang/String;Ljava/lang/Object;)(p, data[p]());\n"
        + "return true;\n"
    )
    static native boolean listenOnProp(
        Object raw, AbstractWizard onChange, String propName
    );
    
    @JavaScriptBody(args = { "raw", "propName", "value" }, body = ""
        + "var fn = raw[propName];\n"
        + "if (typeof fn !== 'function') return false;\n"
        + "fn(value);\n"
        + "return true;\n"
    )
    private static native boolean changeProperty(Object raw, String propName, Object value);

    @JavaScriptBody(args = { "fn", "arr" }, body = ""
        + "return fn.apply(null, arr);"
    )
    private static native Object callFn(Object fn, Object[] arr);
    
    @JavaScriptBody(args = { "raw", "propName" }, body = ""
        + "var fn = raw[propName];\n"
        + "if (typeof fn !== 'function') return null;\n"
        + "return fn();\n"
    )
    static native Object getPropertyValue(Object raw, String propName);
    
    @JavaScriptBody(args = { "raw" }, body = ""
        + "var ret = [];\n"
        + "for (var n in raw) {\n"
        + "  if (n === 'current') continue;\n"
        + "  if (n === 'errorCode') continue;\n"
        + "  if (n === 'steps') continue;\n"
        + "  var fn = raw[n];\n"
        + "  ret.push(n);\n"
        + "  if (typeof fn === 'function') ret.push(fn()); else ret.push(fn);\n"
        + "}\n"
        + "return ret;\n"
    )
    static native Object[] rawProps(Object raw);
    
    @JavaScriptBody(args = { "obj", "id", "fallback" }, body = 
        "if (typeof obj === 'string') return obj;\n"
      + "if (obj[id]) return obj[id].toString();\n"
      + "if (fallback && obj[fallback]) return obj[fallback].toString();\n"
      + "return null;\n"
    )
    static native String stringOrId(Object obj, String id, String fallback);
    
    @JavaScriptBody(args = { "raw" }, body = ""
        + "var current = raw.current || (raw.current = ko.observable());\n"
        + "var steps = raw.steps || (raw.steps = ko.observableArray());\n"
        + "if (!raw.errorCode) raw.errorCode = ko.computed(function() {\n"
        + "  return 1;\n"
        + "});\n"
        + "ko.bindingHandlers.step = {\n"
        + "  init : function(element, valueAccessor, allBindings, viewModel, bindingContext) {\n"
        + "    steps.push(valueAccessor());\n"
        + "  },\n"
        + "  update : function(element, valueAccessor, allBindings, viewModel, bindingContext) {\n"
        + "    var v = valueAccessor();\n"
        + "    if (typeof v !== 'string') v = v.id;\n"
        + "    if (current() === v) {\n"
        + "      element.style.display = '';\n"
        + "    } else {\n"
        + "      element.style.display = 'none';\n"
        + "    }\n;\n"
        + "  }\n"
        + "};\n"
    )
    static native void registerStepHandler(Object raw);
    
    @JavaScriptBody(args = { "raw" }, body = "ko.applyBindings(raw);")
    static native void applyBindings(Object raw);

    Map<String,WizardDescriptor.Panel<WizardDescriptor>> choosers;
    WizardDescriptor.Panel<WizardDescriptor> getChooser(TemplateWizard wizard, String type) {
        if (choosers == null) {
            choosers = new HashMap<>();
        }
        WizardDescriptor.Panel<WizardDescriptor> panel = choosers.get(type);
        
        if (panel == null) {
            try {
                ClassLoader l = Lookup.getDefault().lookup(ClassLoader.class);
                if (l == null) {
                    l = Thread.currentThread().getContextClassLoader();
                }
                if (l == null) {
                    l = AbstractWizard.class.getClassLoader();
                }
                Class<?> clazz = Class.forName("org.netbeans.spi.java.project.support.ui.templates.JavaTemplates", true, l); // NOI18N
                Method create = clazz.getDeclaredMethod("createPackageChooser", Object.class, String.class); // NOI18N
                create.setAccessible(true);
                panel = (WizardDescriptor.Panel<WizardDescriptor>) create.invoke(
                    null, wizard.getProperty("project"), type // NOI18N
                );
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Cannot create targetChooser for type " + type + " using default. "
                    + "Don't forget to include org.netbeans.modules.java.project.ui module in your application.", t
                );
                panel = wizard.targetChooser();
            }
            choosers.put(type, panel);
        }
        return panel;
    }
}
