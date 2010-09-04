package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LeanMonoBetaTransaction_commuteTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenCalled_thenSpeculativeConfigurationError() {
        BetaLongRef ref = createLongRef(stm);
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(config);
        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
        verifyZeroInteractions(function);
        assertTrue(config.getSpeculativeConfig().isCommuteRequired());
    }
}
