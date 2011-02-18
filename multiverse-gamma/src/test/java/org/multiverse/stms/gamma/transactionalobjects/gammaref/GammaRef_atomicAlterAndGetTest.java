package org.multiverse.stms.gamma.transactionalobjects.gammaref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmConfiguration;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.api.functions.Functions.identityLongFunction;
import static org.multiverse.api.functions.Functions.incLongFunction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaRef_atomicAlterAndGetTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        GammaStmConfiguration config = new GammaStmConfiguration();
        config.maxRetries = 10;
        stm = new GammaStm(config);
        clearThreadLocalTransaction();
    }

    @Test
    public void whenFunctionCausesException() {
        Long initialValue = 10L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        long orecValue = ref.orec;
        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        Long initialValue = 5L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.atomicAlterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenSuccess() {
        Long initialValue = 5L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = Functions.incLongFunction(1);

        long result = ref.atomicAlterAndGet(function);

        assertEquals(initialValue + 1, result);
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenNoChange() {
        Long initialValue = 5L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long version = ref.getVersion();

        LongFunction function = identityLongFunction();

        Long result = ref.atomicAlterAndGet(function);

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, version, initialValue);
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        Long initialValue = 5L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, 100L);

        LongFunction function = incLongFunction(1);

        long result = ref.atomicAlterAndGet(function);

        assertEquals(initialValue + 1, result);
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenLockedByOther(){
         whenLockedByOther(LockMode.Read);
        whenLockedByOther(LockMode.Write);
        whenLockedByOther(LockMode.Exclusive);
    }


    public void whenLockedByOther(LockMode lockMode) {
        Long initialValue = 5L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, lockMode);

        long orecValue = ref.orec;
        LongFunction function = mock(LongFunction.class);
        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (LockedException expected) {
        }

        assertOrecValue(ref, orecValue);
        verifyZeroInteractions(function);
        assertRefHasLockMode(ref, otherTx, lockMode.asInt());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenListenersAvailable() {
        Long initialValue = 10L;
        GammaRef<Long> ref = new GammaRef<Long>(stm, initialValue);
        long initialVersion = ref.getVersion();

        RefAwaitThread thread = new RefAwaitThread(ref, initialValue + 1);
        thread.start();

        sleepMs(500);

        ref.atomicAlterAndGet(Functions.incLongFunction());

        joinAll(thread);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }
}
