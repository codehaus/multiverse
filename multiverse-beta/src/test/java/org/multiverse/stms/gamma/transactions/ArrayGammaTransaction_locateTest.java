package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_locateTest extends GammaTransaction_locateTest<ArrayGammaTransaction> {

    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
