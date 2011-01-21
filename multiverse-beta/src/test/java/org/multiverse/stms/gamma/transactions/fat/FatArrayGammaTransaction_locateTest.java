package org.multiverse.stms.gamma.transactions.fat;

public class FatArrayGammaTransaction_locateTest extends FatGammaTransaction_locateTest<FatLinkedGammaTransaction> {

    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }
}
