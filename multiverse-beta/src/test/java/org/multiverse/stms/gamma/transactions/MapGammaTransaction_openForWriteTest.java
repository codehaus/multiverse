package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_openForWriteTest extends GammaTransaction_openForWriteTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MapGammaTransaction(config);
    }

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }

    @Override
    protected int getMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
