package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicGetAndAlterTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        BetaStmConfiguration config = new BetaStmConfiguration();
        config.maxRetries = 10;
        stm = new BetaStm(config);
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm, 2);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        long result = ref.atomicGetAndAlter(function);

        assertEquals(2, result);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(3, ref.atomicGet());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.atomicGetAndAlter(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___getLockOwner());
        assertEquals(0, ref.atomicGet());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm, 2);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        long result = ref.atomicGetAndAlter(function);

        tx.abort();

        assertEquals(2, result);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(3, ref.atomicGet());
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm, 2);
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicGetAndAlter(function);
            fail();
        } catch (LockedException expected) {

        }

        verifyZeroInteractions(function);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenEnsuredByOtherAndNothingDirty() {
        BetaLongRef ref = newLongRef(stm, 2);
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        long result = ref.atomicGetAndAlter(new IdentityLongFunction());

        assertEquals(2, result);
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        BetaLongRef ref = newLongRef(stm, 2);
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicGetAndAlter(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
    }
}
