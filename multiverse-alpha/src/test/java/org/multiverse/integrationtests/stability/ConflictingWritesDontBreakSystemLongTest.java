package org.multiverse.integrationtests.stability;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class ConflictingWritesDontBreakSystemLongTest {
    private TransactionalInteger[] refs;

    private int structureCount = 100;
    private int writerThreadCount = 10;
    private int transactionCount = 100;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //stm.getProfiler().print();
        setThreadLocalTransaction(null);
    }

    @Test
    public void test() {
        setUpStructures();

        WriterThread[] threads = createWriterThreads();
        startAll(threads);
        joinAll(threads);

        //the 10 is quite arbitrary.. but we should have quite a number of conflicts.
        //stm.getProfiler().print();
        //assertTrue(stm.getStatistics().getUpdateTransactionWriteConflictCount() > 10);
        assertValues(transactionCount * 10);
    }

    private void assertValues(int value) {
        for (TransactionalInteger ref : refs) {
            assertEquals(value, ref.get());
        }
    }

    private void setUpStructures() {
        refs = new TransactionalInteger[structureCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new TransactionalInteger(0);
        }
    }

    private WriterThread[] createWriterThreads() {
        WriterThread[] threads = new WriterThread[writerThreadCount];
        for (int k = 0; k < threads.length; k++)
            threads[k] = new WriterThread(k);

        return threads;
    }

    private class WriterThread extends TestThread {
        private WriterThread(int id) {
            super("WriterThread-" + id);
        }

        @Override
        public void doRun() {
            for (int k = 0; k < transactionCount; k++) {
                if (k % 10 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                doTransaction();
            }
        }

        @TransactionalMethod
        public void doTransaction() {
            for (int k = 0; k < refs.length; k++) {
                TransactionalInteger ref = refs[k];
                sleepRandomMs(1);
                ref.inc();
            }
        }
    }
}
