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
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_tryJoinCommitTest {

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
            barrier.tryJoinCommit(null, 1, TimeUnit.DAYS);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
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
    public void whenAborted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.tryJoinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.vetoCommit();

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.tryJoinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertIsAborted(tx);
    }
}
