package org.multiverse.stms.gamma.transactions.fat;

public class FatMapGammaTransaction_abortTest extends FatGammaTransaction_abortTest<FatMapGammaTransaction> {

    @Override
    protected FatMapGammaTransaction newTransaction() {
        return new FatMapGammaTransaction(stm);
    }

    @Override
    protected void assertCleaned(FatMapGammaTransaction tx) {
        //throw new TodoException();
        //todo
    }
}
