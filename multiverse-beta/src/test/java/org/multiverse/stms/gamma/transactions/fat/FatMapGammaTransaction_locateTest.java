package org.multiverse.stms.gamma.transactions.fat;

public class FatMapGammaTransaction_locateTest extends FatGammaTransaction_locateTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }
}
