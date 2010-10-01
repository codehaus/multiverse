package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_ensureWritesTest     
        extends BetaTransaction_ensureWritesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayTreeBetaTransaction(stm);
    }
}