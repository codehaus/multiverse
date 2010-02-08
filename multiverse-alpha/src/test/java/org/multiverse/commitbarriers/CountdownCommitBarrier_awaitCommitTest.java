package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountdownCommitBarrier_awaitCommitTest {
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
    public void whenStartingInterrupted() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);
        Thread.currentThread().interrupt();

        try {
            barrier.awaitCommit();
            fail();
        } catch (InterruptedException ex) {
        }

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenNotLast() {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread thread = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                barrier.awaitCommit();
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);

        assertTrue(barrier.isOpen());
        assertAlive(thread);
    }

    @Test
    public void whenAbortedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread thread = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                barrier.awaitCommit();
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);

        barrier.abort();
        sleepMs(500);

        thread.join();
        thread.assertFailedWithException(IllegalStateException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(0, ref.get());
    }

    @Test
    public void whenCommittedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread thread = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                barrier.awaitCommit();
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);

        barrier.awaitCommit();
        sleepMs(500);

        thread.join();
        thread.assertNothingThrown();
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(1, ref.get());
    }

    @Test
    public void whenInterruptedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread thread = new TestThread() {
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                barrier.awaitCommit();
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);

        assertTrue(barrier.isOpen());

        thread.interrupt();
        thread.join();
        thread.assertFailedWithException(InterruptedException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(0, ref.get());
    }

    @Test
    public void whenAborted_thenIllegalStateException() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        try {
            barrier.awaitCommit();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenIllegalStateException() throws InterruptedException {
        barrier = new CountdownCommitBarrier(0);

        try {
            barrier.awaitCommit();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isCommitted());
    }
}
