package org.multiverse.utils;

import org.junit.Before;
import org.junit.Ignore;
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
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CommitGroup_awaitCommitTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder().build();
    }

    @Test
    public void whenTransactionNull_thenNullPointerException() throws InterruptedException {
        CommitGroup group = new CommitGroup();

        try {
            group.awaitCommit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(group.isOpen());
        assertEquals(0, group.getPreparedCount());
    }

    @Test
    public void whenTransactionPreparable_thenAdded() {
        CommitGroup group = new CommitGroup();
        TransactionalInteger ref = new TransactionalInteger();
        IncThread thread = new IncThread(ref, group);
        thread.start();

        sleepMs(500);
        assertAlive(thread);
        assertTrue(group.isOpen());
        assertEquals(1, group.getPreparedCount());
    }

    @Test
    @Ignore
    public void whenTransactionPrepared() {

    }

    @Test
    public void whenPrepareFails() throws InterruptedException {
        final CommitGroup group = new CommitGroup();
        final TransactionalInteger ref = new TransactionalInteger();

        FailToPrepareThread thread = new FailToPrepareThread(group, ref);
        thread.start();

        sleepMs(500);
        ref.inc();

        thread.join();
        thread.assertFailedWithException(TooManyRetriesException.class);
        assertEquals(0, group.getPreparedCount());
    }

    class FailToPrepareThread extends TestThread {
        final CommitGroup group;
        final TransactionalInteger ref;

        FailToPrepareThread(CommitGroup group, TransactionalInteger ref) {
            this.group = group;
            this.ref = ref;
            setPrintStackTrace(false);
        }

        @Override
        @TransactionalMethod(maxRetryCount = 0)
        public void doRun() throws Exception {
            sleepMs(1000);
            ref.inc();
            group.awaitCommit(getThreadLocalTransaction());
        }
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() throws InterruptedException {
        Transaction tx = txFactory.start();
        tx.abort();

        CommitGroup group = new CommitGroup();
        try {
            group.awaitCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertTrue(group.isOpen());
        assertIsAborted(tx);
        assertEquals(0, group.getPreparedCount());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() throws InterruptedException {
        Transaction tx = txFactory.start();
        tx.commit();

        CommitGroup group = new CommitGroup();
        try {
            group.awaitCommit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertTrue(group.isOpen());
        assertIsCommitted(tx);
        assertEquals(0, group.getPreparedCount());
    }

    @Test
    public void whenCommitGroupAborted_thenIllegalStateException() throws InterruptedException {
        CommitGroup group = new CommitGroup();
        group.abort();

        Transaction tx = txFactory.start();
        try {
            group.awaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsActive(tx);
        assertTrue(group.isAborted());
        assertEquals(0, group.getPreparedCount());
    }

    @Test
    public void whenCommitGroupCommitted_thenIllegalStateException() throws InterruptedException {
        CommitGroup group = new CommitGroup();
        group.commit();

        Transaction tx = txFactory.start();
        try {
            group.awaitCommit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsActive(tx);
        assertTrue(group.isCommitted());
        assertEquals(0, group.getPreparedCount());
    }

    public class IncThread extends TestThread {
        private final TransactionalInteger ref;
        private final CommitGroup group;
        private Transaction tx;

        public IncThread(TransactionalInteger ref, CommitGroup group) {
            super("IncThread");
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
