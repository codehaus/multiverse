package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class VetoCommitBarrier_integrationTest {

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
    public void test() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        BetaIntRef ref1 = new BetaIntRef(stm);
        BetaIntRef ref2 = new BetaIntRef(stm);

        CommitThread t1 = new CommitThread(1, ref1);
        CommitThread t2 = new CommitThread(2, ref2);

        startAll(t1, t2);
        sleepMs(1000);

        barrier.atomicVetoCommit();

        joinAll(t1, t2);

        assertEquals(1, ref1.___unsafeLoad().value);
        assertEquals(1, ref2.___unsafeLoad().value);
    }

    @Test
    public void testAbort() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        BetaIntRef ref1 = new BetaIntRef(stm);
        BetaIntRef ref2 = new BetaIntRef(stm);

        CommitThread t1 = new CommitThread(1, ref1);
        t1.setPrintStackTrace(false);
        CommitThread t2 = new CommitThread(2, ref2);
        t2.setPrintStackTrace(false);

        startAll(t1, t2);
        sleepMs(500);

        barrier.abort();

        t1.join();
        t2.join();

        assertEquals(0, ref1.___unsafeLoad().value);
        assertEquals(0, ref2.___unsafeLoad().value);
    }

    public class CommitThread extends TestThread {
        private BetaIntRef ref;

        public CommitThread(int id, BetaIntRef ref) {
            super("CommitThread-" + id);
            this.ref = ref;
        }

        @Override
        public void doRun() throws Exception {
            stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.getAndIncrement(tx, 1);
                    barrier.joinCommit(getThreadLocalTransaction());
                }
            });
        }
    }
}
