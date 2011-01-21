package org.multiverse.stms.gamma.transactions.fat;

public class FatArrayGammaTransaction_softResetTest extends FatGammaTransaction_softResetTest<FatLinkedGammaTransaction> {
    @Override
    public FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }
}
