package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_registerChangeListenerAndAbortTest
        extends BetaTransaction_registerChangeListenerAndAbortTest {

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
