package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.WriteConflictException;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class VetoCommitBarrier_vetoCommitWithTransactionTest {
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
            barrier.vetoCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenNoPendingTransactions() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = txFactory.start();
        barrier.vetoCommit(tx);

        assertTrue(barrier.isCommitted());
        assertIsCommitted(tx);
    }

    @Test
    public void whenPendingTransaction() throws InterruptedException {
        final VetoCommitBarrier barrier = new VetoCommitBarrier();

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread() {
            @Override
            @TransactionalMethod
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                barrier.joinCommit(tx);
            }
        };
        t.start();

        sleepMs(500);
        assertAlive(t);
        assertTrue(barrier.isClosed());

        barrier.vetoCommit();
        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isCommitted());
        assertEquals(1, ref.get());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionFailedToPrepare_thenBarrierNotAbortedOrCommitted() {
        Transaction tx = mock(Transaction.class);
        doReturn(TransactionStatus.active).when(tx).getStatus();
        doThrow(new WriteConflictException()).when(tx).prepare();

        VetoCommitBarrier barrier = new VetoCommitBarrier();
        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (WriteConflictException expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = txFactory.start();
        tx.abort();

        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = txFactory.start();
        tx.commit();

        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenBarrierCommitted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.vetoCommit();

        Transaction tx = txFactory.start();
        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertIsActive(tx);
    }

    @Test
    public void whenBarrierAborted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        Transaction tx = txFactory.start();
        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertIsActive(tx);
    }

}
