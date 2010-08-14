package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertActive;

public class FatArrayTreeBetaTransaction_constructionTest {

     private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        assertActive(tx);
        assertEquals(1, tx.getAttempt());
    }

     @Test
    public void testTimeout(){
        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10));

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        assertEquals(TimeUnit.SECONDS.toNanos(10), tx.getRemainingTimeoutNs());
    }
}
