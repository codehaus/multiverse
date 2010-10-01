package org.multiverse.stms.beta.transactions;

public class FatMonoBetaTransaction_ensureWritesTest
        extends BetaTransaction_ensureWritesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(stm);
    }
}
