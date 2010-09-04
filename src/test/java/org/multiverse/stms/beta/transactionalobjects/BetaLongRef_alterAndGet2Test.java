package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertCommitted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;


/**
 * Tests {@link BetaLongRef#alterAndGet(BetaTransaction, LongFunction)}.
 */
public class BetaLongRef_alterAndGet2Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(null, function);
            fail();
        } catch (NullPointerException expected) {
        }

        verifyZeroInteractions(function);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.getAndAlter(tx, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAbortedTransaction() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenAlterFunctionCausesProblems() {

    }

    @Test
    public void whenSuccess() {
        LongFunction function = new LongFunction() {
            @Override
            public long call(long current) {
                return current + 1;
            }
        };

        BetaLongRef ref = createLongRef(stm, 100);
        BetaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndAlter(tx, function);
        tx.commit();

        assertEquals(101, ref.___unsafeLoad().value);
        assertEquals(100, result);
    }
}
