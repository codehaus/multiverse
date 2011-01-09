package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_openForReadTest extends GammaTransaction_openForReadTest<MapGammaTransaction> {

    @Override
    protected int getMaxCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }

    @Override
    protected MapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MapGammaTransaction(config);
    }
}
