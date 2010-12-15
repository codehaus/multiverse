package org.multiverse.stms.beta.transactions;

public class FatMonoBetaTransaction_typesTest
        extends BetaTransaction_typesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }
}
