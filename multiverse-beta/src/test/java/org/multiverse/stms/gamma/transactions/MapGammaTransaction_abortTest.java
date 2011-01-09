package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_abortTest extends GammaTransaction_abortTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }

    @Override
    protected void assertCleaned(MapGammaTransaction tx) {
        //throw new TodoException();
        //todo
    }
}
