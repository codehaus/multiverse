package org.multiverse.stms.gamma.transactions.fat;

public class FatMapGammaTransaction_isAbortOnlyTest extends FatGammaTransaction_isAbortOnlyTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }


}
