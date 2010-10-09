package org.multiverse.stms.beta.transactions;

public class LeanArrayBetaTransaction_typesTest
        extends BetaTransaction_typesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }
}
