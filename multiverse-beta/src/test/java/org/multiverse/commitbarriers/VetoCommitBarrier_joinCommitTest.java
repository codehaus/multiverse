package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaIntRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_joinCommitTest {
    private GammaStm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = new GammaStm();
        txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .newTransactionFactory();
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
    @Ignore
    public void whenTransactionPreparable_thenAdded() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        GammaIntRef ref = new GammaIntRef(stm);
        IncThread thread = new IncThread(ref, barrier);
        thread.start();

        sleepMs(1000);
        assertAlive(thread);
        assertTrue(barrier.isClosed());
        assertEquals(1, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenTransactionPrepared_thenAdded() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        GammaIntRef ref = new GammaIntRef(stm);
        IncThread thread = new IncThread(ref, barrier, true);
        thread.start();

        sleepMs(1000);
        assertAlive(thread);
        assertTrue(barrier.isClosed());
        assertEquals(1, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenPrepareFails() throws InterruptedException {
        final VetoCommitBarrier group = new VetoCommitBarrier();
        final GammaIntRef ref = new GammaIntRef(stm);

        FailToPrepareThread thread = new FailToPrepareThread(group, ref);
        thread.start();

        sleepMs(1000);
        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.incrementAndGet(tx, 1);
            }
        });

        thread.join();
        thread.assertFailedWithException(TooManyRetriesException.class);
        assertEquals(0, group.getNumberWaiting());
    }

    class FailToPrepareThread extends TestThread {
        final VetoCommitBarrier group;
        final GammaIntRef ref;

        FailToPrepareThread(VetoCommitBarrier group, GammaIntRef ref) {
            super("FailedToPrepareThread");
            this.group = group;
            this.ref = ref;
            setPrintStackTrace(false);
        }

        @Override
        public void doRun() throws Exception {
            stm.newTransactionFactoryBuilder()
                    .setSpeculativeConfigurationEnabled(false)
                    .setMaxRetries(0)
                    .newAtomicBlock()
                    .execute(new AtomicVoidClosure() {
                        @Override
                        public void execute(Transaction tx) throws Exception {
                            //we need to load it to cause a conflict
                            ref.get(tx);
                            sleepMs(2000);
                            ref.incrementAndGet(tx, 1);
                            group.joinCommit(tx);
                        }
                    });
        }
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() throws InterruptedException {
        Transaction tx = txFactory.newTransaction();
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
        Transaction tx = txFactory.newTransaction();
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

        Transaction tx = txFactory.newTransaction();
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
        barrier.atomicVetoCommit();

        System.out.println("barrier.state: " + barrier);

        Transaction tx = txFactory.newTransaction();
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
        private final GammaIntRef ref;
        private final VetoCommitBarrier barrier;
        private boolean prepare;

        public IncThread(GammaIntRef ref, VetoCommitBarrier barrier) {
            this(ref, barrier, false);
        }

        public IncThread(GammaIntRef ref, VetoCommitBarrier barrier, boolean prepare) {
            super("IncThread");
            this.barrier = barrier;
            this.ref = ref;
            this.prepare = prepare;
        }

        @Override
        public void doRun() throws Exception {
            stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.incrementAndGet(tx, 1);
                    if (prepare) {
                        tx.prepare();
                    }
                    barrier.joinCommit(tx);
                }
            });
        }
    }
}
