package org.multiverse.commitbarriers;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.sleepMs;

public class CountDownCommitBarrier_setTimeoutTest {

    @Test
    public void whenNullTimeUnit_thenNullPointerException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);

        try {
            barrier.setTimeout(10, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTimedOut() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        sleepMs(1000);

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommittedBeforeTimeout() throws InterruptedException {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        barrier.countDown();

        sleepMs(1000);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAbortedBeforeTimeout() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        barrier.abort();

        sleepMs(1000);
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(0);

        try {
            barrier.setTimeout(10, TimeUnit.SECONDS);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAborted_thenClosedCommitBarrierException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        try {
            barrier.setTimeout(10, TimeUnit.SECONDS);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
    }
}
