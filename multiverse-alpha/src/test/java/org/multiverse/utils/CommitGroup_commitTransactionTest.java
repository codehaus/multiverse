package org.multiverse.utils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class CommitGroup_commitTransactionTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder().build();
    }

    @Test
    public void whenNullTx_thenNullPointerException() {
        CommitGroup group = new CommitGroup();

        try {
            group.commit(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(group.isOpen());
    }

    @Test
    public void whenNoPendingTransactions() {
        CommitGroup group = new CommitGroup();

        Transaction tx = txFactory.start();
        group.commit(tx);

        assertTrue(group.isCommitted());
        assertIsCommitted(tx);
    }

    @Test
    @Ignore
    public void whenTransactionIsFirst() {

    }

    @Test
    @Ignore
    public void whenTransactionFailedToPrepare() {

    }

    @Test
    @Ignore
    public void whenPendingTransactions_thenTheyAreNotified() {

    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        CommitGroup group = new CommitGroup();

        Transaction tx = txFactory.start();
        tx.abort();

        try {
            group.commit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(group.isOpen());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        CommitGroup group = new CommitGroup();

        Transaction tx = txFactory.start();
        tx.commit();

        try {
            group.commit(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(group.isOpen());
    }

    @Test
    public void whenGroupCommitted_thenIllegalStateException() {
        CommitGroup group = new CommitGroup();
        group.commit();

        Transaction tx = txFactory.start();
        try {
            group.commit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(group.isCommitted());
        assertIsActive(tx);
    }

    @Test
    public void whenGroupAborted_thenIllegalStateException() {
        CommitGroup group = new CommitGroup();
        group.abort();

        Transaction tx = txFactory.start();
        try {
            group.commit(tx);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(group.isAborted());
        assertIsActive(tx);
    }

}
