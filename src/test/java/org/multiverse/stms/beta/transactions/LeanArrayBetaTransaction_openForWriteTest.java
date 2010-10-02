package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

public class LeanArrayBetaTransaction_openForWriteTest
        extends BetaTransaction_openForWriteTest{

    @Override
    public BetaTransaction newTransaction() {
        return new LeanArrayBetaTransaction(stm);
    }

    @Override
    public boolean hasLocalConflictCounter() {
        return true;
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new LeanArrayBetaTransaction(config);
    }

    @Override
    public boolean isSupportingCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }
}
