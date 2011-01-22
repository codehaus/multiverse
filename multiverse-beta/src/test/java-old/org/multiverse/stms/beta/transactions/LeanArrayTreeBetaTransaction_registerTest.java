package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertIsAborted;

public class LeanArrayTreeBetaTransaction_registerTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenRegistered_thenSpeculativeFailure() {
        TransactionListener listener = mock(TransactionListener.class);

        LeanArrayTreeBetaTransaction tx = new LeanArrayTreeBetaTransaction(stm);

        try {
            tx.register(listener);
            fail();
        } catch (SpeculativeConfigurationError error) {
        }

        assertIsAborted(tx);
        assertTrue(tx.getConfiguration().getSpeculativeConfiguration().areListenersRequired);
    }
}
