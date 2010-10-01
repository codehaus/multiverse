package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

import static org.junit.Assume.assumeTrue;

public class LeanArrayBetaTransaction_openForReadTest
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
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }

    @Override
    protected void assumeIsAbleToNotTrackReads() {
        assumeTrue(true);
    }
}
