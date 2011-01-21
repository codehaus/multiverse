package org.multiverse.stms.gamma.transactions.lean;

public class LeanMonoGammaTransaction_retryTest extends LeanGammaTransaction_retryTest<LeanMonoGammaTransaction> {

    @Override
    public LeanMonoGammaTransaction newTransaction() {
        return new LeanMonoGammaTransaction(stm);
    }
}
