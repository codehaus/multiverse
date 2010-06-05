package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.collections.TransactionalArrayList;
import org.multiverse.transactional.collections.TransactionalLinkedList;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A phantom read is a read that returns inconsistent values based on the number of
 * atomicobjects. In databases you have the concept of table. But raw Java memory
 * doesn't have this concept. So we need to execute the readtest on some sort
 * of collection.
 *
 * @author Peter Veentjer
 */
public class PhantomReadStressTest {

    private volatile boolean stop;
    private int readThreadCount = 10;
    private int modifyThreadCount = 2;
    private List table;

    @Before
    public void setUp() {
        stop = false;
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    //todo: test with map
    //todo: test with set

    @Test
    public void testStrictTransactionalLinkedList() {
        test(new TransactionalLinkedList(1000000, true));
    }

    @Test
    public void testRelaxedTransactionalLinkedList() {
        test(new TransactionalLinkedList(1000000, false));
    }

    @Test
    public void testTransactionalArrayList() {
        test(new TransactionalArrayList());
    }

    public void test(List table) {
        this.table = table;
        ModifyThread[] modifyThreads = new ModifyThread[modifyThreadCount];
        for (int k = 0; k < modifyThreadCount; k++) {
            modifyThreads[k] = new ModifyThread(k);
        }

        ReadThread[] readerThread = new ReadThread[readThreadCount];
        for (int k = 0; k < readThreadCount; k++) {
            readerThread[k] = new ReadThread(k);
        }

        startAll(modifyThreads);
        startAll(readerThread);
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(modifyThreads);
        joinAll(readerThread);
    }

    class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() {
            while (!stop) {
                doLogic();
            }
        }

        //@TransactionalMethod(readonly = false)

        private void doLogic() {
            table.add("foo");
            sleepRandomMs(10);
            table.add("foo");
        }
    }

    public class ReadThread extends TestThread {
        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                switch (k % 4) {
                    case 0:
                        readWithUpdateTransaction();
                        break;
                    case 1:
                        readWithReadonlyTransaction();
                        break;
                    case 2:
                        readWithReadtrackingReadonlyTransaction();
                        break;
                    case 3:
                        readWithReadtrackingUpdateTransaction();
                        break;
                    default:
                        throw new IllegalStateException();
                }
                k++;
            }
        }

        @TransactionalMethod(readonly = false, trackReads = false)
        public void readWithUpdateTransaction() {
            readLogic();
        }

        @TransactionalMethod(readonly = true, trackReads = false)
        private void readWithReadonlyTransaction() {
            readLogic();
        }

        @TransactionalMethod(readonly = false, trackReads = true)
        public void readWithReadtrackingUpdateTransaction() {
            readLogic();
        }

        @TransactionalMethod(readonly = true, trackReads = true)
        private void readWithReadtrackingReadonlyTransaction() {
            readLogic();
        }

        private void readLogic() {
            int read1 = table.size();
            sleepRandomMs(10);
            int read2 = table.size();

            assertEquals(read1, read2);
        }
    }
}
