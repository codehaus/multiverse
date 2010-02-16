package org.multiverse.commitbarriers;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.sleepMs;

public class CountdownCommitBarrier_setTimeoutTest {

    @Test
    public void whenNullTimeUnit_thenNullPointerException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);

        try {
            barrier.setTimeout(10, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTimedOut() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        sleepMs(1000);

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommittedBeforeTimeout() throws InterruptedException {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        barrier.awaitCommit();

        sleepMs(1000);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAbortedBeforeTimeout() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        barrier.abort();

        sleepMs(1000);
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(0);

        try {
            barrier.setTimeout(10, TimeUnit.SECONDS);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAborted_thenClosedCommitBarrierException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        try {
            barrier.setTimeout(10, TimeUnit.SECONDS);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isAborted());
    }
}
