package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_prepareTest extends GammaTransaction_prepareTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }

    @Override
    protected MapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MapGammaTransaction(config);
    }
}
