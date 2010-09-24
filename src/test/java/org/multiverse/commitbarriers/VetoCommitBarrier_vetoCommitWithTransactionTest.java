package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_vetoCommitWithTransactionTest {
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

        Transaction tx = stm.startDefaultTransaction();
        barrier.vetoCommit(tx);

        assertTrue(barrier.isCommitted());
        assertIsCommitted(tx);
    }

    @Test
    @Ignore
    public void whenPendingTransaction() throws InterruptedException {
        final VetoCommitBarrier barrier = new VetoCommitBarrier();

        final BetaIntRef ref = new BetaIntRef(stm);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.incrementAndGet(tx, 1);
                        barrier.joinCommit(tx);
                    }
                });


            }
        };
        t.start();

        sleepMs(500);
        assertAlive(t);
        assertTrue(barrier.isClosed());

        barrier.atomicVetoCommit();
        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isCommitted());
        assertEquals(1, ref.___unsafeLoad().value);
        assertEquals(0, barrier.getNumberWaiting());
    }


    @Test
    public void whenTransactionFailedToPrepare_thenBarrierNotAbortedOrCommitted() {
        final BetaIntRef ref = new BetaIntRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, false);

        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure(){
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.incrementAndGet(tx,1);
            }
        });

        tx.openForWrite(ref, false).value++;

        VetoCommitBarrier barrier = new VetoCommitBarrier();
        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        Transaction tx = stm.startDefaultTransaction();
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

        Transaction tx = stm.startDefaultTransaction();
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
    public void whenBarrierCommitted_thenCommitBarrierOpenException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.atomicVetoCommit();

        Transaction tx = stm.startDefaultTransaction();
        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertIsActive(tx);
    }

    @Test
    public void whenBarrierAborted_thenCommitBarrierOpenException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        Transaction tx = stm.startDefaultTransaction();
        try {
            barrier.vetoCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertIsActive(tx);
    }

}
