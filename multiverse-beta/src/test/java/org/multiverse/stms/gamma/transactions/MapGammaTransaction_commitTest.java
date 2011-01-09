package org.multiverse.stms.gamma.transactions;

public class MapGammaTransaction_commitTest extends GammaTransaction_commitTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }

    @Override
    protected MapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MapGammaTransaction(config);
    }

    @Override
    protected void assertCleaned(MapGammaTransaction transaction) {
        //throw new TodoException();
    }
}
