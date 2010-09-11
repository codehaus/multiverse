package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaLongRef_commute2Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }


    @Test
    @Ignore
    public void whenFunctionCausesException() {
    }

    @Test
    @Ignore
    public void whenLocked() {

    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 10);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.commute(tx, function);

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);
        assertTrue(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertEquals(0, commute.value);
        tx.commit();

        assertEquals(11, ref.get());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = new IdentityLongFunction();
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.commute(tx, function);

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);
        assertTrue(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertEquals(0, commute.value);
        tx.commit();

        assertEquals(10, ref.get());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNormalTransactionUsed() {
        BetaLongRef ref = createLongRef(stm, 10);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        Transaction tx = stm.startDefaultTransaction();
        ref.commute(tx, function);
        tx.commit();

        assertEquals(11, ref.get());
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = createLongRef(stm, 10);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);
        ref.commute(tx, function);

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertEquals(11, commute.value);
        tx.commit();

        assertEquals(11, ref.get());
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);
        ref.commute(tx, function);

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertEquals(1, commute.value);
        tx.commit();

        assertEquals(1, ref.get());
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = createLongRef(stm, 10);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, 11);
        ref.commute(tx, function);

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);
        assertFalse(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertEquals(12, commute.value);
        tx.commit();

        assertEquals(12, ref.get());
    }

    @Test
    public void whenAlreadyCommuting() {
        BetaLongRef ref = createLongRef(stm, 10);

        LongFunction function1 = IncLongFunction.INSTANCE_INC_ONE;
        LongFunction function2 = IncLongFunction.INSTANCE_INC_ONE;
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.commute(tx, function1);
        ref.commute(tx, function2);

        LongRefTranlocal commute = (LongRefTranlocal) tx.get(ref);
        assertTrue(commute.isCommuting);
        assertFalse(commute.isCommitted);
        assertEquals(0, commute.value);
        tx.commit();

        assertEquals(12, ref.get());
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.commute(null, function);
            fail();
        } catch (NullPointerException expected) {

        }

        verifyZeroInteractions(function);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.commute(tx, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.commute(tx, function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        verifyZeroInteractions(function);
        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.commute(tx, function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        verifyZeroInteractions(function);
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.commute(tx, function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        verifyZeroInteractions(function);
        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }
}
