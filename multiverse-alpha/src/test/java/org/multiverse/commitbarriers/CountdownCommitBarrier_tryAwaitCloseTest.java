package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountdownCommitBarrier_tryAwaitCloseTest {
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
    public void whenNullTimeUnit_thenNullPointerException() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);

        try {
            barrier.tryAwaitClose(10, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenNegativeTimeoutAndBufferedOpen() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);

        boolean result = barrier.tryAwaitClose(-1, TimeUnit.DAYS);
        assertFalse(result);
        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenAbortedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                boolean success = barrier.tryAwaitClose(1, TimeUnit.DAYS);
                assertTrue(success);
            }
        };
        t.start();
        sleepMs(500);

        barrier.abort();

        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenCommittedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                boolean success = barrier.tryAwaitClose(1, TimeUnit.DAYS);
                assertTrue(success);
            }
        };
        t.start();
        sleepMs(500);

        barrier.awaitCommit();

        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenInterruptedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.tryAwaitClose(1, TimeUnit.DAYS);
                fail();
            }
        };
        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        t.interrupt();

        t.join();
        t.assertFailedWithException(InterruptedException.class);
        assertTrue(barrier.isOpen());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTimeoutWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                boolean success = barrier.tryAwaitClose(1, TimeUnit.SECONDS);
                assertFalse(success);
            }
        };
        t.start();
        t.join();
        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenAborted() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        boolean result = barrier.tryAwaitClose(1, TimeUnit.DAYS);
        assertTrue(result);
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted() throws InterruptedException {
        barrier = new CountdownCommitBarrier(0);

        boolean result = barrier.tryAwaitClose(1, TimeUnit.DAYS);
        assertTrue(result);
        assertTrue(barrier.isCommitted());
    }
}
