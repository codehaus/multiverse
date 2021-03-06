package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CountDownCommitBarrier_IntegrationTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void testMultipleWaiters() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(2);
        AwaitThread t1 = new AwaitThread(barrier);
        t1.start();

        sleepMs(1000);
        assertTrue(t1.isAlive());

        AwaitThread t2 = new AwaitThread(barrier);
        t2.start();

        joinAll(t1, t2);
        assertEquals(0, barrier.getNumberWaiting());
        assertIsCommitted(t2.tx);
        assertIsCommitted(t1.tx);
    }

    @Test
    public void testSingleWaiter() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        AwaitThread t1 = new AwaitThread(barrier);
        t1.start();

        joinAll(t1);
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isCommitted());
        assertIsCommitted(t1.tx);
    }

    public class AwaitThread extends TestThread {
        private Transaction tx;
        private final CountDownCommitBarrier barrier;


        public AwaitThread(CountDownCommitBarrier barrier) {
            super("AwaitThread");
            this.barrier = barrier;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            //make the transaction dirty.
            IntRef dummy = new IntRef();

            tx = getThreadLocalTransaction();
            barrier.joinCommitUninterruptibly(tx);
        }
    }
}
