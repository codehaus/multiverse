package org.multiverse.stms.beta.transactions;

public class LeanArrayTreeBetaTransaction_typesTest
        extends BetaTransaction_typesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayTreeBetaTransaction(stm);
    }
}
