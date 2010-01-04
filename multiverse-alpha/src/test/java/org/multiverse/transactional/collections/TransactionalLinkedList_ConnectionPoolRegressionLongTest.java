package org.multiverse.transactional.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;

/**
 * There was an issue with the {@link org.multiverse.integrationtests.notification.ConnectionPoolLongTest} that returned nulls with a
 * takeFirst and putLast. These tests are done to locate to bug.
 *
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_ConnectionPoolRegressionLongTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    /**
     * A test that check if concurrent puts only (so no takes) and also no thread notification (retry) is used. If this
     * test fails, we know that condition variables and also the take functionality are not causing the problem.
     */
    @Test
    public void concurrentPutsFirst() {
        concurrentPuts(true);
    }


    @Test
    public void concurrentPutLast() {
        concurrentPuts(false);
    }

    public void concurrentPuts(boolean first) {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        //todo
        //produceCount needs to be made bigger once the assertStructureCheck in the
        //TransactionalList has been removed.
        int produceCount = 400000;
        int threadCount = 2;

        PutFirstThread[] threads = new PutFirstThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PutFirstThread(k, produceCount, list, first);
        }

        startAll(threads);
        joinAll(threads);

        Set<String> expected = new HashSet<String>();
        for (PutFirstThread thread : threads) {
            expected.addAll(thread.producedList);
        }

        Set<String> found = new HashSet<String>();
        found.addAll(list);

        assertEquals(expected, found);
    }

    public class PutFirstThread extends TestThread {

        final int produceCount;
        final TransactionalLinkedList<String> list;
        final List<String> producedList = new LinkedList();
        final boolean first;

        public PutFirstThread(int id, int produceCount, TransactionalLinkedList<String> list, boolean first) {
            super("PutFirstThread-" + id);
            this.produceCount = produceCount;
            this.list = list;
            this.first = first;
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < produceCount; k++) {
                String item = String.valueOf(System.nanoTime());

                if (k % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                if (first) {
                    list.putFirst(item);
                } else {
                    list.putLast(item);
                }
                producedList.add(item);
            }
        }
    }

    @Test
    public void realisticInterleavingPutsAndTakes() throws InterruptedException {
        BlockingDeque<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        Set<String> producedItems = new HashSet<String>();
        Set<String> consumedItems = new HashSet<String>();
        for (int k = 0; k < 1000; k++) {
            if (randomBoolean()) {
                String item = list.poll();
                if (item != null) {
                    consumedItems.add(item);
                }
            } else {
                String newItem = String.valueOf(producedItems.size());
                producedItems.add(newItem);
                list.putLast(newItem);
            }
        }

        int diff = producedItems.size() - consumedItems.size();
        for (int k = 0; k < diff; k++) {
            String item = list.takeFirst();
            consumedItems.add(item);
        }

        assertEquals(version + producedItems.size() * 2, stm.getVersion());
        assertEquals(producedItems, consumedItems);
        assertEquals("[]", list.toString());
    }

    @Test
    public void interleavingPutAndTakes() throws InterruptedException {
        BlockingDeque<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        String item1 = "1";
        String item2 = "2";
        String item3 = "3";
        String item4 = "4";
        list.putLast(item1);
        assertSame(item1, list.takeFirst());
        list.putLast(item2);
        assertSame(item2, list.takeFirst());
        list.putLast(item3);
        list.putLast(item4);

        assertSame(item3, list.takeFirst());
        assertSame(item4, list.takeFirst());

        assertEquals(version + 8, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void nonInterleavingPutsAndTakes() throws InterruptedException {
        BlockingDeque<String> list = new TransactionalLinkedList<String>();
        long version = stm.getVersion();

        String item1 = "1";
        String item2 = "2";
        String item3 = "3";
        String item4 = "4";
        list.putLast(item1);
        list.putLast(item2);
        list.putLast(item3);
        list.putLast(item4);

        assertSame(item1, list.takeFirst());
        assertSame(item2, list.takeFirst());
        assertSame(item3, list.takeFirst());
        assertSame(item4, list.takeFirst());

        assertEquals(version + 8, stm.getVersion());
        assertEquals("[]", list.toString());
    }
}
