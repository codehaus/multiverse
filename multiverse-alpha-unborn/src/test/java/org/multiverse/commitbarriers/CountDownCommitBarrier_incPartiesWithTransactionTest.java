package org.multiverse.commitbarriers;

import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.AbstractTransactionImpl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.multiverse.TestUtils.*;

public class CountDownCommitBarrier_incPartiesWithTransactionTest {

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
    public void whenNullTransaction_thenNullPointerException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);

        try {
            barrier.incParties(null, 1);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(10, barrier.getParties());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionPrepared() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);
        Transaction tx = new AbstractTransactionImpl();
        tx.prepare();

        barrier.incParties(tx, 5);

        assertIsPrepared(tx);
        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(15, barrier.getParties());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);

        Transaction tx = new AbstractTransactionImpl();
        tx.abort();

        try {
            barrier.incParties(tx, 1);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(10, barrier.getParties());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);

        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        try {
            barrier.incParties(tx, 1);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(10, barrier.getParties());
        assertTrue(barrier.isClosed());
    }


    @Test
    public void whenZeroExtraParties() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(5);
        Transaction tx = new AbstractTransactionImpl();
        barrier.incParties(tx, 0);

        assertEquals(5, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenPositiveNumber() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(10);
        Transaction tx = new AbstractTransactionImpl();
        barrier.incParties(tx, 5);

        assertIsNew(tx);
        assertEquals(0, barrier.getNumberWaiting());
        assertEquals(15, barrier.getParties());
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenPartiesAdded_thenAdditionalJoinsNeedToBeExecuted() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(2);

        Transaction tx = new AbstractTransactionImpl();
        barrier.incParties(tx, 1);

        barrier.countDown();
        barrier.countDown();
        assertTrue(barrier.isClosed());
        barrier.countDown();

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionAborted_thenPartiesRestored() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(2);

        Transaction tx = new AbstractTransactionImpl();
        barrier.incParties(tx, 10);

        tx.abort();

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
        assertEquals(2, barrier.getParties());
    }

    @Test
    public void whenPendingTransactions() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(3);

        JoinCommitThread t1 = new JoinCommitThread(barrier);
        JoinCommitThread t2 = new JoinCommitThread(barrier);

        startAll(t1, t2);
        sleepMs(300);

        barrier.incParties(2);
        sleepMs(300);

        assertAlive(t1, t2);

        assertEquals(2, barrier.getNumberWaiting());
        assertEquals(5, barrier.getParties());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.incParties(tx, 10);
            fail("Should have got CommitBarrierOpenException");
        } catch (CommitBarrierOpenException expected) {
        }

        assertEquals(1, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(0);
        Transaction tx = spy(new AbstractTransactionImpl());

        try {
            barrier.incParties(tx, 1);
            fail("Should have got CommitBarrierOpenException");
        } catch (CommitBarrierOpenException expected) {
        }

        assertIsNew(tx);
        assertEquals(0, barrier.getParties());
        assertEquals(0, barrier.getNumberWaiting());
        assertTrue(barrier.isCommitted());
    }
}
