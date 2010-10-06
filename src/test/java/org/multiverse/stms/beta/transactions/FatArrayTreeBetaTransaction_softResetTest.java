package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.LOCKMODE_COMMIT;
import static org.multiverse.TestUtils.LOCKMODE_NONE;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_softResetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenMaximumNumberOfRetriesReached() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setMaxRetries(3);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        assertTrue(tx.softReset());
        assertEquals(2, tx.getAttempt());
        assertIsActive(tx);

        assertTrue(tx.softReset());
        assertEquals(3, tx.getAttempt());
        assertIsActive(tx);

        assertFalse(tx.softReset());
        assertEquals(3, tx.getAttempt());
        assertIsAborted(tx);
    }

    @Test
    public void whenUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
    }

    @Test
    public void whenContainsUnlockedNonPermanentRead() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);        
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenContainsUnlockedPermanent() {
        BetaLongRef ref = newReadBiasedLongRef(stm);
        long version = ref.getVersion();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
        assertReadonlyCount(0, ref);
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenNormalUpdate() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
        assertFalse(write.hasDepartObligation);
        assertFalse(write.isCommitted);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whendLockedWrites() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
        assertFalse(write.hasDepartObligation);
        assertFalse(write.isCommitted);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenPreparedAndUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare();

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
    }

    @Test
    public void whenPreparedResourcesNeedRelease() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.prepare();

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenContainsConstructed() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.hasDepartObligation);
        assertHasCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenHasNormalListener_thenTheyAreRemoved() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        boolean result = tx.softReset();

        assertTrue(result);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);
        assertHasNoNormalListeners(tx);
    }

    @Test
    public void whenAborted() {
        BetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort();

        boolean result = tx.softReset();

        assertIsActive(tx);
        assertTrue(result);
    }

    @Test
    public void whenCommitted() {
        BetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit();

        boolean result = tx.softReset();

        assertTrue(result);
        assertIsActive(tx);
    }
}
