package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_commitTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUnused() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        assertIsCommitted(tx);
    }


    @Test
    public void whenHasPrivatizedWrite() {
        BetaLongRef ref = newLongRef(stm);
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        ref.privatize(tx);
        ref.incrementAndGet(tx, 1);
        LongRefTranlocal committed = (LongRefTranlocal) tx.get(ref);
        tx.commit();

        assertIsCommitted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasEnsuredWrite() {
        BetaLongRef ref = newLongRef(stm);
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        ref.ensure(tx);
        ref.incrementAndGet(tx, 1);
        LongRefTranlocal committed = (LongRefTranlocal) tx.get(ref);
        tx.commit();

        assertIsCommitted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasPrivatizedRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        ref.privatize(tx);

        tx.commit();

        assertIsCommitted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount + 1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasEnsuredRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        ref.ensure(tx);

        tx.commit();

        assertIsCommitted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount + 1, ref);
        assertUpdateBiased(ref);
    }

    /*
    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.registerPermanent(listener);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    } */

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.register(listener);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    /*
    @Test
    public void whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst() {
        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.register(normalListener);
        tx.registerPermanent(permanentListener);
        tx.commit();

        assertIsCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    } */

    @Test
    public void whenChangeListenerAvailableForUpdate_thenListenerNotified() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction listeningTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, LOCKMODE_NONE);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch);

        FatMonoBetaTransaction updatingTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = updatingTx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        updatingTx.commit();

        assertTrue(latch.isOpen());
        assertHasNoListeners(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenMultipleChangeListeners_thenAllNotified() {
        BetaLongRef ref = newLongRef(stm);

        List<Latch> listeners = new LinkedList<Latch>();
        for (int k = 0; k < 10; k++) {
            FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, LOCKMODE_NONE);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }

    @Test
    public void whenContainsOnlyNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertEquals(0, committed.value);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertReadonlyCount(oldReadonlyCount + 1, ref);
    }

    @Test
    public void whenContainsReadBiasedRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.commit();

        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenContainsLockedNormalRead() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_COMMIT);
        tx.commit();

        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenContainsLockedReadBiasedRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_COMMIT);
        tx.commit();

        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenContainsLockedUpdate() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);
        write.value++;
        tx.commit();

        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenNormalUpdate() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = (LongRefTranlocal) tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertIsCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(1, write.value);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setDirtyCheckEnabled(true));
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertFalse(write.isCommitted);
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(1, ref);
    }

    @Test
    public void whenConstructed() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref);
        write.value++;
        tx.commit();

        assertIsCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(1, write.value);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, LOCKMODE_NONE);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict e) {
        }

        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenWriteConflictCausedByLock() {
        BetaLongRef ref = newLongRef(stm, 0);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenAlmostReadBiased() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        //make almost read biased.
        for (int k = 0; k < ref.___getReadBiasedThreshold() - 1; k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, LOCKMODE_NONE);
            tx.commit();
        }

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.commit();

        assertSame(committed, ref.___unsafeLoad());
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertTrue(committed.isCommitted);
        assertFalse(committed.isPermanent);
        assertNull(committed.read);
        assertSame(ref, committed.owner);
        assertEquals(10, committed.value);
        assertHasNoCommitLock(ref);
        assertReadBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenUpdateReadBiasedRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertIsCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(11, write.value);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void repeatedCommits() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
            tranlocal.value++;
            tx.commit();
            tx.hardReset();
        }

        assertEquals(100L, ref.___unsafeLoad().value);
    }

    @Test
    public void whenNotDirtyAndNotLocked() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNotDirtyAndLocked() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNotDirtyAndNoDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setDirtyCheckEnabled(false));
        LongRefTranlocal write = (LongRefTranlocal) tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(0, write.value);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.register(listenerMock);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertEquals(2, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PrePrepare, listenerMock.events.get(0));
        assertEquals(TransactionLifecycleEvent.PostCommit, listenerMock.events.get(1));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertEquals(2, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PrePrepare, listenerMock.events.get(0));
        assertEquals(TransactionLifecycleEvent.PostCommit, listenerMock.events.get(1));
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
    public void whenPreparedUnused() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare();

        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        tx.prepare();

        tx.commit();
        assertIsCommitted(tx);

        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPreparedAndContainsUpdate() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.prepare();

        tx.commit();

        assertIsCommitted(tx);

        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(write, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertTrue(write.isCommitted);
        assertSame(ref, write.owner);
        assertNull(write.read);
    }

    @Test
    public void whenNoDirtyCheckAndCommute() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        BetaLongRef ref = newLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenDirtyCheckAndCommute() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        BetaLongRef ref = newLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    @Ignore("can't be done with a mono, try with an untracked read")
    public void whenInterleavingPossibleWithCommute() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref1, LOCKMODE_NONE).value++;

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForWrite(ref2, LOCKMODE_NONE).value++;
        otherTx.commit();

        tx.commute(ref2, Functions.newIncLongFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);
        assertReadonlyCount(0, ref1);
        assertEquals(1, ref1.___unsafeLoad().value);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
        assertReadonlyCount(0, ref2);
        assertEquals(2, ref2.___unsafeLoad().value);
    }

    @Test
    public void whenMultipleCommutes() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        BetaLongRef ref = newLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(3, ref.___unsafeLoad().value);
    }

    @Test
    public void whenCommuteAndLockedByOtherTransaction_thenWriteConflict() {
        BetaLongRef ref = newLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, Functions.newIncLongFunction(1));

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(0, ref.___unsafeLoad().value);
    }

    @Test
    public void whenAbortOnly() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict conflict) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPessimisticLockLevelWriteAndDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites)
                .setDirtyCheckEnabled(true);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenPessimisticLockLevelReadAndDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
                .setDirtyCheckEnabled(true);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        tx.commit();
        assertIsCommitted(tx);
    }

    @Test
    @Ignore
    public void whenUndefined() {
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort();

        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void integrationTest_whenMultipleUpdatesAndDirtyCheckEnabled() {
        integrationTest_whenMultipleUpdatesAndDirtyCheck(true);
    }

    @Test
    public void integrationTest_whenMultipleUpdatesAndDirtyCheckDisabled() {
        integrationTest_whenMultipleUpdatesAndDirtyCheck(false);
    }

    public void integrationTest_whenMultipleUpdatesAndDirtyCheck(final boolean dirtyCheck) {
        BetaLongRef ref = newLongRef(stm);
        long created = 0;

        //execute all transactions
        Random random = new Random();
        int transactionCount = 10000;
        for (int transaction = 0; transaction < transactionCount; transaction++) {
            FatMonoBetaTransaction tx = new FatMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setDirtyCheckEnabled(dirtyCheck));

            if (random.nextInt(3) == 1) {
                tx.openForWrite(ref, LOCKMODE_NONE).value++;
                created++;
            } else {
                tx.openForWrite(ref, LOCKMODE_NONE);
            }
            tx.commit();
            tx.softReset();
        }

        long sum = ref.___unsafeLoad().value;
        assertEquals(created, sum);
    }
}
