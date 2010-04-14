package org.multiverse.commitbarriers;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public class CountDownCommitBarrier_incPartiesTest {

    @Test
    public void whenNegativeNumber_thenIllegalArgumentException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);

        try {
            barrier.incParties(-1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(10, barrier.getParties());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenZeroExtraParties() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(5);
        barrier.incParties(0);

        assertEquals(5, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenPositiveNumber() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);
        barrier.incParties(5);

        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(15, barrier.getParties());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenPartiesAdded_commitTakesLonger() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(2);
        barrier.incParties(1);

        barrier.countDown();
        barrier.countDown();
        assertTrue(barrier.isClosed());
        barrier.countDown();
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenPendingTransactions() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(3);

        JoinCommitThread t1 = new JoinCommitThread(barrier);
        JoinCommitThread t2 = new JoinCommitThread(barrier);

        startAll(t1, t2);

        sleepMs(500);
        assertTrue(barrier.isClosed());

        barrier.incParties(1);

        sleepMs(500);
        assertAlive(t1, t2);
        assertTrue(barrier.isClosed());
        assertEquals(2, barrier.getNumberWaiting());
        assertEquals(4, barrier.getParties());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        try {
            barrier.incParties(10);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertEquals(1, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(0);

        try {
            barrier.incParties();
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertEquals(0, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isCommitted());
    }
}
