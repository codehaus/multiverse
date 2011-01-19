package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.assertIsActive;

public class FatArrayBetaTransaction_constructionTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void test() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        assertIsActive(tx);
        assertEquals(1, tx.getAttempt());
    }

    @Test
    public void testTimeout() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10));

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        assertEquals(TimeUnit.SECONDS.toNanos(10), tx.getRemainingTimeoutNs());
    }
}
