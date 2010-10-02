package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_registerChangeListenerAndAbortTest
        extends BetaTransaction_registerChangeListenerAndAbortTest {

    @Override
    public int getTransactionMaxCapacity() {
        return 1;
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
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
    }
}
