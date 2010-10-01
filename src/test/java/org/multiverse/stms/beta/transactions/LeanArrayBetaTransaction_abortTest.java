package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

public class LeanArrayBetaTransaction_abortTest
        extends BetaTransaction_abortTest{


    @Override
    public boolean doesSupportListeners() {
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
    public boolean doesTransactionSupportCommute() {
        return false;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }
}
