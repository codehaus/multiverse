package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class VetoCommitBarrier_abortTest {
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
    public void whenNoPreparedTransactions() {
        barrier = new VetoCommitBarrier();

        barrier.abort();
        assertTrue(barrier.isAborted());
    }

    @Test
    public void whenPendingTransactions_theyAreAborted() throws InterruptedException {
        barrier = new VetoCommitBarrier();
        IntRef ref = new IntRef();
        IncThread thread1 = new IncThread(ref);
        IncThread thread2 = new IncThread(ref);

        startAll(thread1, thread2);

        sleepMs(500);
        barrier.abort();
        thread1.join();
        thread2.join();

        assertEquals(0, ref.get());
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
        barrier.vetoCommit();

        try {
            barrier.abort();
            fail();
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
    }

    public class IncThread extends TestThread {
        private final IntRef ref;
        private Transaction tx;

        public IncThread(IntRef ref) {
            super("IncThread");
            setPrintStackTrace(false);
            this.ref = ref;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            ref.inc();
            barrier.joinCommit(tx);
        }
    }
}
