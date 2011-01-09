package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_retryTest extends GammaTransaction_retryTest<MonoGammaTransaction> {

    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }

    @Override
    protected MonoGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MonoGammaTransaction(config);
    }
}
