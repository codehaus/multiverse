package org.multiverse.stms.beta.transactions;

import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_openForReadTest
        extends BetaTransaction_openForReadTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(config);
    }

    @Override
    public boolean doesTransactionSupportCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }

    @Override
    protected void assumeIsAbleToNotTrackReads() {
        assumeTrue(true);
    }

    @Override
    protected boolean hasLocalConflictCounter() {
        return true;
    }

    @Test
    public void whenOverflowing() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);
        BetaLongRef ref4 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
        config.init();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);
        tx.openForRead(ref2, LOCKMODE_NONE);
        tx.openForRead(ref3, LOCKMODE_NONE);
        try {
            tx.openForRead(ref4, LOCKMODE_NONE);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertEquals(4, config.getSpeculativeConfiguration().minimalLength);
    }
}
