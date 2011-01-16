package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.functions.Functions.newIdentityLongFunction;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaLongRef_atomicGetAndAlterTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        GammaStmConfiguration config = new GammaStmConfiguration();
        config.maxRetries = 10;
        stm = new GammaStm(config);
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        long initialValue = 2;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = newIncLongFunction();
        long result = ref.atomicGetAndAlter(function);

        assertEquals(2, result);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.atomicGetAndAlter(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        long initialValue = 2;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        LongFunction function = newIncLongFunction();
        long result = ref.atomicGetAndAlter(function);

        tx.abort();

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenPrivatizedByOther_thenLockedException() {
        long initialValue = 2;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicGetAndAlter(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertRefHasCommitLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertSurplus(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOtherAndNothingDirty_thenLockedException() {
        long initialValue = 2;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        try {
            ref.atomicGetAndAlter(newIdentityLongFunction());
            fail();
        } catch (LockedException expected) {
        }

        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        int initialValue = 2;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicGetAndAlter(function);
            fail();
        } catch (LockedException expected) {
        }

        verifyZeroInteractions(function);
        assertRefHasWriteLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertSurplus(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenListenersAvailable() {
        /*
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + 1);
        thread.start();

        sleepMs(500);

        long result = ref.atomicGetAndAlter(newIncLongFunction());

        assertEquals(result, initialValue);
        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        */
    }
}
