package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
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
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_registerChangeListenerAndAbortTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenMultipleReads_thenMultipleRegisters() {
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref3 = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref1, false);
        tx.openForRead(ref2, false);
        tx.openForRead(ref3, false);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertIsAborted(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    @Test
    public void whenOnlyContainsCommute_thenNoRetryPossibleException(){
        BetaLongRef ref = new BetaLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref,function);

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
    public void whenContainsConstructed_thenNoRetryPossibleException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertNull(constructed.read);
    }

    @Test
    public void whenOneOfThemItemsIsConstructed() {
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);

        Latch latch = new CheapLatch();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref3 = new BetaLongRef(tx);
        tx.openForRead(ref1, false);
        LongRefTranlocal write3 = tx.openForConstruction(ref3);
        tx.openForWrite(ref2, false);
        tx.registerChangeListenerAndAbort(latch);

        assertIsAborted(tx);
        assertHasNoListeners(ref3);
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.openForRead(ref, false);

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
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);

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
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        Latch latch = new CheapLatch();
        tx.register(listenerMock);
        tx.openForRead(ref, false);

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
    public void whenPrepared_thenPreparedTransactionException() {
        Latch latch = mock(Latch.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    @Ignore
    public void whenUndefined(){}

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
