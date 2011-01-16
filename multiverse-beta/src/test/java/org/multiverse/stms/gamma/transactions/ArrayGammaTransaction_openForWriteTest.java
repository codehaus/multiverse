package org.multiverse.stms.gamma.transactions;

public class ArrayGammaTransaction_openForWriteTest extends GammaTransaction_openForWriteTest {

    @Override
    protected GammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new ArrayGammaTransaction(config);
    }

    @Override
    protected GammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }

     @Override
    protected int getMaxCapacity() {
        return new GammaTransactionConfiguration(stm).arrayTransactionSize;
    }
}
