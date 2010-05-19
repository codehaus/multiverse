package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.AbstractTransactionImpl;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CountDownCommitBarrier_joinCommitTest {
    private CountDownCommitBarrier barrier;
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenNullTransaction() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        try {
            barrier.joinCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenLastOneEntering() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();

        barrier.joinCommit(tx);

        assertIsCommitted(tx);
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAbortedWhileWaiting() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final IntRef ref = new IntRef();

        TestThread t = new TestThread() {
            @Override
            @TransactionalMethod
            public void doRun() throws Exception {
                Transaction tx = getThreadLocalTransaction();
                ref.set(10);
                barrier.joinCommit(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        assertAlive(t);
        assertTrue(barrier.isClosed());
        barrier.abort();

        t.join();
        t.assertFailedWithException(IllegalStateException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, ref.get());
    }

    @Test
    public void whenCommittedWhileWaiting() {
        barrier = new CountDownCommitBarrier(3);

        JoinCommitThread t1 = new JoinCommitThread(barrier);
        JoinCommitThread t2 = new JoinCommitThread(barrier);

        startAll(t1, t2);
        sleepMs(500);

        barrier.countDown();

        joinAll(t1, t2);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenInterruptedWhileWaiting() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final IntRef ref = new IntRef();

        TestThread t = new TestThread() {
            @Override
            @TransactionalMethod
            public void doRun() throws Exception {
                Transaction tx = getThreadLocalTransaction();
                ref.set(10);
                barrier.joinCommit(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        t.interrupt();

        t.join();
        t.assertFailedWithException(InterruptedException.class);
        assertEquals(0, ref.get());
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenTransactionAlreadyCommitted() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionAlreadyAborted_thenDeadTransactionException() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.abort();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() throws InterruptedException {
        barrier = new CountDownCommitBarrier(0);

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }
}
