package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_softResetTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenNew() {

    }

    @Test
    public void whenMaximumNumberOfRetriesReached() {
        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setMaxRetries(3);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        assertTrue(tx.softReset());
        assertEquals(2, tx.getAttempt());
        assertActive(tx);

        assertTrue(tx.softReset());
        assertEquals(3, tx.getAttempt());
        assertActive(tx);

        assertFalse(tx.softReset());
        assertEquals(3, tx.getAttempt());
        assertAborted(tx);
    }

    @Test
    public void whenUnused() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
    }

    @Test
    public void whenContainsUnlockedNonPermanentRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        boolean result = tx.softReset(pool);

        assertTrue(result);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        boolean result = tx.softReset();

        assertTrue(result);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        boolean result = tx.softReset();

        assertTrue(result);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        boolean result = tx.softReset();

        assertTrue(result);
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare(pool);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
    }

    @Test
    public void whenPreparedResourcesNeedRelease() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.prepare();

        boolean result = tx.softReset();

        assertTrue(result);
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);

        boolean result = tx.softReset();

        assertTrue(result);
        assertActive(tx);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    public void whenHasPermanentListener_thenTheyRemain() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);

        assertHasNoNormalListeners(tx);
        assertHasPermanentListeners(tx, listener);
    }

    @Test
    public void whenHasNormalListener_thenTheyAreRemoved() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);      
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
    }

    @Test
    public void whenAborted() {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        boolean result = tx.softReset(pool);

        assertActive(tx);
        assertTrue(result);
    }

    @Test
    public void whenCommitted() {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
    }
}
