package org.multiverse.stms.beta.transactions;

public class FatArrayTreeBetaTransaction_commuteTest
        extends BetaTransaction_commuteTest{

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayTreeBetaTransaction(config);
    }

    @Override
    public boolean isTransactionSupportingCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
