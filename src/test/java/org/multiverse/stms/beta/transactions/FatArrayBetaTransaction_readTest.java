package org.multiverse.stms.beta.transactions;

public class FatArrayBetaTransaction_readTest extends BetaTransaction_readTest{

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }
}