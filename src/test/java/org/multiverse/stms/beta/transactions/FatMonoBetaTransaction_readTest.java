package org.multiverse.stms.beta.transactions;

public class FatMonoBetaTransaction_readTest extends BetaTransaction_readTest{
    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }
}
