package org.multiverse.integrationtests.liveness;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Tests that makes sure that normaly Transactions are not the subject to deadlocks. Normally resources are locked and
 * this could lead to a deadlock. With TL2Stm resources are locked only for a small amount of time, if the lock can't be
 * acquired, all locks are released.
 *
 * @author Peter Veentjer.
 */
public class NormalTransactionWontDeadlockStressTest {

    private int threadCount = 20;
    private int resourceCount = 10;
    private int transactionCount = 200;
    private IntRef[] refs;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        refs = new IntRef[resourceCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new IntRef();
        }
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
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

        @Override
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
            IntRef value1 = refs[randomInt(refs.length - 1)];
            value1.inc();
            sleepMs(25);
            IntRef value2 = refs[randomInt(refs.length - 1)];
            value2.inc();
        }
    }
}