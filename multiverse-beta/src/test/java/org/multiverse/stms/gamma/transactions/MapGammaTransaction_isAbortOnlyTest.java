package org.multiverse.stms.gamma.transactions;

import org.junit.Test;

public class MapGammaTransaction_isAbortOnlyTest extends GammaTransaction_isAbortOnlyTest<MapGammaTransaction> {

    @Override
    protected MapGammaTransaction newTransaction() {
        return new MapGammaTransaction(stm);
    }


}
