package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class VetoCommitBarrier_commitTest {

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
    public void whenNoPendingTransactions() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.commit();

        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenPendingTransactions() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();

        TransactionalInteger ref1 = new TransactionalInteger();
        TransactionalInteger ref2 = new TransactionalInteger();
        TransactionalInteger ref3 = new TransactionalInteger();

        IncThread thread1 = new IncThread(ref1, barrier);
        IncThread thread2 = new IncThread(ref2, barrier);
        IncThread thread3 = new IncThread(ref3, barrier);

        startAll(thread1, thread2, thread3);

        sleepMs(500);
        barrier.commit();
        joinAll(thread1, thread2, thread3);

        assertIsCommitted(thread1.tx);
        assertIsCommitted(thread2.tx);
        assertIsCommitted(thread3.tx);

        assertEquals(1, ref1.get());
        assertEquals(1, ref2.get());
        assertEquals(1, ref3.get());
    }

    @Test
    public void whenBarrierCommitted_thenIgnored() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.commit();

        barrier.commit();
        assertTrue(barrier.isCommitted());
    }

    @Test
    public void whenBarrierAborted_thenClosedCommitBarrierException() {
        VetoCommitBarrier barrier = new VetoCommitBarrier();
        barrier.abort();

        try {
            barrier.commit();
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }
        assertTrue(barrier.isAborted());
    }

    public class IncThread extends TestThread {
        private final TransactionalInteger ref;
        private final VetoCommitBarrier barrier;
        private Transaction tx;

        public IncThread(TransactionalInteger ref, VetoCommitBarrier barrier) {
            super("IncThread");
            this.barrier = barrier;
            this.ref = ref;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            ref.inc();
            barrier.awaitCommit(tx);
        }
    }
}
