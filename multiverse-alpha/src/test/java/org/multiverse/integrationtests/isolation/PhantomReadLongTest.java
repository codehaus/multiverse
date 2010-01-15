package org.multiverse.integrationtests.isolation;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.transactional.DefaultTransactionalReference;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;

/**
 * A phantom read is a read that returns inconsistent values based on the number of atomicobjects. In
 * databases you have the concept of table. But raw Java memory doesn't have this concept. So we need
 * to execute the readtest on some sort of collection.
 *
 * @author Peter Veentjer
 */
public class PhantomReadLongTest {

    private int readCount = 10000;
    private int readThreadCount = 10;
    private int modifyThreadCount = 2;
    private int tableLength = 1000;
    private DefaultTransactionalReference<Integer>[] table;
    private volatile boolean readersFinished = false;


    @Test
    public void setUp() {
        ThreadLocalTransaction.setThreadLocalTransaction(null);
        table = new DefaultTransactionalReference[tableLength];
        for (int k = 0; k < table.length; k++) {
            table[k] = new DefaultTransactionalReference<Integer>();
        }
    }

    @Ignore
    @Test
    public void test() {
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

        testIncomplete();
    }

    class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!readersFinished) {
                if (k % 500 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;

                sleepRandomMs(2);
            }
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
            }
        }

        @TransactionalMethod(readonly = false)
        public void readWithUpdateTransaction() {
            readLogic();
        }

        @TransactionalMethod(readonly = false)
        private void readWithReadonlyTransaction() {
            readLogic();
        }

        private void readLogic() {
            //todo
            int firstCount = 0;
            int secondCount = 0;

            assertEquals(firstCount, secondCount);
        }
    }


}
