package org.multiverse.stms.beta.transactions;

public class FatMonoBetaTransaction_setAbortOnlyTest
        extends BetaTransaction_setAbortOnlyTest{

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }
}
