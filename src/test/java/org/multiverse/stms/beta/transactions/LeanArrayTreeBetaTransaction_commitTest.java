package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_commitTest
        extends BetaTransaction_commitTest {

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return false;
    }

    @Override
    public boolean isSupportingListeners() {
        return false;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayTreeBetaTransaction(config);
    }

    @Override
    public boolean isTransactionSupportingCommute() {
        return false;
    }

     @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
