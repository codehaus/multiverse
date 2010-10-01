package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

public class FatArrayBetaTransaction_commuteTest extends BetaTransaction_commuteTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(config);
    }

    @Override
    public boolean isTransactionSupportingCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }
}
