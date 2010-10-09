package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_setAbortOnlyTest
        extends BetaTransaction_setAbortOnlyTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }
}
