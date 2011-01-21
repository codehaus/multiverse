package org.multiverse.stms.gamma.transactions.fat;

public class FatArrayGammaTransaction_isAbortOnlyTest extends FatGammaTransaction_isAbortOnlyTest<FatLinkedGammaTransaction> {

    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }
}
