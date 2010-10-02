package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.IsolationLevel;
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

import static java.lang.Math.min;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.functions.Functions.newLongIncFunction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_commitTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean isTransactionSupportingCommute();

    public abstract int getTransactionMaxCapacity();

    public abstract boolean isSupportingListeners();

    public abstract boolean isSupportingWriteSkewDetection();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Ignore
    public void whenUndefined() {

    }

    @Test
    public void whenConstructed() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value++;
        tx.commit();

        assertIsCommitted(tx);
        assertSame(tranlocal, ref.___unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenContainsOnlyNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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
    public void whenNormalUpdate() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
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
    public void whenAlmostReadBiased() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        //make almost read biased.
        for (int k = 0; k < ref.___getReadBiasedThreshold() - 1; k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, LOCKMODE_NONE);
            tx.commit();
        }

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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
    public void whenNotDirtyAndNotLocked() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
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
    public void whenNotDirtyAndNoDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setDirtyCheckEnabled(false);
        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
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

    @Test
    public void whenOnlyConstructedObjects() {
        assumeTrue(getTransactionMaxCapacity() > 1);

        BetaTransaction tx = newTransaction();
        BetaLongRef ref1 = new BetaLongRef(tx);
        LongRefTranlocal constructed1 = tx.openForConstruction(ref1);
        BetaLongRef ref2 = new BetaLongRef(tx);
        LongRefTranlocal constructed2 = tx.openForConstruction(ref2);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(constructed1, ref1.___unsafeLoad());
        assertTrue(constructed1.isCommitted);
        assertFalse(constructed1.isPermanent);
        assertEquals(DIRTY_FALSE, constructed1.isDirty);
        assertNull(ref1.___getLockOwner());
        assertHasNoCommitLock(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertSame(constructed2, ref2.___unsafeLoad());
        assertTrue(constructed2.isCommitted);
        assertFalse(constructed2.isPermanent);
        assertEquals(DIRTY_FALSE, constructed1.isDirty);
        assertNull(ref2.___getLockOwner());
        assertHasNoCommitLock(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void whenUpdate() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value++;
        tx.commit();

        assertIsCommitted(tx);
        assertSame(tranlocal, ref.___unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
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
    public void whenOnlyRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        int readonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertEquals(0, committed.value);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertReadonlyCount(readonlyCount + 1, ref);
    }


    @Test
    public void whenMultipleItems() {
        int refCount = min(100, getTransactionMaxCapacity());
        BetaLongRef[] refs = new BetaLongRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = newLongRef(stm);
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length);
        BetaTransaction tx = newTransaction(config);
        for (BetaLongRef ref : refs) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
            tranlocal.value++;
        }
        tx.commit();

        tx.hardReset();
        for (BetaLongRef ref : refs) {
            assertEquals(1, tx.openForRead(ref, LOCKMODE_NONE).value);
        }
    }

    @Test
    public void whenWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        ref.atomicIncrementAndGet(1);
        LongRefTranlocal conflictingWrite = ref.___unsafeLoad();

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
    public void whenUnused() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        assertIsCommitted(tx);
    }


    // =========================== listeners =======================

    @Test
    public void listeners_whenPermanentLifecycleListenerAvailable_thenNotified() {
        assumeTrue(isSupportingListeners());

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listener);
        BetaTransaction tx = newTransaction(config);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void listeners_whenLifecycleListenerAvailable_thenNotified() {
        assumeTrue(isSupportingListeners());

        assumeTrue(isSupportingListeners());

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        BetaTransaction tx = newTransaction();
        tx.register(listener);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void listeners_whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst() {
        assumeTrue(isSupportingListeners());

        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(permanentListener);
        BetaTransaction tx = newTransaction(config);
        tx.register(normalListener);
        tx.commit();

        assertIsCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }


    @Test
    public void listeners_whenChangeListenerAvailable_thenListenerNotified() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);
        Latch latch = new CheapLatch();
        tx.registerChangeListenerAndAbort(latch);

        ref.atomicIncrementAndGet(1);

        assertTrue(latch.isOpen());
        assertHasNoListeners(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void listeners_whenChangeListenerAvailableAndNoWrite_thenListenerRemains() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction listeningTx = newTransaction();
        LongRefTranlocal read = listeningTx.openForRead(ref, LOCKMODE_NONE);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch);

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void listeners_whenMultipleChangeListeners_thenAllNotified() {
        BetaLongRef ref = newLongRef(stm);

        List<Latch> listeners = new LinkedList<Latch>();
        for (int k = 0; k < 10; k++) {
            BetaTransaction tx = newTransaction();
            tx.openForRead(ref, LOCKMODE_NONE);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }


    @Test
    public void listeners_whenNormalListenerAvailable() {
        assumeTrue(isSupportingListeners());

        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransaction tx = newTransaction();
        tx.register(listenerMock);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertEquals(2, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PrePrepare, listenerMock.events.get(0));
        assertEquals(TransactionLifecycleEvent.PostCommit, listenerMock.events.get(1));
    }

    @Test
    public void listeners_whenPermanentListenerAvailable() {
        assumeTrue(isSupportingListeners());

        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        BetaTransaction tx = newTransaction(config);
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


    // =================== isolation ====================

    @Test
    public void isolation_whenWriteSkewStillPossibleWithWriteSkewEnabled() {
        assumeTrue(getTransactionMaxCapacity() > 1);

        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = newTransaction();
        tx1.openForWrite(ref1, LOCKMODE_NONE).value++;
        tx1.openForRead(ref2, LOCKMODE_NONE);

        BetaTransaction tx2 = new FatArrayBetaTransaction(stm);
        tx2.openForRead(ref1, LOCKMODE_NONE);
        tx2.openForWrite(ref2, LOCKMODE_NONE).value++;

        tx1.commit();
        tx2.commit();
    }

    @Test
    public void isolation_whenWriteSkewNotPossibleWithoutWriteSkewDisabled() {
        assumeTrue(isSupportingWriteSkewDetection());
        assumeTrue(getTransactionMaxCapacity() > 1);

        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = newTransaction();
        tx1.openForWrite(ref1, LOCKMODE_NONE).value++;
        tx1.openForRead(ref2, LOCKMODE_NONE);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setIsolationLevel(IsolationLevel.Serializable);
        BetaTransaction tx2 = newTransaction(config);
        tx2.openForRead(ref1, LOCKMODE_NONE);
        tx2.openForWrite(ref2, LOCKMODE_NONE).value++;

        tx1.commit();

        try {
            tx2.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx2);
    }

    // ======================= pessimistic lock level =================

    @Test
    public void pessimisticLockLevel_whenPrivatizeWritesAndDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites)
                .setDirtyCheckEnabled(true);
        BetaTransaction tx = newTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void pessimisticLockLevel_whenPrivatizeReadsReadAndDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
                .setDirtyCheckEnabled(true);
        BetaTransaction tx = newTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }


    // ====================== commute =========================

    @Test
    public void commute_whenNoDirtyCheckAndCommute() {
        assumeTrue(isTransactionSupportingCommute());

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        BetaLongRef ref = newLongRef(stm);
        BetaTransaction tx = newTransaction(config);
        tx.commute(ref, Functions.newLongIncFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void commute_whenDirtyCheckAndCommute() {
        assumeTrue(isTransactionSupportingCommute());

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        BetaLongRef ref = newLongRef(stm);
        BetaTransaction tx = newTransaction(config);
        tx.commute(ref, Functions.newLongIncFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void commute_whenMultipleReferencesWithCommute() {
        assumeTrue(isTransactionSupportingCommute());
        assumeTrue(getTransactionMaxCapacity() > 1);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaTransaction tx = newTransaction();
        tx.commute(ref1, newLongIncFunction(1));
        tx.commute(ref2, newLongIncFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);
        assertReadonlyCount(0, ref1);
        assertEquals(1, ref1.___unsafeLoad().value);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
        assertReadonlyCount(0, ref2);
        assertEquals(1, ref2.___unsafeLoad().value);
    }

    @Test
    public void commute_whenInterleavingPossibleWithCommute() {
        assumeTrue(isTransactionSupportingCommute());
        assumeTrue(getTransactionMaxCapacity() > 1);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref1, LOCKMODE_NONE).value++;

        ref2.atomicIncrementAndGet(1);

        tx.commute(ref2, Functions.newLongIncFunction(1));
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
    public void commute_whenMultipleCommutes() {
        assumeTrue(isTransactionSupportingCommute());

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        BetaLongRef ref = newLongRef(stm);
        BetaTransaction tx = newTransaction(config);
        tx.commute(ref, Functions.newLongIncFunction(1));
        tx.commute(ref, Functions.newLongIncFunction(1));
        tx.commute(ref, Functions.newLongIncFunction(1));
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(3, ref.___unsafeLoad().value);
    }

    // ======================== misc ==========================


    @Test
    public void whenAbortOnly() {
        BetaTransaction tx = newTransaction();
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict conflict) {
        }

        assertIsAborted(tx);
    }


    @Test
    public void repeatedCommits() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal tranlocal1 = tx.openForWrite(ref1, LOCKMODE_NONE);
            tranlocal1.value++;
            LongRefTranlocal tranlocal2 = tx.openForWrite(ref2, LOCKMODE_NONE);
            tranlocal2.value++;
            tx.commit();
            tx.hardReset();
        }

        assertEquals(100L, ref1.___unsafeLoad().value);
        assertEquals(100L, ref2.___unsafeLoad().value);
    }

    // ========================== locking ========================

    @Test
    public void locking_whenCommuteAndLockedByOtherTransaction_thenWriteConflict() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);
        BetaTransaction tx = newTransaction();
        tx.commute(ref, newLongIncFunction(1));

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

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
    public void locking_whenHasPrivatizedRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
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
    public void locking_whenHasEnsuredRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
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

    @Test
    public void locking_whenContainsPrivatizedNormalRead() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
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
    public void locking_whenHasPrivatizedWrite() {
        BetaLongRef ref = newLongRef(stm);
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
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
    public void locking_whenHasEnsuredWrite() {
        BetaLongRef ref = newLongRef(stm);
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
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
    public void locking_whenNotDirtyAndLocked() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
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
    public void locking_whenWriteConflictCausedByLock_thenReadWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 0);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
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


    // =========================== state =========================

    @Test
    public void state_whenAlreadyPreparedButUnused() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void state_whenAlreadyPreparedAndContainsRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
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
    public void state_whenAlreadyPreparedAndContainsUpdate() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
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
    public void state_whenAlreadyCommitted_thenIgnore() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        tx.commit();
        assertIsCommitted(tx);
    }

    @Test
    public void state_whenAlreadyAborted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}
