package org.multiverse.stms.gamma.transactions;

import static org.junit.Assert.assertNull;

public class MonoGammaTransaction_commitTest extends GammaTransaction_commitTest<MonoGammaTransaction> {

    @Override
    protected void assertCleaned(MonoGammaTransaction transaction) {
        assertNull(transaction.tranlocal.owner);
    }

    @Override
    protected MonoGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new MonoGammaTransaction(config);
    }

    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(new GammaTransactionConfiguration(stm));
    }
}
