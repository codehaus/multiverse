package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_readTest extends BetaTransaction_readTest{

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }
}
