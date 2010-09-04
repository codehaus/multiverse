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
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertCommitted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

/**
 * Tests {@link BetaLongRef#getAndSet(org.multiverse.api.Transaction, long)}.
 *
 * @author Peter Veentjer.
 */
public class BetaLongRef_getAndSet2Test {

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
            ref.getAndSet(null, 11);
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
            ref.getAndSet(tx, 11);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.getAndSet(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransaction_thenCommittedTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.getAndSet(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        long result = ref.getAndSet(tx, 20);
        tx.commit();

        assertEquals(10, result);
        assertEquals(20, ref.___unsafeLoad().value);
    }

    @Test
    public void whenNormalTransactionUsed() {
        BetaLongRef ref = createLongRef(stm, 10);

        Transaction tx = new FatMonoBetaTransaction(stm);
        long result = ref.getAndSet(tx, 20);
        tx.commit();

        assertEquals(10, result);
        assertEquals(20, ref.___unsafeLoad().value);
    }
}
