package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmUtils.newRef;

public class BetaRef_isNull1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenTransactionNull_thenNullPointerException() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();

        try {
            ref.isNull(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertVersionAndValue(ref, version, null);
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.isNull(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, null);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.isNull(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, null);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        BetaRef ref = newRef(stm);
        long version =ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.isNull(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, null);
    }

    @Test
    public void whenNull() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isNull(tx);
        tx.commit();

        assertTrue(result);
        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, null);
    }

    @Test
    public void whenNotNull() {
        String value = "foo";
        BetaRef ref = newRef(stm, value);
        long version =ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isNull(tx);
        tx.commit();

        assertFalse(result);
        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, value);
    }

    @Test
    public void whenNormalTransactionUsed() {
        BetaRef ref = newRef(stm);
        long version = ref.getVersion();
        Transaction tx = stm.startDefaultTransaction();
        boolean result = ref.isNull(tx);
        tx.commit();

        assertTrue(result);
        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, null);
    }
}
