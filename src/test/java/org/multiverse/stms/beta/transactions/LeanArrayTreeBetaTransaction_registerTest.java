package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertAborted;

public class LeanArrayTreeBetaTransaction_registerTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenRegistered_thenSpeculativeFailure() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        LeanArrayTreeBetaTransaction tx = new LeanArrayTreeBetaTransaction(stm);

        try {
            tx.register(pool, listener);
            fail();
        } catch (SpeculativeConfigurationError error) {
        }

        assertAborted(tx);
        assertTrue(tx.getConfiguration().getSpeculativeConfig().isListenerRequired());
    }
}
