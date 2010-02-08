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

public class CountdownCommitBarrier_awaitCommitWithTransactionTest {
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
    public void whenNullTransaction() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);

        try {
            barrier.awaitCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenLastOneEntering() {
        barrier = new CountdownCommitBarrier(1);
    }

    @Test
    public void whenAbortedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @Override
            @TransactionalMethod
            public void doRun() throws Exception {
                Transaction tx = getThreadLocalTransaction();
                ref.set(10);
                barrier.awaitCommit(tx);
            }
        };

        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        assertAlive(t);
        assertTrue(barrier.isOpen());
        barrier.abort();

        t.join();
        t.assertFailedWithException(IllegalStateException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, ref.get());
    }


    @Test
    @Ignore
    public void whenCommittedWhileWaiting() {

    }

    @Test
    public void whenInterruptedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @Override
            @TransactionalMethod
            public void doRun() throws Exception {
                Transaction tx = getThreadLocalTransaction();
                ref.set(10);
                barrier.awaitCommit(tx);
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
        barrier = new CountdownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        try {
            barrier.awaitCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenTransactionAlreadyAborted_thenDeadTransactionException() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        tx.abort();

        try {
            barrier.awaitCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenAborted_thenIllegalStateException() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.awaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenCommitted_thenIllegalStateException() throws InterruptedException {
        barrier = new CountdownCommitBarrier(0);

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.awaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    public class AwaitCommitThread extends TestThread {
        private Transaction tx;

        @Override
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            barrier.awaitCommit(tx);
        }
    }
}
