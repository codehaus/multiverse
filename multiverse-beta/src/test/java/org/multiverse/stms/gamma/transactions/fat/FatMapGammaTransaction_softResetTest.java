package org.multiverse.stms.gamma.transactions.fat;

public class FatMapGammaTransaction_softResetTest extends FatGammaTransaction_softResetTest<FatMapGammaTransaction> {
    @Override
    public FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }
}
