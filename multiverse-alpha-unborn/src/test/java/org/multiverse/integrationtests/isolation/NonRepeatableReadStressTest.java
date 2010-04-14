package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A test that checks if reads are repeatable.
 * <p/>
 * Within a single transaction the same value should be returned every time. In databases the repeatable read is one of
 * the highest isolation (serializable is higher).
 * <p/>
 * It works by having a shared value. This value is modified by modifier threads in very short transactions. Readers
 * read this value, wait some time and reread the value (within the same transaction) and the values should not have
 * changed.  If the value has changed the system is suffering from non repeatable reads.
 * <p/>
 * The test checks this behavior for real readonly reads, and update transactions that only read, by alternating between
 * the 2 options.
 *
 * @author Peter Veentjer.
 */
public class NonRepeatableReadStressTest {

    private TransactionalInteger ref;
    private int readCount = 10000;
    private int readThreadCount = 5;
    private int modifyThreadCount = 2;
    private volatile boolean readersFinished;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        ref = new TransactionalInteger(0);
        readersFinished = false;
    }

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
    }

    class ModifyThread extends TestThread {

        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!readersFinished) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;
                ref.inc();

                sleepRandomMs(5);
            }
        }
    }

    class ReadThread extends TestThread {

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() {
            for (int k = 0; k < readCount; k++) {
                if (k % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                if (k % 2 == 0) {
                    readUsingUpdateTransaction();
                } else {
                    readUsingReadonlyTransaction();
                }
            }
            readersFinished = true;
        }

        @TransactionalMethod(readonly = true)
        private void readUsingReadonlyTransaction() {
            read();
        }

        @TransactionalMethod(readonly = false)
        private void readUsingUpdateTransaction() {
            read();
        }

        private void read() {
            int firstTime = ref.get();
            sleepRandomMs(2);
            int secondTime = ref.get();
            assertEquals(firstTime, secondTime);
        }
    }
}
