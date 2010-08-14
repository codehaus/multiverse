package org.multiverse.api.blocking;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertClosed;

public class CheapLatch_tryAwaitUninterruptibleTest {

    @Test
    public void whenCalledThenUnsupportedOperationException() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        try {
            latch.tryAwaitUninterruptible(1, 1, TimeUnit.NANOSECONDS);
            fail();
        } catch (UnsupportedOperationException expected) {
        }

        assertClosed(latch);
        assertEquals(era, latch.getEra());
    }
}
