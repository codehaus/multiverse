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
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_commitTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void whenUndefined() {

    }

    @Test
    public void whenOnlyConstructedObjects() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listener);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.register(listener);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst() {
        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(permanentListener);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.register(normalListener);
        tx.commit();

        assertIsCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenOnlyRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        int readonlyCount = ref.___getOrec().___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.commit();

        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref.___getOrec());
        assertEquals(0, committed.value);
        assertUpdateBiased(ref.___getOrec());
        assertHasNoCommitLock(ref.___getOrec());
        assertReadonlyCount(readonlyCount + 1, ref.___getOrec());
    }

    @Test
    public void whenChangeListenerAvailable_thenListenerNotified() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayBetaTransaction listeningTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, LOCKMODE_NONE);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch);

        FatArrayBetaTransaction updatingTx = new FatArrayBetaTransaction(stm);
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
    public void whenChangeListenerAvailableAndNoWrite_thenListenerRemains() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayBetaTransaction listeneningTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = listeneningTx.openForRead(ref, LOCKMODE_NONE);
        Latch latch = new CheapLatch();
        listeneningTx.registerChangeListenerAndAbort(latch);

        FatArrayBetaTransaction updatingTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = updatingTx.openForWrite(ref, LOCKMODE_NONE);
        //write.isDirty = false;
        updatingTx.commit();

        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
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
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
            tx.openForRead(ref, LOCKMODE_NONE);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }

    @Test
    public void whenUpdate() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value++;
        tx.commit();

        assertIsCommitted(tx);
        assertSame(tranlocal, ref.___unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(new BetaTransactionConfiguration(stm));
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commit();

        assertFalse(write.isCommitted);
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(1, ref.___getOrec());
    }


    @Test
    public void whenConstructed() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenMultipleItems() {
        int refCount = 100;
        BetaLongRef[] refs = new BetaLongRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = newLongRef(stm);
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
    public void repeatedCommits() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

    @Test
    public void whenWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        BetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, LOCKMODE_NONE);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict e) {
        }

        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertHasNoCommitLock(ref.___getOrec());
    }

    @Test
    public void whenUnused() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
    public void whenNoDirtyCheckAndCommute() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        BetaLongRef ref = newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);
        tx.commit();

        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenMultipleReferencesWithCommute() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref1, IncLongFunction.INSTANCE_INC_ONE);
        tx.commute(ref2, IncLongFunction.INSTANCE_INC_ONE);
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
    public void whenInterleavingPossibleWithCommute() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref1, LOCKMODE_NONE).value++;

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForWrite(ref2, LOCKMODE_NONE).value++;
        otherTx.commit();

        tx.commute(ref2, IncLongFunction.INSTANCE_INC_ONE);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);

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
    public void whenPessimisticLockLevelWriteAndDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites)
                .setDirtyCheckEnabled(true);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenWriteSkewStillPossibleWithWriteSkewEnabled() {
        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = new FatArrayBetaTransaction(stm);
        tx1.openForWrite(ref1, LOCKMODE_NONE).value++;
        tx1.openForRead(ref2, LOCKMODE_NONE);

        BetaTransaction tx2 = new FatArrayBetaTransaction(stm);
        tx2.openForRead(ref1, LOCKMODE_NONE);
        tx2.openForWrite(ref2, LOCKMODE_NONE).value++;

        tx1.commit();
        tx2.commit();
    }

    @Test
    public void whenWriteSkewNotPossibleWithoutWriteSkewDisabled() {
        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = new FatArrayBetaTransaction(stm);
        tx1.openForWrite(ref1, LOCKMODE_NONE).value++;
        tx1.openForRead(ref2, LOCKMODE_NONE);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setIsolationLevel(IsolationLevel.Serializable);
        BetaTransaction tx2 = new FatArrayBetaTransaction(config);
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


    @Test
    public void whenAbortOnly() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict conflict) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        tx.commit();
        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}
