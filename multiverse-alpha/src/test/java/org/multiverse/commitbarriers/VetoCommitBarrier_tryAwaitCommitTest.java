package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.AbstractTransactionImpl;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_tryAwaitCommitTest {

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
    public void whenOpenAndNullTransaction_thenNullPointerException() throws InterruptedException {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        try {
            barrier.tryAwaitCommit(null, 1, TimeUnit.DAYS);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
    }

    @Test
    @Ignore
    public void whenOpenAndTransactionPrepared_then() {

    }

    @Test
    @Ignore
    public void whenOpenAndTransactionCommitted_then() {

    }

    @Test
    @Ignore
    public void whenOpenAndTransactionAborted_then() {

    }

    @Test
    @Ignore
    public void whenAborted_thenIllegalStateException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();

        try {
            // barrier.tryAwaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isAborted());
        assertIsActive(tx);
    }

    @Test
    @Ignore
    public void whenCommitted_thenIllegalStateException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.commit();

        Transaction tx = new AbstractTransactionImpl();

        try {
            //barrier.tryAwaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertIsActive(tx);
    }
}
