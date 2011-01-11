package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_prepareTest extends GammaTransaction_prepareTest<ArrayGammaTransaction> {

    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
