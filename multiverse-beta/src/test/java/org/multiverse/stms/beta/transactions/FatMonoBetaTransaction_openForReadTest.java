package org.multiverse.stms.beta.transactions;

import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_openForReadTest
        extends BetaTransaction_openForReadTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
    }


    @Override
    public boolean doesTransactionSupportCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
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

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).init();
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);
        try {
            tx.openForRead(ref2, LOCKMODE_NONE);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
    }
}
