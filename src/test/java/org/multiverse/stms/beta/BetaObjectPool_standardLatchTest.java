package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.StandardLatch;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertClosed;
import static org.multiverse.TestUtils.assertEra;

public class BetaObjectPool_standardLatchTest {

    private BetaObjectPool pool;

    @Before
    public void setUp() {
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullLatch_thenNullPointerException() {
        pool.putStandardLatch(null);
    }

    @Test
    public void whenPlacedInPoolThenReset() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.open(era);

        pool.putStandardLatch(latch);

        assertEra(latch, era + 1);
        assertClosed(latch);
    }

    @Test
    public void whenSuccess() {
        StandardLatch latch1 = new StandardLatch();
        StandardLatch latch2 = new StandardLatch();
        StandardLatch latch3 = new StandardLatch();

        pool.putStandardLatch(latch1);
        pool.putStandardLatch(latch2);
        pool.putStandardLatch(latch3);

        assertSame(latch3, pool.takeStandardLatch());
        assertSame(latch2, pool.takeStandardLatch());
        assertSame(latch1, pool.takeStandardLatch());
        assertNull(pool.takeCheapLatch());
    }
}
