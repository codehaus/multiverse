package org.multiverse.stms.beta.transactions;

import static org.junit.Assume.assumeTrue;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_openForReadTest
        extends BetaTransaction_openForReadTest {

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

    @Override
    protected void assumeIsAbleToNotTrackReads() {
        assumeTrue(true);
    }


}
