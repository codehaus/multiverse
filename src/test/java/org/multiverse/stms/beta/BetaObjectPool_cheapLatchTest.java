package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.DefaultRetryLatch;

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
        pool.putDefaultRetryLatch(null);
    }

    @Test
    public void whenPlacedInPoolThenReset() {
        DefaultRetryLatch latch = new DefaultRetryLatch();
        long era = latch.getEra();
        latch.open(era);

        pool.putDefaultRetryLatch(latch);

        assertEra(latch, era + 1);
        assertClosed(latch);
    }

    @Test
    public void whenSuccess() {
        DefaultRetryLatch latch1 = new DefaultRetryLatch();
        DefaultRetryLatch latch2 = new DefaultRetryLatch();
        DefaultRetryLatch latch3 = new DefaultRetryLatch();

        pool.putDefaultRetryLatch(latch1);
        pool.putDefaultRetryLatch(latch2);
        pool.putDefaultRetryLatch(latch3);

        assertSame(latch3, pool.takeDefaultRetryLatch());
        assertSame(latch2, pool.takeDefaultRetryLatch());
        assertSame(latch1, pool.takeDefaultRetryLatch());
        assertNull(pool.takeDefaultRetryLatch());
    }
}
