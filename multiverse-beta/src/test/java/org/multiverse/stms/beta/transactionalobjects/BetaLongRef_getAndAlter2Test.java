package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;


/**
 * Tests {@link BetaLongRef#alterAndGet(BetaTransaction, LongFunction)}.
 */
public class BetaLongRef_getAndAlter2Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();
        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(null, function);
            fail();
        } catch (NullPointerException expected) {
        }

        verifyZeroInteractions(function);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.getAndAlter(tx, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenAbortedTransaction() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenAlterFunctionCausesProblems_thenTransactionAborted() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        when(function.call(initialValue)).thenThrow(new MyException());

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getAndAlter(tx, function);
            fail();
        } catch (MyException expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    class MyException extends RuntimeException{
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + 1);
        thread.start();

        sleepMs(500);

        BetaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndAlter(tx, newIncLongFunction());
        tx.commit();

        joinAll(thread);

        assertEquals(result, initialValue);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
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
        long result = ref.getAndAlter(tx, function);
        tx.commit();

        assertEquals(101, ref.atomicGet());
        assertEquals(100, result);
    }
}