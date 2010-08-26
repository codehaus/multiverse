package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_commitTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenUnused() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        assertCommitted(tx);
    }

    /*
    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.registerPermanent(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    } */

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.register(listener);
        tx.commit();

        assertCommitted(tx);
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

        assertCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    } */

    @Test
    public void whenChangeListenerAvailableForUpdate_thenListenerNotified() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        FatMonoBetaTransaction listeningTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch, pool);

        FatMonoBetaTransaction updatingTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = updatingTx.openForWrite(ref, false, pool);
        write.value++;
        write.isDirty = DIRTY_TRUE;
        updatingTx.commit();

        assertTrue(latch.isOpen());
        assertHasNoListeners(ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenMultipleChangeListeners_thenAllNotified() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        List<Latch> listeners = new LinkedList<Latch>();
        for (int k = 0; k < 10; k++) {
            FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, false, pool);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit(pool);

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }

    @Test
    public void whenContainsOnlyNormalRead() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref.___getOrec());
        assertEquals(0, committed.value);
        assertUpdateBiased(ref.___getOrec());
        assertUnlocked(ref.___getOrec());
        assertReadonlyCount(oldReadonlyCount + 1, ref.___getOrec());
    }

    @Test
    public void whenContainsReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenContainsLockedNormalRead() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.commit(pool);

        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenContainsLockedReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.commit(pool);

        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenContainsLockedUpdate() {
        LongRef ref = createReadBiasedLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);
        write.value++;
        tx.commit(pool);

        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenNormalUpdate() {
        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = (LongRefTranlocal) tx.openForWrite(ref, false, pool);
        write.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(1, write.value);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setDirtyCheckEnabled(true));
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.commit(pool);

        assertFalse(write.isCommitted);
        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(1, ref.___getOrec());
    }

    @Test
    public void whenConstructed() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref, pool);
        write.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(1, write.value);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenWriteConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (WriteConflict e) {
        }

        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertUnlocked(ref.___getOrec());
    }

    @Test
    public void whenWriteConflictCausedByLock() {
        LongRef ref = createLongRef(stm, 0);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertLocked(ref);
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
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        //make almost read biased.
        for (int k = 0; k < ref.___getReadBiasedThreshold() - 1; k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, false, pool);
            tx.commit();
        }

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit();

        assertSame(committed, ref.___unsafeLoad());
        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertTrue(committed.isCommitted);
        assertFalse(committed.isPermanent);
        assertNull(committed.read);
        assertSame(ref, committed.owner);
        assertEquals(10, committed.value);
        assertUnlocked(ref.___getOrec());
        assertReadBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenUpdateReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.commit();

        assertCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(11, write.value);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void repeatedCommits() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
            tranlocal.value++;
            tx.commit(pool);
            tx.hardReset(pool);
        }

        assertEquals(100L, ref.___unsafeLoad().value);
    }

    @Test
    public void whenNotDirtyAndNotLocked() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.commit();

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNotDirtyAndLocked() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);
        tx.commit();

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNotDirtyAndNoDirtyCheck() {
        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setDirtyCheckEnabled(false));
        LongRefTranlocal write = (LongRefTranlocal) tx.openForWrite(ref, false, pool);
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(write, ref.___unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(0, write.value);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenNormalListenerAvailable() {
        LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.register(pool, listenerMock);
        tx.openForWrite(ref, false, pool);
        tx.commit();

        assertEquals(2, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PrePrepare, listenerMock.events.get(0));
        assertEquals(TransactionLifecycleEvent.PostCommit, listenerMock.events.get(1));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.registerPermanent(pool, listenerMock);
        tx.openForWrite(ref, false, pool);
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
        tx.prepare(pool);

        tx.commit();

        assertCommitted(tx);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);
        tx.prepare();

        tx.commit();
        assertCommitted(tx);

        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPreparedAndContainsUpdate() {
        LongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.prepare(pool);

        tx.commit();

        assertCommitted(tx);

        assertUnlocked(ref);
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

        LongRef ref = createLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commit(pool);

        assertCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenDirtyCheckAndCommute() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        LongRef ref = createLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commit(pool);

        assertCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    @Ignore("can't be done with a mono, try with an untracked read")
    public void whenInterleavingPossibleWithCommute() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref1, false, pool).value++;

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForWrite(ref2, false, pool).value++;
        otherTx.commit();

        tx.commute(ref2, pool, IncLongFunction.INSTANCE);
        tx.commit(pool);

        assertCommitted(tx);
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

        LongRef ref = createLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commit(pool);

        assertCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(3, ref.___unsafeLoad().value);
    }

    @Test
    public void whenCommuteAndLockedByOtherTransaction_thenWriteConflict() {
        LongRef ref = createLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true, pool);

        try {
            tx.commit(pool);
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
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
        } catch (WriteConflict conflict) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenPessimisticLockLevelWriteAndDirtyCheck() {
        LongRef ref = createLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write)
                .setDirtyCheckEnabled(true);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenPessimisticLockLevelReadAndDirtyCheck() {
        LongRef ref = createLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read)
                .setDirtyCheckEnabled(true);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        tx.commit(pool);
        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenIllegalStateException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertAborted(tx);
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
        LongRef ref = createLongRef(stm);
        long created = 0;

        //execute all transactions
        Random random = new Random();
        int transactionCount = 10000;
        for (int transaction = 0; transaction < transactionCount; transaction++) {
            FatMonoBetaTransaction tx = new FatMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setDirtyCheckEnabled(dirtyCheck));

             if (random.nextInt(3) == 1) {
                tx.openForWrite(ref, false, pool).value++;
                created++;
            } else {
                tx.openForWrite(ref, false, pool);
            }
            tx.commit(pool);
            tx.softReset();
        }

        long sum =  ref.___unsafeLoad().value;
        assertEquals(created, sum);
    }
}
