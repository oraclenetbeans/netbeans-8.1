/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jumpto.symbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Icon;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.spi.jumpto.symbol.SymbolDescriptor;
import org.netbeans.spi.jumpto.symbol.SymbolProvider;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Tomas Zezula
 */
public class ContentProviderImplTest extends NbTestCase {

    private MockSymbolProvider mockProvider;
    private CountingSymbolProvider countingProvider;

    public ContentProviderImplTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockServices.setServices(MockSymbolProvider.class, CountingSymbolProvider.class);
        mockProvider = MockSymbolProvider.getInstance();
        assertNotNull(mockProvider);
        countingProvider = CountingSymbolProvider.getInstance();
        assertNotNull(countingProvider);
        countingProvider.count.set(0);
    }

    @Override
    protected void tearDown() throws Exception {
        mockProvider = null;
        countingProvider = null;
        super.tearDown();
    }

    public void testNoRetry() throws Exception {
        final MockGoToPanel p = new MockGoToPanel();
        final SymbolDescriptor[] expected = new SymbolDescriptor[]{new MockSymbol("a1"), new MockSymbol("a2")};
        mockProvider.data(new SymbolDescriptor[][]{
            expected
        });
        final Runnable worker = new ContentProviderImpl(null).createWorker("a", SearchType.PREFIX, p);  //NOI18N
        worker.run();
        assertTrue(mockProvider.data.isEmpty());
        waitForEDT();
        assertEquals(1, p.called.get());
        assertSymbols(Arrays.asList(expected), p.symbols);
    }

    public void testSimpleRetry() throws Exception {
        final MockGoToPanel p = new MockGoToPanel();
        final SymbolDescriptor[] expected = new SymbolDescriptor[]{new MockSymbol("a1"), new MockSymbol("a2")};
        mockProvider.data(new SymbolDescriptor[][]{
            new SymbolDescriptor[]{expected[0]},
            expected
        });
        final Runnable worker = new ContentProviderImpl(null).createWorker("a", SearchType.PREFIX, p);  //NOI18N
        worker.run();
        assertTrue(mockProvider.data.isEmpty());
        waitForEDT();
        assertEquals(2, p.called.get());
        assertSymbols(Arrays.asList(expected), p.symbols);
    }

    public void testNotComputedYetRetry() throws Exception {
        final MockGoToPanel p = new MockGoToPanel();
        final SymbolDescriptor[] expected = new SymbolDescriptor[]{new MockSymbol("a1"), new MockSymbol("a2")};
        mockProvider.data(new SymbolDescriptor[][]{
            new SymbolDescriptor[]{expected[0]},
            new SymbolDescriptor[]{expected[0]},
            expected
        });
        final Runnable worker = new ContentProviderImpl(null).createWorker("a", SearchType.PREFIX, p);  //NOI18N
        worker.run();
        assertTrue(mockProvider.data.isEmpty());
        waitForEDT();
        assertEquals(2, p.called.get());
        assertSymbols(Arrays.asList(expected), p.symbols);
    }

    public void testSimpleRetryCompleteNotCalled() throws Exception {
        final MockGoToPanel p = new MockGoToPanel();
        final SymbolDescriptor[] expectedInMoc = new SymbolDescriptor[]{new MockSymbol("a1"), new MockSymbol("a2")};
        mockProvider.data(new SymbolDescriptor[][]{
            new SymbolDescriptor[]{expectedInMoc[0]},
            expectedInMoc
        });
        SymbolDescriptor[] expectedInCounting = new SymbolDescriptor[]{new MockSymbol("aa")};
        countingProvider.data(expectedInCounting);
        final Runnable worker = new ContentProviderImpl(null).createWorker("a", SearchType.PREFIX, p);  //NOI18N
        worker.run();
        assertTrue(mockProvider.data.isEmpty());
        assertEquals(1, countingProvider.count.get());
        waitForEDT();
        assertEquals(2, p.called.get());
        final List<SymbolDescriptor> expectedAll = new ArrayList<>(expectedInMoc.length + expectedInCounting.length);
        Collections.addAll(expectedAll, expectedInMoc);
        Collections.addAll(expectedAll, expectedInCounting);
        assertSymbols(expectedAll, p.symbols);
    }


    public static final class MockSymbolProvider implements SymbolProvider {

        private final AtomicBoolean canceled = new AtomicBoolean();
        private final Queue<SymbolDescriptor[]> data = new ConcurrentLinkedQueue<>();

        @Override
        public String name() {
            return "MOCK";  //NOI18N
        }

        @Override
        public String getDisplayName() {
            return name();
        }

        @Override
        public void computeSymbolNames(Context context, Result result) {
            canceled.set(false);
            SymbolDescriptor[] ds = data.remove();
            for (SymbolDescriptor d : ds) {
                result.addResult(d);
            }
            if (!data.isEmpty()) {
                result.pendingResult();
            }
        }

        @Override
        public void cancel() {
            canceled.set(true);
        }

        @Override
        public void cleanup() {
        }

        void data(SymbolDescriptor[][] res) {
            data.clear();
            for (SymbolDescriptor[] d : res) {
                data.offer(d);
            }
        }

        static MockSymbolProvider getInstance() {
            return Lookup.getDefault().lookup(MockSymbolProvider.class);
        }
    }

    public static final class CountingSymbolProvider implements SymbolProvider {

        private final AtomicInteger count = new AtomicInteger();
        private volatile SymbolDescriptor[] data;

        @Override
        public String name() {
            return "Counting Mock Symbol Provider"; //NOI18N
        }

        @Override
        public String getDisplayName() {
            return name();
        }

        @Override
        public void computeSymbolNames(Context context, Result result) {
            count.incrementAndGet();
            SymbolDescriptor[] res = data;
            if (res != null) {
                result.addResult(Arrays.asList(res));
            }
        }

        @Override
        public void cancel() {
        }

        @Override
        public void cleanup() {
        }

        void data(SymbolDescriptor[] res) {
            data = res;
        }

        static CountingSymbolProvider getInstance() {
            return Lookup.getDefault().lookup(CountingSymbolProvider.class);
        }
    }

    private static final class MockSymbol extends SymbolDescriptor {

        private final String name;

        MockSymbol(@NonNull final String name) {
            assert name != null;
            this.name = name;
        }

        @Override
        public String getSymbolName() {
            return getSimpleName();
        }

        @Override
        public String getSimpleName() {
            return name;
        }

        @Override
        public String getOwnerName() {
            return "";  //NOI18N
        }

        @Override
        public String getProjectName() {
            return null;
        }

        @Override
        public Icon getProjectIcon() {
            return null;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public FileObject getFileObject() {
            return null;
        }

        @Override
        public int getOffset() {
            return -1;
        }

        @Override
        public void open() {
        }
    }

    private static final class MockGoToPanel implements GoToPanel {
        private final Queue<SymbolDescriptor> symbols = new ConcurrentLinkedQueue<>();
        private final AtomicInteger called = new AtomicInteger();

        @Override
        public boolean isCaseSensitive() {
            return false;
        }

        @Override
        public boolean setModel(ListModel model) {
            called.incrementAndGet();
            symbols.clear();
            for (int i=0; i< model.getSize(); i++) {
                symbols.offer((SymbolDescriptor)model.getElementAt(i));
            }
            return true;
        }

        @Override
        public boolean revalidateModel() {
            return true;
        }

        @Override
        public void setWarning(String warningMessage) {
        }

        @Override
        public long getStartTime() {
            return -1;
        }
    }

    private void assertSymbols(
            @NonNull Collection<? extends SymbolDescriptor> expected,
            @NonNull Collection<? extends SymbolDescriptor> result) {
        final String[] exp = toSimpleNames(expected);
        final String[] res = toSimpleNames(result);
        assertTrue(
                "Expected: " + Arrays.toString(exp) +" Got: " + Arrays.toString(res),   //NOI18N
                Arrays.equals(exp, res));
    }

    private static String[] toSimpleNames(@NonNull Collection<? extends SymbolDescriptor> c) {
        final String[] r = new String[c.size()];
        final Iterator<? extends SymbolDescriptor> it = c.iterator();
        for (int i=0; i < r.length; i++) {
            r[i] = it.next().getSimpleName();
        }
        return r;
    }

    private static void waitForEDT() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                l.countDown();
            }
        });
        l.await();
    }
}
