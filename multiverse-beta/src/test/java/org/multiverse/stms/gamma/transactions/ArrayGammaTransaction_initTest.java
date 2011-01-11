package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_initTest extends GammaTransaction_initTest<ArrayGammaTransaction> {
    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
