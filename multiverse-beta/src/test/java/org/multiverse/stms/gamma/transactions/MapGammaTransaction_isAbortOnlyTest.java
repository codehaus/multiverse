package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_isAbortOnlyTest extends GammaTransaction_isAbortOnlyTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }


}
