package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

public class LeanArrayBetaTransaction_openForConstructionTest
        extends BetaTransaction_openForConstructionTest {

    @Override
    protected boolean hasLocalConflictCounter() {
        return true;
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
    protected int getMaxTransactionCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }
}
