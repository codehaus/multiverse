package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.assertHasNoCommitLock;

public class FatArrayTreeBetaTransaction_initTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenTimeoutSetThenCopied() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setTimeoutNs(100);

        tx.init(config);

        assertEquals(100, tx.getRemainingTimeoutNs());
    }

    @Test
    public void whenNullConfig_thenNullPointerException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);
    }

    @Test
    public void whenPrepared_thenAborted() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_EXCLUSIVE);
        tx.prepare();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);

        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenAborted() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.abort();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);
    }

    @Test
    public void whenCommitted() {
        BetaLongRef ref = newLongRef(stm);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE).value++;
        tx.commit();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        tx.init(config);

        assertSame(config, tx.getConfiguration());
        assertInitialized(tx);
    }

    private void assertInitialized(FatArrayTreeBetaTransaction tx) {
        assertIsActive(tx);
        assertHasNoNormalListeners(tx);
        assertAllNull((BetaTranlocal[]) getField(tx, "array"));
        assertEquals(1, tx.getAttempt());
    }
}
