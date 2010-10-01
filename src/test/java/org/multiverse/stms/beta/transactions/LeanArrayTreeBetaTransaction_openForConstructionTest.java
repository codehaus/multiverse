package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_openForConstructionTest 
    extends BetaTransaction_openForConstructionTest{

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayTreeBetaTransaction(config);
    }
}
