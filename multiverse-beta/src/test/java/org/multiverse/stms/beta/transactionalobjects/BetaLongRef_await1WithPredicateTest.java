package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.predicates.LongPredicate;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.predicates.LongPredicate.newEqualsPredicate;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class BetaLongRef_await1WithPredicateTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPredicateEvaluatesToFalse() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .build()
                .newTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.await(newEqualsPredicate(initialValue + 1));
            fail();
        } catch (Retry expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPredicateReturnsTrue() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.await(newEqualsPredicate(initialValue));

        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPredicateThrowsException_thenTransactionAborted() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongPredicate predicate = mock(LongPredicate.class);

        when(predicate.evaluate(initialValue)).thenThrow(new MyException());

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.await(predicate);
            fail();
        } catch (MyException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    class MyException extends RuntimeException {
    }

    @Test
    public void whenNullPredicate_thenTransactionAbortedAndNullPointerException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.await(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNoTransaction_thenTransactionRequiredException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(predicate);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        verifyZeroInteractions(predicate);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(predicate);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(predicate);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(predicate);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        verifyZeroInteractions(predicate);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(predicate);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        verifyZeroInteractions(predicate);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenSomeWaitingNeeded() {
        int initialValue = 0;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefAwaitThread thread1 = new LongRefAwaitThread(ref, 10);
        LongRefAwaitThread thread2 = new LongRefAwaitThread(ref, 20);
        thread1.start();
        thread2.start();

        sleepMs(1000);
        assertAlive(thread1, thread2);

        ref.atomicSet(10);

        sleepMs(500);

        assertNotAlive(thread1);
        thread1.assertNothingThrown();
        assertAlive(thread2);

        ref.atomicSet(20);

        sleepMs(500);
        assertNotAlive(thread2);
        thread2.assertNothingThrown();

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 2, 20);
    }
}
