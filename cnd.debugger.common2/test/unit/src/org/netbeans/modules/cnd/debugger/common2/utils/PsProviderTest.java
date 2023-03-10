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
package org.netbeans.modules.cnd.debugger.common2.utils;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;
import org.junit.Test;
import org.netbeans.modules.cnd.debugger.common2.debugger.remote.Host;
import org.netbeans.modules.cnd.debugger.common2.debugger.test.CndBaseTestCase;

/**
 *
 * @author
 * Egor Ushakov
 */
public class PsProviderTest extends CndBaseTestCase {
    
    public PsProviderTest(String name) {
        super(name);
    }
    
    private PsProvider.PsData prepareWinData() {
        PsProvider provider = new PsProvider.WindowsPsProvider(Host.getLocal());
        PsProvider.PsData data = provider.new PsData();
        data.setHeader(provider.parseHeader("      PID    PPID    PGID     WINPID  TTY  UID    STIME COMMAND"));
        return data;
    }

    @Test
    public void testWinPs1() {
        PsProvider.PsData data = prepareWinData();
        data.addProcess("     5408       0       0       5408    ?    0 11:36:17 C:\\Program Files (x86)\\totalcmd\\TOTALCMD.EXE");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("C:\\Program Files (x86)\\totalcmd\\TOTALCMD.EXE", res.get(0).get(data.commandColumnIdx()));
        assertEquals("5408", res.get(0).get(0));
    }
    
    @Test
    public void testWinPs2() {
        PsProvider.PsData data = prepareWinData();
        data.addProcess("S    4316    6592    4316       5564    1 13352 13:54:32 /cygdrive/d/Projekty/moderngres-bin/bin/initdb");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("/cygdrive/d/Projekty/moderngres-bin/bin/initdb", res.get(0).get(data.commandColumnIdx()));
        assertEquals("4316", res.get(0).get(0));
    }
    
    @Test
    public void testWinPs3() {
        PsProvider.PsData data = prepareWinData();
        data.addProcess("I    6484    6760    6484       6400    0 13352 13:39:04 /usr/bin/bash");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("/usr/bin/bash", res.get(0).get(data.commandColumnIdx()));
        assertEquals("6484", res.get(0).get(0));
    }
    
    private PsProvider.PsData prepareSolarisData() {
        PsProvider provider = new PsProvider.SolarisPsProvider(Host.getLocal());
        PsProvider.PsData data = provider.new PsData();
        data.setHeader(provider.parseHeader("     UID   PID  PPID   C    STIME TTY         TIME CMD"));
        return data;
    }
    
    @Test
    public void testSolarisPs() {
        PsProvider.PsData data = prepareSolarisData();
        data.addProcess("    abcd 18719   994   1   Oct 05 pts/1     273:08 ./firefox");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("./firefox", res.get(0).get(data.commandColumnIdx()));
        assertEquals("18719", res.get(0).get(1));
    }
    
    @Test
    public void testSolarisArgs() { // CR 7116814
        PsProvider.PsData data = prepareSolarisData();
        data.addProcess("    abcd 12345   994   1   Oct 05 pts/1     273:08 ./firefox1");
        data.addProcess("    abcd 18719   994   1   Oct 05 pts/1     273:08 ./firefox2");
        ArrayList<String> pargs = new ArrayList<String>();
        pargs.add("");
        pargs.add("pargs: cannot examine 12345: no such process or core file");
        pargs.add("pargs: Couldn't determine locale of target process.");
        pargs.add("pargs: Some strings may not be displayed properly.");
        pargs.add("firefox2 a b c");
        PsProvider.updatePargsData(data, new String[]{"","12345","18719"}, pargs);
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("./firefox1", res.get(0).get(data.commandColumnIdx()));
        assertEquals("12345", res.get(0).get(1));
        assertEquals("firefox2 a b c", res.get(1).get(data.commandColumnIdx()));
        assertEquals("18719", res.get(1).get(1));
    }
    
    @Test
    public void testSolarisPsLong() {
        PsProvider.PsData data = prepareSolarisData();
        data.addProcess("longusername 18719   994   1   Oct 05 pts/1     273:08 ./firefox");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("./firefox", res.get(0).get(data.commandColumnIdx()));
        assertEquals("18719", res.get(0).get(1));
    }
    
    private PsProvider.PsData prepareLinuxData() {
        PsProvider provider = new PsProvider.LinuxPsProvider(Host.getLocal());
        PsProvider.PsData data = provider.new PsData();
        data.setHeader(provider.parseHeader("UID        PID  PPID  C STIME TTY          TIME CMD"));
        return data;
    }
    
    @Test
    public void testLinuxPs() {
        PsProvider.PsData data = prepareLinuxData();
        data.addProcess("tester   29270 29241  0 20:15 pts/2    00:00:00 ps -ef");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("ps -ef", res.get(0).get(data.commandColumnIdx()));
        assertEquals("29270", res.get(0).get(1));
    }
    
    @Test
    public void testLinuxPsLong() {
        PsProvider.PsData data = prepareLinuxData();
        data.addProcess("longusername 29270 29241  0 20:15 pts/2    00:00:00 ps -ef");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("ps -ef", res.get(0).get(data.commandColumnIdx()));
        assertEquals("29270", res.get(0).get(1));
    }
    
    private static String MAC_HEADER     = "  UID   PID  PPID   C     STIME TTY           TIME CMD";
    private static String MAC_107_HEADER = "  UID   PID  PPID   C STIME   TTY           TIME CMD";
    
    private PsProvider.PsData prepareMacData(String header) {
        PsProvider provider = new PsProvider.MacOSPsProvider(Host.getLocal());
        PsProvider.PsData data = provider.new PsData();
        data.setHeader(provider.parseHeader(header));
        return data;
    }
    
    @Test
    public void testMacPs() {
        PsProvider.PsData data = prepareMacData(MAC_HEADER);
        data.addProcess("    0   625   615   0   0:00.00 ttys000    0:00.00 ps -ef");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("ps -ef", res.get(0).get(data.commandColumnIdx()));
        assertEquals("625", res.get(0).get(1));
    }
    
    @Test
    public void testMacPsLong() {
        PsProvider.PsData data = prepareMacData(MAC_HEADER);
        data.addProcess("longusername   625   615   0   0:00.00 ttys000    0:00.00 ps -ef");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("ps -ef", res.get(0).get(data.commandColumnIdx()));
        assertEquals("625", res.get(0).get(1));
    }
    
    @Test // IZ 206862
    public void test107MacPs() {
        PsProvider.PsData data = prepareMacData(MAC_107_HEADER);
        data.addProcess("  502   632   631   0  7Nov11 ttys000    0:00.19 -bash");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("-bash", res.get(0).get(data.commandColumnIdx()));
        assertEquals("632", res.get(0).get(1));
    }
    
    @Test // IZ 206862
    public void test107MacPsLong() {
        PsProvider.PsData data = prepareMacData(MAC_107_HEADER);
        data.addProcess("longusername   632   631   0  7Nov11 ttys000    0:00.19 -bash");
        Vector<Vector<String>> res = data.processes(Pattern.compile(".*"));
        assertEquals("-bash", res.get(0).get(data.commandColumnIdx()));
        assertEquals("632", res.get(0).get(1));
    }
}
