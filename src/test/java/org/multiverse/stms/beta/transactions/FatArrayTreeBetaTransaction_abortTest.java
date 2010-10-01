package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_abortTest
        extends BetaTransaction_abortTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayBetaTransaction(config);
    }

    @Override
    public boolean doesSupportListeners() {
        return true;
    }

    @Override
    public boolean doesTransactionSupportCommute() {
        return true;
    }

    @Override
    public int getTransactionMaxCapacity() {
        return Integer.MAX_VALUE;
    }
}
