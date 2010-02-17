package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.AbstractTransactionImpl;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CountDownCommitBarrier_joinCommitUninterruptiblyWithTransactionTest {
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
    public void whenTransactionNull() {
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
    @Ignore
    public void whenTransactionFailsToPrepare() {
        barrier = new CountDownCommitBarrier(1);


    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.abort();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.commit();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenStartingInterrupted() {

    }

    @Test
    public void whenInterruptedWhileWaiting_thenNoInterruption() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.joinCommitUninterruptibly(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        t.interrupt();
        sleepMs(500);

        assertAlive(t);
        assertTrue(barrier.isClosed());

        //todo
        //assertTrue(t.isInterrupted());
    }

    @Test
    public void whenCommittedWhileWaiting() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.joinCommitUninterruptibly(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        barrier.countDown();
        sleepMs(500);

        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isCommitted());
        assertEquals(1, ref.get());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAbortedWhileWaiting_() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.joinCommitUninterruptibly(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        barrier.abort();
        sleepMs(500);

        t.join();
        t.assertFailedWithException(IllegalStateException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, ref.get());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted_thenClosedCommitBarrierException() {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        barrier = new CountDownCommitBarrier(0);

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }
}
