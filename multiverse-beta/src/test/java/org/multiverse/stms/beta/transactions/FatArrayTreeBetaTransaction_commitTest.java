package org.multiverse.stms.beta.transactions;

import org.multiverse.stms.beta.BetaStmConfiguration;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_commitTest
        extends BetaTransaction_commitTest {

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return true;
    }

    @Override
    public boolean isSupportingListeners() {
        return true;
    }

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayTreeBetaTransaction(config);
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
