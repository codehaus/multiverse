package org.multiverse.stms.gamma.transactions.lean;

public class LeanLinkedGammaTransaction_setAbortOnlyTest extends LeanGammaTransaction_setAbortOnlyTest<LeanLinkedGammaTransaction> {

    @Override
    public LeanLinkedGammaTransaction newTransaction() {
        return new LeanLinkedGammaTransaction(stm);
    }
}
