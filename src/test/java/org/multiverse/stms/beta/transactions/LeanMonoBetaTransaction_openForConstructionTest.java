package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanMonoBetaTransaction(config);
    }
}
