package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertAborted;

public class LeanArrayBetaTransaction_orelseTest {
    
     private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenStartEitherBranch_thenSpeculativeConfigurationError() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        LeanArrayBetaTransaction tx = new LeanArrayBetaTransaction(config);

        try {
            tx.startEitherBranch(pool);
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
            tx.startOrElseBranch(pool);
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
            tx.endEitherBranch(pool);
            fail();
        } catch (IllegalStateException expected) {

        }

        assertAborted(tx);
        assertFalse(config.getSpeculativeConfig().isOrelseRequired());
    }
}
