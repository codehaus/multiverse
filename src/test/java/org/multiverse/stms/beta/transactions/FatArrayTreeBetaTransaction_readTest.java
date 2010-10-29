package org.multiverse.stms.beta.transactions;

public class FatArrayTreeBetaTransaction_readTest extends BetaTransaction_readTest{
    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }
}
