package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;

public class LeanArrayTreeBetaTransaction_commitTest {

     private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenAbortOnly() {
        LeanArrayTreeBetaTransaction tx = new LeanArrayTreeBetaTransaction(stm);
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict conflict) {
        }

        assertIsAborted(tx);
    }
}
