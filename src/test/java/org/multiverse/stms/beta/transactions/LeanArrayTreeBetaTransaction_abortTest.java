package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_abortTest
        extends BetaTransaction_abortTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayTreeBetaTransaction(config);
    }

    @Override
    public boolean doesSupportListeners() {
        return false;
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