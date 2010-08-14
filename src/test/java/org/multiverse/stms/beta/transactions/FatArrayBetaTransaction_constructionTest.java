package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertActive;

public class FatArrayBetaTransaction_constructionTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        assertActive(tx);
        assertEquals(1, tx.getAttempt());
    }

    @Test
    public void testTimeout() {
        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10));

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        assertEquals(TimeUnit.SECONDS.toNanos(10), tx.getRemainingTimeoutNs());
    }
}
