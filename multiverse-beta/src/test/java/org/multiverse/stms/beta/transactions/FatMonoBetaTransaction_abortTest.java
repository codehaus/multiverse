package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_abortTest
        extends BetaTransaction_abortTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatMonoBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatMonoBetaTransaction(config);
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
        return 1;
    }
}
