package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
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

    private int readCount = 1000;
    private int readThreadCount = 10;
    private int modifyThreadCount = 2;
    private List table;
    private volatile boolean readersFinished;

    @Before
    public void setUp() {
        readersFinished = false;
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
        joinAll(modifyThreads);
        joinAll(readerThread);
    }

    class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!readersFinished) {
                doLogic();
                k++;

                if (k % 500 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
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
            for (int k = 0; k < readCount; k++) {
                boolean readonly = k % 2 == 0;
                if (readonly) {
                    readWithReadonlyTransaction();
                } else {
                    readWithUpdateTransaction();
                }

                if (k % 500 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            readersFinished = true;
        }

        @TransactionalMethod(readonly = false)
        public void readWithUpdateTransaction() {
            readLogic();
        }

        @TransactionalMethod(readonly = true)
        private void readWithReadonlyTransaction() {
            readLogic();
        }

        private void readLogic() {
            int sizeT1 = table.size();
            sleepRandomMs(10);
            int sizeT2 = table.size();

            assertEquals(sizeT1, sizeT2);
        }
    }
}
