package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
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
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.api.functions.Functions.newIdentityLongFunction;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

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
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        int initialValue = 5;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.atomicAlterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenSuccess() {
        int initialValue = 5;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = Functions.newIncLongFunction(1);

        long result = ref.atomicAlterAndGet(function);

        assertEquals(initialValue+1, result);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion+1, initialValue+1);
    }

    @Test
    public void whenNoChange() {
        int initialValue = 5;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long version = ref.getVersion();

        LongFunction function = newIdentityLongFunction();

        long result = ref.atomicAlterAndGet(function);

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, version, initialValue);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        long initialValue = 5;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, 100);

        LongFunction function = newIncLongFunction(1);

        long result = ref.atomicAlterAndGet(function);

        assertEquals(initialValue + 1, result);
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenEnsuredByOther_thenLockedException() {
        int initialValue = 5;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

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
        assertRefHasUpdateLock(ref, otherTx);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedByOther() {
        int initialValue = 5;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

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
        assertRefHasCommitLock(ref, otherTx);
        assertUpdateBiased(ref);
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

        ref.atomicAlterAndGet(newIncLongFunction());

        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }
}
