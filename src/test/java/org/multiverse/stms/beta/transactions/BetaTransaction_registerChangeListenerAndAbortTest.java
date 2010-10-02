package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_registerChangeListenerAndAbortTest implements BetaStmConstants {
    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    public abstract boolean isSupportingCommute();

    public abstract boolean isSupportingListeners();

    public abstract int getTransactionMaxCapacity();

    @Test
    public void whenMultipleReads_thenMultipleRegisters() {
        assumeTrue(getTransactionMaxCapacity()>=3);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref1, LOCKMODE_NONE);
        tx.openForRead(ref2, LOCKMODE_NONE);
        tx.openForRead(ref3, LOCKMODE_NONE);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertIsAborted(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    @Test
    public void whenOnlyContainsCommute_thenNoRetryPossibleException() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = new BetaLongRef(stm, 0);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        Latch listener = new CheapLatch();
        try {
            tx.registerChangeListenerAndAbort(listener);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsAborted(tx);
        assertHasNoListeners(ref);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        BetaTransaction tx = newTransaction();
        Latch latch = new CheapLatch();

        try {
            tx.registerChangeListenerAndAbort(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
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

        Latch latch = new CheapLatch();

        try {
            tx.registerChangeListenerAndAbort(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenContainsRead_thenSuccess() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertIsAborted(tx);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertSurplus(0, ref);
        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenContainsConstructed_thenNoRetryPossibleException() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);

        Latch listener = new CheapLatch();
        try {
            tx.registerChangeListenerAndAbort(listener);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsAborted(tx);
        assertHasNoListeners(ref);
        assertHasCommitLock(ref);
        //assertNull(ref.getLockOwner());
        //assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertNull(constructed.read);
        //    assertSame(ref, constructed.owner);
        //    assertFalse(constructed.isCommitted);
        //    assertFalse(constructed.isPermanent);
    }

    @Test
    public void whenOneOfThemItemsIsConstructed() {
        assumeTrue(getTransactionMaxCapacity()>=2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        Latch latch = new CheapLatch();

        BetaTransaction tx = newTransaction();
        BetaLongRef ref3 = new BetaLongRef(tx);
        tx.openForRead(ref1, LOCKMODE_NONE);
        LongRefTranlocal write3 = tx.openForConstruction(ref3);
        tx.openForWrite(ref2, LOCKMODE_NONE);
        tx.registerChangeListenerAndAbort(latch);

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
        Latch latch = new CheapLatch();
        tx.register(listenerMock);
        tx.openForRead(ref, LOCKMODE_NONE);

        tx.registerChangeListenerAndAbort(latch);

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
        Latch latch = new CheapLatch();
        tx.openForRead(ref, LOCKMODE_NONE);

        tx.registerChangeListenerAndAbort(latch);

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
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

    @Test
    public void whenAlreadyPrepared_thenPreparedTransactionException() {
        Latch latch = mock(Latch.class);
        BetaTransaction tx = newTransaction();
        tx.prepare();

        try {
            tx.registerChangeListenerAndAbort(latch);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(latch);
    }

   @Test
    public void whenCommitted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.registerChangeListenerAndAbort(latch);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        verifyZeroInteractions(latch);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        try {
            tx.registerChangeListenerAndAbort(latch);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(latch);
    }
}
