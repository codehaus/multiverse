package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_prepareTest
        extends BetaTransaction_prepareTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return false;
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayTreeBetaTransaction(config);
    }

    @Override
    public boolean doesTransactionSupportCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
