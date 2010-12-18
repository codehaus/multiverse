package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.api.predicates.LongPredicate;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.execute;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.predicates.LongPredicate.newEqualsPredicate;
import static org.multiverse.api.predicates.LongPredicate.newLargerThanOrEqualsPredicate;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class BetaLongRef_await2WithPredicateTest {

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
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(2))
                .build()
                .newTransaction();


        try {
            ref.await(tx, newEqualsPredicate(initialValue + 1));
            fail();
        } catch (RetryTimeoutException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPredicateReturnsTrue() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();

        ref.await(tx, newEqualsPredicate(initialValue));

        assertIsActive(tx);
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

        try {
            ref.await(tx, predicate);
            fail();
        } catch (MyException expected) {
        }

        assertIsAborted(tx);
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

        try {
            ref.await(tx, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(null, predicate);
            fail();
        } catch (NullPointerException expected) {

        }

        verifyZeroInteractions(predicate);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(tx, predicate);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(predicate);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(tx, predicate);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(predicate);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        LongPredicate predicate = mock(LongPredicate.class);
        try {
            ref.await(tx, predicate);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        verifyZeroInteractions(predicate);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenSomeWaitingNeeded() {
        int initialValue = 0;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        AwaitThread thread1 = new AwaitThread(0, 10, ref);
        AwaitThread thread2 = new AwaitThread(1, 20, ref);
        thread1.start();
        thread2.start();

        sleepMs(500);
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

        assertVersionAndValue(ref, initialVersion + 2, 20);
    }

    public class AwaitThread extends TestThread {
        private long minimumValue;
        private LongRef ref;

        public AwaitThread(int id, long minimumValue, LongRef ref) {
            super("AwaitThread-" + id);
            this.minimumValue = minimumValue;
            this.ref = ref;
        }

        @Override
        public void doRun() throws Exception {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.await(tx, newLargerThanOrEqualsPredicate(minimumValue));
                }
            });
        }
    }
}