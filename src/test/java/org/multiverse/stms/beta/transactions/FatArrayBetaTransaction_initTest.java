package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoCommitLock;

public class FatArrayBetaTransaction_initTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUndefined() {

    }

    @Test
    public void whenTimeoutSetThenCopied() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setTimeoutNs(100);

        tx.init(config);

        assertEquals(100, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenNullConfig_thenNullPointerException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare();

        try {
            tx.init(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenNormalListenersAvailable_thenTheyAreRemoved() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);
    }

    @Test
    public void whenPrepared_thenAborted() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, true);
        tx.prepare();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);

        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenAborted() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false).value++;
        tx.abort();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);
    }

    @Test
    public void whenCommitted() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false).value++;
        tx.commit();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);
    }

    private void assertInitialized(FatArrayBetaTransaction tx) {
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
        assertEquals(1, tx.getAttempt());
    }
}
