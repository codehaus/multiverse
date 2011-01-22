package org.multiverse.stms.gamma.transactions.fat;

public class FatVariableLengthGammaTransaction_hardResetTest extends FatGammaTransaction_hardResetTest<FatVariableLengthGammaTransaction> {

    @Override
    protected FatVariableLengthGammaTransaction newTransaction() {
        return new FatVariableLengthGammaTransaction(stm);
    }
}
