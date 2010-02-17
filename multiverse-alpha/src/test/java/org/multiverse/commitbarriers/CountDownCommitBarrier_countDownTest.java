package org.multiverse.commitbarriers;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CountDownCommitBarrier_countDownTest {

    @Test
    @Ignore
    public void whenLastOne() {

    }

    @Test
    @Ignore
    public void whenNotLastOne() {
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        try {
            barrier.countDown();
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(0);

        try {
            barrier.countDown();
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
    }
}
