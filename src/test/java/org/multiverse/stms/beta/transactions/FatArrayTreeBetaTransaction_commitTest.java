package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

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
public class FatArrayTreeBetaTransaction_commitTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    //since the readset is not checked for conflicting writes at the commit, a writeskew still can happen.

    @Test
    public void whenMultipleChangeListeners_thenAllNotified() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        List<Latch> listeners = new LinkedList<Latch>();
        for (int k = 0; k < 10; k++) {
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
            tx.openForRead(ref, false);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, false).value++;
        tx.commit();

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }

    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listener);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.register(normalListener);
        tx.commit();

        assertIsCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }


    @Test
    public void whenUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void whenOnlyReads() {
        BetaLongRef ref = newLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, false);
        tx.commit();

        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenListenerAvailableForUpdate_thenListenerNotified() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction listeningTx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, false);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch);

        FatArrayTreeBetaTransaction updatingTx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = updatingTx.openForWrite(ref, false);
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
    public void whenUpdates() {
        BetaLongRef ref = newLongRef(stm, 0);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, false);
        tranlocal.value++;
        tx.commit();

        assertSame(tranlocal, ref.___unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommitted());
        assertEquals(1, tranlocal.value);
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(new BetaTransactionConfiguration(stm));
        LongRefTranlocal write = tx.openForWrite(ref, false);
        tx.commit();

        assertFalse(write.isCommitted);
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(1, ref.___getOrec());
    }

    @Test
    public void whenWriteSkewStillPossibleWithWriteSkewEnabled() {
        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = new FatArrayTreeBetaTransaction(stm);
        tx1.openForWrite(ref1, false).value++;
        tx1.openForRead(ref2, false);

        BetaTransaction tx2 = new FatArrayTreeBetaTransaction(stm);
        tx2.openForRead(ref1, false);
        tx2.openForWrite(ref2, false).value++;

        tx1.commit();
        tx2.commit();
    }

    @Test
    public void whenWriteSkewNotPossibleWithoutWriteSkewDisabled() {
        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = new FatArrayTreeBetaTransaction(stm);
        tx1.openForWrite(ref1, false).value++;
        tx1.openForRead(ref2, false);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setWriteSkewAllowed(false);
        BetaTransaction tx2 = new FatArrayTreeBetaTransaction(config);
        tx2.openForRead(ref1, false);
        tx2.openForWrite(ref2, false).value++;

        tx1.commit();

        try {
            tx2.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertIsAborted(tx2);
    }

    @Test
    public void whenWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal writeConflict = otherTx.openForWrite(ref, false);
        writeConflict.value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict e) {
        }

        assertNull(ref.___getLockOwner());
        assertSame(writeConflict, ref.___unsafeLoad());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertUnlocked(ref.___getOrec());
    }

    @Test
    public void whenMultipleItems() {
        int refCount = 100;
        BetaLongRef[] refs = new BetaLongRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = BetaStmUtils.newLongRef(stm);
        }

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        for (BetaLongRef ref : refs) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, false);
            tranlocal.value++;
        }
        tx.commit();

        for (BetaLongRef ref : refs) {
            LongRefTranlocal tranlocal = ref.___unsafeLoad();
            assertEquals(1, tranlocal.value);
        }
    }

    @Test
    public void repeatedCommits() {
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal write1 = tx.openForWrite(ref1, false);
            write1.value++;
            LongRefTranlocal write2 = tx.openForWrite(ref2, false);
            write2.value++;
            tx.commit();
            tx.hardReset();
        }

        assertEquals(100L, ref1.___unsafeLoad().value);
        assertEquals(100L, ref2.___unsafeLoad().value);
    }

    @Test
    public void whenPreparedUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare();

        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);
        tx.prepare();

        tx.commit();
        assertIsCommitted(tx);

        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPreparedAndContainsUpdate() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;
        tx.prepare();

        tx.commit();

        assertIsCommitted(tx);

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

        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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

        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    public void whenMultipleCommutes() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
    public void whenInterleavingPossibleWithCommute() {
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref1, false).value++;

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
        otherTx.openForWrite(ref2, false).value++;
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
    public void whenCommuteAndLockedByOtherTransaction_thenWriteConflict() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref, IncLongFunction.INSTANCE_INC_ONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true);

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(0, ref.___unsafeLoad().value);
    }

    @Test
    public void whenPessimisticLockLevelWriteAndDirtyCheck() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write)
                .setDirtyCheckEnabled(true);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.openForWrite(ref, false).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenPessimisticLockLevelReadAndDirtyCheck() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read)
                .setDirtyCheckEnabled(true);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.openForWrite(ref, false).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenAbortOnly() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict conflict) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit();

        tx.commit();
        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenIllegalStateException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort();

        try {
            tx.commit();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsAborted(tx);
    }
}
