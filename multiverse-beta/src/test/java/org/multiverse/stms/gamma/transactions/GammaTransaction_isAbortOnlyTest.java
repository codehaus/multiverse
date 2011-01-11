package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public abstract class GammaTransaction_isAbortOnlyTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    @Test
    public void whenActiveAndNotSetAbortOnly() {
        T tx = newTransaction();

        boolean result = tx.isAbortOnly();

        assertFalse(result);
        assertIsActive(tx);
    }

    @Test
    public void whenActiveAndSetAbortOnly() {
        T tx = newTransaction();
        tx.setAbortOnly();

        boolean result = tx.isAbortOnly();

        assertTrue(result);
        assertIsActive(tx);
    }


    @Test
    public void whenPreparedAndNotSetAbortOnly() {
        T tx = newTransaction();
        tx.prepare();

        boolean result = tx.isAbortOnly();

        assertFalse(result);
        assertIsPrepared(tx);
    }


    @Test
    public void whenPreparedAndSetAbortOnly() {
        T tx = newTransaction();
        tx.setAbortOnly();
        tx.prepare();

        boolean result = tx.isAbortOnly();

        assertTrue(result);
        assertIsPrepared(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.abort();
        try {
            tx.isAbortOnly();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.commit();
        try {
            tx.isAbortOnly();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
