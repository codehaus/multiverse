package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.stms.AbstractTransactionImpl;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

@Ignore
public class CountDownCommitBarrier_joinCommitUninterruptiblyTest {
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
    public void whenOpenAndNullTransaction_thenNullPointerException() {
        barrier = new CountDownCommitBarrier(1);

        try {
            barrier.joinCommitUninterruptibly(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenOpenAndThreadAlreadyInterrupted_thenNotInterruptedButInterruptStatusIsSet() {
        barrier = new CountDownCommitBarrier(1);

        Thread.currentThread().interrupt();
        Transaction tx = new AbstractTransactionImpl();

        barrier.joinCommitUninterruptibly(tx);

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void whenOpenAndTransactionActive() {
        Transaction tx = new AbstractTransactionImpl();
        tx.prepare();

        barrier = new CountDownCommitBarrier(1);
        barrier.joinCommitUninterruptibly(tx);

        assertIsCommitted(tx);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenOpenAndTransactionPrepared() {
        Transaction tx = new AbstractTransactionImpl();
        tx.prepare();

        barrier = new CountDownCommitBarrier(1);
        barrier.joinCommitUninterruptibly(tx);

        assertIsCommitted(tx);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenOpenAndLastTransaction_thenAllTransactionsCommitted() {
        barrier = new CountDownCommitBarrier(3);
        AwaitThread t1 = new AwaitThread();
        AwaitThread t2 = new AwaitThread();

        startAll(t1, t2);
        sleepMs(500);

        assertAlive(t1, t2);
        assertTrue(barrier.isClosed());

        Transaction tx = new AbstractTransactionImpl();
        barrier.joinCommitUninterruptibly(tx);
        joinAll(t1, t2);

        assertIsCommitted(tx, t1.tx, t2.tx);
    }

    @Test
    public void whenOpenAndTransactionAborted_thenIllegalStateException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.abort();

        barrier = new CountDownCommitBarrier(1);
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (IllegalStateException ex) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenOpenAndTransactionCommitted_thenIllegalStateException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        barrier = new CountDownCommitBarrier(1);
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (IllegalStateException ex) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() {
        barrier = new CountDownCommitBarrier(1);
        barrier.joinCommitUninterruptibly(new AbstractTransactionImpl());

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertIsAborted(tx);
    }

    class AwaitThread extends TestThread {
        private Transaction tx;

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            barrier.joinCommitUninterruptibly(tx);
        }
    }
}
