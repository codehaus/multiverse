package org.multiverse.stms.gamma.transactions.lean;

public class LeanLinkedGammaTransaction_abortTest extends LeanGammaTransaction_abortTest<LeanLinkedGammaTransaction> {
    @Override
    public LeanLinkedGammaTransaction newTransaction() {
        return new LeanLinkedGammaTransaction(stm);
    }
}
