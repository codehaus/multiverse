package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_registerChangeListenerAndAbortTest
        extends BetaTransaction_registerChangeListenerAndAbortTest {

    @Override
    public boolean isSupportingListeners() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
    }

    @Override
    public boolean isSupportingCommute() {
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
}