package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_openForWriteTest 
        extends BetaTransaction_openForWriteTest{

    @Override
    public boolean hasLocalConflictCounter() {
        return true;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayBetaTransaction(config);
    }

    @Override
    public boolean isSupportingCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
