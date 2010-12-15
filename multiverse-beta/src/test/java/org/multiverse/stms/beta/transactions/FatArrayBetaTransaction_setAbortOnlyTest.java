package org.multiverse.stms.beta.transactions;

public class FatArrayBetaTransaction_setAbortOnlyTest
        extends BetaTransaction_setAbortOnlyTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }
}
