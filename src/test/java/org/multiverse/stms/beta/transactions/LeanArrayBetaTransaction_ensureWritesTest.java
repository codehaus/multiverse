package org.multiverse.stms.beta.transactions;

public class LeanArrayBetaTransaction_ensureWritesTest
        extends BetaTransaction_ensureWritesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayBetaTransaction(stm);
    }
}