package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_abortTest {
    private VetoCommitBarrier barrier;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
        stm = new BetaStm();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenNoPreparedTransactions() {
        barrier = new VetoCommitBarrier();

        barrier.abort();
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenPendingTransactions_theyAreAborted() throws InterruptedException {
        barrier = new VetoCommitBarrier();
        BetaIntRef ref = new BetaIntRef(stm, 0);
        IncThread thread1 = new IncThread(ref);
        IncThread thread2 = new IncThread(ref);

        startAll(thread1, thread2);

        sleepMs(500);
        barrier.abort();
        thread1.join();
        thread2.join();

        assertEquals(0, ref.atomicGet());
        assertIsAborted(thread1.tx);
        assertIsAborted(thread2.tx);
        thread1.assertFailedWithException(CommitBarrierOpenException.class);
        thread2.assertFailedWithException(CommitBarrierOpenException.class);
    }

    @Test
    public void whenBarrierAborted_thenCallIgnored() {
        barrier = new VetoCommitBarrier();
        barrier.abort();

        barrier.abort();
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenBarrierCommitted_thenCommitBarrierOpenException() {
        barrier = new VetoCommitBarrier();
        barrier.atomicVetoCommit();

        try {
            barrier.abort();
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
    }

    public class IncThread extends TestThread {
        private final BetaIntRef ref;
        private Transaction tx;

        public IncThread(BetaIntRef ref) {
            super("IncThread");
            setPrintStackTrace(false);
            this.ref = ref;
        }

        @Override
        public void doRun() throws Exception {
            stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    IncThread.this.tx = tx;
                    ref.incrementAndGet(tx, 1);
                    barrier.joinCommit(tx);
                }
            });
        }
    }
}
