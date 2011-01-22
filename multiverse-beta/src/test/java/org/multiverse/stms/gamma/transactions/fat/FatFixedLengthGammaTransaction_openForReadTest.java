package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatFixedLengthGammaTransaction_openForReadTest extends FatGammaTransaction_openForReadTest<FatFixedLengthGammaTransaction> {

    @Override
    protected FatFixedLengthGammaTransaction newTransaction() {
        return new FatFixedLengthGammaTransaction(stm);
    }

    @Override
    protected FatFixedLengthGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatFixedLengthGammaTransaction(config);
    }

    @Override
    protected int getMaxCapacity() {
        return new GammaTransactionConfiguration(stm).maxFixedLengthTransactionSize;
    }
}