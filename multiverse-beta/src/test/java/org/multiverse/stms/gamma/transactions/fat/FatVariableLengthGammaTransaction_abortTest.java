package org.multiverse.stms.gamma.transactions.fat;

public class FatVariableLengthGammaTransaction_abortTest extends FatGammaTransaction_abortTest<FatVariableLengthGammaTransaction> {

    @Override
    protected FatVariableLengthGammaTransaction newTransaction() {
        return new FatVariableLengthGammaTransaction(stm);
    }

    @Override
    protected void assertCleaned(FatVariableLengthGammaTransaction tx) {
        //throw new TodoException();
        //todo
    }
}
