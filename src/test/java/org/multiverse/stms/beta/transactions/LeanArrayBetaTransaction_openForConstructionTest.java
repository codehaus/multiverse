package org.multiverse.stms.beta.transactions;

public class LeanArrayBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
            return new LeanArrayBetaTransaction(config);
    }
}
