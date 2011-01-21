package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatMapGammaTransaction_openForReadTest extends FatGammaTransaction_openForReadTest<FatMapGammaTransaction> {

    @Override
    protected int getMaxCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }

    @Override
    protected FatMapGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatMapGammaTransaction(config);
    }
}
