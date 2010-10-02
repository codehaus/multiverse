package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_commitTest extends BetaTransaction_commitTest {

    @Override
    public boolean isSupportingListeners() {
        return true;
    }

    @Override
    public boolean isSupportingWriteSkewDetection() {
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

    @Override
    public boolean isTransactionSupportingCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
