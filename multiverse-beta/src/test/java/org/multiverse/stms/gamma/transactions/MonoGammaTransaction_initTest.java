package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_initTest extends GammaTransaction_initTest<MonoGammaTransaction> {

    @Override
    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }
}