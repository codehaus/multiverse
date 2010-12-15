package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_retryTest
        extends BetaTransaction_retryTest {

    @Override
    public int getTransactionMaxCapacity() {
        return new BetaStmConfiguration().maxArrayTransactionSize;
    }

    @Override
    public boolean isSupportingListeners() {
        return true;
    }

    @Override
    public boolean isSupportingCommute() {
        return true;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(config);
    }
}
