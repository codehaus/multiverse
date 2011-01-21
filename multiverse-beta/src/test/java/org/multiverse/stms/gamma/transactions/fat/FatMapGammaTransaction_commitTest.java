package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatMapGammaTransaction_commitTest extends FatGammaTransaction_commitTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }

    @Override
    protected FatMapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatMapGammaTransaction(config);
    }

    @Override
    protected void assertCleaned(FatMapGammaTransaction transaction) {
        //throw new TodoException();
    }
}
