package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_locateTest extends GammaTransaction_locateTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }
}
