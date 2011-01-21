package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatArrayGammaTransaction_openForConstructionTest
        extends FatGammaTransaction_openForConstructionTest<FatLinkedGammaTransaction> {

    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }

    @Override
    protected FatLinkedGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatLinkedGammaTransaction(config);
    }
}
