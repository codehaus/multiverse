package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_typesTest
        extends BetaTransaction_typesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }
}
