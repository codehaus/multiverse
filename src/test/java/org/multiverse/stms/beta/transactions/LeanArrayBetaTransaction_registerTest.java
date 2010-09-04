package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertAborted;

public class LeanArrayBetaTransaction_registerTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenRegistered_thenSpeculativeFailure() {
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        LeanArrayBetaTransaction tx = new LeanArrayBetaTransaction(stm);

        try {
            tx.register(listener);
            fail();
        } catch (SpeculativeConfigurationError error) {
        }

        assertAborted(tx);
        assertTrue(tx.getConfiguration().getSpeculativeConfig().isListenerRequired());
    }
}
