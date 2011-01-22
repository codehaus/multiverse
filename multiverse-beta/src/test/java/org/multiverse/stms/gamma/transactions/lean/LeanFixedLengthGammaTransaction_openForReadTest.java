package org.multiverse.stms.gamma.transactions.lean;

public class LeanFixedLengthGammaTransaction_openForReadTest extends LeanGammaTransaction_openForReadTest<LeanFixedLengthGammaTransaction> {

    @Override
    public LeanFixedLengthGammaTransaction newTransaction() {
        return new LeanFixedLengthGammaTransaction(stm);
    }
}
