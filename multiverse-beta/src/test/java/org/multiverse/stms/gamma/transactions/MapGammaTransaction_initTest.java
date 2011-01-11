package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_initTest extends GammaTransaction_initTest<MapGammaTransaction>{

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }
}
