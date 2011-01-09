package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_openForReadTest extends GammaTransaction_openForReadTest<MonoGammaTransaction> {

    @Override
    protected MonoGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MonoGammaTransaction(config);
    }

    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(new GammaTransactionConfiguration(stm));
    }

     @Override
    protected int getMaxCapacity() {
        return 1;
    }
}
