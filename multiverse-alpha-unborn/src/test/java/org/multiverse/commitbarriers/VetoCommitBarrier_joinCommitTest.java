package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class VetoCommitBarrier_joinCommitTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build();
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenTransactionNull_thenNullPointerException() throws InterruptedException {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        try {
            barrier.joinCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionPreparable_thenAdded() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        TransactionalInteger ref = new TransactionalInteger();
        IncThread thread = new IncThread(ref, barrier);
        thread.start();

        sleepMs(500);
        assertAlive(thread);
        assertTrue(barrier.isClosed());
        assertEquals(1, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionPrepared_thenAdded() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        TransactionalInteger ref = new TransactionalInteger();
        IncThread thread = new IncThread(ref, barrier, true);
        thread.start();

        sleepMs(500);
        assertAlive(thread);
        assertTrue(barrier.isClosed());
        assertEquals(1, barrier.getNumberWaiting());

    }

    @Test
    public void whenPrepareFails() throws InterruptedException {
        final VetoCommitBarrier group = new VetoCommitBarrier();
        final TransactionalInteger ref = new TransactionalInteger();

        FailToPrepareThread thread = new FailToPrepareThread(group, ref);
        thread.start();

        sleepMs(500);
        ref.inc();

        thread.join();
        thread.assertFailedWithException(TooManyRetriesException.class);
        assertEquals(0, group.getNumberWaiting());
    }

    class FailToPrepareThread extends TestThread {
        final VetoCommitBarrier group;
        final TransactionalInteger ref;

        FailToPrepareThread(VetoCommitBarrier group, TransactionalInteger ref) {
            this.group = group;
            this.ref = ref;
            setPrintStackTrace(false);
        }

        @Override
        @TransactionalMethod(maxRetries = 0)
        public void doRun() throws Exception {
            sleepMs(1000);
            ref.inc();
            group.joinCommit(getThreadLocalTransaction());
        }
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() throws InterruptedException {
        Transaction tx = txFactory.start();
        tx.abort();

        VetoCommitBarrier group = new VetoCommitBarrier();
        try {
            group.joinCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertTrue(group.isClosed());
        assertIsAborted(tx);
        assertEquals(0, group.getNumberWaiting());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() throws InterruptedException {
        Transaction tx = txFactory.start();
        tx.commit();

        VetoCommitBarrier group = new VetoCommitBarrier();
        try {
            group.joinCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertTrue(group.isClosed());
        assertIsCommitted(tx);
        assertEquals(0, group.getNumberWaiting());
    }

    @Test
    public void whenBarrierAborted_thenCommitBarrierOpenException() throws InterruptedException {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        Transaction tx = txFactory.start();
        try {
            barrier.joinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertIsActive(tx);
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() throws InterruptedException {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.vetoCommit();

        Transaction tx = txFactory.start();
        try {
            barrier.joinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertIsActive(tx);
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    public class IncThread extends TestThread {
        private final TransactionalInteger ref;
        private final VetoCommitBarrier barrier;
        private Transaction tx;
        private boolean prepare;

        public IncThread(TransactionalInteger ref, VetoCommitBarrier barrier) {
            this(ref, barrier, false);
        }

        public IncThread(TransactionalInteger ref, VetoCommitBarrier barrier, boolean prepare) {
            super("IncThread");
            this.barrier = barrier;
            this.ref = ref;
            this.prepare = prepare;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            ref.inc();
            if (prepare) {
                tx.prepare();
            }
            barrier.joinCommit(tx);
        }
    }
}
