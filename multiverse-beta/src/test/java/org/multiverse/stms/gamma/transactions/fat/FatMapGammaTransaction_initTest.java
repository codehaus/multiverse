package org.multiverse.stms.gamma.transactions.fat;

public class FatMapGammaTransaction_initTest extends FatGammaTransaction_initTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }
}
