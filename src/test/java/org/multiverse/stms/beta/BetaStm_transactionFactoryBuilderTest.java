package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class BetaStm_transactionFactoryBuilderTest {
    private BetaStm stm;

    @Before
    public void setUp(){
           stm = new BetaStm();
    }

    @Test
    public void whenReadtrackingDisabled(){
         BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                 .setReadTrackingEnabled(false)
                 .setBlockingAllowed(false)
                 .build();

        assertFalse(txFactory.getTransactionConfiguration().isReadTrackingEnabled());
    }
}
