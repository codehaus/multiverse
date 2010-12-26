package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.functions.Functions.newIdentityLongFunction;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

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
    public void whenSuccess() {
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = newIncLongFunction();
        long result = ref.atomicGetAndAlter(function);

        assertEquals(2, result);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion+1, initialValue+1);
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.atomicGetAndAlter(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        LongFunction function = newIncLongFunction();
        long result = ref.atomicGetAndAlter(function);

        tx.abort();

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion+1, initialValue+1);
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicGetAndAlter(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertRefHasCommitLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOtherAndNothingDirty_thenLockedException() {
        long initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        try {
            ref.atomicGetAndAlter(newIdentityLongFunction());
            fail();
        } catch (LockedException expected) {
        }

        assertRefHasUpdateLock(ref, otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        int initialValue = 2;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicGetAndAlter(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertRefHasUpdateLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + 1);
        thread.start();

        sleepMs(500);

        long result = ref.atomicGetAndAlter(newIncLongFunction());

        assertEquals(result, initialValue);
        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }
}
