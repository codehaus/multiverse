package org.multiverse.stms.beta.transactions;

import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

public class LeanMonoBetaTransaction_openForReadTest
        extends BetaTransaction_openForReadTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanMonoBetaTransaction(config);
    }

    @Override
    public boolean doesTransactionSupportCommute() {
        return false;
    }

    @Override
    protected void assumeIsAbleToNotTrackReads() {
        assumeTrue(false);
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
    }

    @Override
    protected boolean hasLocalConflictCounter() {
        return false;
    }

    @Test
    public void whenUntrackedRead_thenSpeculativeConfigError() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false)
                .init();

        LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(config);

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
        assertIsAborted(tx);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertRefHasNoLocks(ref);
    }
}
