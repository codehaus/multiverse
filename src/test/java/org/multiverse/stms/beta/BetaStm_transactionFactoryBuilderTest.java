package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BetaStm_transactionFactoryBuilderTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenReadtrackingDisabled() {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .build();

        assertFalse(txFactory.getTransactionConfiguration().isReadTrackingEnabled());
    }

    @Test
    public void whenSpeculativeConfigEnabled() {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .build();


        BetaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertFalse(configuration.getSpeculativeConfig().isFat());
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }

     @Test
    public void whenWriteSkewNotAllowed() {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .setWriteSkewAllowed(false)
                .build();


        BetaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertTrue(configuration.getSpeculativeConfig().isFat());
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }
}
