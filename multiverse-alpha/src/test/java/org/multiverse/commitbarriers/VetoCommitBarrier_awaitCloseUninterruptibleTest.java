package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_awaitCloseUninterruptibleTest {
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

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                Thread.currentThread().interrupt();
                barrier.awaitCloseUninterruptibly();
            }
        };
        t.start();

        sleepMs(500);
        assertAlive(t);

        barrier.commit();

        joinAll(t);
    }

    @Test
    public void whenInterruptedWhileWaiting_thenInterruptedException() {
        barrier = new VetoCommitBarrier();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.awaitCloseUninterruptibly();
            }
        };
        t.start();

        sleepMs(500);
        t.interrupt();

        sleepMs(500);
        assertAlive(t);

        barrier.commit();

        joinAll(t);
    }

    @Test
    public void whenAbortedWhileWaiting() {
        barrier = new VetoCommitBarrier();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.awaitCloseUninterruptibly();
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
                barrier.awaitCloseUninterruptibly();
            }
        };
        t.start();

        sleepMs(500);
        barrier.commit();

        joinAll(t);
    }

    @Test
    public void whenCommitted() throws InterruptedException {
        barrier = new VetoCommitBarrier();
        barrier.commit();

        barrier.awaitCloseUninterruptibly();
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted() throws InterruptedException {
        barrier = new VetoCommitBarrier();
        barrier.abort();

        barrier.awaitCloseUninterruptibly();
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }
}
