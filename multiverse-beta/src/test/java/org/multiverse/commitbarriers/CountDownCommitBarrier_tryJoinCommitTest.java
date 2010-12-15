package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountDownCommitBarrier_tryJoinCommitTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenOpenAndNullTransaction_thenNullPointerException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);

        try {
            barrier.tryJoinCommit(null);
            fail("Expecting NullPointerException");
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenOpenAndTransactionCommitted_thenDeadTransactionException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        Transaction tx = stm.startDefaultTransaction();
        tx.commit();
        try {
            barrier.tryJoinCommit(tx);
            fail();
        } catch (DeadTransactionException ex) {
        }
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenOpenAndTransactionAborted_DeadTransactionException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        Transaction tx = stm.startDefaultTransaction();
        tx.abort();
        try {
            barrier.tryJoinCommit(tx);
            fail();
        } catch (DeadTransactionException ex) {
        }
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = stm.startDefaultTransaction();
        try {
            barrier.tryJoinCommit(tx);
            fail("Expecting CommitBarrierOpenException");
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(0);

        Transaction tx = stm.startDefaultTransaction();
        try {
            barrier.tryJoinCommit(tx);
            fail("Expected CommitBarrierOpenException");
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }
}
