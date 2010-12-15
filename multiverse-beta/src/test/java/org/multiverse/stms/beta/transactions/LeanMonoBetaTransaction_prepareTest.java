package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_prepareTest
        extends BetaTransaction_prepareTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return false;
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
    public int getTransactionMaxCapacity() {
        return 1;
    }
}
