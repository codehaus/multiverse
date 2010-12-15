package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    protected int getMaxTransactionCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean hasLocalConflictCounter() {
        return true;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayTreeBetaTransaction(config);
    }
}
