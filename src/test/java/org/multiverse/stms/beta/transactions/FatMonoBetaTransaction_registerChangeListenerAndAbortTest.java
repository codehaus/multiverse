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
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_registerChangeListenerAndAbortTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void whenUnstarted() {

    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        Latch latch = new CheapLatch();

        try {
            tx.registerChangeListenerAndAbort(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsAborted(tx);
    }

       @Test
    public void whenOnlyContainsCommute_thenNoRetryPossibleException(){
        BetaLongRef ref = new BetaLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref,function);

        Latch listener = new CheapLatch();
        try {
            tx.registerChangeListenerAndAbort(listener);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsAborted(tx);
        assertHasNoListeners(ref);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenExplicitRetryNotAllowed_thenNoRetryPossibleException() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
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
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertIsAborted(tx);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertIsAborted(tx);
    }

    @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsConstructed_thenNoRetryPossibleException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        Latch latch = new CheapLatch();
        tx.register(listenerMock);
        tx.openForRead(ref, false);

        tx.registerChangeListenerAndAbort(latch);

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        BetaLongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        Latch latch = new CheapLatch();
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
    public void whenAlreadyPrepared_thenPreparedTransactionException() {
        Latch latch = mock(Latch.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
    public void whenAlreadyAborted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
