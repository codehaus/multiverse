package org.multiverse.integrationtests.stability;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ConflictingWritesDontBreakSystemStressTest {
    private IntRef[] refs;

    private volatile boolean stop;
    private int structureCount = 100;
    private int writerThreadCount = 10;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        refs = new IntRef[structureCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new IntRef(0);
        }

        WriterThread[] threads = new WriterThread[writerThreadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WriterThread(k);
        }

        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        assertValues(sum(threads));
    }

    private void assertValues(int value) {
        for (IntRef ref : refs) {
            assertEquals(value, ref.get());
        }
    }

    public int sum(WriterThread[] threads) {
        int value = 0;
        for (WriterThread t : threads) {
            value += t.writeCount;
        }
        return value;
    }

    private class WriterThread extends TestThread {
        private int writeCount;

        private WriterThread(int id) {
            super("WriterThread-" + id);
        }

        @Override
        public void doRun() {
            while (!stop) {
                doTransaction();
                if (writeCount % 10 == 0) {
                    System.out.printf("%s is at %s\n", getName(), writeCount);
                }
                writeCount++;
            }
        }

        @TransactionalMethod
        public void doTransaction() {
            for (int k = 0; k < refs.length; k++) {
                IntRef ref = refs[k];
                sleepRandomMs(1);
                ref.inc();
            }
        }
    }
}
