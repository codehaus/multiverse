package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_softResetTest extends GammaTransaction_softResetTest<ArrayGammaTransaction> {
    @Override
    public ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
