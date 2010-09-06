package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;

public class LeanMonoBetaTransaction_commitTest {

     private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenAbortOnly() {
        LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict conflict) {
        }

        assertIsAborted(tx);
    }
}
