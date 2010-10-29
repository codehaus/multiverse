package org.multiverse.stms.beta.transactions;

public class LeanArrayBetaTransaction_readTest extends BetaTransaction_readTest{
    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }
}
