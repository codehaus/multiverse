package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatFixedLengthGammaTransaction_openForWriteTest extends FatGammaTransaction_openForWriteTest {

    @Override
    protected GammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatFixedLengthGammaTransaction(config);
    }

    @Override
    protected GammaTransaction newTransaction() {
        return new FatFixedLengthGammaTransaction(stm);
    }

    @Override
    protected int getMaxCapacity() {
        return new GammaTransactionConfiguration(stm).maxFixedLengthTransactionSize;
    }
}
