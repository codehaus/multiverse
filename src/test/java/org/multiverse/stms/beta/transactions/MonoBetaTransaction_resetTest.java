package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertActive;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.StmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MonoBetaTransaction_resetTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    @Ignore
    public void whenNew(){

    }

    @Test
    public void whenUnused() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);

        tx.reset(pool);

        assertActive(tx);
    }

    @Test
    public void whenContainsUnlockedNonPermanentRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.reset(pool);

        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenContainsUnlockedPermanent() {
        LongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        tx.reset();

        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenNormalUpdate() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        tx.reset();

        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whendLockedWrites() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        tx.reset();

        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenPreparedAndUnused() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.prepare(pool);

        tx.reset(pool);
        assertActive(tx);
    }

    @Test
    public void whenPreparedResourcesNeedRelease() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.prepare();

        tx.reset();

        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenContainsConstructed() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref,pool);
        tx.reset();

        assertActive(tx);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref);
        assertSame(tx,ref.getLockOwner());
        assertSurplus(1,ref);
    }

    @Test
    public void whenHasPermanentListener_thenTheyRemain() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener);

        tx.reset(pool);

        verifyZeroInteractions(listener);
        assertNull(getField(tx, "normalListeners"));
        assertSame(listener, getField(tx, "permanentListeners"));
    }

    @Test
    public void whenHasNormalListener_thenTheyAreRemoved() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        tx.reset(pool);

        verifyZeroInteractions(listener);
        assertNull(getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
    }

    @Test
    public void whenAborted() {
        BetaTransaction tx = new MonoBetaTransaction(stm);
        tx.abort(pool);

        tx.reset(pool);
        assertActive(tx);
    }

    @Test
    public void whenCommitted() {
        BetaTransaction tx = new MonoBetaTransaction(stm);
        tx.commit(pool);

        tx.reset(pool);
        assertActive(tx);
    }
}
