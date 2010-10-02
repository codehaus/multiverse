package org.multiverse.stms.beta.transactions;

public class FatArrayTreeBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    protected boolean hasLocalConflictCounter() {
        return true;
    }

    @Override
    protected int getMaxTransactionCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayTreeBetaTransaction(config);
    }
}
