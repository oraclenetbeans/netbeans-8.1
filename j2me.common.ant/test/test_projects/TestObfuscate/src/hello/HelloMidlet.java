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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

/*
 * HelloMidlet.java
 *
 * Created on 13. duben 2005, 19:27
 */
package hello;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 *
 * @author bohemius
 */
public class HelloMidlet extends MIDlet implements javax.microedition.lcdui.CommandListener {
    
    /** Creates a new instance of HelloMidlet */
    public HelloMidlet() {
    }
    
// --- This section is auto-generated by NetBeans IDE. Do not modify or you may lose your changes.//<editor-fold id="MVDMethods" defaultstate="collapsed" desc="This section is auto-generated by NetBeans IDE.">//GEN-BEGIN:MVDMethods
    /**
     * This method initializes UI of the application.
     */
    private void initialize() {
// For adding user code into this block, select "Design" item in the inspector and invoke property editor on Action property in Properties window.
        javax.microedition.lcdui.Display.getDisplay(this).setCurrent(get_helloForm());
    }
    
    /**
     * Called by the system to indicate that a command has been invoked on a particular displayable.
     * @param command the Command that ws invoked
     * @param displayable the Displayable on which the command was invoked
     **/
    public void commandAction(javax.microedition.lcdui.Command command, javax.microedition.lcdui.Displayable displayable) {
        if (displayable == helloForm) {
            if (command == exitCommand) {
// For adding user code into this block, select "Design | Screens | helloForm [Form] | Assigned Commands | exitCommand" item in the inspector and invoke property editor on Action property in Properties window.
                javax.microedition.lcdui.Display.getDisplay(this).setCurrent(null);
                destroyApp(true);
                notifyDestroyed();
            }
        }
    }
    
    /**
     * This method returns instance for helloForm component and should be called instead of accessing helloForm field directly.
     * @return Instance for helloForm component
     **/
    private javax.microedition.lcdui.Form get_helloForm() {
        if (helloForm == null) {
            helloForm = new javax.microedition.lcdui.Form(null, new javax.microedition.lcdui.Item[] {get_helloStringItem()});
            helloForm.addCommand(get_exitCommand());
            helloForm.setCommandListener(this);
        }
        return helloForm;
    }
    
    /**
     * This method returns instance for helloStringItem component and should be called instead of accessing helloStringItem field directly.
     * @return Instance for helloStringItem component
     **/
    private javax.microedition.lcdui.StringItem get_helloStringItem() {
        if (helloStringItem == null) {
            helloStringItem = new javax.microedition.lcdui.StringItem("Hello", "Hello, World!");
        }
        return helloStringItem;
    }
    
    /**
     * This method returns instance for exitCommand component and should be called instead of accessing exitCommand field directly.
     * @return Instance for exitCommand component
     **/
    private javax.microedition.lcdui.Command get_exitCommand() {
        if (exitCommand == null) {
            exitCommand = new javax.microedition.lcdui.Command("Exit", javax.microedition.lcdui.Command.EXIT, 1);
        }
        return exitCommand;
    }
    
    javax.microedition.lcdui.Form helloForm;
    javax.microedition.lcdui.StringItem helloStringItem;
    javax.microedition.lcdui.Command exitCommand;
// --- This is the end of auto-generated section.//</editor-fold>//GEN-END:MVDMethods
    
    public void startApp() {
        initialize();
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
    }
    
}
