package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

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
public class FatArrayBetaTransaction_softResetTest {
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
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setMaxRetries(3);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
    }

    @Test
    public void whenContainsUnlockedNonPermanentRead() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenContainsUnlockedPermanent() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        boolean result = tx.softReset();

        assertTrue(result);
        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenNormalUpdate() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        boolean result = tx.softReset();

        assertTrue(result);
        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whendLockedWrites() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        boolean result = tx.softReset();

        assertTrue(result);
        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenPreparedAndUnused() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare(pool);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
    }

    @Test
    public void whenPreparedResourcesNeedRelease() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        tx.prepare();

        boolean result = tx.softReset();

        assertTrue(result);
        assertActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenContainsConstructed() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);

        boolean result = tx.softReset();

        assertTrue(result);
        assertActive(tx);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertHasNoUpdates(tx);
    }
    
    @Test
    public void whenHasNormalListener_thenTheyAreRemoved() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);
        assertHasNoNormalListeners(tx);        
    }

    @Test
    public void whenAborted() {
        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort(pool);

        boolean result = tx.softReset(pool);

        assertActive(tx);
        assertTrue(result);
    }

    @Test
    public void whenCommitted() {
        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit(pool);

        boolean result = tx.softReset(pool);

        assertTrue(result);
        assertActive(tx);
    }
}
