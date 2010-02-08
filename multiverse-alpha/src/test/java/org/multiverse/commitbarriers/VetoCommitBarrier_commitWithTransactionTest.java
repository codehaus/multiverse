package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.commitbarriers.VetoCommitBarrier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_commitWithTransactionTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder().build();
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }


    @Test
    public void whenNullTx_thenNullPointerException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        try {
            barrier.commit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenNoPendingTransactions() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = txFactory.start();
        barrier.commit(tx);

        assertTrue(barrier.isCommitted());
        assertIsCommitted(tx);
    }

    @Test
    @Ignore
    public void whenTransactionIsFirst() {

    }

    @Test
    @Ignore
    public void whenTransactionFailedToPrepare() {

    }

    @Test
    @Ignore
    public void whenPendingTransactions_thenTheyAreNotified() {

    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = txFactory.start();
        tx.abort();

        try {
            barrier.commit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = txFactory.start();
        tx.commit();

        try {
            barrier.commit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isOpen());
    }

    @Test
    public void whenBarrierCommitted_thenIllegalStateException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.commit();

        Transaction tx = txFactory.start();
        try {
            barrier.commit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertIsActive(tx);
    }

    @Test
    public void whenBarrierAborted_thenIllegalStateException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        Transaction tx = txFactory.start();
        try {
            barrier.commit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(barrier.isAborted());
        assertIsActive(tx);
    }

}
