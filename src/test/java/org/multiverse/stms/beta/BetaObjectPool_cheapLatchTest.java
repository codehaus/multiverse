package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertClosed;
import static org.multiverse.TestUtils.assertEra;

public class BetaObjectPool_cheapLatchTest {
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullLatch_thenNullPointerException() {
        pool.putCheapLatch(null);
    }

    @Test
    public void whenPlacedInPoolThenReset() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        pool.putCheapLatch(latch);

        assertEra(latch, era + 1);
        assertClosed(latch);
    }

    @Test
    public void whenSuccess() {
        CheapLatch latch1 = new CheapLatch();
        CheapLatch latch2 = new CheapLatch();
        CheapLatch latch3 = new CheapLatch();

        pool.putCheapLatch(latch1);
        pool.putCheapLatch(latch2);
        pool.putCheapLatch(latch3);

        assertSame(latch3, pool.takeCheapLatch());
        assertSame(latch2, pool.takeCheapLatch());
        assertSame(latch1, pool.takeCheapLatch());
        assertNull(pool.takeCheapLatch());
    }
}
