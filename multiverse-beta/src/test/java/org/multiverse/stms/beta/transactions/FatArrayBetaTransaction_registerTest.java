package org.multiverse.stms.beta.transactions;

public class FatArrayBetaTransaction_registerTest
        extends BetaTransaction_registerTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(config);
    }
}
