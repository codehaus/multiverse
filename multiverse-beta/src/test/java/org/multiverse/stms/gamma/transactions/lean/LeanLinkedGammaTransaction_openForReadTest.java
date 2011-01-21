package org.multiverse.stms.gamma.transactions.lean;

public class LeanLinkedGammaTransaction_openForReadTest extends LeanGammaTransaction_openForReadTest<LeanLinkedGammaTransaction> {

    @Override
    public LeanLinkedGammaTransaction newTransaction() {
        return new LeanLinkedGammaTransaction(stm);
    }
}
