package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * Tests {@link BetaLongRef#alterAndGet(BetaTransaction, LongFunction)}.
 *
 * @author Peter Veentjer.
 */
public class BetaLongRef_alterAndGet2Test implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();
        LongFunction function = mock(LongFunction.class);

        try {
            ref.alterAndGet(null, function);
            fail();
        } catch (NullPointerException expected) {
        }

        verifyZeroInteractions(function);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.alterAndGet(tx, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.alterAndGet(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.alterAndGet(tx, function);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAbortedTransaction() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.alterAndGet(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenFunctionCausesException() {
        BetaLongRef ref = newLongRef(stm);

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.alterAndGet(tx, function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertIsAborted(tx);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenPrivatizedByOther() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongFunction function = mock(LongFunction.class);

        try {
            ref.alterAndGet(tx, function);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(1, ref);
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    
    @Test
    public void whenEnsuredByOther_thenOperationSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongFunction function = Functions.newLongIncFunction(1);
        ref.alterAndGet(tx, function);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(1, ref);
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {
    }

    @Test
    public void whenSuccess() {
        LongFunction function = new LongFunction() {
            @Override
            public long call(long current) {
                return current + 1;
            }
        };

        BetaLongRef ref = newLongRef(stm, 100);
        BetaTransaction tx = stm.startDefaultTransaction();
        long result = ref.alterAndGet(tx, function);
        tx.commit();

        assertEquals(101, ref.atomicGet());
        assertEquals(101, result);
    }
}
