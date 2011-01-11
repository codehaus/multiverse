package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_hardResetTest extends GammaTransaction_hardResetTest<GammaTransaction> {

    @Override
    protected GammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
