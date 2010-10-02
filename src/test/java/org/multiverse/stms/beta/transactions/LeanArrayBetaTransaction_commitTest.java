package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

public class LeanArrayBetaTransaction_commitTest
        extends BetaTransaction_commitTest{

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return false;
    }

    @Override
    public boolean isSupportingListeners() {
        return false;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayBetaTransaction(config);
    }

    @Override
    public boolean isTransactionSupportingCommute() {
        return false;
    }

     @Override
    public int getTransactionMaxCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }
}
