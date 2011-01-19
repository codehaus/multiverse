package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;

public class LeanArrayTreeBetaTransaction_orelseTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenStartEitherBranch_thenSpeculativeConfigurationError() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).init();
        LeanArrayTreeBetaTransaction tx = new LeanArrayTreeBetaTransaction(config);

        try {
            tx.startEitherBranch();
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertTrue(config.getSpeculativeConfiguration().isOrelseRequired);
    }

    @Test
    public void whenStartOrElseBranchBranchCalled_thenIllegalStateException() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).init();
        LeanArrayTreeBetaTransaction tx = new LeanArrayTreeBetaTransaction(config);

        try {
            tx.startOrElseBranch();
            fail();
        } catch (IllegalStateException expected) {

        }

        assertIsAborted(tx);
        assertFalse(config.getSpeculativeConfiguration().isOrelseRequired);
    }

    @Test
    public void whenEndEitherBranchCalled_thenIllegalStateException() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).init();
        LeanArrayTreeBetaTransaction tx = new LeanArrayTreeBetaTransaction(config);

        try {
            tx.endEitherBranch();
            fail();
        } catch (IllegalStateException expected) {

        }

        assertIsAborted(tx);
        assertFalse(config.getSpeculativeConfiguration().isOrelseRequired);
    }
}
