package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_openForReadTest extends GammaTransaction_openForReadTest<ArrayGammaTransaction> {

    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }

    @Override
    protected ArrayGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new ArrayGammaTransaction(config);
    }

    @Override
    protected int getMaxCapacity() {
        return new GammaTransactionConfiguration(stm).arraySize;
    }
}
