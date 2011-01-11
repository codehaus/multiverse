package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_isAbortOnlyTest extends GammaTransaction_isAbortOnlyTest<MonoGammaTransaction>{

    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }
}
