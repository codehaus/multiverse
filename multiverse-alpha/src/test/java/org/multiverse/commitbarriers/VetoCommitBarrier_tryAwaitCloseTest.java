package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_tryAwaitCloseTest {
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
    public void whenNullTimeout_thenNullPointerException() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        try {
            barrier.tryAwaitClose(1, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenAlreadyInterrupted() {
        Thread.currentThread().interrupt();

        barrier = new VetoCommitBarrier();
        try {
            barrier.tryAwaitClose(1, TimeUnit.DAYS);
            fail();
        } catch (InterruptedException expected) {
        }

        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenInterruptedWhileWaiting() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.tryAwaitClose(1, TimeUnit.DAYS);
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);
        thread.interrupt();

        thread.join();
        thread.assertFailedWithException(InterruptedException.class);
        //assertIsOpen()
    }

    @Test
    public void whenCommittedWhileWaiting() {
        barrier = new VetoCommitBarrier();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                barrier.tryAwaitClose(1, TimeUnit.DAYS);
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);
        thread.interrupt();

        //thread.join();
        //thread.assertFailedWithException(InterruptedException.class);
    }

    @Test
    public void whenAbortedWhileWaiting() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                boolean result = barrier.tryAwaitClose(1, TimeUnit.DAYS);
                assertTrue(result);
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        sleepMs(500);
        barrier.abort();

        joinAll(thread);
    }

    @Test
    public void whenTimeout() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        TestThread thread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                boolean result = barrier.tryAwaitClose(1, TimeUnit.SECONDS);
                assertFalse(result);
            }
        };

        thread.setPrintStackTrace(false);
        thread.start();
        joinAll(thread);

    }

    @Test
    public void whenAborted() throws InterruptedException {
        barrier = new VetoCommitBarrier();
        barrier.abort();

        boolean success = barrier.tryAwaitClose(1, TimeUnit.DAYS);
        assertTrue(barrier.isAborted());
        assertTrue(success);
    }

    @Test
    public void whenCommitted() throws InterruptedException {
        barrier = new VetoCommitBarrier();
        barrier.commit();

        barrier.awaitClose();
        assertTrue(barrier.isCommitted());
    }
}
