package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public abstract class BetaTransaction_setAbortOnlyTest {

    protected BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    public abstract BetaTransaction newTransaction();

    @Test
    @Ignore
    public void whenUndefined() {
    }

    @Test
    public void whenActive() {
        BetaTransaction tx  = newTransaction();

        tx.setAbortOnly();

        assertIsActive(tx);
        assertTrue((Boolean) getField(tx, "abortOnly"));
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaTransaction tx  = newTransaction();
        tx.prepare();

        try {
            tx.setAbortOnly();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertFalse((Boolean) getField(tx, "abortOnly"));
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaTransaction tx  = newTransaction();
        tx.abort();

        try {
            tx.setAbortOnly();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertFalse((Boolean) getField(tx, "abortOnly"));
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx  = newTransaction();
        tx.commit();

        try {
            tx.setAbortOnly();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertFalse((Boolean) getField(tx, "abortOnly"));
    }
}
