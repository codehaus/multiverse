package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_abortTest
        extends BetaTransaction_abortTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public boolean doesSupportListeners() {
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
