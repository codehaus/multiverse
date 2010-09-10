package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.references.IntRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class CountDownCommitBarrier_joinCommitTest {
    private CountDownCommitBarrier barrier;
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
    public void whenNullTransaction() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        try {
            barrier.joinCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenLastOneEntering() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();

        barrier.joinCommit(tx);

        assertIsCommitted(tx);
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAbortedWhileWaiting() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final BetaIntRef ref = stm.getDefaultRefFactory().createIntRef(0);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.set(tx,10);
                        barrier.joinCommit(tx);
                    }
                });
            }
        };

        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        assertAlive(t);
        assertTrue(barrier.isClosed());
        barrier.abort();

        t.join();
        t.assertFailedWithException(IllegalStateException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, ref.___unsafeLoad().value);
    }

    @Test
    public void whenCommittedWhileWaiting() {
        barrier = new CountDownCommitBarrier(3);

        JoinCommitThread t1 = new JoinCommitThread(stm, barrier);
        JoinCommitThread t2 = new JoinCommitThread(stm, barrier);

        startAll(t1, t2);
        sleepMs(500);

        barrier.countDown();

        joinAll(t1, t2);
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenInterruptedWhileWaiting() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final BetaIntRef ref = stm.getDefaultRefFactory().createIntRef(0);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                stm.getDefaultAtomicBlock().executeChecked(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.set(tx, 10);
                        barrier.joinCommit(tx);
                    }
                });
            }
        };

        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        t.interrupt();

        t.join();
        t.assertFailedWithException(InterruptedException.class);
        assertEquals(0, ref.___unsafeLoad().value);
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenTransactionAlreadyCommitted() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenTransactionAlreadyAborted_thenDeadTransactionException() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);

        Transaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() throws InterruptedException {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Transaction tx = stm.startDefaultTransaction();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() throws InterruptedException {
        barrier = new CountDownCommitBarrier(0);

        Transaction tx = stm.startDefaultTransaction();

        try {
            barrier.joinCommit(tx);
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }
}
