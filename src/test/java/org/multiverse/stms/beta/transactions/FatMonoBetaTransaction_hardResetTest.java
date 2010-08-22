package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.LongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class FatMonoBetaTransaction_hardResetTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void testThatAttemptsAreReset() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.softReset(pool);
        tx.softReset(pool);
        tx.softReset(pool);

        tx.hardReset(pool);

        assertWasHardReset(tx);
    }

    @Test
    public void whenHasConstructed() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        tx.openForConstruction(ref, pool);

        tx.hardReset(pool);
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasNormalRead() {
        LongRef ref = createLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);

        tx.hardReset(pool);
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasPermanentRead() {
        LongRef ref = createReadBiasedLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.hardReset(pool);
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasWrite() {
        LongRef ref = createLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);
        tx.hardReset(pool);
        assertWasHardReset(tx);
    }

    private static void assertWasHardReset(FatMonoBetaTransaction tx) {
        assertActive(tx);
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        assertEquals(1, tx.getAttempt());
        assertFalse((Boolean) getField(tx, "hasReads"));
        assertFalse((Boolean) getField(tx, "hasUntrackedReads"));
        assertHasNoUpdates(tx);
    }

    @Test
    @Ignore
    public void whenHasCommute() {
    }

    @Test
    public void whenTimeout() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setTimeoutNs(100);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.setRemainingTimeoutNs(50);
        tx.prepare(pool);

        tx.hardReset(pool);

        assertWasHardReset(tx);
        assertEquals(100, tx.getRemainingTimeoutNs());
    }


    @Test
    public void whenPrepared() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare(pool);

        tx.hardReset(pool);

        assertWasHardReset(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenHasPermanentListener() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(pool,listener);

        tx.hardReset(pool);

        assertEquals(1, tx.getAttempt());
        assertActive(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);

        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenHasNormalListener() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        tx.hardReset(pool);

        assertActive(tx);
        assertEquals(1, tx.getAttempt());
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenAborted() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        tx.hardReset(pool);
        assertEquals(1, tx.getAttempt());
        assertActive(tx);
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenCommitted() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        tx.hardReset(pool);
        assertEquals(1, tx.getAttempt());
        assertActive(tx);
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }
}
