package org.multiverse.utils;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CommitGroup_abortTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder().build();
    }

    @Test
    public void whenNoPreparedTransactions() {
        CommitGroup group = new CommitGroup();

        group.abort();
        assertTrue(group.isAborted());
    }

    @Test
    public void whenPendingTransactions_theyAreAborted() throws InterruptedException {
        CommitGroup group = new CommitGroup();
        TransactionalInteger ref = new TransactionalInteger();
        IncThread thread1 = new IncThread(ref, group);
        IncThread thread2 = new IncThread(ref, group);

        startAll(thread1, thread2);

        sleepMs(500);
        group.abort();
        thread1.join();
        thread2.join();

        assertEquals(0, ref.get());
        assertIsAborted(thread1.tx);
        assertIsAborted(thread2.tx);
        thread1.assertFailedWithException(DeadTransactionException.class);
        thread2.assertFailedWithException(DeadTransactionException.class);
    }

    @Test
    public void whenCommitGroupAborted_thenCallIgnored() {
        CommitGroup group = new CommitGroup();
        group.abort();

        group.abort();
        assertTrue(group.isAborted());
    }

    @Test
    public void whenCommitGroupCommitted_thenIllegalStateException() {
        CommitGroup group = new CommitGroup();
        group.commit();

        try {
            group.abort();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(group.isCommitted());
    }

    public class IncThread extends TestThread {
        private final TransactionalInteger ref;
        private final CommitGroup group;
        private Transaction tx;

        public IncThread(TransactionalInteger ref, CommitGroup group) {
            super("IncThread");
            setPrintStackTrace(false);
            this.group = group;
            this.ref = ref;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            tx = getThreadLocalTransaction();
            ref.inc();
            group.awaitCommit(tx);
        }
    }
}
