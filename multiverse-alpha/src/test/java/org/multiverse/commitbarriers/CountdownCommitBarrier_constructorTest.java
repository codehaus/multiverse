package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountdownCommitBarrier_constructorTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeParties_thenIllegalArgumentException() {
        new CountdownCommitBarrier(-1);
    }

    @Test
    public void whenZeroParties_thenBarrierCommitted() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(0);
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenPositiveParties() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(10);
        assertTrue(barrier.isClosed());
        assertEquals(10, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
    }
}
