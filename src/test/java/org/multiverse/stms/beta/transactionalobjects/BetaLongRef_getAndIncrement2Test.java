package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertCommitted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

/**
 * Tests {@link BetaLongRef}
 *
 * @author Peter Veentjer.
 */
public class BetaLongRef_getAndIncrement2Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenTransactionNull_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.getAndIncrement(null, 10);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        try {
            ref.getAndIncrement(tx, 10);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        try {
            ref.getAndIncrement(tx, 10);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        try {
            ref.getAndIncrement(tx, 10);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndIncrement(tx, 0);
        tx.commit();

        assertEquals(10, result);
        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertEquals(10, ref.___unsafeLoad().value);
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndIncrement(tx, 20);
        tx.commit();

        assertCommitted(tx);
        assertEquals(10, result);
        assertNotSame(committed, ref.___unsafeLoad());
        assertEquals(30, ref.___unsafeLoad().value);
    }
}
