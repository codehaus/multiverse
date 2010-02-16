package org.multiverse.commitbarriers;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.sleepMs;

public class VetoCommitBarrier_setTimeoutTest {

    @Test
    public void whenNullTimeUnit_thenNullPointerException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        try {
            barrier.setTimeout(10, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTimedOut() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        sleepMs(1000);

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommittedBeforeTimeout() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        barrier.commit();

        sleepMs(1000);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAbortedBeforeTimeout() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        barrier.setTimeout(500, TimeUnit.MILLISECONDS);
        barrier.abort();

        sleepMs(1000);
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.commit();

        try {
            barrier.setTimeout(10, TimeUnit.SECONDS);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenAborted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        try {
            barrier.setTimeout(10, TimeUnit.SECONDS);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isAborted());
    }
}
