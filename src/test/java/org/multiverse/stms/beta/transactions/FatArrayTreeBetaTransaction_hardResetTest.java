package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class FatArrayTreeBetaTransaction_hardResetTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void testThatAttemptsAreReset() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.softReset();
        tx.softReset();
        tx.softReset();

        tx.hardReset();

        assertIsActive(tx);
        assertEquals(1, tx.getAttempt());
    }

    @Test
    public void whenHasConstructed() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);

        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);

        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasPermanentRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasWrite() {
        BetaLongRef ref = newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.hardReset();
        assertWasHardReset(tx);
    }

    private static void assertWasHardReset(FatArrayTreeBetaTransaction tx) {
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(1, tx.getAttempt());
        assertFalse((Boolean) getField(tx, "hasReads"));
        assertFalse((Boolean) getField(tx, "hasUntrackedReads"));
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenTimeoutConfigured() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setTimeoutNs(100);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.setRemainingTimeoutNs(50);
        tx.prepare();

        tx.hardReset();

        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(1, tx.getAttempt());
        assertEquals(100, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenPrepared() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare();

        tx.hardReset();

        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenHasPermanentListener() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listener);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

        tx.hardReset();

        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenHasNormalListener() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        tx.hardReset();

        assertIsActive(tx);
        assertEquals(1, tx.getAttempt());
        verify(listener).notify(tx, TransactionLifecycleEvent.PostAbort);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    @Ignore
    public void whenUndefined() {
    }

    @Test
    public void whenAborted() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort();

        tx.hardReset();
        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenCommitted() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit();

        tx.hardReset();
        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }
}
