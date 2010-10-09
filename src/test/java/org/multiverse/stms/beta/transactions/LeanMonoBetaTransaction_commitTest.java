package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_commitTest
        extends BetaTransaction_commitTest {

    @Override
    public boolean isSupportingListeners() {
        return false;
    }

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return false;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanMonoBetaTransaction(config);
    }

    @Override
    public boolean isTransactionSupportingCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
    }
}
