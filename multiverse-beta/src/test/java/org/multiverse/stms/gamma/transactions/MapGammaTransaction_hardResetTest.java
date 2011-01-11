package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_hardResetTest extends GammaTransaction_hardResetTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }
}
