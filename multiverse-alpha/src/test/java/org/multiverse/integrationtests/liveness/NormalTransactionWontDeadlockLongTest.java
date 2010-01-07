package org.multiverse.integrationtests.liveness;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A Tests that makes sure that normaly Transactions are not the subject to deadlocks. Normally resources are locked and
 * this could lead to a deadlock. With TL2Stm resources are locked only for a small amount of time, if the lock can't be
 * acquired, all locks are released.
 *
 * @author Peter Veentjer.
 */
public class NormalTransactionWontDeadlockLongTest {

    private int threadCount = 20;
    private int resourceCount = 10;
    private int transactionCount = 200;
    private TransactionalInteger[] refs;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);

        refs = new TransactionalInteger[resourceCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new TransactionalInteger();
        }
    }

    @After
    public void tearDown() {
        //    stm.getProfiler().print();
    }

    @Test
    public void noReadTracking() {
        ModifyThread[] threads = createThreads();
        startAll(threads);
        joinAll(threads);
    }

    public ModifyThread[] createThreads() {
        ModifyThread[] threads = new ModifyThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ModifyThread(k);
        }
        return threads;
    }

    public class ModifyThread extends TestThread {

        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Test
        public void doRun() {
            for (int k = 0; k < transactionCount; k++) {
                doit();
                if (k % 25 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod
        private void doit() {
            TransactionalInteger value1 = refs[randomInt(refs.length - 1)];
            value1.inc();
            sleepMs(25);
            TransactionalInteger value2 = refs[randomInt(refs.length - 1)];
            value2.inc();
        }
    }
}