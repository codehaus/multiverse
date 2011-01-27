package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionEvent;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class FatMonoBetaTransaction_hardResetTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void testThatAttemptsAreReset() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.softReset();
        tx.softReset();
        tx.softReset();

        tx.hardReset();

        assertWasHardReset(tx);
    }

    @Test
    public void whenHasConstructed() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);

        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);

        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasPermanentRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.hardReset();
        assertWasHardReset(tx);
    }

    @Test
    public void whenHasWrite() {
        BetaLongRef ref = newLongRef(stm);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.hardReset();
        assertWasHardReset(tx);
    }

    private static void assertWasHardReset(FatMonoBetaTransaction tx) {
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
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
        tx.prepare();

        tx.hardReset();

        assertWasHardReset(tx);
        assertEquals(100, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenPrepared() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare();

        tx.hardReset();

        assertWasHardReset(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenHasNormalListener() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        TransactionListener listener = mock(TransactionListener.class);
        tx.register(listener);

        tx.hardReset();

        assertIsActive(tx);
        assertEquals(1, tx.getAttempt());
        verify(listener).notify(tx, TransactionEvent.PostAbort);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenAborted() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort();

        tx.hardReset();
        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenCommitted() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        tx.hardReset();
        assertEquals(1, tx.getAttempt());
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(Long.MAX_VALUE, tx.getRemainingTimeoutNs());
    }
}
