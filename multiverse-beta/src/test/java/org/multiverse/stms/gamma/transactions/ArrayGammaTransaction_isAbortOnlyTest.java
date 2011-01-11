package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_isAbortOnlyTest extends GammaTransaction_isAbortOnlyTest<ArrayGammaTransaction> {

    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
