package org.multiverse.stms.beta.transactions;

public class FatArrayTreeBetaTransaction_ensureWritesTest
        extends BetaTransaction_ensureWritesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayTreeBetaTransaction(stm);
    }
}
