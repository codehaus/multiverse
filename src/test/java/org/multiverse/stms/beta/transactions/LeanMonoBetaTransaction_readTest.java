package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_readTest extends BetaTransaction_readTest{
    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }
}
