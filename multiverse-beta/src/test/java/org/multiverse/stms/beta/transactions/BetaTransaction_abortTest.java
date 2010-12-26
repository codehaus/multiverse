package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.MultiverseConstants.LOCKMODE_NONE;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

public abstract class BetaTransaction_abortTest {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean doesTransactionSupportCommute();

    public abstract int getTransactionMaxCapacity();

    public abstract boolean doesSupportListeners();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUnused() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenContainsNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenContainsReadBiasedRead_thenSurplusRemains() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        //once arrived, a depart will not be called on a readbiased tranlocal
        assertSurplus(1, ref);
    }

    @Test
    public void whenContainsWriteBasedOnNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenContainsWriteBasedOnReadBiasedRead_thenSurplusRemains() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, version, 0);
    }


    // ============== listeners ===========================

    @Test
    public void listeners_whenNormalListenerAvailable() {
        assumeTrue(doesSupportListeners());

        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransaction tx = newTransaction();
        tx.register(listenerMock);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.abort();

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void listeners_whenPermanentListenerAvailable() {
        assumeTrue(doesSupportListeners());

        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        BetaTransaction tx = newTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.abort();

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

    // ============== locking ====================

    @Test
    public void locking_whenHasPrivatizedWrite() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        ref.getLock().acquireCommitLock(tx);

        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void locking_whenHasEnsuredWrite() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        ref.getLock().acquireWriteLock(tx);

        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void locking_whenHasPrivatizedRead() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        ref.getLock().acquireCommitLock(tx);

        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
        assertSurplus(0, ref);

        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void locking_whenHasEnsuredRead() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        ref.getLock().acquireWriteLock(tx);

        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void locking_whenHasConstructed_thenRemainLocked() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal write = tx.openForConstruction(ref);
        tx.abort();

        assertIsAborted(tx);

        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0, 0);
        assertFalse(write.hasDepartObligation());
        assertFalse(write.isReadonly());
    }

    // ================= state ===================

    @Test
    public void state_whenAlreadyPrepared_thenAborted() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void state_whenAlreadyAborted_thenIgnored() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void state_whenAlreadyCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.abort();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
