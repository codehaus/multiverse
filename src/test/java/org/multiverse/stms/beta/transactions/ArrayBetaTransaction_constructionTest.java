package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertActive;

public class ArrayBetaTransaction_constructionTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void test() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);

        assertActive(tx);
        assertEquals(1, tx.getAttempt());
    }
}
