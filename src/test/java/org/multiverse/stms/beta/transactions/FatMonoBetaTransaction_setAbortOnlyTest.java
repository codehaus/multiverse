package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public class FatMonoBetaTransaction_setAbortOnlyTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenActive(){
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        tx.setAbortOnly();

        assertIsActive(tx);
        assertTrue((Boolean) getField(tx, "abortOnly"));
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
