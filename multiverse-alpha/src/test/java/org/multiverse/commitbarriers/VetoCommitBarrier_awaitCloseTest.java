package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_awaitCloseTest {
    private VetoCommitBarrier barrier;

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
    public void whenStartInterrupted_thenInterruptedException() {
        barrier = new VetoCommitBarrier();

        Thread.currentThread().interrupt();

        try {
            barrier.awaitClose();
            fail();
        } catch (InterruptedException expected) {
        }

        assertTrue(barrier.isOpen());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenInterruptedWhileWaiting_thenInterruptedException() {
        barrier = new VetoCommitBarrier();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                try {
                    barrier.awaitClose();
                    fail();
                } catch (InterruptedException expected) {
                }
            }
        };
        t.start();

        sleepMs(500);
        t.interrupt();

        joinAll(t);
    }

    @Test
    public void whenAbortedWhileWaiting() {
        barrier = new VetoCommitBarrier();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.awaitClose();
            }
        };
        t.start();

        sleepMs(500);
        barrier.abort();

        joinAll(t);
    }

    @Test
    public void whenCommittedWhileWaiting() {
        barrier = new VetoCommitBarrier();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.awaitClose();
            }
        };
        t.start();

        sleepMs(500);
        barrier.commit();

        joinAll(t);
    }

    @Test
    public void whenCommitted() throws InterruptedException {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.commit();

        barrier.awaitClose();
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted() throws InterruptedException {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        barrier.awaitClose();
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }
}
