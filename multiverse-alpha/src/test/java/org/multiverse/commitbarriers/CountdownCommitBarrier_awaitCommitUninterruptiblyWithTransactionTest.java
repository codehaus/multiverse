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

public class CountdownCommitBarrier_awaitCommitUninterruptiblyWithTransactionTest {
    private CountdownCommitBarrier barrier;

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
        barrier = new CountdownCommitBarrier(1);

        try {
            barrier.awaitCommitUninterruptibly(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenTransactionFailsToPrepare() {
        barrier = new CountdownCommitBarrier(1);


    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        barrier = new CountdownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.abort();
        try {
            barrier.awaitCommitUninterruptibly(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        barrier = new CountdownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.commit();
        try {
            barrier.awaitCommitUninterruptibly(tx);
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
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.awaitCommitUninterruptibly(tx);
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
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.awaitCommitUninterruptibly(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        barrier.awaitCommit();
        sleepMs(500);

        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isCommitted());
        assertEquals(1, ref.get());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAbortedWhileWaiting_() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.awaitCommitUninterruptibly(tx);
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
        barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.awaitCommitUninterruptibly(tx);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        barrier = new CountdownCommitBarrier(0);

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.awaitCommitUninterruptibly(tx);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }
}
