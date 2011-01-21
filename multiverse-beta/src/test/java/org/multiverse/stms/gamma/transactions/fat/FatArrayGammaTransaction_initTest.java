package org.multiverse.stms.gamma.transactions.fat;

public class FatArrayGammaTransaction_initTest extends FatGammaTransaction_initTest<FatLinkedGammaTransaction> {
    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }
}
