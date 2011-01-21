package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatArrayGammaTransaction_commuteTest extends FatGammaTransaction_commuteTest<FatLinkedGammaTransaction> {

    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }

    @Override
    protected FatLinkedGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatLinkedGammaTransaction(config);
    }

    @Override
    protected int getMaxCapacity() {
        return new GammaTransactionConfiguration(stm).arrayTransactionSize;
    }
}
