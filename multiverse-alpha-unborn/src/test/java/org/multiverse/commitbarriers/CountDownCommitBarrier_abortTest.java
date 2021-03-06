package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CountDownCommitBarrier_abortTest {
    private CountDownCommitBarrier barrier;

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
    public void whenNoPartiesWaiting() {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenPartiesWaiting_theyAreAborted() {
        barrier = new CountDownCommitBarrier(3);

        CommitThread thread1 = new CommitThread(barrier);
        CommitThread thread2 = new CommitThread(barrier);
        startAll(thread1, thread2);

        sleepMs(500);
        barrier.abort();
        sleepMs(500);

        assertTrue(barrier.isAborted());
        assertIsAborted(thread1.tx);
        assertIsAborted(thread2.tx);
    }

    class CommitThread extends TestThread {
        final CountDownCommitBarrier barrier;
        Transaction tx;

        CommitThread(CountDownCommitBarrier barrier) {
            setPrintStackTrace(false);
            this.barrier = barrier;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            barrier.joinCommitUninterruptibly(tx);
        }
    }

    @Test
    public void whenAborted_thenIgnored() {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        barrier.abort();
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        barrier = new CountDownCommitBarrier(0);

        try {
            barrier.abort();
            fail();
        } catch (CommitBarrierOpenException expected) {

        }

        assertTrue(barrier.isCommitted());
    }
}
