package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatMapGammaTransaction_openForConstructionTest
        extends FatGammaTransaction_openForConstructionTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }

    @Override
    protected FatMapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatMapGammaTransaction(config);
    }
}
