package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

/**
 * Tests {@link BetaLongRef#set(BetaTransaction, long)}.
 *
 * @author Peter Veentjer.
 */
public class BetaLongRef_set2Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullTransaction() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.set(null, 11);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.set(tx, 11);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.set(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransaction_thenCommittedTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.set(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        long result = ref.set(tx, 20);
        tx.commit();

        assertEquals(20, result);
        assertEquals(20, ref.___unsafeLoad().value);
    }

    @Test
    public void whenNormalTransactionMethodCalled() {
        BetaLongRef ref = createLongRef(stm, 10);

        Transaction tx = new FatMonoBetaTransaction(stm);
        long result = ref.set(tx, 20);
        tx.commit();

        assertEquals(20, result);
        assertEquals(20, ref.___unsafeLoad().value);
    }
}
