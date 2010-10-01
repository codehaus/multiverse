package org.multiverse.stms.beta.transactions;

import static org.junit.Assume.assumeTrue;

public class LeanArrayTreeBetaTransaction_openForReadTest
        extends BetaTransaction_openForReadTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayBetaTransaction(config);
    }

    @Override
    public boolean doesTransactionSupportCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void assumeIsAbleToNotTrackReads() {
        assumeTrue(true);
    }
}
