package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayBetaTransaction_commitTest {

    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    @Ignore
    public void whenUnstarted() {

    }

    @Test
    public void whenOnlyConstructedObjects() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        LongRef ref1 = new LongRef(tx);
        LongRefTranlocal constructed1 = tx.openForConstruction(ref1, pool);
        LongRef ref2 = new LongRef(tx);
        LongRefTranlocal constructed2 = tx.openForConstruction(ref2, pool);
        tx.commit();

        assertCommitted(tx);
        assertSame(constructed1, ref1.unsafeLoad());
        assertTrue(constructed1.isCommitted);
        assertFalse(constructed1.isPermanent);
        assertFalse(constructed1.isDirty);
        assertNull(ref1.getLockOwner());
        assertUnlocked(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertSame(constructed2, ref2.unsafeLoad());
        assertTrue(constructed2.isCommitted);
        assertFalse(constructed2.isPermanent);
        assertFalse(constructed2.isDirty);
        assertNull(ref2.getLockOwner());
        assertUnlocked(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.registerPermanent(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.register(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst() {
        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.register(normalListener);
        tx.registerPermanent(permanentListener);
        tx.commit();

        assertCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenOnlyRead() {
        LongRef ref = StmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        int readonlyCount = ref.getOrec().getReadonlyCount();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(0, ref.getOrec());
        assertEquals(0, committed.value);
        assertUpdateBiased(ref.getOrec());
        assertUnlocked(ref.getOrec());
        assertReadonlyCount(readonlyCount + 1, ref.getOrec());
    }

    @Test
    public void whenListenerAvailableForUpdate_thenListenerNotified() {
        LongRef ref = StmUtils.createLongRef(stm);

        ArrayBetaTransaction listeningTx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = listeningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch, pool);

        ArrayBetaTransaction updatingTx = new ArrayBetaTransaction(stm, 1);
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
    public void whenListenerAvailableAndNoWrite_thenListenerRemains() {
        LongRef ref = StmUtils.createLongRef(stm);

        ArrayBetaTransaction listeneningTx = new ArrayBetaTransaction(stm, 2);
        LongRefTranlocal read = listeneningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeneningTx.registerChangeListenerAndAbort(latch, pool);

        ArrayBetaTransaction updatingTx = new ArrayBetaTransaction(stm, 2);
        LongRefTranlocal write = updatingTx.openForWrite(ref, false, pool);
        write.isDirty = false;
        updatingTx.commit();

        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdate() {
        LongRef ref = StmUtils.createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
        tranlocal.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(tranlocal, ref.unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        LongRef ref = StmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(new BetaTransactionConfig(stm), 10);
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
            ArrayBetaTransaction tx = new ArrayBetaTransaction(new BetaTransactionConfig(stm).setDirtyCheckEnabled(dirtyCheck), refs.length
            );

            for (int k = 0; k < refs.length; k++) {
                if (random.nextInt(3) == 1) {
                    tx.openForWrite(refs[k], false, pool).value++;
                    created++;
                } else {
                    tx.openForWrite(refs[k], false, pool);
                }
            }
            tx.commit(pool);
            tx.reset();
        }

        long sum = 0;
        for (int k = 0; k < refs.length; k++) {
            sum += refs[k].unsafeLoad().value;
        }

        assertEquals(created, sum);
    }

    @Test
    public void whenConstructed() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref, pool);
        tranlocal.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(tranlocal, ref.unsafeLoad());
        assertTrue(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertSame(ref, tranlocal.owner);
        assertEquals(1, tranlocal.value);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }

    @Test
    public void whenMultipleItems() {
        int refCount = 100;
        LongRef[] refs = new LongRef[refCount];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = StmUtils.createLongRef(stm);
        }

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, refs.length);
        for (LongRef ref : refs) {
            LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
            tranlocal.value++;
        }
        tx.commit(pool);

        tx.reset(pool);
        for (LongRef ref : refs) {
            assertEquals(1, tx.openForRead(ref, false, pool).value);
        }
    }

    @Test
    public void repeatedCommits() {
        LongRef ref1 = StmUtils.createLongRef(stm);
        LongRef ref2 = StmUtils.createLongRef(stm);

        BetaTransaction tx = new ArrayBetaTransaction(stm, 2);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal tranlocal1 = tx.openForWrite(ref1, false, pool);
            tranlocal1.value++;
            LongRefTranlocal tranlocal2 = tx.openForWrite(ref2, false, pool);
            tranlocal2.value++;
            tx.commit(pool);
            tx.reset(pool);
        }

        assertEquals(100L, ref1.unsafeLoad().value);
        assertEquals(100L, ref2.unsafeLoad().value);
    }

    @Test
    public void whenWriteConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = new ArrayBetaTransaction(stm, 10);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (WriteConflict e) {
        }

        assertSame(conflictingWrite, ref.unsafeLoad());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertUnlocked(ref.getOrec());
    }

    @Test
    public void whenUnused() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        tx.commit(pool);

        assertCommitted(tx);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.commit(pool);

        tx.commit(pool);
        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.abort(pool);

        try {
            tx.commit(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }
}
