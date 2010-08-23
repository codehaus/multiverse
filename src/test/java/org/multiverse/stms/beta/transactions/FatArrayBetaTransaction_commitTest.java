package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_commitTest implements BetaStmConstants {

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
    public void whenOnlyConstructedObjects() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRef ref1 = new LongRef(tx);
        LongRefTranlocal constructed1 = tx.openForConstruction(ref1, pool);
        LongRef ref2 = new LongRef(tx);
        LongRefTranlocal constructed2 = tx.openForConstruction(ref2, pool);
        tx.commit();

        assertCommitted(tx);
        assertSame(constructed1, ref1.___unsafeLoad());
        assertTrue(constructed1.isCommitted);
        assertFalse(constructed1.isPermanent);
        assertEquals(DIRTY_FALSE, constructed1.isDirty);
        assertNull(ref1.___getLockOwner());
        assertUnlocked(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertSame(constructed2, ref2.___unsafeLoad());
        assertTrue(constructed2.isCommitted);
        assertFalse(constructed2.isPermanent);
        assertEquals(DIRTY_FALSE, constructed1.isDirty);
        assertNull(ref2.___getLockOwner());
        assertUnlocked(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.registerPermanent(pool, listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.register(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst() {
        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.register(normalListener);
        tx.registerPermanent(pool, permanentListener);
        tx.commit();

        assertCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenOnlyRead() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        int readonlyCount = ref.___getOrec().___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref.___getOrec());
        assertEquals(0, committed.value);
        assertUpdateBiased(ref.___getOrec());
        assertUnlocked(ref.___getOrec());
        assertReadonlyCount(readonlyCount + 1, ref.___getOrec());
    }

    @Test
    public void whenChangeListenerAvailable_thenListenerNotified() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        FatArrayBetaTransaction listeningTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch, pool);

        FatArrayBetaTransaction updatingTx = new FatArrayBetaTransaction(stm);
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
    public void whenChangeListenerAvailableAndNoWrite_thenListenerRemains() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        FatArrayBetaTransaction listeneningTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = listeneningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeneningTx.registerChangeListenerAndAbort(latch, pool);

        FatArrayBetaTransaction updatingTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = updatingTx.openForWrite(ref, false, pool);
        //write.isDirty = false;
        updatingTx.commit();

        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
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
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
            tx.openForRead(ref, false, pool);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit(pool);

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }

    @Test
    public void whenUpdate() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
        tranlocal.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(tranlocal, ref.___unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(new BetaTransactionConfiguration(stm));
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
    public void integrationTest_whenMultipleUpdatesAndDirtyCheckEnabled() {
        integrationTest_whenMultipleUpdatesAndDirtyCheck(true);
    }

    @Test
    public void integrationTest_whenMultipleUpdatesAndDirtyCheckDisabled() {
        integrationTest_whenMultipleUpdatesAndDirtyCheck(false);
    }

    public void integrationTest_whenMultipleUpdatesAndDirtyCheck(final boolean dirtyCheck) {
        LongRef[] refs = new LongRef[30];
        long created = 0;

        //create the references
        for (int k = 0; k < refs.length; k++) {
            refs[k] = createLongRef(stm);
        }

        //execute all transactions
        Random random = new Random();
        int transactionCount = 100000;
        for (int transaction = 0; transaction < transactionCount; transaction++) {
            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length)
                    .setDirtyCheckEnabled(dirtyCheck);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            for (int k = 0; k < refs.length; k++) {
                if (random.nextInt(3) == 1) {
                    tx.openForWrite(refs[k], false, pool).value++;
                    created++;
                } else {
                    tx.openForWrite(refs[k], false, pool);
                }
            }
            tx.commit(pool);
            tx.softReset();
        }

        long sum = 0;
        for (int k = 0; k < refs.length; k++) {
            sum += refs[k].___unsafeLoad().value;
        }

        assertEquals(created, sum);
    }

    @Test
    public void whenConstructed() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref, pool);
        tranlocal.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(tranlocal, ref.___unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
    }

    @Test
    public void whenMultipleItems() {
        int refCount = 100;
        LongRef[] refs = new LongRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = BetaStmUtils.createLongRef(stm);
        }

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        for (LongRef ref : refs) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
            tranlocal.value++;
        }
        tx.commit(pool);

        tx.hardReset(pool);
        for (LongRef ref : refs) {
            assertEquals(1, tx.openForRead(ref, false, pool).value);
        }
    }

    @Test
    public void repeatedCommits() {
        LongRef ref1 = BetaStmUtils.createLongRef(stm);
        LongRef ref2 = BetaStmUtils.createLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal tranlocal1 = tx.openForWrite(ref1, false, pool);
            tranlocal1.value++;
            LongRefTranlocal tranlocal2 = tx.openForWrite(ref2, false, pool);
            tranlocal2.value++;
            tx.commit(pool);
            tx.hardReset(pool);
        }

        assertEquals(100L, ref1.___unsafeLoad().value);
        assertEquals(100L, ref2.___unsafeLoad().value);
    }

    @Test
    public void whenWriteConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = new FatArrayBetaTransaction(stm);
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
    public void whenUnused() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit(pool);

        assertCommitted(tx);
    }

    @Test
    public void whenNormalListenerAvailable() {
        LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
    public void whenNoDirtyCheckAndCommute() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        LongRef ref = createLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.commute(ref, pool, IncLongFunction.INSTANCE);
        tx.commit(pool);

        assertCommitted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenMultipleReferencesWithCommute() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref1, pool, IncLongFunction.INSTANCE);
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
        assertEquals(1, ref2.___unsafeLoad().value);
    }

    @Test
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
    public void whenPessimisticLockLevelWriteAndDirtyCheck() {
        LongRef ref = createLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write)
                .setDirtyCheckEnabled(true);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit();

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenAbortOnly() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict conflict) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit(pool);

        tx.commit(pool);
        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }
}
