package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

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
public class FatArrayTreeBetaTransaction_commitTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    //since the readset is not checked for conflicting writes at the commit, a writeskew still can happen.

    @Test
    public void whenMultipleChangeListeners_thenAllNotified() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        List<Latch> listeners = new LinkedList<Latch>();
        for (int k = 0; k < 10; k++) {
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
            tx.openForRead(ref, false, pool);
            Latch listener = new CheapLatch();
            listeners.add(listener);
            tx.registerChangeListenerAndAbort(listener);
        }

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit(pool);

        for (Latch listener : listeners) {
            assertTrue(listener.isOpen());
        }
    }

    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.registerPermanent(pool,listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.register(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst() {
        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.register(normalListener);
        tx.registerPermanent(pool,permanentListener);
        tx.commit();

        assertCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }


    @Test
    public void whenUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        assertCommitted(tx);
    }

    @Test
    public void whenOnlyReads() {
        LongRef ref = createLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenListenerAvailableForUpdate_thenListenerNotified() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        FatArrayTreeBetaTransaction listeningTx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch, pool);

        FatArrayTreeBetaTransaction updatingTx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = updatingTx.openForWrite(ref, false, pool);
        write.value++;
        write.isDirty = true;
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
        LongRef ref = createLongRef(stm, 0);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
        tranlocal.value++;
        tx.commit(pool);

        assertSame(tranlocal, ref.___unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommitted());
        assertEquals(1, tranlocal.value);
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(new BetaTransactionConfiguration(stm));
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
    public void writeSkewStillPossible() {
        LongRef ref1 = createLongRef(stm, 0);
        LongRef ref2 = createLongRef(stm, 0);

        BetaTransaction tx1 = new FatArrayTreeBetaTransaction(stm);
        tx1.openForWrite(ref1, false, pool);
        tx1.openForRead(ref2, false, pool);

        BetaTransaction tx2 = new FatArrayTreeBetaTransaction(stm);
        tx2.openForRead(ref1, false, pool);
        tx2.openForWrite(ref2, false, pool);

        tx1.commit(pool);
        tx2.commit(pool);
    }

    @Test
    public void whenWriteConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal writeConflict = otherTx.openForWrite(ref, false, pool);
        writeConflict.value++;
        otherTx.commit(pool);

        try {
            tx.commit(pool);
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
        LongRef[] refs = new LongRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = BetaStmUtils.createLongRef(stm);
        }

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        for (LongRef ref : refs) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
            tranlocal.value++;
        }
        tx.commit(pool);

        for (LongRef ref : refs) {
            LongRefTranlocal tranlocal = ref.___unsafeLoad();
            assertEquals(1, tranlocal.value);
        }
    }

    @Test
    public void repeatedCommits() {
        LongRef ref1 = BetaStmUtils.createLongRef(stm);
        LongRef ref2 = BetaStmUtils.createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal write1 = tx.openForWrite(ref1, false, pool);
            write1.value++;
            LongRefTranlocal write2 = tx.openForWrite(ref2, false, pool);
            write2.value++;
            tx.commit(pool);
            tx.hardReset(pool);
        }

        assertEquals(100L, ref1.___unsafeLoad().value);
        assertEquals(100L, ref2.___unsafeLoad().value);
    }

    @Test
    public void whenPreparedUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        tx.commit();

        assertCommitted(tx);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        int transactionCount = 10000;
        for (int transaction = 0; transaction < transactionCount; transaction++) {
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(
                    new BetaTransactionConfiguration(stm).setDirtyCheckEnabled(dirtyCheck));

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
    public void whenNoDirtyCheckAndCommute() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        LongRef ref = createLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    public void whenMultipleCommutes() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        LongRef ref = createLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
    public void whenInterleavingPossibleWithCommute() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref1, false, pool).value++;

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
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
    public void whenCommuteAndLockedByOtherTransaction_thenWriteConflict() {
        LongRef ref = createLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        tx.commit(pool);
        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenIllegalStateException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertAborted(tx);
    }
}
