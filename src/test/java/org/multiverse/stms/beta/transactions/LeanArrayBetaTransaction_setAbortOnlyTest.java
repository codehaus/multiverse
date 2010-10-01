package org.multiverse.stms.beta.transactions;

public class LeanArrayBetaTransaction_setAbortOnlyTest
        extends BetaTransaction_setAbortOnlyTest{

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }
}
