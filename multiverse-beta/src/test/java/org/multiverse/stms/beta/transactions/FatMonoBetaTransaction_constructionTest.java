package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;

public class FatMonoBetaTransaction_constructionTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void test() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        assertIsActive(tx);
        assertEquals(1, tx.getAttempt());
    }

    @Test
    public void testTimeout() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10));

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        assertEquals(TimeUnit.SECONDS.toNanos(10), tx.getRemainingTimeoutNs());
    }
}
