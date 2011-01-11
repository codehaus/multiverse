package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_softResetTest extends GammaTransaction_softResetTest<MapGammaTransaction> {
    @Override
    public MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }
}
