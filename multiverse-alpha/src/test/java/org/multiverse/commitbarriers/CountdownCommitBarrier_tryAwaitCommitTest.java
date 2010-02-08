package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.AbstractTransactionImpl;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountdownCommitBarrier_tryAwaitCommitTest {

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
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);

        try {
            barrier.tryAwaitCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenOpenAndTransactionCommitted_thenIllegalStateException() {

    }

    @Test
    @Ignore
    public void whenOpenAndTransactionAborted_thenIllegalStateException() {

    }

    @Test
    public void whenAborted_thenIllegalStateException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.tryAwaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsActive(tx);
    }

    @Test
    public void whenCommitted_thenIllegalStateException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(0);

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.tryAwaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsActive(tx);
    }
}
