package org.multiverse.integrationtests.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;


/**
 * A test that checks that committing transactions, won't cause deadlocks.
 * <p/>
 * Tests for direct deadlocks and deadlock chains.
 */
public class CommitWontDeadlockStressTest {

    private int txObjectCount = 100;
    private int threadCount = 10;
    private int transactionCountPerThread = 1000 * 1000;

    private TransactionalInteger[] refs;
    private ChangeThread[] threads;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        refs = new TransactionalInteger[txObjectCount];
        for (int k = 0; k < txObjectCount; k++) {
            refs[k] = new TransactionalInteger();
        }

        threads = new ChangeThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new ChangeThread(k);
        }
    }

    @Test
    public void test() {
        startAll(threads);
        joinAll(threads);
    }

    public class ChangeThread extends TestThread {

        public ChangeThread(int id) {
            super("ChangeThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCountPerThread; k++) {
                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                transaction();
            }
        }

        @TransactionalMethod
        public void transaction() {
            for (int k = 0; k < refs.length; k++) {
                if (randomInt(10) == 5) {
                    refs[k].inc();
                }
            }
        }
    }
}
