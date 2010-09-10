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
import static org.multiverse.stms.beta.BetaStmUtils.createRef;

public class BetaRef_isNull1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
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

        assertIsAborted(tx);
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

        assertIsCommitted(tx);
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

        assertIsAborted(tx);
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
        assertIsCommitted(tx);
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
        assertIsCommitted(tx);
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
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }
}
