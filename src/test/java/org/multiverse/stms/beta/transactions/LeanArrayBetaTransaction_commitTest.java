package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertAborted;

public class LeanArrayBetaTransaction_commitTest {

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
            tx.commit();
            fail();
        } catch (WriteConflict conflict) {
        }

        assertAborted(tx);
    }
}