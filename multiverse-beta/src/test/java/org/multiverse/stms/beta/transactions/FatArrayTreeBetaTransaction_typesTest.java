package org.multiverse.stms.beta.transactions;

public class FatArrayTreeBetaTransaction_typesTest
        extends BetaTransaction_typesTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }
}
