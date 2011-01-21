package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

import static org.junit.Assert.assertNull;

public class FatMonoGammaTransaction_commitTest extends FatGammaTransaction_commitTest<FatMonoGammaTransaction> {

    @Override
    protected void assertCleaned(FatMonoGammaTransaction transaction) {
        assertNull(transaction.tranlocal.owner);
    }

    @Override
    protected FatMonoGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatMonoGammaTransaction(config);
    }

    protected FatMonoGammaTransaction newTransaction() {
        return new FatMonoGammaTransaction(new GammaTransactionConfiguration(stm));
    }
}
