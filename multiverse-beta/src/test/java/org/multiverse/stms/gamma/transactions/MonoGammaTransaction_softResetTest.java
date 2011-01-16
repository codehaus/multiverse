package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_softResetTest extends GammaTransaction_softResetTest<MonoGammaTransaction> {

    @Override
    public MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(stm);
    }
}
