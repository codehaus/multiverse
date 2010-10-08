package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_openForWriteTest
        extends BetaTransaction_openForWriteTest{

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public boolean hasLocalConflictCounter() {
        return false;
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanMonoBetaTransaction(config);
    }

    @Override
    public boolean isSupportingCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
    }
}