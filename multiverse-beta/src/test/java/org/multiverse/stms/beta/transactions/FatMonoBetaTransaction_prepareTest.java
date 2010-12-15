package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_prepareTest extends BetaTransaction_prepareTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public boolean isSupportingWriteSkewDetection() {
        return true;
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
    }

    @Override
    public boolean doesTransactionSupportCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
    }
}
