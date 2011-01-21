package org.multiverse.stms.gamma.transactions.lean;

public class LeanLinkedGammaTransaction_openForWriteTest extends LeanGammaTransaction_openForWriteTest<LeanLinkedGammaTransaction> {

    @Override
    public LeanLinkedGammaTransaction newTransaction() {
        return new LeanLinkedGammaTransaction(stm);
    }
}
