package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;

public class LeanArrayBetaTransaction_prepareTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenAbortOnly() {
        LeanArrayBetaTransaction tx = new LeanArrayBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.prepare();
            fail();
        } catch (WriteConflict conflict) {
        }

        assertIsAborted(tx);
    }
}
