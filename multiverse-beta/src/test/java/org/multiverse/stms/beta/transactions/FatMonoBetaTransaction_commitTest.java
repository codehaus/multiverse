package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_commitTest
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
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
    }

    @Override
    public boolean isTransactionSupportingCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
    }


}
