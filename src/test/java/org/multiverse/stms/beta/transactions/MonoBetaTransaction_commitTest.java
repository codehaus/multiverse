package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
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
public class MonoBetaTransaction_commitTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenUnused() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.commit(pool);

        assertCommitted(tx);
    }

    @Test
    public void whenPermanentLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.registerPermanent(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenLifecycleListenerAvailable_thenNotified() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.register(listener);
        tx.commit();

        assertCommitted(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenNormalAndPermanentLifecycleListenersAvailable_permanentGetsCalledFirst(){
        TransactionLifecycleListener normalListener = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener permanentListener = mock(TransactionLifecycleListener.class);
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.register(normalListener);
        tx.registerPermanent(permanentListener);
        tx.commit();

        assertCommitted(tx);

        InOrder inOrder = inOrder(permanentListener, normalListener);

        inOrder.verify(permanentListener).notify(tx, TransactionLifecycleEvent.PostCommit);
        inOrder.verify(normalListener).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenListenerAvailableForUpdate_thenListenerNotified() {
        LongRef ref = StmUtils.createLongRef(stm);

        MonoBetaTransaction listeningTx = new MonoBetaTransaction(stm);
        LongRefTranlocal read = listeningTx.openForRead(ref, false, pool);
        Latch latch = new CheapLatch();
        listeningTx.registerChangeListenerAndAbort(latch, pool);

        MonoBetaTransaction updatingTx = new MonoBetaTransaction(stm);
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
    public void whenContainsOnlyNormalRead() {
        LongRef ref = StmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        int oldReadonlyCount = ref.getReadonlyCount();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(0, ref.getOrec());
        assertEquals(0, committed.value);
        assertUpdateBiased(ref.getOrec());
        assertUnlocked(ref.getOrec());
        assertReadonlyCount(oldReadonlyCount + 1, ref.getOrec());
    }

    @Test
    public void whenContainsReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit(pool);

        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenContainsLockedNormalRead() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.commit(pool);

        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenContainsLockedReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.commit(pool);

        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenContainsLockedUpdate() {
        LongRef ref = createReadBiasedLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);
        write.value++;
        tx.commit(pool);

        assertSame(write, ref.unsafeLoad());
        assertTrue(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertCommitted(tx);
    }

    @Test
    public void whenNormalUpdate() {
        BetaTransactionalObject ref = StmUtils.createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = (LongRefTranlocal) tx.openForWrite(ref, false, pool);
        write.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(write, ref.unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(1, write.value);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }

    @Test
    public void whenNormalUpdateButNotChange() {
        LongRef ref = StmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(new BetaTransactionConfig(stm).setDirtyCheckEnabled(true));
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
    public void whenConstructed() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);       
        LongRefTranlocal write = tx.openForConstruction(ref, pool);
        write.value++;
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(write, ref.unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(1, write.value);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }

    @Test
    public void whenWriteConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        BetaTransaction otherTx = new MonoBetaTransaction(stm);
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
    public void whenWriteConflictCausedByLock() {
        LongRef ref = createLongRef(stm, 0);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        MonoBetaTransaction otherTx = new MonoBetaTransaction(stm);
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
        assertSame(otherTx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenAlmostReadBiased() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.unsafeLoad();

        //make almost read biased.
        for (int k = 0; k < ref.getReadBiasedThreshold() - 1; k++) {
            BetaTransaction tx = new MonoBetaTransaction(stm);
            tx.openForRead(ref, false, pool);
            tx.commit();
        }

        BetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.commit();

        assertSame(committed, ref.unsafeLoad());
        assertCommitted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertTrue(committed.isCommitted);
        assertTrue(committed.isPermanent);
        assertNull(committed.read);
        assertSame(ref, committed.owner);
        assertEquals(10, committed.value);
        assertUnlocked(ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }

    @Test
    public void whenUpdateReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm, 10);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.commit();

        assertCommitted(tx);
        assertSame(write, ref.unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(11, write.value);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }

    @Test
    public void repeatedCommits() {
        LongRef ref = StmUtils.createLongRef(stm);

        BetaTransaction tx = new MonoBetaTransaction(stm);
        for (int k = 0; k < 100; k++) {
            LongRefTranlocal tranlocal = (LongRefTranlocal) tx.openForWrite(ref, false, pool);
            tranlocal.value++;
            tx.commit(pool);
            tx.reset(pool);
        }

        assertEquals(100L, ref.unsafeLoad().value);
    }

    @Test
    public void whenNotDirtyAndNotLocked() {
           LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false,pool);
        tx.commit();

        assertCommitted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNotDirtyAndLocked() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true,pool);
        tx.commit();

        assertCommitted(tx);
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(1, ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNotDirtyAndNoDirtyCheck() {
        BetaTransactionalObject ref = StmUtils.createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(new BetaTransactionConfig(stm).setDirtyCheckEnabled(false));
        LongRefTranlocal write = (LongRefTranlocal) tx.openForWrite(ref, false, pool);
        tx.commit(pool);

        assertCommitted(tx);
        assertSame(write, ref.unsafeLoad());
        assertTrue(write.isCommitted);
        assertNull(write.read);
        assertSame(ref, write.owner);
        assertEquals(0, write.value);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(0, ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
    }


    @Test
    public void whenPreparedUnused() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.prepare(pool);

        tx.commit();

        assertCommitted(tx);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
    public void whenCommitted_thenIgnore() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.commit(pool);

        tx.commit(pool);
        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenIllegalStateException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
        LongRef[] refs = new LongRef[1];
        long created = 0;

        //create the references
        for (int k = 0; k < refs.length; k++) {
            refs[k] = createLongRef(stm);
        }

        //execute all transactions
        Random random = new Random();
        int transactionCount = 10000;
        for (int transaction = 0; transaction < transactionCount; transaction++) {
            MonoBetaTransaction tx = new MonoBetaTransaction(
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
            tx.reset();
        }

        long sum = 0;
        for (int k = 0; k < refs.length; k++) {
            sum += refs[k].unsafeLoad().value;
        }

        assertEquals(created, sum);
    }
}
