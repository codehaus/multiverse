package org.multiverse.stms.gamma.transactions.lean;

public class LeanFixedLengthGammaTransaction_openForWriteTest extends LeanGammaTransaction_openForWriteTest<LeanFixedLengthGammaTransaction> {

    @Override
    public LeanFixedLengthGammaTransaction newTransaction() {
        return new LeanFixedLengthGammaTransaction(stm);
    }
}
