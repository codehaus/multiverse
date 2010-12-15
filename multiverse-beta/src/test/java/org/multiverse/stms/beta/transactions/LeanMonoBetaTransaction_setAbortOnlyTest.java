package org.multiverse.stms.beta.transactions;

public class LeanMonoBetaTransaction_setAbortOnlyTest
        extends BetaTransaction_setAbortOnlyTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanMonoBetaTransaction(stm);
    }
}
