package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

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
        tx.registerPermanent(listener);
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
        tx.registerPermanent(permanentListener);
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
        Tranlocal committed = ref.unsafeLoad();

        BetaTransaction tx = stm.start();
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertSame(committed, ref.unsafeLoad());
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
        assertNull(ref.getLockOwner());
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

        assertSame(tranlocal, ref.unsafeLoad());
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommitted());
        assertEquals(1, tranlocal.value);
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(new BetaTransactionConfig(stm));
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.commit(pool);

        assertFalse(write.isCommitted);
        assertCommitted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(1, ref.getOrec());
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

        BetaTransaction otherTx = stm.start();
        LongRefTranlocal writeConflict = otherTx.openForWrite(ref, false, pool);
        writeConflict.value++;
        otherTx.commit(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (WriteConflict e) {
        }

        assertNull(ref.getLockOwner());
        assertSame(writeConflict, ref.unsafeLoad());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertUnlocked(ref.getOrec());
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
            LongRefTranlocal tranlocal = ref.unsafeLoad();
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

        assertEquals(100L, ref1.unsafeLoad().value);
        assertEquals(100L, ref2.unsafeLoad().value);
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
        LongRefTranlocal committed = ref.unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);
        tx.prepare();

        tx.commit();
        assertCommitted(tx);

        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
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
        assertNull(ref.getLockOwner());
        assertSame(write, ref.unsafeLoad());
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
                    new BetaTransactionConfig(stm).setDirtyCheckEnabled(dirtyCheck));

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
            sum += refs[k].unsafeLoad().value;
        }

        assertEquals(created, sum);
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
