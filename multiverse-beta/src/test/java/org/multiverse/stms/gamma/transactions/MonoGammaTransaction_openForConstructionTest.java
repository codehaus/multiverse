package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_openForConstructionTest
        extends GammaTransaction_openForConstructionTest<MonoGammaTransaction> {

    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }

    @Override
    protected MonoGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MonoGammaTransaction(config);
    }
}
