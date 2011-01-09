package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.api.functions.Functions.newIdentityLongFunction;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaLongRef_alterAndGet1Test {
     private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenActiveTransactionAvailableAndNullFunction_thenNullPointerException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.alterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenFunctionCausesException() {
        GammaLongRef ref = new GammaLongRef(stm);

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        LongFunction function = newIncLongFunction();
        ref.alterAndGet(function);
        assertEquals(initialValue + 1, ref.get());
        assertVersionAndValue(ref, initialVersion, initialValue);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenActiveTransactionAvailableButNoChange_thenNoWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        LongFunction function = newIdentityLongFunction();
        ref.alterAndGet(function);
        assertEquals(initialValue, ref.get());
        assertVersionAndValue(ref, initialVersion, initialValue);
        tx.commit();

        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        GammaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        verifyZeroInteractions(function);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();
        LongFunction function = Functions.newIncLongFunction(1);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();
        LongFunction function = Functions.newIncLongFunction(1);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();
        LongFunction function = newIncLongFunction(1);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenListenersAvailable_thenTheyAreNotified() {
        /*
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + 1);
        thread.start();

        sleepMs(500);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.alterAndGet(newIncLongFunction());
        tx.commit();

        joinAll(thread);

        assertVersionAndValue(ref, initialVersion+1,initialValue+1);*/
    }
}
