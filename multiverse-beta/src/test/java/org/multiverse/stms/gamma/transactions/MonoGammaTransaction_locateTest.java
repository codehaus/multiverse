package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_locateTest extends GammaTransaction_locateTest<MonoGammaTransaction> {

    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }
}
