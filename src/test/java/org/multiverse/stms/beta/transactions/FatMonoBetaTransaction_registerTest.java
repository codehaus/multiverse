package org.multiverse.stms.beta.transactions;

public class FatMonoBetaTransaction_registerTest
        extends BetaTransaction_registerTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
    }
}
