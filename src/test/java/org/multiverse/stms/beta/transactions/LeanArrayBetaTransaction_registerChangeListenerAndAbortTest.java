package org.multiverse.stms.beta.transactions;

public class LeanArrayBetaTransaction_registerChangeListenerAndAbortTest
        extends BetaTransaction_registerChangeListenerAndAbortTest {

    @Override
    public boolean isSupportingListeners() {
        return false;
    }

    @Override
    public boolean isSupportingCommute() {
        return false;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayBetaTransaction(config);
    }
}
