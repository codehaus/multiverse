package org.multiverse.stms.gamma.transactions.fat;

public class FatMapGammaTransaction_hardResetTest extends FatGammaTransaction_hardResetTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }
}
