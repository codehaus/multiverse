package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_hardResetTest extends GammaTransaction_hardResetTest<MonoGammaTransaction> {

    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }
}
