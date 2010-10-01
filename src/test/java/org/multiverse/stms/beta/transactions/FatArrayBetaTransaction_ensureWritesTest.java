package org.multiverse.stms.beta.transactions;

public class FatArrayBetaTransaction_ensureWritesTest
        extends BetaTransaction_ensureWritesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(stm);
    }
}
