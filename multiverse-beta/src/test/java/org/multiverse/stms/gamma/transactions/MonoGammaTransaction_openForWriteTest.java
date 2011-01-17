package org.multiverse.stms.gamma.transactions;

public class MonoGammaTransaction_openForWriteTest extends GammaTransaction_openForWriteTest {

    @Override
    protected GammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MonoGammaTransaction(config);
    }

    protected GammaTransaction newTransaction() {
        return new MonoGammaTransaction(new GammaTransactionConfiguration(stm));
    }

    @Override
    protected int getMaxCapacity() {
        return 1;
    }
}
