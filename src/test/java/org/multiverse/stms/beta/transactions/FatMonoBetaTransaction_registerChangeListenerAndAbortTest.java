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
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
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
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
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
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenExplicitRetryNotAllowed_thenNoRetryPossibleException() {
        LongRef ref = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setBlockingAllowed(false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenContainsRead_thenSuccess() {
        LongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertAborted(tx);
    }

    @Test
    public void whenLockedRead_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsWrite_thenSuccess() {
        LongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertAborted(tx);
    }

    @Test
    public void whenLockedWrite_thenSuccessAndLockReleased() {
        LongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch, pool);

        assertFalse(latch.isOpen());
        assertSurplus(0, ref);
        assertAborted(tx);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
    }

    @Test
    public void whenContainsConstructed_thenNoRetryPossibleException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);

        Latch listener = new CheapLatch();
        try {
            tx.registerChangeListenerAndAbort(listener, pool);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertAborted(tx);
        assertHasNoListeners(ref);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.unsafeLoad());
    }

      @Test
    public void whenNormalListenerAvailable() {
        LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        Latch latch = new CheapLatch();
        tx.register(pool, listenerMock);
        tx.openForRead(ref,false,pool);

        tx.registerChangeListenerAndAbort(latch,pool);

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenPermanentListenerAvailable() {
         LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        Latch latch = new CheapLatch();
        tx.registerPermanent(pool, listenerMock);
        tx.openForRead(ref,false,pool);

        tx.registerChangeListenerAndAbort(latch,pool);

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
        tx.prepare(pool);

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        verifyZeroInteractions(latch);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        verifyZeroInteractions(latch);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        Latch latch = mock(Latch.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.registerChangeListenerAndAbort(latch, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
        verifyZeroInteractions(latch);
    }
}
