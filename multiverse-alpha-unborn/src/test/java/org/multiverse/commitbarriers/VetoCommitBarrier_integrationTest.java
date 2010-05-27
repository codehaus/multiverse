package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class VetoCommitBarrier_integrationTest {

    private VetoCommitBarrier barrier;

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
    public void test() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        IntRef ref1 = new IntRef();
        IntRef ref2 = new IntRef();

        CommitThread t1 = new CommitThread(1, ref1);
        CommitThread t2 = new CommitThread(2, ref2);

        startAll(t1, t2);
        sleepMs(1000);

        barrier.vetoCommit();

        joinAll(t1, t2);

        assertEquals(1, ref1.get());
        assertEquals(1, ref2.get());
    }

    @Test
    public void testAbort() throws InterruptedException {
        barrier = new VetoCommitBarrier();

        IntRef ref1 = new IntRef();
        IntRef ref2 = new IntRef();

        CommitThread t1 = new CommitThread(1, ref1);
        t1.setPrintStackTrace(false);
        CommitThread t2 = new CommitThread(2, ref2);
        t2.setPrintStackTrace(false);

        startAll(t1, t2);
        sleepMs(500);

        barrier.abort();

        t1.join();
        t2.join();

        assertEquals(0, ref1.get());
        assertEquals(0, ref2.get());
    }

    public class CommitThread extends TestThread {
        private IntRef ref;

        public CommitThread(int id, IntRef ref) {
            super("CommitThread-" + id);
            this.ref = ref;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            ref.inc();
            barrier.joinCommit(getThreadLocalTransaction());
        }
    }
}
