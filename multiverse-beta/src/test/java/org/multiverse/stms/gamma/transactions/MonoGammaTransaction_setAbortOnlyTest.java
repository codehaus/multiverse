package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_setAbortOnlyTest extends GammaTransaction_setAbortOnlyTest<MonoGammaTransaction> {
    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }

    @Override
    protected MonoGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MonoGammaTransaction(config);
    }
}
