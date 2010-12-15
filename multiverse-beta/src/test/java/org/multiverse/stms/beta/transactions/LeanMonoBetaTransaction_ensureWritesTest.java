package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_ensureWritesTest
        extends BetaTransaction_ensureWritesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanMonoBetaTransaction(stm);
    }
}