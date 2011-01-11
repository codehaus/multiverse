package org.multiverse.stms.gamma.transactions;

import static org.junit.Assert.assertNull;

public class MonoGammaTransaction_abortTest extends GammaTransaction_abortTest<MonoGammaTransaction> {

    @Override
    protected void assertCleaned(MonoGammaTransaction tx) {
        assertNull(tx.tranlocal.owner);
    }

    protected MonoGammaTransaction newTransaction() {
        return new MonoGammaTransaction(new GammaTransactionConfiguration(stm));
    }
}
