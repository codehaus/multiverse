package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicAlterAndGetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        BetaStmConfiguration config = new BetaStmConfiguration();
        config.maxRetries = 10;
        stm = new BetaStm(config);
        clearThreadLocalTransaction();
    }

    @Test
    public void whenFunctionCausesException() {
        BetaLongRef ref = newLongRef(stm);

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.atomicGet());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm, 5);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.atomicAlterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertEquals(5, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm, 5);
        LongFunction function = Functions.newIncLongFunction(1);

        long result = ref.atomicAlterAndGet(function);

        assertEquals(6, result);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(6, ref.atomicGet());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = newLongRef(stm, 5);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = new IdentityLongFunction();

        long result = ref.atomicAlterAndGet(function);

        assertEquals(5, result);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(5, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm, 5);
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, 100);

        LongFunction function = Functions.newIncLongFunction(1);

        long result = ref.atomicAlterAndGet(function);

        assertEquals(6, result);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(6, ref.atomicGet());
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertSurplus(1, ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPrivatizedByOther() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertSurplus(1, ref);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }


}
