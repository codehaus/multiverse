package org.multiverse.stms.beta.transactions;

import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;

public class FatArrayBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    protected int getMaxTransactionCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(config);
    }

    @Override
    protected boolean hasLocalConflictCounter() {
        return true;
    }

    @Test
    public void whenOverflowing() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
        config.init();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        BetaLongRef ref1 = new BetaLongRef(tx);
        BetaLongRef ref2 = new BetaLongRef(tx);
        BetaLongRef ref3 = new BetaLongRef(tx);
        BetaLongRef ref4 = new BetaLongRef(tx);

        tx.openForConstruction(ref1);
        tx.openForConstruction(ref2);
        tx.openForConstruction(ref3);
        try {
            tx.openForConstruction(ref4);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertEquals(4, config.getSpeculativeConfiguration().minimalLength);
    }
}
