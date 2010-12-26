package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

public abstract class BetaTransaction_retryTest implements BetaStmConstants {
    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    public abstract boolean isSupportingCommute();

    public abstract boolean isSupportingListeners();

    public abstract int getTransactionMaxCapacity();

    @Test
    public void whenMultipleReads_thenMultipleRegisters() {
        assumeTrue(getTransactionMaxCapacity() >= 3);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref1, LOCKMODE_NONE);
        tx.openForRead(ref2, LOCKMODE_NONE);
        tx.openForRead(ref3, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {
        }

        assertIsAborted(tx);
        RetryLatch latch = getFirstListener(ref1);

        assertFalse(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    class RetryThread extends TestThread {
        final Transaction tx;

        RetryThread(Transaction tx) {
            this.tx = tx;
        }

        @Override
        public void doRun() throws Exception {
            tx.retry();
        }
    }

    @Test
    public void whenOnlyContainsCommute_thenNoRetryPossibleException() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = new BetaLongRef(stm, 0);
        long version = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.retry();
            fail();
        } catch (RetryNotPossibleException expected) {
        }

        assertIsAborted(tx);
        assertHasNoListeners(ref);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.retry();
            fail();
        } catch (RetryNotPossibleException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenExplicitRetryNotAllowed_thenNoRetryPossibleException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false);

        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (RetryNotAllowedException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenContainsRead_thenSuccess() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {

        }
        RetryLatch latch = getFirstListener(ref);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {
        }

        RetryLatch latch = getFirstListener(ref);

        assertSurplus(0, ref);
        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {

        }

        RetryLatch latch = getFirstListener(ref);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_COMMIT);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {

        }

        RetryLatch latch = getFirstListener(ref);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenContainsConstructed_thenNoRetryPossibleException() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);

        try {
            tx.retry();
            fail();
        } catch (RetryNotPossibleException expected) {
        }

        assertIsAborted(tx);
        assertHasNoListeners(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0, 0);
    }

    @Test
    public void whenOneOfThemItemsIsConstructed() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRef ref3 = new BetaLongRef(tx);
        tx.openForRead(ref1, LOCKMODE_NONE);
        BetaLongRefTranlocal write3 = tx.openForConstruction(ref3);
        tx.openForWrite(ref2, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {

        }


        RetryLatch latch = getFirstListener(ref1);

        assertIsAborted(tx);
        assertHasNoListeners(ref3);
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
    }

    @Test
    public void whenNormalListenerAvailable() {
        assumeTrue(isSupportingListeners());

        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransaction tx = newTransaction();

        tx.register(listenerMock);
        tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {
        }

        RetryLatch latch = getFirstListener(ref);

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        assumeTrue(isSupportingListeners());

        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.retry();
            fail();
        } catch (Retry retry) {
        }

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenAlreadyPrepared_thenPreparedTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        try {
            tx.retry();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.retry();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        try {
            tx.retry();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    class TransactionLifecycleListenerMock implements TransactionLifecycleListener {
        List<Transaction> transactions = new LinkedList<Transaction>();
        List<TransactionLifecycleEvent> events = new LinkedList<TransactionLifecycleEvent>();

        @Override
        public void notify(Transaction transaction, TransactionLifecycleEvent e) {
            transactions.add(transaction);
            events.add(e);
        }
    }

}
