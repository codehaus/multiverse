package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

public class FatMonoGammaTransaction_openForWriteTest extends FatGammaTransaction_openForWriteTest {

    @Override
    protected GammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatMonoGammaTransaction(config);
    }

    protected GammaTransaction newTransaction() {
        return new FatMonoGammaTransaction(new GammaTransactionConfiguration(stm));
    }

    @Override
    protected int getMaxCapacity() {
        return 1;
    }
}
