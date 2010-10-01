package org.multiverse.stms.beta.transactions;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_openForWriteTest
        extends BetaTransaction_openForWriteTest {

    @Override
    public BetaTransaction newTransaction() {
        return new FatArrayTreeBetaTransaction(stm);
    }

    @Override
    public BetaTransaction newTransaction(BetaTransactionConfiguration config) {
        return new FatArrayTreeBetaTransaction(config);
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
