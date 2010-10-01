package org.multiverse.stms.beta.transactions;

import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;

public class FatMonoBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
    }

    @Test
    public void whenOverflowing() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        config.init();
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        BetaLongRef ref1 = new BetaLongRef(tx);
        BetaLongRef ref2 = new BetaLongRef(tx);

        tx.openForConstruction(ref1);
        try {
            tx.openForConstruction(ref2);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
    }
}
