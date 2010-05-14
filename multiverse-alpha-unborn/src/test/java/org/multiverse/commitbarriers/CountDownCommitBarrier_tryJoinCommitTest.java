package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionConfiguration;
import org.multiverse.api.TransactionStatus;
import org.multiverse.stms.AbstractTransactionImpl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountDownCommitBarrier_tryJoinCommitTest {

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
    public void whenOpenAndTransactionCommitted_thenIllegalStateException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        Transaction tx = new AbstractTransactionImpl();
        tx.commit();
        try{
          barrier.tryJoinCommit(tx);
          fail("Expected Illegal state exception");
        } catch (IllegalStateException ex) {
        }
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenOpenAndTransactionAborted_thenIllegalStateException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        Transaction tx = new AbstractTransactionImpl();
        tx.abort();
        try{
          barrier.tryJoinCommit(tx);
          fail("Expected Illegal state exception");
        } catch (IllegalStateException ex) {
        }
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        CountDownCommitBarrier barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();
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

        Transaction tx = new AbstractTransactionImpl();
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
