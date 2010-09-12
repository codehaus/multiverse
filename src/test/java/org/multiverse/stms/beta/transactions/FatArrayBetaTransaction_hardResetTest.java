package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;

public class FatArrayBetaTransaction_hardResetTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenHasConstructed() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);

        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasNormalRead() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false);

        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasPermanentRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false);
        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasWrite() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false);
        tx.hardReset();
        assertWasHardReset(tx);
    }

    private static void assertWasHardReset(FatArrayBetaTransaction tx) {
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(1, tx.getAttempt());
        assertFalse((Boolean) getField(tx, "hasReads"));
        assertFalse((Boolean) getField(tx, "hasUntrackedReads"));
        assertHasNoUpdates(tx);
    }

    @Test
    public void testThatAttemptsAreReset() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.softReset();
        tx.softReset();
        tx.softReset();

        tx.hardReset();

        assertIsActive(tx);
        assertEquals(1, tx.getAttempt());
    }

    @Test
    public void whenTimeout() {
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
    public void whenHasNormalListener() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
    public void whenAborted() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        tx.hardReset();
        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenCommitted() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        tx.hardReset();
        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }
}
