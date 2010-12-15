package org.multiverse.stms.beta.transactions;

public class FatArrayTreeTransaction_setAbortOnlyTest
        extends BetaTransaction_setAbortOnlyTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }
}
