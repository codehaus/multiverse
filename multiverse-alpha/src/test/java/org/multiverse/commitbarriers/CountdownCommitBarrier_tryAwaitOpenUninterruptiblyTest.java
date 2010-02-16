package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountdownCommitBarrier_tryAwaitOpenUninterruptiblyTest {
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
    public void whenNullTimeout_thenNullPointerException() {
        barrier = new CountdownCommitBarrier(1);

        try {
            barrier.tryAwaitOpenUninterruptibly(1, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertTrue(barrier.isClosed());
    }

    @Test
    @Ignore
    public void whenInterruptedWhileWaiting() {

    }

    @Test
    public void whenCommittedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(1);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                boolean result = barrier.tryAwaitOpenUninterruptibly(1, TimeUnit.DAYS);
                assertTrue(result);
            }
        };

        t.start();
        sleepMs(500);
        assertAlive(t);

        barrier.awaitCommit();
        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isCommitted());
    }

    @Test
    @Ignore
    public void whenAbortedWhileWaiting() {

    }

    @Test
    @Ignore
    public void whenTimeout() {

    }

    @Test
    public void whenCommitted() {
        barrier = new CountdownCommitBarrier(0);

        boolean result = barrier.tryAwaitOpenUninterruptibly(1, TimeUnit.DAYS);

        assertTrue(result);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAborted() {
        barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        boolean result = barrier.tryAwaitOpenUninterruptibly(1, TimeUnit.DAYS);

        assertTrue(result);
        assertTrue(barrier.isAborted());
    }
}
