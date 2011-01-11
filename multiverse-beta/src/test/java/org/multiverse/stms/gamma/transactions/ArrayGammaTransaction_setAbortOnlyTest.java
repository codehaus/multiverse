package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_setAbortOnlyTest extends GammaTransaction_setAbortOnlyTest<ArrayGammaTransaction> {

    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }

    @Override
    protected ArrayGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new ArrayGammaTransaction(config);
    }
}
