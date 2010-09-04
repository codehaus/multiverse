package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertCommitted;
import static org.multiverse.stms.beta.BetaStmUtils.createRef;

public class BetaRef_isNull2Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenTransactionNull_thenNullPointerException() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.isNull(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.isNull(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.isNull(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.isNull(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNull() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isNull(tx);
        tx.commit();

        assertTrue(result);
        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNotNull() {
        BetaRef ref = createRef(stm, "foo");
        RefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isNull(tx);
        tx.commit();

        assertFalse(result);
        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNormalTransactionUsed() {
        BetaRef ref = createRef(stm);
        RefTranlocal committed = ref.___unsafeLoad();
        Transaction tx = stm.startDefaultTransaction();
        boolean result = ref.isNull(tx);
        tx.commit();

        assertTrue(result);
        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }
}
