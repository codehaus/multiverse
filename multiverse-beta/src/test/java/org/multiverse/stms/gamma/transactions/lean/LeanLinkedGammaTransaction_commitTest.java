package org.multiverse.stms.gamma.transactions.lean;

public class LeanLinkedGammaTransaction_commitTest extends LeanGammaTransaction_commitTest<LeanLinkedGammaTransaction> {

    @Override
    public LeanLinkedGammaTransaction newTransaction() {
        return new LeanLinkedGammaTransaction(stm);
    }

    @Override
    public void assertClearedAfterCommit() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void assertClearedAfterAbort() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
