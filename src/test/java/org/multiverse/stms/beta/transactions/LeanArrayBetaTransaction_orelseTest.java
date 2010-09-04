package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertAborted;

public class LeanArrayBetaTransaction_orelseTest {
    
     private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenStartEitherBranch_thenSpeculativeConfigurationError() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        LeanArrayBetaTransaction tx = new LeanArrayBetaTransaction(config);

        try {
            tx.startEitherBranch();
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertAborted(tx);
        assertTrue(config.getSpeculativeConfig().isOrelseRequired());
    }

    @Test
    public void whenStartOrElseBranchBranchCalled_thenIllegalStateException(){
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        LeanArrayBetaTransaction tx = new LeanArrayBetaTransaction(config);

        try {
            tx.startOrElseBranch();
            fail();
        } catch (IllegalStateException expected) {

        }

        assertAborted(tx);
        assertFalse(config.getSpeculativeConfig().isOrelseRequired());
    }

     @Test
    public void whenEndEitherBranchCalled_thenIllegalStateException(){
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        LeanArrayBetaTransaction tx = new LeanArrayBetaTransaction(config);

        try {
            tx.endEitherBranch();
            fail();
        } catch (IllegalStateException expected) {

        }

        assertAborted(tx);
        assertFalse(config.getSpeculativeConfig().isOrelseRequired());
    }
}
